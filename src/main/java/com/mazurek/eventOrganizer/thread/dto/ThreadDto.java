package com.mazurek.eventOrganizer.thread.dto;

import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.user.dto.UserProfileDto;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    private LocalDateTime createDate;
    private Integer editCounter;
    private LocalDateTime lastUpdate;

    public ThreadDto(Thread thread) {
        this.id = thread.getId();
        this.eventId = thread.getEvent().getId();
        this.owner = new UserProfileDto(thread.getOwner());
        this.name = thread.getName();
        this.content = thread.getContent();
        this.createDate = thread.getCreateDate();
        this.editCounter = thread.getEditCounter();
        this.lastUpdate = thread.getLastUpdate();
    }
}
