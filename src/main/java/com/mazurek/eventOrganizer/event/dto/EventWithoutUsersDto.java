package com.mazurek.eventOrganizer.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventWithoutUsersDto {
    private UUID id;
    private String name;
    private String city;
    private String shortDescription;
    private Date eventStartDate;
    private int amountOfAttenders;
    private List<String> tags;

}
