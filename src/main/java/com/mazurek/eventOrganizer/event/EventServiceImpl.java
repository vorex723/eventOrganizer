package com.mazurek.eventOrganizer.event;


import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.city.CityUtils;
import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.dto.EventDto;
import com.mazurek.eventOrganizer.event.dto.EventOverviewDto;
import com.mazurek.eventOrganizer.event.dto.EventOverviewPageDto;
import com.mazurek.eventOrganizer.exception.event.*;
import com.mazurek.eventOrganizer.exception.file.EmptyUploadedFileException;
import com.mazurek.eventOrganizer.exception.file.FileNotFoundException;
import com.mazurek.eventOrganizer.exception.file.FileTypeNotAllowedException;
import com.mazurek.eventOrganizer.exception.search.NoSearchParametersPresentException;
import com.mazurek.eventOrganizer.exception.search.NoSearchResultException;
import com.mazurek.eventOrganizer.exception.thread.*;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.file.File;
import com.mazurek.eventOrganizer.file.FileRepository;
import com.mazurek.eventOrganizer.jwt.JwtUtil;
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
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class EventServiceImpl implements EventService{
    private final int REPOSITORY_PAGE_SIZE = 20;
    private final EventRepository eventRepository;
    private final CityRepository cityRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final ThreadRepository threadRepository;
    private final ThreadReplyRepository threadReplyRepository;
    private final FileRepository fileRepository;
    private final NotificationService notificationService;
    private final CityUtils cityUtils;
    private final JwtUtil jwtUtil;
    private final Tika tikaFileTypeDetector;
    private final int  PAGE_DEFAULT_SIZE = 30;
    private final static String[] FILE_EXTENSION_WHITELIST = {".jpg", ".jpeg", ".png", "pdf", ".doc", ".docx", ".ppt",".pptx" ,".odt", ".xls", ".xlsx", ".mp4", ".avi"};
    private final static String[] CONTENT_TYPE_WHITELIST = {
            "image/jpeg",
            "image/jpeg",
            "image/png",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.oasis.opendocument.text",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "video/mp4",
            "video/x-msvideo"
    };


    /*
     ********************************************************************************************************************
     *                                              GETTERS
     ********************************************************************************************************************
     */

    @Override

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
        return new EventOverviewPageDto(eventRepository.findEventsByOwnerId(id, PageRequest.of(pageNumber, PAGE_DEFAULT_SIZE)));
    }

    @Transactional
    public EventOverviewPageDto getUserAttendingEventsByUserId(UUID id, int pageNumber, boolean upcomingEventsOnly, String jwt){
        return new EventOverviewPageDto(eventRepository.findUserAttendingEventsByUserId(id, PageRequest.of(pageNumber, PAGE_DEFAULT_SIZE)));
    }

    @Override
    public File getFile(UUID id, UUID eventId, String jwtToken) {
        File fileToBeServed = fileRepository.findById(id).orElseThrow(FileNotFoundException::new);
        if(!fileToBeServed.getEvent().isUserAttending(userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).get()))
            throw new NotEventAttenderException();
        return fileToBeServed;
    }

    //to-do
    public boolean removeAttenderFromEvent(Long eventId, String jwtToken){
        return true;
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
    public EventDto createEvent(EventCreateDto eventCreateDto, String jwtToken) throws RuntimeException {
        if(eventCreateDto.getEventStartDate().isBefore(ZonedDateTime.now()))
            throw new InvalidEventStartDateException();

        User owner = userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).get();

        Event newEvent = Event.builder()
                .name(eventCreateDto.getName())
                .shortDescription(eventCreateDto.getShortDescription())
                .longDescription(eventCreateDto.getLongDescription())
                .owner(owner)
                .exactAddress(eventCreateDto.getExactAddress())
                .timeZoneId(eventCreateDto.getEventStartDate().getZone().getId())
                .city(cityUtils.resolveCity(eventCreateDto.getCity()))
                .eventStartDate(eventCreateDto.getEventStartDate().withSecond(0).withNano(0))
                .createDate(ZonedDateTime.now())
                .build();
        newEvent.setLastUpdate(newEvent.getCreateDate());

        resolveTagsForNewEvent(newEvent, eventCreateDto);

        return new EventDto(eventRepository.save(newEvent));
    }


    @Override
    @Transactional
    public ThreadDto createThreadInEvent(ThreadCreateDto threadCreateDto, UUID eventId, String jwtToken) throws RuntimeException{
        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);

        User threadOwner = userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).orElseThrow(UserNotFoundException::new);

        if (!event.isUserAttending(threadOwner))
            throw new NotEventAttenderException();

        Thread newThread = Thread.builder()
                .owner(threadOwner)
                .name(threadCreateDto.getName())
                .content(threadCreateDto.getContent())
                .event(event)
                .createDate(LocalDateTime.now())
                .editCounter(0)
                .replies(new HashSet<>())
                .build();
        newThread.setLastUpdate(newThread.getCreateDate());

        event.addThread(newThread);
        threadOwner.addThread(newThread);
        Thread savedThread = threadRepository.save(newThread);

        return new ThreadDto(savedThread);
    }

    @Override
    @Transactional
    public ThreadReplyDto createReplyInThread(ThreadReplyCreateDto threadReplyCreateDto, UUID eventId, UUID threadId, String jwtToken) throws RuntimeException{
        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);

        User replayingUser = userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).get();

        if(!event.isUserAttending(replayingUser))
            throw new NotEventAttenderException();

        Thread thread = threadRepository.findByIdAndEventId(threadId,eventId).orElseThrow(ThreadNotFoundInEventException::new);

        ThreadReply newThreadReply = ThreadReply.builder()
                .content(threadReplyCreateDto.getReplyContent())
                .thread(thread)
                .replier(replayingUser)
                .replayDate(ZonedDateTime.now())
                .editCounter(0)
                .build();
        newThreadReply.setLastUpdate(newThreadReply.getReplayDate());

        thread.addReplayToThread(newThreadReply);
        threadRepository.save(thread);

        if (!thread.getOwner().equals(replayingUser))
            notificationService.notifyThreadOwner(thread ,replayingUser.getFullName());

        return new ThreadReplyDto(threadReplyRepository.save(newThreadReply));
    }

    @Override
    @Transactional
    public EventDto uploadFileToEvent(MultipartFile uploadedFile, UUID eventId, String jwtToken) throws RuntimeException, IOException {
        if (uploadedFile.isEmpty())
            throw new EmptyUploadedFileException();
        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);
        User user = userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).get();
        if (!event.isUserAttending(user))
            throw new NotEventAttenderException();

        if (!isFileCorrect(uploadedFile))
            throw new FileTypeNotAllowedException();


        File fileToSave = File.builder()
                .owner(user)
                .event(event)
                .name(uploadedFile.getOriginalFilename())
                .contentType(uploadedFile.getContentType())
                .content(uploadedFile.getBytes())
                .build();

        event.addFile(fileToSave);
        user.addFile(fileToSave);
        fileRepository.save(fileToSave);

        notificationService.notifyEventAttenders(event, NotificationType.EVENT_NEW_FILE, event.getId(), user.getFullName());

        return new EventDto(event);
    }

    /*
     ********************************************************************************************************************
     *                                              UPDATE
     ********************************************************************************************************************
    */

    @Override
    @Transactional
    public EventDto updateEvent(EventCreateDto updatedEventDto, UUID id, String jwtToken) throws RuntimeException{

        Event storedEvent = eventRepository.findById(id).orElseThrow(EventNotFoundException::new);

        if (storedEvent.hadPlace())
            throw new EventAlreadyHadPlaceException();
        if (!storedEvent.getOwner().equals(userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).get()))
            throw new NotEventOwnerException();

        updateEventFields(storedEvent, updatedEventDto);

        notificationService.notifyEventAttenders(storedEvent, NotificationType.EVENT_UPDATE, storedEvent.getId(),storedEvent.getOwner().getFullName());

        return new EventDto(eventRepository.save(storedEvent));

    }

    @Override
    @Transactional
    public ThreadDto updateThreadInEvent(ThreadCreateDto threadCreateDto, UUID eventId, UUID threadId, String jwtToken) throws RuntimeException{
        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);

        User threadOwner = userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).orElseThrow(UserNotFoundException::new);

        Thread threadToUpdate = threadRepository.findByIdAndEventId(threadId,eventId).orElseThrow(ThreadNotFoundInEventException::new);

        if(!event.isUserAttending(threadOwner))
            throw new NotEventAttenderException();
        if(!threadToUpdate.isUserOwner(threadOwner))
            throw new NotThreadOwnerException();


        threadToUpdate.update(threadCreateDto);

        Thread updatedThread = threadRepository.save(threadToUpdate);

        return new ThreadDto(updatedThread);
    }

    @Override
    @Transactional
    public ThreadReplyDto updateThreadReplyInEvent(ThreadReplyCreateDto threadReplayUpdateDto, UUID eventId, UUID threadId, UUID threadReplyId, String jwtToken) throws RuntimeException{
        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);

        User replayingUser = userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).get();

        if(!event.isUserAttending(replayingUser))
            throw new NotEventAttenderException();

        Thread thread = threadRepository.findByIdAndEventId(threadId,eventId).orElseThrow(ThreadNotFoundInEventException::new);

        ThreadReply threadReply = threadReplyRepository.findByIdAndThreadId(threadReplyId, threadId).orElseThrow(ReplyNotFoundInThreadException::new);

        if (!threadReply.isReplier(replayingUser))
            throw new NotThreadReplyOwnerException();

        threadReply.setContent(threadReplayUpdateDto.getReplyContent());
        threadReply.incrementEditCounter();
        threadReplyRepository.save(threadReply);

        return new ThreadReplyDto(threadReplyRepository.save(threadReply));
    }


    /*
     ********************************************************************************************************************
     *                                         ACTIONS
     ********************************************************************************************************************
     */

    @Override
    @Transactional
    public boolean addAttenderToEvent(UUID id, String jwt) throws RuntimeException {

        Event event = eventRepository.findById(id).orElseThrow(EventNotFoundException::new);

        if (event.hadPlace())
            throw new EventAlreadyHadPlaceException();
        User attender = userRepository.findByEmail(jwtUtil.extractUsername(jwt)).get();

        if (event.getOwner().equals(attender))
            throw new EventOwnerAlreadyAttendsEventException();

        if (event.getAttendingUsers().contains(attender))
            return false;

        event.addAttendingUser(attender);
        eventRepository.save(event);

        return true;
    }

    @Override
    @Transactional
    public boolean removeAttenderFromEvent(UUID id, String jwt) {

        Event event = eventRepository.findById(id).orElseThrow(EventNotFoundException::new);

        if (event.hadPlace())
            throw new EventAlreadyHadPlaceException();
        User attender = userRepository.findByEmail(jwtUtil.extractUsername(jwt)).get();

        if (event.getOwner().equals(attender))
            throw new EventOwnerAlreadyAttendsEventException();

        if (event.isUserAttending(attender))
            event.removeAttendingUser(attender);

        attender.getThreads().removeIf(thread -> thread.getEvent().equals(event));
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

    private void updateEventFields(Event eventToUpdate, EventCreateDto source){
        eventToUpdate.setName(source.getName());
        eventToUpdate.setShortDescription(source.getShortDescription());
        eventToUpdate.setLongDescription(source.getLongDescription());
        eventToUpdate.setCity(cityUtils.resolveCity(source.getCity()));
        eventToUpdate.setExactAddress(source.getExactAddress());
        eventToUpdate.setEventStartDate(source.getEventStartDate().withSecond(0).withNano(0));
        eventToUpdate.setTimeZoneId(source.getEventStartDate().getZone().getId());
        eventToUpdate.setLastUpdate(ZonedDateTime.now());
        resolveTagsForUpdatingEvent(eventToUpdate,source);
    }
    private boolean isFileCorrect(MultipartFile uploadedFile) throws IOException {
        String tikaOutput = tikaFileTypeDetector.detect(uploadedFile.getBytes());
        boolean correctFileExtensionFlag = false;

        if(tikaOutput.equals(uploadedFile.getContentType())){
            for (int iterator = 0; iterator < FILE_EXTENSION_WHITELIST.length; iterator++) {
                if (uploadedFile.getOriginalFilename().endsWith(FILE_EXTENSION_WHITELIST[iterator]) && uploadedFile.getContentType().equals(CONTENT_TYPE_WHITELIST[iterator])) {
                    correctFileExtensionFlag = true;
                    break;
                }
            }
        }
        return correctFileExtensionFlag;
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

                tagOptional = tagRepository.findByIgnoreCaseName(tagName);

                if (tagOptional.isPresent())
                    event.addTag(tagOptional.get());
                else
                    event.addTag(tagRepository.save(new Tag(tagName.toLowerCase())));
            }
        }
        else
            event.clearTags();
    }

    private void removeEventsFromOtherCities(Set<Event> foundEvents, String cityName) {
        Iterator<Event> eventIterator = foundEvents.iterator();
        foundEvents.removeIf(event -> !event.getCity().getName().equals(cityName));
    }
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
    private void resolveTagsForNewEvent(Event event, EventCreateDto sourceDto) {
        if (!sourceDto.getTags().isEmpty()){
            Optional<Tag> tagOptional;
            for (String tagName : sourceDto.getTags())
            {
                tagOptional = tagRepository.findByIgnoreCaseName(tagName);
                if (tagOptional.isPresent())
                    event.addTag(tagOptional.get());
                else
                    event.addTag(tagRepository.save(new Tag(tagName.toLowerCase())));


            }
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
