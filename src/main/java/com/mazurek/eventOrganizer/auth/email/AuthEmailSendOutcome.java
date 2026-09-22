package com.mazurek.eventOrganizer.auth.email;

public enum AuthEmailSendOutcome {
    SENT,
    RETRYABLE_FAILURE,
    PERMANENT_FAILURE
}
