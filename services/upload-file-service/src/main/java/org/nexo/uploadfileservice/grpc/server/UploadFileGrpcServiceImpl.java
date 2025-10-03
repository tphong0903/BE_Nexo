package org.nexo.uploadfileservice.grpc.server;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.nexo.uploadfile.grpc.PostMediaGrpcServiceGrpc;
import org.nexo.uploadfile.grpc.PostMediaServiceProto;
import org.nexo.uploadfileservice.service.IUploadFileService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class UploadFileGrpcServiceImpl extends PostMediaGrpcServiceGrpc.PostMediaGrpcServiceImplBase {

    private final IUploadFileService uploadFileService;

    @Override
    public void uploadAvatar(PostMediaServiceProto.AvatarUploadRequest request,
            StreamObserver<PostMediaServiceProto.AvatarUploadResponse> responseObserver) {
        try {
            log.info("Received avatar upload request for file: {}", request.getFileName());

            // Convert ByteString to byte array
            byte[] avatarData = request.getAvatarData().toByteArray();

            // Upload avatar and get URL
            String avatarUrl = uploadFileService.uploadAvatar(
                    avatarData,
                    request.getFileName(),
                    request.getContentType());

            // Build successful response
            PostMediaServiceProto.AvatarUploadResponse response = PostMediaServiceProto.AvatarUploadResponse
                    .newBuilder()
                    .setSuccess(true)
                    .setMessage("Avatar uploaded successfully")
                    .setAvatarUrl(avatarUrl)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Avatar uploaded successfully: {}", avatarUrl);

        } catch (Exception e) {
            log.error("Error uploading avatar: {}", e.getMessage(), e);

            // Build error response
            PostMediaServiceProto.AvatarUploadResponse response = PostMediaServiceProto.AvatarUploadResponse
                    .newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to upload avatar: " + e.getMessage())
                    .setAvatarUrl("")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}