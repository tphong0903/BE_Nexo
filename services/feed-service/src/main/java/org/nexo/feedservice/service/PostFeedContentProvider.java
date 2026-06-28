package org.nexo.feedservice.service;

import lombok.RequiredArgsConstructor;
import org.nexo.feedservice.dto.PostResponseDTO;
import org.nexo.feedservice.exception.GrpcFeedException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PostFeedContentProvider implements FeedContentProvider<PostResponseDTO> {
    private final PostGrpcClient postGrpcClient;

    @Override
    public FeedContentType type() {
        return FeedContentType.POST;
    }

    @Override
    public Mono<List<PostResponseDTO>> getByIds(List<Long> ids, Long viewerId) {
        return postGrpcClient.getPostsByIdsAsync(ids, viewerId)
                .onErrorMap(error -> new GrpcFeedException("Cannot query posts by ids", error));
    }

    @Override
    public Comparator<PostResponseDTO> newestFirst() {
        return Comparator.comparing(PostResponseDTO::getCreatedAt).reversed();
    }
}
