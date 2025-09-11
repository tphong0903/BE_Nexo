package org.nexo.postservice.service.GrpcServiceImpl;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
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

import java.util.ArrayList;
import java.util.List;

@GrpcService
@RequiredArgsConstructor
public class PostMediaGrpcServiceImpl extends PostMediaGrpcServiceGrpc.PostMediaGrpcServiceImplBase {
    private final IPostMediaService postMediaService;
    private final IReelRepository reelRepository;
    private final IStoryRepository storyRepository;

    @Override
    public void savePostMedias(PostMediaServiceProto.PostMediaListRequest request,
                               StreamObserver<PostMediaServiceProto.PostMediaResponse> responseObserver) {
        List<PostMediaDTO> list = new ArrayList<>();
        request.getPostsList().forEach(post -> {
            list.add(PostMediaDTO.builder()
                    .postId(post.getPostID())
                    .mediaUrl(post.getMediaUrl())
                    .mediaType(post.getMediaType())
                    .mediaOrder(post.getMediaOrder())
                    .build());
        });
        postMediaService.savePostMedia(list);
        PostMediaServiceProto.PostMediaResponse response =
                PostMediaServiceProto.PostMediaResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("Posts saved successfully")
                        .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void saveReelMedias(PostMediaServiceProto.ReelDto request,
                               StreamObserver<PostMediaServiceProto.PostMediaResponse> responseObserver) {
        ReelModel model = reelRepository.findById(request.getPostId()).orElseThrow(() -> new CustomException("Reel is not exist", HttpStatus.BAD_REQUEST));
        model.setVideoUrl(request.getMediaUrl());
        reelRepository.save(model);
        PostMediaServiceProto.PostMediaResponse response =
                PostMediaServiceProto.PostMediaResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("Reel saved successfully")
                        .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void saveStoryMedias(PostMediaServiceProto.StoryDto request,
                                StreamObserver<PostMediaServiceProto.PostMediaResponse> responseObserver) {
        StoryModel model = storyRepository.findById(request.getStoryId()).orElseThrow(() -> new CustomException("Reel is not exist", HttpStatus.BAD_REQUEST));
        model.setMediaURL(request.getMediaUrl());
        model.setMediaType(Enum.valueOf(EMediaType.class, request.getMediaType()));
        storyRepository.save(model);
        PostMediaServiceProto.PostMediaResponse response =
                PostMediaServiceProto.PostMediaResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("Story saved successfully")
                        .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void findPostMediasOfPost(PostMediaServiceProto.PostId request,
                                     StreamObserver<PostMediaServiceProto.PostMediaListRequest> responseObserver) {
        List<PostMediaRequestDTO> list = postMediaService.findPostMediasOfPost(request.getPostId());

        PostMediaServiceProto.PostMediaListRequest response =
                PostMediaServiceProto.PostMediaListRequest.newBuilder().addAllPosts(list).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void deletePostMedia(PostMediaServiceProto.PostId request,
                                StreamObserver<PostMediaServiceProto.PostMediaResponse> responseObserver) {
        postMediaService.deletePostMedia(request.getPostId());

        PostMediaServiceProto.PostMediaResponse response =
                PostMediaServiceProto.PostMediaResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("Post Media deleted successfully")
                        .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
