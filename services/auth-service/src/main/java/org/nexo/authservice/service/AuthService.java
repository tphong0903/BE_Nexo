package org.nexo.authservice.service;

import org.nexo.authservice.dto.*;
import reactor.core.publisher.Mono;

import java.util.List;

public interface AuthService {
    Mono<String> register(RegisterRequest registerRequest);

    Mono<TokenResponse> login(LoginRequest loginRequest, String ipAddress);

    Mono<TokenResponse> refreshToken(String refreshToken, String ipAddress);

    Mono<Void> logout(String refreshToken);

    Mono<Void> resendVerifyEmail(String userId);

    Mono<Void> forgotPassword(String email);

    Mono<String> callBack(CallBackRequest request);

    Mono<String> getAdminToken();

    Mono<Void> changeUserRole(String userId, String roleName, String adminToken);

    Mono<Void> banUser(String userId);

    Mono<Void> unBanUser(String userId);

    Mono<OAuthLoginResponse> oauthCallback(OAuthCallbackRequest request);

    Mono<Void> changePassword(String keycloakUserId, String oldPassword, String newPassword);

    Mono<List<SyncUserResponse>> syncUsersToKeycloak(List<SyncUserRequest> users);

    Mono<SeedUsersResponse> seedFakeUsers(int count);
}