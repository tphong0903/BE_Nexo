package org.nexo.feedservice.service;

import lombok.RequiredArgsConstructor;
import org.nexo.feedservice.dto.ResponseData;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class FeedService {
    private final FeedFanOutService feedFanOutService;
    private final FeedQueryService feedQueryService;

    public Mono<Void> handleNewPost(Long authorId, Long postId, Long createdAt) {
        return feedFanOutService.handleNewItem(FeedContentType.POST, authorId, postId, createdAt);
    }

    public Mono<Void> handleNewReel(Long authorId, Long postId, Long createdAt) {
        return feedFanOutService.handleNewItem(FeedContentType.REEL, authorId, postId, createdAt);
    }

    public Mono<Void> fanOutToFollowersSmart(Long authorId, Long postId, Long createdAt, boolean isPost) {
        return feedFanOutService.fanOutToFollowers(FeedContentType.from(isPost), authorId, postId, createdAt);
    }

    public Mono<ResponseData<?>> getHybridFeed(Long userId, int page, int limit, Boolean isPost) {
        return feedQueryService.getHybridFeed(userId, page, limit, FeedContentType.from(isPost));
    }
}
