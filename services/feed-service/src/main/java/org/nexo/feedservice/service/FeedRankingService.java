package org.nexo.feedservice.service;

import lombok.RequiredArgsConstructor;
import org.nexo.feedservice.config.FeedProperties;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class FeedRankingService {
    private final FeedProperties feedProperties;
    private final AffinityService affinityService;

    public Mono<Long> scoreFor(Long followerId, Long authorId, Long createdAt) {
        if (followerId.equals(authorId)) {
            return Mono.just(createdAt);
        }

        return affinityService.getAffinityScore(followerId, authorId)
                .map(affinityScore -> createdAt
                        + affinityScore * feedProperties.getAffinityMultiplierSeconds() * 1_000);
    }
}
