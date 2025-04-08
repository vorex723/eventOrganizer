package com.mazurek.eventOrganizer.exception.event;

public class NotAttenderException extends RuntimeException {
    public NotAttenderException() {
        super();
    }

    public NotAttenderException(String message) {
        super(message);
    }

    public NotAttenderException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotAttenderException(Throwable cause) {
        super(cause);
    }

    protected NotAttenderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
