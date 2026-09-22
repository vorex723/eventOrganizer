package com.mazurek.eventOrganizer.notification.delivery;

public enum NotificationSendOutcome {
    SENT,
    SKIPPED,
    RETRYABLE_FAILURE,
    PERMANENT_FAILURE
}
