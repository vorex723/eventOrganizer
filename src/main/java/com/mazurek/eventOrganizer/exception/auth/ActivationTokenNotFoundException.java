package com.mazurek.eventOrganizer.exception.auth;

public class ActivationTokenNotFoundException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "Verification token does not exist!";

    public ActivationTokenNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public ActivationTokenNotFoundException(String message) {
        super(message);
    }

    public ActivationTokenNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ActivationTokenNotFoundException(Throwable cause) {
        super(cause);
    }

    protected ActivationTokenNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
