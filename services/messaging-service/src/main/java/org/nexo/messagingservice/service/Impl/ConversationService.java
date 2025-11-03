package org.nexo.messagingservice.service.Impl;

import org.nexo.messagingservice.dto.ConversationResponseDTO;
import org.nexo.messagingservice.dto.PageModelResponse;
import org.springframework.data.domain.Pageable;

public interface ConversationService {
    ConversationResponseDTO getOrCreateDirectConversation(String keycloakUserId, Long recipientUserId);

    PageModelResponse<ConversationResponseDTO> getUserConversations(String keycloakUserId, Pageable pageable);

    ConversationResponseDTO getConversationById(Long conversationId, Long requestingUserId);

    void archiveConversation(Long conversationId, String requestingUserId);

    void muteConversation(Long conversationId, String requestingUserId);

    PageModelResponse<ConversationResponseDTO> getPendingRequests(String keycloakUserId, Pageable pageable);

    void acceptMessageRequest(Long conversationId, String keycloakUserId);

    void declineMessageRequest(Long conversationId, String keycloakUserId);
}