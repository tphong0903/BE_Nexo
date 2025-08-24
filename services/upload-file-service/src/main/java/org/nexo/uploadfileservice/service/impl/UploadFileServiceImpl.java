package org.nexo.uploadfileservice.service.impl;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import lombok.RequiredArgsConstructor;
import org.nexo.uploadfileservice.dto.PostMediaDTO;
import org.nexo.uploadfileservice.dto.PostMediaRequestDTO;
import org.nexo.uploadfileservice.service.IUploadFileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UploadFileServiceImpl implements IUploadFileService {
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
    public void savePostMedia(List<MultipartFile> files,String postId) {
        List<PostMediaDTO> postMediaDTOList =  new ArrayList<>();
        for(int i=0;i<files.size();i++){
            MultipartFile file = files.get(i);
            String contentType = file.getContentType();

            String mediaType;
            if (contentType.startsWith("image")) {
                mediaType = "PICTURE";
            } else  {
                mediaType = "VIDEO";
            }

            PostMediaDTO item = PostMediaDTO.builder()
                    .postId(postId)
                    .mediaType(mediaType)
                    .order(i)
                    .mediaUrl(upload(files.get(i)))
                    .build();
            postMediaDTOList.add(item);
        }

    }

    public String uploadFile(File file, String fileName) throws IOException {
        if (FIREBASE_PRIVATE_KEY == null) {
            throw new RuntimeException("Firebase private key is not found. " +
                    "Please set 'app.firebase.file' in application.properties");
        }
        BlobId blobId = BlobId.of(BUCKET_NAME, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("media").build();
        try (InputStream inputStream = UploadFileServiceImpl.class.getClassLoader().getResourceAsStream(FIREBASE_PRIVATE_KEY)) {
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

    public File convertToFile(MultipartFile multipartFile, String fileName) throws IOException {
        File tempFile = new File(fileName);
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(multipartFile.getBytes());
        }
        return tempFile;
    }
}
