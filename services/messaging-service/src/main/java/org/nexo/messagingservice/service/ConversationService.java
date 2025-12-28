package org.nexo.messagingservice.service;

import org.nexo.messagingservice.dto.ConversationResponseDTO;
import org.nexo.messagingservice.dto.NicknameRequest;
import org.nexo.messagingservice.dto.PageModelResponse;
import org.springframework.data.domain.Pageable;

public interface ConversationService {
    ConversationResponseDTO getOrCreateDirectConversation(String keycloakUserId, Long recipientUserId);

    PageModelResponse<ConversationResponseDTO> getUserConversations(String keycloakUserId, Pageable pageable);

    ConversationResponseDTO getConversationById(Long conversationId, Long requestingUserId);

    void archiveConversation(Long conversationId, String requestingUserId);

    void muteConversation(Long conversationId, String requestingUserId);

    PageModelResponse<ConversationResponseDTO> getPendingRequests(String keycloakUserId, Pageable pageable);

    PageModelResponse<ConversationResponseDTO> getUnreadConversations(String keycloakUserId, Pageable pageable);

    PageModelResponse<ConversationResponseDTO> getArchivedConversations(String keycloakUserId, Pageable pageable);

    void acceptMessageRequest(Long conversationId, String keycloakUserId);

    void declineMessageRequest(Long conversationId, String keycloakUserId);

    ConversationResponseDTO setNickname(Long conversationId, String keycloakUserId, NicknameRequest request);

    ConversationResponseDTO getNickname(Long conversationId, String keycloakUserId);
    
    void handleBlockStatusChange(Long userId1, Long userId2, boolean isBlocked);
}