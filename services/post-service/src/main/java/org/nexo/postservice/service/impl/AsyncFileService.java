package org.nexo.postservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.nexo.postservice.feignClient.FileServiceClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AsyncFileService {
    private final FileServiceClient fileServiceClient;

    @Async
    public void savePostMedia(List<MultipartFile> files, Long postId) {
        fileServiceClient.savePostMedia(files, String.valueOf(postId));
    }

    @Async
    public void saveReelMedia(List<MultipartFile> files, Long postId) {
        fileServiceClient.saveReelMedia(files, String.valueOf(postId));
    }

    @Async
    public void saveStoryMedia(List<MultipartFile> files, Long postId) {
        fileServiceClient.saveStoryMedia(files, String.valueOf(postId));
    }
}
