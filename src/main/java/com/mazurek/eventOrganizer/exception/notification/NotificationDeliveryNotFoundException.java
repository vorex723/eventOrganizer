package com.mazurek.eventOrganizer.exception.notification;

public class NotificationDeliveryNotFoundException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Notification delivery not found.";

    public NotificationDeliveryNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public NotificationDeliveryNotFoundException(String message) {
        super(message);
    }

    public NotificationDeliveryNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotificationDeliveryNotFoundException(Throwable cause) {
        super(cause);
    }

    protected NotificationDeliveryNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
