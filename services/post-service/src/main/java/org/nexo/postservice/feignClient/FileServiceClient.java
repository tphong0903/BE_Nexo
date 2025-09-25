package org.nexo.postservice.feignClient;

import org.nexo.postservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@FeignClient(name = "files", url = "http://localhost:8086", configuration = FeignConfig.class)
public interface FileServiceClient {
    @PostMapping(value = "/post/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<String> savePostMedia(
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart("postId") String postId,
            @RequestHeader("Authorization") String token
    );

    @PostMapping(value = "/reel/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<String> saveReelMedia(
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart("postId") String postId,
            @RequestHeader("Authorization") String token
    );

    @PostMapping(value = "/story/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<String> saveStoryMedia(
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart("storyId") String postId,
            @RequestHeader("Authorization") String token
    );
}