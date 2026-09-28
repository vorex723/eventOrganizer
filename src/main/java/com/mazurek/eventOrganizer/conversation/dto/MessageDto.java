package com.mazurek.eventOrganizer.conversation.dto;

import com.mazurek.eventOrganizer.conversation.message.Message;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDto {
    private Long id;
    private UUID senderId;
    private String senderName;
    private Instant sentDate;
    private String content;
    private boolean contentUnavailable;

    public MessageDto(Long id, UUID senderId, Instant sentDate, String content) {
        this(id, senderId, null, sentDate, content, false);
    }

    public MessageDto(Message message) {
        this.id = message.getId();
        this.senderId = message.getSender() == null ? null : message.getSender().getId();
        this.senderName = message.getSenderNameAtCreation() != null
                ? message.getSenderNameAtCreation()
                : message.getSender() == null ? "Deleted user" : message.getSender().getFullName();
        this.sentDate = message.getSentDate();
        this.content = message.getContent();
    }
}
