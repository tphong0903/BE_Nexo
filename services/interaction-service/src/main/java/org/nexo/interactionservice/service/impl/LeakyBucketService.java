package org.nexo.interactionservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeakyBucketService {

    private static final String LEAKY_BUCKET_SCRIPT = """
                local countKey = KEYS[1]
                local timeKey = KEYS[2]
                local capacity = tonumber(ARGV[1])
                local leakRate = tonumber(ARGV[2])
                local now = tonumber(ARGV[3])

                local lastTime = tonumber(redis.call('GET', timeKey) or now)
                local currentCount = tonumber(redis.call('GET', countKey) or '0')

                local deltaTime = math.max(now - lastTime, 0)
                local leaked = math.floor(deltaTime / leakRate)
                local newCount = math.max(currentCount - leaked, 0)
                
                local ttl = math.ceil((capacity * leakRate) / 1000) + 5

                if newCount < capacity then
                    redis.call('SET', countKey, newCount + 1, 'EX', ttl)
                    redis.call('SET', timeKey, now, 'EX', ttl)
                    return 1
                else
                    redis.call('SET', countKey, newCount, 'EX', ttl)
                    redis.call('SET', timeKey, now, 'EX', ttl)
                    return 0
                end
            """;
    private final RedisTemplate<String, Object> redisTemplate;

    public boolean allowRequest(String key, long bucketCapacity, long leakRateMillis) {
        String countKey = "bucket:" + key + ":count";
        String timeKey = "bucket:" + key + ":time";
        long currentTime = System.currentTimeMillis();

        Long result = redisTemplate.execute(
                RedisScript.of(LEAKY_BUCKET_SCRIPT, Long.class),
                List.of(countKey, timeKey),
                String.valueOf(bucketCapacity),
                String.valueOf(leakRateMillis),
                String.valueOf(currentTime)
        );

        return result != null && result == 1L;
    }
}