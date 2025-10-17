package org.nexo.notificationservice.repository;

import org.nexo.notificationservice.model.NotificationModel;
import org.nexo.notificationservice.util.ENotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface INotificationRepository extends JpaRepository<NotificationModel, Long> {
    Page<NotificationModel> getNotificationModelByRecipientId(Long id, Pageable pageable);

    List<NotificationModel> getAllByRecipientIdAndIsRead(Long id, Boolean isRead);

    Long countByRecipientIdAndIsRead(Long id, Boolean isRead);

    List<NotificationModel> findAllByRecipientIdAndTargetUrlAndNotificationTypeAndIsRead(
            Long recipientId,
            String targetUrl,
            ENotificationType notificationType,
            boolean isRead
    );
}
