package org.nexo.messagingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nexo.messagingservice.enums.EMessageType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private Long id;
    private Long conversationId;
    private UserDTO sender;
    private String content;
    private EMessageType messageType;
    private Long replyToMessageId;
    private MessageDTO replyToMessage;
    private List<MessageMediaDTO> mediaList;
    private List<ReactionDTO> reactions;
    private Boolean isEdited;
    private LocalDateTime editedAt;
    private LocalDateTime createdAt;
}