package org.nexo.interactionservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.interactionservice.dto.request.CommentDto;
import org.nexo.interactionservice.dto.response.ResponseData;
import org.nexo.interactionservice.service.ICommentService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comment")
@Validated
@Slf4j
@RequiredArgsConstructor
public class CommentController {
    private final ICommentService commentService;

    @PostMapping()
    public ResponseData<String> addComment(@RequestBody CommentDto dto) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", commentService.saveComment(dto));
    }

    @PutMapping
    public ResponseData<String> updateComment(@RequestBody CommentDto dto) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", commentService.saveComment(dto));
    }

    @DeleteMapping("/comment/{id}")
    public ResponseData<String> deleteComment(@PathVariable Long id) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", commentService.deleteComment(id));
    }

}
