package com.mazurek.eventOrganizer.exception.jwt;

public class InvalidRefreshTokenException extends RuntimeException{

    public static final String DEFAULT_MESSAGE = "Invalid token";

    public InvalidRefreshTokenException() {
        super(DEFAULT_MESSAGE);
    }

    public InvalidRefreshTokenException(String message) {
        super(message);
    }

    public InvalidRefreshTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
