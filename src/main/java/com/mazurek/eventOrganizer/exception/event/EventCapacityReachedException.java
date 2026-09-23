package com.mazurek.eventOrganizer.exception.event;

public class EventCapacityReachedException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "This event has reached its attendee capacity.";

    public EventCapacityReachedException() {
        super(DEFAULT_MESSAGE);
    }
}
