package com.mazurek.eventOrganizer.exception.event;

public class NoEventsException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "There are no events.";

    public NoEventsException() {
        super(DEFAULT_MESSAGE);
    }

    public NoEventsException(String message) {
        super(message);
    }

    public NoEventsException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoEventsException(Throwable cause) {
        super(cause);
    }

    protected NoEventsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
