package org.nexo.interactionservice.service.impl;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.nexo.grpc.post.PostServiceGrpc;
import org.nexo.grpc.post.PostServiceOuterClass;
import org.springframework.stereotype.Service;


@Service
public class PostGrpcClient {

    @GrpcClient("posts")
    private PostServiceGrpc.PostServiceBlockingStub postStub;

    public void addLikeQuantityById(
            PostServiceOuterClass.GetPostRequest2 request) {
        postStub.addLikeQuantityById(request);
    }

    public void addCommentQuantityById(
            PostServiceOuterClass.GetPostRequest2 request) {
        postStub.addCommentQuantityById(request);
    }

}

