package com.mazurek.eventOrganizer.event.dto;

import com.mazurek.eventOrganizer.file.FileDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadShortDto;
import com.mazurek.eventOrganizer.user.dto.UserWithoutEventsDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventWithUsersDto {

    private UUID id;
    private String name;
    private String shortDescription;
    private String longDescription;
    private String city;
    private String exactAddress;
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    private UserWithoutEventsDto owner;
    @Builder.Default
    private List<UserWithoutEventsDto> attendingUsers = new ArrayList<>();
    private LocalDateTime eventStartDate;
    private Date createDate;
    private Date lastUpdate;
    @Builder.Default
    private List<ThreadShortDto> threads = new ArrayList<>();
    @Builder.Default
    private List<FileDto> files = new ArrayList<>();


}
