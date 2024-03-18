package com.mazurek.eventOrganizer.conversation.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationDto {

    private UUID id;
    private List<MessageDto> messages;

}
