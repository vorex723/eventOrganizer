package com.mazurek.eventOrganizer.exception.thread;

public class NotThreadOwnerException extends RuntimeException{
    public NotThreadOwnerException() {
        super();
    }

    public NotThreadOwnerException(String message) {
        super(message);
    }

    public NotThreadOwnerException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotThreadOwnerException(Throwable cause) {
        super(cause);
    }

    protected NotThreadOwnerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
