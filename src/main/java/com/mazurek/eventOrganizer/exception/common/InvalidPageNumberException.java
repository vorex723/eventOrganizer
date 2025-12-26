package com.mazurek.eventOrganizer.exception.common;

public class InvalidPageNumberException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "Invalid page number.";

    public InvalidPageNumberException() {
        super(DEFAULT_MESSAGE);
    }

    public InvalidPageNumberException(String message) {
        super(message);
    }

    public InvalidPageNumberException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPageNumberException(Throwable cause) {
        super(cause);
    }

    protected InvalidPageNumberException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
