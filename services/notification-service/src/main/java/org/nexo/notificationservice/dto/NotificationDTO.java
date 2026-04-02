package org.nexo.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDTO implements Serializable {
    private Long id;
    private Long recipientId;
    private String notificationType;
    private String targetUrl;
    private String message;
    private Boolean isRead;
    private List<UserDTO> userList;
    private LocalDateTime createdAt;
}
