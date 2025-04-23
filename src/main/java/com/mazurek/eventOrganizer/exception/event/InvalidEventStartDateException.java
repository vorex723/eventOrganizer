package com.mazurek.eventOrganizer.exception.event;

public class InvalidEventStartDateException extends RuntimeException {
    private static final String MESSAGE = "You can not create event with start date in the past. It has to exceed at lest 48 hours from moment of creation";

    public InvalidEventStartDateException() {
        super(MESSAGE);
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
