package org.nexo.postservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.nexo.postservice.feignClient.FileServiceClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService {
    private final FileServiceClient fileServiceClient;


    public void savePostMedia(List<MultipartFile> files, Long postId, String token) {
        fileServiceClient.savePostMedia(files, String.valueOf(postId), "Bearer " + token);
    }

    public void saveReelMedia(List<MultipartFile> files, Long postId, String token) {
        fileServiceClient.saveReelMedia(files, String.valueOf(postId), "Bearer " + token);
    }

    public void saveStoryMedia(List<MultipartFile> files, Long postId, String token) {

        fileServiceClient.saveStoryMedia(files, String.valueOf(postId), "Bearer " + token);
    }
}
