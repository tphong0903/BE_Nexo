package org.nexo.postservice.service.GrpcServiceImpl;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.nexo.postservice.dto.PostMediaDTO;
import org.nexo.postservice.grpc.PostMediaGrpcServiceGrpc;
import org.nexo.postservice.grpc.PostMediaServiceProto;
import org.nexo.postservice.service.IPostMediaService;

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
            list.add(new PostMediaDTO(post.getPostID(), post.getMediaUrl(), post.getMediaType(), post.getMediaOrder()));
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
}
