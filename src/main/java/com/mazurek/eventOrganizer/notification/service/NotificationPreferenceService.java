package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.notification.dto.NotificationPreferenceDto;
import com.mazurek.eventOrganizer.notification.dto.UpdateNotificationPreferencesDto;
import com.mazurek.eventOrganizer.notification.dto.NotificationPreferencesDto;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface NotificationPreferenceService {

    List<NotificationPreferenceDto> getCurrentUserNotificationPreferences();

    NotificationPreferencesDto getCurrentUserNotificationPreferencesWithVersion();

    NotificationPreferencesDto updateCurrentUserNotificationPreferences(
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
