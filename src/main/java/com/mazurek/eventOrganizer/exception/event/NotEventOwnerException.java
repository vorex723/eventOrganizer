package com.mazurek.eventOrganizer.exception.event;

public class NotEventOwnerException extends RuntimeException{

    private static final String MESSAGE = "You are not owner of this event!";

    public NotEventOwnerException() {
        super(MESSAGE);
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
