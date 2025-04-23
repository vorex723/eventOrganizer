package com.mazurek.eventOrganizer.exception.event;

public class NotAttenderException extends RuntimeException {

    private static final String MESSAGE = "You are not attending this event.";

    public NotAttenderException() {
        super(MESSAGE);
    }

    public NotAttenderException(String message) {
        super(message);
    }

    public NotAttenderException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotAttenderException(Throwable cause) {
        super(cause);
    }

    protected NotAttenderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
