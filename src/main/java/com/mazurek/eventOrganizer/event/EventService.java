package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.dto.EventAttendeePageDto;
import com.mazurek.eventOrganizer.event.dto.EventDto;
import com.mazurek.eventOrganizer.event.dto.EventOverviewPageDto;

import java.util.UUID;

public interface EventService {
    EventOverviewPageDto getEvents(int pageNumber);
    EventDto getEventById(UUID id);
    EventAttendeePageDto getEventAttendees(UUID eventId, int pageNumber);
    EventOverviewPageDto getUserEventsByUserId(UUID userId, int pageNumber, boolean upcomingEventsOnly);
    EventOverviewPageDto getCurrentUserAttendingEvents(int pageNumber, boolean upcomingEventsOnly);
    EventOverviewPageDto getCityEventsByCityId(UUID cityId, int pageNumber);
    EventOverviewPageDto getTagEventsByTagName(String tagName, int pageNumber);

    EventDto createEvent(EventCreateDto eventCreateDto);
    void addAttendeeToEvent(UUID eventId);
    void removeAttendeeFromEvent(UUID eventId);

    EventDto updateEvent(EventCreateDto eventCreateDto, UUID eventId);

}
