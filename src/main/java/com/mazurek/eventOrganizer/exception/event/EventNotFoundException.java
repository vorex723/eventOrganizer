package com.mazurek.eventOrganizer.exception.event;

public class EventNotFoundException extends RuntimeException{

    public static final String DEFAULT_MESSAGE = "There is no event with this id.";

    public EventNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public EventNotFoundException(String message) {
        super(message);
    }

    public EventNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public EventNotFoundException(Throwable cause) {
        super(cause);
    }

    protected EventNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
