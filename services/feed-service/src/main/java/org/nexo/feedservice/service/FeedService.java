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
        PageRequest pageRequest = PageRequest.of(page, limit.intValue());

        return reactiveRedisTemplate.opsForZSet()
                .reverseRange(key, Range.closed(start, end))
                .collectList()
                .flatMap(redisPosts -> {
                    int redisCount = redisPosts.size();
                    log.info("Redis returned {} posts", redisCount);

                    if (redisCount >= limit) {
                        return Flux.fromIterable(redisPosts)
                                .flatMap(postId -> postGrpcClient.getPostByIdAsync(Long.parseLong(postId)))
                                .filter(dto -> dto.getPostId() != 0)
                                .sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                                .collectList()
                                .flatMap(content -> Mono.fromCallable(() -> {
                                            Page<Long> pageResult = feedRepository.findPostIdsByFollowerId(userId, pageRequest);
                                            long totalElements = pageResult.getTotalElements();
                                            int totalPages = pageResult.getTotalPages();
                                            PageModelResponse<PostResponseDTO> pageModelResponse = PageModelResponse.<PostResponseDTO>builder()
                                                    .pageNo(page)
                                                    .pageSize(limit.intValue())
                                                    .totalElements(totalElements)
                                                    .totalPages(totalPages)
                                                    .last(page + 1 >= totalPages)
                                                    .content(content)
                                                    .build();

                                            return ResponseData.builder()
                                                    .status(200)
                                                    .message("Followings retrieved successfully")
                                                    .data(pageModelResponse)
                                                    .build();
                                        }).subscribeOn(Schedulers.boundedElastic())
                                );
                    } else {
                        log.warn("Redis MISS or not enough posts ({} < {}), fallback to DB", redisCount, limit);
                        return Mono.fromCallable(() -> {
                                    return feedRepository.findPostIdsByFollowerId(userId, pageRequest);
                                })
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMapMany(pageResult -> {
                                    List<Long> dbPostIds = pageResult.getContent();
                                    log.info("Fetched {} posts from DB for userId={}", dbPostIds.size(), userId);
                                    return Flux.fromIterable(dbPostIds)
                                            .flatMap(postGrpcClient::getPostByIdAsync);
                                })
                                .filter(dto -> dto.getPostId() != 0)
                                .sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                                .collectList()
                                .flatMap(content -> Mono.fromCallable(() -> {
                                            long totalElements = feedRepository.countPostsByFollowerId(userId);
                                            int totalPages = (int) Math.ceil((double) totalElements / limit);
                                            PageModelResponse<PostResponseDTO> pageModelResponse = PageModelResponse.<PostResponseDTO>builder()
                                                    .pageNo(page)
                                                    .pageSize(limit.intValue())
                                                    .totalElements(totalElements)
                                                    .totalPages(totalPages)
                                                    .last(page + 1 >= totalPages)
                                                    .content(content)
                                                    .build();
                                            return ResponseData.builder()
                                                    .status(200)
                                                    .message("Followings retrieved successfully")
                                                    .data(pageModelResponse)
                                                    .build();
                                        }).subscribeOn(Schedulers.boundedElastic())
                                );
                    }
                });
    }


    public Mono<ResponseData<?>> getLatestReelsFeed(Long userId, int page, Long limit) {
        Long start = page * limit;
        Long end = start + limit - 1;
        String key = "feed:reel:" + userId;

        log.info("Fetching REEL feed for userId={} page={} limit={} -> Redis key={} start={} end={}",
                userId, page, limit, key, start, end);

        PageRequest pageRequest = PageRequest.of(page, limit.intValue());

        return reactiveRedisTemplate.opsForZSet()
                .reverseRange(key, Range.closed(start, end))
                .collectList()
                .flatMap(redisReels -> {
                    int redisCount = redisReels.size();
                    log.info("Redis returned {} reel IDs", redisCount);

                    if (redisCount >= limit) {
                        return Flux.fromIterable(redisReels)
                                .flatMap(reelId -> postGrpcClient.getReelByIdAsync(Long.parseLong(reelId)))
                                .filter(dto -> dto.getReelId() != 0)
                                .sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                                .collectList()
                                .flatMap(content -> Mono.fromCallable(() -> {
                                            Page<Long> pageResult = feedReelRepository.findReelIdsByFollowerId(userId, pageRequest);
                                            long totalElements = pageResult.getTotalElements();
                                            int totalPages = pageResult.getTotalPages();

                                            PageModelResponse<ReelResponseDTO> pageModelResponse =
                                                    PageModelResponse.<ReelResponseDTO>builder()
                                                            .pageNo(page)
                                                            .pageSize(limit.intValue())
                                                            .totalElements(totalElements)
                                                            .totalPages(totalPages)
                                                            .last(page + 1 >= totalPages)
                                                            .content(content)
                                                            .build();

                                            return ResponseData.builder()
                                                    .status(200)
                                                    .message("Reel feed retrieved successfully (from Redis)")
                                                    .data(pageModelResponse)
                                                    .build();
                                        }).subscribeOn(Schedulers.boundedElastic())
                                );
                    } else {
                        log.warn("Redis MISS or insufficient data ({} < {}), fallback to DB", redisCount, limit);

                        return Mono.fromCallable(() -> feedReelRepository.findReelIdsByFollowerId(userId, pageRequest))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMapMany(pageResult -> {
                                    List<Long> dbReelIds = pageResult.getContent();
                                    log.info("Fetched {} reels from DB for userId={}", dbReelIds.size(), userId);
                                    return Flux.fromIterable(dbReelIds)
                                            .flatMap(postGrpcClient::getReelByIdAsync);
                                })
                                .filter(dto -> dto.getReelId() != 0)
                                .sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                                .collectList()
                                .flatMap(content -> Mono.fromCallable(() -> {
                                            long totalElements = feedReelRepository.countFeedReelModelsByFollowerId(userId);
                                            int totalPages = (int) Math.ceil((double) totalElements / limit);

                                            PageModelResponse<ReelResponseDTO> pageModelResponse =
                                                    PageModelResponse.<ReelResponseDTO>builder()
                                                            .pageNo(page)
                                                            .pageSize(limit.intValue())
                                                            .totalElements(totalElements)
                                                            .totalPages(totalPages)
                                                            .last(page + 1 >= totalPages)
                                                            .content(content)
                                                            .build();

                                            return ResponseData.builder()
                                                    .status(200)
                                                    .message("Reel feed retrieved successfully (from DB)")
                                                    .data(pageModelResponse)
                                                    .build();
                                        }).subscribeOn(Schedulers.boundedElastic())
                                );
                    }
                });
    }


}

