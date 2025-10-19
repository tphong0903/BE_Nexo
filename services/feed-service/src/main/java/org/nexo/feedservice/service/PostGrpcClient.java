package org.nexo.feedservice.service;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.nexo.feedservice.dto.PostResponseDTO;
import org.nexo.feedservice.dto.ReelResponseDTO;
import org.nexo.feedservice.dto.UserTagDTO;
import org.nexo.grpc.post.PostServiceGrpc;
import org.nexo.grpc.post.PostServiceOuterClass;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

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

    public Mono<ReelResponseDTO> getReelByIdAsync(Long postId) {
        return Mono.create(sink -> {
            var request = PostServiceOuterClass.GetPostRequest.newBuilder()
                    .setId(postId)
                    .build();

            postStub.getReelById(request, new StreamObserver<PostServiceOuterClass.ReelResponse>() {
                @Override
                public void onNext(PostServiceOuterClass.ReelResponse response) {
                    ReelResponseDTO dto = convertToDTO(response);
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

    public Mono<List<PostResponseDTO>> getPostsByIdsAsync(List<Long> postIds, Long viewerId) {
        return Mono.<List<PostResponseDTO>>create(sink -> {
            PostServiceOuterClass.GetPostsByIdsRequest request = PostServiceOuterClass.GetPostsByIdsRequest
                    .newBuilder()
                    .addAllPostIds(postIds)
                    .setUserId(viewerId)
                    .build();

            postStub.getPostsByIds(request, new StreamObserver<PostServiceOuterClass.GetPostsByIdsResponse>() {
                @Override
                public void onNext(PostServiceOuterClass.GetPostsByIdsResponse response) {
                    List<PostResponseDTO> dtos = response.getPostsList().stream()
                            .map(PostGrpcClient.this::convertToDTO)
                            .toList();
                    sink.success(dtos);
                }

                @Override
                public void onError(Throwable t) {
                    sink.error(t);
                }

                @Override
                public void onCompleted() {
                }
            });
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<List<ReelResponseDTO>> getReelsByIdsAsync(List<Long> postIds, Long viewerId) {
        return Mono.<List<ReelResponseDTO>>create(sink -> {
            PostServiceOuterClass.GetPostsByIdsRequest request = PostServiceOuterClass.GetPostsByIdsRequest
                    .newBuilder()
                    .addAllPostIds(postIds)
                    .setUserId(viewerId)
                    .build();

            postStub.getReelsByIds(request, new StreamObserver<PostServiceOuterClass.GetReelsByIdsResponse>() {
                @Override
                public void onNext(PostServiceOuterClass.GetReelsByIdsResponse response) {
                    List<ReelResponseDTO> dtos = response.getReelsList().stream()
                            .map(PostGrpcClient.this::convertToDTO)
                            .toList();
                    sink.success(dtos);
                }

                @Override
                public void onError(Throwable t) {
                    sink.error(t);
                }

                @Override
                public void onCompleted() {
                }
            });
        }).subscribeOn(Schedulers.boundedElastic());
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
                .isLike(response.getIsLike())
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

    private ReelResponseDTO convertToDTO(PostServiceOuterClass.ReelResponse response) {
        return ReelResponseDTO.builder()
                .reelId(response.getPostId())
                .userId(response.getUserId())
                .userName(response.getUserName())
                .avatarUrl(response.getAvatarUrl())
                .caption(response.getCaption())
                .visibility(response.getVisibility())
                .listUserTag(
                        response.getListUserTagList().stream()
                                .map(tag -> new UserTagDTO(
                                        tag.getUserId(),
                                        tag.getUserName()
                                ))
                                .toList()
                )
                .mediaUrl(response.getMediaUrl())
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
                .isLike(response.getIsLike())
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
