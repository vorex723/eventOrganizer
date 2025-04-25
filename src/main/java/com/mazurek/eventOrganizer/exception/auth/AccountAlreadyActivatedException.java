package com.mazurek.eventOrganizer.exception.auth;

public class AccountAlreadyActivatedException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "Account is already activated.";

    public AccountAlreadyActivatedException() {
        super(DEFAULT_MESSAGE);
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
