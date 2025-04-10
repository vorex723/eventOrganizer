package com.mazurek.eventOrganizer.thread.dto;

import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.user.dto.UserProfileDto;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreadDto {
    private UUID id;
    private UserProfileDto owner;
    private String name;
    private String content;
    private LocalDateTime createDate;
    private Integer editCounter;
    private LocalDateTime lastTimeEdited;
    @Builder.Default
    private List<ThreadReplayDto> replies= new ArrayList<>();

    public ThreadDto(Thread thread) {
        this.id = thread.getId();
        this.owner = new UserProfileDto(thread.getOwner());
        this.name = thread.getName();
        this.content = thread.getContent();
        this.createDate = thread.getCreateDate();
        this.editCounter = thread.getEditCounter();
        this.lastTimeEdited = thread.getLastTimeEdited();
        this.replies = thread.getReplies().stream().map(ThreadReplayDto::new).toList();
    }
}
