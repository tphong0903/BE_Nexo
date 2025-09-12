package org.nexo.authservice.service;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.nexo.grpc.user.UserServiceGrpc;
import org.nexo.grpc.user.UserServiceProto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
public class UserGrpcClient {

        @GrpcClient("user-service")
        private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

        public Mono<UserServiceProto.CreateUserResponse> createUser(String keycloakUserId, String email,
                        String fullname, String username) {
                return Mono.fromCallable(() -> {
                        UserServiceProto.CreateUserRequest request = UserServiceProto.CreateUserRequest.newBuilder()
                                        .setKeycloakUserId(keycloakUserId)
                                        .setEmail(email)
                                        .setUsername(username)
                                        .setFullName(fullname)
                                        .build();

                        log.info("Calling user-service gRPC to create user: {}", email);
                        return userServiceStub.createUser(request);
                })
                                .subscribeOn(Schedulers.boundedElastic())
                                .doOnSuccess(response -> log.info("User created successfully: {}",
                                                response.getMessage()))
                                .doOnError(error -> log.error("Failed to create user: {}", error.getMessage()));
        }

        public Mono<UserServiceProto.UpdateEmailVerificationResponse> updateUserEmailVerification(String keycloakUserId,
                        boolean emailVerified) {
                return Mono.fromCallable(() -> {
                        UserServiceProto.UpdateEmailVerificationRequest request = UserServiceProto.UpdateEmailVerificationRequest
                                        .newBuilder()
                                        .setKeycloakUserId(keycloakUserId)
                                        .setEmailVerified(emailVerified)
                                        .build();

                        log.info("Calling user-service gRPC to update email verification for user: {}, verified: {}",
                                        keycloakUserId, emailVerified);
                        return userServiceStub.updateUserEmailVerification(request);
                })
                                .subscribeOn(Schedulers.boundedElastic())
                                .doOnSuccess(response -> log.info("Email verification updated successfully: {}",
                                                response.getMessage()))
                                .doOnError(error -> log.error("Failed to update email verification: {}",
                                                error.getMessage()));
        }

        public Mono<UserServiceProto.UpdateAccountStatusResponse> updateAccountStatus(String keycloakUserId,
                        String accountStatus) {
                return Mono.fromCallable(() -> {
                        UserServiceProto.UpdateAccountStatusRequest request = UserServiceProto.UpdateAccountStatusRequest
                                        .newBuilder()
                                        .setKeycloakUserId(keycloakUserId)
                                        .setAccountStatus(accountStatus)
                                        .build();

                        log.info("Calling user-service gRPC to update account status for user: {}, status: {}",
                                        keycloakUserId, accountStatus);
                        return userServiceStub.updateAccountStatus(request);
                })
                                .subscribeOn(Schedulers.boundedElastic())
                                .doOnSuccess(response -> log.info("Account status updated successfully: {}",
                                                response.getMessage()))
                                .doOnError(error -> log.error("Failed to update account status: {}",
                                                error.getMessage()));
        }
}
