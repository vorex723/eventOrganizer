package com.mazurek.eventOrganizer.thread;

import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.event.EventRepository;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotEventAttenderException;
import com.mazurek.eventOrganizer.exception.thread.NotThreadOwnerException;
import com.mazurek.eventOrganizer.exception.thread.ThreadNotFoundInEventException;
import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadDto;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ThreadService {

    private final AuthenticationService authenticationService;
    private final ThreadRepository threadRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional
    public ThreadDto createThreadInEvent(ThreadCreateDto threadCreateDto, UUID eventId){
        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);

        User threadOwner = authenticationService.getCurrentUser();

        if (!event.isUserAttending(threadOwner))
            throw new NotEventAttenderException();

        Instant createDateTime = clock.instant();

        Thread newThread = new Thread();
        newThread.setName(threadCreateDto.getName());
        newThread.setContent(threadCreateDto.getContent());
        newThread.setOwner(threadOwner);
        newThread.setEvent(event);
        newThread.setCreateDate(createDateTime);
        newThread.setLastUpdate(createDateTime);
        newThread.setEditCounter(0);
        newThread.setReplies(new HashSet<>());

        Thread savedThread = threadRepository.save(newThread);

        event.addThread(savedThread);
        threadOwner.addThread(savedThread);

        eventRepository.save(event);
        userRepository.save(threadOwner);
        return new ThreadDto(savedThread);
    }

    @Transactional
    public ThreadDto updateThreadInEvent(ThreadCreateDto threadCreateDto, UUID eventId, UUID threadId){
        Event event = eventRepository.findById(eventId)
                .orElseThrow(EventNotFoundException::new);

        User threadOwner = authenticationService.getCurrentUser();

        Thread threadToUpdate = threadRepository.findByIdAndEventId(threadId,eventId)
                .orElseThrow(ThreadNotFoundInEventException::new);

        if(!event.isUserAttending(threadOwner))
            throw new NotEventAttenderException();
        if(!threadToUpdate.isUserOwner(threadOwner))
            throw new NotThreadOwnerException();


        threadToUpdate.setName(threadCreateDto.getName());
        threadToUpdate.setContent(threadCreateDto.getContent());
        threadToUpdate.setLastUpdate(clock.instant());
        threadToUpdate.incrementEditCounter();

        Thread updatedThread = threadRepository.save(threadToUpdate);

        return new ThreadDto(updatedThread);
    }
}
