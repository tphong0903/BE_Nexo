package org.nexo.userservice.service.Impl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.nexo.userservice.model.FollowId;
import org.nexo.userservice.model.FollowModel;
import org.nexo.userservice.model.UserModel;
import org.nexo.userservice.repository.FollowRepository;
import org.nexo.userservice.service.FollowService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {
        private final FollowRepository followRepository;
        private final StringRedisTemplate redis;

        private String followsKey(Long userId) {
                return "follows:" + userId;
        }

        private String followersKey(Long userId) {
                return "followers:" + userId;
        }

        public Set<String> getFollowees(Long userId) {
                String key = followsKey(userId);
                Set<String> cached = redis.opsForSet().members(key);

                if (cached != null && !cached.isEmpty()) {
                        return cached;
                }

                List<FollowModel> rows = followRepository.findAllByFollower(UserModel.builder().id(userId).build());
                Set<String> followees = rows.stream()
                                .map(f -> f.getFollowing().getId().toString())
                                .collect(Collectors.toSet());

                if (!followees.isEmpty()) {
                        redis.opsForSet().add(key, followees.toArray(new String[0]));
                }

                return followees;
        }

        @Transactional
        public void addFollow(Long followerId, Long followingId, boolean isCloseFriend) {
                FollowId id = FollowId.builder()
                                .followerId(followerId)
                                .followingId(followingId)
                                .build();
                FollowModel follow = FollowModel.builder()
                                .id(id)
                                .follower(UserModel.builder().id(followerId).build())
                                .following(UserModel.builder().id(followingId).build())
                                .isCloseFriend(isCloseFriend)
                                .createdAt(OffsetDateTime.now())
                                .build();

                followRepository.save(follow);

                redis.opsForSet().add(followsKey(followerId), followingId.toString());
                redis.opsForSet().add(followersKey(followingId), followerId.toString());
        }

        @Transactional
        public void removeFollow(Long followerId, Long followingId) {
                FollowId id = FollowId.builder()
                                .followerId(followerId)
                                .followingId(followingId)
                                .build();
                followRepository.deleteById(id);
                redis.opsForSet().remove(followsKey(followerId), followingId.toString());
                redis.opsForSet().remove(followersKey(followingId), followerId.toString());
        }
}
