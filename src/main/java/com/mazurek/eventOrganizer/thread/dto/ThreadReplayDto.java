package com.mazurek.eventOrganizer.thread.dto;

import com.mazurek.eventOrganizer.thread.ThreadReply;
import com.mazurek.eventOrganizer.user.dto.UserProfileDto;
import lombok.*;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreadReplayDto {

    private UUID id;
    private UserProfileDto replier;
    private String content;
    private Date replayDate;
    private Date lastEditDate;
    private Integer editCounter;

    public ThreadReplayDto(ThreadReply threadReply) {
        this.id = threadReply.getId();
        this.replier = new UserProfileDto(threadReply.getReplier());
        this.content = threadReply.getContent();
        this.replayDate = threadReply.getReplayDate();
        this.lastEditDate = threadReply.getLastEditDate();
        this.editCounter = threadReply.getEditCounter();
    }
}
