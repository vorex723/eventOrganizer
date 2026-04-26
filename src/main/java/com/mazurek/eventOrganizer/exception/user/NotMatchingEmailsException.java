package com.mazurek.eventOrganizer.exception.user;

public class NotMatchingEmailsException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "Email addresses are not matching.";

    public NotMatchingEmailsException() {
        super(DEFAULT_MESSAGE);
    }

    public NotMatchingEmailsException(String message) {
        super(message);
    }

    public NotMatchingEmailsException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotMatchingEmailsException(Throwable cause) {
        super(cause);
    }

    protected NotMatchingEmailsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
