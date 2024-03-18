package com.mazurek.eventOrganizer.exception.converastion;

public class MessagingYourselfException extends RuntimeException {
    public MessagingYourselfException() {
        super();
    }

    public MessagingYourselfException(String message) {
        super(message);
    }

    public MessagingYourselfException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessagingYourselfException(Throwable cause) {
        super(cause);
    }

    protected MessagingYourselfException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
