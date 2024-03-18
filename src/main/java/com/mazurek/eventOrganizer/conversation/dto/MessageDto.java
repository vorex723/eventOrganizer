package com.mazurek.eventOrganizer.conversation.dto;

import com.mazurek.eventOrganizer.user.dto.UserWithoutEventsDto;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDto {
    private UserWithoutEventsDto sender;
    private Date sentDate;
    private String message;
}
