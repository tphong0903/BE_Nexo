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
@RequestMapping("/reel")
@Validated
@Slf4j
@RequiredArgsConstructor
public class ReelController {
    private final IPostService postService;

    @PostMapping
    public ResponseData<String> addReel(@RequestPart("reelRequestDTO") @Valid String postRequestDTOJson,
                                        @RequestPart(value = "files", required = false) List<MultipartFile> files) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        PostRequestDTO postRequestDTO = objectMapper.readValue(postRequestDTOJson, PostRequestDTO.class);
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", postService.saveReel(postRequestDTO, files));
    }

    @PutMapping()
    public ResponseData<String> updateReel(@RequestPart("reelRequestDTO") @Valid String postRequestDTOJson,
                                           @RequestPart(value = "files", required = false) List<MultipartFile> files) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        PostRequestDTO postRequestDTO = objectMapper.readValue(postRequestDTOJson, PostRequestDTO.class);
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", postService.saveReel(postRequestDTO, files));
    }

    @PatchMapping("/{id}")
    public ResponseData<String> inactiveReel(@PathVariable Long id) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", postService.inactiveReel(id));
    }

    @DeleteMapping("/{id}")
    public ResponseData<String> deletePost(@PathVariable Long id) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", postService.deleteReel(id));
    }

    @GetMapping("/users/{id}")
    public ResponseData<?> getAllReelfUser(@PathVariable Long id, @RequestParam(defaultValue = "0") int pageNo, @RequestParam(defaultValue = "20") int pageSize) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", postService.getAllReelOfUser(id, pageNo, pageSize));
    }

    @GetMapping("/{id}")
    public ResponseData<?> getPost(@PathVariable Long id) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", postService.getReelById(id));
    }
}
