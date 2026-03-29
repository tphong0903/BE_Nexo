package org.nexo.uploadfileservice.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.uploadfile.grpc.PostMediaServiceProto;
import org.nexo.uploadfileservice.dto.UploadResult;
import org.nexo.uploadfileservice.grpc.PostGrpcClient;
import org.nexo.uploadfileservice.service.IUploadFileService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadFileServiceImpl implements IUploadFileService {
    private final PostGrpcClient postGrpcClient;
    private final Cloudinary cloudinary;

    private final ExecutorService sharedExecutor =
            Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors() * 2
            );

    @PreDestroy
    public void destroy() {
        if (sharedExecutor != null && !sharedExecutor.isShutdown()) {
            sharedExecutor.shutdown();
        }
    }

    @Override
    public String upload(MultipartFile multipartFile) {
        return uploadToCloudinary(multipartFile).getUrl();
    }

    @Override
    public List<String> uploadFileMessage(List<MultipartFile> multipartFiles) {
        List<CompletableFuture<String>> futures = multipartFiles.stream()
                .map(file -> CompletableFuture.supplyAsync(() -> upload(file), sharedExecutor))
                .toList();

        return futures.stream()
                .map(future -> future.exceptionally(ex -> {
                    log.error("Upload failed", ex);
                    return null;
                }))
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public void savePostMedia(List<MultipartFile> files, Long postId) {
        PostMediaServiceProto.PostMediaListRequest postMediaListRequests = postGrpcClient
                .findPostMediasOfPost(PostMediaServiceProto.PostId.newBuilder().setPostId(postId).build());
        int mediaOrderStart = postMediaListRequests.getPostsList().size();
        List<UploadResult> successfulUploads = Collections.synchronizedList(new ArrayList<>());
        try {
            List<CompletableFuture<PostMediaServiceProto.PostMediaRequestDTO>> futures = IntStream.range(0, files.size())
                    .mapToObj(index -> CompletableFuture.supplyAsync(() -> {
                        MultipartFile file = files.get(index);
                        boolean isImage = file.getContentType() != null && file.getContentType().startsWith("image");
                        String mediaType = isImage ? "PICTURE" : "VIDEO";

                        UploadResult result = isImage ? uploadToCloudinary(file) : handleVideoUpload(file);
                        successfulUploads.add(result); // Ghi nhận đã lên Cloudinary thành công

                        return PostMediaServiceProto.PostMediaRequestDTO.newBuilder()
                                .setPostID(postId)
                                .setMediaType(mediaType)
                                .setMediaOrder(mediaOrderStart + index)
                                .setMediaUrl(result.getUrl())
                                .build();
                    }, sharedExecutor))
                    .toList();

            List<PostMediaServiceProto.PostMediaRequestDTO> grpcRequests = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();

            PostMediaServiceProto.PostMediaListRequest request = PostMediaServiceProto.PostMediaListRequest.newBuilder()
                    .addAllPosts(grpcRequests)
                    .build();

            postGrpcClient.savePostMedias(request);

        } catch (Exception e) {
            log.error("Error during Post Media upload. Initiating rollback to clean up orphaned files...", e);
            rollbackCloudinaryUploads(successfulUploads);
            throw new RuntimeException("Failed to save post media. Rollback initiated.", e);
        }
    }

    @Override
    public void saveReelMedia(List<MultipartFile> files, Long postId) {
        List<UploadResult> successfulUploads = Collections.synchronizedList(new ArrayList<>());

        try {
            List<CompletableFuture<Void>> futures = files.stream()
                    .filter(file -> file.getContentType() != null && !file.getContentType().startsWith("image"))
                    .map(file -> CompletableFuture.runAsync(() -> {
                        UploadResult result = handleVideoUpload(file);
                        successfulUploads.add(result);

                        PostMediaServiceProto.ReelDto grpcItem = PostMediaServiceProto.ReelDto.newBuilder()
                                .setPostId(postId)
                                .setMediaUrl(result.getUrl())
                                .build();
                        postGrpcClient.saveReelMedias(grpcItem);
                    }, sharedExecutor))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.error("Error during Reel Media upload. Initiating rollback...", e);
            rollbackCloudinaryUploads(successfulUploads);
            throw new RuntimeException("Failed to save reel media", e);
        }
    }

    @Override
    public void saveStoryMedia(List<MultipartFile> files, Long postId) {
        List<UploadResult> successfulUploads = Collections.synchronizedList(new ArrayList<>());

        try {
            List<CompletableFuture<Void>> futures = files.stream()
                    .map(file -> CompletableFuture.runAsync(() -> {
                        boolean isImage = file.getContentType() != null && file.getContentType().startsWith("image");
                        String mediaType = isImage ? "PICTURE" : "VIDEO";

                        UploadResult result = isImage ? uploadToCloudinary(file) : handleVideoUpload(file);
                        successfulUploads.add(result);

                        PostMediaServiceProto.StoryDto grpcItem = PostMediaServiceProto.StoryDto.newBuilder()
                                .setStoryId(postId)
                                .setMediaUrl(result.getUrl())
                                .setMediaType(mediaType)
                                .build();
                        postGrpcClient.saveStoryMedias(grpcItem);
                    }, sharedExecutor))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.error("Error during Story Media upload. Initiating rollback...", e);
            rollbackCloudinaryUploads(successfulUploads);
            throw new RuntimeException("Failed to save story media", e);
        }
    }

    @Override
    public String uploadAvatar(byte[] avatarData, String fileName, String contentType) {
        try {
            if (avatarData == null || avatarData.length == 0) {
                throw new RuntimeException("Avatar data is empty!");
            }
            String uniqueFileName = "avatars/" + UUID.randomUUID() + "_" + fileName;
            Map uploadResult = cloudinary.uploader().upload(avatarData,
                    ObjectUtils.asMap(
                            "public_id", uniqueFileName,
                            "resource_type", "image",
                            "format", contentType != null ? contentType.split("/")[1] : "jpg"));

            return uploadResult.get("secure_url").toString();
        } catch (Exception e) {
            log.error("Avatar upload failed", e);
            throw new RuntimeException("Avatar couldn't upload, something went wrong: " + e.getMessage(), e);
        }
    }

    private UploadResult uploadToCloudinary(MultipartFile multipartFile) {
        try {
            Map uploadResult = cloudinary.uploader().upload(multipartFile.getInputStream(),
                    ObjectUtils.asMap(
                            "public_id", UUID.randomUUID() + "_" + multipartFile.getOriginalFilename(),
                            "folder", "posts",
                            "resource_type", "auto"));

            return new UploadResult(
                    uploadResult.get("secure_url").toString(),
                    uploadResult.get("public_id").toString(),
                    uploadResult.get("resource_type").toString()
            );
        } catch (Exception e) {
            throw new RuntimeException("Image couldn't upload", e);
        }
    }

    private UploadResult handleVideoUpload(MultipartFile file) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("video_" + UUID.randomUUID(), ".mp4");
            file.transferTo(tempFile);
            return uploadHlsToCloudinary(tempFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to process video file", e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.delete(tempFile.toPath());
                } catch (IOException e) {
                    log.error("Failed to delete temp video file: {}", tempFile.getAbsolutePath(), e);
                }
            }
        }
    }

    private UploadResult uploadHlsToCloudinary(File mp4File) throws IOException {
        if (!mp4File.exists() || !mp4File.isFile()) {
            throw new IllegalArgumentException("File does not exist or is not a file");
        }

        Transformation hlsTransformation = new Transformation()
                .param("streaming_profile", "full_hd");

        Map uploadResult = cloudinary.uploader().uploadLarge(mp4File,
                ObjectUtils.asMap(
                        "resource_type", "video",
                        "folder", "videos/" + UUID.randomUUID(),
                        "public_id", "master",
                        "eager", Collections.singletonList(hlsTransformation),
                        "eager_async", true));

        return new UploadResult(
                uploadResult.get("playback_url").toString(),
                uploadResult.get("public_id").toString(),
                uploadResult.get("resource_type").toString()
        );
    }

    private void rollbackCloudinaryUploads(List<UploadResult> successfulUploads) {
        if (successfulUploads == null || successfulUploads.isEmpty()) return;

        log.warn("Initiating deletion of {} orphaned files on Cloudinary due to process failure...", successfulUploads.size());

        for (UploadResult item : successfulUploads) {
            CompletableFuture.runAsync(() -> {
                try {
                    cloudinary.uploader().destroy(item.getPublicId(),
                            ObjectUtils.asMap("resource_type", item.getResourceType()));
                    log.info("Successfully rolled back file: {}", item.getPublicId());
                } catch (Exception e) {
                    log.error("Failed to roll back file: {}", item.getPublicId(), e);
                }
            }, sharedExecutor);
        }
    }
}