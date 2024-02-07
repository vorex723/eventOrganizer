package com.mazurek.eventOrganizer.user.dto;

import com.mazurek.eventOrganizer.event.dto.EventWithoutUsersDto;
import lombok.*;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWithEventsDto {

    private long id;
    private String firstName;
    private String lastName;
    private String homeCity;
    private List<EventWithoutUsersDto> userEvents;
    private List<EventWithoutUsersDto> attendingEvents;

}
