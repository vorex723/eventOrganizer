package com.mazurek.eventOrganizer.exception.event;

public class NoEventsException extends RuntimeException {

    private static final String MESSAGE = "There are no events.";

    public NoEventsException() {
        super(MESSAGE);
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
