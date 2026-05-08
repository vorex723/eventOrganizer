package com.mazurek.eventOrganizer.exception.conversation;

public class ConversationParticipantNotFound extends RuntimeException {

    public static final String DEFAULT_MESSAGE = "Conversation participant not found;";

    public ConversationParticipantNotFound() {
        super(DEFAULT_MESSAGE);
    }

    public ConversationParticipantNotFound(String message) {
        super(message);
    }

    public ConversationParticipantNotFound(String message, Throwable cause) {
        super(message, cause);
    }

    public ConversationParticipantNotFound(Throwable cause) {
        super(cause);
    }
}
