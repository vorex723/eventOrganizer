package com.mazurek.eventOrganizer.notification.dto;

import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.notification.domain.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;



public record NotificationDto(
        UUID id,
        NotificationType type,
        String title,
        String body,
        NotificationResourceType resourceType,
        UUID resourceId,
       // Map<String, String> metadata,
        Instant createdAt,
        Instant readAt,
        boolean read
) {
    public NotificationDto(Notification notification) {
        this(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getResourceType(),
                notification.getResourceId(),
              //  notification.getMetadata(),
                notification.getCreatedAt(),
                notification.getReadAt(),
                notification.isRead()
        );
    }
}
