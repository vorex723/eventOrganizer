package com.mazurek.eventOrganizer.exception.notification;

public class InvalidNotificationPreferencesException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Invalid notification preferences matrix.";

    public InvalidNotificationPreferencesException() {
        super(DEFAULT_MESSAGE);
    }

    public InvalidNotificationPreferencesException(String message) {
        super(message);
    }

    public InvalidNotificationPreferencesException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidNotificationPreferencesException(Throwable cause) {
        super(cause);
    }

    protected InvalidNotificationPreferencesException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
