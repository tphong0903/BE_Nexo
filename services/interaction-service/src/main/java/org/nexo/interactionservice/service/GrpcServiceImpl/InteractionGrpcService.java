package org.nexo.interactionservice.service.GrpcServiceImpl;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.nexo.grpc.interaction.InteractionServiceGrpc;
import org.nexo.grpc.interaction.InteractionServiceOuterClass;
import org.nexo.grpc.user.UserServiceProto;
import org.nexo.interactionservice.repository.ICommentRepository;
import org.nexo.interactionservice.repository.ILikeRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class InteractionGrpcService extends InteractionServiceGrpc.InteractionServiceImplBase {
    private final ILikeRepository likeRepository;
    private final ICommentRepository commentRepository;

    @Override
    public void existLikesByUserAndPostIds(InteractionServiceOuterClass.BatchIsLikeRequest request,
            StreamObserver<InteractionServiceOuterClass.BatchIsLikeResponse> responseObserver) {
        Long userId = request.getUserId();
        List<Long> postIds = request.getPostIdsList();

        List<InteractionServiceOuterClass.LikeResult> results = postIds.stream()
                .map(postId -> {
                    boolean liked = likeRepository.existsByPostIdAndUserId(postId, userId);
                    return InteractionServiceOuterClass.LikeResult.newBuilder()
                            .setPostId(postId)
                            .setIsLike(liked)
                            .build();
                })
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
        List<Long> postIds = request.getPostIdsList();

        List<InteractionServiceOuterClass.LikeResult> results = postIds.stream()
                .map(postId -> {
                    boolean liked = likeRepository.existsByReelIdAndUserId(postId, userId);
                    return InteractionServiceOuterClass.LikeResult.newBuilder()
                            .setPostId(postId)
                            .setIsLike(liked)
                            .build();
                })
                .collect(Collectors.toList());

        InteractionServiceOuterClass.BatchIsLikeResponse response = InteractionServiceOuterClass.BatchIsLikeResponse
                .newBuilder()
                .addAllResults(results)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getTotalInteractions(InteractionServiceOuterClass.Empty request,
            StreamObserver<InteractionServiceOuterClass.QuantityTotalInteract> responseObserver) {
        long total = likeRepository.count() + commentRepository.count();

        InteractionServiceOuterClass.QuantityTotalInteract response = InteractionServiceOuterClass.QuantityTotalInteract
                .newBuilder()
                .setQuantity(total)
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
                + commentRepository.countByCreatedAtBetween(startOfThisMonth, startOfThisMonth.plusMonths(1));

        double percent = 0;
        if (lastMonth > 0) {
            percent = ((double) (thisMonth - lastMonth) / lastMonth) * 100;
        }

        InteractionServiceOuterClass.PercentInteract response = InteractionServiceOuterClass.PercentInteract
                .newBuilder()
                .setPercent(percent)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getInteractionsByTime(InteractionServiceOuterClass.DateRange request,
                                      StreamObserver<InteractionServiceOuterClass.GetUsersByTimeResponse> responseObserver) {
        LocalDateTime start = LocalDate.parse(request.getStartDate()).atStartOfDay();
        LocalDateTime end = LocalDate.parse(request.getEndDate()).atTime(23, 59, 59);
        ;

        List<Object[]> resultLike = likeRepository.countLikesByDate(start, end);
        List<Object[]> resultComment = commentRepository.countCommentsByDate(start, end);

        Map<String, Long> interactionMap = new TreeMap<>();

        for (Object[] row : resultLike) {
            String date = row[0].toString();
            long total = ((Number) row[1]).longValue();
            interactionMap.put(date, interactionMap.getOrDefault(date, 0L) + total);
        }

        for (Object[] row : resultComment) {
            String date = row[0].toString();
            long total = ((Number) row[1]).longValue();
            interactionMap.put(date, interactionMap.getOrDefault(date, 0L) + total);
        }

        InteractionServiceOuterClass.GetUsersByTimeResponse.Builder responseBuilder = InteractionServiceOuterClass.GetUsersByTimeResponse
                .newBuilder();

        for (Map.Entry<String, Long> entry : interactionMap.entrySet()) {
            responseBuilder.addData(
                    InteractionServiceOuterClass.UserCountByDate.newBuilder()
                            .setDate(entry.getKey())
                            .setCount(entry.getValue())
                            .build());
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getUserInteractionsCount(InteractionServiceOuterClass.GetUserInteractionsCountRequest request,
            StreamObserver<InteractionServiceOuterClass.GetUserInteractionsCountResponse> responseObserver) {
        Long userId = request.getUserId();
        long likesCount = likeRepository.countByUserId(userId);
        long commentsCount = commentRepository.countByUserId(userId);

        long totalInteractions = likesCount + commentsCount;

        InteractionServiceOuterClass.GetUserInteractionsCountResponse response = InteractionServiceOuterClass.GetUserInteractionsCountResponse
                .newBuilder()
                .setInteractionsCount(totalInteractions)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
