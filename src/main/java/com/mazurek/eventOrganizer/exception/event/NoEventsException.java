package com.mazurek.eventOrganizer.exception.event;

public class NoEventsException extends RuntimeException {
    public NoEventsException() {
        super();
    }

    public NoEventsException(String message) {
        super(message);
    }

    public NoEventsException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoEventsException(Throwable cause) {
        super(cause);
    }

    protected NoEventsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
