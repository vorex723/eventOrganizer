package com.mazurek.eventOrganizer.conversation.dto;

import com.mazurek.eventOrganizer.conversation.Conversation;
import com.mazurek.eventOrganizer.conversation.ConversationType;

import java.time.Instant;
import java.util.UUID;

public record ConversationOverviewDto(
        UUID id,
        Instant lastActiveAt,
        String displayName
) {

    public ConversationOverviewDto(Conversation conversation, UUID userId) {
        this(
                conversation.getId(),
                conversation.getLastActiveAt(),
                conversation.getType().equals(ConversationType.DIRECT) ?
                        conversation.getParticipants().stream()
                                .filter(participant -> !participant.getUser().getId().equals(userId))
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException(
                                        "Direct conversation must contain another participant"
                                ))
                                .getUser().getFullName()
                        : conversation.getName()

        );
    }

}
