package com.mazurek.eventOrganizer.exception.search;

public class NoSearchResultException extends RuntimeException {

    public static final String MESSAGE = "No event have matched your search parameters.";

    public NoSearchResultException() {
        super(MESSAGE);
    }

    public NoSearchResultException(String message) {
        super(message);
    }

    public NoSearchResultException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSearchResultException(Throwable cause) {
        super(cause);
    }

    protected NoSearchResultException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
