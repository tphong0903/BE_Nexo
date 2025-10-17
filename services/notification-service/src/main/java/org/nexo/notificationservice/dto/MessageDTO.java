package org.nexo.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageDTO {
    private Long recipientId;
    private Long actorId;
    private String notificationType;
    private String targetUrl;
}
