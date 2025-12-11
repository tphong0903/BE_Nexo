package org.nexo.postservice.service.GrpcServiceImpl.client;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.nexo.grpc.interaction.InteractionServiceGrpc;
import org.nexo.grpc.interaction.InteractionServiceOuterClass;
import org.nexo.grpc.user.UserServiceProto;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InteractionGrpcClient {
    @GrpcClient("interactions")
    private InteractionServiceGrpc.InteractionServiceBlockingStub interactionBlockingStub;


    public Map<Long, Boolean> checkBatchLikesPost(Long userId, List<Long> postIds) {
        InteractionServiceOuterClass.BatchIsLikeRequest request = InteractionServiceOuterClass.BatchIsLikeRequest.newBuilder()
                .setUserId(userId)
                .addAllPostIds(postIds)
                .build();

        InteractionServiceOuterClass.BatchIsLikeResponse response = interactionBlockingStub.existLikesByUserAndPostIds(request);

        return response.getResultsList().stream()
                .collect(Collectors.toMap(InteractionServiceOuterClass.LikeResult::getPostId, InteractionServiceOuterClass.LikeResult::getIsLike));
    }

    public Map<Long, Boolean> checkBatchLikesReel(Long userId, List<Long> postIds) {
        InteractionServiceOuterClass.BatchIsLikeRequest request = InteractionServiceOuterClass.BatchIsLikeRequest.newBuilder()
                .setUserId(userId)
                .addAllPostIds(postIds)
                .build();

        InteractionServiceOuterClass.BatchIsLikeResponse response = interactionBlockingStub.existLikesByUserAndReelIds(request);

        return response.getResultsList().stream()
                .collect(Collectors.toMap(InteractionServiceOuterClass.LikeResult::getPostId, InteractionServiceOuterClass.LikeResult::getIsLike));
    }

    public Long getTotalInteractions() {
        InteractionServiceOuterClass.QuantityTotalInteract response = interactionBlockingStub.getTotalInteractions(InteractionServiceOuterClass.Empty.newBuilder().build());
        return response.getQuantity();
    }

    public Double getPercentInteractionsInThisMonth() {
        InteractionServiceOuterClass.PercentInteract response = interactionBlockingStub.getPercentInteractionsInThisMonth(InteractionServiceOuterClass.Empty.newBuilder().build());
        return response.getPercent();
    }

    public List<InteractionServiceOuterClass.UserCountByDate> getInteractionsByTime(LocalDate startDate, LocalDate endDate) {
        InteractionServiceOuterClass.DateRange request = InteractionServiceOuterClass.DateRange.newBuilder()
                .setStartDate(startDate.toString())
                .setEndDate(endDate.toString())
                .build();

        InteractionServiceOuterClass.GetUsersByTimeResponse response = interactionBlockingStub.getInteractionsByTime(request);

        return response.getDataList();
    }

    public InteractionServiceOuterClass.GetCommentByIdResponse getCommentById(Long id) {
        InteractionServiceOuterClass.GetCommentByIdRequest request = InteractionServiceOuterClass.GetCommentByIdRequest.newBuilder().setCommentId(id).build();
        return interactionBlockingStub.getCommentById(request);
    }

    public boolean deleteCommentById(Long id) {
        InteractionServiceOuterClass.DeleteCommentByIdRequest request = InteractionServiceOuterClass.DeleteCommentByIdRequest.newBuilder().setCommentId(id).build();
        return interactionBlockingStub.deleteCommentById(request).getIsSuccess();
    }
}
