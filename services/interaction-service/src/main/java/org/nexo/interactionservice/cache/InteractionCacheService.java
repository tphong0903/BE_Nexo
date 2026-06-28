package org.nexo.interactionservice.cache;

import lombok.RequiredArgsConstructor;
import org.nexo.interactionservice.model.CommentModel;
import org.nexo.interactionservice.model.LikeCommentModel;
import org.nexo.interactionservice.model.LikeModel;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class InteractionCacheService {
    private static final Duration VERSION_TTL = Duration.ofDays(7);
    private static final Duration PAGE_TTL = Duration.ofMinutes(10);
    private static final Duration LIKE_PAGE_TTL = Duration.ofMinutes(15);
    private static final Duration LOCK_TTL = Duration.ofSeconds(5);
    private static final long MAX_VERSION = 1_000_000L;

    private final RedisTemplate<String, Object> redisTemplate;

    public long commentsVersion(String ownerType, Long ownerId) {
        return version(CacheKeys.commentsVersion(ownerType, ownerId));
    }

    public long likesVersion(String ownerType, Long ownerId) {
        return version(CacheKeys.likesVersion(ownerType, ownerId));
    }

    public long repliesVersion(Long commentId) {
        return version(CacheKeys.repliesVersion(commentId));
    }

    public void incrementCommentsVersion(String ownerType, Long ownerId) {
        incrementVersion(CacheKeys.commentsVersion(ownerType, ownerId));
    }

    public void incrementLikesVersion(String ownerType, Long ownerId) {
        incrementVersion(CacheKeys.likesVersion(ownerType, ownerId));
    }

    public void incrementRepliesVersion(Long commentId) {
        incrementVersion(CacheKeys.repliesVersion(commentId));
    }

    public void invalidateCommentLists(CommentModel model) {
        if (model.getParentComment() != null) {
            incrementRepliesVersion(model.getParentComment().getId());
            return;
        }
        if (model.getPostId() != null) {
            incrementCommentsVersion("post", model.getPostId());
        }
        if (model.getReelId() != null) {
            incrementCommentsVersion("reel", model.getReelId());
        }
    }

    public void invalidateLikeList(String ownerType, Long ownerId) {
        incrementLikesVersion(ownerType, ownerId);
    }

    @SuppressWarnings("unchecked")
    public CachedPage<Long> getCachedPageIds(String key) {
        return (CachedPage<Long>) redisTemplate.opsForValue().get(key);
    }

    public void putCachedPageIds(String key, CachedPage<Long> page, Duration ttl) {
        redisTemplate.opsForValue().set(key, page, randomized(ttl));
    }

    public CachedPage<Long> getOrLoadPageIds(String key, Duration ttl, Supplier<CachedPage<Long>> loader) {
        CachedPage<Long> cached = getCachedPageIds(key);
        if (cached != null) {
            return cached;
        }

        String lockKey = CacheKeys.lock(key);
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL);
        if (Boolean.TRUE.equals(locked)) {
            try {
                CachedPage<Long> loaded = loader.get();
                putCachedPageIds(key, loaded, ttl);
                return loaded;
            } finally {
                redisTemplate.delete(lockKey);
            }
        }

        sleepBriefly();
        cached = getCachedPageIds(key);
        return cached != null ? cached : loader.get();
    }

    public CachedPage<Long> toCommentIdPage(Page<CommentModel> page) {
        return CachedPage.<Long>builder()
                .content(page.getContent().stream().map(CommentModel::getId).toList())
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    public CachedPage<Long> toLikeUserIdPage(Page<LikeModel> page) {
        return CachedPage.<Long>builder()
                .content(page.getContent().stream().map(LikeModel::getUserId).toList())
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    public CachedPage<Long> toCommentLikeUserIdPage(Page<LikeCommentModel> page) {
        return CachedPage.<Long>builder()
                .content(page.getContent().stream().map(LikeCommentModel::getUserId).toList())
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    public boolean isLiked(String ownerType, Long ownerId, Long userId) {
        Boolean liked = redisTemplate.opsForSet().isMember(CacheKeys.likeMembers(ownerType, ownerId), String.valueOf(userId));
        return Boolean.TRUE.equals(liked);
    }

    public void addLikeStatus(String ownerType, Long ownerId, Long userId) {
        redisTemplate.opsForSet().add(CacheKeys.likeMembers(ownerType, ownerId), String.valueOf(userId));
    }

    public void removeLikeStatus(String ownerType, Long ownerId, Long userId) {
        redisTemplate.opsForSet().remove(CacheKeys.likeMembers(ownerType, ownerId), String.valueOf(userId));
    }

    public void incrementCounter(String ownerType, Long ownerId, String field, long delta) {
        redisTemplate.opsForHash().increment(CacheKeys.counters(ownerType, ownerId), field, delta);
    }

    public void incrementUserCounter(Long userId, String field, long delta) {
        redisTemplate.opsForHash().increment(CacheKeys.userCounters(userId), field, delta);
    }

    public void incrementGlobalCounter(String field, long delta) {
        redisTemplate.opsForHash().increment(CacheKeys.globalCounters(), field, delta);
    }

    public long getGlobalCounterWithFallback(String field, Supplier<Long> fallback) {
        return getHashCounterWithFallback(CacheKeys.globalCounters(), field, fallback);
    }

    public long getUserCounterWithFallback(Long userId, String field, Supplier<Long> fallback) {
        return getHashCounterWithFallback(CacheKeys.userCounters(userId), field, fallback);
    }

    public Duration commentPageTtl() {
        return PAGE_TTL;
    }

    public Duration likePageTtl() {
        return LIKE_PAGE_TTL;
    }

    private long version(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof Number number) {
            redisTemplate.expire(key, VERSION_TTL);
            return number.longValue();
        }
        Boolean initialized = redisTemplate.opsForValue().setIfAbsent(key, 1L, VERSION_TTL);
        return Boolean.TRUE.equals(initialized) ? 1L : version(key);
    }

    private void incrementVersion(String key) {
        Long version = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, VERSION_TTL);
        if (version != null && version > MAX_VERSION) {
            redisTemplate.opsForValue().set(key, 1L, VERSION_TTL);
        }
    }

    private long getHashCounterWithFallback(String key, String field, Supplier<Long> fallback) {
        Object cachedValue = redisTemplate.opsForHash().get(key, field);
        if (cachedValue instanceof Number number) {
            return number.longValue();
        }
        if (cachedValue != null) {
            return Long.parseLong(cachedValue.toString());
        }
        long value = fallback.get();
        redisTemplate.opsForHash().put(key, field, value);
        return value;
    }

    private Duration randomized(Duration ttl) {
        long jitter = ThreadLocalRandom.current().nextLong(0, Math.max(1, ttl.toSeconds() / 10));
        return ttl.plusSeconds(jitter);
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(80L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
