package org.nexo.notificationservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import org.nexo.notificationservice.util.ENotificationType;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationModel extends AbstractEntity<Long> {
    private Long recipientId;
    private Long actorId;
    @Enumerated(EnumType.STRING)
    private ENotificationType notificationType;
    private String targetUrl;
    private String message;
    private Boolean isRead;
}
