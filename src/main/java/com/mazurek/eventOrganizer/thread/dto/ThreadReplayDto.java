package com.mazurek.eventOrganizer.thread.dto;

import com.mazurek.eventOrganizer.thread.Thread;

import com.mazurek.eventOrganizer.user.dto.UserWithoutEventsDto;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreadReplayDto {

    private Long id;
    private UserWithoutEventsDto replier;
    private String content;
    private Date replayDate;
    private Date lastEditDate;
    private Integer editCounter;
}
