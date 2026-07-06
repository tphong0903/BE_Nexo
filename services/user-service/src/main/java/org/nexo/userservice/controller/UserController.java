package org.nexo.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.userservice.dto.*;
import org.nexo.userservice.service.MeilisearchService;
import org.nexo.userservice.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;
    private final Validator validator;
    private final MeilisearchService meilisearchService;

    @PutMapping
    public ResponseData<?> updateUser(Authentication authentication,
                                      @RequestBody UpdateUserRequest request) {
        String keycloakUserId = authentication.getName();
        userService.updateUserOauth(keycloakUserId, request);
        return ResponseData.builder()
                .status(200)
                .message("User updated successfully")
                .data(null)
                .build();
    }

    @GetMapping("/profile/{username}")
    public ResponseData<?> getProfile(@PathVariable String username,
                                      @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        String accessToken = authHeader.replace("Bearer ", "").trim();
        return ResponseData.builder()
                .status(200)
                .message("User profile retrieved successfully")
                .data(userService.getUserProfile(username, accessToken))
                .build();
    }

    @GetMapping("/profile")
    public ResponseData<?> getProfile(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        String accessToken = authHeader.replace("Bearer ", "").trim();
        return ResponseData.builder()
                .status(200)
                .message("User profile retrieved successfully")
                .data(userService.getUserProfileMe(accessToken))
                .build();
    }

    @PutMapping("/profile")
    public ResponseData<?> updateProfile(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @Valid @RequestPart(value = "request", required = false) String requestJson,
            @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile) {
        try {
            String accessToken = authHeader.replace("Bearer ", "").trim();

            UpdateUserRequest request;
            if (requestJson != null && !requestJson.isEmpty()) {
                ObjectMapper objectMapper = new ObjectMapper();
                request = objectMapper.readValue(requestJson, UpdateUserRequest.class);

                Set<ConstraintViolation<UpdateUserRequest>> violations = validator.validate(request);
                if (!violations.isEmpty()) {
                    String errorMessages = violations.stream()
                            .map(ConstraintViolation::getMessage)
                            .collect(Collectors.joining(", "));
                    return ResponseData.builder()
                            .status(400)
                            .message(errorMessages)
                            .build();
                }
            } else {
                request = new UpdateUserRequest();
            }

            return ResponseData.builder()
                    .status(200)
                    .message("User profile updated successfully")
                    .data(userService.updateUser(accessToken, request, avatarFile))
                    .build();
        } catch (Exception e) {
            log.error("Error updating profile: {}", e.getMessage(), e);
            return ResponseData.builder()
                    .status(500)
                    .message("Error updating profile: " + e.getMessage())
                    .build();
        }
    }

    @DeleteMapping("/profile/avatar")
    public ResponseData<?> deleteAvatar(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        String accessToken = authHeader.replace("Bearer ", "").trim();
        userService.deleteAvatar(accessToken);
        return ResponseData.builder()
                .status(200)
                .message("Avatar deleted successfully and reset to default")
                .data(null)
                .build();
    }

    @GetMapping("/search")
    public ResponseData<?> searchUsers(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String filter) throws MeilisearchException {
        PageModelResponse<?> response = meilisearchService.searchUsers(query, pageNo, pageSize, filter);
        return ResponseData.builder()
                .status(200)
                .message("User search completed successfully")
                .data(response)
                .build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseData<?> getUser(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String filter) throws MeilisearchException {
        PageModelResponse<?> response = meilisearchService.searchUsersAdmin(query, pageNo, pageSize, filter);
        return ResponseData.builder()
                .status(200)
                .message("Admin user search completed successfully")
                .data(response)
                .build();
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseData<?> getInfoDashboardUser() {

        InfoDashboardUser response = userService.getInfoDashboardUser();

        return ResponseData.builder()
                .status(200)
                .message("Admin user search completed successfully")
                .data(response)
                .build();
    }


    @PostMapping("/assign-role/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseData<?> assignRoleToUser(@PathVariable String username,
                                            @RequestParam String role) {
        userService.assignRoleToUser(username, role);
        return ResponseData.builder()
                .status(200)
                .message("Role assigned to user successfully")
                .data(null)
                .build();
    }

    @PostMapping("/ban/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseData<?> banUser(@PathVariable String username) {
        userService.banUser(username);
        return ResponseData.builder()
                .status(200)
                .message("User banned successfully")
                .data(null)
                .build();
    }

    @PostMapping("/unban/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseData<?> unbanUser(@PathVariable String username) {
        userService.unbanUser(username);
        return ResponseData.builder()
                .status(200)
                .message("User unbanned successfully")
                .data(null)
                .build();
    }

    @PostMapping("change-password")
    public ResponseData<?> changePassword(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @Valid @RequestBody ChangePasswordRequest request) {
        String accessToken = authHeader.replace("Bearer ", "").trim();
        userService.changePassword(accessToken, request);
        return ResponseData.builder()
                .status(200)
                .message("Password changed successfully")
                .data(null)
                .build();
    }

    @GetMapping("/statistics/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseData<?> getUserStatistics(@PathVariable Long userId) {
        return ResponseData.builder()
                .status(200)
                .message("User statistics retrieved successfully")
                .data(userService.getUserStatistics(userId))
                .build();
    }

    @GetMapping("/activity-logs")
    public ResponseData<?> getActivityLogs(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        String accessToken = authHeader.replace("Bearer ", "").trim();
        return ResponseData.builder()
                .status(200)
                .message("Activity logs retrieved successfully")
                .data(userService.getUserActivityLogs(accessToken, pageNo, pageSize))
                .build();
    }

}
