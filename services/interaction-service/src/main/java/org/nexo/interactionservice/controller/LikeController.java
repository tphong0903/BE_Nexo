package org.nexo.interactionservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.interactionservice.dto.response.ResponseData;
import org.nexo.interactionservice.service.ILikeService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/like")
@Validated
@Slf4j
@RequiredArgsConstructor
public class LikeController {
    private final ILikeService likeService;

    @PostMapping("/comment/{id}")
    public ResponseData<String> saveLikeComment(@PathVariable Long id) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", likeService.saveLikeComment(id));
    }

    @PostMapping("/post/{id}")
    public ResponseData<String> saveLikePost(@PathVariable Long id) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", likeService.saveLikePost(id));
    }

    @PostMapping("/reel/{id}")
    public ResponseData<String> saveLikeReel(@PathVariable Long id) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", likeService.saveLikeReel(id));
    }
}
