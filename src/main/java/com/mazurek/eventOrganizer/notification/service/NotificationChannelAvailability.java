package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.config.properties.FirebaseProperties;
import com.mazurek.eventOrganizer.config.properties.NotificationProperties;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationChannelAvailability {

    private final FirebaseProperties firebaseProperties;
    private final NotificationProperties notificationProperties;

    public boolean isAvailable(NotificationChannel channel) {
        return switch (channel) {
            case PUSH_MOBILE, PUSH_WEB -> firebaseProperties.isEnabled();
            case EMAIL -> notificationProperties.getEmail().isEnabled();
        };
    }
}
