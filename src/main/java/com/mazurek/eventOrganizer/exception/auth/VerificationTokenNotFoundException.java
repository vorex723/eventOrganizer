package com.mazurek.eventOrganizer.exception.auth;

public class VerificationTokenNotFoundException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "Verification token does not exist!";

    public VerificationTokenNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public VerificationTokenNotFoundException(String message) {
        super(message);
    }

    public VerificationTokenNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public VerificationTokenNotFoundException(Throwable cause) {
        super(cause);
    }

    protected VerificationTokenNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
