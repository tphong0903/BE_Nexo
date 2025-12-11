package org.nexo.messagingservice.service;

import org.nexo.messagingservice.dto.MessageDTO;
import org.nexo.messagingservice.dto.ReactionDetailDTO;
import org.nexo.messagingservice.dto.SendMessageRequest;
import org.nexo.messagingservice.enums.EReactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MessageService {
    MessageDTO sendMessage(SendMessageRequest request, Long senderUserId);

    Page<MessageDTO> getMessages(Long conversationId, Pageable pageable, Long requestingUserId, String search);

    void deleteMessage(Long messageId, Long userId);

    void markAsRead(Long messageId, Long userId);

    void markConversationAsRead(Long conversationId, Long userId);

    Long getLastReadMessageId(Long conversationId, Long userId);

    void addReaction(Long messageId, Long userId, EReactionType reactionType);

    void removeReaction(Long messageId, Long userId, EReactionType reactionType);

    List<ReactionDetailDTO> getMessageReactions(Long messageId, Long requestingUserId);
}
