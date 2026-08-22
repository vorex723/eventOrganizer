package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.notification.dto.NotificationPreferenceDto;
import com.mazurek.eventOrganizer.notification.dto.UpdateNotificationPreferencesDto;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface NotificationPreferenceService {

    List<NotificationPreferenceDto> getCurrentUserNotificationPreferences();

    void updateCurrentUserNotificationPreferences(
            UpdateNotificationPreferencesDto request
    );

    Set<NotificationChannel> getEnabledExternalChannels(
            UUID userId,
            NotificationResourceType resourceType
    );

    boolean isEnabled(
            UUID userId,
            NotificationResourceType resourceType,
            NotificationChannel channel
    );
}
