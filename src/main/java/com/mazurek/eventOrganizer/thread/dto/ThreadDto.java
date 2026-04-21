package com.mazurek.eventOrganizer.thread.dto;

import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.user.dto.UserProfileDto;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreadDto {
    private UUID id;
    private UUID eventId;
    private UserProfileDto owner;
    private String name;
    private String content;
    private int repliesCount;
    private Instant createDate;
    private Instant lastUpdate;
    private Integer editCounter;

    public ThreadDto(Thread thread) {
        this.id = thread.getId();
        this.eventId = thread.getEvent().getId();
        this.owner = new UserProfileDto(thread.getOwner());
        this.name = thread.getName();
        this.content = thread.getContent();
        this.createDate = thread.getCreateDate();
        this.lastUpdate = thread.getLastUpdate();
        this.editCounter = thread.getEditCount();
    }
}
