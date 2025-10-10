package org.nexo.notificationservice.dto;

import lombok.Data;

@Data
public class ReadNotificationGroupRequest {
    private String targetUrl;
    private String notificationType;
}