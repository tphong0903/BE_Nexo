package org.nexo.messagingservice.dto;

import lombok.AllArgsConstructor;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import org.nexo.messagingservice.enums.EConversationStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponseDTO {
    private Long id;
    private String fullname;
    private String avatarUrl;
    private List<UserDTO> participants;
    private MessageDTO lastMessage;
    private Long unreadCount;
    private EConversationStatus status;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
    private boolean isBlockedByMe;
    private Long lastReadMessageId;
    private Boolean onlineStatus;
}
