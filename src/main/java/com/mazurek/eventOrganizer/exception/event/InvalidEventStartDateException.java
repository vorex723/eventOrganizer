package com.mazurek.eventOrganizer.exception.event;

public class InvalidEventStartDateException extends RuntimeException {
    public InvalidEventStartDateException() {
        super();
    }

    public InvalidEventStartDateException(String message) {
        super(message);
    }

    public InvalidEventStartDateException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidEventStartDateException(Throwable cause) {
        super(cause);
    }

    protected InvalidEventStartDateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
