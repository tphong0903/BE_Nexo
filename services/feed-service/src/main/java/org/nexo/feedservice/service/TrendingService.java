package org.nexo.feedservice.service;

import lombok.RequiredArgsConstructor;
import org.nexo.feedservice.dto.FeedItem;
import org.nexo.feedservice.exception.TrendingFeedException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrendingService {
    private final FeedRedisService feedRedisService;
    private final FeedMergeService feedMergeService;

    public Mono<List<Long>> getTrendingIds(FeedContentType type, long start, long end) {
        return feedRedisService.fetchTrending(type, start, end)
                .map(feedMergeService::toItemIds)
                .onErrorMap(error -> new TrendingFeedException("Cannot query trending feed", error));
    }

    public Mono<List<FeedItem>> getTrendingItems(FeedContentType type, long start, long end) {
        return feedRedisService.fetchTrending(type, start, end)
                .onErrorMap(error -> new TrendingFeedException("Cannot query trending feed items", error));
    }
}
