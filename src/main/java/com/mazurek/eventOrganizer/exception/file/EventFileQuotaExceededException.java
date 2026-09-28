package com.mazurek.eventOrganizer.exception.file;

public class EventFileQuotaExceededException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "This event has reached its file storage limit.";

    public EventFileQuotaExceededException() {
        super(DEFAULT_MESSAGE);
    }
}
