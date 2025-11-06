package org.nexo.messagingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.messagingservice.enums.EConversationStatus;
import org.nexo.messagingservice.grpc.UserGrpcClient;
import org.nexo.messagingservice.model.ConversationModel;
import org.nexo.messagingservice.repository.ConversationRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PresenceService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserGrpcClient userGrpcClient;
    private final ConversationRepository conversationRepository;

    private static final String ONLINE_USERS_KEY = "online_users";
    private static final String USER_LAST_SEEN_PREFIX = "user:last_seen:";
    private static final String USER_ONLINE_STATUS_ENABLED_PREFIX = "user:status_enabled:";
    private static final long ONLINE_TIMEOUT_MINUTES = 5;
    private static final long CACHE_STATUS_ENABLED_MINUTES = 10;

    public void setUserOnline(Long userId) {
        if (!isActivityStatusEnabled(userId)) {
            log.info("User {} has activity status disabled, not marking as online", userId);
            return;
        }
        redisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId.toString());

        String userKey = "user:online:" + userId;
        redisTemplate.opsForValue().set(userKey, "1", ONLINE_TIMEOUT_MINUTES, TimeUnit.MINUTES);

        updateLastSeen(userId);

        log.info("User {} is now online", userId);
    }

    public void setUserOffline(Long userId) {
        redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId.toString());

        String userKey = "user:online:" + userId;
        redisTemplate.delete(userKey);

        updateLastSeen(userId);

        log.info("User {} is now offline", userId);
    }

    public boolean canSeeUserOnline(Long requestingUserId, Long targetUserId) {
        if (!isActivityStatusEnabled(requestingUserId)) {
            return false;
        }

        if (!isActivityStatusEnabled(targetUserId)) {
            return false;
        }

        if (!hasAcceptedConversation(requestingUserId, targetUserId)) {
            return false;
        }

        String userKey = "user:online:" + targetUserId;
        boolean isOnline = Boolean.TRUE.equals(redisTemplate.hasKey(userKey));

        return isOnline;
    }

    private boolean isActivityStatusEnabled(Long userId) {
        String cacheKey = USER_ONLINE_STATUS_ENABLED_PREFIX + userId;

        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return Boolean.parseBoolean(cached.toString());
        }

        UserServiceProto.GetUserOnlineStatusResponse response = userGrpcClient.isUserOnline(userId);

        boolean isOnlineStatus = response.getOnlineStatus();

        redisTemplate.opsForValue().set(
                cacheKey,
                String.valueOf(isOnlineStatus),
                CACHE_STATUS_ENABLED_MINUTES,
                TimeUnit.MINUTES);

        return isOnlineStatus;
    }

    private boolean hasAcceptedConversation(Long userId1, Long userId2) {
        Optional<ConversationModel> conversation = conversationRepository
                .findDirectConversationBetweenUsers(userId1, userId2);

        return conversation.isPresent() &&
                conversation.get().getStatus() == EConversationStatus.NORMAL;
    }

    public void refreshUserPresence(Long userId) {
        if (!isActivityStatusEnabled(userId)) {
            return;
        }

        String userKey = "user:online:" + userId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(userKey))) {
            redisTemplate.expire(userKey, ONLINE_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            updateLastSeen(userId);
        }
    }

    public Map<Long, Boolean> getUsersOnlineStatus(Long requestingUserId, List<Long> targetUserIds) {
        Map<Long, Boolean> statusMap = new HashMap<>();

        if (!isActivityStatusEnabled(requestingUserId)) {
            for (Long userId : targetUserIds) {
                statusMap.put(userId, false);
            }
            return statusMap;
        }

        for (Long targetUserId : targetUserIds) {
            boolean canSee = canSeeUserOnline(requestingUserId, targetUserId);
            statusMap.put(targetUserId, canSee);
        }

        return statusMap;
    }

    public List<Long> getOnlineMutualFriends(Long requestingUserId) {
        if (!isActivityStatusEnabled(requestingUserId)) {
            return Collections.emptyList();
        }

        // Lấy danh sách user có conversation NORMAL với requesting user
        // TODO: Có thể cần thêm method trong ConversationRepository để lấy danh sách
        // này
        UserServiceProto.GetMutualFriendsResponse response = userGrpcClient.getMutualFriends(requestingUserId);

        List<Long> mutualFriendIds = response.getUserIdsList();

        return mutualFriendIds.stream()
                .filter(friendId -> canSeeUserOnline(requestingUserId, friendId))
                .collect(Collectors.toList());
    }

    private void updateLastSeen(Long userId) {
        String key = USER_LAST_SEEN_PREFIX + userId;
        redisTemplate.opsForValue().set(key, LocalDateTime.now().toString());
    }

    public String getLastSeen(Long requestingUserId, Long targetUserId) {
        if (!isActivityStatusEnabled(requestingUserId)) {
            return null;
        }
        if (!hasAcceptedConversation(requestingUserId, targetUserId)) {
            return null;
        }

        String onlineKey = "user:online:" + targetUserId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(onlineKey))) {
            return null; 
        }
        if (!isActivityStatusEnabled(targetUserId)) {
            return null;
        }

        String key = USER_LAST_SEEN_PREFIX + targetUserId;
        Object lastSeen = redisTemplate.opsForValue().get(key);
        return lastSeen != null ? lastSeen.toString() : null;
    }

    public void clearActivityStatusCache(Long userId) {
        String cacheKey = USER_ONLINE_STATUS_ENABLED_PREFIX + userId;
        redisTemplate.delete(cacheKey);
        log.info("Cleared activity status cache for user {}", userId);
    }

    public Long getOnlineUsersCount() {
        return redisTemplate.opsForSet().size(ONLINE_USERS_KEY);
    }
}