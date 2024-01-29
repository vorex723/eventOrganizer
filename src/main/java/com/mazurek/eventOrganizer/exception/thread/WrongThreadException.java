package com.mazurek.eventOrganizer.exception.thread;

public class WrongThreadException extends RuntimeException {
    public WrongThreadException() {
        super();
    }

    public WrongThreadException(String message) {
        super(message);
    }

    public WrongThreadException(String message, Throwable cause) {
        super(message, cause);
    }

    public WrongThreadException(Throwable cause) {
        super(cause);
    }

    protected WrongThreadException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
