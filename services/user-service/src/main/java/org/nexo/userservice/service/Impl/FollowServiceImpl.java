package org.nexo.userservice.service.Impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.nexo.userservice.dto.FolloweeDTO;
import org.nexo.userservice.dto.PageModelResponse;
import org.nexo.userservice.enums.EStatusFollow;
import org.nexo.userservice.exception.ResourceNotFoundException;
import org.nexo.userservice.model.FollowId;
import org.nexo.userservice.model.FollowModel;
import org.nexo.userservice.model.UserModel;
import org.nexo.userservice.repository.FollowRepository;
import org.nexo.userservice.repository.UserRepository;
import org.nexo.userservice.service.BlockService;
import org.nexo.userservice.service.FollowService;
import org.nexo.userservice.util.JwtUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.AccessDeniedException;
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
        private final BlockService blockService;

        private String followsKey(Long userId) {
                return "follows:" + userId;
        }

        private String followersKey(Long userId) {
                return "followers:" + userId;
        }

        public PageModelResponse<FolloweeDTO> getFollowers(String username, Pageable pageable, String accessToken) {
                UserModel user = userRepository.findActiveByUsername(username)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                String keycloakUserId = jwtUtil.getUserIdFromToken(accessToken);
                Long currentUserId = userRepository.findActiveByKeycloakUserId(keycloakUserId)
                                .map(UserModel::getId)
                                .orElse(null);
                // check block
                if (currentUserId != null) {
                        boolean hasBlockedTarget = blockService.isBlocked(currentUserId, user.getId());
                        boolean isBlockedByTarget = blockService.isBlocked(user.getId(), currentUserId);

                        if (hasBlockedTarget || isBlockedByTarget) {
                                throw new ResourceNotFoundException("User not found with username: " + username);
                        }
                }
                if (user.getIsPrivate()) {
                        boolean isOwner = currentUserId != null && currentUserId.equals(user.getId());
                        boolean isFollowedByUser = false;

                        if (!isOwner && currentUserId != null) {
                                isFollowedByUser = followRepository.existsByFollowerIdAndFollowingIdAndStatus(
                                                currentUserId, user.getId(), EStatusFollow.ACTIVE);

                        }

                        if (!isOwner && !isFollowedByUser) {
                                throw new AccessDeniedException(
                                                "This account is private. You cannot view their followers.");
                        }
                }

                Page<FollowModel> rows = followRepository.findAllByFollowingAndStatus(
                                user, EStatusFollow.ACTIVE, pageable);
                Set<Long> followingIds = new HashSet<>();
                Set<Long> requestedIds = new HashSet<>();

                if (currentUserId != null) {
                        followingIds = followRepository.findAllFollowingIdsByFollowerIdAndStatus(
                                        currentUserId, EStatusFollow.ACTIVE);
                        requestedIds = followRepository.findAllFollowingIdsByFollowerIdAndStatus(
                                        currentUserId, EStatusFollow.PENDING);
                }

                Set<Long> finalFollowingIds = followingIds;
                Set<Long> finalRequestedIds = requestedIds;

                Page<FolloweeDTO> followeeDTOPage = rows.map(f -> FolloweeDTO.builder()
                                .userId(f.getFollower().getId())
                                .userName(f.getFollower().getUsername())
                                .fullName(f.getFollower().getFullName())
                                .avatar(f.getFollower().getAvatar())
                                .isCloseFriend(f.getIsCloseFriend())
                                .isFollowing(finalFollowingIds.contains(f.getFollower().getId()))
                                .hasRequestedFollow(finalRequestedIds.contains(f.getFollower().getId()))
                                .build());

                return PageModelResponse.<FolloweeDTO>builder()
                                .content(followeeDTOPage.getContent())
                                .pageNo(followeeDTOPage.getNumber())
                                .pageSize(followeeDTOPage.getSize())
                                .totalElements(followeeDTOPage.getTotalElements())
                                .totalPages(followeeDTOPage.getTotalPages())
                                .build();
        }

        public PageModelResponse<FolloweeDTO> getFollowings(String username, Pageable pageable, String accessToken) {
                UserModel user = userRepository.findActiveByUsername(username)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                String keycloakUserId = jwtUtil.getUserIdFromToken(accessToken);
                Long currentUserId = userRepository.findActiveByKeycloakUserId(keycloakUserId)
                                .map(UserModel::getId)
                                .orElse(null);
                // check block
                if (currentUserId != null) {
                        boolean hasBlockedTarget = blockService.isBlocked(currentUserId, user.getId());
                        boolean isBlockedByTarget = blockService.isBlocked(user.getId(), currentUserId);

                        if (hasBlockedTarget || isBlockedByTarget) {
                                throw new ResourceNotFoundException("User not found with username: " + username);
                        }
                }
                if (user.getIsPrivate()) {
                        boolean isOwner = currentUserId != null && currentUserId.equals(user.getId());
                        boolean isFollowedByUser = false;

                        if (!isOwner && currentUserId != null) {
                                isFollowedByUser = followRepository.existsByFollowerIdAndFollowingIdAndStatus(
                                                currentUserId, user.getId(), EStatusFollow.ACTIVE);

                        }

                        if (!isOwner && !isFollowedByUser) {
                                throw new AccessDeniedException(
                                                "This account is private. You cannot view their followings.");
                        }
                }
                Page<FollowModel> rows = followRepository.findAllByFollowerId(user.getId(), pageable);
                Set<Long> followingIds = new HashSet<>();
                Set<Long> requestedIds = new HashSet<>();

                if (currentUserId != null) {
                        followingIds = followRepository.findAllFollowingIdsByFollowerIdAndStatus(
                                        currentUserId, EStatusFollow.ACTIVE);
                        requestedIds = followRepository.findAllFollowingIdsByFollowerIdAndStatus(
                                        currentUserId, EStatusFollow.PENDING);
                }
                Set<Long> finalFollowingIds = followingIds;
                Set<Long> finalRequestedIds = requestedIds;
                Page<FolloweeDTO> followeeDTOPage = rows.map(f -> FolloweeDTO.builder()
                                .userId(f.getFollowing().getId())
                                .userName(f.getFollowing().getUsername())
                                .fullName(f.getFollowing().getFullName())
                                .avatar(f.getFollowing().getAvatar())
                                .isCloseFriend(f.getIsCloseFriend())
                                .isFollowing(finalFollowingIds.contains(f.getFollowing().getId()))
                                .hasRequestedFollow(finalRequestedIds.contains(f.getFollowing().getId()))

                                .build());

                return PageModelResponse.<FolloweeDTO>builder()
                                .content(followeeDTOPage.getContent())
                                .pageNo(followeeDTOPage.getNumber())
                                .pageSize(followeeDTOPage.getSize())
                                .totalElements(followeeDTOPage.getTotalElements())
                                .totalPages(followeeDTOPage.getTotalPages())
                                .build();
        }

        public PageModelResponse<FolloweeDTO> getFollowRequests(String accessToken, Pageable pageable) {
                String keycloakUserId = jwtUtil.getUserIdFromToken(accessToken);

                UserModel user = userRepository.findByKeycloakUserId(keycloakUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                Page<FollowModel> rows = followRepository.findAllByFollowingAndStatus(
                                user, EStatusFollow.PENDING, pageable);

                Page<FolloweeDTO> followeeDTOPage = rows.map(f -> FolloweeDTO.builder()
                                .userId(f.getFollower().getId())
                                .userName(f.getFollower().getUsername())
                                .fullName(f.getFollower().getFullName())
                                .avatar(f.getFollower().getAvatar())
                                .isCloseFriend(f.getIsCloseFriend())
                                .isFollowing(false)
                                .build());

                return PageModelResponse.<FolloweeDTO>builder()
                                .content(followeeDTOPage.getContent())
                                .pageNo(followeeDTOPage.getNumber())
                                .pageSize(followeeDTOPage.getSize())
                                .totalElements(followeeDTOPage.getTotalElements())
                                .totalPages(followeeDTOPage.getTotalPages())
                                .build();
        }

        @Transactional
        public void addFollow(String accessToken, String username) {
                String keycloakUserId = jwtUtil.getUserIdFromToken(accessToken);

                UserModel followerUser = userRepository.findActiveByKeycloakUserId(keycloakUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("Follower user not found"));
                UserModel followingUser = userRepository.findActiveByUsername(username)
                                .orElseThrow(() -> new ResourceNotFoundException("Following user not found"));
                Long followerId = followerUser.getId();
                Long followingId = followingUser.getId();
                if (followerId.equals(followingId)) {
                        throw new ResourceNotFoundException("You cannot follow yourself");
                }
                // check block
                if (followerId != null) {
                        boolean hasBlockedTarget = blockService.isBlocked(followerId, followingId);
                        boolean isBlockedByTarget = blockService.isBlocked(followingId, followerId);

                        if (hasBlockedTarget || isBlockedByTarget) {
                                throw new ResourceNotFoundException("User not found with username: " + username);
                        }
                }
                FollowId id = FollowId.builder()
                                .followerId(followerId)
                                .followingId(followingId)
                                .build();

                FollowModel follow = FollowModel.builder()
                                .id(id)
                                .follower(UserModel.builder().id(followerId).build())
                                .following(UserModel.builder().id(followingId).build())
                                .build();

                if (Boolean.TRUE.equals(followingUser.getIsPrivate())) {
                        follow.setStatus(EStatusFollow.PENDING);
                        followRepository.save(follow);
                        return;
                }

                followRepository.save(follow);

                redis.opsForSet().add(followsKey(followerId), followingId.toString());
                redis.opsForSet().add(followersKey(followingId), followerId.toString());
        }

        @Transactional
        public void acceptFollowRequest(String accessToken, String username) {
                String keycloakUserId = jwtUtil.getUserIdFromToken(accessToken);

                UserModel followingUser = userRepository.findActiveByKeycloakUserId(keycloakUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("Following user not found"));
                UserModel followerUser = userRepository.findActiveByUsername(username)
                                .orElseThrow(() -> new ResourceNotFoundException("Follower user not found"));
                FollowId id = FollowId.builder()
                                .followerId(followerUser.getId())
                                .followingId(followingUser.getId())
                                .build();
                FollowModel follow = followRepository.findByIdAndStatus(id, EStatusFollow.PENDING)
                                .orElseThrow(() -> new ResourceNotFoundException("Follow request not found"));
                follow.setStatus(EStatusFollow.ACTIVE);
                followRepository.save(follow);

                redis.opsForSet().add(followsKey(followerUser.getId()), followingUser.getId().toString());
                redis.opsForSet().add(followersKey(followingUser.getId()), followerUser.getId().toString());
        }

        @Transactional
        public void rejectFollowRequest(String accessToken, String username) {
                String keycloakUserId = jwtUtil.getUserIdFromToken(accessToken);

                UserModel followingUser = userRepository.findActiveByKeycloakUserId(keycloakUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("Following user not found"));
                UserModel followerUser = userRepository.findActiveByUsername(username)
                                .orElseThrow(() -> new ResourceNotFoundException("Follower user not found"));
                FollowId id = FollowId.builder()
                                .followerId(followerUser.getId())
                                .followingId(followingUser.getId())
                                .build();
                FollowModel follow = followRepository.findByIdAndStatus(id, EStatusFollow.PENDING)
                                .orElseThrow(() -> new ResourceNotFoundException("Follow request not found"));
                followRepository.delete(follow);
        }

        @Transactional
        public void removeFollow(String accessToken, String username) {
                String keycloakUserId = jwtUtil.getUserIdFromToken(accessToken);

                UserModel followerUser = userRepository.findByKeycloakUserId(keycloakUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("Follower user not found"));

                Long followerId = followerUser.getId();
                UserModel followingUser = userRepository.findActiveByUsername(username)
                                .orElseThrow(() -> new ResourceNotFoundException("Following user not found"));

                Long followingId = followingUser.getId();

                FollowId id = FollowId.builder()
                                .followerId(followerId)
                                .followingId(followingId)
                                .build();
                if (!followRepository.existsById(id)) {
                        throw new ResourceNotFoundException("Follow relationship not found");
                }
                followRepository.deleteById(id);
                redis.opsForSet().remove(followsKey(followerId), followingId.toString());
                redis.opsForSet().remove(followersKey(followingId), followerId.toString());
        }

        @Transactional
        public void toggleCloseFriend(String accessToken, String username) {
                String keycloakUserId = jwtUtil.getUserIdFromToken(accessToken);

                UserModel followerUser = userRepository.findByKeycloakUserId(keycloakUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("Follower user not found"));
                UserModel followingUser = userRepository.findActiveByUsername(username)
                                .orElseThrow(() -> new ResourceNotFoundException("Following user not found"));
                Long followerId = followerUser.getId();
                Long followingId = followingUser.getId();
                FollowId id = FollowId.builder()
                                .followerId(followerId)
                                .followingId(followingId)
                                .build();

                FollowModel follow = followRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Follow relationship not found"));

                follow.setIsCloseFriend(!follow.getIsCloseFriend());

                followRepository.save(follow);

        }

        @Transactional
        public Set<FolloweeDTO> getFollowersByUserId(Long userId) {
                UserModel user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                String key = followsKey(userId);
                Set<String> cached = redis.opsForSet().members(key);

                if (cached != null && !cached.isEmpty()) {
                        List<FollowModel> rows = followRepository.findAllByFollowing(user);
                        return rows.stream()
                                        .map(f -> FolloweeDTO.builder()
                                                        .userId(f.getFollower().getId())
                                                        .userName(f.getFollower().getUsername())
                                                        .avatar(f.getFollower().getAvatar())
                                                        .isCloseFriend(f.getIsCloseFriend())
                                                        .build())
                                        .collect(Collectors.toSet());
                }

                List<FollowModel> rows = followRepository.findAllByFollowing(user);
                Set<FolloweeDTO> followees = rows.stream()
                                .map(f -> FolloweeDTO.builder()
                                                .userId(f.getFollower().getId())
                                                .userName(f.getFollower().getUsername())
                                                .avatar(f.getFollower().getAvatar())
                                                .isCloseFriend(f.getIsCloseFriend())
                                                .build())
                                .collect(Collectors.toSet());

                if (!followees.isEmpty()) {
                        Set<String> userIds = followees.stream()
                                        .map(followee -> followee.getUserId().toString())
                                        .collect(Collectors.toSet());
                        redis.opsForSet().add(key, userIds.toArray(new String[0]));
                }

                return followees;
        }

        @Transactional
        public Set<FolloweeDTO> getFollowingsByUserId(Long userId) {
                UserModel user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                String key = followsKey(user.getId());
                Set<String> cached = redis.opsForSet().members(key);

                if (cached != null && !cached.isEmpty()) {
                        List<FollowModel> rows = followRepository.findAllByFollowing(user);
                        return rows.stream()
                                        .map(f -> FolloweeDTO.builder()
                                                        .userId(f.getFollowing().getId())
                                                        .userName(f.getFollowing().getUsername())
                                                        .avatar(f.getFollowing().getAvatar())
                                                        .isCloseFriend(f.getIsCloseFriend())
                                                        .build())
                                        .collect(Collectors.toSet());
                }

                List<FollowModel> rows = followRepository.findAllByFollowing(user);
                Set<FolloweeDTO> followings = rows.stream()
                                .map(f -> FolloweeDTO.builder()
                                                .userId(f.getFollowing().getId())
                                                .userName(f.getFollowing().getUsername())
                                                .avatar(f.getFollowing().getAvatar())
                                                .isCloseFriend(f.getIsCloseFriend())
                                                .build())
                                .collect(Collectors.toSet());

                if (!followings.isEmpty()) {
                        Set<String> userIds = followings.stream()
                                        .map(followee -> followee.getUserId().toString())
                                        .collect(Collectors.toSet());
                        redis.opsForSet().add(key, userIds.toArray(new String[0]));
                }

                return followings;
        }

        public List<Boolean> isFollow(Long userId1, Long userId2) {
                List<Boolean> list = new ArrayList<>();
                FollowId id1 = FollowId.builder()
                                .followerId(userId1)
                                .followingId(userId2)
                                .build();
                list.add(followRepository.existsById(id1));

                UserModel user = userRepository.findById(userId2).orElse(null);
                list.add(user != null ? user.getIsPrivate() : false);
                return list;
        }

        public PageModelResponse<FolloweeDTO> getCloseFriends(String accessToken, Pageable pageable) {
                String keycloakUserId = jwtUtil.getUserIdFromToken(accessToken);

                UserModel user = userRepository.findByKeycloakUserId(keycloakUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                Page<FollowModel> rows = followRepository.findAllByFollowerAndIsCloseFriendAndStatus(
                                user, true, EStatusFollow.ACTIVE, pageable);

                Page<FolloweeDTO> followeeDTOPage = rows.map(f -> FolloweeDTO.builder()
                                .userId(f.getFollowing().getId())
                                .userName(f.getFollowing().getUsername())
                                .fullName(f.getFollowing().getFullName())
                                .avatar(f.getFollowing().getAvatar())
                                .isCloseFriend(f.getIsCloseFriend())
                                .isFollowing(true)
                                .build());

                return PageModelResponse.<FolloweeDTO>builder()
                                .content(followeeDTOPage.getContent())
                                .pageNo(followeeDTOPage.getNumber())
                                .pageSize(followeeDTOPage.getSize())
                                .totalElements(followeeDTOPage.getTotalElements())
                                .totalPages(followeeDTOPage.getTotalPages())
                                .build();
        }

        public PageModelResponse<FolloweeDTO> getMutualFollowers(String accessToken, Pageable pageable) {
                String keycloakUserId = jwtUtil.getUserIdFromToken(accessToken);

                UserModel user = userRepository.findByKeycloakUserId(keycloakUserId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                Page<FollowModel> rows = followRepository.findMutualFollowers(user.getId(), pageable);

                Page<FolloweeDTO> followeeDTOPage = rows.map(f -> FolloweeDTO.builder()
                                .userId(f.getFollowing().getId())
                                .userName(f.getFollowing().getUsername())
                                .fullName(f.getFollowing().getFullName())
                                .avatar(f.getFollowing().getAvatar())
                                .isCloseFriend(f.getIsCloseFriend())
                                .isFollowing(true)
                                .build());

                return PageModelResponse.<FolloweeDTO>builder()
                                .content(followeeDTOPage.getContent())
                                .pageNo(followeeDTOPage.getNumber())
                                .pageSize(followeeDTOPage.getSize())
                                .totalElements(followeeDTOPage.getTotalElements())
                                .totalPages(followeeDTOPage.getTotalPages())
                                .build();
        }
}
