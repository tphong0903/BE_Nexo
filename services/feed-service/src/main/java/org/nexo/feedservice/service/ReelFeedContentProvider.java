package org.nexo.feedservice.service;

import lombok.RequiredArgsConstructor;
import org.nexo.feedservice.dto.ReelResponseDTO;
import org.nexo.feedservice.exception.GrpcFeedException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReelFeedContentProvider implements FeedContentProvider<ReelResponseDTO> {
    private final PostGrpcClient postGrpcClient;

    @Override
    public FeedContentType type() {
        return FeedContentType.REEL;
    }

    @Override
    public Mono<List<ReelResponseDTO>> getByIds(List<Long> ids, Long viewerId) {
        return postGrpcClient.getReelsByIdsAsync(ids, viewerId)
                .onErrorMap(error -> new GrpcFeedException("Cannot query reels by ids", error));
    }

    @Override
    public Comparator<ReelResponseDTO> newestFirst() {
        return Comparator.comparing(ReelResponseDTO::getCreatedAt).reversed();
    }
}
