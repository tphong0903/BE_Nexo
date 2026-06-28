package org.nexo.feedservice.service;

import lombok.RequiredArgsConstructor;
import org.nexo.feedservice.exception.RedisFeedException;
import org.nexo.feedservice.util.RedisKeyUtil;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AffinityService {
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    public Mono<Long> getAffinityScore(Long followerId, Long authorId) {
        return reactiveRedisTemplate.opsForHash()
                .get(RedisKeyUtil.affinity(followerId), String.valueOf(authorId))
                .map(value -> Long.parseLong(value.toString()))
                .defaultIfEmpty(0L)
                .onErrorMap(error -> new RedisFeedException("Cannot fetch affinity score", error));
    }
}
