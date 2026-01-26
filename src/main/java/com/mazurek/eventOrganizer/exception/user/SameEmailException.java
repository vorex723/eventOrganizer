package com.mazurek.eventOrganizer.exception.user;

public class SameEmailException extends RuntimeException {

        public static final String DEFAULT_MESSAGE = "New email have to be different from current one.";

    public SameEmailException() {
        super(DEFAULT_MESSAGE);
    }

    public SameEmailException(String message) {
        super(message);
    }

    public SameEmailException(String message, Throwable cause) {
        super(message, cause);
    }

    public SameEmailException(Throwable cause) {
        super(cause);
    }
}
