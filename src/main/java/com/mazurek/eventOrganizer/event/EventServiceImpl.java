package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityService;
import com.mazurek.eventOrganizer.config.properties.PaginationProperties;
import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.dto.EventDto;
import com.mazurek.eventOrganizer.event.dto.EventOverviewPageDto;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import com.mazurek.eventOrganizer.exception.event.*;
import com.mazurek.eventOrganizer.exception.thread.*;
import com.mazurek.eventOrganizer.file.*;
import com.mazurek.eventOrganizer.notification.NotificationService;
import com.mazurek.eventOrganizer.notification.NotificationType;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.tag.TagRepository;
import com.mazurek.eventOrganizer.tag.TagService;
import com.mazurek.eventOrganizer.thread.*;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;
    private final CityService cityService;
    private final TagService tagService;
    private final NotificationService notificationService;
    private final PaginationProperties paginationProperties;
    private final Clock clock;

    @Override
    @Transactional
    public EventOverviewPageDto getEvents(int pageNumber) {
        validatePageNumber(pageNumber);
        Page<Event> eventPage = eventRepository.findAll(
                PageRequest.of(pageNumber, paginationProperties.getDefaultPageSize(), Sort.by("eventStartDate").descending())
        );

        return new EventOverviewPageDto(eventPage);
    }

    @Override
    @Transactional
    public EventDto getEventById(UUID id) {
        Event event = eventRepository.findById(id).orElseThrow(EventNotFoundException::new);
        return new EventDto(event);
    }

    @Override
    @Transactional
    public EventOverviewPageDto getUserEventsByUserId(UUID id, int pageNumber, boolean upcomingEventsOnly) {
        validatePageNumber(pageNumber);
        userRepository.findById(id).orElseThrow(UserNotFoundException::new);
        Instant now = clock.instant();
        PageRequest pageRequest = PageRequest.of(pageNumber, paginationProperties.getDefaultPageSize(), Sort.by("eventStartDate").descending());
        if (upcomingEventsOnly)
            return new EventOverviewPageDto(eventRepository.findUpcomingEventsByOwnerId(id, now, pageRequest));

        return new EventOverviewPageDto(eventRepository.findByOwnerId(id, pageRequest));
    }

    @Override
    @Transactional
    public EventOverviewPageDto getCurrentUserAttendingEvents(int pageNumber, boolean upcomingEventsOnly) {
        validatePageNumber(pageNumber);
        UUID userId = authenticationService.getCurrentUserId();
        Instant now = clock.instant();
        PageRequest pageRequest = PageRequest.of(pageNumber, paginationProperties.getDefaultPageSize(), Sort.by("eventStartDate").descending());
        if (upcomingEventsOnly)
            return new EventOverviewPageDto(eventRepository.findUpcomingUserAttendingEventsByUserId(userId, now, pageRequest));

        return new EventOverviewPageDto(eventRepository.findUserAttendingEventsByUserId(userId, pageRequest));
    }


    @Override
    @Transactional
    public EventDto createEvent(EventCreateDto eventCreateDto) {

        User eventOwner = authenticationService.getCurrentUser();
        City city = cityService.getCityByNameOrCreate(eventCreateDto.getCity());
        Set<Tag> tags = tagService.getTagsByNames(eventCreateDto.getTags());

        Instant createDateTime = clock.instant();
        Event newEvent = Event.builder()
                .name(eventCreateDto.getName())
                .shortDescription(eventCreateDto.getShortDescription())
                .longDescription(eventCreateDto.getLongDescription())
                .createDate(createDateTime)
                .lastUpdate(createDateTime)
                .eventStartDate(eventCreateDto.getEventStartDate().truncatedTo(ChronoUnit.MINUTES))
                .timeZoneId(eventCreateDto.getTimeZone())
                .exactAddress(eventCreateDto.getExactAddress())
                .build();

        newEvent.setOwner(eventOwner);
        newEvent.setCity(city);
        newEvent.setTags(tags);

        return new EventDto(eventRepository.save(newEvent));
    }

    @Override
    @Transactional
    public EventDto updateEvent(EventCreateDto updatedEventDto, UUID id) throws RuntimeException {
        updatedEventDto.setTags(updatedEventDto.getTags().stream().map(String::toLowerCase).collect(Collectors.toSet()));

        Event storedEvent = eventRepository.findById(id).orElseThrow(EventNotFoundException::new);
        Instant now = clock.instant();

        if (storedEvent.hadPlace(now))
            throw new EventAlreadyHadPlaceException();
        if (!storedEvent.getOwner().equals(authenticationService.getCurrentUser()))
            throw new NotEventOwnerException();

        storedEvent.setName(updatedEventDto.getName());
        storedEvent.setShortDescription(updatedEventDto.getShortDescription());
        storedEvent.setLongDescription(updatedEventDto.getLongDescription());
        storedEvent.setExactAddress(updatedEventDto.getExactAddress());
        storedEvent.setEventStartDate(updatedEventDto.getEventStartDate().truncatedTo(ChronoUnit.MINUTES));
        storedEvent.setTimeZoneId(updatedEventDto.getTimeZone());
        storedEvent.setLastUpdate(now);
        storedEvent.setCity(cityService.getCityByNameOrCreate(updatedEventDto.getCity()));

        Set<Tag> tags = tagService.getTagsByNames(updatedEventDto.getTags());
        storedEvent.setTags(tags);

        notificationService.notifyEventAttenders(storedEvent, NotificationType.EVENT_UPDATE, storedEvent.getId(), storedEvent.getOwner().getFullName());

        return new EventDto(eventRepository.save(storedEvent));
    }


    @Override
    @Transactional
    public void addAttenderToEvent(UUID eventId) throws RuntimeException {

        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);

        if (event.hadPlace(clock.instant()))
            throw new EventAlreadyHadPlaceException();
        User attender = authenticationService.getCurrentUser();

        if (event.getOwner().equals(attender))
            throw new EventOwnerAlreadyAttendsEventException();

        if (event.isUserAttending(attender))
            throw new AlreadyAttendingEventException();

        event.addAttendingUser(attender);
        eventRepository.save(event);

    }

    @Override
    @Transactional
    public void removeAttenderFromEvent(UUID eventId) {

        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);

        if (event.hadPlace(clock.instant()))
            throw new EventAlreadyHadPlaceException();
        User attender = authenticationService.getCurrentUser();

        if (event.getOwner().equals(attender))
            throw new EventOwnerMustAttendEventException();

        if (!event.isUserAttending(attender))
            throw new NotEventAttenderException();

        event.removeAttendingUser(attender);

        eventRepository.save(event);
        userRepository.save(attender);
    }

    private void validatePageNumber(int pageNumber) {
        if (pageNumber < 0) {
            throw new InvalidPageNumberException();
        }
    }

}
