package com.mazurek.eventOrganizer.thread.dto;

import com.mazurek.eventOrganizer.thread.ThreadReply;
import com.mazurek.eventOrganizer.user.dto.UserProfileDto;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreadReplyDto {

    private UUID id;
    private UUID threadId;
    private UserProfileDto replier;
    private String content;
    private Instant replyDate;
    private Instant lastUpdate;
    private Integer editCounter;

    public ThreadReplyDto(ThreadReply threadReply) {
        this.id = threadReply.getId();
        this.threadId = threadReply.getThread().getId();
        this.replier = new UserProfileDto(threadReply.getReplier());
        this.content = threadReply.getContent();
        this.replyDate = threadReply.getReplyDate();
        this.lastUpdate = threadReply.getLastUpdate();
        this.editCounter = threadReply.getEditCounter();
    }
}
