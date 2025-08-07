package com.mazurek.eventOrganizer.exception.event;

public class AlreadyAttendingEventException extends RuntimeException {
    public static final String DEFAULT_MESSAGE = "You already attend this event.";

    public AlreadyAttendingEventException() {
        super(DEFAULT_MESSAGE);
    }

    public AlreadyAttendingEventException(String message) {
        super(message);
    }

    public AlreadyAttendingEventException(String message, Throwable cause) {
        super(message, cause);
    }

    public AlreadyAttendingEventException(Throwable cause) {
        super(cause);
    }

    protected AlreadyAttendingEventException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
