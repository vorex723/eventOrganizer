package com.mazurek.eventOrganizer.notification.dto;

import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationPreference;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;

public record NotificationPreferenceDto(
        NotificationResourceType resourceType,
        NotificationChannel channel,
        boolean enabled
) {
    public NotificationPreferenceDto(NotificationPreference preference) {
        this(
                preference.getResourceType(),
                preference.getChannel(),
                preference.isEnabled()
        );
    }
}
