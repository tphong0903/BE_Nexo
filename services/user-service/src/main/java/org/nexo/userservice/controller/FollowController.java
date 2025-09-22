package org.nexo.userservice.controller;

import java.util.Set;

import org.nexo.userservice.dto.ResponseData;
import org.nexo.userservice.service.FollowService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final FollowService followService;

    @PostMapping("/{followerId}/{followingId}")
    public ResponseData<?> addFollow(@PathVariable Long followerId,
            @PathVariable Long followingId,
            @RequestParam(defaultValue = "false") boolean closeFriend) {
        followService.addFollow(followerId, followingId, closeFriend);
        return ResponseData.builder()
                .status(200)
                .message("Follow added successfully")
                .data(null)
                .build();
    }

    @DeleteMapping("/{followerId}/{followingId}")
    public ResponseData<?> removeFollow(@PathVariable Long followerId, @PathVariable Long followingId) {
        followService.removeFollow(followerId, followingId);
        return ResponseData.builder()
                .status(200)
                .message("Follow removed successfully")
                .data(null)
                .build();
    }

    @GetMapping("/followees/{userId}")
    public ResponseData<?> getFollowees(@PathVariable Long userId) {
        Set<String> followees = followService.getFollowees(userId);
        return ResponseData.builder()
                .status(200)
                .message("Followees retrieved successfully")
                .data(followees)
                .build();
    }
}
