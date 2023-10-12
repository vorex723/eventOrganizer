package com.mazurek.eventOrganizer.event;


import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.city.CityUtils;
import com.mazurek.eventOrganizer.event.dto.EventCreationDto;
import com.mazurek.eventOrganizer.event.dto.EventWithUsersDto;
import com.mazurek.eventOrganizer.event.mapper.EventMapper;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.jwt.JwtUtil;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.tag.TagRepository;
import com.mazurek.eventOrganizer.user.UserRepository;
import com.mazurek.eventOrganizer.user.dto.UserWithEventsDto;
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
    public EventWithUsersDto createEvent(EventCreationDto eventCreationDto, String jwtToken) {

        Event newEvent = Event.builder()
                .name(eventCreationDto.getName())
                .shortDescription(eventCreationDto.getShortDescription())
                .longDescription(eventCreationDto.getLongDescription())
                .exactAddress(eventCreationDto.getExactAddress())
                .tags(new HashSet<>())
                .attendingUsers(new HashSet<>())
                .build();
        newEvent.setOwner(userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).get());
        newEvent.setCity(cityUtils.resolveCity(eventCreationDto.getCity()));

        if (!eventCreationDto.getTags().isEmpty()){
            Optional<Tag> tagOptional;
            for (String tagName : eventCreationDto.getTags())
            {
                 tagOptional = tagRepository.findByName(tagName);
                 if (tagOptional.isPresent())
                     newEvent.addTag(tagOptional.get());
                 else
                     newEvent.addTag(tagRepository.save(new Tag(tagName)));
            }
        }
      /* if (!eventCreationDto.getTags().isEmpty())
                eventCreationDto.getTags().forEach(tag ->
                        newEvent.addTag(tagRepository.findByName(tag).orElse(tagRepository.save(new Tag(tag)))));*/
        newEvent.setEventStartDate(eventCreationDto.getEventStartDate() != null ? eventCreationDto.getEventStartDate() : null);
        newEvent.setCreateDate(new Date(Calendar.getInstance().getTimeInMillis()));
        newEvent.setLastUpdate(newEvent.getCreateDate());


        return eventMapper.mapEventToEventWithUsersDto(eventRepository.save(newEvent));
    }

    @Override
    public EventWithUsersDto updateEvent(EventCreationDto eventCreationDto) {
        return null;
    }
}
