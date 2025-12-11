package org.nexo.messagingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import org.nexo.messagingservice.enums.EMessageType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    private Long conversationId;
    private String content;
    private EMessageType messageType;
    private Long replyToMessageId;
    private List<String> mediaUrls;
}