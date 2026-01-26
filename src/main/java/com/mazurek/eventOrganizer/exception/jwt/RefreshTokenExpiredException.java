package com.mazurek.eventOrganizer.exception.jwt;

public class RefreshTokenExpiredException extends RuntimeException {
    public static final String DEFAULT_MESSAGE = "Refresh token have expired.";

    public RefreshTokenExpiredException() {
        super(DEFAULT_MESSAGE);
    }

    public RefreshTokenExpiredException(String message) {
        super(message);
    }

    public RefreshTokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public RefreshTokenExpiredException(Throwable cause) {
        super(cause);
    }
}
