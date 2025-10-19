package org.nexo.interactionservice.service.GrpcServiceImpl;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.nexo.grpc.interaction.InteractionServiceGrpc;
import org.nexo.grpc.interaction.InteractionServiceOuterClass;
import org.nexo.interactionservice.repository.ILikeRepository;

import java.util.List;
import java.util.stream.Collectors;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class InteractionGrpcService extends InteractionServiceGrpc.InteractionServiceImplBase {
    private final ILikeRepository likeRepository;

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

        InteractionServiceOuterClass.BatchIsLikeResponse response = InteractionServiceOuterClass.BatchIsLikeResponse.newBuilder()
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

        InteractionServiceOuterClass.BatchIsLikeResponse response = InteractionServiceOuterClass.BatchIsLikeResponse.newBuilder()
                .addAllResults(results)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
