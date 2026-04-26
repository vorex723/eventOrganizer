package com.mazurek.eventOrganizer.exception.user;


public class UserRoleNotFoundException extends RuntimeException {
    public static final String DEFAULT_MESSAGE = "Role not found.";

    public UserRoleNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public UserRoleNotFoundException(String message) {
        super(message);
    }

    public UserRoleNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserRoleNotFoundException(Throwable cause) {
        super(cause);
    }
}
