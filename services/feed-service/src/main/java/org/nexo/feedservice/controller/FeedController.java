package org.nexo.feedservice.controller;

import lombok.RequiredArgsConstructor;
import org.nexo.feedservice.exception.CustomException;
import org.nexo.feedservice.service.FeedService;
import org.nexo.feedservice.util.SecurityUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class FeedController {
    private final FeedService feedService;
    private final SecurityUtil securityUtil;

    @GetMapping(value = "/posts/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<?> getFeedOfPosts(@PathVariable Long userId,
                                  @RequestParam(defaultValue = "0") int pageNo,
                                  @RequestParam(defaultValue = "20") int pageSize) {
        return securityUtil.getUserIdFromToken()
                .flatMap(currentUserId -> {
                    if (!currentUserId.equals(userId)) {
                        return Mono.error(new CustomException("Don't allow to get feed", HttpStatus.UNAUTHORIZED));
                    }
                    return feedService.getHybridFeed(userId, pageNo, pageSize, true);
                });
    }

    @GetMapping(value = "/reels/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<?> getFeedOfReels(@PathVariable Long userId,
                                  @RequestParam(defaultValue = "0") int pageNo,
                                  @RequestParam(defaultValue = "20") int pageSize) {
        return securityUtil.getUserIdFromToken()
                .flatMap(currentUserId -> {
                    if (!currentUserId.equals(userId)) {
                        return Mono.error(new CustomException("Don't allow to get feed", HttpStatus.UNAUTHORIZED));
                    }
                    return feedService.getHybridFeed(userId, pageNo, pageSize, false);
                });

    }
}
