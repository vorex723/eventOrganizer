package com.mazurek.eventOrganizer.user.dto;

import com.mazurek.eventOrganizer.event.dto.EventOverviewDto;
import com.mazurek.eventOrganizer.user.User;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWithEventsDto {

    private UUID id;
    private String firstName;
    private String lastName;
    private String homeCity;
    private List<EventOverviewDto> userEvents;
    private List<EventOverviewDto> attendingEvents;

    public UserWithEventsDto(User user) {
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.homeCity = user.getHomeCity().getName();
        this.userEvents = user.getUserEvents().stream().map(EventOverviewDto::new).toList();
        this.attendingEvents = user.getAttendingEvents().stream().map(EventOverviewDto::new).toList();
    }
}
