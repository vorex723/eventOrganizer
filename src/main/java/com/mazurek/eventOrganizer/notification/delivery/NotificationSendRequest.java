package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;

import java.util.Map;
import java.util.UUID;

public record NotificationSendRequest(
        UUID notificationId,
        UUID recipientId,
        String title,
        String body,
        NotificationResourceType resourceType,
        UUID resourceId,
        Map<String, String> metadata
) {
}
