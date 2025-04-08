package com.mazurek.eventOrganizer.exception.thread;

public class ReplyNotFoundException extends RuntimeException {
    public ReplyNotFoundException() {
        super();
    }

    public ReplyNotFoundException(String message) {
        super(message);
    }

    public ReplyNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReplyNotFoundException(Throwable cause) {
        super(cause);
    }

    protected ReplyNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
