package com.mazurek.eventOrganizer.exception.event;

public class EventAlreadyHadPlaceException extends RuntimeException {
    public EventAlreadyHadPlaceException() {
        super();
    }

    public EventAlreadyHadPlaceException(String message) {
        super(message);
    }

    public EventAlreadyHadPlaceException(String message, Throwable cause) {
        super(message, cause);
    }

    public EventAlreadyHadPlaceException(Throwable cause) {
        super(cause);
    }

    protected EventAlreadyHadPlaceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
