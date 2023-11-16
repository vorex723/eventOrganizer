package com.mazurek.eventOrganizer.event;


import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.city.CityUtils;
import com.mazurek.eventOrganizer.event.dto.EventCreationDto;
import com.mazurek.eventOrganizer.event.dto.EventWithUsersDto;
import com.mazurek.eventOrganizer.event.mapper.EventMapper;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.WrongEventOwnerException;
import com.mazurek.eventOrganizer.jwt.JwtUtil;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.tag.TagRepository;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class EventServiceImpl implements EventService{

    private final EventRepository eventRepository;
    private final CityRepository cityRepository;
    private final CityUtils cityUtils;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final JwtUtil jwtUtil;
    @Override
    public EventWithUsersDto getEventById(Long id) {
        Optional<Event> eventOptional = eventRepository.findById(id);
        if (eventOptional.isEmpty())
            throw new EventNotFoundException("Event doesn't exist.");

        return eventMapper.mapEventToEventWithUsersDto(eventOptional.get());
    }

    @Override
    public EventWithUsersDto createEvent(EventCreationDto eventCreationDto, String jwtToken) throws RuntimeException {

        Event newEvent = Event.builder()
                .name(eventCreationDto.getName())
                .shortDescription(eventCreationDto.getShortDescription())
                .longDescription(eventCreationDto.getLongDescription())
                .exactAddress(eventCreationDto.getExactAddress())
                .eventStartDate(eventCreationDto.getEventStartDate())
                .tags(new HashSet<>())
                .attendingUsers(new HashSet<>())
                .build();
        newEvent.setOwner(userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).get());
        newEvent.setCity(cityUtils.resolveCity(eventCreationDto.getCity()));

        resolveTagsForNewEvent(newEvent, eventCreationDto);

        newEvent.setEventStartDate(eventCreationDto.getEventStartDate() != null ? eventCreationDto.getEventStartDate() : null);
        newEvent.setCreateDate(new Date(Calendar.getInstance().getTimeInMillis()));
        newEvent.setLastUpdate(newEvent.getCreateDate());

        return eventMapper.mapEventToEventWithUsersDto(eventRepository.save(newEvent));
    }

    /*
           Maybe should add reaction for null or empty fields to not change them at all instead of making them NotNullable.
    */
    @Override
    public EventWithUsersDto updateEvent(EventCreationDto updatedEventDto, Long id, String jwtToken) throws RuntimeException{

        Optional<Event> eventOptional = eventRepository.findById(id);

        if (eventOptional.isEmpty())
            throw new EventNotFoundException("There is no event with that id.");

        Event storedEvent = eventOptional.get();

        if (!storedEvent.getOwner().equals(userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).get()))
            throw new WrongEventOwnerException("It is not your event.");

        updateEventFields(storedEvent, updatedEventDto);

        return eventMapper.mapEventToEventWithUsersDto(eventRepository.save(storedEvent));

    }

    @Override
    public boolean addAttenderToEvent(Long id, String jwt) throws RuntimeException {
        Optional<Event> eventOptional = eventRepository.findById(id);

        if (eventOptional.isEmpty())
            throw new EventNotFoundException("There is no event with that id.");

        User attender = userRepository.findByEmail(jwtUtil.extractUsername(jwt)).get();
        Event storedEvent = eventOptional.get();
        storedEvent.addAttendingUser(attender);

        eventRepository.save(storedEvent);
        return true;
    }

    private void resolveTagsForNewEvent(Event event, EventCreationDto sourceDto) {
        if (!sourceDto.getTags().isEmpty()){
            Optional<Tag> tagOptional;
            for (String tagName : sourceDto.getTags())
            {
                tagOptional = tagRepository.findByName(tagName);
                if (tagOptional.isPresent())
                    event.addTag(tagOptional.get());
                else
                    event.addTag(tagRepository.save(new Tag(tagName)));
            }
        }
    }

    private void resolveTagsForUpdatingEvent(Event event,EventCreationDto sourceDto) {
        if (!sourceDto.getTags().isEmpty()) {
            Optional<Tag> tagOptional;

            for (Tag tagIterator : event.getTags())
                if (!sourceDto.getTags().contains(tagIterator.getName()))
                    tagIterator.removeEvent(event);
            event.getTags().removeIf(tag -> !sourceDto.getTags().contains(tag.getName()));

            for (String tagName : sourceDto.getTags()){
                if (event.containsTagByName(tagName))
                    continue;

                tagOptional = tagRepository.findByName(tagName);

                if (tagOptional.isPresent())
                    event.addTag(tagOptional.get());
                else
                    event.addTag(tagRepository.save(new Tag(tagName)));
            }
        }
        else
            event.clearTags();
    }

    private void updateEventFields(Event eventToUpdate, EventCreationDto source){
        eventToUpdate.setName(source.getName());
        eventToUpdate.setShortDescription(source.getShortDescription());
        eventToUpdate.setLongDescription(source.getLongDescription());
        eventToUpdate.setCity(cityUtils.resolveCity(source.getCity()));
        eventToUpdate.setExactAddress(source.getExactAddress());
        eventToUpdate.setEventStartDate(source.getEventStartDate());
        eventToUpdate.setLastUpdate(Calendar.getInstance().getTime());
        resolveTagsForUpdatingEvent(eventToUpdate,source);
    }


}
