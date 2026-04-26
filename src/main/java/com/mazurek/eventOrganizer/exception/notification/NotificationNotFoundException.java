package com.mazurek.eventOrganizer.exception.notification;

public class NotificationNotFoundException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "There is no notification with this id.";

    public NotificationNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public NotificationNotFoundException(String message) {
        super(message);
    }

    public NotificationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotificationNotFoundException(Throwable cause) {
        super(cause);
    }

    protected NotificationNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
