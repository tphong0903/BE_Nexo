package org.nexo.userservice.grpc;

import net.devh.boot.grpc.client.inject.GrpcClient;

import org.nexo.grpc.post.PostServiceGrpc;
import org.nexo.grpc.post.PostServiceOuterClass;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PostGrpcClient {

    @GrpcClient("post-service")
    private PostServiceGrpc.PostServiceBlockingStub postServiceStub;

    public Long getUserPostsCount(Long userId) {
        try {
            PostServiceOuterClass.GetUserPostsCountRequest request = PostServiceOuterClass.GetUserPostsCountRequest
                    .newBuilder()
                    .setUserId(userId)
                    .build();

            PostServiceOuterClass.GetUserPostsCountResponse response = postServiceStub.getUserPostsCount(request);

            return response.getPostsCount();
        } catch (Exception e) {
            log.error("Error getting posts count for userId: {}, error: {}", userId, e.getMessage());
            return 0L;
        }
    }
}
