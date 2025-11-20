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
}