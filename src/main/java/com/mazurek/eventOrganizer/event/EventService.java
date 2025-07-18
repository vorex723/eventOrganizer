package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.dto.EventDto;
import com.mazurek.eventOrganizer.event.dto.EventOverviewDto;
import com.mazurek.eventOrganizer.event.dto.EventOverviewPageDto;
import com.mazurek.eventOrganizer.file.File;
import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadReplyCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadReplyDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface EventService {
    List<EventOverviewDto> getEvents(int pageNumber);
    EventDto getEventById(UUID id);
    EventOverviewPageDto getUserEventsByUserId(UUID id, int pageNumber, boolean upcomingEventsOnly);
    EventOverviewPageDto getUserAttendingEventsByUserId(UUID id, int pageNumber, boolean upcomingEventsOnly, String jwt);
    File getFile(UUID id, UUID eventId, String jwtToken);
    List<EventOverviewDto> searchEvents(List<String> words, List<String> tags, String cityName);


    EventDto createEvent(EventCreateDto eventCreateDto, String jwtToken);
    ThreadDto createThreadInEvent(ThreadCreateDto threadCreateDto,UUID eventId, String jwtToken);
    ThreadReplyDto createReplyInThread(ThreadReplyCreateDto threadReplyCreateDto, UUID eventId, UUID threadId, String jwtToken);
    EventDto uploadFileToEvent(MultipartFile uploadedFile, UUID eventId, String jwtToken) throws RuntimeException,IOException;
    boolean addAttenderToEvent(UUID eventId, String jwt);
    boolean removeAttenderFromEvent(UUID eventId, String jwt);

    ThreadDto updateThreadInEvent(ThreadCreateDto threadCreateDto,UUID eventId, UUID threadId, String jwtToken);
    ThreadReplyDto updateThreadReplyInEventThread(ThreadReplyCreateDto threadReplyCreateDto, UUID eventId, UUID threadId, UUID threadReplyId, String jwtToken);
    EventDto updateEvent(EventCreateDto eventCreateDto, UUID id, String jwtToken);




}
