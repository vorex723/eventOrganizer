package com.mazurek.eventOrganizer.notification.firebaseCloudMessaging;

public enum FcmSendOutcome {
    SENT,
    NO_TARGETS,
    RETRYABLE_FAILURE,
    PERMANENT_FAILURE
}
