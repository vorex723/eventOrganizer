package com.mazurek.eventOrganizer.exception.jwt;

public class RefreshTokenRevokedException extends RuntimeException {
    public static final String DEFAULT_MESSAGE = "Refresh token have been revoked.";

    public RefreshTokenRevokedException() {
        super(DEFAULT_MESSAGE );
    }

    public RefreshTokenRevokedException(String message) {
        super(message);
    }

    public RefreshTokenRevokedException(String message, Throwable cause) {
        super(message, cause);
    }

    public RefreshTokenRevokedException(Throwable cause) {
        super(cause);
    }
}
