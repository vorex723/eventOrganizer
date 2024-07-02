package com.mazurek.eventOrganizer.exception.event;

public class EventOwnerAlreadyAttendsEventException extends RuntimeException {
    public EventOwnerAlreadyAttendsEventException() {
        super();
    }

    public EventOwnerAlreadyAttendsEventException(String message) {
        super(message);
    }

    public EventOwnerAlreadyAttendsEventException(String message, Throwable cause) {
        super(message, cause);
    }

    public EventOwnerAlreadyAttendsEventException(Throwable cause) {
        super(cause);
    }
}
