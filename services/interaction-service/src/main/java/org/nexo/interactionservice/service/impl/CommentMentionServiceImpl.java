package org.nexo.interactionservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.nexo.interactionservice.dto.MessageDTO;
import org.nexo.interactionservice.model.CommentMentionModel;
import org.nexo.interactionservice.model.CommentModel;
import org.nexo.interactionservice.repository.ICommentMentionRepository;
import org.nexo.interactionservice.service.ICommentMentionService;
import org.nexo.interactionservice.util.Enum.ENotificationType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentMentionServiceImpl implements ICommentMentionService {
    private final ICommentMentionRepository commentMentionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public String addMentionComment(Long userId, CommentModel commentModel) {
        List<CommentMentionModel> commentMentionModelList = commentMentionRepository.findAllByCommentModelId(commentModel.getId());

        commentMentionRepository.deleteByCommentId(commentModel.getId());

        commentMentionRepository.save(CommentMentionModel.builder()
                .mentionUserId(userId)
                .commentModel(commentModel)
                .build());
        MessageDTO messageDTO = MessageDTO.builder()
                .actorId(commentModel.getUserId())
                .recipientId(userId)
                .notificationType(String.valueOf(ENotificationType.MENTION_COMMENT))
                .targetUrl(commentModel.getPostId() != null ? "/posts/" + commentModel.getPostId() : "/reels/" + commentModel.getReelId())
                .build();
        kafkaTemplate.send("notification", messageDTO);
        return "Success";
    }
}
