package com.mazurek.eventOrganizer.conversation.dto;

import com.mazurek.eventOrganizer.conversation.Conversation;
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

    public ConversationDto(Conversation conversation) {
        this.id = conversation.getId();
        this.messages = conversation.getMessages().stream().map(MessageDto::new).toList();
    }
}
