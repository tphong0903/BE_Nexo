package org.nexo.interactionservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.nexo.interactionservice.dto.request.LikeCommentRequestDto;
import org.nexo.interactionservice.dto.request.LikePostRequestDto;
import org.nexo.interactionservice.exception.CustomException;
import org.nexo.interactionservice.model.LikeCommentModel;
import org.nexo.interactionservice.model.LikeModel;
import org.nexo.interactionservice.repository.ICommentRepository;
import org.nexo.interactionservice.repository.ILikeCommentRepository;
import org.nexo.interactionservice.repository.ILikeRepository;
import org.nexo.interactionservice.service.ILikeService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements ILikeService {
    private final ILikeCommentRepository likeCommentRepository;
    private final ICommentRepository commentRepository;
    private final ILikeRepository likeRepository;

    @Override
    public String saveLikeComment(LikeCommentRequestDto dto) {
        if (dto.getId() != 0) {
            likeCommentRepository.delete(likeCommentRepository.findById(dto.getId()).orElseThrow(() -> new CustomException("LikeComment is not exist", HttpStatus.BAD_REQUEST)));
        } else {
            LikeCommentModel model = LikeCommentModel.builder()
                    .commentModel(commentRepository.findById(dto.getCommentId()).orElseThrow(() -> new CustomException("Comment is not exist", HttpStatus.BAD_REQUEST)))
                    .userId(dto.getUserId())
                    .build();
            likeCommentRepository.save(model);
        }
        return "Success";
    }

    @Override
    public String saveLikePost(LikePostRequestDto dto) {
        if (dto.getId() != 0) {
            LikeModel model = likeRepository.findById(dto.getId()).orElseThrow(() -> new CustomException("LikePost is not exist", HttpStatus.BAD_REQUEST));
            likeRepository.delete(model);
        } else {
            LikeModel model = LikeModel.builder()
                    .userId(dto.getUserId())
                    .build();
            if (dto.getReelId() != 0) {
                model.setReelId(dto.getReelId());
            } else if (dto.getPostId() != 0) {
                model.setPostId(dto.getPostId());
            }
            likeRepository.save(model);
        }
        return "Success";
    }
}
