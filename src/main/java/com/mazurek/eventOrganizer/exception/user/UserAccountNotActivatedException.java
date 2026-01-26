package com.mazurek.eventOrganizer.exception.user;

public class UserAccountNotActivatedException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "User account was not activated.";

    public UserAccountNotActivatedException() {
        super(DEFAULT_MESSAGE);
    }

    public UserAccountNotActivatedException(String message) {
        super(message);
    }

    public UserAccountNotActivatedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserAccountNotActivatedException(Throwable cause) {
        super(cause);
    }
}
