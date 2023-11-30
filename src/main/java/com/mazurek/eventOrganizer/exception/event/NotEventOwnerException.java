package com.mazurek.eventOrganizer.exception.event;

public class NotEventOwnerException extends RuntimeException{
    public NotEventOwnerException() {
        super();
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
