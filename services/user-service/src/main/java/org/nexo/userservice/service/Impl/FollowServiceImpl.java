package org.nexo.userservice.service.Impl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.nexo.userservice.dto.FolloweeDTO;
import org.nexo.userservice.model.FollowId;
import org.nexo.userservice.model.FollowModel;
import org.nexo.userservice.model.UserModel;
import org.nexo.userservice.repository.FollowRepository;
import org.nexo.userservice.repository.UserRepository;
import org.nexo.userservice.service.FollowService;
import org.nexo.userservice.util.JwtUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {
        private final FollowRepository followRepository;
        private final UserRepository userRepository;
        private final StringRedisTemplate redis;
        private final JwtUtil jwtUtil;

        private String followsKey(Long userId) {
                return "follows:" + userId;
        }

        private String followersKey(Long userId) {
                return "followers:" + userId;
        }

        public Set<FolloweeDTO> getFollowees(String accessToken) {
                String keycloakUserId = jwtUtil.getUserIdFromToken(accessToken);

                UserModel user = userRepository.findByKeycloakUserId(keycloakUserId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                String key = followsKey(user.getId());
                Set<String> cached = redis.opsForSet().members(key);

                if (cached != null && !cached.isEmpty()) {
                        List<FollowModel> rows = followRepository.findAllByFollower(user);
                        return rows.stream()
                                        .map(f -> FolloweeDTO.builder()
                                                        .userId(f.getFollowing().getId().toString())
                                                        .userName(f.getFollowing().getUsername())
                                                        .avatar(f.getFollowing().getAvatar())
                                                        .isCloseFriend(f.getIsCloseFriend())
                                                        .build())
                                        .collect(Collectors.toSet());
                }

                List<FollowModel> rows = followRepository.findAllByFollower(user);
                Set<FolloweeDTO> followees = rows.stream()
                                .map(f -> FolloweeDTO.builder()
                                                .userId(f.getFollowing().getId().toString())
                                                .userName(f.getFollowing().getUsername())
                                                .avatar(f.getFollowing().getAvatar())
                                                .isCloseFriend(f.getIsCloseFriend())
                                                .build())
                                .collect(Collectors.toSet());

                if (!followees.isEmpty()) {
                        Set<String> userIds = followees.stream()
                                        .map(FolloweeDTO::getUserId)
                                        .collect(Collectors.toSet());
                        redis.opsForSet().add(key, userIds.toArray(new String[0]));
                }

                return followees;
        }

        @Transactional
        public void addFollow(String accessToken, Long followingId) {
                String keycloakUserId = jwtUtil.getUserIdFromToken(accessToken);

                UserModel followerUser = userRepository.findByKeycloakUserId(keycloakUserId)
                                .orElseThrow(() -> new RuntimeException("Follower user not found"));

                Long followerId = followerUser.getId();

                FollowId id = FollowId.builder()
                                .followerId(followerId)
                                .followingId(followingId)
                                .build();
                FollowModel follow = FollowModel.builder()
                                .id(id)
                                .follower(UserModel.builder().id(followerId).build())
                                .following(UserModel.builder().id(followingId).build())
                                .build();

                followRepository.save(follow);

                redis.opsForSet().add(followsKey(followerId), followingId.toString());
                redis.opsForSet().add(followersKey(followingId), followerId.toString());
        }

        @Transactional
        public void removeFollow(String accessToken, Long followingId) {
                String keycloakUserId = jwtUtil.getUserIdFromToken(accessToken);

                UserModel followerUser = userRepository.findByKeycloakUserId(keycloakUserId)
                                .orElseThrow(() -> new RuntimeException("Follower user not found"));

                Long followerId = followerUser.getId();

                FollowId id = FollowId.builder()
                                .followerId(followerId)
                                .followingId(followingId)
                                .build();
                followRepository.deleteById(id);
                redis.opsForSet().remove(followsKey(followerId), followingId.toString());
                redis.opsForSet().remove(followersKey(followingId), followerId.toString());
        }

        @Transactional
        public void toggleCloseFriend(String accessToken, Long followingId) {
                String keycloakUserId = jwtUtil.getUserIdFromToken(accessToken);

                UserModel followerUser = userRepository.findByKeycloakUserId(keycloakUserId)
                                .orElseThrow(() -> new RuntimeException("Follower user not found"));

                Long followerId = followerUser.getId();

                FollowId id = FollowId.builder()
                                .followerId(followerId)
                                .followingId(followingId)
                                .build();

                FollowModel follow = followRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Follow relationship not found"));

                follow.setIsCloseFriend(!follow.getIsCloseFriend());

                followRepository.save(follow);

        }

        public Set<FolloweeDTO> getFolloweesByUserId(Long userId) {
                UserModel user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                String key = followsKey(userId);
                Set<String> cached = redis.opsForSet().members(key);

                if (cached != null && !cached.isEmpty()) {
                        List<FollowModel> rows = followRepository.findAllByFollower(user);
                        return rows.stream()
                                        .map(f -> FolloweeDTO.builder()
                                                        .userId(f.getFollowing().getId().toString())
                                                        .userName(f.getFollowing().getUsername())
                                                        .avatar(f.getFollowing().getAvatar())
                                                        .isCloseFriend(f.getIsCloseFriend())
                                                        .build())
                                        .collect(Collectors.toSet());
                }

                List<FollowModel> rows = followRepository.findAllByFollower(user);
                Set<FolloweeDTO> followees = rows.stream()
                                .map(f -> FolloweeDTO.builder()
                                                .userId(f.getFollowing().getId().toString())
                                                .userName(f.getFollowing().getUsername())
                                                .avatar(f.getFollowing().getAvatar())
                                                .isCloseFriend(f.getIsCloseFriend())
                                                .build())
                                .collect(Collectors.toSet());

                if (!followees.isEmpty()) {
                        Set<String> userIds = followees.stream()
                                        .map(FolloweeDTO::getUserId)
                                        .collect(Collectors.toSet());
                        redis.opsForSet().add(key, userIds.toArray(new String[0]));
                }

                return followees;
        }
}
