package com.mazurek.eventOrganizer.exception.thread;

public class NotThreadReplyOwnerException extends RuntimeException {
    public NotThreadReplyOwnerException() {
        super();
    }

    public NotThreadReplyOwnerException(String message) {
        super(message);
    }

    public NotThreadReplyOwnerException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotThreadReplyOwnerException(Throwable cause) {
        super(cause);
    }

    protected NotThreadReplyOwnerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
