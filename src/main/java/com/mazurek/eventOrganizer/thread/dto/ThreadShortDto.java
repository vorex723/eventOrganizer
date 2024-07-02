package com.mazurek.eventOrganizer.thread.dto;

import com.mazurek.eventOrganizer.user.dto.UserWithoutEventsDto;
import lombok.*;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreadShortDto {
    private UUID id;
    private UserWithoutEventsDto owner;
    private String name;
    private Date createDate;
    private int replies;
}
