package org.nexo.feedservice.service;

import lombok.RequiredArgsConstructor;
import org.nexo.feedservice.dto.PostResponseDTO;
import org.nexo.feedservice.dto.UserTagDTO;
import org.nexo.grpc.post.PostServiceGrpc;
import org.nexo.grpc.post.PostServiceOuterClass;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class PostGrpcClient {

    private final PostServiceGrpc.PostServiceBlockingStub postStub;

    public PostResponseDTO getPostById(Long id) {
        var request = PostServiceOuterClass.GetPostRequest.newBuilder()
                .setId(id)
                .build();
        var response = postStub.getPostById(request);
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
                .build();
    }
}

