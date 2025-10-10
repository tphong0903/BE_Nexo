package org.nexo.postservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.postservice.dto.PostMediaDTO;
import org.nexo.postservice.dto.PostRequestDTO;
import org.nexo.postservice.dto.response.ResponseData;
import org.nexo.postservice.service.IPostMediaService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/media")
@Validated
@Slf4j
@RequiredArgsConstructor
public class PostMediaController {
    private final IPostMediaService postMediaService;

    @PostMapping("/")
    public ResponseData<String> addPostMedia(List<PostMediaDTO> postMediaDTOList) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", postMediaService.savePostMedia(postMediaDTOList));
    }
}
