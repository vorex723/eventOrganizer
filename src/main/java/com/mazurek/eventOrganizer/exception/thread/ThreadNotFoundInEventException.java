package com.mazurek.eventOrganizer.exception.thread;

public class ThreadNotFoundInEventException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "Thread not found in event.";

    public ThreadNotFoundInEventException() {
        super(DEFAULT_MESSAGE);
    }

    public ThreadNotFoundInEventException(String message) {
        super(message);
    }

    public ThreadNotFoundInEventException(String message, Throwable cause) {
        super(message, cause);
    }

    public ThreadNotFoundInEventException(Throwable cause) {
        super(cause);
    }

    protected ThreadNotFoundInEventException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
