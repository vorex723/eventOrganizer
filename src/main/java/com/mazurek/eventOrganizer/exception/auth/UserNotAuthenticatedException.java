package com.mazurek.eventOrganizer.exception.auth;

public class UserNotAuthenticatedException extends RuntimeException {
   public static final String DEFAULT_MESSAGE = "User not authenticated.";

    public UserNotAuthenticatedException() {
        super(DEFAULT_MESSAGE);
    }

    public UserNotAuthenticatedException(String message) {
        super(message);
    }

    public UserNotAuthenticatedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserNotAuthenticatedException(Throwable cause) {
        super(cause);
    }
}
