package com.mazurek.eventOrganizer.notification.dto;

import com.mazurek.eventOrganizer.notification.domain.DevicePlatform;
import com.mazurek.eventOrganizer.notification.domain.NotificationDevice;

import java.time.Instant;
import java.util.UUID;

public record NotificationDeviceDto(
        UUID id,
        DevicePlatform platform,
        Instant createdAt,
        Instant lastSeenAt
) {
    public NotificationDeviceDto(NotificationDevice device) {
        this(
                device.getId(),
                device.getPlatform(),
                device.getCreatedAt(),
                device.getLastSeenAt()
        );
    }
}
