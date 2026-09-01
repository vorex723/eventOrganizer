package com.mazurek.eventOrganizer.exception.notification;

public class NotificationSendFailException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Notification send fail.";

    public NotificationSendFailException() {
        super();
    }

    public NotificationSendFailException(String message) {
        super(message);
    }

    public NotificationSendFailException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotificationSendFailException(Throwable cause) {
        super(cause);
    }

    protected NotificationSendFailException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
