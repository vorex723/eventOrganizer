package com.mazurek.eventOrganizer.conversation.dto;

import com.mazurek.eventOrganizer.conversation.Conversation;
import com.mazurek.eventOrganizer.conversation.ConversationType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversationDetailsDto(
        UUID id,
        ConversationType type,
        Instant lastActiveAt,
        Instant createdAt,
        String name,
        List<ConversationParticipantDto> participants
) {

    public ConversationDetailsDto(Conversation conversation) {
        this(
                conversation.getId(),
                conversation.getType(),
                conversation.getLastActiveAt(),
                conversation.getCreatedAt(),
                conversation.getName(),
                conversation.getParticipants().stream().filter(participant -> participant.getLeftAt() == null).map(ConversationParticipantDto::new).toList()
        );
    }
    public ConversationDetailsDto(Conversation conversation, String userFullName) {
        this(
                conversation.getId(),
                conversation.getType(),
                conversation.getLastActiveAt(),
                conversation.getCreatedAt(),
                userFullName,
                conversation.getParticipants().stream().filter(participant -> participant.getLeftAt() == null).map(ConversationParticipantDto::new).toList()
        );
    }
}
