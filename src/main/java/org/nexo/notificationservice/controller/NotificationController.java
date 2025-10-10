package org.nexo.notificationservice.controller;

import lombok.RequiredArgsConstructor;
import org.nexo.notificationservice.dto.ReadNotificationGroupRequest;
import org.nexo.notificationservice.dto.ResponseData;
import org.nexo.notificationservice.service.INotificationService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class NotificationController {
    private final INotificationService notificationService;

    @GetMapping()
    public ResponseData<?> getNotifications(@PageableDefault(size = 20) Pageable pageable) {
        return new ResponseData<>(200, "Success", notificationService.getNotifications(pageable));
    }

    @GetMapping("/unread-count")
    public ResponseData<?> getNotificationsUnread() {
        return new ResponseData<>(200, "Success", notificationService.getNotificationsUnread());
    }

    @PutMapping("/{id}/read")
    public ResponseData<?> readNotification(@PathVariable Long id) {
        return new ResponseData<>(200, "Success", notificationService.readNotification(id));
    }

    @PutMapping("/read-all")
    public ResponseData<?> readAllNotification() {
        return new ResponseData<>(200, "Success", notificationService.readAllNotification());
    }

    @PutMapping("/read-group")
    public ResponseData<String> readNotificationGroup(@RequestBody ReadNotificationGroupRequest request) {
        notificationService.readNotificationGroup(request.getTargetUrl(), request.getNotificationType());
        return new ResponseData<>(200, "Success", "Success");
    }
}
