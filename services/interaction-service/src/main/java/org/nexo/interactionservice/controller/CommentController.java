package org.nexo.interactionservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.interactionservice.dto.request.CommentDto;
import org.nexo.interactionservice.dto.response.ListCommentResponse;
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
        return new ResponseData<>(200, "Success", commentService.saveComment(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseData<String> deleteComment(@PathVariable Long id) {
        return new ResponseData<>(200, "Success", commentService.deleteComment(id));
    }

    @GetMapping("/post/{postId}")
    public ResponseData<ListCommentResponse> getCommentOfPost(@PathVariable Long postId,
                                                              @RequestParam(defaultValue = "0") int pageNo,
                                                              @RequestParam(defaultValue = "10") int pageSize) {
        return new ResponseData<>(200, "Success", commentService.getCommentOfPost(postId, pageNo, pageSize));
    }

    @GetMapping("/reel/{reelId}")
    public ResponseData<ListCommentResponse> getCommentOfReel(@PathVariable Long reelId,
                                                              @RequestParam(defaultValue = "0") int pageNo,
                                                              @RequestParam(defaultValue = "10") int pageSize) {
        return new ResponseData<>(200, "Success", commentService.getCommentOfReel(reelId, pageNo, pageSize));
    }

    @GetMapping("/{commentId}/replies")
    public ResponseData<ListCommentResponse> getReplies(
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return new ResponseData<>(200, "Success", commentService.getReplies(commentId, pageNo, pageSize));
    }
}
