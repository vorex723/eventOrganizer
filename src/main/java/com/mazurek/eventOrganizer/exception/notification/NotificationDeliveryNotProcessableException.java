package com.mazurek.eventOrganizer.exception.notification;

public class NotificationDeliveryNotProcessableException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "Notification delivery not processable";

    public NotificationDeliveryNotProcessableException() {
        super(DEFAULT_MESSAGE);
    }

    public NotificationDeliveryNotProcessableException(String message) {
        super(message);
    }

    public NotificationDeliveryNotProcessableException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotificationDeliveryNotProcessableException(Throwable cause) {
        super(cause);
    }

    protected NotificationDeliveryNotProcessableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
