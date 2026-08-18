package com.mazurek.eventOrganizer.notification.dto;

import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;

import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        String title,
        String body,
        NotificationResourceType resourceType,
        UUID resourceId,
        NotificationResourceType parentResourceType,
        UUID parentResourceId,
        Instant createdAt,
        Instant readAt,
        boolean read
) {
    public NotificationDto(Notification notification) {
        this(
                notification.getId(),
                notification.getTitle(),
                notification.getBody(),
                notification.getResourceType(),
                notification.getResourceId(),
                notification.getParentResourceType(),
                notification.getParentResourceId(),
                notification.getCreatedAt(),
                notification.getReadAt(),
                notification.isRead()
        );
    }
}
