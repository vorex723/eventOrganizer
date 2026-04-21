package com.mazurek.eventOrganizer.thread;

import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.common.SortDirection;
import com.mazurek.eventOrganizer.config.properties.PaginationProperties;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.event.EventRepository;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotEventAttenderException;
import com.mazurek.eventOrganizer.exception.thread.NotThreadOwnerException;
import com.mazurek.eventOrganizer.exception.thread.ThreadNotFoundInEventException;
import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadOverviewPageDto;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ThreadServiceImpl implements ThreadService{

    private final AuthenticationService authenticationService;
    private final ThreadRepository threadRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final PaginationProperties paginationProperties;

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
        newThread.setOwnerNameAtCreation(threadOwner.getFullName());
        newThread.setEvent(event);
        newThread.setCreateDate(createDateTime);
        newThread.setLastUpdate(createDateTime);
        newThread.setLastActivity(createDateTime);
        newThread.setEditCount(0);
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

    @Transactional(readOnly = true)
    public ThreadOverviewPageDto getThreadsByEventId(UUID eventId, int pageNumber, ThreadSortField sortByField, SortDirection direction) {
        if (pageNumber < 0)
            throw new InvalidPageNumberException();

        User user = authenticationService.getCurrentUser();

        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);

        if (!event.isUserAttending(user))
            throw new NotEventAttenderException();

        PageRequest pageRequest = PageRequest.of(
                pageNumber,
                paginationProperties.getDefaultPageSize(),
                direction.equals(SortDirection.ASC) ?
                        Sort.by(sortByField.getSortField()).ascending().and(Sort.by("id")).ascending() :
                        Sort.by(sortByField.getSortField()).descending().and(Sort.by("id")).descending()
        );

        Page<Thread> threadPage = threadRepository.findByEventId(eventId, pageRequest);
        return new ThreadOverviewPageDto(threadPage);
    }
}
