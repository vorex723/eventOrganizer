package com.mazurek.eventOrganizer.exception.thread;

public class ThreadNotFoundException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "There is no thread with this id.";

    public ThreadNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public ThreadNotFoundException(String message) {
        super(message);
    }

    public ThreadNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ThreadNotFoundException(Throwable cause) {
        super(cause);
    }

    protected ThreadNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
