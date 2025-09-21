package org.nexo.postservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.postservice.dto.PostRequestDTO;
import org.nexo.postservice.dto.response.ResponseData;
import org.nexo.postservice.service.IPostService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/")
@Validated
@Slf4j
@RequiredArgsConstructor
public class PostController {
    private final IPostService postService;

    @PostMapping
    public ResponseData<String> addPost(@RequestPart("postRequestDTO") @Valid String postRequestDTOJson,
                                        @RequestPart(value = "files", required = false) List<MultipartFile> files) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        PostRequestDTO postRequestDTO = objectMapper.readValue(postRequestDTOJson, PostRequestDTO.class);
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", postService.savePost(postRequestDTO, files));
    }


    @PutMapping
    public ResponseData<String> updatePost(@RequestPart("postRequestDTO") @Valid String postRequestDTOJson,
                                           @RequestPart(value = "files", required = false) List<MultipartFile> files) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        PostRequestDTO postRequestDTO = objectMapper.readValue(postRequestDTOJson, PostRequestDTO.class);
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", postService.savePost(postRequestDTO, files));
    }

    @DeleteMapping("/{id}")
    public ResponseData<String> inactivePost(@PathVariable Long id) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", postService.inactivePost(id));
    }

    
    @GetMapping
    public ResponseData<String> test() {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", "Test Post Service");
    }
}
