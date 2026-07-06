package org.nexo.interactionservice.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nexo.interactionservice.dto.MessageDTO;
import org.nexo.interactionservice.model.CommentMentionModel;
import org.nexo.interactionservice.model.CommentModel;
import org.nexo.interactionservice.repository.ICommentMentionRepository;
import org.nexo.interactionservice.service.ICommentMentionService;
import org.nexo.interactionservice.util.Enum.ENotificationType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentMentionServiceImpl implements ICommentMentionService {

    private final ICommentMentionRepository commentMentionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional
    public String addMentionComment(Long userId, CommentModel commentModel) {
        boolean isAlreadyMentioned = commentMentionRepository
                .existsByCommentModelIdAndMentionUserId(commentModel.getId(), userId);

        if (isAlreadyMentioned) {
            return "Already mentioned";
        }

        commentMentionRepository.save(CommentMentionModel.builder()
                .mentionUserId(userId)
                .commentModel(commentModel)
                .build());

        sendNotification(userId, commentModel);

        log.info("Successfully added mention for user {} in comment {}", userId, commentModel.getId());
        return "Success";
    }

    @Override
    @Transactional
    public String syncMentionComment(List<Long> mentionedUserIds, CommentModel commentModel) {
        if (mentionedUserIds == null || mentionedUserIds.isEmpty()) return "No mentions";

        List<CommentMentionModel> existingMentions = commentMentionRepository.findAllByCommentModelId(commentModel.getId());
        Set<Long> existingUserIds = existingMentions.stream()
                .map(CommentMentionModel::getMentionUserId)
                .collect(Collectors.toSet());

        List<Long> newUserIds = mentionedUserIds.stream()
                .filter(id -> !existingUserIds.contains(id))
                .toList();

        for (Long newUserId : newUserIds) {
            commentMentionRepository.save(CommentMentionModel.builder()
                    .mentionUserId(newUserId)
                    .commentModel(commentModel)
                    .build());

            sendNotification(newUserId, commentModel);
        }

        log.info("Synced mentions for comment {}. Added {} new mentions.", commentModel.getId(), newUserIds.size());
        return "Success";
    }

    private void sendNotification(Long recipientId, CommentModel commentModel) {
        String targetUrl = commentModel.getPostId() != null
                ? "/posts/" + commentModel.getPostId()
                : "/reels/" + commentModel.getReelId();

        MessageDTO messageDTO = MessageDTO.builder()
                .actorId(commentModel.getUserId())
                .recipientId(recipientId)
                .notificationType(ENotificationType.MENTION_COMMENT.name())
                .targetUrl(targetUrl)
                .build();

        kafkaTemplate.send("notification", messageDTO);
    }
}
