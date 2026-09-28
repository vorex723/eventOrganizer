package com.mazurek.eventOrganizer.conversation.dto;

import com.mazurek.eventOrganizer.conversation.Conversation;
import com.mazurek.eventOrganizer.conversation.ConversationType;

import java.time.Instant;
import java.util.UUID;

public record ConversationOverviewDto(
        UUID id,
        ConversationType type,
        Instant lastActiveAt,
        String displayName
) {

    public ConversationOverviewDto(Conversation conversation, UUID userId) {
        this(
                conversation.getId(),
                conversation.getType(),
                conversation.getLastActiveAt(),
                        conversation.getType().equals(ConversationType.DIRECT) ?
                        directConversationDisplayName(conversation, userId)
                        : conversation.getName()

        );
    }

    private static String directConversationDisplayName(Conversation conversation, UUID userId) {
        var participant = conversation.getParticipants().stream()
                .filter(candidate -> candidate.getUser() == null || !candidate.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Direct conversation must contain another participant"));
        return participant.getUserNameAtJoin() != null
                ? participant.getUserNameAtJoin()
                : participant.getUser() == null ? "Deleted user" : participant.getUser().getFullName();
    }

}
