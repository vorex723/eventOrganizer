package com.mazurek.eventOrganizer.conversation.dto;

import com.mazurek.eventOrganizer.conversation.Conversation;

import java.time.Instant;
import java.util.UUID;

public record ConversationDetailsDto(
        UUID id,
        Instant lastActiveAt,
        Instant createdAt,
        String name
) {

    public ConversationDetailsDto(Conversation conversation) {
        this(
                conversation.getId(),
                conversation.getLastActiveAt(),
                conversation.getCreatedAt(),
                conversation.getName()
        );
    }
    public ConversationDetailsDto(Conversation conversation, String userFullName) {
        this(
                conversation.getId(),
                conversation.getLastActiveAt(),
                conversation.getCreatedAt(),
                userFullName
        );
    }
}
