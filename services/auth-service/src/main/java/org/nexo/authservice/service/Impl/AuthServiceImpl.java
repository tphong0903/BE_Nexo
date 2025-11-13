package org.nexo.authservice.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.authservice.config.KeycloakConfig;
import org.nexo.authservice.dto.KeycloakErrorResponse;
import org.nexo.authservice.dto.LoginRequest;
import org.nexo.authservice.dto.RegisterRequest;
import org.nexo.authservice.dto.TokenResponse;
import org.nexo.authservice.exception.KeycloakClientException;
import org.nexo.authservice.service.AuthService;
import org.nexo.authservice.service.UserGrpcClient;
import org.nexo.authservice.util.JwtUtil;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@AllArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final String GRANT_TYPE = "grant_type";
    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_SECRET = "client_secret";
    private static final String EMAIL = "email";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String REFRESH_TOKEN = "refresh_token";
    private static final String GRANT_TYPE_PASSWORD = "password";
    private static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
    private static final String ADMIN_CLI = "admin-cli";
    private static final String BEARER_PREFIX = "Bearer ";

    private final WebClient webClient;
    private final KeycloakConfig keycloakConfig;
    private final TokenCacheService tokenCacheService;
    private final ObjectMapper objectMapper;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final UserGrpcClient userGrpcClient;
    private final JwtUtil jwtUtil;

    public Mono<String> getClientSecret(String realm, String clientId) {
        return getAdminToken()
                .flatMap(adminToken -> getOrCacheClientUUID(realm, clientId, adminToken)
                        .flatMap(uuid -> fetchClientSecret(realm, uuid, adminToken)));
    }

    public Mono<TokenResponse> login(LoginRequest loginRequest) {
        log.info("Starting login process for user: {}", loginRequest.getEmail());

        return getClientSecret(keycloakConfig.getRealm(), keycloakConfig.getClientId())
                .flatMap(clientSecret -> {
                    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
                    formData.add(GRANT_TYPE, GRANT_TYPE_PASSWORD);
                    formData.add(CLIENT_ID, keycloakConfig.getClientId());
                    formData.add(CLIENT_SECRET, clientSecret);
                    formData.add(USERNAME, loginRequest.getEmail());
                    formData.add(PASSWORD, loginRequest.getPassword());

                    return webClient.post()
                            .uri(keycloakConfig.getLoginUrl())
                            .body(BodyInserters.fromFormData(formData))
                            .retrieve()
                            .bodyToMono(TokenResponse.class)
                            .flatMap(tokenResponse -> {
                                boolean emailVerified = jwtUtil.isEmailVerified(tokenResponse.getAccessToken());
                                String userId = jwtUtil.getUserIdFromToken(tokenResponse.getAccessToken());

                                log.info("Login successful for user: {}, email_verified: {}, userId: {}",
                                        loginRequest.getEmail(), emailVerified, userId);

                                userGrpcClient.updateAccountStatus(userId, "ACTIVE")
                                        .doOnSuccess(grpcResponse -> {
                                            if (grpcResponse.getSuccess()) {
                                                log.info("Account status updated to ACTIVE for userId: {}", userId);
                                            } else {
                                                log.warn("Failed to update account status for userId: {}, message: {}",
                                                        userId, grpcResponse.getMessage());
                                            }
                                        })
                                        .doOnError(error -> {
                                            log.error("Failed to update account status for userId: {}, error: {}",
                                                    userId, error.getMessage());
                                        })
                                        .subscribe();

                                return tokenCacheService.cacheToken(
                                        loginRequest.getEmail(),
                                        tokenResponse.getAccessToken(),
                                        tokenResponse.getRefreshToken(),
                                        tokenResponse.getExpiresIn())
                                        .thenReturn(tokenResponse);
                            });
                });
    }

    @Override
    public Mono<String> register(RegisterRequest registerRequest) {
        return getAdminToken()
                .flatMap(adminToken -> {
                    JsonNode userJson = createUserJson(registerRequest);

                    return webClient.post()
                            .uri(keycloakConfig.getUsersUrl())
                            .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + adminToken)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .bodyValue(userJson)
                            .exchangeToMono(response -> {
                                if (response.statusCode().is2xxSuccessful()) {
                                    return handleSuccessfulRegistration(response, adminToken, registerRequest);
                                } else {
                                    return handleRegistrationError(response);
                                }
                            });
                });
    }

    private Mono<String> handleSuccessfulRegistration(
            ClientResponse response,
            String adminToken,
            RegisterRequest registerRequest) {
        String location = response.headers().asHttpHeaders().getFirst(HttpHeaders.LOCATION);
        String userId = location != null ? location.substring(location.lastIndexOf("/") + 1) : null;

        if (userId == null) {
            return Mono.error(new KeycloakClientException(500, "Cannot extract userId"));
        }

        return userGrpcClient.createUser(
                userId,
                registerRequest.getEmail(),
                registerRequest.getFullname(),
                registerRequest.getUsername())
                .doOnSuccess(grpcResponse -> {
                    if (grpcResponse.getSuccess()) {
                        log.info("User created in user-service successfully: userId={}, userServiceId={}",
                                userId, grpcResponse.getUserId());

                        Mono.fromRunnable(() -> sendVerifyEmail(userId, adminToken)
                                .doOnError(ex -> {
                                    log.error("Send verify email failed for userId={} : {}", userId, ex.getMessage());
                                    enqueueRetry(userId, adminToken, 1);
                                })
                                .subscribe()).subscribe();
                    } else {
                        log.error("Failed to create user in user-service: {}", grpcResponse.getMessage());
                    }
                })
                .doOnError(error -> {
                    log.error("gRPC call to user-service failed for userId={}: {}", userId, error.getMessage());
                    Mono.fromRunnable(() -> sendVerifyEmail(userId, adminToken)
                            .doOnError(ex -> {
                                log.error("Send verify email failed for userId={} : {}", userId, ex.getMessage());
                                enqueueRetry(userId, adminToken, 1);
                            })
                            .subscribe()).subscribe();
                })
                .map(grpcResponse -> userId)
                .onErrorReturn(userId);
    }

    private Mono<String> handleRegistrationError(
            org.springframework.web.reactive.function.client.ClientResponse response) {
        return response.bodyToMono(String.class)
                .flatMap(body -> {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        KeycloakErrorResponse err = mapper.readValue(body, KeycloakErrorResponse.class);
                        return Mono.error(
                                new KeycloakClientException(response.statusCode().value(), err.getErrorMessage()));
                    } catch (Exception e) {
                        return Mono.error(new KeycloakClientException(response.statusCode().value(), "Unknown error"));
                    }
                });
    }

    public Mono<Void> resendVerifyEmail(String userId) {
        return getAdminToken()
                .flatMap(adminToken -> sendVerifyEmail(userId, adminToken));
    }

    private Mono<Void> sendVerifyEmail(String userId, String adminToken) {
        return webClient.put()
                .uri(keycloakConfig.getUsersUrl() + "/" + userId + "/send-verify-email")
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + adminToken)
                .retrieve()
                .bodyToMono(Void.class);
    }

    private void enqueueRetry(String userId, String adminToken, int attempt) {
        if (attempt > 3) {
            log.error("Send verify email failed permanently for userId={}", userId);
            return;
        }

        int delaySeconds = attempt * 10;
        log.info("Retry sending verify email for userId={} after {}s (attempt #{})", userId, delaySeconds, attempt);

        Mono.delay(Duration.ofSeconds(delaySeconds))
                .flatMap(t -> sendVerifyEmail(userId, adminToken)
                        .doOnSuccess(v -> log.info("Retry success for userId={}", userId))
                        .doOnError(ex -> {
                            log.error("Retry failed for userId={} (attempt #{}) : {}", userId, attempt,
                                    ex.getMessage());
                            enqueueRetry(userId, adminToken, attempt + 1);
                        }))
                .subscribe();
    }

    public Mono<TokenResponse> refreshToken(String refreshToken) {
        log.info("Starting refresh token process");

        return getClientSecret(keycloakConfig.getRealm(), keycloakConfig.getClientId())
                .flatMap(clientSecret -> {
                    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
                    formData.add(GRANT_TYPE, GRANT_TYPE_REFRESH_TOKEN);
                    formData.add(CLIENT_ID, keycloakConfig.getClientId());
                    formData.add(CLIENT_SECRET, clientSecret);
                    formData.add(REFRESH_TOKEN, refreshToken);

                    return webClient.post()
                            .uri(keycloakConfig.getRefreshTokenUrl())
                            .body(BodyInserters.fromFormData(formData))
                            .retrieve()
                            .bodyToMono(TokenResponse.class)
                            .flatMap(tokenResponse -> {
                                String email = jwtUtil.getEmailFromToken(tokenResponse.getAccessToken());

                                if (email != null) {
                                    return tokenCacheService.cacheToken(
                                            email,
                                            tokenResponse.getAccessToken(),
                                            tokenResponse.getRefreshToken(),
                                            tokenResponse.getExpiresIn()).thenReturn(tokenResponse);
                                } else {
                                    log.warn("Could not extract email from token, skipping cache update");
                                    return Mono.just(tokenResponse);
                                }
                            });
                });
    }

    public Mono<Void> logout(String refreshToken) {
        log.info("Starting logout process");

        return getClientSecret(keycloakConfig.getRealm(), keycloakConfig.getClientId())
                .flatMap(clientSecret -> {
                    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
                    formData.add(CLIENT_ID, keycloakConfig.getClientId());
                    formData.add(CLIENT_SECRET, clientSecret);
                    formData.add(REFRESH_TOKEN, refreshToken);

                    return webClient.post()
                            .uri(keycloakConfig.getLogoutUrl())
                            .body(BodyInserters.fromFormData(formData))
                            .retrieve()
                            .bodyToMono(String.class)
                            .then();
                });
    }

    private Mono<String> getAdminToken() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add(GRANT_TYPE, GRANT_TYPE_PASSWORD);
        formData.add(CLIENT_ID, ADMIN_CLI);
        formData.add(USERNAME, keycloakConfig.getAdminUsername());
        formData.add(PASSWORD, keycloakConfig.getAdminPassword());

        return webClient.post()
                .uri(keycloakConfig.getTokenUrl())
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .map(TokenResponse::getAccessToken);
    }

    private ObjectNode createUserJson(RegisterRequest request) {
        ObjectNode userNode = objectMapper.createObjectNode();
        userNode.put(EMAIL, request.getEmail());
        userNode.put(USERNAME, request.getUsername());
        userNode.put("lastName", request.getFullname());
        ArrayNode requiredActions = userNode.putArray("requiredActions");
        requiredActions.add("VERIFY_EMAIL");
        userNode.put("enabled", true);

        ArrayNode credentialsArray = objectMapper.createArrayNode();
        ObjectNode credentialNode = objectMapper.createObjectNode();
        credentialNode.put("type", PASSWORD);
        credentialNode.put("value", request.getPassword());
        credentialNode.put("temporary", false);
        credentialsArray.add(credentialNode);

        userNode.set("credentials", credentialsArray);
        return userNode;
    }

    private Mono<String> getOrCacheClientUUID(String realm, String clientId, String adminToken) {
        String redisKey = "kc:uuid:" + realm + ":" + clientId;

        return redisTemplate.opsForValue().get(redisKey)
                .flatMap(Mono::just)
                .switchIfEmpty(
                        webClient.get()
                                .uri(keycloakConfig.getServerUrl() + "/admin/realms/" + realm + "/clients?clientId="
                                        + clientId)
                                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + adminToken)
                                .retrieve()
                                .bodyToMono(JsonNode.class)
                                .flatMap(clients -> {
                                    if (clients.isArray() && clients.size() > 0) {
                                        String uuid = clients.get(0).get("id").asText();
                                        log.info("Fetched UUID {} for client {}", uuid, clientId);
                                        return redisTemplate.opsForValue()
                                                .set(redisKey, uuid, Duration.ofDays(1))
                                                .thenReturn(uuid);
                                    } else {
                                        return Mono.error(new RuntimeException("Client not found: " + clientId));
                                    }
                                }));
    }

    private Mono<String> fetchClientSecret(String realm, String uuid, String token) {
        return webClient.get()
                .uri(keycloakConfig.getServerUrl() + "/admin/realms/" + realm + "/clients/" + uuid + "/client-secret")
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> json.get("value").asText());
    }

    public Mono<Void> forgotPassword(String email) {
        log.info("Starting forgot password process for email: {}", email);

        return userGrpcClient.getUserIdByEmail(email)
                .flatMap(grpcResponse -> {
                    if (!grpcResponse.getSuccess()) {
                        log.warn("User not found or not active for email: {}", email);
                        return Mono.error(new KeycloakClientException(404, grpcResponse.getMessage()));
                    }

                    String userId = grpcResponse.getKeycloakUserId();
                    log.info("Found active user with ID: {} for email: {}", userId, email);

                    return getAdminToken()
                            .flatMap(adminToken -> sendResetPasswordEmail(userId, adminToken));
                });
    }

    private Mono<Void> sendResetPasswordEmail(String userId, String adminToken) {
        return webClient.put()
                .uri(keycloakConfig.getServerUrl() + "/admin/realms/" + keycloakConfig.getRealm() + "/users/" + userId
                        + "/execute-actions-email")
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + adminToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(new String[] { "UPDATE_PASSWORD" })
                .retrieve()
                .bodyToMono(Void.class);
    }
}
