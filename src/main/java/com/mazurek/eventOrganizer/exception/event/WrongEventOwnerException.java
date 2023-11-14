package com.mazurek.eventOrganizer.exception.event;

public class WrongEventOwnerException extends RuntimeException{
    public WrongEventOwnerException() {
        super();
    }

    public WrongEventOwnerException(String message) {
        super(message);
    }

    public WrongEventOwnerException(String message, Throwable cause) {
        super(message, cause);
    }

    public WrongEventOwnerException(Throwable cause) {
        super(cause);
    }
}
