package com.mazurek.eventOrganizer.exception.notification;

public class StaleNotificationPreferencesException extends RuntimeException {

    public StaleNotificationPreferencesException() {
        super("Notification preferences were changed by another session. Refresh and try again.");
    }
}
