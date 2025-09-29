package org.nexo.postservice.service.GrpcServiceImpl.client;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.nexo.grpc.user.UserServiceGrpc;
import org.nexo.grpc.user.UserServiceProto;
import org.springframework.stereotype.Service;

@Service
public class UserGrpcClient {

    @GrpcClient("users")
    private UserServiceGrpc.UserServiceBlockingStub userStub;

    public UserServiceProto.UserDto getUserByKeycloakId(String keycloakId) {
        UserServiceProto.KeycloakId request = UserServiceProto.KeycloakId.newBuilder()
                .setKeycloakUserId(keycloakId)
                .build();

        return userStub.getUserDto(request);
    }

    public UserServiceProto.GetUserFolloweesResponse getUserFollowees(Long userId) {
        UserServiceProto.GetUserFolloweesRequest request = UserServiceProto.GetUserFolloweesRequest.newBuilder()
                .setUserId(userId)
                .build();

        return userStub.getUserFollowees(request);
    }

    public UserServiceProto.UserDTOResponse getUserDTOById(Long userId) {
        UserServiceProto.GetUserDtoByIdRequest request = UserServiceProto.GetUserDtoByIdRequest.newBuilder()
                .setUserId(userId)
                .build();

        return userStub.getUserDtoById(request);
    }

    public UserServiceProto.CheckFollowResponse checkFollow(Long userId, Long userId2) {
        UserServiceProto.CheckMutualFollowRequest request = UserServiceProto.CheckMutualFollowRequest.newBuilder()
                .setUserId1(userId)
                .setUserId2(userId2)
                .build();

        return userStub.checkFollow(request);
    }
}
