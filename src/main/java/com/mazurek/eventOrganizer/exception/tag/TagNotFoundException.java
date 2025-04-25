package com.mazurek.eventOrganizer.exception.tag;

public class TagNotFoundException extends RuntimeException{

    public static final String DEFAULT_MESSAGE = "There is no tag with this name.";

    public TagNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public TagNotFoundException(String message) {
        super(message);
    }

    public TagNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public TagNotFoundException(Throwable cause) {
        super(cause);
    }

    protected TagNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
