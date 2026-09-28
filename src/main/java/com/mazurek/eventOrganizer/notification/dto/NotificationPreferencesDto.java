package com.mazurek.eventOrganizer.notification.dto;

import java.util.List;

public record NotificationPreferencesDto(
        long version,
        List<NotificationPreferenceDto> preferences
) {
}
