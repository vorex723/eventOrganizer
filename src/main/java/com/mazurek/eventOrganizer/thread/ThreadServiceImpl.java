package com.mazurek.eventOrganizer.thread;

import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.common.PaginationUtils;
import com.mazurek.eventOrganizer.common.SortDirection;
import com.mazurek.eventOrganizer.config.properties.PaginationProperties;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.event.EventRepository;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotEventAttendeeException;
import com.mazurek.eventOrganizer.exception.thread.NotThreadOwnerException;
import com.mazurek.eventOrganizer.exception.thread.ThreadNotFoundInEventException;
import com.mazurek.eventOrganizer.notification.service.NotificationCommandService;
import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadOverviewPageDto;
import com.mazurek.eventOrganizer.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ThreadServiceImpl implements ThreadService{

    private final AuthenticationService authenticationService;
    private final NotificationCommandService notificationCommandService;
    private final ThreadRepository threadRepository;
    private final EventRepository eventRepository;
    private final Clock clock;
    private final PaginationProperties paginationProperties;

    @Transactional
    public ThreadDto createThreadInEvent(ThreadCreateDto threadCreateDto, UUID eventId){
        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);

        User threadOwner = authenticationService.getCurrentUser();

        if (!eventRepository.isUserAttendeeOrOwner(threadOwner.getId(), eventId))
            throw new NotEventAttendeeException();

        Instant createDateTime = clock.instant();

        Thread newThread = Thread.builder()
                .name(threadCreateDto.getName())
                .content(threadCreateDto.getContent())
                .owner(threadOwner)
                .event(event)
                .createDate(createDateTime)
                .lastUpdate(createDateTime)
                .lastActivity(createDateTime)
                .build();

        Thread savedThread = threadRepository.save(newThread);

        List<UUID> recipientIds = new ArrayList<>(eventRepository.findAttendeeIdsByEventId(eventId));
        if (!event.getOwner().equals(threadOwner))
            recipientIds.add(event.getOwner().getId());
        recipientIds.removeIf(id -> threadOwner.getId().equals(id));
        recipientIds = recipientIds.stream().distinct().toList();

        notificationCommandService.notifyNewEventThread(eventId, savedThread.getId(), recipientIds, threadOwner.getFullName());

        return new ThreadDto(savedThread);
    }

    @Transactional
    public ThreadDto updateThreadInEvent(ThreadCreateDto threadCreateDto, UUID eventId, UUID threadId){
        if (!eventRepository.existsById(eventId))
            throw new EventNotFoundException();

        User threadOwner = authenticationService.getCurrentUser();

        Thread threadToUpdate = threadRepository.findByIdAndEventId(threadId,eventId)
                .orElseThrow(ThreadNotFoundInEventException::new);

        if(!eventRepository.isUserAttendeeOrOwner(threadOwner.getId(), eventId))
            throw new NotEventAttendeeException();
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
        PaginationUtils.requireValidPageNumber(pageNumber);
        UUID userId = authenticationService.getCurrentUserId();

        if (!eventRepository.existsById(eventId))
            throw new EventNotFoundException();

        if (!eventRepository.isUserAttendeeOrOwner(userId, eventId))
            throw new NotEventAttendeeException();

        PageRequest pageRequest = PaginationUtils.pageRequest(
                pageNumber,
                paginationProperties.getDefaultPageSize(),
                direction.equals(SortDirection.ASC) ?
                        Sort.by(sortByField.getSortField()).ascending().and(Sort.by("id")).ascending() :
                        Sort.by(sortByField.getSortField()).descending().and(Sort.by("id")).descending()
        );

        Page<Thread> threadPage = threadRepository.findByEventId(eventId, pageRequest);
        return new ThreadOverviewPageDto(threadPage);
    }

    @Transactional(readOnly = true)
    public ThreadDto getThreadInEvent(UUID eventId, UUID threadId){
        if (!eventRepository.existsById(eventId))
            throw new EventNotFoundException();

        UUID userId = authenticationService.getCurrentUserId();

        if (!eventRepository.isUserAttendeeOrOwner(userId, eventId))
            throw new NotEventAttendeeException();

        Thread thread = threadRepository.findByIdAndEventId(threadId, eventId)
                .orElseThrow(ThreadNotFoundInEventException::new);

        return new ThreadDto(thread);
    }
}
