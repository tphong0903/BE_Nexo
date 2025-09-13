package org.nexo.postservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.postservice.dto.StoryRequestDto;
import org.nexo.postservice.dto.response.ResponseData;
import org.nexo.postservice.service.IStoryService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/story")
@Validated
@Slf4j
@RequiredArgsConstructor
public class StoryController {
    private final IStoryService storyService;

    @PostMapping
    public ResponseData<String> addStory(@RequestPart("storyRequestDTO") @Valid String postRequestDTOJson,
                                         @RequestPart(value = "files", required = false) List<MultipartFile> files) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        StoryRequestDto postRequestDTO = objectMapper.readValue(postRequestDTOJson, StoryRequestDto.class);
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", storyService.saveStory(postRequestDTO, files));
    }

    @PutMapping
    public ResponseData<String> updateStory(@RequestPart("storyRequestDTO") @Valid String postRequestDTOJson,
                                            @RequestPart(value = "files", required = false) List<MultipartFile> files) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        StoryRequestDto postRequestDTO = objectMapper.readValue(postRequestDTOJson, StoryRequestDto.class);
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", storyService.saveStory(postRequestDTO, files));
    }

    @DeleteMapping("/{id}")
    public ResponseData<String> deleteStory(@PathVariable Long id) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", storyService.deleteStory(id));
    }

    @PutMapping("/{id}")
    public ResponseData<String> archiveStory(@PathVariable Long id) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", storyService.archiveStory(id));
    }

    @PostMapping("/view/{id}")
    public ResponseData<String> viewStory(@PathVariable Long id) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", storyService.viewStory(id));
    }

    @GetMapping("/view/{id}")
    public ResponseData<String> getViewStory(@PathVariable Long id) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", storyService.archiveStory(id));
    }
}
