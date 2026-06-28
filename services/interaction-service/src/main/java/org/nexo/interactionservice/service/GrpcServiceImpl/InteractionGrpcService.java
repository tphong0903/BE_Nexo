package org.nexo.interactionservice.service.GrpcServiceImpl;

import io.grpc.stub.StreamObserver;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.nexo.grpc.interaction.InteractionServiceGrpc;
import org.nexo.grpc.interaction.InteractionServiceOuterClass;
import org.nexo.interactionservice.cache.CacheKeys;
import org.nexo.interactionservice.cache.InteractionCacheService;
import org.nexo.interactionservice.model.CommentModel;
import org.nexo.interactionservice.repository.ICommentRepository;
import org.nexo.interactionservice.repository.ILikeRepository;
import org.springframework.data.redis.core.RedisTemplate;

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
    private final ILikeRepository likeRepository;
    private final ICommentRepository commentRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final InteractionCacheService cacheService;

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

        long totalLikes = cacheService.getGlobalCounterWithFallback("likes", () -> likeRepository.count());
        long totalComments = cacheService.getGlobalCounterWithFallback("comments", () -> commentRepository.count());

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

        long likesCount = cacheService.getUserCounterWithFallback(userId, "likes", () -> likeRepository.countByUserId(userId));
        long commentsCount = cacheService.getUserCounterWithFallback(userId, "comments", () -> commentRepository.countByUserId(userId));

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
                    invalidateDeletedComment(comment);
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

    private CommentModel getCommentWithCache(Long commentId) {
        return commentRepository.findById(commentId).orElse(null);
    }

    private void invalidateDeletedComment(CommentModel comment) {
        cacheService.invalidateCommentLists(comment);
        redisTemplate.delete(CacheKeys.comment(comment.getId()));
        cacheService.incrementGlobalCounter("comments", -1L);
        cacheService.incrementUserCounter(comment.getUserId(), "comments", -1L);

        if (comment.getParentComment() != null) {
            cacheService.incrementCounter("comment", comment.getParentComment().getId(), "replies", -1L);
        } else if (comment.getPostId() != null) {
            cacheService.incrementCounter("post", comment.getPostId(), "comments", -1L);
        } else if (comment.getReelId() != null) {
            cacheService.incrementCounter("reel", comment.getReelId(), "comments", -1L);
        }
    }
}
