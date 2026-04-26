package com.mazurek.eventOrganizer.exception.city;

public class CityNotFoundException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "There is no city with this name.";

    public CityNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public CityNotFoundException(String message) {
        super(message);
    }

    public CityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public CityNotFoundException(Throwable cause) {
        super(cause);
    }

    protected CityNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
