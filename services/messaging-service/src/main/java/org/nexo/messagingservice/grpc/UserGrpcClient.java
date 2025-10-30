package org.nexo.messagingservice.grpc;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.nexo.grpc.user.UserServiceProto;

import java.util.List;

import org.nexo.grpc.user.UserServiceGrpc;
import org.springframework.stereotype.Service;

/**
 * gRPC client service for communicating with User Service
 */
@Service
@Slf4j
public class UserGrpcClient {

    @GrpcClient("users")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    public UserServiceProto.UserDto getUserByKeycloakId(String keycloakId) {
        UserServiceProto.KeycloakId request = UserServiceProto.KeycloakId.newBuilder()
                .setKeycloakUserId(keycloakId)
                .build();

        return userServiceStub.getUserDto(request);
    }

    public UserServiceProto.UserDTOResponse getUserById(Long userId) {
        UserServiceProto.GetUserDtoByIdRequest userIDsRequest = UserServiceProto.GetUserDtoByIdRequest.newBuilder()
                .setUserId(userId)
                .build();
        return userServiceStub.getUserDtoById(userIDsRequest);
    }

    public List<UserServiceProto.UserDTOResponse2> getUsersByIds(List<Long> userIds) {
        UserServiceProto.GetUsersByIdsRequest userIDsRequest = UserServiceProto.GetUsersByIdsRequest.newBuilder()
                .addAllUserIds(userIds)
                .build();
        return userServiceStub.getUsersByIds(userIDsRequest).getUsersList();
    }

    public UserServiceProto.GetUserOnlineStatusResponse isUserOnline(Long userId) {
        UserServiceProto.GetUserOnlineStatusRequest request = UserServiceProto.GetUserOnlineStatusRequest.newBuilder()
                .setUserId(userId)
                .build();

        return userServiceStub.getUserOnlineStatus(request);
    }

    public UserServiceProto.CheckFollowResponse checkMutualFollow(Long userId1, Long userId2) {
        UserServiceProto.CheckMutualFollowRequest request = UserServiceProto.CheckMutualFollowRequest.newBuilder()
                .setUserId1(userId1)
                .setUserId2(userId2)
                .build();

        return userServiceStub.checkFollow(request);
    }

    public UserServiceProto.CheckMutualFriendsResponse checkMutualFriends(Long userId1, Long userId2) {
        UserServiceProto.CheckMutualFriendsRequest request = UserServiceProto.CheckMutualFriendsRequest.newBuilder()
                .setUserId1(userId1)
                .setUserId2(userId2)
                .build();
        return userServiceStub.checkMutualFriends(request);

    }

    public UserServiceProto.GetMutualFriendsResponse getMutualFriends(Long userId) {
        UserServiceProto.GetMutualFriendsRequest request = UserServiceProto.GetMutualFriendsRequest.newBuilder()
                .setUserId(userId)
                .build();
        return userServiceStub.getMutualFriends(request);

    }
}