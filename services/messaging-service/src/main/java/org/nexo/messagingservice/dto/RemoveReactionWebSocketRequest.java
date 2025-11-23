package org.nexo.messagingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nexo.messagingservice.enums.EReactionType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemoveReactionWebSocketRequest {
    private Long messageId;
    private EReactionType reactionType;
}