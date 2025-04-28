package com.mazurek.eventOrganizer.exception.event;

public class NotEventAttenderException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "You are not attending this event.";

    public NotEventAttenderException() {
        super(DEFAULT_MESSAGE);
    }

    public NotEventAttenderException(String message) {
        super(message);
    }

    public NotEventAttenderException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotEventAttenderException(Throwable cause) {
        super(cause);
    }

    protected NotEventAttenderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
