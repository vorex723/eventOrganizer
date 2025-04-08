package com.mazurek.eventOrganizer.conversation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class SendMessageDto {
    private UUID recipientId;
    private String message;
}
