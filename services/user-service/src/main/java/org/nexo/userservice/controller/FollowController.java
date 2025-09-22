package org.nexo.userservice.controller;

import java.util.Set;

import org.nexo.userservice.dto.FolloweeDTO;
import org.nexo.userservice.dto.ResponseData;
import org.nexo.userservice.service.FollowService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class FollowController {
    private static final String BEARER_PREFIX = "Bearer ";
    private final FollowService followService;

    @PostMapping("/follow/{followingId}")
    public ResponseData<?> addFollow(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @PathVariable Long followingId) {
        String accessToken = authHeader.replace(BEARER_PREFIX, "").trim();
        followService.addFollow(accessToken, followingId);
        return ResponseData.builder()
                .status(200)
                .message("Follow added successfully")
                .data(null)
                .build();
    }

    @DeleteMapping("/unfollow/{followingId}")
    public ResponseData<?> removeFollow(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @PathVariable Long followingId) {
        String accessToken = authHeader.replace(BEARER_PREFIX, "").trim();
        followService.removeFollow(accessToken, followingId);
        return ResponseData.builder()
                .status(200)
                .message("Follow removed successfully")
                .data(null)
                .build();
    }

    @GetMapping("/followees")
    public ResponseData<?> getFollowees(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        String accessToken = authHeader.replace(BEARER_PREFIX, "").trim();
        Set<FolloweeDTO> followees = followService.getFollowees(accessToken);
        return ResponseData.builder()
                .status(200)
                .message("Followees retrieved successfully")
                .data(followees)
                .build();
    }

    @PutMapping("/close-friend/{followingId}")
    public ResponseData<?> toggleCloseFriend(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @PathVariable Long followingId) {
        String accessToken = authHeader.replace(BEARER_PREFIX, "").trim();
        followService.toggleCloseFriend(accessToken, followingId);
        return ResponseData.builder()
                .status(200)
                .message("Close friend status updated successfully")
                .data(null)
                .build();
    }
}
