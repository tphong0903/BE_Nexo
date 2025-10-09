package org.nexo.feedservice.service;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.nexo.feedservice.dto.PostResponseDTO;
import org.nexo.feedservice.dto.UserTagDTO;
import org.nexo.grpc.post.PostServiceGrpc;
import org.nexo.grpc.post.PostServiceOuterClass;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneId;

@Service
public class PostGrpcClient {
    @GrpcClient("posts")
    private PostServiceGrpc.PostServiceStub postStub;

    public Mono<PostResponseDTO> getPostByIdAsync(Long postId) {
        return Mono.create(sink -> {
            var request = PostServiceOuterClass.GetPostRequest.newBuilder()
                    .setId(postId)
                    .build();

            postStub.getPostById(request, new StreamObserver<PostServiceOuterClass.PostResponse>() {
                @Override
                public void onNext(PostServiceOuterClass.PostResponse response) {
                    PostResponseDTO dto = convertToDTO(response);
                    sink.success(dto);
                }

                @Override
                public void onError(Throwable t) {
                    sink.error(t);
                }

                @Override
                public void onCompleted() {

                }
            });
        });
    }

    private PostResponseDTO convertToDTO(PostServiceOuterClass.PostResponse response) {
        return PostResponseDTO.builder()
                .postId(response.getPostId())
                .userId(response.getUserId())
                .userName(response.getUserName())
                .avatarUrl(response.getAvatarUrl())
                .caption(response.getCaption())
                .visibility(response.getVisibility())
                .tag(response.getTag())
                .mediaUrl(response.getMediaUrlList())
                .quantityLike(response.getQuantityLike())
                .quantityComment(response.getQuantityComment())
                .listUserTag(
                        response.getListUserTagList().stream()
                                .map(tag -> new UserTagDTO(
                                        tag.getUserId(),
                                        tag.getUserName()
                                ))
                                .toList()
                )
                .isActive(response.getIsActive())
                .createdAt(
                        Instant.ofEpochMilli(response.getCreatedAt())
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime()
                )
                .updatedAt(Instant.ofEpochMilli(response.getUpdateAt())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime())
                .build();
    }
}
