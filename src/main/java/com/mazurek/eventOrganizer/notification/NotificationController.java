package com.mazurek.eventOrganizer.notification;

import com.mazurek.eventOrganizer.notification.dto.NotificationPageDto;
import com.mazurek.eventOrganizer.notification.dto.NotificationUnreadCountDto;
import com.mazurek.eventOrganizer.notification.service.NotificationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    @GetMapping
    public ResponseEntity<NotificationPageDto> getUserNotifications(@RequestParam(name = "page", defaultValue = "0") int pageNumber) {
        return ResponseEntity.ok(
                notificationQueryService.getCurrentUserNotifications(pageNumber)
        );
    }

    @GetMapping("/unread-count")
    public ResponseEntity<NotificationUnreadCountDto> getUserUnreadNotificationCount() {
        return ResponseEntity.ok(
                notificationQueryService.getCurrentUserUnreadCount()
        );
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> readAllUserNotifications() {
        notificationQueryService.markAllAsRead();
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> readUserNotification(@PathVariable UUID notificationId) {
        notificationQueryService.markAsRead(notificationId);
        return ResponseEntity.noContent().build();
    }

}
