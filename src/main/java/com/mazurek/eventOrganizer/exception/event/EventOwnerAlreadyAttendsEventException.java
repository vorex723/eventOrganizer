package com.mazurek.eventOrganizer.exception.event;

public class EventOwnerAlreadyAttendsEventException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "As owner of the event, you are already attending this event";

    public EventOwnerAlreadyAttendsEventException() {
        super(DEFAULT_MESSAGE);
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
