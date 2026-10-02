package com.mazurek.eventOrganizer.city.cityLookupClient;

public class CityLookupException extends RuntimeException {

    public CityLookupException(String message) {
        super(message);
    }

    public CityLookupException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }

}
