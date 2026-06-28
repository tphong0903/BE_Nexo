package org.nexo.feedservice.service;

import lombok.extern.slf4j.Slf4j;
import org.nexo.feedservice.config.FeedProperties;
import org.nexo.feedservice.dto.FeedItem;
import org.nexo.feedservice.exception.RedisFeedException;
import org.nexo.feedservice.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class FeedRedisService {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final FeedProperties feedProperties;
    private final RedisScript<Long> pushFeedItemScript;

    public FeedRedisService(
            ReactiveRedisTemplate<String, String> redisTemplate,
            FeedProperties feedProperties,
            @Value("classpath:lua/push-feed-item.lua") Resource scriptResource
    ) {
        this.redisTemplate = redisTemplate;
        this.feedProperties = feedProperties;
        this.pushFeedItemScript = RedisScript.of(readScript(scriptResource), Long.class);
    }

    public Mono<Void> push(FeedContentType type, Long followerId, Long itemId, Long score) {
        return push(RedisKeyUtil.personalFeed(type, followerId), itemId, score);
    }

    public Mono<Void> pushToOutbox(FeedContentType type, Long authorId, Long itemId, Long score) {
        return push(RedisKeyUtil.userOutbox(type, authorId), itemId, score);
    }

    public Mono<Void> push(String key, Long itemId, Long score) {
        return redisTemplate.execute(
                        pushFeedItemScript,
                        List.of(key),
                        List.of(score.toString(), itemId.toString(), String.valueOf(feedProperties.getMaxRedisFeedSize()))
                )
                .then()
                .transform(mono -> logLatency(mono, "redis.push", key, 1))
                .onErrorMap(error -> new RedisFeedException("Cannot push item to Redis feed key " + key, error));
    }

    public Mono<List<FeedItem>> fetch(String key, long start, long end) {
        return redisTemplate.opsForZSet()
                .reverseRangeWithScores(key, Range.closed(start, end))
                .map(tuple -> new FeedItem(
                        Long.parseLong(tuple.getValue()),
                        tuple.getScore() != null ? tuple.getScore().longValue() : 0L
                ))
                .collectList()
                .transform(mono -> logLatency(mono, "redis.fetch", key, (int) (end - start + 1)))
                .onErrorMap(error -> new RedisFeedException("Cannot fetch Redis feed key " + key, error))
                .onErrorResume(RedisFeedException.class, error -> {
                    log.error("Redis feed fetch failed, using empty result. key={}", key, error);
                    return Mono.just(new ArrayList<>());
                });
    }

    public Mono<List<List<FeedItem>>> fetchOutboxes(FeedContentType type, List<Long> authorIds, long start, long end) {
        return Flux.fromIterable(authorIds)
                .flatMap(authorId -> fetch(RedisKeyUtil.userOutbox(type, authorId), start, end),
                        feedProperties.getRedisBatchConcurrency())
                .collectList();
    }

    public Mono<List<FeedItem>> fetchPersonalFeed(FeedContentType type, Long userId, long start, long end) {
        return fetch(RedisKeyUtil.personalFeed(type, userId), start, end);
    }

    public Mono<List<FeedItem>> fetchTrending(FeedContentType type, long start, long end) {
        return fetch(RedisKeyUtil.trending(type), start, end);
    }

    private String readScript(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new RedisFeedException("Cannot load Redis Lua script", error);
        }
    }

    private <T> Mono<T> logLatency(Mono<T> mono, String operation, String key, int size) {
        return Mono.defer(() -> {
            long start = System.nanoTime();
            return mono.doFinally(signal -> log.debug("{} key={} size={} latencyMs={}",
                    operation, key, size, (System.nanoTime() - start) / 1_000_000));
        });
    }
}
