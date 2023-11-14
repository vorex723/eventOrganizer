package com.mazurek.eventOrganizer.event.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class EventCreationDto {
    private String name;
    private String shortDescription;
    private String longDescription;
    private String city;
    private String exactAddress;
    private List<String> tags = new ArrayList<>();
    private Date eventStartDate;
}

