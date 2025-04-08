package com.mazurek.eventOrganizer.exception.search;

public class NoSearchResultException extends RuntimeException {
    public NoSearchResultException() {
        super();
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
