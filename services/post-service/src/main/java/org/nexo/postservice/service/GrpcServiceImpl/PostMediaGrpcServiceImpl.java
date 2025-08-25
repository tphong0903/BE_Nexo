package org.nexo.postservice.service.GrpcServiceImpl;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.nexo.postservice.dto.PostMediaDTO;
import org.nexo.postservice.grpc.PostMediaGrpcServiceGrpc;
import org.nexo.postservice.grpc.PostMediaServiceProto;
import org.nexo.postservice.service.IPostMediaService;
import org.nexo.postservice.grpc.PostMediaServiceProto.PostMediaRequestDTO;
import java.util.ArrayList;
import java.util.List;

@GrpcService
@RequiredArgsConstructor
public class PostMediaGrpcServiceImpl extends PostMediaGrpcServiceGrpc.PostMediaGrpcServiceImplBase{
    private final IPostMediaService postMediaService;

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
