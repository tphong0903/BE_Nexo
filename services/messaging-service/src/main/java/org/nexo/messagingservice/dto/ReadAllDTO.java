package org.nexo.messagingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadAllDTO {
    private Long conversationId;
    private Long lastReadMessageId;
    private Long userId;
}
