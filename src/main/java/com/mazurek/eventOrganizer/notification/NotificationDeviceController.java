package com.mazurek.eventOrganizer.notification;

import com.mazurek.eventOrganizer.notification.dto.NotificationDeviceDto;
import com.mazurek.eventOrganizer.notification.dto.RegisterNotificationDeviceDto;
import com.mazurek.eventOrganizer.notification.service.NotificationDeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notification-devices")
public class NotificationDeviceController {

    private final NotificationDeviceService notificationDeviceService;

    @PostMapping
    public ResponseEntity<NotificationDeviceDto> registerCurrentUserDevice(
            @Valid @RequestBody RegisterNotificationDeviceDto request
    ) {
        return ResponseEntity.ok(notificationDeviceService.registerCurrentUserDevice(request));
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> deleteCurrentUserDevice(@PathVariable UUID deviceId) {
        notificationDeviceService.deleteCurrentUserNotificationDevice(deviceId);

        return ResponseEntity.noContent().build();
    }
}
