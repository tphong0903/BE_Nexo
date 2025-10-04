package org.nexo.feedservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.feedservice.dto.PostResponseDTO;
import org.nexo.feedservice.model.FeedModel;
import org.nexo.feedservice.repository.IFeedRepository;
import org.nexo.grpc.user.UserServiceProto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedService {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final UserGrpcClient userClient;
    private final PostGrpcClient postGrpcClient;
    private final IFeedRepository feedRepository;

    public Mono<Void> handleNewPost(Long authorId, Long postId, Long createdAt) {
        List<UserServiceProto.FolloweeInfo> listFriend = userClient.getUserFollowees(authorId).getFolloweesList();
        List<FeedModel> feedModelList = new ArrayList<>();
        for (UserServiceProto.FolloweeInfo model : listFriend) {
            feedModelList.add(FeedModel.builder().followerId(model.getUserId()).postId(postId).userId(authorId).build());
        }
        feedRepository.saveAll(feedModelList);
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
        Long start = page * limit;
        Long end = start + limit - 1;
        String key = "feed:" + userId;
        log.info("Fetching feed for userId={} page={} limit={} -> Redis key={} start={} end={}",
                userId, page, limit, key, start, end);
        return reactiveRedisTemplate.opsForZSet()
                .reverseRange(key, Range.closed(start, end))
                .collectList()
                .publishOn(Schedulers.boundedElastic())
                .flatMapMany(posts -> {
                    if (!posts.isEmpty()) {
                        log.info("Redis HIT: found {} posts for userId={}", posts.size(), userId);
                        return Flux.fromIterable(posts)
                                .flatMap(postId -> {
                                    log.info("Fetching post details from gRPC for postId={}", postId);
                                    return postGrpcClient.getPostByIdAsync(Long.parseLong(postId));
                                })
                                .filter(dto -> dto.getPostId() != 0);
                    } else {
                        log.warn("Redis MISS: no posts found for userId={}, fallback to DB", userId);
                        return Mono.fromCallable(() -> {
                                    PageRequest pageRequest = PageRequest.of(page, limit.intValue());
                                    return feedRepository.findPostIdsByFollowerId(userId, pageRequest);
                                })
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMapMany(list -> {
                                    log.info("Fetched {} posts from DB for userId={}", list.size(), userId);
                                    return Flux.fromIterable(list)
                                            .flatMap(postId -> {
                                                log.debug("Fetching post details from gRPC for postId={}", postId);
                                                return postGrpcClient.getPostByIdAsync(postId);
                                            });
                                });
                    }
                });
    }

    public Flux<PostResponseDTO> getLatestReelsFeed(Long userId, int page, Long limit) {
        Long start = page * limit;
        Long end = start + limit - 1;
        String key = "feed:" + userId;
        log.info("Fetching feed for userId={} page={} limit={} -> Redis key={} start={} end={}",
                userId, page, limit, key, start, end);
        return reactiveRedisTemplate.opsForZSet()
                .reverseRange(key, Range.closed(start, end))
                .collectList()
                .publishOn(Schedulers.boundedElastic())
                .flatMapMany(posts -> {
                    if (!posts.isEmpty()) {
                        log.info("Redis HIT: found {} posts for userId={}", posts.size(), userId);
                        return Flux.fromIterable(posts)
                                .flatMap(postId -> {
                                    log.info("Fetching post details from gRPC for postId={}", postId);
                                    return postGrpcClient.getPostByIdAsync(Long.parseLong(postId));
                                });
                    } else {
                        log.warn("Redis MISS: no posts found for userId={}, fallback to DB", userId);
                        return Mono.fromCallable(() -> {
                                    PageRequest pageRequest = PageRequest.of(page, limit.intValue());
                                    return feedRepository.findPostIdsByFollowerId(userId, pageRequest);
                                })
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMapMany(list -> {
                                    log.info("Fetched {} posts from DB for userId={}", list.size(), userId);
                                    return Flux.fromIterable(list)
                                            .flatMap(postId -> {
                                                log.debug("Fetching post details from gRPC for postId={}", postId);
                                                return postGrpcClient.getPostByIdAsync(postId);
                                            });
                                });
                    }
                });

    }
}

