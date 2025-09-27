package org.nexo.feedservice.controller;

import lombok.RequiredArgsConstructor;
import org.nexo.feedservice.dto.PostResponseDTO;
import org.nexo.feedservice.service.FeedService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping()
@RequiredArgsConstructor
public class FeedController {
    private final FeedService feedService;

    @GetMapping(value = "/{userId}", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<PostResponseDTO> getFeed(@PathVariable Long userId,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") Long limit) {
        return feedService.getLatestFeed(userId, page, limit);
    }
}
