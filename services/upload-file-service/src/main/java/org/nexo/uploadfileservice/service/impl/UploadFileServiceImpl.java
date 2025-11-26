package org.nexo.uploadfileservice.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.uploadfile.grpc.PostMediaServiceProto;
import org.nexo.uploadfileservice.grpc.PostGrpcClient;
import org.nexo.uploadfileservice.service.IHlsService;
import org.nexo.uploadfileservice.service.IUploadFileService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadFileServiceImpl implements IUploadFileService {
    private final PostGrpcClient postGrpcClient;
    private final IHlsService hlsService;
    private final Cloudinary cloudinary;

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
    public List<String> uploadFileMessage(List<MultipartFile> multipartFiles) {
        List<String> urls = new ArrayList<>();
        try {
            for (MultipartFile file : multipartFiles) {
                String fileName = file.getOriginalFilename();
                File convertedFile = this.convertToFile(file, fileName);
                String URL = this.uploadFile(convertedFile, fileName);
                urls.add(URL);
                if (convertedFile.delete()) {
                    System.out.println("File deleted successfully");
                } else {
                    System.err.println("Failed to delete file");
                }
            }
            return urls;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
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
                        // String hlsOutputDir = tempFile.getParent() + "/hls_" + index + "_" +
                        // System.currentTimeMillis();
                        file.transferTo(tempFile);
                        // File hlsFolder = hlsService.convertToHls(tempFile, hlsOutputDir);
                        // mediaUrl = uploadHlsToCloudinary(hlsFolder);
                        mediaUrl = uploadHlsToCloudinary(tempFile);
                    }

                    PostMediaServiceProto.PostMediaRequestDTO grpcItem = PostMediaServiceProto.PostMediaRequestDTO
                            .newBuilder()
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
                    // File hlsFolder = hlsService.convertToHls(tempFile, tempFile.getParent() +
                    // "/hls");
                    // String m3u8Url = uploadHlsToCloudinary(hlsFolder);
                    String m3u8Url = uploadHlsToCloudinary(tempFile);

                    PostMediaServiceProto.ReelDto grpcItem = PostMediaServiceProto.ReelDto.newBuilder()
                            .setPostId(postId)
                            .setMediaUrl(m3u8Url)
                            .build();
                    postGrpcClient.saveReelMedias(grpcItem);

                    deleteRecursive(tempFile);
                    // deleteRecursive(hlsFolder);
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
                    // File hlsFolder = hlsService.convertToHls(tempFile, tempFile.getParent() +
                    // "/hls");
                    mediaUrl = uploadHlsToCloudinary(tempFile);
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
        try {
            Map uploadResult = cloudinary.uploader().upload(file,
                    ObjectUtils.asMap(
                            "public_id", fileName,
                            "folder", "posts",
                            "resource_type", "auto"));
            return uploadResult.get("secure_url").toString();

        } catch (Exception e) {
            throw new RuntimeException("Upload to Cloudinary failed: " + e.getMessage(), e);
        }
    }

    public String uploadHlsToCloudinary(File mp4File) throws IOException {
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
                        "eager", Arrays.asList(hlsTransformation),
                        "eager_async", true));

        return uploadResult.get("playback_url").toString();
    }

    private File zipFolder(File folder) throws IOException {
        File zipFile = new File(folder.getParentFile(), folder.getName() + ".zip");
        try (FileOutputStream fos = new FileOutputStream(zipFile);
                ZipOutputStream zos = new ZipOutputStream(fos)) {

            Path folderPath = folder.toPath();
            Files.walk(folderPath).forEach(path -> {
                File file = path.toFile();
                if (file.isFile()) {
                    String zipEntryName = folderPath.relativize(path).toString().replace("\\", "/");
                    try (FileInputStream fis = new FileInputStream(file)) {
                        ZipEntry entry = new ZipEntry(zipEntryName);
                        zos.putNextEntry(entry);
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = fis.read(buffer)) != -1) {
                            zos.write(buffer, 0, len);
                        }
                        zos.closeEntry();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            });
        }
        return zipFile;
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
            throw new RuntimeException("Avatar couldn't upload, something went wrong: " + e.getMessage(), e);
        }
    }
}
