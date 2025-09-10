package org.nexo.interactionservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.nexo.interactionservice.model.CommentMentionModel;
import org.nexo.interactionservice.model.CommentModel;
import org.nexo.interactionservice.repository.ICommentMentionRepository;
import org.nexo.interactionservice.service.ICommentMentionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentMentionServiceImpl implements ICommentMentionService {
    private final ICommentMentionRepository commentMentionRepository;

    @Override
    public String addMentionComment(Long userId, CommentModel commentModel) {
        //TODO notification to user
        List<CommentMentionModel> commentMentionModelList = commentMentionRepository.findAllByCommentModelId(commentModel.getId());

        commentMentionRepository.deleteByCommentId(commentModel.getId());

        commentMentionRepository.save(CommentMentionModel.builder()
                .mentionUserId(userId)
                .commentModel(commentModel)
                .build());
        return "Success";
    }
}
