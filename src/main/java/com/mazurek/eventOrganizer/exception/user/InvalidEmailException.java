package com.mazurek.eventOrganizer.exception.user;

public class InvalidEmailException extends RuntimeException {

    private static final String MESSAGE = "Invalid email.";

    public InvalidEmailException() {
        super(MESSAGE);
    }

    public InvalidEmailException(String message) {
        super(message);
    }

    public InvalidEmailException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidEmailException(Throwable cause) {
        super(cause);
    }

    protected InvalidEmailException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
