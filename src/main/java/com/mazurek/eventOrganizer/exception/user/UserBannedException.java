package com.mazurek.eventOrganizer.exception.user;

public class UserBannedException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "Your account is banned";

    public UserBannedException() {
        super(DEFAULT_MESSAGE);
    }

    public UserBannedException(String message) {
        super(message);
    }

    public UserBannedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserBannedException(Throwable cause) {
        super(cause);
    }
}
