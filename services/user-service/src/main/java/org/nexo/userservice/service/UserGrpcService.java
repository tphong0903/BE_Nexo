package org.nexo.userservice.service;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.nexo.grpc.user.UserServiceGrpc;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.userservice.dto.FolloweeDTO;
import org.nexo.userservice.dto.UserSearchEvent;
import org.nexo.userservice.enums.EAccountStatus;
import org.nexo.userservice.enums.EStatusFollow;
import org.nexo.userservice.exception.ResourceNotFoundException;
import org.nexo.userservice.model.FollowId;
import org.nexo.userservice.model.FollowModel;
import org.nexo.userservice.model.UserModel;
import org.nexo.userservice.repository.FollowRepository;
import org.nexo.userservice.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

        private final UserRepository userRepository;
        private final FollowService followService;
        private final FollowRepository followRepository;
        private final UserEventProducer userEventProducer;
        private final BlockService blockService;

        @Override
        public void createUser(UserServiceProto.CreateUserRequest request,
                        StreamObserver<UserServiceProto.CreateUserResponse> responseObserver) {
                log.info("Received gRPC request to create user: email={}, keycloakUserId={}",
                                request.getEmail(), request.getKeycloakUserId());

                if (userRepository.existsByEmail(request.getEmail())) {
                        UserServiceProto.CreateUserResponse response = UserServiceProto.CreateUserResponse
                                        .newBuilder()
                                        .setSuccess(false)
                                        .setMessage("User with email " + request.getEmail() + " already exists")
                                        .setUserId(0)
                                        .build();
                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                        return;
                }

                if (userRepository.existsByKeycloakUserId(request.getKeycloakUserId())) {
                        UserServiceProto.CreateUserResponse response = UserServiceProto.CreateUserResponse
                                        .newBuilder()
                                        .setSuccess(false)
                                        .setMessage("User with keycloak ID " + request.getKeycloakUserId()
                                                        + " already exists")
                                        .setUserId(0)
                                        .build();
                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                        return;
                }

                UserModel user = UserModel.builder()
                                .keycloakUserId(request.getKeycloakUserId())
                                .email(request.getEmail())
                                .username(request.getUsername())
                                .fullName(request.getFullName())
                                .accountStatus(EAccountStatus.valueOf(request.getAccountStatus()))
                                .build();

                UserModel savedUser = userRepository.save(user);

                UserServiceProto.CreateUserResponse response = UserServiceProto.CreateUserResponse.newBuilder()
                                .setSuccess(true)
                                .setMessage("User created successfully")
                                .setUserId(savedUser.getId())
                                .build();

                log.info("User created successfully: id={}, email={}, keycloakUserId={}",
                                savedUser.getId(), savedUser.getEmail(), savedUser.getKeycloakUserId());
                publishUserEvent(savedUser, "CREATE");

                responseObserver.onNext(response);
                responseObserver.onCompleted();
        }

        private void publishUserEvent(UserModel user, String eventType) {
                UserSearchEvent event = UserSearchEvent.builder()
                                .id(user.getId())
                                .username(user.getUsername())
                                .fullName(user.getFullName())
                                .avatar(user.getAvatar())
                                .email(user.getEmail())
                                .eventType(eventType)
                                .build();

                userEventProducer.sendUserEvent(event);
        }

        @Override
        public void updateUserEmailVerification(UserServiceProto.UpdateEmailVerificationRequest request,
                        StreamObserver<UserServiceProto.UpdateEmailVerificationResponse> responseObserver) {
                try {
                        UserModel user = userRepository.findByKeycloakUserId(request.getKeycloakUserId())
                                        .orElse(null);

                        if (user == null) {
                                UserServiceProto.UpdateEmailVerificationResponse response = UserServiceProto.UpdateEmailVerificationResponse
                                                .newBuilder()
                                                .setSuccess(false)
                                                .setMessage("User not found with keycloak ID: "
                                                                + request.getKeycloakUserId())
                                                .build();
                                responseObserver.onNext(response);
                                responseObserver.onCompleted();
                                return;
                        }

                        if (request.getEmailVerified()) {
                                user.setAccountStatus(EAccountStatus.ACTIVE);
                                log.info("User account activated for keycloakUserId: {}", request.getKeycloakUserId());
                        } else {
                                user.setAccountStatus(EAccountStatus.PENDING);
                        }

                        UserModel updatedUser = userRepository.save(user);

                        publishUserEvent(updatedUser, "UPDATE");

                        UserServiceProto.UpdateEmailVerificationResponse response = UserServiceProto.UpdateEmailVerificationResponse
                                        .newBuilder()
                                        .setSuccess(true)
                                        .setMessage("Email verification status updated successfully. Account status: " +
                                                        updatedUser.getAccountStatus())
                                        .build();

                        log.info("Email verification updated successfully: keycloakUserId={}, accountStatus={}",
                                        updatedUser.getKeycloakUserId(), updatedUser.getAccountStatus());

                        responseObserver.onNext(response);
                        responseObserver.onCompleted();

                } catch (Exception e) {
                        log.error("Error updating email verification: {}", e.getMessage(), e);

                        UserServiceProto.UpdateEmailVerificationResponse response = UserServiceProto.UpdateEmailVerificationResponse
                                        .newBuilder()
                                        .setSuccess(false)
                                        .setMessage("Error updating email verification: " + e.getMessage())
                                        .build();

                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                }
        }

        @Override
        public void updateAccountStatus(UserServiceProto.UpdateAccountStatusRequest request,
                        StreamObserver<UserServiceProto.UpdateAccountStatusResponse> responseObserver) {
                log.info("Received gRPC request to update account status: keycloakUserId={}, accountStatus={}",
                                request.getKeycloakUserId(), request.getAccountStatus());

                try {
                        UserModel user = userRepository.findByKeycloakUserId(request.getKeycloakUserId())
                                        .orElse(null);

                        if (user == null) {
                                UserServiceProto.UpdateAccountStatusResponse response = UserServiceProto.UpdateAccountStatusResponse
                                                .newBuilder()
                                                .setSuccess(false)
                                                .setMessage("User not found with keycloak ID: "
                                                                + request.getKeycloakUserId())
                                                .build();
                                responseObserver.onNext(response);
                                responseObserver.onCompleted();
                                return;
                        }

                        try {
                                EAccountStatus accountStatus = EAccountStatus.valueOf(request.getAccountStatus());
                                user.setAccountStatus(accountStatus);
                                UserModel updatedUser = userRepository.save(user);

                                publishUserEvent(updatedUser, "UPDATE");

                                UserServiceProto.UpdateAccountStatusResponse response = UserServiceProto.UpdateAccountStatusResponse
                                                .newBuilder()
                                                .setSuccess(true)
                                                .setMessage("Account status updated successfully to: " +
                                                                updatedUser.getAccountStatus())
                                                .build();

                                log.info("Account status updated successfully: keycloakUserId={}, newStatus={}",
                                                updatedUser.getKeycloakUserId(), updatedUser.getAccountStatus());

                                responseObserver.onNext(response);
                                responseObserver.onCompleted();

                        } catch (IllegalArgumentException e) {
                                UserServiceProto.UpdateAccountStatusResponse response = UserServiceProto.UpdateAccountStatusResponse
                                                .newBuilder()
                                                .setSuccess(false)
                                                .setMessage("Invalid account status: " + request.getAccountStatus())
                                                .build();
                                responseObserver.onNext(response);
                                responseObserver.onCompleted();
                        }

                } catch (Exception e) {
                        log.error("Error updating account status: {}", e.getMessage(), e);

                        UserServiceProto.UpdateAccountStatusResponse response = UserServiceProto.UpdateAccountStatusResponse
                                        .newBuilder()
                                        .setSuccess(false)
                                        .setMessage("Error updating account status: " + e.getMessage())
                                        .build();

                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                }
        }

        @Override
        public void getUserFollowees(UserServiceProto.GetUserFolloweesRequest request,
                        StreamObserver<UserServiceProto.GetUserFolloweesResponse> responseObserver) {

                try {
                        Long userId = request.getUserId();
                        Set<FolloweeDTO> followees = followService.getFollowersByUserId(userId);

                        UserServiceProto.GetUserFolloweesResponse.Builder responseBuilder = UserServiceProto.GetUserFolloweesResponse
                                        .newBuilder()
                                        .setSuccess(true)
                                        .setMessage("Followees retrieved successfully");

                        for (FolloweeDTO followee : followees) {
                                UserServiceProto.FolloweeInfo followeeInfo = UserServiceProto.FolloweeInfo.newBuilder()
                                                .setUserId(followee.getUserId())
                                                .setUserName(followee.getUserName())
                                                .setAvatar(followee.getAvatar() != null ? followee.getAvatar() : "")
                                                .setIsCloseFriend(followee.isCloseFriend())
                                                .build();

                                responseBuilder.addFollowees(followeeInfo);
                        }

                        UserServiceProto.GetUserFolloweesResponse response = responseBuilder.build();

                        responseObserver.onNext(response);
                        responseObserver.onCompleted();

                        log.info("Successfully returned {} followees for userId={}", followees.size(), userId);

                } catch (Exception e) {
                        log.error("Error getting user followees: {}", e.getMessage(), e);

                        UserServiceProto.GetUserFolloweesResponse response = UserServiceProto.GetUserFolloweesResponse
                                        .newBuilder()
                                        .setSuccess(false)
                                        .setMessage("Error getting user followees: " + e.getMessage())
                                        .build();

                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                }
        }

        @Override
        public void getUserFollowings(UserServiceProto.GetUserFollowingsRequest request,
                        StreamObserver<UserServiceProto.GetUserFollowingsResponse> responseObserver) {
                try {
                        Long userId = request.getUserId();
                        Set<FolloweeDTO> followings = followService.getFollowingsByUserId(userId);

                        UserServiceProto.GetUserFollowingsResponse.Builder responseBuilder = UserServiceProto.GetUserFollowingsResponse
                                        .newBuilder()
                                        .setSuccess(true)
                                        .setMessage("Followings retrieved successfully");

                        for (FolloweeDTO following : followings) {
                                UserServiceProto.FolloweeInfo followingInfo = UserServiceProto.FolloweeInfo.newBuilder()
                                                .setUserId(following.getUserId())
                                                .setUserName(following.getUserName())
                                                .setAvatar(following.getAvatar() != null ? following.getAvatar() : "")
                                                .setIsCloseFriend(following.isCloseFriend())
                                                .build();
                                responseBuilder.addFollowings(followingInfo);
                        }

                        UserServiceProto.GetUserFollowingsResponse response = responseBuilder.build();
                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                        log.info("Successfully returned {} followings for userId={}", followings.size(), userId);
                } catch (Exception e) {
                        log.error("Error getting user followings: {}", e.getMessage(), e);
                        UserServiceProto.GetUserFollowingsResponse response = UserServiceProto.GetUserFollowingsResponse
                                        .newBuilder()
                                        .setSuccess(false)
                                        .setMessage("Error getting user followings: " + e.getMessage())
                                        .build();
                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                }
        }

        @Override
        public void getUserDto(UserServiceProto.KeycloakId request,
                        StreamObserver<UserServiceProto.UserDto> responseObserver) {
                try {
                        String keycloakId = request.getKeycloakUserId();
                        UserModel user = userRepository.findByKeycloakUserId(keycloakId).orElse(null);

                        if (user == null) {
                                UserServiceProto.UserDto response = UserServiceProto.UserDto.newBuilder()
                                                .setUserId(0L)
                                                .setKeycloakUserId(keycloakId)
                                                .setEmail("")
                                                .setUsername("")
                                                .setFullName("")
                                                .build();

                                responseObserver.onNext(response);
                                responseObserver.onCompleted();
                                return;
                        }

                        UserServiceProto.UserDto response = UserServiceProto.UserDto.newBuilder()
                                        .setUserId(user.getId())
                                        .setKeycloakUserId(user.getKeycloakUserId())
                                        .setEmail(user.getEmail() != null ? user.getEmail() : "")
                                        .setUsername(user.getUsername() != null ? user.getUsername() : "")
                                        .setFullName(user.getFullName() != null ? user.getFullName() : "")
                                        .build();

                        responseObserver.onNext(response);
                        responseObserver.onCompleted();

                        log.info("Successfully returned user dto for userId={}, keycloakId={}", user.getId(),
                                        keycloakId);

                } catch (Exception e) {
                        log.error("Error getting user dto: {}", e.getMessage(), e);

                        UserServiceProto.UserDto response = UserServiceProto.UserDto.newBuilder()
                                        .setUserId(0L)
                                        .setKeycloakUserId("")
                                        .setEmail("")
                                        .setUsername("")
                                        .setFullName("")
                                        .build();

                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                }
        }

        @Override
        public void getUserDtoById(UserServiceProto.GetUserDtoByIdRequest request,
                        StreamObserver<UserServiceProto.UserDTOResponse> responseObserver) {
                try {
                        Long userId = request.getUserId();
                        var userOpt = userRepository.findById(userId);
                        if (userOpt.isEmpty()) {
                                UserServiceProto.UserDTOResponse response = UserServiceProto.UserDTOResponse
                                                .newBuilder()
                                                .setId(0L)
                                                .setUsername("")
                                                .setFullName("")
                                                .setAvatar("")
                                                .setBio("")
                                                .setCreatedAt("")
                                                .build();
                                responseObserver.onNext(response);
                                responseObserver.onCompleted();
                                return;
                        }
                        var user = userOpt.get();
                        UserServiceProto.UserDTOResponse response = UserServiceProto.UserDTOResponse.newBuilder()
                                        .setId(user.getId())
                                        .setUsername(user.getUsername() != null ? user.getUsername() : "")
                                        .setFullName(user.getFullName() != null ? user.getFullName() : "")
                                        .setAvatar(user.getAvatar() != null ? user.getAvatar() : "")
                                        .setBio(user.getBio() != null ? user.getBio() : "")
                                        .setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : "")
                                        .build();
                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                } catch (Exception e) {
                        log.error("Error getting user dto by id: {}", e.getMessage(), e);
                        UserServiceProto.UserDTOResponse response = UserServiceProto.UserDTOResponse.newBuilder()
                                        .setId(0L)
                                        .setUsername("")
                                        .setFullName("")
                                        .setAvatar("")
                                        .setBio("")
                                        .setCreatedAt("")
                                        .build();
                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                }
        }

        @Override
        public void checkFollow(UserServiceProto.CheckMutualFollowRequest request,
                        StreamObserver<UserServiceProto.CheckFollowResponse> responseObserver) {
                List<Boolean> list = followService.isFollow(request.getUserId1(), request.getUserId2());
                UserServiceProto.CheckFollowResponse response = UserServiceProto.CheckFollowResponse
                                .newBuilder()
                                .setIsFollow(list.get(0))
                                .setIsPrivate(list.get(1) != null ? list.get(1) : false)
                                .setIsCloseFriend(list.get(2) != null ? list.get(2) : false)
                                .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
        }

        @Override
        public void getUsersByIds(UserServiceProto.GetUsersByIdsRequest request,
                        StreamObserver<UserServiceProto.GetUsersByIdsResponse> responseObserver) {
                List<UserServiceProto.UserDTOResponse2> list = new ArrayList<>();
                for (Long id : request.getUserIdsList()) {
                        var user = userRepository.findById(id).orElse(null);
                        if (user != null) {
                                UserServiceProto.UserDTOResponse2 response = UserServiceProto.UserDTOResponse2
                                                .newBuilder()
                                                .setId(user.getId())
                                                .setUsername(user.getUsername() != null ? user.getUsername() : "")
                                                .setAvatar(user.getAvatar() != null ? user.getAvatar() : "")
                                                .setFullName(user.getFullName() != null ? user.getFullName() : "")
                                                .setOnlineStatus(user.getOnlineStatus() != null ? user.getOnlineStatus()
                                                                : false)
                                                .build();
                                list.add(response);
                        }
                }
                responseObserver.onNext(UserServiceProto.GetUsersByIdsResponse.newBuilder().addAllUsers(list).build());
                responseObserver.onCompleted();
        }

        @Override
        public void getUserOnlineStatus(UserServiceProto.GetUserOnlineStatusRequest request,
                        StreamObserver<UserServiceProto.GetUserOnlineStatusResponse> responseObserver) {
                Long userId = request.getUserId();
                UserModel user = userRepository.findById(userId).orElse(null);

                boolean onlineStatus = false;
                if (user != null && user.getOnlineStatus() != null) {
                        onlineStatus = user.getOnlineStatus();
                }

                UserServiceProto.GetUserOnlineStatusResponse response = UserServiceProto.GetUserOnlineStatusResponse
                                .newBuilder()
                                .setOnlineStatus(onlineStatus)
                                .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
        }

        @Override
        public void checkMutualFriends(UserServiceProto.CheckMutualFriendsRequest request,
                        StreamObserver<UserServiceProto.CheckMutualFriendsResponse> responseObserver) {
                Long userId1 = request.getUserId1();
                Long userId2 = request.getUserId2();

                List<Boolean> followStatus1 = followService.isFollow(userId1, userId2);
                List<Boolean> followStatus2 = followService.isFollow(userId2, userId1);

                boolean areMutualFriends = followStatus1.get(0) && followStatus2.get(0);

                UserServiceProto.CheckMutualFriendsResponse response = UserServiceProto.CheckMutualFriendsResponse
                                .newBuilder()
                                .setAreMutualFriends(areMutualFriends)
                                .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
        }

        @Override
        public void getMutualFriends(UserServiceProto.GetMutualFriendsRequest request,
                        StreamObserver<UserServiceProto.GetMutualFriendsResponse> responseObserver) {
                Long userId = request.getUserId();
                Set<FolloweeDTO> followers = followService.getFollowersByUserId(userId);
                Set<FolloweeDTO> followings = followService.getFollowingsByUserId(userId);

                List<Long> mutualFriendIds = followers.stream()
                                .filter(follower -> followings.stream()
                                                .anyMatch(following -> following.getUserId()
                                                                .equals(follower.getUserId())))
                                .map(FolloweeDTO::getUserId)
                                .toList();

                UserServiceProto.GetMutualFriendsResponse response = UserServiceProto.GetMutualFriendsResponse
                                .newBuilder()
                                .addAllUserIds(mutualFriendIds)
                                .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
        }

        @Override
        public void checkBlock(UserServiceProto.CheckBlockRequest request,
                        StreamObserver<UserServiceProto.CheckBlockResponse> responseObserver) {
                Long requesterUserId = request.getRequesterUserId();
                Long targetUserId = request.getTargetUserId();

                boolean isBlocked = blockService.isBlocked(requesterUserId, targetUserId);

                UserServiceProto.CheckBlockResponse response = UserServiceProto.CheckBlockResponse
                                .newBuilder()
                                .setIsBlocked(isBlocked)
                                .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
        }

        @Override
        public void checkIfBlocked(UserServiceProto.CheckIfBlockedRequest request,
                        StreamObserver<UserServiceProto.CheckIfBlockedResponse> responseObserver) {
                Long userId = request.getUserId();
                Long targetUserId = request.getTargetUserId();

                boolean userBlockedTarget = blockService.isBlocked(userId, targetUserId);
                boolean targetBlockedUser = blockService.isBlocked(targetUserId, userId);
                boolean isBlocked = userBlockedTarget || targetBlockedUser;
                long blockedByUserId = 0;

                if (userBlockedTarget) {
                        blockedByUserId = userId;
                } else if (targetBlockedUser) {
                        blockedByUserId = targetUserId;
                }

                UserServiceProto.CheckIfBlockedResponse response = UserServiceProto.CheckIfBlockedResponse
                                .newBuilder()
                                .setIsBlocked(isBlocked)
                                .setBlockedByUserId(blockedByUserId)
                                .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
        }

        @Override
        public void getLikeUsersByIds(UserServiceProto.GetUsersByIdsRequest2 request,
                        StreamObserver<UserServiceProto.GetUsersByIdsResponse2> responseObserver) {
                List<UserServiceProto.UserDTOResponse3> list = new ArrayList<>();
                UserModel user1 = userRepository.findById(request.getId())
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                for (Long id : request.getUserIdsList()) {
                        var user = userRepository.findById(id).orElse(null);
                        if (user != null) {
                                int isFollow = 0;
                                FollowModel followModel = followRepository
                                                .findById(new FollowId(user1.getId(), user.getId())).orElse(null);
                                if (followModel != null) {
                                        if (followModel.getStatus() == EStatusFollow.ACTIVE)
                                                isFollow = 1;
                                        else
                                                isFollow = 2;
                                }
                                UserServiceProto.UserDTOResponse3 response = UserServiceProto.UserDTOResponse3
                                                .newBuilder()
                                                .setId(user.getId())
                                                .setUsername(user.getUsername() != null ? user.getUsername() : "")
                                                .setAvatar(user.getAvatar() != null ? user.getAvatar() : "")
                                                .setIsPrivate(user.getIsPrivate())
                                                .setIsFollow(isFollow)
                                                .build();
                                list.add(response);
                        }
                }
                responseObserver.onNext(UserServiceProto.GetUsersByIdsResponse2.newBuilder().addAllUsers(list).build());
                responseObserver.onCompleted();
        }

        @Override
        public void getTotalUsers(UserServiceProto.Empty request,
                        StreamObserver<UserServiceProto.QuantityTotalUsers> responseObserver) {
                long total = userRepository.count();

                UserServiceProto.QuantityTotalUsers response = UserServiceProto.QuantityTotalUsers.newBuilder()
                                .setQuantity(total)
                                .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
        }

        @Override
        public void getPercentUsersInThisMonth(UserServiceProto.Empty request,
                        StreamObserver<UserServiceProto.UserPercentResponse> responseObserver) {
                LocalDateTime startOfThisMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
                LocalDateTime startOfLastMonth = startOfThisMonth.minusMonths(1);
                LocalDateTime endOfLastMonth = startOfThisMonth;

                long thisMonth = userRepository.countByCreatedAtBetween(startOfThisMonth,
                                startOfThisMonth.plusMonths(1));
                long lastMonth = userRepository.countByCreatedAtBetween(startOfLastMonth, endOfLastMonth);

                double percent = 0;
                if (lastMonth > 0) {
                        percent = ((double) (thisMonth - lastMonth) / lastMonth) * 100;
                }

                UserServiceProto.UserPercentResponse response = UserServiceProto.UserPercentResponse.newBuilder()
                                .setPercent(percent)
                                .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
        }

        @Override
        public void getUsersByTime(UserServiceProto.DateRange request,
                        StreamObserver<UserServiceProto.GetUsersByTimeResponse> responseObserver) {
                LocalDateTime start = LocalDate.parse(request.getStartDate()).atStartOfDay();
                LocalDateTime end = LocalDate.parse(request.getEndDate()).atTime(23, 59, 59);

                List<Object[]> result = userRepository.countUsersByDate(start, end);

                UserServiceProto.GetUsersByTimeResponse.Builder responseBuilder = UserServiceProto.GetUsersByTimeResponse
                                .newBuilder();

                for (Object[] row : result) {
                        String date = row[0].toString();
                        long total = ((Number) row[1]).longValue();

                        responseBuilder.addData(UserServiceProto.UserCountByDate.newBuilder()
                                        .setDate(date)
                                        .setCount(total)
                                        .build());
                }

                responseObserver.onNext(responseBuilder.build());
                responseObserver.onCompleted();
        }

        @Override
        public void getUserIdByEmail(UserServiceProto.GetUserIdByEmailRequest request,
                        StreamObserver<UserServiceProto.GetUserIdByEmailResponse> responseObserver) {

                UserModel user = userRepository.findByEmailAndAccountStatus(request.getEmail(), EAccountStatus.ACTIVE)
                                .orElse(null);

                if (user == null) {
                        UserServiceProto.GetUserIdByEmailResponse response = UserServiceProto.GetUserIdByEmailResponse
                                        .newBuilder()
                                        .setSuccess(false)
                                        .setMessage("User not found with email: " + request.getEmail())
                                        .setKeycloakUserId("")
                                        .build();

                        log.warn("User not found with email: {}", request.getEmail());
                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                        return;
                }

                UserServiceProto.GetUserIdByEmailResponse response = UserServiceProto.GetUserIdByEmailResponse
                                .newBuilder()
                                .setSuccess(true)
                                .setMessage("User found successfully")
                                .setKeycloakUserId(user.getKeycloakUserId())
                                .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
        }
}
