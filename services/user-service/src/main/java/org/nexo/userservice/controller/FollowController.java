package org.nexo.userservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.userservice.dto.FolloweeDTO;
import org.nexo.userservice.dto.ResponseData;
import org.nexo.userservice.service.FollowService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

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

    @GetMapping("/followees/{id}")
    public ResponseData<?> getFollowees(@PathVariable Long id) {
        Set<FolloweeDTO> followees = followService.getFollowees(id);
        return ResponseData.builder()
                .status(200)
                .message("Followees retrieved successfully")
                .data(followees)
                .build();
    }

    @GetMapping("/followings/{id}")
    public ResponseData<?> getFollowings(@PathVariable Long id) {
        Set<FolloweeDTO> followings = followService.getFollowings(id);
        return ResponseData.builder()
                .status(200)
                .message("Followings retrieved successfully")
                .data(followings)
                .build();
    }

    @GetMapping("/requests")
    public ResponseData<?> getRequests(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        String accessToken = authHeader.replace(BEARER_PREFIX, "").trim();
        Set<FolloweeDTO> requests = followService.getFollowRequests(accessToken);
        return ResponseData.builder()
                .status(200)
                .message("Follow requests retrieved successfully")
                .data(requests)
                .build();
    }

    @PostMapping("/accept")
    public ResponseData<?> acceptFollow(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                        @RequestParam Long followerId) {
        String accessToken = authHeader.replace(BEARER_PREFIX, "").trim();
        followService.acceptFollowRequest(accessToken, followerId);
        return ResponseData.builder()
                .status(200)
                .message("Follow request accepted successfully")
                .build();
    }

    @PostMapping("/reject")
    public ResponseData<?> rejectFollow(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                                        @RequestParam Long followerId) {
        String accessToken = authHeader.replace(BEARER_PREFIX, "").trim();
        followService.rejectFollowRequest(accessToken, followerId);
        return ResponseData.builder()
                .status(200)
                .message("Follow request rejected successfully")
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
