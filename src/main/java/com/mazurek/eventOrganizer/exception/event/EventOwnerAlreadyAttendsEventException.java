package com.mazurek.eventOrganizer.exception.event;

public class EventOwnerAlreadyAttendsEventException extends RuntimeException {

    private static final String MESSAGE = "As owner of the event, you are already attending this event";

    public EventOwnerAlreadyAttendsEventException() {
        super(MESSAGE);
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
