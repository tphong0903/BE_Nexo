package org.nexo.feedservice.controller;

import lombok.RequiredArgsConstructor;
import org.nexo.feedservice.dto.PostResponseDTO;
import org.nexo.feedservice.service.FeedService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class FeedController {
    private final FeedService feedService;

    @GetMapping(value = "/posts/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<?> getFeedOfPosts(@PathVariable Long userId,
                                  @RequestParam(defaultValue = "0") int pageNo,
                                  @RequestParam(defaultValue = "20") Long pageSize) {
        return feedService.getLatestFeed(userId, pageNo, pageSize);
    }

    @GetMapping(value = "/reels/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<?> getFeedOfReels(@PathVariable Long userId,
                                  @RequestParam(defaultValue = "0") int pageNo,
                                  @RequestParam(defaultValue = "20") Long pageSize) {
        return feedService.getLatestReelsFeed(userId, pageNo, pageSize);
    }
}
