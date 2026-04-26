package com.mazurek.eventOrganizer.exception.thread;

public class ThreadReplyNotFoundException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "There is no reply with this id.";

    public ThreadReplyNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public ThreadReplyNotFoundException(String message) {
        super(message);
    }

    public ThreadReplyNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ThreadReplyNotFoundException(Throwable cause) {
        super(cause);
    }

    protected ThreadReplyNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
