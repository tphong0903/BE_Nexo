package org.nexo.messagingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadMessageRequest {
    private Long messageId;
    private Long conversationId;
}