package org.nexo.userservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.userservice.dto.FolloweeDTO;
import org.nexo.userservice.dto.ResponseData;
import org.nexo.userservice.service.FollowService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class FollowController {
        private static final String BEARER_PREFIX = "Bearer ";
        private final FollowService followService;

        @PostMapping("/follow/{username}")
        public ResponseData<?> addFollow(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                        @PathVariable String username) {
                String accessToken = authHeader.replace(BEARER_PREFIX, "").trim();
                followService.addFollow(accessToken, username);
                return ResponseData.builder()
                                .status(200)
                                .message("Follow added successfully")
                                .data(null)
                                .build();
        }

        @DeleteMapping("/unfollow/{username}")
        public ResponseData<?> removeFollow(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                        @PathVariable String username) {
                String accessToken = authHeader.replace(BEARER_PREFIX, "").trim();
                followService.removeFollow(accessToken, username);
                return ResponseData.builder()
                                .status(200)
                                .message("Follow removed successfully")
                                .data(null)
                                .build();
        }

        @GetMapping("/followers/{username}")
        public ResponseData<?> getFollowers(
                        @PathVariable String username,
                        @PageableDefault(size = 10, sort = "id.followerId") Pageable pageable) {
                Page<FolloweeDTO> followers = followService.getFollowers(username, pageable);
                return ResponseData.builder()
                                .status(200)
                                .message("Followers retrieved successfully")
                                .data(followers)
                                .build();
        }

        @GetMapping("/followings/{username}")
        public ResponseData<?> getFollowings(
                        @PathVariable String username,
                        @PageableDefault(size = 10, sort = "id.followingId") Pageable pageable) {
                Page<FolloweeDTO> followings = followService.getFollowings(username, pageable);
                return ResponseData.builder()
                                .status(200)
                                .message("Followings retrieved successfully")
                                .data(followings)
                                .build();
        }

        @GetMapping("/requests")
        public ResponseData<?> getRequests(
                        @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                        @PageableDefault(size = 10, sort = "id.followerId") Pageable pageable) {
                String accessToken = authHeader.replace(BEARER_PREFIX, "").trim();
                Page<FolloweeDTO> requests = followService.getFollowRequests(accessToken, pageable);
                return ResponseData.builder()
                                .status(200)
                                .message("Follow requests retrieved successfully")
                                .data(requests)
                                .build();
        }

        @PostMapping("/accept")
        public ResponseData<?> acceptFollow(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                        @RequestParam String username) {
                String accessToken = authHeader.replace(BEARER_PREFIX, "").trim();
                followService.acceptFollowRequest(accessToken, username);
                return ResponseData.builder()
                                .status(200)
                                .message("Follow request accepted successfully")
                                .build();
        }

        @PostMapping("/reject")
        public ResponseData<?> rejectFollow(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                        @RequestParam String username) {
                String accessToken = authHeader.replace(BEARER_PREFIX, "").trim();
                followService.rejectFollowRequest(accessToken, username);
                return ResponseData.builder()
                                .status(200)
                                .message("Follow request rejected successfully")
                                .build();
        }

        @PutMapping("/close-friend/{username}")
        public ResponseData<?> toggleCloseFriend(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                        @PathVariable String username) {
                String accessToken = authHeader.replace(BEARER_PREFIX, "").trim();
                followService.toggleCloseFriend(accessToken, username);
                return ResponseData.builder()
                                .status(200)
                                .message("Close friend status updated successfully")
                                .data(null)
                                .build();
        }

        @GetMapping("/close-friends")
        public ResponseData<?> getCloseFriends(
                        @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
                        @PageableDefault(size = 10, sort = "id.followingId") Pageable pageable) {
                String accessToken = authHeader.replace(BEARER_PREFIX, "").trim();
                Page<FolloweeDTO> closeFriends = followService.getCloseFriends(accessToken, pageable);
                return ResponseData.builder()
                                .status(200)
                                .message("Close friends retrieved successfully")
                                .data(closeFriends)
                                .build();
        }
}
