package com.mazurek.eventOrganizer.exception.user;

public class UserAlreadyExistException extends RuntimeException {

    private static final String MESSAGE = "User already exist.";

    public UserAlreadyExistException() {
        super(MESSAGE);
    }

    public UserAlreadyExistException(String message) {
        super(message);
    }

    public UserAlreadyExistException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserAlreadyExistException(Throwable cause) {
        super(cause);
    }

    protected UserAlreadyExistException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
