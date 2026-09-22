package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.config.properties.NotificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class NotificationRetryPolicy {

    private final NotificationProperties notificationProperties;

    public boolean attemptsExhausted(int attemptCount) {
        return attemptCount >= notificationProperties.getDelivery().getMaxAttempts();
    }

    public Duration delayAfterFailedAttempt(int attemptCount) {
        if (attemptCount < 1 || attemptsExhausted(attemptCount)) {
            throw new IllegalArgumentException("A retry delay is only available for a non-final failed attempt.");
        }

        return notificationProperties.getDelivery().getRetryDelays().get(attemptCount - 1);
    }
}
