package org.nexo.feedservice.controller;

import lombok.RequiredArgsConstructor;
import org.nexo.feedservice.dto.PostResponseDTO;
import org.nexo.feedservice.service.FeedService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class FeedController {
    private final FeedService feedService;

    @GetMapping(value = "/posts/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<PostResponseDTO> getFeedOfPosts(@PathVariable Long userId,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") Long limit) {
        return feedService.getLatestFeed(userId, page, limit);
    }

    @GetMapping(value = "/reels/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<PostResponseDTO> getFeedOfReels(@PathVariable Long userId,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") Long limit) {
        return feedService.getLatestFeed(userId, page, limit);
    }
}
