package com.mazurek.eventOrganizer.notification.dto;

import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationType;
import jakarta.validation.constraints.NotNull;

public record UpdateNotificationPreferenceDto(
        @NotNull(message = "Notification type must be provided.")
        NotificationType notificationType,

        @NotNull(message = "Notification channel must be provided.")
        NotificationChannel channel,

        boolean enabled
) {
}
