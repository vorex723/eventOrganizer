package com.mazurek.eventOrganizer.exception.converastion;

public class MessagingYourselfException extends RuntimeException {

    private static final String MESSAGE = "You can not send messages to yourself.";

    public MessagingYourselfException() {
        super(MESSAGE);
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
