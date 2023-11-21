package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.dto.EventWithUsersDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadReplayCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadReplayDto;

public interface EventService {
    EventWithUsersDto getEventById(Long id);
    EventWithUsersDto createEvent(EventCreateDto eventCreateDto, String jwtToken);
    EventWithUsersDto updateEvent(EventCreateDto eventCreateDto, Long id, String jwtToken);
    boolean addAttenderToEvent(Long id, String jwt);

    ThreadDto createThreadInEvent(ThreadCreateDto threadCreateDto,Long eventId, String jwtToken);
    ThreadDto createReplyInThread(ThreadReplayCreateDto threadReplayCreateDto,Long eventId, Long threadId, String jwtToken);
    ThreadDto updateThreadInEvent(ThreadCreateDto threadCreateDto,Long eventId, Long threadId, String jwtToken);

}
