package org.nexo.userservice.grpc;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.nexo.uploadfile.grpc.PostMediaGrpcServiceGrpc;
import org.nexo.uploadfile.grpc.PostMediaServiceProto;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class UploadFileGrpcClient {

    @GrpcClient("upload-file-service")
    private PostMediaGrpcServiceGrpc.PostMediaGrpcServiceBlockingStub uploadFileStub;

    public String uploadAvatar(MultipartFile avatarFile) {
        try {
            if (avatarFile == null || avatarFile.isEmpty()) {
                log.warn("Avatar file is null or empty");
                return null;
            }

            log.info("Uploading avatar via gRPC: {}", avatarFile.getOriginalFilename());

            byte[] avatarData = avatarFile.getBytes();

            PostMediaServiceProto.AvatarUploadRequest request = PostMediaServiceProto.AvatarUploadRequest.newBuilder()
                    .setAvatarData(ByteString.copyFrom(avatarData))
                    .setFileName(
                            avatarFile.getOriginalFilename() != null ? avatarFile.getOriginalFilename() : "avatar.jpg")
                    .setContentType(avatarFile.getContentType() != null ? avatarFile.getContentType() : "image/jpeg")
                    .build();

            PostMediaServiceProto.AvatarUploadResponse response = uploadFileStub.uploadAvatar(request);

            if (response.getSuccess()) {
                log.info("Avatar uploaded successfully: {}", response.getAvatarUrl());
                return response.getAvatarUrl();
            } else {
                log.error("Failed to upload avatar: {}", response.getMessage());
                return null;
            }

        } catch (Exception e) {
            log.error("Error uploading avatar via gRPC: {}", e.getMessage(), e);
            return null;
        }
    }
}
