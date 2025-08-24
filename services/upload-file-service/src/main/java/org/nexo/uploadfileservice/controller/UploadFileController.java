package org.nexo.uploadfileservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.uploadfileservice.dto.PostMediaRequestDTO;
import org.nexo.uploadfileservice.dto.response.ResponseData;
import org.nexo.uploadfileservice.service.IUploadFileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/")
@Slf4j
public class UploadFileController {
    private final IUploadFileService uploadFileService;
    @PostMapping(value ="/post/media")
    public ResponseEntity<String> uploadMedia(@RequestPart("files") List<MultipartFile> files, @RequestPart("postId") String postId ) {
        if (files != null && !files.isEmpty() && postId != null && !postId.isEmpty()) {
            log.info("Add File");
            uploadFileService.savePostMedia(files, postId);
            return ResponseEntity.ok("Upload thành công");
        }
        return ResponseEntity.badRequest().body("Dữ liệu không hợp lệ");
    }
    @GetMapping
    public ResponseData<String> test(){
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", "Test File Service");
    }
    @GetMapping("/ss")
    public ResponseData<String> test2(){
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", "Test File Service");
    }
}
