package org.nexo.messagingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.nexo.messagingservice.enums.EMessageType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReplyStoryRequsestDTO {
    private Long userId;
    private String content;
    private EMessageType messageType;
    private Long storyId;
}
