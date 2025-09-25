package org.nexo.uploadfileservice.grpc;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.nexo.uploadfile.grpc.PostMediaGrpcServiceGrpc;
import org.nexo.uploadfile.grpc.PostMediaServiceProto;
import org.springframework.stereotype.Service;


@Service
public class PostGrpcClient {

    @GrpcClient("posts")
    private PostMediaGrpcServiceGrpc.PostMediaGrpcServiceBlockingStub postStub;

    public PostMediaServiceProto.PostMediaResponse savePostMedias(
            PostMediaServiceProto.PostMediaListRequest request) {
        return postStub.savePostMedias(request);
    }

    public PostMediaServiceProto.PostMediaResponse saveReelMedias(
            PostMediaServiceProto.ReelDto request) {
        return postStub.saveReelMedias(request);
    }

    public PostMediaServiceProto.PostMediaResponse saveStoryMedias(
            PostMediaServiceProto.StoryDto request) {
        return postStub.saveStoryMedias(request);
    }

    public PostMediaServiceProto.PostMediaListRequest findPostMediasOfPost(
            PostMediaServiceProto.PostId request) {
        return postStub.findPostMediasOfPost(request);
    }

    public PostMediaServiceProto.PostMediaResponse deletePostMedia(
            PostMediaServiceProto.PostMediaListRequest request) {
        return postStub.savePostMedias(request);
    }
}

