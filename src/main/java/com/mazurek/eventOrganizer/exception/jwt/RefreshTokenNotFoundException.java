package com.mazurek.eventOrganizer.exception.jwt;

public class RefreshTokenNotFoundException extends RuntimeException{

    public static final String DEFAULT_MESSAGE = "RefreshToken not found.";

    public RefreshTokenNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public RefreshTokenNotFoundException(String message) {
        super(message);
    }

    public RefreshTokenNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public RefreshTokenNotFoundException(Throwable cause) {
        super(cause);
    }
}
