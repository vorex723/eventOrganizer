package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationType;
import com.mazurek.eventOrganizer.notification.dto.NotificationPreferenceDto;
import com.mazurek.eventOrganizer.notification.dto.UpdateNotificationPreferencesDto;

import java.util.List;
import java.util.UUID;

public interface NotificationPreferenceService {
    List<NotificationPreferenceDto> getCurrentUserNotificationPreferences();
    void updateCurrentUserNotificationPreferences(UpdateNotificationPreferencesDto updateNotificationPreferencesDto);
    boolean isEnabled(UUID userId, NotificationType type, NotificationChannel channel);
}
