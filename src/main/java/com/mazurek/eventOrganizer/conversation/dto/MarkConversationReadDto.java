package com.mazurek.eventOrganizer.conversation.dto;

import jakarta.validation.constraints.NotNull;

public record MarkConversationReadDto(
        @NotNull(message = "Last read message id must be provided.")
        Long lastReadMessageId
) {
}
