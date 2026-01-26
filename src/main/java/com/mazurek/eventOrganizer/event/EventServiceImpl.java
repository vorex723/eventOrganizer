package com.mazurek.eventOrganizer.event;


import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.dto.EventDto;
import com.mazurek.eventOrganizer.event.dto.EventOverviewDto;
import com.mazurek.eventOrganizer.event.dto.EventOverviewPageDto;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import com.mazurek.eventOrganizer.exception.event.*;
import com.mazurek.eventOrganizer.exception.file.FileNotFoundInEventException;
import com.mazurek.eventOrganizer.exception.file.FileTypeNotAllowedException;
import com.mazurek.eventOrganizer.exception.search.NoSearchParametersPresentException;
import com.mazurek.eventOrganizer.exception.search.NoSearchResultException;
import com.mazurek.eventOrganizer.exception.thread.*;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.file.*;
import com.mazurek.eventOrganizer.jwt.JwtUtils;
import com.mazurek.eventOrganizer.notification.NotificationService;
import com.mazurek.eventOrganizer.notification.NotificationType;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.tag.TagRepository;
import com.mazurek.eventOrganizer.thread.*;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadReplyCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadReplyDto;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import com.mazurek.eventOrganizer.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class EventServiceImpl implements EventService{

    private final EventRepository eventRepository;
    private final CityRepository cityRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final ThreadRepository threadRepository;
    private final ThreadReplyRepository threadReplyRepository;
    private final FileRepository fileRepository;
    private final AuthenticationService authenticationService;
    private final NotificationService notificationService;
    private final JwtUtils jwtUtils;
    private final FileUtils fileUtils;
    private final int  PAGE_DEFAULT_SIZE = 20;

    /*
     ********************************************************************************************************************
     *                                              GETTERS
     ********************************************************************************************************************
     */

    @Override
    @Transactional
    public List<EventOverviewDto> getEvents(int pageNumber) {
        List<Event> events = eventRepository.findAll();

        if (events.isEmpty())
            throw new NoEventsException();

        return events.stream().map(EventOverviewDto::new).toList();
    }

    @Override
    public EventDto getEventById(UUID id) {
        Event event = eventRepository.findById(id).orElseThrow(EventNotFoundException::new);
        return new EventDto(event);
    }

    @Override
    @Transactional
    public EventOverviewPageDto getUserEventsByUserId(UUID id, int pageNumber, boolean upcomingEventsOnly) {
        return new EventOverviewPageDto(eventRepository.findEventsByOwnerId(id, PageRequest.of(pageNumber, PAGE_DEFAULT_SIZE, Sort.by("eventStartDate").descending())));
    }

    @Transactional
    public EventOverviewPageDto getUserAttendingEventsByUserId(UUID id, int pageNumber, boolean upcomingEventsOnly, String jwt){
        return new EventOverviewPageDto(eventRepository.findUserAttendingEventsByUserId(id, PageRequest.of(pageNumber, PAGE_DEFAULT_SIZE)));
    }

    @Override
    public FileOverviewDto getFileOverviewById(UUID fileId, UUID eventId, String jwtToken) {
        User performingUser = userRepository.findByEmail(jwtUtils.extractUsername(jwtToken)).orElseThrow(UserNotFoundException::new);
        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);

        if (!event.isUserAttending(performingUser))
            throw new NotEventAttenderException();

        return new FileOverviewDto(fileRepository.findByIdAndEventId(fileId, eventId).orElseThrow(FileNotFoundInEventException::new));
    }

    @Override
    public File getFileDataById(UUID fileId, UUID eventId, String jwtToken) {
        User performingUser = userRepository.findByEmail(jwtUtils.extractUsername(jwtToken)).orElseThrow(UserNotFoundException::new);
        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);

        if (!event.isUserAttending(performingUser))
            throw new NotEventAttenderException();

        return fileRepository.findByIdAndEventId(fileId, eventId).orElseThrow(FileNotFoundInEventException::new);
    }

    @Override
    public FileOverviewPageDto getFileOverviewPageByEventId(UUID eventId, int pageNumber, String jwtToken) {
        if (pageNumber < 0)
            throw new InvalidPageNumberException();
        User performingUser = userRepository.findByEmail(jwtUtils.extractUsername(jwtToken)).orElseThrow(UserNotFoundException::new);
        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);
        if (!event.isUserAttending(performingUser))
            throw new NotEventAttenderException();

        PageRequest pageRequest = PageRequest.of(pageNumber, PAGE_DEFAULT_SIZE, Sort.by("uploadDateTime").ascending());

        Page<File> filePage = fileRepository.findByEventId(eventId, pageRequest);

        return new FileOverviewPageDto(filePage);
    }

    @Override

    @Transactional
    public List<EventOverviewDto> searchEvents(List<String> words, List<String> tags, String cityName) {
        if((words == null || words.isEmpty())  &&  (tags == null || tags.isEmpty()))
            throw new NoSearchParametersPresentException();

        Set<Event> foundEvents = new HashSet<>();

        if (tags!=null && !tags.isEmpty()){
            foundEvents.addAll(findEventsByTagNames(tags));
            if (words != null && !words.isEmpty()){
                filterEventsByWords(words, foundEvents);
            }
        }   else if (words != null && !words.isEmpty()){
            foundEvents.addAll(findEventsByWords(words));
        }

        if(cityName!=null && !cityName.isEmpty())
            foundEvents.removeIf(event -> !event.getCity().getName().equals(cityName.toLowerCase()));
        removeEventsWhichHadPlace(foundEvents);
        if (foundEvents.isEmpty())
            throw  new NoSearchResultException();

        return foundEvents.stream().map(EventOverviewDto::new).toList();
    }

    /*
     ********************************************************************************************************************
     *                                              CREATE
     ********************************************************************************************************************
    */

    @Override
    @Transactional
    public EventDto createEvent(EventCreateDto eventCreateDto) throws RuntimeException {

        User eventOwner = authenticationService.getCurrentUser();

        City city = cityRepository.findByIgnoreCaseName(eventCreateDto.getCity())
                .orElseGet(() -> cityRepository.save(new City(eventCreateDto.getCity().toLowerCase())));

        Set<Tag> tags = eventCreateDto.getTags().stream()
                .map(tagName -> tagRepository.findByIgnoreCaseName(tagName).orElseGet(() -> tagRepository.save(new Tag(tagName))))
                .collect(Collectors.toSet());

        Event newEvent = new Event();
        newEvent.setName(eventCreateDto.getName());
        newEvent.setShortDescription(eventCreateDto.getShortDescription());
        newEvent.setLongDescription(eventCreateDto.getLongDescription());
        newEvent.setOwner(eventOwner);
        newEvent.setExactAddress(eventCreateDto.getExactAddress());
        newEvent.setEventStartDate(eventCreateDto.getEventStartDate().withSecond(0).withNano(0));
        newEvent.setTimeZoneId(eventCreateDto.getEventStartDate().getZone().getId());
        ZonedDateTime createDateTime = ZonedDateTime.now();
        newEvent.setCreateDate(createDateTime);
        newEvent.setLastUpdate(createDateTime);
        newEvent.setCity(city);
        newEvent.setTags(tags);

        Event savedEvent = eventRepository.save(newEvent);

        city.addEvent(savedEvent);
        cityRepository.save(city);

        tags.forEach(tag -> tag.addEvent(savedEvent));
        tagRepository.saveAll(tags.stream().toList());

        eventOwner.addUserEvent(savedEvent);
        userRepository.save(eventOwner);

        return new EventDto(savedEvent);
    }

    @Override
    @Transactional
    public ThreadDto createThreadInEvent(ThreadCreateDto threadCreateDto, UUID eventId, String jwtToken) throws RuntimeException{
        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);

        User threadOwner = userRepository.findByEmail(jwtUtils.extractUsername(jwtToken)).orElseThrow(UserNotFoundException::new);

        if (!event.isUserAttending(threadOwner))
            throw new NotEventAttenderException();

        ZonedDateTime createDateTime = ZonedDateTime.now();

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

    @Override
    @Transactional
    public ThreadReplyDto createReplyInThread(ThreadReplyCreateDto threadReplyCreateDto, UUID eventId, UUID threadId, String jwtToken) throws RuntimeException{
        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);

        User replayingUser = userRepository.findByEmail(jwtUtils.extractUsername(jwtToken)).orElseThrow(UserNotFoundException::new);

        if(!event.isUserAttending(replayingUser))
            throw new NotEventAttenderException();

        Thread thread = threadRepository.findByIdAndEventId(threadId,eventId).orElseThrow(ThreadNotFoundInEventException::new);

        ZonedDateTime createDateTime = ZonedDateTime.now();

        ThreadReply savedThreadReply = threadReplyRepository.save(
                ThreadReply.builder()
                    .content(threadReplyCreateDto.getReplyContent())
                    .thread(thread)
                    .replier(replayingUser)
                    .replyDate(createDateTime)
                    .lastUpdate(createDateTime)
                    .editCounter(0)
                    .build());

        replayingUser.addThreadReply(savedThreadReply);
        thread.addReplyToThread(savedThreadReply);
        threadRepository.save(thread);

        if (!thread.getOwner().equals(replayingUser))
            notificationService.notifyThreadOwner(thread, replayingUser.getFullName());

        return new ThreadReplyDto(savedThreadReply);
    }

    @Override
    @Transactional
    public FileOverviewDto uploadFileToEvent(FileUploadDto fileUploadDto, UUID eventId, String jwtToken) throws RuntimeException, IOException {

        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);
        User user = userRepository.findByEmail(jwtUtils.extractUsername(jwtToken)).orElseThrow(UserNotFoundException::new);

        if (!event.isUserAttending(user))
            throw new NotEventAttenderException();

        if (!fileUtils.isFileCorrect(fileUploadDto.getFile()))
            throw new FileTypeNotAllowedException();
        ZonedDateTime uploadDateTime = ZonedDateTime.now();
        File fileToSave = File.builder()
                .owner(user)
                .event(event)
                .userFileName(fileUploadDto.getUserFileName())
                .originalFileName(fileUploadDto.getFile().getOriginalFilename())
                .contentType(fileUploadDto.getFile().getContentType())
                .content(fileUploadDto.getFile().getBytes())
                .uploadDateTime(uploadDateTime.withSecond(0).withNano(0))
                .build();

        event.addFile(fileToSave);
        user.addFile(fileToSave);
        File savedFile = fileRepository.save(fileToSave);

        notificationService.notifyEventAttenders(event, NotificationType.EVENT_NEW_FILE, event.getId(), user.getFullName());

        return new FileOverviewDto(savedFile);
    }

    /*
     ********************************************************************************************************************
     *                                              UPDATE
     ********************************************************************************************************************
    */

    @Override
    @Transactional
    public EventDto updateEvent(EventCreateDto updatedEventDto, UUID id, String jwtToken) throws RuntimeException{
        updatedEventDto.setTags(updatedEventDto.getTags().stream().map(String::toLowerCase).toList());

        Event storedEvent = eventRepository.findById(id).orElseThrow(EventNotFoundException::new);

        if (storedEvent.hadPlace())
            throw new EventAlreadyHadPlaceException();
        if (!storedEvent.getOwner().equals(userRepository.findByEmail(jwtUtils.extractUsername(jwtToken)).get()))
            throw new NotEventOwnerException();

        storedEvent.setName(updatedEventDto.getName());
        storedEvent.setShortDescription(updatedEventDto.getShortDescription());
        storedEvent.setLongDescription(updatedEventDto.getLongDescription());
        storedEvent.setExactAddress(updatedEventDto.getExactAddress());
        storedEvent.setEventStartDate(updatedEventDto.getEventStartDate().withSecond(0).withNano(0));
        storedEvent.setTimeZoneId(updatedEventDto.getEventStartDate().getZone().getId());
        storedEvent.setLastUpdate(ZonedDateTime.now());

        City newCity = cityRepository.findByIgnoreCaseName(updatedEventDto.getCity())
                .orElseGet(() -> cityRepository.save(new City(updatedEventDto.getCity())));

        if (!storedEvent.getCity().equals(newCity)) {
            storedEvent.getCity().removeEvent(storedEvent);
            cityRepository.save(storedEvent.getCity());
            storedEvent.setCity(newCity);
            newCity.addEvent(storedEvent);
            cityRepository.save(newCity);
        }

        Set<Tag> removedTags = storedEvent.getTags().stream()
                .filter(tag -> !updatedEventDto.getTags().contains(tag.getName()))
                .collect(Collectors.toSet());

        removedTags.forEach(storedEvent::removeTag);

        tagRepository.saveAll(removedTags);

        updatedEventDto.getTags().forEach(tagName -> {
            if (storedEvent.containsTagByName(tagName))
                return;
            Tag newTag = tagRepository.findByIgnoreCaseName(tagName).orElseGet(() -> tagRepository.save(new Tag(tagName)));
            storedEvent.addTag(newTag);
        });

        notificationService.notifyEventAttenders(storedEvent, NotificationType.EVENT_UPDATE, storedEvent.getId(),storedEvent.getOwner().getFullName());

        return new EventDto(eventRepository.save(storedEvent));
    }

    @Override
    @Transactional
    public ThreadDto updateThreadInEvent(ThreadCreateDto threadCreateDto, UUID eventId, UUID threadId, String jwtToken) throws RuntimeException{
        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);

        User threadOwner = userRepository.findByEmail(jwtUtils.extractUsername(jwtToken)).orElseThrow(UserNotFoundException::new);

        Thread threadToUpdate = threadRepository.findByIdAndEventId(threadId,eventId).orElseThrow(ThreadNotFoundInEventException::new);

        if(!event.isUserAttending(threadOwner))
            throw new NotEventAttenderException();
        if(!threadToUpdate.isUserOwner(threadOwner))
            throw new NotThreadOwnerException();

        threadToUpdate.setName(threadCreateDto.getName());
        threadToUpdate.setContent(threadCreateDto.getContent());
        threadToUpdate.setLastUpdate(ZonedDateTime.now());
        threadToUpdate.incrementEditCounter();

        Thread updatedThread = threadRepository.save(threadToUpdate);

        return new ThreadDto(updatedThread);
    }

    @Override
    @Transactional
    public ThreadReplyDto updateThreadReplyInEventThread(ThreadReplyCreateDto threadReplyUpdateDto, UUID eventId, UUID threadId, UUID threadReplyId, String jwtToken) throws RuntimeException{
        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);

        User replyingUser = userRepository.findByEmail(jwtUtils.extractUsername(jwtToken)).orElseThrow(UserNotFoundException::new);

        if(!event.isUserAttending(replyingUser))
            throw new NotEventAttenderException();

        Thread thread = threadRepository.findByIdAndEventId(threadId,eventId).orElseThrow(ThreadNotFoundInEventException::new);

        ThreadReply threadReply = threadReplyRepository.findByIdAndThreadId(threadReplyId, threadId).orElseThrow(ReplyNotFoundInThreadException::new);

        if (!threadReply.isReplier(replyingUser))
            throw new NotThreadReplyOwnerException();

        threadReply.setContent(threadReplyUpdateDto.getReplyContent());
        threadReply.incrementEditCounter();
        threadReply.setLastUpdate(ZonedDateTime.now());

        return new ThreadReplyDto(threadReplyRepository.save(threadReply));
    }


    /*
     ********************************************************************************************************************
     *                                         ACTIONS
     ********************************************************************************************************************
     */

    @Override
    @Transactional
    public boolean addAttenderToEvent(UUID eventId, String jwt) throws RuntimeException {

        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);

        if (event.hadPlace())
            throw new EventAlreadyHadPlaceException();
        User attender = userRepository.findByEmail(jwtUtils.extractUsername(jwt)).orElseThrow(UserNotFoundException::new);

        if (event.getOwner().equals(attender))
            throw new EventOwnerAlreadyAttendsEventException();

        if (event.isUserAttending(attender))
            throw new AlreadyAttendingEventException();

        event.addAttendingUser(attender);
        eventRepository.save(event);

        return true;
    }

    @Override
    @Transactional
    public boolean removeAttenderFromEvent(UUID eventId, String jwt) {

        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);

        if (event.hadPlace())
            throw new EventAlreadyHadPlaceException();
        User attender = userRepository.findByEmail(jwtUtils.extractUsername(jwt)).orElseThrow(UserNotFoundException::new);

        if (event.getOwner().equals(attender))
            throw new EventOwnerMustAttendEventException();

        if (!event.isUserAttending(attender))
            throw new NotEventAttenderException();

        event.removeAttendingUser(attender);
        attender.getThreads().removeIf(thread -> thread.getEvent().equals(event));
        attender.getThreadReplies().removeIf(threadReply -> threadReply.getThread().getEvent().equals(event));
        attender.getFiles().removeIf(file -> file.getEvent().equals(event));

        eventRepository.save(event);
        userRepository.save(attender);

        return true;
    }
    /*
     ********************************************************************************************************************
     *                                         PRIVATE HELPERS
     ********************************************************************************************************************
    */

    private Set<Event> findEventsByTagNames(List<String> tagNames){
        List<String> eventTagNames = new ArrayList<>();

        Set<Event> foundEvents = new HashSet<>(eventRepository.findByIgnoreCaseTagsNameIn(tagNames));

        Iterator<Event> eventIterator = foundEvents.iterator();
        while (eventIterator.hasNext()) {
            Event event = eventIterator.next();
            event.getTags().forEach(tag -> eventTagNames.add(tag.getName()));
            if (!eventTagNames.containsAll(tagNames)) {
                eventIterator.remove();
            }
            tagNames.clear();
        }
        return foundEvents;
    }

    private Set<Event> findEventsByTagNames2(List<String> tagNames){
        List<String> eventTagNames = new ArrayList<>();

        Set<Event> foundEvents = new HashSet<>(eventRepository.findByIgnoreCaseTagsNameIn(tagNames));

        Set<Event> filteredEvents = foundEvents.stream().filter(event -> event.getTags().stream().anyMatch(tag -> tagNames.contains(tag.getName()))).collect(Collectors.toSet());


        return filteredEvents;
    }

    private Set<Event> findEventsByWords(List<String> words){
        Set<Event> foundEvents = new HashSet<>();
        words.forEach(word -> foundEvents.addAll(eventRepository.findByIgnoreCaseNameContaining(word)));
        return foundEvents;
    }
    private void filterEventsByWords(List<String> words, Set<Event> foundEvents){
        Iterator<Event> eventIterator = foundEvents.iterator();
        boolean containsAny = false;
        while (eventIterator.hasNext()) {
            Event event = eventIterator.next();

            for (String word : words) {
                if (event.getName().contains(word)){
                    containsAny = true;
                    break;
                }
            }
            if (!containsAny)
                eventIterator.remove();
            containsAny=false;
        }
    }
    private void removeEventsWhichHadPlace(Set<Event> foundEvents) {
        Iterator<Event> eventIterator = foundEvents.iterator();
        Event event;
        while(eventIterator.hasNext()){
            event = eventIterator.next();
            if(event.hadPlace())
                eventIterator.remove();
        }

    }

  /*  @Override
    @Transactional
    public EventOverviewPageDto getUserEventsByUserId(UUID id, int pageNumber, boolean upcomingEventsOnly) {
        return new EventOverviewPageDto(eventRepository.customQuery(id, PageRequest.of(pageNumber, PAGE_DEFAULT_SIZE)));
        if (upcomingEventsOnly)
            return userRepository.findById(id).orElseThrow(UserNotFoundException::new)
                    .getUserEvents().stream().filter(event -> !event.hadPlace()).map(EventOverviewDto::new).toList();

        return userRepository.findById(id).orElseThrow(UserNotFoundException::new)
                .getUserEvents().stream().map(EventOverviewDto::new).toList();
    }


    public EventOverviewPageDto getUserAttendingEventsByUserId(UUID id, int pageNumber, boolean upcomingEventsOnly, String jwt){
        if (!userRepository.findByEmail(jwtUtil.extractUsername(jwt)).orElseThrow(UserNotFoundException::new).getId().equals(id))
            throw new InvalidUserException();

        if (upcomingEventsOnly)
            return userRepository.findById(id).orElseThrow(UserNotFoundException::new))
                    .getAttendingEvents().stream().filter(event -> !event.hadPlace()).map(EventOverviewPageDto::new).toList();

        return userRepository.findById(id).orElseThrow(UserNotFoundException::new)
                .getAttendingEvents().stream().map(EventOverviewDto::new).toList();
        return new EventOverviewPageDto(eventRepository.customQuery(id, PageRequest.of(pageNumber, PAGE_DEFAULT_SIZE)));
    }*/

}
