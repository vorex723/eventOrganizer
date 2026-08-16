package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.dto.EventDto;
import com.mazurek.eventOrganizer.event.dto.EventOverviewPageDto;

import java.util.UUID;

public interface EventService {
    EventOverviewPageDto getEvents(int pageNumber);
    EventDto getEventById(UUID id);
    EventOverviewPageDto getUserEventsByUserId(UUID userId, int pageNumber, boolean upcomingEventsOnly);
    EventOverviewPageDto getCurrentUserAttendingEvents(int pageNumber, boolean upcomingEventsOnly);

    EventDto createEvent(EventCreateDto eventCreateDto);
    void addAttenderToEvent(UUID eventId);
    void removeAttenderFromEvent(UUID eventId);

    EventDto updateEvent(EventCreateDto eventCreateDto, UUID eventId);

}
