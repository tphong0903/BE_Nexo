package org.nexo.notificationservice.service;

import org.nexo.notificationservice.dto.PageModelResponse;
import org.springframework.data.domain.Pageable;

public interface INotificationService {
    PageModelResponse<?> getNotifications(Pageable pageable);

    Long getNotificationsUnread();

    String readNotification(Long id);

    String readAllNotification();

    void readNotificationGroup(String targetUrl, String notificationType);
}
