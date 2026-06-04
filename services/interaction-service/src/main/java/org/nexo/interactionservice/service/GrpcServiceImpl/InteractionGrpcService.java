package org.nexo.interactionservice.service.GrpcServiceImpl;

import io.grpc.stub.StreamObserver;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.nexo.grpc.interaction.InteractionServiceGrpc;
import org.nexo.grpc.interaction.InteractionServiceOuterClass;
import org.nexo.interactionservice.model.CommentModel;
import org.nexo.interactionservice.repository.ICommentRepository;
import org.nexo.interactionservice.repository.ILikeRepository;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class InteractionGrpcService extends InteractionServiceGrpc.InteractionServiceImplBase {
    private static final Duration CACHE_TTL = Duration.ofDays(7);
    private final ILikeRepository likeRepository;
    private final ICommentRepository commentRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void existLikesByUserAndPostIds(InteractionServiceOuterClass.BatchIsLikeRequest request,
                                           StreamObserver<InteractionServiceOuterClass.BatchIsLikeResponse> responseObserver) {
        Long userId = request.getUserId();
        List<Long> postIds = request.getPostIdsList();

        Set<Long> likedPostIds = likeRepository.findPostIdsByUserIdAndPostIdIn(userId, postIds);

        List<InteractionServiceOuterClass.LikeResult> results = postIds.stream()
                .map(postId -> InteractionServiceOuterClass.LikeResult.newBuilder()
                        .setPostId(postId)
                        .setIsLike(likedPostIds.contains(postId))
                        .build())
                .collect(Collectors.toList());

        InteractionServiceOuterClass.BatchIsLikeResponse response = InteractionServiceOuterClass.BatchIsLikeResponse
                .newBuilder()
                .addAllResults(results)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void existLikesByUserAndReelIds(InteractionServiceOuterClass.BatchIsLikeRequest request,
                                           StreamObserver<InteractionServiceOuterClass.BatchIsLikeResponse> responseObserver) {
        Long userId = request.getUserId();
        List<Long> reelIds = request.getPostIdsList();

        Set<Long> likedReelIds = likeRepository.findReelIdsByUserIdAndReelIdIn(userId, reelIds);

        List<InteractionServiceOuterClass.LikeResult> results = reelIds.stream()
                .map(reelId -> InteractionServiceOuterClass.LikeResult.newBuilder()
                        .setPostId(reelId)
                        .setIsLike(likedReelIds.contains(reelId))
                        .build())
                .collect(Collectors.toList());

        responseObserver.onNext(InteractionServiceOuterClass.BatchIsLikeResponse.newBuilder().addAllResults(results).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getTotalInteractions(InteractionServiceOuterClass.Empty request,
                                     StreamObserver<InteractionServiceOuterClass.QuantityTotalInteract> responseObserver) {

        long totalLikes = getCounterWithFallback("global:likes:total", () -> likeRepository.count());
        long totalComments = getCounterWithFallback("global:comments:total", () -> commentRepository.count());

        InteractionServiceOuterClass.QuantityTotalInteract response = InteractionServiceOuterClass.QuantityTotalInteract
                .newBuilder()
                .setQuantity(totalLikes + totalComments)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getPercentInteractionsInThisMonth(InteractionServiceOuterClass.Empty request,
                                                  StreamObserver<InteractionServiceOuterClass.PercentInteract> responseObserver) {
        LocalDateTime startOfThisMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfLastMonth = startOfThisMonth.minusMonths(1);
        LocalDateTime endOfLastMonth = startOfThisMonth;

        long thisMonth = likeRepository.countByCreatedAtBetween(startOfThisMonth, startOfThisMonth.plusMonths(1))
                + commentRepository.countByCreatedAtBetween(startOfThisMonth, startOfThisMonth.plusMonths(1));

        long lastMonth = likeRepository.countByCreatedAtBetween(startOfLastMonth, endOfLastMonth)
                + commentRepository.countByCreatedAtBetween(startOfLastMonth, endOfLastMonth);

        double percent = 0;
        if (lastMonth > 0) {
            percent = ((double) (thisMonth - lastMonth) / lastMonth) * 100;
        }

        responseObserver.onNext(InteractionServiceOuterClass.PercentInteract.newBuilder().setPercent(percent).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getInteractionsByTime(InteractionServiceOuterClass.DateRange request,
                                      StreamObserver<InteractionServiceOuterClass.GetUsersByTimeResponse> responseObserver) {
        LocalDateTime start = LocalDate.parse(request.getStartDate()).atStartOfDay();
        LocalDateTime end = LocalDate.parse(request.getEndDate()).atTime(23, 59, 59);

        Map<String, Long> interactionMap = new TreeMap<>();

        mergeQueryResultToMap(likeRepository.countLikesByDate(start, end), interactionMap);
        mergeQueryResultToMap(commentRepository.countCommentsByDate(start, end), interactionMap);

        InteractionServiceOuterClass.GetUsersByTimeResponse.Builder responseBuilder =
                InteractionServiceOuterClass.GetUsersByTimeResponse.newBuilder();

        interactionMap.forEach((date, count) ->
                responseBuilder.addData(
                        InteractionServiceOuterClass.UserCountByDate.newBuilder()
                                .setDate(date)
                                .setCount(count)
                                .build()
                )
        );

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getUserInteractionsCount(InteractionServiceOuterClass.GetUserInteractionsCountRequest request,
                                         StreamObserver<InteractionServiceOuterClass.GetUserInteractionsCountResponse> responseObserver) {
        Long userId = request.getUserId();

        long likesCount = getCounterWithFallback("user:" + userId + ":likes:total", () -> likeRepository.countByUserId(userId));
        long commentsCount = getCounterWithFallback("user:" + userId + ":comments:total", () -> commentRepository.countByUserId(userId));

        InteractionServiceOuterClass.GetUserInteractionsCountResponse response =
                InteractionServiceOuterClass.GetUserInteractionsCountResponse.newBuilder()
                        .setInteractionsCount(likesCount + commentsCount)
                        .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getCommentById(InteractionServiceOuterClass.GetCommentByIdRequest request,
                               StreamObserver<InteractionServiceOuterClass.GetCommentByIdResponse> responseObserver) {

        Long commentId = request.getCommentId();
        InteractionServiceOuterClass.GetCommentByIdResponse.Builder response =
                InteractionServiceOuterClass.GetCommentByIdResponse.newBuilder();

        CommentModel commentModel = getCommentWithCache(commentId);

        if (commentModel != null) {
            response.setCommentId(commentModel.getId());
            response.setContent(commentModel.getContent());
            response.setUserId(commentModel.getUserId());

            if (commentModel.getPostId() != null) {
                response.setPostId(commentModel.getPostId());
                response.setReelId(0L);
            } else {
                response.setReelId(commentModel.getReelId() != null ? commentModel.getReelId() : 0L);
                response.setPostId(0L);
            }
        } else {
            response.setCommentId(0L);
        }

        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void deleteCommentById(InteractionServiceOuterClass.DeleteCommentByIdRequest request,
                                  StreamObserver<InteractionServiceOuterClass.DeleteCommentByIdResponse> responseObserver) {

        InteractionServiceOuterClass.DeleteCommentByIdResponse.Builder response =
                InteractionServiceOuterClass.DeleteCommentByIdResponse.newBuilder();

        commentRepository.findById(request.getCommentId()).ifPresentOrElse(
                comment -> {
                    commentRepository.delete(comment);
                    redisTemplate.delete("comment_cache:" + comment.getId());
                    response.setIsSuccess(true);
                },
                () -> response.setIsSuccess(false)
        );

        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    private void mergeQueryResultToMap(List<Object[]> queryResult, Map<String, Long> map) {
        for (Object[] row : queryResult) {
            String date = row[0].toString();
            long count = ((Number) row[1]).longValue();
            map.merge(date, count, Long::sum);
        }
    }

    private long getCounterWithFallback(String key, java.util.function.Supplier<Long> dbFallback) {
        Object cachedValue = redisTemplate.opsForValue().get(key);
        if (cachedValue != null) {
            return Long.parseLong(cachedValue.toString());
        }
        long dbValue = dbFallback.get();
        redisTemplate.opsForValue().set(key, String.valueOf(dbValue));
        return dbValue;
    }

    private CommentModel getCommentWithCache(Long commentId) {
//        String cacheKey = "comment_cache:" + commentId;
//        CommentModel cachedComment = (CommentModel) redisTemplate.opsForValue().get(cacheKey);
//
//        if (cachedComment != null) return cachedComment;

        CommentModel dbComment = commentRepository.findById(commentId).orElse(null);
//        if (dbComment != null) {
//            redisTemplate.opsForValue().set(cacheKey, dbComment, CACHE_TTL);
//        }
        return dbComment;
    }
}
