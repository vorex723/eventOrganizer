package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.dto.EventDto;
import com.mazurek.eventOrganizer.event.dto.EventOverviewDto;
import com.mazurek.eventOrganizer.event.dto.EventOverviewPageDto;
import com.mazurek.eventOrganizer.file.File;
import com.mazurek.eventOrganizer.file.FileOverviewDto;
import com.mazurek.eventOrganizer.file.FileOverviewPageDto;
import com.mazurek.eventOrganizer.file.FileUploadDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadReplyCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadReplyDto;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface EventService {
    List<EventOverviewDto> getEvents(int pageNumber);
    EventDto getEventById(UUID id);
    EventOverviewPageDto getUserEventsByUserId(UUID userId, int pageNumber, boolean upcomingEventsOnly);
    EventOverviewPageDto getCurrentUserAttendingEvents(int pageNumber, boolean upcomingEventsOnly);

    EventDto createEvent(EventCreateDto eventCreateDto);
    void addAttenderToEvent(UUID eventId);
    void removeAttenderFromEvent(UUID eventId);

    EventDto updateEvent(EventCreateDto eventCreateDto, UUID id);

}
