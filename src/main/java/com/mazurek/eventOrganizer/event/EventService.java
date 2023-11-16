package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.event.dto.EventCreationDto;
import com.mazurek.eventOrganizer.event.dto.EventWithUsersDto;

public interface EventService {
    EventWithUsersDto getEventById(Long id);
    EventWithUsersDto createEvent(EventCreationDto eventCreationDto, String jwtToken);
    EventWithUsersDto updateEvent(EventCreationDto eventCreationDto, Long id, String jwtToken);
    boolean addAttenderToEvent(Long id, String jwt);

}
