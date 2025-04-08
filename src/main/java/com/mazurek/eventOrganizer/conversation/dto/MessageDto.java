package com.mazurek.eventOrganizer.conversation.dto;

import com.mazurek.eventOrganizer.conversation.Message;
import com.mazurek.eventOrganizer.user.dto.UserProfileDto;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDto {
    private UserProfileDto sender;
    private Date sentDate;
    private String message;

    public MessageDto(Message message) {
        this.sender = new UserProfileDto(message.getSender());
        this.sentDate = message.getSentDate();
        this.message = message.getMessage();
    }
}
