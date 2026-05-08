package com.mazurek.eventOrganizer.conversation.dto;

import com.mazurek.eventOrganizer.conversation.message.Message;
import com.mazurek.eventOrganizer.user.dto.UserProfileDto;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDto {
    private UserProfileDto sender;
    private Instant sentDate;
    private String content;

    public MessageDto(Message message) {
        this.sender = new UserProfileDto(message.getSender());
        this.sentDate = message.getSentDate();
        this.content = message.getContent();
    }
}
