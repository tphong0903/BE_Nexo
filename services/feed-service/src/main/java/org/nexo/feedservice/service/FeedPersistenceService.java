package org.nexo.feedservice.service;

import lombok.extern.slf4j.Slf4j;
import org.nexo.feedservice.exception.DatabaseFeedException;
import org.nexo.feedservice.exception.FeedProcessingException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class FeedPersistenceService {
    private final Map<FeedContentType, FeedPersistenceStrategy> strategies;

    public FeedPersistenceService(List<FeedPersistenceStrategy> strategies) {
        this.strategies = new EnumMap<>(FeedContentType.class);
        for (FeedPersistenceStrategy strategy : strategies) {
            this.strategies.put(strategy.type(), strategy);
        }
    }

    public Mono<Void> saveFanOut(FeedContentType type, Long authorId, Long itemId, List<Long> recipientIds) {
        return Mono.fromRunnable(() -> strategy(type).saveFanOut(authorId, itemId, recipientIds))
                .subscribeOn(Schedulers.boundedElastic())
                .transform(mono -> logLatency(mono, "db.saveFanOut", type, recipientIds.size()))
                .onErrorMap(error -> new DatabaseFeedException("Cannot save feed fan-out", error))
                .then();
    }

    public Mono<Page<Long>> findItemIdsByFollowerId(FeedContentType type, Long followerId, int page, int limit) {
        return Mono.fromCallable(() -> strategy(type).findItemIdsByFollowerId(followerId, PageRequest.of(page, limit)))
                .subscribeOn(Schedulers.boundedElastic())
                .transform(mono -> logLatency(mono, "db.findFeedIds", type, limit))
                .onErrorMap(error -> new DatabaseFeedException("Cannot query feed ids", error));
    }

    private FeedPersistenceStrategy strategy(FeedContentType type) {
        FeedPersistenceStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new FeedProcessingException("Missing persistence strategy for " + type);
        }
        return strategy;
    }

    private <T> Mono<T> logLatency(Mono<T> mono, String operation, FeedContentType type, int size) {
        return Mono.defer(() -> {
            long start = System.nanoTime();
            return mono.doFinally(signal -> log.debug("{} type={} size={} latencyMs={}",
                    operation, type, size, (System.nanoTime() - start) / 1_000_000));
        });
    }
}
