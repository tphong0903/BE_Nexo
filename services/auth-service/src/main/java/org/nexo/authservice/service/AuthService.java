package org.nexo.authservice.service;

import org.nexo.authservice.dto.LoginRequest;
import org.nexo.authservice.dto.RegisterRequest;
import org.nexo.authservice.dto.TokenResponse;

import reactor.core.publisher.Mono;

public interface AuthService {
    Mono<String> register(RegisterRequest registerRequest);

    Mono<TokenResponse> login(LoginRequest loginRequest);

    Mono<TokenResponse> refreshToken(String username);

    Mono<Void> logout(String refreshToken);
}
