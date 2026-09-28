package com.mazurek.eventOrganizer.conversation.dto;

import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipant;

import java.time.Instant;
import java.util.UUID;

public record ConversationParticipantDto(
        String fullName,
        UUID userId,
        Instant joinedAt,
        Instant lastReadAt,
        Long lastReadMessageId
) {
    public ConversationParticipantDto(ConversationParticipant participant){
        this(
                participant.getUser() == null
                        ? participant.getUserNameAtJoin()
                        : participant.getUserNameAtJoin() == null
                                ? participant.getUser().getFullName()
                                : participant.getUserNameAtJoin(),
                participant.getUser() == null ? null : participant.getUser().getId(),
                participant.getJoinedAt(),
                participant.getLastReadAt(),
                participant.getLastReadMessageId()
        );
    }
}
