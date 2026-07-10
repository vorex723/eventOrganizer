package com.mazurek.eventOrganizer.notification.dto;

import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationPreference;
import com.mazurek.eventOrganizer.notification.domain.NotificationType;

public record NotificationPreferenceDto(
        NotificationType notificationType,
        NotificationChannel channel,
        boolean enabled
) {
    public NotificationPreferenceDto(NotificationPreference preference) {
        this(
                preference.getNotificationType(),
                preference.getChannel(),
                preference.isEnabled()
        );
    }
}
