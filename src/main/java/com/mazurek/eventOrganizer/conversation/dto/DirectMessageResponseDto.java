package com.mazurek.eventOrganizer.conversation.dto;

import java.util.UUID;

public record DirectMessageResponseDto(
        UUID conversationId,
        boolean conversationCreated,
        MessageDto message
) {

}
