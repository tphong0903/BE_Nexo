package org.nexo.interactionservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.interactionservice.dto.request.LikeCommentRequestDto;
import org.nexo.interactionservice.dto.request.LikePostRequestDto;
import org.nexo.interactionservice.dto.response.ResponseData;
import org.nexo.interactionservice.service.ICommentService;
import org.nexo.interactionservice.service.ILikeService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/like")
@Validated
@Slf4j
@RequiredArgsConstructor
public class LikeController {
    private final ICommentService commentService;
    private final ILikeService likeService;

    @DeleteMapping("/comment/{id}")
    public ResponseData<String> deleteComment(@PathVariable Long id) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", commentService.deleteComment(id));
    }

    @PutMapping("/comment/like")
    public ResponseData<String> saveLikeComment(@RequestBody LikeCommentRequestDto dto) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", likeService.saveLikeComment(dto));
    }

    @PostMapping("/post/like")
    public ResponseData<String> saveLikePost(@RequestBody LikePostRequestDto dto) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", likeService.saveLikePost(dto));
    }
}
