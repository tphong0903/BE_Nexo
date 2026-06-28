package org.nexo.feedservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.feedservice.config.FeedProperties;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedFanOutService {
    private final FeedProperties feedProperties;
    private final UserFeedGraphService userFeedGraphService;
    private final FeedPersistenceService feedPersistenceService;
    private final FeedRankingService feedRankingService;
    private final FeedRedisService feedRedisService;

    public Mono<Void> handleNewItem(FeedContentType type, Long authorId, Long itemId, Long createdAt) {
        return userFeedGraphService.countFollowerOfUser(authorId)
                .flatMap(followerCount -> followerCount >= feedProperties.getKolFollowerThreshold()
                        ? pushToAuthorOutbox(type, authorId, itemId, createdAt)
                        : fanOutToFollowers(type, authorId, itemId, createdAt));
    }

    public Mono<Void> fanOutToFollowers(FeedContentType type, Long authorId, Long itemId, Long createdAt) {
        return userFeedGraphService.getFanOutRecipientIds(authorId)
                .flatMap(recipientIds -> feedPersistenceService.saveFanOut(type, authorId, itemId, recipientIds)
                        .thenMany(Flux.fromIterable(recipientIds))
                        .flatMap(recipientId -> feedRankingService.scoreFor(recipientId, authorId, createdAt)
                                        .flatMap(score -> feedRedisService.push(type, recipientId, itemId, score)),
                                feedProperties.getRedisBatchConcurrency())
                        .then()
                        .doOnSuccess(ignored -> log.info("Fan-out completed type={} authorId={} itemId={} recipients={}",
                                type, authorId, itemId, recipientIds.size())));
    }

    private Mono<Void> pushToAuthorOutbox(FeedContentType type, Long authorId, Long itemId, Long createdAt) {
        return feedRedisService.pushToOutbox(type, authorId, itemId, createdAt)
                .doOnSuccess(ignored -> log.info("Pull mode saved to author outbox type={} authorId={} itemId={}",
                        type, authorId, itemId));
    }
}
