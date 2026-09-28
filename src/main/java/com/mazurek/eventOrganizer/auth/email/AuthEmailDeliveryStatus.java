package com.mazurek.eventOrganizer.auth.email;

public enum AuthEmailDeliveryStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED,
    DEAD,
    CANCELLED
}
