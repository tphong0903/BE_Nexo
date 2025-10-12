package org.nexo.userservice.service;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.nexo.grpc.user.UserServiceGrpc;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.userservice.dto.FolloweeDTO;
import org.nexo.userservice.enums.EAccountStatus;
import org.nexo.userservice.model.UserModel;
import org.nexo.userservice.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserRepository userRepository;
    private final FollowService followService;

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
                .accountStatus(EAccountStatus.PENDING)
                .build();

        UserModel savedUser = userRepository.save(user);

        UserServiceProto.CreateUserResponse response = UserServiceProto.CreateUserResponse.newBuilder()
                .setSuccess(true)
                .setMessage("User created successfully")
                .setUserId(savedUser.getId())
                .build();

        log.info("User created successfully: id={}, email={}, keycloakUserId={}",
                savedUser.getId(), savedUser.getEmail(), savedUser.getKeycloakUserId());

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void updateUserEmailVerification(UserServiceProto.UpdateEmailVerificationRequest request,
                                            StreamObserver<UserServiceProto.UpdateEmailVerificationResponse> responseObserver) {
        log.info("Received gRPC request to update email verification: keycloakUserId={}, emailVerified={}",
                request.getKeycloakUserId(), request.getEmailVerified());

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

            // Update email verification status and account status
            if (request.getEmailVerified()) {
                user.setAccountStatus(EAccountStatus.ACTIVE); // Kích hoạt tài khoản khi email được xác
                // thực
                log.info("User account activated for keycloakUserId: {}", request.getKeycloakUserId());
            } else {
                user.setAccountStatus(EAccountStatus.PENDING); // Đặt lại về trạng thái chờ
            }

            UserModel updatedUser = userRepository.save(user);

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
                .setIsPrivate(list.get(1))
                .setIsCloseFriend(list.get(2))
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
                UserServiceProto.UserDTOResponse2 response = UserServiceProto.UserDTOResponse2.newBuilder()
                        .setId(user.getId())
                        .setUsername(user.getUsername() != null ? user.getUsername() : "")
                        .setAvatar(user.getAvatar() != null ? user.getAvatar() : "")
                        .build();
                list.add(response);
            }
        }
        responseObserver.onNext(UserServiceProto.GetUsersByIdsResponse.newBuilder().addAllUsers(list).build());
        responseObserver.onCompleted();
    }

}
