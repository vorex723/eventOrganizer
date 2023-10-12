package com.mazurek.eventOrganizer.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventWithoutUsersDto {
    private long id;
    private String name;
    private String shortDescription;
    private LocalDateTime eventStartDate;
    private int amountOfAttenders;
    private List<String> tags;

}
