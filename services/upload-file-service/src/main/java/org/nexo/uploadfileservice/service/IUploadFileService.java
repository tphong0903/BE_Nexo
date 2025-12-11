package org.nexo.uploadfileservice.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public interface IUploadFileService {

    String upload(MultipartFile multipartFile);

    List<String> uploadFileMessage(List<MultipartFile> multipartFile)
            throws IOException, InterruptedException, ExecutionException;

    void savePostMedia(List<MultipartFile> files, Long postId)
            throws IOException, InterruptedException, ExecutionException;

    void saveReelMedia(List<MultipartFile> files, Long postId);

    void saveStoryMedia(List<MultipartFile> files, Long postId);

    String uploadAvatar(byte[] avatarData, String fileName, String contentType);
}
