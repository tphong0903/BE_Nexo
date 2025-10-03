package org.nexo.interactionservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.interactionservice.dto.response.ResponseData;
import org.nexo.interactionservice.service.ILikeService;
import org.nexo.interactionservice.service.impl.LeakyBucketService;
import org.nexo.interactionservice.util.Enum.SecurityUtil;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/like")
@Validated
@Slf4j
@RequiredArgsConstructor
public class LikeController {
    private static final long LIKE_BUCKET_CAPACITY = 10;
    private static final long LIKE_LEAK_RATE_MILLIS = 60_000 / 10;

    private final ILikeService likeService;
    private final LeakyBucketService leakyBucketService;
    private final SecurityUtil securityUtil;

    @PostMapping("/comment/{id}")
    public ResponseData<String> saveLikeComment(@PathVariable Long id) {
        Long userId = securityUtil.getUserIdFromToken();
        String key = "like:comment:" + id + ":" + userId;

        if (!leakyBucketService.allowRequest(key, LIKE_BUCKET_CAPACITY, LIKE_LEAK_RATE_MILLIS)) {
            return new ResponseData<>(429, "Too many requests. Please try later.", null);
        }

        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", likeService.saveLikeComment(id));
    }

    @PostMapping("/post/{id}")
    public ResponseData<String> saveLikePost(@PathVariable Long id) {
        Long userId = securityUtil.getUserIdFromToken();
        String key = "like:post:" + id + ":" + userId;

        if (!leakyBucketService.allowRequest(key, LIKE_BUCKET_CAPACITY, LIKE_LEAK_RATE_MILLIS)) {
            return new ResponseData<>(429, "Too many requests. Please try later.", null);
        }

        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", likeService.saveLikePost(id));
    }

    @PostMapping("/reel/{id}")
    public ResponseData<String> saveLikeReel(@PathVariable Long id) {
        Long userId = securityUtil.getUserIdFromToken();
        String key = "like:reel:" + id + ":" + userId;

        if (!leakyBucketService.allowRequest(key, LIKE_BUCKET_CAPACITY, LIKE_LEAK_RATE_MILLIS)) {
            return new ResponseData<>(429, "Too many requests. Please try later.", null);
        }

        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", likeService.saveLikeReel(id));
    }
}
