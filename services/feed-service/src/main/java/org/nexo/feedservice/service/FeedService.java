package org.nexo.feedservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.feedservice.dto.*;
import org.nexo.feedservice.model.FeedModel;
import org.nexo.feedservice.model.FeedReelModel;
import org.nexo.feedservice.repository.IFeedReelRepository;
import org.nexo.feedservice.repository.IFeedRepository;
import org.nexo.grpc.user.UserServiceProto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedService {

    private static final int MAX_REDIS_FEED_SIZE = 50;
    private static final long KOL_FOLLOWER_THRESHOLD = 10000L;

    private static final long AFFINITY_MULTIPLIER = 3600;

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final UserGrpcClient userClient;
    private final PostGrpcClient postGrpcClient;
    private final IFeedRepository feedRepository;
    private final IFeedReelRepository feedReelRepository;

    String script = """
                redis.call('ZADD', KEYS[1], ARGV[1], ARGV[2])
                redis.call('ZREMRANGEBYRANK', KEYS[1], 0, -ARGV[3]-1)
                return 1
            """;


    public Mono<Void> handleNewPost(Long authorId, Long postId, Long createdAt) {
        return Mono.fromCallable(() -> userClient.countFollowerOfUser(authorId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(followerCount -> {
                    if (followerCount >= KOL_FOLLOWER_THRESHOLD) {
                        return pushToUserOutbox(authorId, postId, createdAt, true);
                    } else {
                        return fanOutToFollowersSmart(authorId, postId, createdAt, true);
                    }
                });
    }

    public Mono<Void> handleNewReel(Long authorId, Long postId, Long createdAt) {
        return Mono.fromCallable(() -> userClient.countFollowerOfUser(authorId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(followerCount -> {
                    if (followerCount >= KOL_FOLLOWER_THRESHOLD) {
                        return pushToUserOutbox(authorId, postId, createdAt, false);
                    } else {
                        return fanOutToFollowersSmart(authorId, postId, createdAt, false);
                    }
                });
    }

    private Mono<Void> pushToUserOutbox(Long authorId, Long postId, Long createdAt, boolean isPost) {
        String key = (isPost ? "user_posts:" : "user_reels:") + authorId;
        log.info("PULL MODE: Saved to outbox for KOL {} -> Redis key: {}", authorId, key);
        return pushToRedisZSet(key, postId, createdAt);
    }

    public Mono<Void> fanOutToFollowersSmart(Long authorId, Long postId, Long createdAt, boolean isPost) {
        return Mono.fromCallable(() -> {
                    List<UserServiceProto.FolloweeInfo> listFriend = new ArrayList<>(
                            userClient.getUserFollowees(authorId).getFolloweesList()
                    );

                    if (isPost) {
                        List<FeedModel> feedModelList = listFriend.stream()
                                .map(friend -> FeedModel.builder().followerId(friend.getUserId()).postId(postId).userId(authorId).build())
                                .collect(Collectors.toCollection(ArrayList::new));
                        feedModelList.add(FeedModel.builder().followerId(authorId).postId(postId).userId(authorId).build());
                        feedRepository.saveAll(feedModelList);
                    } else {
                        List<FeedReelModel> feedModelList = listFriend.stream()
                                .map(friend -> FeedReelModel.builder().followerId(friend.getUserId()).reelId(postId).userId(authorId).build())
                                .collect(Collectors.toCollection(ArrayList::new));
                        feedModelList.add(FeedReelModel.builder().followerId(authorId).reelId(postId).userId(authorId).build());
                        feedReelRepository.saveAll(feedModelList);
                    }

                    listFriend.add(UserServiceProto.FolloweeInfo.newBuilder().setUserId(authorId).build());
                    return listFriend;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .flatMap(followee -> calculateSmartScoreAndPush(followee.getUserId(), authorId, postId, createdAt, isPost))
                .then();
    }

    private Mono<Void> calculateSmartScoreAndPush(Long followerId, Long authorId, Long postId, Long createdAt, boolean isPost) {
        String prefix = isPost ? "feed:" : "feed:reel:";
        String feedKey = prefix + followerId;
        String affinityKey = "affinity:" + followerId;

        if (followerId.equals(authorId)) {
            return pushToRedisZSet(feedKey, postId, createdAt);
        }

        return reactiveRedisTemplate.opsForHash().get(affinityKey, String.valueOf(authorId))
                .map(val -> Long.parseLong(val.toString()))
                .defaultIfEmpty(0L)
                .flatMap(affinityScore -> {
                    long smartScore = createdAt + (affinityScore * AFFINITY_MULTIPLIER * 1000);
                    return pushToRedisZSet(feedKey, postId, smartScore);
                });
    }


    public Mono<ResponseData<?>> getHybridFeed(Long userId, int page, int limit, Boolean isPost) {
        long startOffset = (long) page * limit;
        long endOffset = startOffset + limit - 1;

        if (startOffset >= MAX_REDIS_FEED_SIZE) {
            log.warn("User {} requested page {} which exceeds Redis limit. Fallback to DB.", userId, page);
            return fallbackToDatabase(userId, page, limit, isPost);
        }

        long fetchEnd = Math.min(endOffset, MAX_REDIS_FEED_SIZE);

        String keyFeedRedis1 = isPost ? "feed:" : "feed:reel:";
        String keyFeedRedis2 = isPost ? "user_posts:" : "user_reels:";

        Mono<List<FeedItem>> pushDataStream = fetchFromRedisZSet(keyFeedRedis1 + userId, 0, fetchEnd);

        Mono<List<Long>> followedKolsMono = Mono.fromCallable(() -> userClient.getFollowedKols(userId))
                .subscribeOn(Schedulers.boundedElastic());

        Mono<List<FeedItem>> pullDataStream = followedKolsMono
                .flatMapMany(Flux::fromIterable)
                .flatMap(kolId -> fetchFromRedisZSet(keyFeedRedis2 + kolId, 0, fetchEnd))
                .flatMap(Flux::fromIterable)
                .collectList();

        return Mono.zip(pushDataStream, pullDataStream)
                .map(tuple -> {
                    List<FeedItem> pushItems = tuple.getT1();
                    List<FeedItem> pullItems = tuple.getT2();

                    List<FeedItem> mergedList = new ArrayList<>(pushItems);

                    mergedList.addAll(pullItems);

                    mergedList.sort(Comparator.naturalOrder());

                    return mergedList.stream()
                            .skip(startOffset)
                            .limit(limit)
                            .map(FeedItem::getPostId)
                            .collect(Collectors.toList());
                })
                .flatMap(finalPostIds -> {
                    if (finalPostIds.isEmpty() || finalPostIds.size() < limit) {
                        return fallbackToDatabase(userId, page, limit, isPost);
                    }

                    if (isPost)
                        return postGrpcClient.getPostsByIdsAsync(finalPostIds, userId)
                                .flatMap(posts -> buildPostResponse(posts, page, (long) limit, null));
                    else
                        return postGrpcClient.getReelsByIdsAsync(finalPostIds, userId)
                                .flatMap(reels -> buildReelResponse(reels, page, (long) limit, null));
                });
    }

    private Mono<ResponseData<?>> fallbackToDatabase(Long userId, int page, int limit, boolean isPost) {
        Mono<Page<Long>> dbResultMono = Mono.fromCallable(() -> {
            PageRequest pageRequest = PageRequest.of(page, limit);
            if (isPost) {
                return feedRepository.findPostIdsByFollowerId(userId, pageRequest);
            } else {
                return feedReelRepository.findReelIdsByFollowerId(userId, pageRequest);
            }
        }).subscribeOn(Schedulers.boundedElastic());

        return dbResultMono.flatMap(pageResult -> {
            List<Long> dbItemIds = pageResult.getContent();

            if (dbItemIds.isEmpty()) return Mono.just(createEmptyResponse(page, limit));

            if (isPost) {
                return postGrpcClient.getPostsByIdsAsync(dbItemIds, userId)
                        .flatMap(posts -> buildPostResponse(posts, page, (long) limit, pageResult));
            } else {
                return postGrpcClient.getReelsByIdsAsync(dbItemIds, userId)
                        .flatMap(reels -> buildReelResponse(reels, page, (long) limit, pageResult));
            }
        });
    }

    private Mono<Void> pushToRedisZSet(String key, Long postId, Long smartScore) {
        return reactiveRedisTemplate.execute(
                RedisScript.of(script, Long.class),
                List.of(key),
                List.of(smartScore.toString(), postId.toString(), String.valueOf(MAX_REDIS_FEED_SIZE))
        ).then();
    }

    private Mono<List<FeedItem>> fetchFromRedisZSet(String key, long start, long end) {
        return reactiveRedisTemplate.opsForZSet()
                .reverseRangeWithScores(key, Range.closed(start, end))
                .map(tuple -> new FeedItem(
                        Long.parseLong(tuple.getValue()),
                        tuple.getScore() != null ? tuple.getScore().longValue() : 0L
                ))
                .collectList()
                .onErrorResume(e -> {
                    log.error("Error fetching from Redis key: {}", key, e);
                    return Mono.just(new ArrayList<>());
                });
    }

    private Mono<ResponseData<?>> buildPostResponse(List<PostResponseDTO> posts, int page, Long limit, Page<Long> pageResult) {
        List<PostResponseDTO> sorted = (pageResult != null) ?
                posts.stream().sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())).toList() : posts;
        return createResponseData(sorted, page, limit, pageResult);
    }

    private Mono<ResponseData<?>> buildReelResponse(List<ReelResponseDTO> posts, int page, Long limit, Page<Long> pageResult) {
        List<ReelResponseDTO> sorted = (pageResult != null) ?
                posts.stream().sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())).toList() : posts;
        return createResponseData(sorted, page, limit, pageResult);
    }

    private <T> Mono<ResponseData<?>> createResponseData(List<T> sortedContent, int page, Long limit, Page<Long> pageResult) {
        long totalElements = (pageResult != null) ? pageResult.getTotalElements() : sortedContent.size();
        int totalPages = (pageResult != null) ? pageResult.getTotalPages() : 1;

        PageModelResponse<T> pageModelResponse = PageModelResponse.<T>builder()
                .pageNo(page)
                .pageSize(limit.intValue())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .last(page + 1 >= totalPages)
                .content(sortedContent)
                .build();

        return Mono.just(ResponseData.builder()
                .status(200)
                .message("Feed retrieved successfully")
                .data(pageModelResponse)
                .build());
    }

    private ResponseData<?> createEmptyResponse(int page, int limit) {
        PageModelResponse<Object> emptyPage = PageModelResponse.builder()
                .pageNo(page)
                .pageSize(limit)
                .totalElements(0L)
                .totalPages(0)
                .last(true)
                .content(new ArrayList<>())
                .build();

        return ResponseData.builder().status(200).message("No more feed available").data(emptyPage).build();
    }
}