package org.nexo.notificationservice.dto;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nexo.notificationservice.util.ENotificationType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDTO {
    private Long id;
    private Long recipientId;
    private String notificationType;
    private String targetUrl;
    private String message;
    private Boolean isRead;
    private List<UserDTO> userList;
    private LocalDateTime createdAt;
}
