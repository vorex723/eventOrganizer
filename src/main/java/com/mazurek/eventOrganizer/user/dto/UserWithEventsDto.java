package com.mazurek.eventOrganizer.user.dto;

import com.mazurek.eventOrganizer.event.dto.EventSmallDto;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWithEventsDto {

    private long id;
    private String firstName;
    private String lastName;
    private String email;
    private String homeCity;
    private List<EventSmallDto> userEvents;
    private List<EventSmallDto> attendingEvents;

}
