package org.nexo.notificationservice.controller;

import lombok.RequiredArgsConstructor;
import org.nexo.notificationservice.dto.ResponseData;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class NotificationController {
    @GetMapping
    public ResponseData<?> getNotifications(@PageableDefault(size = 20) Pageable pageable) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", "Test Post Service");
    }

    @GetMapping("/unread-count")
    public ResponseData<?> getNotificationsUnread() {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", 4);
    }

    @PutMapping("/{id}/read")
    public ResponseData<?> readNotification(@PathVariable Long id) {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", "hehe");
    }

    @PutMapping("/read-all")
    public ResponseData<?> readAllNotification() {
        return new ResponseData<>(HttpStatus.CREATED.value(), "Success", "hehee");
    }
}
