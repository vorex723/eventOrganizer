package com.mazurek.eventOrganizer.event;


import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.city.CityUtils;
import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.dto.EventWithUsersDto;
import com.mazurek.eventOrganizer.event.mapper.EventMapper;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotAttenderException;
import com.mazurek.eventOrganizer.exception.event.NotEventOwnerException;
import com.mazurek.eventOrganizer.exception.thread.*;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.jwt.JwtUtil;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.tag.TagRepository;
import com.mazurek.eventOrganizer.thread.*;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadReplayCreateDto;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class EventServiceImpl implements EventService{

    private final EventRepository eventRepository;
    private final CityRepository cityRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final ThreadRepository threadRepository;
    private final ThreadReplyRepository threadReplyRepository;
    private final EventMapper eventMapper;
    private final ThreadMapper threadMapper;
    private final CityUtils cityUtils;
    private final JwtUtil jwtUtil;
    @Override
    public EventWithUsersDto getEventById(Long id) {
        Optional<Event> eventOptional = eventRepository.findById(id);
        if (eventOptional.isEmpty())
            throw new EventNotFoundException("Event does not exist.");
        return eventMapper.mapEventToEventWithUsersDto(eventOptional.get());
    }

    @Override
    public EventWithUsersDto createEvent(EventCreateDto eventCreateDto, String jwtToken) throws RuntimeException {

        Event newEvent = Event.builder()
                .name(eventCreateDto.getName())
                .shortDescription(eventCreateDto.getShortDescription())
                .longDescription(eventCreateDto.getLongDescription())
                .exactAddress(eventCreateDto.getExactAddress())
                .eventStartDate(eventCreateDto.getEventStartDate())
                .tags(new HashSet<>())
                .attendingUsers(new HashSet<>())
                .threads(new HashSet<>())
                .build();
        newEvent.setOwner(userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).get());
        newEvent.setCity(cityUtils.resolveCity(eventCreateDto.getCity()));

        resolveTagsForNewEvent(newEvent, eventCreateDto);

        newEvent.setEventStartDate(eventCreateDto.getEventStartDate() != null ? eventCreateDto.getEventStartDate() : null);
        newEvent.setCreateDate(new Date(Calendar.getInstance().getTimeInMillis()));
        newEvent.setLastUpdate(newEvent.getCreateDate());

        return eventMapper.mapEventToEventWithUsersDto(eventRepository.save(newEvent));
    }
    @Override
    @Transactional
    public EventWithUsersDto updateEvent(EventCreateDto updatedEventDto, Long id, String jwtToken) throws RuntimeException{

        Optional<Event> eventOptional = eventRepository.findById(id);

        if (eventOptional.isEmpty())
            throw new EventNotFoundException("There is no event with that id.");

        Event storedEvent = eventOptional.get();

        if (!storedEvent.getOwner().equals(userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).get()))
            throw new NotEventOwnerException("It is not your event.");

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

        if (storedEvent.getAttendingUsers().contains(attender))
            return false;

        storedEvent.addAttendingUser(attender);
        eventRepository.save(storedEvent);
        return true;
    }

    @Override
    public ThreadDto createThreadInEvent(ThreadCreateDto threadCreateDto, Long eventId, String jwtToken) throws RuntimeException{
        Optional<Event> eventOptional = eventRepository.findById(eventId);

        if (eventOptional.isEmpty())
            throw new EventNotFoundException("You can not create new thread in not existing event.");

        Event storedEvent = eventOptional.get();
        User threadOwner = userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).get();

        if (!storedEvent.isUserAttending(threadOwner))
            throw new NotAttenderException("You are not attending this event. You have to be attending this event to be able to start new thread. ");

        Thread newThread = Thread.builder()
                .owner(threadOwner)
                .name(threadCreateDto.getName())
                .content(threadCreateDto.getContent())
                .event(storedEvent)
                .createDate(Calendar.getInstance().getTime())
                .editCounter(0)
                .replies(new HashSet<>())
                .build();
        newThread.setLastTimeEdited(newThread.getCreateDate());

        storedEvent.addThread(newThread);

        Thread savedThread = threadRepository.save(newThread);
        return threadMapper.mapThreadToThreadDto(savedThread);
    }

    @Override
    public ThreadDto updateThreadInEvent(ThreadCreateDto threadCreateDto, Long eventId, Long threadId, String jwtToken) throws RuntimeException{
        Optional<Event> eventOptional = eventRepository.findById(1L);
        if (eventOptional.isEmpty())
            throw new EventNotFoundException("There is no event with this id.");
        Event event = eventOptional.get();

        User threadOwner = userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).get();

        Optional<Thread> threadToUpdateOptional = threadRepository.findById(threadId);
        if (threadToUpdateOptional.isEmpty())
            throw new ThreadNotFoundException("Thread with this id do not exist.");
        Thread threadToUpdate = threadToUpdateOptional.get();

        if(!event.isUserAttending(threadOwner))
            throw new NotAttenderException("You are not attending event.");
        if(!threadToUpdate.isUserOwner(threadOwner))
            throw new NotThreadOwnerException("You are not creator of this thread.");

        threadToUpdate.setName(threadCreateDto.getName());
        threadToUpdate.setContent(threadCreateDto.getContent());
        threadToUpdate.setLastTimeEdited(Calendar.getInstance().getTime());
        threadToUpdate.incrementEditCounter();

        Thread updatedThread = threadRepository.save(threadToUpdate);

        return threadMapper.mapThreadToThreadDto(updatedThread);
    }
    @Override
    public ThreadDto createReplyInThread(ThreadReplayCreateDto threadReplayCreateDto, Long eventId, Long threadId, String jwtToken) throws RuntimeException{
        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if (eventOptional.isEmpty())
            throw new EventNotFoundException("There is no event with that id.");
        Event event = eventOptional.get();
        User replayingUser = userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).get();

        if(!event.isUserAttending(replayingUser))
            throw new NotAttenderException("You are not attending event.");

        Optional<Thread> threadOptional = threadRepository.findById(threadId);
        if(threadOptional.isEmpty())
            throw new ThreadNotFoundException("There is no event with that id");

        ThreadReply newThreadReply = ThreadReply.builder()
                .content(threadReplayCreateDto.getReplyContent())
                .thread(threadOptional.get())
                .replier(replayingUser)
                .replayDate(Calendar.getInstance().getTime())
                .editCounter(0)
                .build();
        newThreadReply.setLastEditDate(newThreadReply.getReplayDate());
        threadReplyRepository.save(newThreadReply);

        return threadMapper.mapThreadToThreadDto(threadOptional.get());
    }

    @Override
    public ThreadDto updateThreadReplyInEvent(ThreadReplayCreateDto threadReplayUpdateDto, Long eventId, Long threadId, Long threadReplyId, String jwtToken) throws RuntimeException{
        Optional<Event> eventOptional = eventRepository.findById(eventId);
        if (eventOptional.isEmpty())
            throw new EventNotFoundException("There is no event with that id.");
        Event event = eventOptional.get();
        User replayingUser = userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).get();

        if(!event.isUserAttending(replayingUser))
            throw new NotAttenderException("You are not attending event.");

        Optional<Thread> threadOptional = threadRepository.findById(threadId);
        if(threadOptional.isEmpty())
            throw new ThreadNotFoundException("There is no event with that id");
        Thread thread = threadOptional.get();

        Optional<ThreadReply> threadReplyOptional = threadReplyRepository.findById(threadReplyId);
        if (threadReplyOptional.isEmpty())//koniec testów
            throw new ThreadReplyNotFoundException("There is no replay with this id.");
        ThreadReply replyToUpdate = threadReplyOptional.get();

        if (!thread.containsReply(replyToUpdate)){
            throw new WrongThreadException("This reply is not in this thread.");
        }
        if (!replyToUpdate.isReplier(replayingUser))
            throw new NotThreadReplyOwnerException("It is not your reply!");

        replyToUpdate.setContent(threadReplayUpdateDto.getReplyContent());
        replyToUpdate.incrementEditCounter();
        threadReplyRepository.save(replyToUpdate);

        return threadMapper.mapThreadToThreadDto(thread);
    }

    private void resolveTagsForNewEvent(Event event, EventCreateDto sourceDto) {
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

    private void resolveTagsForUpdatingEvent(Event event, EventCreateDto sourceDto) {
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

    private void updateEventFields(Event eventToUpdate, EventCreateDto source){
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
