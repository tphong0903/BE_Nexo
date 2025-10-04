package org.nexo.interactionservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LeakyBucketService {

    private final RedisTemplate<String, Object> redisTemplate;

    public boolean allowRequest(String key, long bucketCapacity, long leakRateMillis) {
        String countKey = "bucket:" + key + ":count";
        String timeKey = "bucket:" + key + ":time";

        Long currentTime = System.currentTimeMillis();
        Long lastTime = (Long) redisTemplate.opsForValue().get(timeKey);
        Long currentCount = (Long) redisTemplate.opsForValue().get(countKey);

        if (currentCount == null) currentCount = 0L;
        if (lastTime == null) lastTime = currentTime;

        long deltaTime = currentTime - lastTime;
        long leaked = deltaTime / leakRateMillis;

        long newCount = Math.max(currentCount - leaked, 0);

        if (newCount < bucketCapacity) {
            redisTemplate.opsForValue().set(countKey, newCount + 1);
            redisTemplate.opsForValue().set(timeKey, currentTime);
            return true;
        } else {
            redisTemplate.opsForValue().set(countKey, newCount);
            redisTemplate.opsForValue().set(timeKey, currentTime);
            return false;
        }
    }
}
