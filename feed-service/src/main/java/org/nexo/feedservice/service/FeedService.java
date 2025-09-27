package org.nexo.feedservice.service;

import lombok.RequiredArgsConstructor;
import org.nexo.feedservice.dto.PostResponseDTO;
import org.nexo.grpc.user.UserServiceProto;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final UserGrpcClient userClient;
    private final PostGrpcClient postGrpcClient;

    public Mono<Void> handleNewPost(Long authorId, Long postId, Long createdAt) {
        List<UserServiceProto.FolloweeInfo> listFriend = userClient.getUserFollowees(authorId).getFolloweesList();
        return Flux.fromIterable(listFriend)
                .flatMap(followeeInfo -> {
                    Long followerId = followeeInfo.getUserId();
                    String key = "feed:" + followerId;
                    return reactiveRedisTemplate.opsForZSet()
                            .add(key, postId.toString(), createdAt)
                            .then(
                                    reactiveRedisTemplate.opsForZSet()
                                            .removeRange(key, Range.closed(0L, -51L))
                            )
                            .then();
                })
                .then();
    }

    public Flux<PostResponseDTO> getLatestFeed(Long userId, int page, Long limit) {
        long start = (long) page * limit;
        long end = start + limit - 1;
        String key = "feed:" + userId;
        return reactiveRedisTemplate.opsForZSet()
                .reverseRange(key, Range.closed(start, end))
                .collectList()
                .flatMapMany(posts -> {
                    if (!posts.isEmpty()) {
                        return Flux.fromIterable(posts)
                                .flatMap(postId ->
                                        Mono.fromCallable(() ->
                                                postGrpcClient.getPostById(Long.parseLong(postId))
                                        )
                                );
                    } else {
                        return Flux.empty();
//                        return feedRepository.findByUserId(userId, offset, limit)
//                                .flatMap(feed -> postGrpcClient.getPostById(feed.getPostId()));
                    }
                });

    }
}

