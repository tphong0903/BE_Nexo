package org.nexo.userservice.service.Impl;

import java.time.LocalDateTime;

import org.nexo.userservice.dto.ChangePasswordRequest;
import org.nexo.userservice.dto.UpdateUserRequest;
import org.nexo.userservice.dto.UserDTOResponse;
import org.nexo.userservice.dto.UserProfileDTOResponse;
import org.nexo.userservice.dto.UserSearchEvent;
import org.nexo.userservice.dto.UserStatisticsResponse;
import org.nexo.userservice.enums.EAccountStatus;
import org.nexo.userservice.enums.ERole;
import org.nexo.userservice.enums.EStatusFollow;
import org.nexo.userservice.exception.ResourceNotFoundException;
import org.nexo.userservice.grpc.AuthGrpcClient;
import org.nexo.userservice.grpc.InteractionGrpcClient;
import org.nexo.userservice.grpc.PostGrpcClient;
import org.nexo.userservice.grpc.UploadFileGrpcClient;
import org.nexo.userservice.mapper.UserMapper;
import org.nexo.userservice.model.UserModel;
import org.nexo.userservice.repository.FollowRepository;
import org.nexo.userservice.repository.UserRepository;
import org.nexo.userservice.service.BlockService;
import org.nexo.userservice.service.UserEventProducer;
import org.nexo.userservice.service.UserService;
import org.nexo.userservice.util.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final FollowRepository followRepository;
    private final UserMapper userMapper;
    private final UploadFileGrpcClient uploadFileGrpcClient;
    private final BlockService blockService;
    private final UserEventProducer userEventProducer;
    private final AuthGrpcClient authGrpcClient;
    private final PostGrpcClient postGrpcClient;
    private final InteractionGrpcClient interactionGrpcClient;

    public UserProfileDTOResponse getUserProfile(String username, String accessToken) {
        UserModel user = userRepository.findByUsernameAndAccountStatus(username, EAccountStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
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

        boolean isFollowing = false;
        boolean hasRequestedFollow = false;
        boolean isCloseFriend = false;
        // check owwner profile
        if (currentUserId != null && !currentUserId.equals(user.getId())) {
            isFollowing = followRepository.existsByFollowerIdAndFollowingIdAndStatus(
                    currentUserId, user.getId(), EStatusFollow.ACTIVE);

            hasRequestedFollow = followRepository.existsByFollowerIdAndFollowingIdAndStatus(
                    currentUserId, user.getId(), EStatusFollow.PENDING);

            isCloseFriend = followRepository.existsByFollowerIdAndFollowingIdAndIsCloseFriendAndStatus(
                    currentUserId, user.getId(), true, EStatusFollow.ACTIVE);
        }
        UserProfileDTOResponse dto = userMapper.toUserProfileDTOResponse(user);
        dto.setIsFollowing(isFollowing);
        dto.setHasRequestedFollow(hasRequestedFollow);
        dto.setIsCloseFriend(isCloseFriend);
        return dto;
    }

    public UserDTOResponse getUserProfileMe(String accessToken) {
        String keycloakUserId = jwtUtil.getUserIdFromToken(accessToken);
        UserModel user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + keycloakUserId));
        return userMapper.toUserDTOResponse(user);
    }

    @Transactional
    public UserDTOResponse updateUser(String accessToken, UpdateUserRequest request, MultipartFile avatarFile) {
        String keycloakUserId = jwtUtil.getUserIdFromToken(accessToken);
        UserModel user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + keycloakUserId));

        if (avatarFile != null && !avatarFile.isEmpty()) {
            log.info("Uploading avatar for user: {}", user.getUsername());
            String avatarUrl = uploadFileGrpcClient.uploadAvatar(avatarFile);
            if (avatarUrl != null) {
                request.setAvatar(avatarUrl);
                log.info("Avatar uploaded successfully: {}", avatarUrl);
            } else {
                log.warn("Failed to upload avatar, keeping existing avatar");
            }
        }

        UserModel updatedUser = userMapper.updateUserModelFromDTO(request, user);
        userRepository.save(updatedUser);

        publishUserEvent(updatedUser, "UPDATE");

        return userMapper.toUserDTOResponse(updatedUser);
    }

    @Transactional
    public void deleteAvatar(String accessToken) {
        String keycloakUserId = jwtUtil.getUserIdFromToken(accessToken);
        UserModel user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + keycloakUserId));

        if (user.isDefaultAvatar()) {
            throw new ResourceNotFoundException("Cannot delete default avatar. Avatar is already set to default.");
        }
        user.setAvatar(UserModel.DEFAULT_AVATAR_URL);
        userRepository.save(user);

        publishUserEvent(user, "UPDATE");

        log.info("Avatar deleted for user: {}, reset to default", user.getUsername());
    }

    private void publishUserEvent(UserModel user, String eventType) {
        UserSearchEvent event = UserSearchEvent.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .avatar(user.getAvatar())
                .eventType(eventType)
                .email(user.getEmail())
                .accountStatus(Enum.valueOf(EAccountStatus.class, user.getAccountStatus().name()).name())
                .role(Enum.valueOf(ERole.class, user.getRole().name()).name())
                .violationCount(user.getViolationCount())
                .build();

        userEventProducer.sendUserEvent(event);
    }

    @Transactional
    public void assignRoleToUser(String username, String role) {
        UserModel user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        boolean success = authGrpcClient.changeUserRole(user.getKeycloakUserId(), role);
        if (!success) {
            throw new ResourceNotFoundException("Failed to change role in auth-service");
        }
        user.setRole(ERole.valueOf(role.toUpperCase()));
        userRepository.save(user);
        publishUserEvent(user, "UPDATE");

    }

    @Transactional
    public void banUser(String username) {
        UserModel user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        boolean success = authGrpcClient.banUser(user.getKeycloakUserId());
        if (!success) {
            throw new ResourceNotFoundException("Failed to ban user in auth-service");
        }
        user.setAccountStatus(EAccountStatus.LOCKED);
        userRepository.save(user);
        publishUserEvent(user, "UPDATE");

    }

    @Transactional
    public void unbanUser(String username) {
        UserModel user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        boolean success = authGrpcClient.unBanUser(user.getKeycloakUserId());
        if (!success) {
            throw new ResourceNotFoundException("Failed to unban user in auth-service");
        }
        user.setAccountStatus(EAccountStatus.ACTIVE);
        userRepository.save(user);
        publishUserEvent(user, "UPDATE");

    }

    @Transactional
    public void updateUserOauth(String keycloakUserId, UpdateUserRequest request) {
        UserModel user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + keycloakUserId));

        UserModel updatedUser = userMapper.updateUserModelFromDTO(request, user);
        updatedUser.setAccountStatus(EAccountStatus.ACTIVE);
        userRepository.save(updatedUser);

        publishUserEvent(updatedUser, "UPDATE");
    }

    @Transactional
    public void changePassword(String accessToken, ChangePasswordRequest request) {
        String keycloakUserId = jwtUtil.getUserIdFromToken(accessToken);
        UserModel user = userRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + keycloakUserId));
        if (user.getAccountStatus() != EAccountStatus.ACTIVE) {
            throw new ResourceNotFoundException("Cannot change password for inactive user");
        }
        authGrpcClient.changePassword(keycloakUserId, request.getOldPassword(), request.getNewPassword());
    }

    @Override
    public UserStatisticsResponse getUserStatistics(Long userId) {
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        Long postsCount = postGrpcClient.getUserPostsCount(userId);
        Long interactionsCount = interactionGrpcClient.getUserInteractionsCount(userId);
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        Long newFollowersCount = followRepository.countNewFollowersByUserIdAndDateRange(userId, thirtyDaysAgo);
        Long totalFollowersCount = followRepository.countTotalFollowersByUserId(userId);
        Long totalFollowingCount = followRepository.countFollowingByUserId(userId);

        return UserStatisticsResponse.builder()
                .userId(userId)
                .username(user.getUsername())
                .postsCount(postsCount != null ? postsCount : 0L)
                .interactionsCount(interactionsCount != null ? interactionsCount : 0L)
                .newFollowersCount(newFollowersCount != null ? newFollowersCount : 0L)
                .totalFollowersCount(totalFollowersCount != null ? totalFollowersCount : 0L)
                .totalFollowingCount(totalFollowingCount != null ? totalFollowingCount : 0L)
                .build();
    }

}
