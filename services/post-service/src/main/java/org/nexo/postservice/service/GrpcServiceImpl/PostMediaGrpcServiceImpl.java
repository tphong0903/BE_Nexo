package org.nexo.postservice.service.GrpcServiceImpl;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.nexo.postservice.dto.PostMediaDTO;
import org.nexo.postservice.exception.CustomException;
import org.nexo.postservice.grpc.PostMediaGrpcServiceGrpc;
import org.nexo.postservice.grpc.PostMediaServiceProto;
import org.nexo.postservice.grpc.PostMediaServiceProto.PostMediaRequestDTO;
import org.nexo.postservice.model.ReelModel;
import org.nexo.postservice.model.StoryModel;
import org.nexo.postservice.repository.IReelRepository;
import org.nexo.postservice.repository.IStoryRepository;
import org.nexo.postservice.service.IPostMediaService;
import org.nexo.postservice.util.Enum.EMediaType;
import org.springframework.http.HttpStatus;

import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class PostMediaGrpcServiceImpl extends PostMediaGrpcServiceGrpc.PostMediaGrpcServiceImplBase {
    private final IPostMediaService postMediaService;
    private final IReelRepository reelRepository;
    private final IStoryRepository storyRepository;

    @Override
    public void savePostMedias(PostMediaServiceProto.PostMediaListRequest request,
                               StreamObserver<PostMediaServiceProto.PostMediaResponse> responseObserver) {
        try {
            List<PostMediaDTO> list = request.getPostsList().stream()
                    .map(post -> PostMediaDTO.builder()
                            .postId(post.getPostID())
                            .mediaUrl(post.getMediaUrl())
                            .mediaType(post.getMediaType())
                            .mediaOrder(post.getMediaOrder())
                            .build())
                    .toList();

            postMediaService.savePostMedia(list);
            sendSuccessResponse(responseObserver, "Posts saved successfully");
        } catch (Exception e) {
            handleGrpcError("savePostMedias", e, responseObserver);
        }
    }

    @Override
    public void saveReelMedias(PostMediaServiceProto.ReelDto request,
                               StreamObserver<PostMediaServiceProto.PostMediaResponse> responseObserver) {
        try {
            ReelModel model = reelRepository.findById(request.getPostId())
                    .orElseThrow(() -> new CustomException("Reel does not exist", HttpStatus.BAD_REQUEST));

            model.setVideoUrl(request.getMediaUrl());
            reelRepository.save(model);

            sendSuccessResponse(responseObserver, "Reel saved successfully");
        } catch (Exception e) {
            handleGrpcError("saveReelMedias", e, responseObserver);
        }
    }

    @Override
    public void saveStoryMedias(PostMediaServiceProto.StoryDto request,
                                StreamObserver<PostMediaServiceProto.PostMediaResponse> responseObserver) {
        try {
            StoryModel model = storyRepository.findById(request.getStoryId())
                    .orElseThrow(() -> new CustomException("Story does not exist", HttpStatus.BAD_REQUEST));

            model.setMediaURL(request.getMediaUrl());
            model.setMediaType(EMediaType.valueOf(request.getMediaType()));
            storyRepository.save(model);

            sendSuccessResponse(responseObserver, "Story saved successfully");
        } catch (Exception e) {
            handleGrpcError("saveStoryMedias", e, responseObserver);
        }
    }

    @Override
    public void findPostMediasOfPost(PostMediaServiceProto.PostId request,
                                     StreamObserver<PostMediaServiceProto.PostMediaListRequest> responseObserver) {
        try {
            List<PostMediaRequestDTO> list = postMediaService.findPostMediasOfPost(request.getPostId());

            PostMediaServiceProto.PostMediaListRequest response = PostMediaServiceProto.PostMediaListRequest.newBuilder()
                    .addAllPosts(list)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            handleGrpcError("findPostMediasOfPost", e, responseObserver);
        }
    }

    @Override
    public void deletePostMedia(PostMediaServiceProto.PostId request,
                                StreamObserver<PostMediaServiceProto.PostMediaResponse> responseObserver) {
        try {
            postMediaService.deletePostMedia(request.getPostId());
            sendSuccessResponse(responseObserver, "Post Media deleted successfully");
        } catch (Exception e) {
            handleGrpcError("deletePostMedia", e, responseObserver);
        }
    }


    private void sendSuccessResponse(StreamObserver<PostMediaServiceProto.PostMediaResponse> responseObserver, String message) {
        PostMediaServiceProto.PostMediaResponse response = PostMediaServiceProto.PostMediaResponse.newBuilder()
                .setSuccess(true)
                .setMessage(message)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void handleGrpcError(String methodName, Exception e, StreamObserver<?> responseObserver) {
        log.error("Error in {}: {}", methodName, e.getMessage(), e);
        responseObserver.onError(Status.INTERNAL
                .withDescription(e.getMessage())
                .withCause(e)
                .asRuntimeException());
    }
}