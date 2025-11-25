package org.nexo.authservice.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.nexo.authservice.service.AuthService;
import org.nexo.grpc.auth.AuthServiceGrpc;
import org.nexo.grpc.auth.AuthServiceProto.BanUserRequest;
import org.nexo.grpc.auth.AuthServiceProto.BanUserResponse;
import org.nexo.grpc.auth.AuthServiceProto.ChangeUserRoleRequest;
import org.nexo.grpc.auth.AuthServiceProto.ChangeUserRoleResponse;
import org.nexo.grpc.auth.AuthServiceProto.UnBanUserRequest;
import org.nexo.grpc.auth.AuthServiceProto.UnBanUserResponse;
import org.nexo.grpc.auth.AuthServiceProto.ChangePasswordRequest;
import org.nexo.grpc.auth.AuthServiceProto.ChangePasswordResponse;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class AuthServiceGrpcImpl extends AuthServiceGrpc.AuthServiceImplBase {

    private final AuthService authService;

    @Override
    public void changeUserRole(ChangeUserRoleRequest request, StreamObserver<ChangeUserRoleResponse> responseObserver) {
        String userId = request.getUserId();
        String roleName = request.getRoleName();
        String adminToken = authService.getAdminToken().block();

        authService.changeUserRole(userId, roleName, adminToken)
                .doOnSuccess(v -> {
                    log.info("Successfully changed role for userId: {}", userId);
                    responseObserver.onNext(ChangeUserRoleResponse.newBuilder()
                            .setSuccess(true)
                            .setMessage("Role changed successfully")
                            .build());
                    responseObserver.onCompleted();
                })
                .doOnError(e -> {
                    log.error("Failed to change role for userId: {}", userId, e);
                    responseObserver.onNext(ChangeUserRoleResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage("Failed to change role: " + e.getMessage())
                            .build());
                    responseObserver.onCompleted();
                })
                .subscribe();
    }

    @Override
    public void banUser(BanUserRequest request, StreamObserver<BanUserResponse> responseObserver) {
        String userId = request.getUserId();

        authService.banUser(userId)
                .doOnSuccess(v -> {
                    log.info("Successfully banned userId: {}", userId);
                    responseObserver.onNext(BanUserResponse.newBuilder()
                            .setSuccess(true)
                            .setMessage("User banned successfully")
                            .build());
                    responseObserver.onCompleted();
                })
                .doOnError(e -> {
                    log.error("Failed to ban userId: {}", userId, e);
                    responseObserver.onNext(BanUserResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage("Failed to ban user: " + e.getMessage())
                            .build());
                    responseObserver.onCompleted();
                })
                .subscribe();
    }

    @Override
    public void unBanUser(UnBanUserRequest request, StreamObserver<UnBanUserResponse> responseObserver) {
        String userId = request.getUserId();

        authService.unBanUser(userId)
                .doOnSuccess(v -> {
                    log.info("Successfully unbanned userId: {}", userId);
                    responseObserver.onNext(UnBanUserResponse.newBuilder()
                            .setSuccess(true)
                            .setMessage("User unbanned successfully")
                            .build());
                    responseObserver.onCompleted();
                })
                .doOnError(e -> {
                    log.error("Failed to unban userId: {}", userId, e);
                    responseObserver.onNext(UnBanUserResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage("Failed to unban user: " + e.getMessage())
                            .build());
                    responseObserver.onCompleted();
                })
                .subscribe();
    }

    @Override
    public void changePassword(ChangePasswordRequest request, StreamObserver<ChangePasswordResponse> responseObserver) {
        String userId = request.getUserId();
        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();

        authService.changePassword(userId, oldPassword, newPassword)
                .doOnSuccess(v -> {
                    log.info("Successfully changed password for userId: {}", userId);
                    responseObserver.onNext(ChangePasswordResponse.newBuilder()
                            .setSuccess(true)
                            .setMessage("Password changed successfully")
                            .build());
                    responseObserver.onCompleted();
                })
                .doOnError(e -> {
                    log.error("Failed to change password for userId: {}", userId, e);
                    responseObserver.onNext(ChangePasswordResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage("Failed to change password: " + e.getMessage())
                            .build());
                    responseObserver.onCompleted();
                })
                .subscribe();
    }
}