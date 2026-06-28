package org.nexo.feedservice.service;

import lombok.extern.slf4j.Slf4j;
import org.nexo.feedservice.config.FeedProperties;
import org.nexo.feedservice.dto.FeedItem;
import org.nexo.feedservice.dto.ResponseData;
import org.nexo.feedservice.exception.FeedProcessingException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class FeedQueryService {
    private final FeedProperties feedProperties;
    private final FeedRedisService feedRedisService;
    private final FeedPersistenceService feedPersistenceService;
    private final UserFeedGraphService userFeedGraphService;
    private final TrendingService trendingService;
    private final FeedMergeService feedMergeService;
    private final FeedResponseBuilder responseBuilder;
    private final Map<FeedContentType, FeedContentProvider<?>> contentProviders;

    public FeedQueryService(
            FeedProperties feedProperties,
            FeedRedisService feedRedisService,
            FeedPersistenceService feedPersistenceService,
            UserFeedGraphService userFeedGraphService,
            TrendingService trendingService,
            FeedMergeService feedMergeService,
            FeedResponseBuilder responseBuilder,
            List<FeedContentProvider<?>> contentProviders
    ) {
        this.feedProperties = feedProperties;
        this.feedRedisService = feedRedisService;
        this.feedPersistenceService = feedPersistenceService;
        this.userFeedGraphService = userFeedGraphService;
        this.trendingService = trendingService;
        this.feedMergeService = feedMergeService;
        this.responseBuilder = responseBuilder;
        this.contentProviders = new EnumMap<>(FeedContentType.class);
        for (FeedContentProvider<?> provider : contentProviders) {
            this.contentProviders.put(provider.type(), provider);
        }
    }

    public Mono<ResponseData<?>> getHybridFeed(Long userId, int page, int limit, FeedContentType type) {
        long startOffset = (long) page * limit;

        if (startOffset >= feedProperties.getMaxRedisFeedSize()) {
            return fallbackToDatabase(userId, page, limit, type);
        }

        long fetchEnd = feedProperties.getMaxRedisFeedSize() - 1L;
        Mono<List<Long>> personalIdsMono = getPersonalFeedIds(userId, fetchEnd, type);
        Mono<List<Long>> trendingIdsMono = trendingService.getTrendingIds(type, 0, fetchEnd);

        return Mono.zip(personalIdsMono, trendingIdsMono)
                .map(tuple -> feedMergeService.interleavePersonalAndTrending(
                        tuple.getT1(), tuple.getT2(), startOffset, limit))
                .flatMap(ids -> fetchContentAndBuildResponse(type, ids, userId, page, limit, null));
    }

    private Mono<List<Long>> getPersonalFeedIds(Long userId, long fetchEnd, FeedContentType type) {
        Mono<List<FeedItem>> pushDataStream = feedRedisService.fetchPersonalFeed(type, userId, 0, fetchEnd);

        Mono<List<List<FeedItem>>> pullDataStream = userFeedGraphService.getFollowedKols(userId)
                .flatMap(kolIds -> feedRedisService.fetchOutboxes(type, kolIds, 0, fetchEnd));

        return Mono.zip(pushDataStream, pullDataStream)
                .flatMap(tuple -> {
                    List<List<FeedItem>> allSources = new ArrayList<>(tuple.getT2().size() + 1);
                    allSources.add(tuple.getT1());
                    allSources.addAll(tuple.getT2());

                    List<Long> redisIds = feedMergeService.toItemIds(feedMergeService.mergeSortedUnique(allSources));
                    if (redisIds.size() >= feedProperties.getMaxRedisFeedSize()) {
                        return Mono.just(redisIds);
                    }

                    return feedPersistenceService.findItemIdsByFollowerId(
                                    type, userId, 0, feedProperties.getMaxRedisFeedSize())
                            .map(pageResult -> feedMergeService.appendMissing(redisIds, pageResult.getContent()));
                });
    }

    private Mono<ResponseData<?>> fallbackToDatabase(Long userId, int page, int limit, FeedContentType type) {
        return feedPersistenceService.findItemIdsByFollowerId(type, userId, page, limit)
                .flatMap(pageResult -> {
                    List<Long> dbItemIds = pageResult.getContent();

                    if (dbItemIds.isEmpty()) {
                        log.info("DB feed empty for user {}, falling back to trending", userId);
                        return fallbackToTrending(userId, page, limit, type);
                    }

                    return fetchContentAndBuildResponse(type, dbItemIds, userId, page, limit, pageResult);
                });
    }

    private Mono<ResponseData<?>> fallbackToTrending(Long userId, int page, int limit, FeedContentType type) {
        long startOffset = (long) page * limit;
        long endOffset = startOffset + limit - 1L;

        return trendingService.getTrendingIds(type, startOffset, endOffset)
                .flatMap(ids -> fetchContentAndBuildResponse(type, ids, userId, page, limit, null));
    }

    private Mono<ResponseData<?>> fetchContentAndBuildResponse(
            FeedContentType type,
            List<Long> ids,
            Long userId,
            int page,
            int limit,
            Page<Long> pageResult
    ) {
        if (ids.isEmpty()) {
            return Mono.just(responseBuilder.empty(page, limit));
        }

        FeedContentProvider<?> provider = contentProviders.get(type);
        if (provider == null) {
            return Mono.error(new FeedProcessingException("Missing content provider for " + type));
        }

        return fetchContentAndBuildResponse(provider, ids, userId, page, limit, pageResult);
    }

    private <T> Mono<ResponseData<?>> fetchContentAndBuildResponse(
            FeedContentProvider<T> provider,
            List<Long> ids,
            Long userId,
            int page,
            int limit,
            Page<Long> pageResult
    ) {
        return provider.getByIds(ids, userId)
                .flatMap(content -> responseBuilder.build(content, page, (long) limit, pageResult, provider.newestFirst()));
    }
}
