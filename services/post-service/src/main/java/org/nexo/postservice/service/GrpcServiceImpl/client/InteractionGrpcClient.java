package org.nexo.postservice.service.GrpcServiceImpl.client;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.nexo.grpc.interaction.InteractionServiceGrpc;
import org.nexo.grpc.interaction.InteractionServiceOuterClass;
import org.springframework.stereotype.Service;

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
}
