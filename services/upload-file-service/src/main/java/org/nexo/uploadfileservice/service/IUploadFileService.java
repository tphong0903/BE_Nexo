package org.nexo.uploadfileservice.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IUploadFileService {

    String upload(MultipartFile multipartFile);

    void savePostMedia(List<MultipartFile> files, Long postId);

    void saveReelMedia(List<MultipartFile> files, Long postId);

    void saveStoryMedia(List<MultipartFile> files, Long postId);

    String uploadAvatar(byte[] avatarData, String fileName, String contentType);
}
