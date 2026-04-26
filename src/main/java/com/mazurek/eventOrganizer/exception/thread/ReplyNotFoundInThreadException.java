package com.mazurek.eventOrganizer.exception.thread;

public class ReplyNotFoundInThreadException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "Reply not found in thread.";

    public ReplyNotFoundInThreadException() {
        super(DEFAULT_MESSAGE);
    }

    public ReplyNotFoundInThreadException(String message) {
        super(message);
    }

    public ReplyNotFoundInThreadException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReplyNotFoundInThreadException(Throwable cause) {
        super(cause);
    }

    protected ReplyNotFoundInThreadException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
