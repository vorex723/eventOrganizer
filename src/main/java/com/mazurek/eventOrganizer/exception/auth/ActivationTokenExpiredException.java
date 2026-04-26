package com.mazurek.eventOrganizer.exception.auth;

public class ActivationTokenExpiredException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "Your verification token has expired!";

    public ActivationTokenExpiredException() {
        super(DEFAULT_MESSAGE);
    }

    public ActivationTokenExpiredException(String message) {
        super(message);
    }

    public ActivationTokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public ActivationTokenExpiredException(Throwable cause) {
        super(cause);
    }

    protected ActivationTokenExpiredException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
