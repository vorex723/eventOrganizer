package com.mazurek.eventOrganizer.thread.dto;

import com.mazurek.eventOrganizer.thread.ThreadReply;
import com.mazurek.eventOrganizer.user.dto.UserWithoutEventsDto;
import lombok.*;

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
    private UserWithoutEventsDto owner;
    private String name;
    private String content;
    private Date createDate;
    private Integer editCounter;
    private Date lastTimeEdited;
    @Builder.Default
    private List<ThreadReplayDto> replies= new ArrayList<>();

}
