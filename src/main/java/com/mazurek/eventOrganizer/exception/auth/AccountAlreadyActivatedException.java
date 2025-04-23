package com.mazurek.eventOrganizer.exception.auth;

public class AccountAlreadyActivatedException extends RuntimeException {

    private static final String MESSAGE = "Account is already activated.";

    public AccountAlreadyActivatedException() {
        super(MESSAGE);
    }

    public AccountAlreadyActivatedException(String message) {
        super(message);
    }

    public AccountAlreadyActivatedException(String message, Throwable cause) {
        super(message, cause);
    }

    public AccountAlreadyActivatedException(Throwable cause) {
        super(cause);
    }

    protected AccountAlreadyActivatedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
