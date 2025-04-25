package com.mazurek.eventOrganizer.exception.search;

public class NoSearchParametersPresentException extends RuntimeException{

    public static final String DEFAULT_MESSAGE = "No search parameters present";

    public NoSearchParametersPresentException() {
        super(DEFAULT_MESSAGE);
    }

    public NoSearchParametersPresentException(String message) {
        super(message);
    }

    public NoSearchParametersPresentException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSearchParametersPresentException(Throwable cause) {
        super(cause);
    }

    protected NoSearchParametersPresentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
