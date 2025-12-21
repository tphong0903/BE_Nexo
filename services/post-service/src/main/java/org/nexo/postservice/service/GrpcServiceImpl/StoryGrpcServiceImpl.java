package org.nexo.postservice.service.GrpcServiceImpl;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import org.nexo.grpc.story.GetStoryMediaRequest;
import org.nexo.grpc.story.GetStoryMediaResponse;
import org.nexo.grpc.story.StoryServiceGrpc;
import org.nexo.postservice.model.StoryModel;
import org.nexo.postservice.repository.IStoryRepository;

import java.time.LocalDateTime;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class StoryGrpcServiceImpl extends StoryServiceGrpc.StoryServiceImplBase {
    private final IStoryRepository storyRepository;

    @Override
    public void getStoryMediaIfActive(GetStoryMediaRequest request,
            StreamObserver<GetStoryMediaResponse> responseObserver) {
        StoryModel story = storyRepository.findById(request.getStoryId()).orElse(null);
        String mediaUrl = "";
        if (story != null && Boolean.TRUE.equals(story.getIsActive()) && story.getExpiresAt() != null
                && story.getExpiresAt().isAfter(LocalDateTime.now())) {
            mediaUrl = story.getMediaURL();
        }
        GetStoryMediaResponse response = GetStoryMediaResponse.newBuilder()
                .setMediaUrl(mediaUrl == null ? "" : mediaUrl).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
