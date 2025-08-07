package com.mazurek.eventOrganizer.exception.event;

public class EventOwnerMustAttendEventException extends RuntimeException {
    public static final String DEFAULT_MESSAGE = "As an event owner you have to be attending event.";

    public EventOwnerMustAttendEventException() {
        super(DEFAULT_MESSAGE);
    }

    public EventOwnerMustAttendEventException(String message) {
        super(message);
    }

    public EventOwnerMustAttendEventException(String message, Throwable cause) {
        super(message, cause);
    }

    public EventOwnerMustAttendEventException(Throwable cause) {
        super(cause);
    }

    protected EventOwnerMustAttendEventException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
