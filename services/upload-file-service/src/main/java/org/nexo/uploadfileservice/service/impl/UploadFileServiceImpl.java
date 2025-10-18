package org.nexo.uploadfileservice.service.impl;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.uploadfile.grpc.PostMediaServiceProto;
import org.nexo.uploadfileservice.grpc.PostGrpcClient;
import org.nexo.uploadfileservice.service.IHlsService;
import org.nexo.uploadfileservice.service.IUploadFileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadFileServiceImpl implements IUploadFileService {
    private final PostGrpcClient postGrpcClient;
    private final IHlsService hlsService;
    @Value("${app.firebase.bucket}")
    private String BUCKET_NAME;
    @Value("${app.firebase.file}")
    private String FIREBASE_PRIVATE_KEY;

    @Override
    public String upload(MultipartFile multipartFile) {
        try {
            String fileName = multipartFile.getOriginalFilename();
            File file = this.convertToFile(multipartFile, fileName);
            String URL = this.uploadFile(file, fileName);
            if (file.delete()) {
                System.out.println("File deleted successfully");
            } else {
                System.err.println("Failed to delete file");
            }
            return URL;
        } catch (Exception e) {
            e.printStackTrace();
            return "Image couldn't upload, Something went wrong";
        }
    }

    @Override
    public void savePostMedia(List<MultipartFile> files, Long postId) throws InterruptedException, ExecutionException {
        List<PostMediaServiceProto.PostMediaRequestDTO> grpcRequests = Collections.synchronizedList(new ArrayList<>());

        PostMediaServiceProto.PostMediaListRequest postMediaListRequests = postGrpcClient
                .findPostMediasOfPost(PostMediaServiceProto.PostId.newBuilder().setPostId(postId).build());
        int mediaOrderStart = postMediaListRequests.getPostsList().size();

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(files.size(), 8));

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            final int index = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    MultipartFile file = files.get(index);
                    String contentType = file.getContentType();
                    String mediaUrl = "";
                    String mediaType;

                    if (contentType.startsWith("image")) {
                        mediaType = "PICTURE";
                        mediaUrl = upload(file);
                    } else {
                        mediaType = "VIDEO";
                        File tempFile = File.createTempFile("video", ".mp4");
                        String hlsOutputDir = tempFile.getParent() + "/hls_" + index + "_" + System.currentTimeMillis();
                        file.transferTo(tempFile);
                        File hlsFolder = hlsService.convertToHls(tempFile, hlsOutputDir);
                        mediaUrl = uploadHlsToFirebase(hlsFolder);
                    }

                    PostMediaServiceProto.PostMediaRequestDTO grpcItem = PostMediaServiceProto.PostMediaRequestDTO.newBuilder()
                            .setPostID(postId)
                            .setMediaType(mediaType)
                            .setMediaOrder(mediaOrderStart + index)
                            .setMediaUrl(mediaUrl)
                            .build();

                    grpcRequests.add(grpcItem);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

        executor.shutdown();

        PostMediaServiceProto.PostMediaListRequest request = PostMediaServiceProto.PostMediaListRequest.newBuilder()
                .addAllPosts(grpcRequests)
                .build();
        postGrpcClient.savePostMedias(request);
    }


    @Override
    public void saveReelMedia(List<MultipartFile> files, Long postId) {
        try {
            for (MultipartFile file : files) {
                String contentType = file.getContentType();
                String mediaType;
                if (contentType.startsWith("image")) {
                    mediaType = "PICTURE";
                } else {
                    mediaType = "VIDEO";
                }
                if (mediaType.equals("VIDEO")) {
                    File tempFile = File.createTempFile("video", ".mp4");
                    file.transferTo(tempFile);
                    File hlsFolder = hlsService.convertToHls(tempFile, tempFile.getParent() + "/hls");
                    String m3u8Url = uploadHlsToFirebase(hlsFolder);

                    PostMediaServiceProto.ReelDto grpcItem = PostMediaServiceProto.ReelDto.newBuilder()
                            .setPostId(postId)
                            .setMediaUrl(m3u8Url)
                            .build();
                    postGrpcClient.saveReelMedias(grpcItem);

                    deleteRecursive(tempFile);
                    deleteRecursive(hlsFolder);
                }
            }
        } catch (Exception e) {
            log.error("Failed: " + e.getMessage());
        }
    }

    @Override
    public void saveStoryMedia(List<MultipartFile> files, Long postId) {
        for (MultipartFile file : files) {
            String contentType = file.getContentType();
            String mediaUrl = "";
            String mediaType;
            try {
                if (contentType.startsWith("image")) {
                    mediaType = "PICTURE";
                    mediaUrl = upload(file);
                } else {
                    File tempFile = File.createTempFile("video", ".mp4");
                    file.transferTo(tempFile);
                    File hlsFolder = hlsService.convertToHls(tempFile, tempFile.getParent() + "/hls");
                    mediaUrl = uploadHlsToFirebase(hlsFolder);
                    mediaType = "VIDEO";
                }
                PostMediaServiceProto.StoryDto grpcItem = PostMediaServiceProto.StoryDto.newBuilder()
                        .setStoryId(postId)
                        .setMediaUrl(mediaUrl)
                        .setMediaType(mediaType)
                        .build();
                postGrpcClient.saveStoryMedias(grpcItem);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String uploadFile(File file, String fileName) throws IOException {
        if (FIREBASE_PRIVATE_KEY == null) {
            throw new RuntimeException("Firebase private key is not found. " +
                    "Please set 'app.firebase.file' in application.properties");
        }
        BlobId blobId = BlobId.of(BUCKET_NAME, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("media").build();
        try (InputStream inputStream = UploadFileServiceImpl.class.getClassLoader()
                .getResourceAsStream(FIREBASE_PRIVATE_KEY)) {
            if (inputStream == null) {
                throw new RuntimeException("Firebase private key is not found. " +
                        "Please check 'app.firebase.file' in application.properties");
            }
            // change the file name with your one
            Credentials credentials = GoogleCredentials.fromStream(inputStream);
            Storage storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();
            storage.create(blobInfo, Files.readAllBytes(file.toPath()));
        }
        String DOWNLOAD_URL = "https://firebasestorage.googleapis.com/v0/b/" + BUCKET_NAME + "/o/%s?alt=media";
        return String.format(DOWNLOAD_URL, URLEncoder.encode(fileName, StandardCharsets.UTF_8));
    }

    public String uploadHlsToFirebase(File hlsFolder) throws IOException {
        if (FIREBASE_PRIVATE_KEY == null) {
            throw new RuntimeException("Firebase private key is not found. " +
                    "Please set 'app.firebase.file' in application.properties");
        }
        String folderName = "videos/" + UUID.randomUUID();
        try (InputStream inputStream = UploadFileServiceImpl.class.getClassLoader()
                .getResourceAsStream(FIREBASE_PRIVATE_KEY)) {
            if (inputStream == null) {
                throw new RuntimeException("Firebase private key is not found. " +
                        "Please check 'app.firebase.file' in application.properties");
            }
            Credentials credentials = GoogleCredentials.fromStream(inputStream);
            Storage storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();
            for (File file : Objects.requireNonNull(hlsFolder.getParentFile().listFiles())) {
                String blobName = folderName + "/" + file.getName();
                BlobId blobId = BlobId.of(BUCKET_NAME, blobName);
                String contentType;

                if (file.getName().endsWith(".m3u8")) {
                    contentType = "application/vnd.apple.mpegurl";

                    String m3u8Content = Files.readString(file.toPath(), StandardCharsets.UTF_8);

                    String baseUrl = "https://firebasestorage.googleapis.com/v0/b/"
                            + BUCKET_NAME + "/o/" + URLEncoder.encode(folderName + "/", StandardCharsets.UTF_8);

                    m3u8Content = m3u8Content.replaceAll("(segment_\\d+\\.ts)", baseUrl + "$1?alt=media");

                    // Upload lại file m3u8 đã chỉnh sửa
                    BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                            .setContentType(contentType)
                            .build();
                    storage.create(blobInfo, m3u8Content.getBytes(StandardCharsets.UTF_8));

                } else if (file.getName().endsWith(".ts")) {
                    contentType = "video/MP2T";
                    BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                            .setContentType(contentType)
                            .build();
                    storage.create(blobInfo, Files.readAllBytes(file.toPath()));

                } else {
                    contentType = Files.probeContentType(file.toPath());
                    BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                            .setContentType(contentType)
                            .build();
                    storage.create(blobInfo, Files.readAllBytes(file.toPath()));
                }
            }
        }
        String playlistName = Arrays.stream(Objects.requireNonNull(hlsFolder.getParentFile().listFiles()))
                .filter(f -> f.getName().endsWith(".m3u8"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No .m3u8 file found"))
                .getName();
        String DOWNLOAD_URL = "https://firebasestorage.googleapis.com/v0/b/" + BUCKET_NAME + "/o/%s?alt=media";
        return String.format(DOWNLOAD_URL, URLEncoder.encode(folderName + "/" + playlistName, StandardCharsets.UTF_8));
    }

    public File convertToFile(MultipartFile multipartFile, String fileName) throws IOException {
        File tempFile = new File(fileName);
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(multipartFile.getBytes());
        }
        return tempFile;
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            for (File subFile : Objects.requireNonNull(file.listFiles())) {
                deleteRecursive(subFile);
            }
        }
        file.delete();
    }

    @Override
    public String uploadAvatar(byte[] avatarData, String fileName, String contentType) {
        try {
            if (FIREBASE_PRIVATE_KEY == null) {
                throw new RuntimeException("Firebase private key is not found. " +
                        "Please set 'app.firebase.file' in application.properties");
            }

            String uniqueFileName = "avatars/" + UUID.randomUUID() + "_" + fileName;
            BlobId blobId = BlobId.of(BUCKET_NAME, uniqueFileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(contentType != null ? contentType : "image/jpeg")
                    .build();

            try (InputStream inputStream = UploadFileServiceImpl.class.getClassLoader()
                    .getResourceAsStream(FIREBASE_PRIVATE_KEY)) {
                if (inputStream == null) {
                    throw new RuntimeException("Firebase private key is not found. " +
                            "Please check 'app.firebase.file' in application.properties");
                }
                Credentials credentials = GoogleCredentials.fromStream(inputStream);
                Storage storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();
                storage.create(blobInfo, avatarData);
            }

            String DOWNLOAD_URL = "https://firebasestorage.googleapis.com/v0/b/" + BUCKET_NAME + "/o/%s?alt=media";
            return String.format(DOWNLOAD_URL, URLEncoder.encode(uniqueFileName, StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Error uploading avatar: {}", e.getMessage(), e);
            throw new RuntimeException("Avatar couldn't upload, Something went wrong: " + e.getMessage());
        }
    }
}
