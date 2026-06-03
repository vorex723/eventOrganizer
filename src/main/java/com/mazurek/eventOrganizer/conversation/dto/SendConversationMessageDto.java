package com.mazurek.eventOrganizer.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendConversationMessageDto(
        @NotBlank(message = "Message content must be provided.")
        @Size(min = 1, max = 2500, message = "Message content must be between 1 and 2500 characters.")
        String content
) {
}
