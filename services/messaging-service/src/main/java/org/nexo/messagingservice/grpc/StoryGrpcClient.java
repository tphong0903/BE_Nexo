package org.nexo.messagingservice.grpc;

import net.devh.boot.grpc.client.inject.GrpcClient;

import org.nexo.grpc.story.GetStoryMediaRequest;
import org.nexo.grpc.story.GetStoryMediaResponse;
import org.nexo.grpc.story.StoryServiceGrpc;
import org.springframework.stereotype.Service;

@Service
public class StoryGrpcClient {
    @GrpcClient("post-service")
    private StoryServiceGrpc.StoryServiceBlockingStub storyStub;

    public String getStoryMediaIfActive(Long storyId) {
        GetStoryMediaRequest request = GetStoryMediaRequest.newBuilder()
                .setStoryId(storyId)
                .build();
        GetStoryMediaResponse response = storyStub.getStoryMediaIfActive(request);
        String mediaUrl = response.getMediaUrl();
        return (mediaUrl != null && !mediaUrl.isEmpty()) ? mediaUrl : null;
    }
}
