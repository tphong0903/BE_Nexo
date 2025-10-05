package org.nexo.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.util.Set;
import java.util.stream.Collectors;

import org.nexo.userservice.dto.ResponseData;
import org.nexo.userservice.dto.UpdateUserRequest;
import org.nexo.userservice.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;
    private final Validator validator;

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
}
