package com.mazurek.eventOrganizer.exception.event;

public class EventCapacityTooSmallException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "Attendee capacity cannot be lower than the current attendee count.";

    public EventCapacityTooSmallException() {
        super(DEFAULT_MESSAGE);
    }
}
