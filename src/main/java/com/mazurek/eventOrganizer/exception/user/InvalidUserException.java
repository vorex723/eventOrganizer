package com.mazurek.eventOrganizer.exception.user;

public class InvalidUserException extends RuntimeException {

    //to-do: add message

    public static final String DEFAULT_MESSAGE = "Invalid user.";

    public InvalidUserException() {
        super(DEFAULT_MESSAGE);
    }

    public InvalidUserException(String message) {
        super(message);
    }

    public InvalidUserException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidUserException(Throwable cause) {
        super(cause);
    }

    protected InvalidUserException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
