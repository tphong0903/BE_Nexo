package org.nexo.interactionservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nexo.interactionservice.dto.request.SavedPostStatusCheckRequestDTO;
import org.nexo.interactionservice.dto.response.PageModelResponse;
import org.nexo.interactionservice.dto.response.ResponseData;
import org.nexo.interactionservice.dto.response.SavedPostResponseDTO;
import org.nexo.interactionservice.dto.response.SavedPostStatusCheckResponseDTO;
import org.nexo.interactionservice.service.ISavedPostService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/api/saved-posts")
@RequiredArgsConstructor
public class SavedPostController {

    private final ISavedPostService savedPostService;

    @PostMapping("/{postId}")
    public ResponseData<SavedPostResponseDTO> savePost(@PathVariable Long postId) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Post saved successfully", savedPostService.savePost(postId));
    }

    @GetMapping
    public ResponseData<PageModelResponse<SavedPostResponseDTO>> getSavedPosts(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return new ResponseData<>(HttpStatus.OK.value(), "Success", savedPostService.getSavedPosts(pageable));
    }

    @PostMapping("/check")
    public SavedPostStatusCheckResponseDTO checkSavedPosts(@Valid @RequestBody SavedPostStatusCheckRequestDTO request) {
        return savedPostService.checkSavedPosts(request.getPostIds());
    }
}
