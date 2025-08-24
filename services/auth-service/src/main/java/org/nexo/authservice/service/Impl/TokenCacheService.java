package org.nexo.authservice.service.Impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.Duration;

@Service
public class TokenCacheService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public TokenCacheService(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static final String TOKEN_PREFIX = "token:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    public Mono<Void> cacheToken(String userId, String accessToken, String refreshToken, Integer expiresIn) {
        String tokenKey = TOKEN_PREFIX + userId;
        String refreshKey = REFRESH_TOKEN_PREFIX + userId;

        Duration accessTokenTtl = Duration.ofSeconds(expiresIn - 60);
        Duration refreshTokenTtl = Duration.ofMinutes(30);

        return redisTemplate.opsForValue()
                .set(tokenKey, accessToken, accessTokenTtl)
                .then(redisTemplate.opsForValue().set(refreshKey, refreshToken, refreshTokenTtl))
                .then();
    }

    public Mono<String> getToken(String userId) {
        return redisTemplate.opsForValue().get(TOKEN_PREFIX + userId);
    }

    public Mono<String> getRefreshToken(String userId) {
        return redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);
    }

    public Mono<Void> removeTokens(String userId) {
        String tokenKey = TOKEN_PREFIX + userId;
        String refreshKey = REFRESH_TOKEN_PREFIX + userId;

        return redisTemplate.delete(tokenKey, refreshKey).then();
    }

    public Mono<Boolean> hasValidToken(String userId) {
        return redisTemplate.hasKey(TOKEN_PREFIX + userId);
    }
}
