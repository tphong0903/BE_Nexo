package org.nexo.postservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.postservice.dto.response.ResponseData;
import org.nexo.postservice.service.IHashTagService;
import org.nexo.postservice.service.IPostService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/explore")
@Validated
@Slf4j
@RequiredArgsConstructor
public class ExploreController {
    private final IHashTagService hashTagService;
    private final IPostService postService;

    @GetMapping("/hashtags")
    public ResponseData<?> getTrendingHashtags(
            @RequestParam(defaultValue = "10") int top) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", hashTagService.getTrendingHashtags(top));
    }

    @GetMapping("")
    public ResponseData<?> getExplorePosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", postService.getPopularPosts(page, size));
    }
}
