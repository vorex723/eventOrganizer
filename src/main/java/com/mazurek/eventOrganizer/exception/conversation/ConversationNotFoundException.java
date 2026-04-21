package com.mazurek.eventOrganizer.exception.conversation;

public class ConversationNotFoundException extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "There is no conversation with this id.";

    public ConversationNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public ConversationNotFoundException(String message) {
        super(message);
    }

    public ConversationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConversationNotFoundException(Throwable cause) {
        super(cause);
    }

    protected ConversationNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
