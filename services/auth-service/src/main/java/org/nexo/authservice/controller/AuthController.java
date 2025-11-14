package org.nexo.authservice.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.nexo.authservice.dto.CallBackRequest;
import org.nexo.authservice.dto.ForgotPasswordRequest;
import org.nexo.authservice.dto.LoginRequest;
import org.nexo.authservice.dto.RegisterRequest;
import org.nexo.authservice.dto.RegisterResponse;
import org.nexo.authservice.dto.ResendVerifyEmailRequest;
import org.nexo.authservice.dto.ResponseData;
import org.nexo.authservice.dto.TokenResponse;
import org.nexo.authservice.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping
@AllArgsConstructor
@Slf4j
public class AuthController {
        private final AuthService authService;

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
        public Mono<ResponseData<?>> register(@Valid @RequestBody RegisterRequest registerRequest) {
                return authService.register(registerRequest)
                                .map(userId -> {
                                        log.info("Registration successful for user: {}, userId: {}",
                                                        registerRequest.getEmail(), userId);
                                        RegisterResponse registerResponse = RegisterResponse.builder()
                                                        .userId(userId)
                                                        .build();
                                        return ResponseData.<RegisterResponse>builder()
                                                        .status(HttpStatus.OK.value())
                                                        .message("User registered successfully. Please check your email for verification.")
                                                        .data(registerResponse)
                                                        .build();
                                });
        }

        @PostMapping("/refresh")
        public Mono<ResponseData<?>> refresh(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
                String refreshToken = authHeader.replace("Bearer ", "").trim();

                return authService.refreshToken(refreshToken)
                                .map(tokenResponse -> {
                                        log.info("Token refresh successful for user with refresh token: {}",
                                                        refreshToken);
                                        return ResponseData.<TokenResponse>builder()
                                                        .status(HttpStatus.OK.value())
                                                        .message("Token refresh successful")
                                                        .data(tokenResponse)
                                                        .build();
                                });
        }

        @PostMapping("/logout")
        public Mono<ResponseData<?>> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
                String refreshToken = authHeader.replace("Bearer ", "").trim();

                return authService.logout(refreshToken)
                                .then(Mono.just(ResponseData.<Void>builder()
                                                .status(HttpStatus.OK.value())
                                                .message("Logged out successfully")
                                                .build()));

        }

        @PostMapping("/resend-verify-email")
        public Mono<ResponseData<?>> resendVerifyEmail(@Valid @RequestBody ResendVerifyEmailRequest request) {
                return authService.resendVerifyEmail(request.getID())
                                .then(Mono.just(ResponseData.<Void>builder()
                                                .status(HttpStatus.OK.value())
                                                .message("Verification email resent successfully")
                                                .build()));
        }

        @PostMapping("forgot-password")
        public Mono<ResponseData<?>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
                return authService.forgotPassword(request.getEMAIL())
                                .then(Mono.just(ResponseData.<Void>builder()
                                                .status(HttpStatus.OK.value())
                                                .message("If the email exists, a password reset link has been sent")
                                                .build()));
        }

        @PostMapping("verify-email")
        public Mono<ResponseData<?>> verifyEmail(
                        @Valid @RequestBody CallBackRequest request) {
                return authService.callBack(request)
                                .map(success -> ResponseData.<Void>builder()
                                                .status(HttpStatus.OK.value())
                                                .message("Email verified and updated successfully")
                                                .build());
        }

}
