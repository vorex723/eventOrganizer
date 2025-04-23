package com.mazurek.eventOrganizer.exception.thread;

public class WrongThreadException extends RuntimeException {

    private static final String MESSAGE = "This reply is not in this thread.";

    public WrongThreadException() {
        super(MESSAGE);
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
