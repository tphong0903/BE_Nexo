package org.nexo.feedservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.feedservice.dto.PageModelResponse;
import org.nexo.feedservice.dto.PostResponseDTO;
import org.nexo.feedservice.dto.ReelResponseDTO;
import org.nexo.feedservice.dto.ResponseData;
import org.nexo.feedservice.model.FeedModel;
import org.nexo.feedservice.model.FeedReelModel;
import org.nexo.feedservice.repository.IFeedReelRepository;
import org.nexo.feedservice.repository.IFeedRepository;
import org.nexo.grpc.user.UserServiceProto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Range;
import org.springframework.data.domain.Sort;
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
    private final IFeedReelRepository feedReelRepository;

    public Mono<Void> handleNewPost(Long authorId, Long postId, Long createdAt) {
        List<UserServiceProto.FolloweeInfo> listFriend = new ArrayList<>(
                userClient.getUserFollowees(authorId).getFolloweesList()
        );
        List<FeedModel> feedModelList = new ArrayList<>();
        for (UserServiceProto.FolloweeInfo model : listFriend) {
            feedModelList.add(FeedModel.builder().followerId(model.getUserId()).postId(postId).userId(authorId).build());
        }
        feedModelList.add(FeedModel.builder().followerId(authorId).postId(postId).userId(authorId).build());
        listFriend.add(UserServiceProto.FolloweeInfo.newBuilder().setUserId(authorId).setUserName("").setAvatar("").build());
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

    public Mono<Void> handleNewReel(Long authorId, Long postId, Long createdAt) {
        List<UserServiceProto.FolloweeInfo> listFriend = new ArrayList<>(
                userClient.getUserFollowees(authorId).getFolloweesList()
        );
        List<FeedReelModel> feedModelList = new ArrayList<>();
        for (UserServiceProto.FolloweeInfo model : listFriend) {
            feedModelList.add(FeedReelModel.builder().followerId(model.getUserId()).reelId(postId).userId(authorId).build());
        }
        feedModelList.add(FeedReelModel.builder().followerId(authorId).reelId(postId).userId(authorId).build());
        listFriend.add(UserServiceProto.FolloweeInfo.newBuilder().setUserId(authorId).setUserName("").setAvatar("").build());
        feedReelRepository.saveAll(feedModelList);
        return Flux.fromIterable(listFriend)
                .flatMap(followeeInfo -> {
                    Long followerId = followeeInfo.getUserId();
                    String key = "feed:reel:" + followerId;
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

    public Mono<ResponseData<?>> getLatestFeed(Long userId, int page, Long limit) {
        Long start = page * limit;
        Long end = start + limit - 1;
        String key = "feed:" + userId;

        log.info("Fetching feed for userId={} page={} limit={} -> Redis key={} start={} end={}",
                userId, page, limit, key, start, end);

        Mono<Page<Long>> pageResultMono = Mono.fromCallable(() -> {
            PageRequest pageRequest = PageRequest.of(page, limit.intValue());
            return feedRepository.findPostIdsByFollowerId(userId, pageRequest);
        }).subscribeOn(Schedulers.boundedElastic());

        return reactiveRedisTemplate.opsForZSet()
                .reverseRange(key, Range.closed(start, end))
                .collectList()
                .flatMap(redisPosts -> pageResultMono.flatMap(pageResult -> {

                    List<Long> postIds = redisPosts.stream()
                            .map(Long::parseLong)
                            .toList();
                    int redisCount = postIds.size();
                    log.info("Redis returned {} posts", redisCount);

                    if (redisCount >= limit) {
                        return postGrpcClient.getPostsByIdsAsync(postIds, userId)
                                .flatMap(posts -> buildPostResponse(posts, page, limit, pageResult));
                    } else {
                        log.warn("Redis MISS or not enough posts ({} < {}), fallback to DB", redisCount, limit);
                        List<Long> dbPostIds = pageResult.getContent();
                        log.info("Fetched {} posts from DB for userId={}", dbPostIds.size(), userId);
                        return postGrpcClient.getPostsByIdsAsync(dbPostIds, userId)
                                .flatMap(posts -> buildPostResponse(posts, page, limit, pageResult));
                    }
                }));
    }


    public Mono<ResponseData<?>> getLatestReelsFeed(Long userId, int page, Long limit) {
        Long start = page * limit;
        Long end = start + limit - 1;
        String key = "feed:reel:" + userId;

        log.info("Fetching REEL feed for userId={} page={} limit={} -> Redis key={} start={} end={}",
                userId, page, limit, key, start, end);

        Mono<Page<Long>> pageResultMono = Mono.fromCallable(() -> {
            PageRequest pageRequest = PageRequest.of(page, limit.intValue());
            return feedReelRepository.findReelIdsByFollowerId(userId, pageRequest);
        }).subscribeOn(Schedulers.boundedElastic());

        return reactiveRedisTemplate.opsForZSet()
                .reverseRange(key, Range.closed(start, end))
                .collectList()
                .flatMap(redisReels -> pageResultMono.flatMap(pageResult -> {
                    List<Long> reelIds = redisReels.stream()
                            .map(Long::parseLong)
                            .toList();
                    int redisCount = redisReels.size();
                    log.info("Redis returned {} reel IDs", redisCount);

                    if (redisCount >= limit) {
                        return postGrpcClient.getReelsByIdsAsync(reelIds, userId)
                                .flatMap(posts -> buildReelResponse(posts, page, limit, pageResult));
                    } else {
                        log.warn("Redis MISS or insufficient data ({} < {}), fallback to DB", redisCount, limit);
                        List<Long> dbPostIds = pageResult.getContent();
                        log.info("Fetched {} posts from DB for userId={}", dbPostIds.size(), userId);
                        return postGrpcClient.getReelsByIdsAsync(dbPostIds, userId)
                                .flatMap(posts -> buildReelResponse(posts, page, limit, pageResult));
                    }
                }));
    }

    private Mono<ResponseData<?>> buildPostResponse(List<PostResponseDTO> posts, int page, Long limit, Page<Long> pageResult) {
        List<PostResponseDTO> sorted = posts.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();

        long totalElements = (pageResult != null) ? pageResult.getTotalElements() : sorted.size();
        int totalPages = (pageResult != null) ? pageResult.getTotalPages() : 1;

        PageModelResponse<PostResponseDTO> pageModelResponse = PageModelResponse.<PostResponseDTO>builder()
                .pageNo(page)
                .pageSize(limit.intValue())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .last(page + 1 >= totalPages)
                .content(sorted)
                .build();

        return Mono.just(ResponseData.builder()
                .status(200)
                .message("Feed retrieved successfully")
                .data(pageModelResponse)
                .build());
    }

    private Mono<ResponseData<?>> buildReelResponse(List<ReelResponseDTO> posts, int page, Long limit, Page<Long> pageResult) {
        List<ReelResponseDTO> sorted = posts.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
        long totalElements = pageResult.getTotalElements();
        int totalPages = pageResult.getTotalPages();
        PageModelResponse<ReelResponseDTO> pageModelResponse = PageModelResponse.<ReelResponseDTO>builder()
                .pageNo(page)
                .pageSize(limit.intValue())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .last(page + 1 >= totalPages)
                .content(sorted)
                .build();

        return Mono.just(ResponseData.builder()
                .status(200)
                .message("Feed retrieved successfully")
                .data(pageModelResponse)
                .build());
    }

}

