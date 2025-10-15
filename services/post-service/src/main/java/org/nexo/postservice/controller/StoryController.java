package org.nexo.postservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.postservice.dto.CollectionRequestDto;
import org.nexo.postservice.dto.StoryRequestDto;
import org.nexo.postservice.dto.response.ResponseData;
import org.nexo.postservice.service.IStoryService;
import org.springframework.http.HttpHeaders;
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
    private static final String BEARER_PREFIX = "Bearer ";
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
        return new ResponseData<>(200, "Success", storyService.saveStory(postRequestDTO, files));
    }

    @DeleteMapping("/{id}")
    public ResponseData<String> deleteStory(@PathVariable Long id, @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        String accessToken = authHeader.replace(BEARER_PREFIX, "").trim();
        return new ResponseData<>(200, "Success", storyService.deleteStory(id));
    }

    @PutMapping("/{id}")
    public ResponseData<String> archiveStory(@PathVariable Long id) {
        return new ResponseData<>(200, "Success", storyService.archiveStory(id));
    }

    @PostMapping("/view/{id}")
    public ResponseData<String> viewStory(@PathVariable Long id) {
        return new ResponseData<>(200, "Success", storyService.viewStory(id));
    }

    @PostMapping("/like/{id}")
    public ResponseData<String> likeStory(@PathVariable Long id) {
        return new ResponseData<>(200, "Success", storyService.likeStory(id));
    }

    @GetMapping("/view/detail/{id}")
    public ResponseData<?> viewDetailStory(@PathVariable Long id, @RequestParam(defaultValue = "0") int pageNo, @RequestParam(defaultValue = "20") int pageSize) {
        return new ResponseData<>(200, "Success", storyService.viewDetailStory(id, pageNo, pageSize));
    }

    @GetMapping("/view/{id}")
    public ResponseData<?> getAllStoryOfFriend(@PathVariable Long id, @RequestParam(defaultValue = "0") int pageNo, @RequestParam(defaultValue = "20") int pageSize) {
        return new ResponseData<>(200, "Success", storyService.getAllStoryOfFriend(id, pageNo, pageSize));
    }

    @GetMapping("/{id}")
    public ResponseData<?> getStoriesOfUser(@PathVariable Long id, @RequestParam(defaultValue = "0") int pageNo, @RequestParam(defaultValue = "20") int pageSize) {
        return new ResponseData<>(200, "Success", storyService.getStoriesOfUser(id, pageNo, pageSize));
    }

    @GetMapping("/all/{id}")
    public ResponseData<?> getAllStoriesOfUser(@PathVariable Long id, @RequestParam(defaultValue = "0") int pageNo, @RequestParam(defaultValue = "20") int pageSize) {
        return new ResponseData<>(200, "Success", storyService.getAllStoriesOfUser(id, pageNo, pageSize));
    }

    @PostMapping("/collections")
    public ResponseData<String> saveCollection(@RequestBody @Valid CollectionRequestDto dto) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", storyService.saveCollection(dto));
    }

    @PutMapping("/collections")
    public ResponseData<String> updateCollection(@RequestBody @Valid CollectionRequestDto dto) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", storyService.saveCollection(dto));
    }


    @DeleteMapping("/collections/{id}")
    public ResponseData<String> deleteCollection(@PathVariable Long id) {
        return new ResponseData<>(200, "Collection deleted successfully", storyService.deleteCollection(id));
    }

    @GetMapping("/collections/{id}")
    public ResponseData<?> getMyCollections(@PathVariable Long id,
                                            @RequestParam(defaultValue = "0") int pageNo,
                                            @RequestParam(defaultValue = "10") int pageSize) {
        return new ResponseData<>(200, "Success", storyService.getAllCollections(id, pageNo, pageSize));
    }

    @GetMapping("/collections/my/detail/{id}")
    public ResponseData<?> getMyCollectionDetail(@PathVariable Long id) {
        return new ResponseData<>(200, "Success", storyService.getCollectionDetail(id));
    }

    @GetMapping("/collections/detail/{id}")
    public ResponseData<?> getFriendCollectionDetail(@PathVariable Long id) {
        return new ResponseData<>(200, "Success", storyService.getFriendCollectionDetail(id));
    }
}
