package org.nexo.interactionservice.service.impl;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.nexo.grpc.post.PostServiceGrpc;
import org.nexo.grpc.post.PostServiceOuterClass;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class PostGrpcClient {

    @GrpcClient("posts")
    private PostServiceGrpc.PostServiceBlockingStub postStub;

    public void addLikeQuantityById(Long id, Boolean isPost, Boolean isIncrease) {
        postStub.addLikeQuantityById(PostServiceOuterClass.GetPostRequest2.newBuilder().setId(id).setIsPost(isPost).setIsIncrease(isIncrease).build());
    }

    public void addCommentQuantityById(Long id, Boolean isPost, Boolean isIncrease) {
        postStub.addCommentQuantityById(PostServiceOuterClass.GetPostRequest2.newBuilder().setId(id).setIsPost(isPost).setIsIncrease(isIncrease).build());
    }

    public PostServiceOuterClass.PostResponse getPostById(Long id) {
        return postStub.getPostById(PostServiceOuterClass.GetPostRequest.newBuilder().setId(id).build());
    }

    public PostServiceOuterClass.ReelResponse getReelById(Long id) {
        return postStub.getReelById(PostServiceOuterClass.GetPostRequest.newBuilder().setId(id).build());
    }

    public Map<Long, PostServiceOuterClass.PostResponse> getPostsByIds(List<Long> postIds, Long userId) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }

        PostServiceOuterClass.GetPostsByIdsResponse response = postStub.getPostsByIds(
                PostServiceOuterClass.GetPostsByIdsRequest.newBuilder()
                        .addAllPostIds(postIds)
                        .setUserId(userId)
                        .build()
        );

        return response.getPostsList().stream()
                .collect(Collectors.toMap(PostServiceOuterClass.PostResponse::getPostId, Function.identity()));
    }

}

