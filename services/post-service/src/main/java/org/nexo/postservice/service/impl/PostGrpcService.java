package org.nexo.postservice.service.impl;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.nexo.grpc.post.PostServiceGrpc;
import org.nexo.grpc.post.PostServiceOuterClass;
import org.nexo.postservice.dto.response.PostResponseDTO;
import org.nexo.postservice.service.IPostService;

import java.time.ZoneId;
import java.util.List;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class PostGrpcService extends PostServiceGrpc.PostServiceImplBase {
    private final IPostService postService;

    @Override
    public void getPostById(PostServiceOuterClass.GetPostRequest request, StreamObserver<PostServiceOuterClass.PostResponse> responseObserver) {
        try {
            Long id = request.getId();
            PostResponseDTO dto = postService.getPostById(id);
            PostServiceOuterClass.PostResponse response = PostServiceOuterClass.PostResponse.newBuilder()
                    .setPostId(dto.getPostId())
                    .setUserId(dto.getUserId())
                    .setUserName(dto.getUserName() != null ? dto.getUserName() : "")
                    .setAvatarUrl(dto.getAvatarUrl() != null ? dto.getAvatarUrl() : "")
                    .setCaption(dto.getCaption() != null ? dto.getCaption() : "")
                    .setVisibility(dto.getVisibility() != null ? dto.getVisibility() : "")
                    .setTag(dto.getTag() != null ? dto.getTag() : "")
                    .addAllMediaUrl(dto.getMediaUrl() != null ? dto.getMediaUrl() : List.of())
                    .setQuantityLike(dto.getQuantityLike() != null ? dto.getQuantityLike() : 0)
                    .setQuantityComment(dto.getQuantityComment() != null ? dto.getQuantityComment() : 0)
                    .addAllListUserTag(
                            dto.getListUserTag() != null
                                    ? dto.getListUserTag().stream()
                                    .map(tag -> PostServiceOuterClass.UserTag.newBuilder()
                                            .setUserId(tag.getUserId())
                                            .setUserName(tag.getUserName())
                                            .build())
                                    .toList()
                                    : List.of()
                    )
                    .setIsActive(dto.getIsActive() != null ? dto.getIsActive() : false)
                    .setCreatedAt(dto.getCreatedAt() != null
                            ? dto.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            : 0L)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error in getPostById", e);
            responseObserver.onError(e);
        }
    }
}
