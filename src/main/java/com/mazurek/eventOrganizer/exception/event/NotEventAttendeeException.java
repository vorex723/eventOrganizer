package com.mazurek.eventOrganizer.exception.event;

public class NotEventAttendeeException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "You are not attending this event.";

    public NotEventAttendeeException() {
        super(DEFAULT_MESSAGE);
    }

    public NotEventAttendeeException(String message) {
        super(message);
    }

    public NotEventAttendeeException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotEventAttendeeException(Throwable cause) {
        super(cause);
    }

    protected NotEventAttendeeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
