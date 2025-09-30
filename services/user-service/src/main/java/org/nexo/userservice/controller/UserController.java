package org.nexo.userservice.controller;

import org.nexo.userservice.dto.ResponseData;
import org.nexo.userservice.dto.UpdateUserRequest;
import org.nexo.userservice.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;

    @GetMapping("/profile/{userId}")
    public ResponseData<?> getProfile(@PathVariable Long userId, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        String accessToken = authHeader.replace("Bearer ", "").trim();
        return ResponseData.builder()
                .status(200)
                .message("User profile retrieved successfully")
                .data(userService.getUserProfile(userId, accessToken))
                .build();
    }
     @GetMapping("/profile")
    public ResponseData<?> getProfile(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        String accessToken = authHeader.replace("Bearer ", "").trim();
        return ResponseData.builder()
                .status(200)
                .message("User profile retrieved successfully")
                .data(userService.getUserProfileMe( accessToken))
                .build();
    }

    @PutMapping("/profile")
    public ResponseData<?> updateProfile(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @RequestBody UpdateUserRequest request) {
        String accessToken = authHeader.replace("Bearer ", "").trim();
        return ResponseData.builder()
                .status(200)
                .message("User profile updated successfully")
                .data(userService.updateUser(accessToken, request))
                .build();
    }
}
