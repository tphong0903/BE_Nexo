package org.nexo.interactionservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.nexo.interactionservice.dto.request.CommentDto;
import org.nexo.interactionservice.exception.CustomException;
import org.nexo.interactionservice.model.CommentModel;
import org.nexo.interactionservice.repository.ICommentRepository;
import org.nexo.interactionservice.service.ICommentService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements ICommentService {
    private final ICommentRepository commentRepository;

    @Override
    public String saveComment(CommentDto a) {
        CommentModel model;
        if (a.getId() != 0) {
            model = commentRepository.findById(a.getId()).orElseThrow(() -> new CustomException("Comment is not exist", HttpStatus.BAD_REQUEST));
        } else {
            model = CommentModel.builder()
                    .content(a.getContent())
                    .userId(a.getUserId())
                    .build();
        }


        if (a.getPostId() != null) {
            model.setPostId(a.getPostId());
        } else {
            model.setReelId(a.getReelId());
        }

        if (a.getParentId() != null) {
            model.setParentComment(commentRepository.findById(a.getParentId()).orElseThrow(() -> new CustomException("Comment is not exist", HttpStatus.BAD_REQUEST)));
        }

        commentRepository.save(model);

        return "Success";
    }


    @Override
    public String deleteComment(Long id) {
        CommentModel model = commentRepository.findById(id).orElseThrow(() -> new CustomException("Comment is not exist", HttpStatus.BAD_REQUEST));
        commentRepository.delete(model);
        return "Success";
    }
}
