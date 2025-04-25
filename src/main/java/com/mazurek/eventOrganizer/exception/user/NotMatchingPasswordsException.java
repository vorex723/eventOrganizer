package com.mazurek.eventOrganizer.exception.user;

public class NotMatchingPasswordsException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "Passwords are not matching.";

    public NotMatchingPasswordsException() {
        super(DEFAULT_MESSAGE);
    }

    public NotMatchingPasswordsException(String message) {
        super(message);
    }

    public NotMatchingPasswordsException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotMatchingPasswordsException(Throwable cause) {
        super(cause);
    }

    protected NotMatchingPasswordsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
