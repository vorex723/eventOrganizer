package com.mazurek.eventOrganizer.exception.event;

public class NotEventOwnerException extends RuntimeException{

    public static final String DEFAULT_MESSAGE = "You are not owner of this event!";

    public NotEventOwnerException() {
        super(DEFAULT_MESSAGE);
    }

    public NotEventOwnerException(String message) {
        super(message);
    }

    public NotEventOwnerException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotEventOwnerException(Throwable cause) {
        super(cause);
    }
}
