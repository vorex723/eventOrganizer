package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.dto.EventWithUsersDto;
import com.mazurek.eventOrganizer.event.dto.EventWithoutUsersDto;
import com.mazurek.eventOrganizer.file.File;
import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadReplayCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadReplayDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface EventService {
    EventWithUsersDto getEventById(Long id);
    EventWithUsersDto createEvent(EventCreateDto eventCreateDto, String jwtToken);
    EventWithUsersDto updateEvent(EventCreateDto eventCreateDto, Long id, String jwtToken);
    boolean addAttenderToEvent(Long id, String jwt);
    ThreadDto createThreadInEvent(ThreadCreateDto threadCreateDto,Long eventId, String jwtToken);
    ThreadDto createReplyInThread(ThreadReplayCreateDto threadReplayCreateDto,Long eventId, Long threadId, String jwtToken);
    ThreadDto updateThreadInEvent(ThreadCreateDto threadCreateDto,Long eventId, Long threadId, String jwtToken);
    ThreadDto updateThreadReplyInEvent(ThreadReplayCreateDto threadReplayCreateDto,Long eventId, Long threadId, Long threadReplyId, String jwtToken);
    List<EventWithoutUsersDto> searchEvents(List<String> words, List<String> tags, String cityName);
    EventWithUsersDto uploadFileToEvent(MultipartFile uploadedFile,Long eventId, String jwtToken) throws RuntimeException,IOException;
    File getFile(UUID id, Long eventId, String jwtToken);

}
