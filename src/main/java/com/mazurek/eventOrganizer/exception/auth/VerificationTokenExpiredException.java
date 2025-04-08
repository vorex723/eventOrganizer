package com.mazurek.eventOrganizer.exception.auth;

public class VerificationTokenExpiredException extends RuntimeException {
    public VerificationTokenExpiredException() {
        super();
    }

    public VerificationTokenExpiredException(String message) {
        super(message);
    }

    public VerificationTokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public VerificationTokenExpiredException(Throwable cause) {
        super(cause);
    }

    protected VerificationTokenExpiredException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
