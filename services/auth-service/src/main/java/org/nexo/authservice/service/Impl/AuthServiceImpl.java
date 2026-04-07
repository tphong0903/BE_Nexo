package org.nexo.authservice.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.nexo.authservice.config.KeycloakConfig;
import org.nexo.authservice.dto.*;
import org.nexo.authservice.exception.KeycloakClientException;
import org.nexo.authservice.service.AuthService;
import org.nexo.authservice.service.UserGrpcClient;
import org.nexo.authservice.util.JwtUtil;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    private static final String GRANT_TYPE_AUTH_CODE = "authorization_code";

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

                })
                .map(grpcResponse -> userId)
                .onErrorReturn(userId);
    }

    private Mono<String> handleRegistrationError(ClientResponse response) {
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

    public Mono<String> getAdminToken() {
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
        return userGrpcClient.getUserIdByEmail(email)
                .flatMap(grpcResponse -> {
                    if (!grpcResponse.getSuccess()) {
                        log.warn("User not found or not active for email: {}", email);
                        return Mono.error(new KeycloakClientException(404, grpcResponse.getMessage()));
                    }
                    String userId = grpcResponse.getKeycloakUserId();
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
                .bodyValue(new String[]{"UPDATE_PASSWORD"})
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<String> callBack(CallBackRequest request) {
        return getAdminToken()
                .flatMap(adminToken -> {
                    return webClient.get()
                            .uri(keycloakConfig.getServerUrl() + "/admin/realms/" + keycloakConfig.getRealm()
                                    + "/users/" + request.getKeycloakId())
                            .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + adminToken)
                            .retrieve()
                            .bodyToMono(String.class)
                            .flatMap(userInfo -> {
                                if (userInfo == null || userInfo.isEmpty()) {
                                    log.error("User not found in Keycloak for keycloakId: {}", request.getKeycloakId());
                                    return Mono.error(new KeycloakClientException(404, "User not found"));
                                }

                                try {
                                    JsonNode userNode = objectMapper.readTree(userInfo);
                                    String keycloakUserId = userNode.has("id") ? userNode.get("id").asText() : null;
                                    String keycloakEmail = userNode.has("email") ? userNode.get("email").asText()
                                            : null;
                                    if (!request.getKeycloakId().equals(keycloakUserId)) {
                                        log.error("Keycloak user id mismatch. Expected: {}, Actual: {}",
                                                request.getKeycloakId(), keycloakUserId);
                                        return Mono.error(new KeycloakClientException(400, "User id mismatch"));
                                    }
                                    if (request.getEmail() != null && !request.getEmail().equals(keycloakEmail)) {
                                        log.error("Email mismatch. Expected: {}, Actual: {}",
                                                request.getEmail(), keycloakEmail);
                                        return Mono.error(new KeycloakClientException(400, "Email mismatch"));
                                    }

                                } catch (Exception e) {
                                    return Mono.error(new KeycloakClientException(500, "Failed to parse user info"));
                                }
                                verifyEmail(request.getKeycloakId(), adminToken).subscribe();
                                userGrpcClient.updateAccountStatus(request.getKeycloakId(), "ACTIVE")
                                        .doOnSuccess(grpcResponse -> {
                                            if (grpcResponse.getSuccess()) {
                                                log.info("Account status updated to ACTIVE for userId: {}",
                                                        request.getKeycloakId());
                                            } else {
                                                log.warn("Failed to update account status for userId: {}, message: {}",
                                                        request.getKeycloakId(), grpcResponse.getMessage());
                                            }
                                        })
                                        .doOnError(error -> {
                                            log.error("Failed to update account status for userId: {}, error: {}",
                                                    request.getKeycloakId(), error.getMessage());
                                        })
                                        .subscribe();
                                return Mono.just(userInfo);
                            });
                });
    }

    private Mono<Void> verifyEmail(String userId, String adminToken) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("emailVerified", true);
        payload.put("requiredActions", Collections.emptyList());
        return webClient.put()
                .uri(keycloakConfig.getServerUrl() + "/admin/realms/"
                        + keycloakConfig.getRealm() + "/users/" + userId)
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + adminToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<Void> banUser(String userId) {
        return getAdminToken()
                .flatMap(adminToken -> disableUser(userId, adminToken));
    }

    public Mono<Void> unBanUser(String userId) {
        return getAdminToken()
                .flatMap(adminToken -> enableUser(userId, adminToken));
    }

    private Mono<Void> disableUser(String userId, String adminToken) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("enabled", false);
        return webClient.put()
                .uri(keycloakConfig.getServerUrl() + "/admin/realms/"
                        + keycloakConfig.getRealm() + "/users/" + userId)
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + adminToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Void.class);
    }

    private Mono<Void> enableUser(String userId, String adminToken) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("enabled", true);
        return webClient.put()
                .uri(keycloakConfig.getServerUrl() + "/admin/realms/"
                        + keycloakConfig.getRealm() + "/users/" + userId)
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + adminToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<List<Map<String, Object>>> getAllUserRoles(String userId, String clientUUID, String adminToken) {
        return webClient.get()
                .uri(keycloakConfig.getServerUrl()
                                + "/admin/realms/{realm}/users/{userId}/role-mappings/clients/{clientUUID}",
                        keycloakConfig.getRealm(), userId, clientUUID)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .collectList();
    }

    public Mono<Void> removeAllRoles(String userId, String clientUUID, String adminToken) {
        return getAllUserRoles(userId, clientUUID, adminToken)
                .flatMap(roles -> webClient.method(HttpMethod.DELETE)
                        .uri(keycloakConfig.getServerUrl()
                                        + "/admin/realms/{realm}/users/{userId}/role-mappings/clients/{clientUUID}",
                                keycloakConfig.getRealm(), userId, clientUUID)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .bodyValue(roles)
                        .retrieve()
                        .bodyToMono(Void.class));
    }

    public Mono<Map<String, Object>> getClientRole(String clientUUID, String roleName, String adminToken) {
        return webClient.get()
                .uri(keycloakConfig.getServerUrl()
                                + "/admin/realms/{realm}/clients/{clientUUID}/roles/{roleName}",
                        keycloakConfig.getRealm(), clientUUID, roleName)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                });
    }

    public Mono<Void> changeUserRole(String userId, String newRoleName, String adminToken) {

        return getOrCacheClientUUID(keycloakConfig.getRealm(), keycloakConfig.getClientId(), adminToken)
                .flatMap(clientUUID -> removeAllRoles(userId, clientUUID, adminToken)
                        .then(getClientRole(clientUUID, newRoleName, adminToken))
                        .flatMap(newRole -> webClient.post()
                                .uri(keycloakConfig.getServerUrl()
                                                + "/admin/realms/{realm}/users/{userId}/role-mappings/clients/{clientUUID}",
                                        keycloakConfig.getRealm(), userId, clientUUID)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .bodyValue(List.of(newRole))
                                .retrieve()
                                .bodyToMono(Void.class)));
    }

    @Override
    public Mono<OAuthLoginResponse> oauthCallback(OAuthCallbackRequest request) {
        return getClientSecret(keycloakConfig.getRealm(), keycloakConfig.getClientId())
                .flatMap(clientSecret -> {
                    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
                    formData.add(GRANT_TYPE, GRANT_TYPE_AUTH_CODE);
                    formData.add(CLIENT_ID, keycloakConfig.getClientId());
                    formData.add(CLIENT_SECRET, clientSecret);
                    formData.add("code", request.getCode());
                    formData.add("redirect_uri", request.getRedirectUri());

                    return webClient.post()
                            .uri(keycloakConfig.getOAuthLoginUrl())
                            .body(BodyInserters.fromFormData(formData))
                            .retrieve()
                            .bodyToMono(OAuthLoginResponse.class)
                            .flatMap(tokenResponse -> {
                                String email = jwtUtil.getEmailFromToken(tokenResponse.getAccessToken());
                                String keycloakId = jwtUtil.getUserIdFromToken(tokenResponse.getAccessToken());
                                String username = jwtUtil.getUsernameFromToken(tokenResponse.getAccessToken());
                                String fullname = jwtUtil.getFullnameFromToken(tokenResponse.getAccessToken());
                                if (email != null) {
                                    return userGrpcClient.getUserIdByEmail(email)
                                            .flatMap(grpc -> {
                                                if (!grpc.getSuccess()) {
                                                    tokenResponse.setMissingInfo(true);
                                                    userGrpcClient.createUserOauth(
                                                            keycloakId,
                                                            email,
                                                            fullname,
                                                            username).subscribe();
                                                    return Mono.just(tokenResponse);
                                                } else {
                                                    tokenResponse.setMissingInfo(false);
                                                    return tokenCacheService.cacheToken(
                                                            email,
                                                            tokenResponse.getAccessToken(),
                                                            tokenResponse.getRefreshToken(),
                                                            tokenResponse.getExpiresIn()).thenReturn(tokenResponse);
                                                }
                                            });
                                } else {
                                    tokenResponse.setMissingInfo(true);
                                    return Mono.just(tokenResponse);
                                }
                            });
                });
    }

    @Override
    public Mono<SeedUsersResponse> seedFakeUsers(int count) {
        Faker faker = new Faker();
        List<SyncUserRequest> users = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            String cleaned = (faker.internet().username() + uniqueSuffix).replaceAll("[^a-zA-Z0-9_.]", "");
            String username = cleaned.length() >= 3
                    ? cleaned.substring(0, Math.min(30, cleaned.length()))
                    : "user" + uniqueSuffix;
            String email = uniqueSuffix + "@nexo.dev";
            String fullname = faker.name().fullName();

            SyncUserRequest req = new SyncUserRequest();
            req.setEmail(email);
            req.setUsername(username);
            req.setFullname(fullname);
            req.setDefaultPassword("Nexo@123456");
            users.add(req);
        }

        return getAdminToken()
                .flatMap(adminToken -> Flux.fromIterable(users)
                        .flatMap(user -> seedSingleUser(user, adminToken), 5)
                        .collectList())
                .map(results -> {
                    List<String> errors = results.stream()
                            .filter(r -> r.getStatus().startsWith("FAILED") || r.getStatus().equals("CONFLICT_EXISTS"))
                            .map(r -> r.getUsername() + ": " + r.getStatus())
                            .toList();
                    long succeeded = results.stream()
                            .filter(r -> r.getStatus().equals("SUCCESS"))
                            .count();
                    return SeedUsersResponse.builder()
                            .requested(count)
                            .succeeded((int) succeeded)
                            .failed(results.size() - (int) succeeded)
                            .errors(errors)
                            .build();
                });
    }

    private Mono<SyncUserResponse> seedSingleUser(SyncUserRequest request, String adminToken) {
        ObjectNode userNode = objectMapper.createObjectNode();
        userNode.put("email", request.getEmail());
        userNode.put("username", request.getUsername());
        userNode.put("lastName", request.getFullname());
        userNode.put("enabled", true);
        userNode.put("emailVerified", true);

        ArrayNode credentialsArray = objectMapper.createArrayNode();
        ObjectNode credentialNode = objectMapper.createObjectNode();
        credentialNode.put("type", "password");
        credentialNode.put("value", request.getDefaultPassword() != null ? request.getDefaultPassword() : "Nexo@123456");
        credentialNode.put("temporary", false);
        credentialsArray.add(credentialNode);
        userNode.set("credentials", credentialsArray);

        return webClient.post()
                .uri(keycloakConfig.getUsersUrl())
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + adminToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(userNode)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        String location = response.headers().asHttpHeaders().getFirst(HttpHeaders.LOCATION);
                        String keycloakId = location != null ? location.substring(location.lastIndexOf("/") + 1) : null;
                        if (keycloakId == null) {
                            return Mono.just(new SyncUserResponse(request.getUsername(), request.getEmail(), "FAILED: no keycloakId", null));
                        }
                        return userGrpcClient.createUser(keycloakId, request.getEmail(), request.getFullname(), request.getUsername())
                                .flatMap(grpcResp -> userGrpcClient.updateAccountStatus(keycloakId, "ACTIVE"))
                                .thenReturn(new SyncUserResponse(request.getUsername(), request.getEmail(), "SUCCESS", keycloakId))
                                .onErrorResume(ex -> {
                                    log.error("gRPC failed for {}: {}", request.getEmail(), ex.getMessage());
                                    return Mono.just(new SyncUserResponse(request.getUsername(), request.getEmail(), "FAILED: grpc - " + ex.getMessage(), keycloakId));
                                });
                    } else if (response.statusCode().value() == 409) {
                        return Mono.just(new SyncUserResponse(request.getUsername(), request.getEmail(), "CONFLICT_EXISTS", null));
                    } else {
                        return response.bodyToMono(String.class)
                                .map(body -> new SyncUserResponse(request.getUsername(), request.getEmail(), "FAILED: " + response.statusCode(), null));
                    }
                });
    }

    private Mono<Void> updatePassword(String userId, String newPassword, String adminToken) {
        ObjectNode passwordNode = objectMapper.createObjectNode();
        passwordNode.put("type", PASSWORD);
        passwordNode.put("value", newPassword);
        passwordNode.put("temporary", false);

        return webClient.put()
                .uri(keycloakConfig.getServerUrl() + "/admin/realms/" + keycloakConfig.getRealm()
                        + "/users/" + userId + "/reset-password")
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + adminToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(passwordNode)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("Password updated successfully for userId: {}", userId))
                .doOnError(error -> {
                    log.error("Failed to update password for userId: {}, error: {}", userId, error.getMessage());
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException ex = (WebClientResponseException) error;
                        log.error("Response body: {}", ex.getResponseBodyAsString());
                    }
                });
    }

    @Override
    public Mono<Void> changePassword(String keycloakUserId, String oldPassword, String newPassword) {
        return getAdminToken()
                .flatMap(adminToken -> {
                    return webClient.get()
                            .uri(keycloakConfig.getServerUrl() + "/admin/realms/" + keycloakConfig.getRealm()
                                    + "/users/" + keycloakUserId)
                            .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + adminToken)
                            .retrieve()
                            .bodyToMono(String.class)
                            .flatMap(userInfo -> {
                                try {
                                    JsonNode userNode = objectMapper.readTree(userInfo);
                                    String email = userNode.has("email") ? userNode.get("email").asText() : null;

                                    if (email == null) {
                                        return Mono.error(new KeycloakClientException(404, "User email not found"));
                                    }
                                    return getClientSecret(keycloakConfig.getRealm(), keycloakConfig.getClientId())
                                            .flatMap(clientSecret -> {
                                                MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
                                                formData.add(GRANT_TYPE, GRANT_TYPE_PASSWORD);
                                                formData.add(CLIENT_ID, keycloakConfig.getClientId());
                                                formData.add(CLIENT_SECRET, clientSecret);
                                                formData.add(USERNAME, email);
                                                formData.add(PASSWORD, oldPassword);

                                                return webClient.post()
                                                        .uri(keycloakConfig.getLoginUrl())
                                                        .body(BodyInserters.fromFormData(formData))
                                                        .exchangeToMono(response -> {
                                                            if (response.statusCode().is2xxSuccessful()) {
                                                                return updatePassword(keycloakUserId, newPassword,
                                                                        adminToken);
                                                            } else {
                                                                log.warn(
                                                                        "Old password verification failed for userId: {}",
                                                                        keycloakUserId);
                                                                return Mono.error(new KeycloakClientException(400,
                                                                        "Old password is incorrect"));
                                                            }
                                                        });
                                            });
                                } catch (Exception e) {
                                    log.error("Failed to parse user info: {}", e.getMessage());
                                    return Mono.error(
                                            new KeycloakClientException(500, "Failed to process user information"));
                                }
                            });
                });
    }

    public Mono<List<SyncUserResponse>> syncUsersToKeycloak(List<SyncUserRequest> users) {
        return getAdminToken()
                .flatMap(adminToken -> Flux.fromIterable(users)
                        .flatMap(user -> syncSingleUser(user, adminToken))
                        .collectList());
    }

    private Mono<SyncUserResponse> syncSingleUser(SyncUserRequest request, String adminToken) {
        ObjectNode userNode = objectMapper.createObjectNode();
        userNode.put("email", request.getEmail());
        userNode.put("username", request.getUsername());
        userNode.put("lastName", request.getFullname());
        userNode.put("enabled", true);
        userNode.put("emailVerified", true);

        ArrayNode credentialsArray = objectMapper.createArrayNode();
        ObjectNode credentialNode = objectMapper.createObjectNode();
        credentialNode.put("type", "password");
        credentialNode.put("value", request.getDefaultPassword() != null ? request.getDefaultPassword() : "Nexo@123456");
        credentialNode.put("temporary", false);
        credentialsArray.add(credentialNode);

        userNode.set("credentials", credentialsArray);

        return webClient.post()
                .uri(keycloakConfig.getUsersUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(userNode)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        String location = response.headers().asHttpHeaders().getFirst(HttpHeaders.LOCATION);
                        String userId = location != null ? location.substring(location.lastIndexOf("/") + 1) : null;
                        return Mono.just(new SyncUserResponse(request.getUsername(), request.getEmail(), "SUCCESS", userId));
                    } else if (response.statusCode().value() == 409) {
                        return Mono.just(new SyncUserResponse(request.getUsername(), request.getEmail(), "CONFLICT_EXISTS", null));
                    } else {
                        return response.bodyToMono(String.class)
                                .map(errorBody -> new SyncUserResponse(request.getUsername(), request.getEmail(), "FAILED: " + response.statusCode(), null));
                    }
                });
    }
}
