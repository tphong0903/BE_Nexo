package org.nexo.userservice.grpc;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.nexo.grpc.auth.AuthServiceGrpc;
import org.nexo.grpc.auth.AuthServiceProto.BanUserRequest;
import org.nexo.grpc.auth.AuthServiceProto.BanUserResponse;
import org.nexo.grpc.auth.AuthServiceProto.ChangePasswordRequest;
import org.nexo.grpc.auth.AuthServiceProto.ChangePasswordResponse;
import org.nexo.grpc.auth.AuthServiceProto.ChangeUserRoleRequest;
import org.nexo.grpc.auth.AuthServiceProto.ChangeUserRoleResponse;
import org.nexo.userservice.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuthGrpcClient {

    @GrpcClient("auth-service")
    private AuthServiceGrpc.AuthServiceBlockingStub authServiceBlockingStub;

    public boolean changeUserRole(String userId, String roleName) {
        ChangeUserRoleRequest request = ChangeUserRoleRequest.newBuilder()
                .setUserId(userId)
                .setRoleName(roleName)
                .build();
        ChangeUserRoleResponse response = authServiceBlockingStub.changeUserRole(request);
        if (response.getSuccess()) {
            log.info("Successfully changed role for userId: {} to {}", userId, roleName);
        } else {
            log.warn("Failed to change role for userId: {}, message: {}", userId, response.getMessage());
        }
        return response.getSuccess();
    }

    public boolean banUser(String userId) {
        BanUserRequest request = BanUserRequest.newBuilder()
                .setUserId(userId)
                .build();
        BanUserResponse response = authServiceBlockingStub.banUser(request);
        if (response.getSuccess()) {
            log.info("Successfully banned userId: {}", userId);
        } else {
            log.warn("Failed to ban userId: {}, message: {}", userId, response.getMessage());
        }
        return response.getSuccess();
    }

    public void changePassword(String keycloakUserId, String oldPassword, String newPassword) {
        ChangePasswordRequest request = ChangePasswordRequest.newBuilder()
                .setUserId(keycloakUserId)
                .setOldPassword(oldPassword)
                .setNewPassword(newPassword)
                .build();
        ChangePasswordResponse response = authServiceBlockingStub.changePassword(request);
        if (response.getSuccess()) {
            log.info("Successfully changed password for keycloakUserId: {}",
                    keycloakUserId);
        } else {
            log.warn("Failed to change password for keycloakUserId: {}, message: {}",
                    keycloakUserId,
                    response.getMessage());
            throw new ResourceNotFoundException("Failed to change password: " +
                    response.getMessage());
        }
    }
}