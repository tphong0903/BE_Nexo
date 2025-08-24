package org.nexo.authservice.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.nexo.authservice.dto.LoginRequest;
import org.nexo.authservice.dto.RegisterRequest;
import org.nexo.authservice.dto.ResponseData;
import org.nexo.authservice.dto.TokenResponse;
import org.nexo.authservice.service.Impl.AuthServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping
@AllArgsConstructor
@Slf4j
public class AuthController {
    private final AuthServiceImpl authService;

    @PostMapping("/login")
    public Mono<ResponseData<?>> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login request received for user: {}", loginRequest.getEmail());
        return authService.login(loginRequest)
                .map(tokenResponse -> {
                    log.info("Login successful for user: {}", loginRequest.getEmail());
                    return ResponseData.<TokenResponse>builder()
                            .status(HttpStatus.OK.value())
                            .message("Login successful")
                            .data(tokenResponse)
                            .build();
                });
    }

    @PostMapping("/register")
    public Mono<ResponseData<?>>register(@Valid @RequestBody RegisterRequest registerRequest) {
        return authService.register(registerRequest)
            .map(tokenResponse -> {
                log.info("Registration successful for user: {}", registerRequest.getEmail());
                return ResponseData.<TokenResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message("Registration successful")
                        .build();
            });
    }

    @PostMapping("/refresh")
    public Mono<ResponseData<?>> refresh(@RequestParam String username) {
        return authService.refreshToken(username)
                .map(tokenResponse -> {
                    log.info("Token refresh successful for user: {}", username);
                    return ResponseData.<TokenResponse>builder()
                            .status(HttpStatus.OK.value())
                            .message("Token refresh successful")
                            .data(tokenResponse)
                            .build();
                });
    }

    @PostMapping("/logout")
    public Mono<ResponseData<?>> logout(@RequestParam String username) {
        return authService.logout(username)
                .then(Mono.just(ResponseData.<Void>builder()
                        .status(HttpStatus.OK.value())
                        .message("Logged out successfully")
                        .build()));
              
    }
}
