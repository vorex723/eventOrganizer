package com.mazurek.eventOrganizer.notification.domain;

public enum NotificationDeliveryStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED,
    DEAD,
    SKIPPED
}
