package com.mazurek.eventOrganizer.notification.dto;

import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import jakarta.validation.constraints.NotNull;

public record UpdateNotificationPreferenceDto(
        @NotNull(message = "Notification resource type must be provided.")
        NotificationResourceType resourceType,

        @NotNull(message = "Notification channel must be provided.")
        NotificationChannel channel,

        boolean enabled
) {
}
