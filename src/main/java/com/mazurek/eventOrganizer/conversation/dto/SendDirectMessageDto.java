package com.mazurek.eventOrganizer.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class SendDirectMessageDto {
    @NotNull(message = "Recipient id must be provided.")
    private UUID recipientId;

    @NotBlank(message = "Message content must be provided.")
    @Size(min = 1, max = 2500, message = "Message content must be between 1 and 2500 characters.")
    private String content;
}
