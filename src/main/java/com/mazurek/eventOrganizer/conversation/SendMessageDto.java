package com.mazurek.eventOrganizer.conversation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class SendMessageDto {
    private Long recipientId;
    private String message;
}
