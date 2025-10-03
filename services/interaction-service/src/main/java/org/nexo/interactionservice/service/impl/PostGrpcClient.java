package org.nexo.interactionservice.service.impl;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.nexo.grpc.post.PostServiceGrpc;
import org.nexo.grpc.post.PostServiceOuterClass;
import org.springframework.stereotype.Service;


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

}

