package org.nexo.messagingservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String username;
    private Long senderUserId;
    private List<UserDTO> participants;
    private MessageDTO lastMessage;
    private Long unreadCount;
    private EConversationStatus status;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
    @JsonProperty("blockedByMe")
    private boolean isBlockedByMe;
    private Long lastReadMessageId;
    private Boolean onlineStatus;

    // Group fields
    @JsonProperty("isGroup")
    private boolean isGroup;
    private String groupName;
    private String groupAvatarUrl;
    private Long createdByUserId;
    @JsonProperty("isGroupAdmin")
    private boolean isGroupAdmin;

    private Long activeCallId;
    private String activeCallType;
}
