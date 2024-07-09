package com.mazurek.eventOrganizer.event;


import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.city.CityUtils;
import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.dto.EventWithUsersDto;
import com.mazurek.eventOrganizer.event.dto.EventWithoutUsersDto;
import com.mazurek.eventOrganizer.event.mapper.EventMapper;
import com.mazurek.eventOrganizer.exception.event.*;
import com.mazurek.eventOrganizer.exception.file.EmptyUploadedFileException;
import com.mazurek.eventOrganizer.exception.file.FileNotFoundException;
import com.mazurek.eventOrganizer.exception.file.FileTypeNotAllowedException;
import com.mazurek.eventOrganizer.exception.search.NoSearchParametersPresentException;
import com.mazurek.eventOrganizer.exception.search.NoSearchResultException;
import com.mazurek.eventOrganizer.exception.thread.*;
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
import com.mazurek.eventOrganizer.thread.dto.ThreadReplayCreateDto;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

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
    private final EventMapper eventMapper;
    private final ThreadMapper threadMapper;
    private final NotificationService notificationService;
    private final CityUtils cityUtils;
    private final JwtUtil jwtUtil;
    private final Tika tikaFileTypeDetector;
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

    @Override

    public List<EventWithoutUsersDto> getEvents(int page) {
        List<Event> events = eventRepository.findAll();

        if (events.isEmpty())
            throw new NoEventsException();
        List<EventWithoutUsersDto> eventDtoList = new ArrayList<>();
        events.forEach(event -> eventDtoList.add(eventMapper.mapEventToEventWithoutUsersDto(event)));
        return eventDtoList;
    }

    @Override
    public EventWithUsersDto getEventById(UUID id) {
        Optional<Event> eventOptional = eventRepository.findById(id);
        if (eventOptional.isEmpty())
            throw new EventNotFoundException("Event does not exist.");
        return eventMapper.mapEventToEventWithUsersDto(eventOptional.get());
    }


    //---------------------------------TESTY----------------------------------------------------------

    @Override
    @Transactional
    public EventWithUsersDto createEvent(EventCreateDto eventCreateDto, String jwtToken) throws RuntimeException {
        if(eventCreateDto.getEventStartDate().getTime() < Calendar.getInstance().getTimeInMillis())
            throw new InvalidEventStartDateException("You can not set event start date from the past.");

        User owner = userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).get();

        Event newEvent = Event.builder()
                .name(eventCreateDto.getName())
                .shortDescription(eventCreateDto.getShortDescription())
                .longDescription(eventCreateDto.getLongDescription())
                .exactAddress(eventCreateDto.getExactAddress())
                .eventStartDate(eventCreateDto.getEventStartDate())
                //.city(cityUtils.resolveCity(eventCreateDto.getCity()))
                .eventStartDate(eventCreateDto.getEventStartDate() != null ? eventCreateDto.getEventStartDate() : null)
                .createDate(new Date(Calendar.getInstance().getTimeInMillis()))
                .build();

        newEvent.setOwner(owner);
        newEvent.setLastUpdate(newEvent.getCreateDate());
        newEvent.setCity(cityUtils.resolveCity(eventCreateDto.getCity()));
        resolveTagsForNewEvent(newEvent, eventCreateDto);

        //notificationService.registerEventTopicInFcm(storedEvent, owner.getFcmAndroidToken());

        return eventMapper.mapEventToEventWithUsersDto(eventRepository.save(newEvent));
    }

    @Override
    @Transactional
    public EventWithUsersDto updateEvent(EventCreateDto updatedEventDto, UUID id, String jwtToken) throws RuntimeException{

        Optional<Event> eventOptional = eventRepository.findById(id);

        if (eventOptional.isEmpty())
            throw new EventNotFoundException("There is no event with that id.");

        Event storedEvent = eventOptional.get();
        if (storedEvent.hadPlace())
            throw new EventAlreadyHadPlaceException("You can't edit event after it had place.");
        if (!storedEvent.getOwner().equals(userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).get()))
            throw new NotEventOwnerException("You are not owner of this event!");

        updateEventFields(storedEvent, updatedEventDto);

        //notificationService.sendEventHasBeenUpdatedNotificationByTopic(storedEvent);
        notificationService.notifyEventAttenders(storedEvent, NotificationType.EVENT_UPDATE, storedEvent.getId(),storedEvent.getOwner().getFullName());

        return eventMapper.mapEventToEventWithUsersDto(eventRepository.save(storedEvent));

    }

    @Override
    @Transactional
    public boolean addAttenderToEvent(UUID id, String jwt) throws RuntimeException {

        Optional<Event> eventOptional = eventRepository.findById(id);

        if (eventOptional.isEmpty())
            throw new EventNotFoundException("There is no event with that id.");

        Event storedEvent = eventOptional.get();
        if (storedEvent.hadPlace())
            throw new EventAlreadyHadPlaceException("Event already had place, you cannot attend old events");
        User attender = userRepository.findByEmail(jwtUtil.extractUsername(jwt)).get();

        if (storedEvent.getOwner().equals(attender))
            throw new EventOwnerAlreadyAttendsEventException("You are an owner of this event, you have to attend.");


        if (storedEvent.getAttendingUsers().contains(attender))
            return false;

        storedEvent.addAttendingUser(attender);
        eventRepository.save(storedEvent);

       // notificationService.registerNewAttenderInEventTopic(storedEvent, attender.getFcmAndroidToken());

        return true;
    }

    @Override
    @Transactional
    public ThreadDto createThreadInEvent(ThreadCreateDto threadCreateDto, UUID eventId, String jwtToken) throws RuntimeException{
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
        notificationService.notifyEventAttenders(storedEvent, NotificationType.EVENT_NEW_THREAD, savedThread.getId(),threadOwner.getFullName());

        return threadMapper.mapThreadToThreadDto(savedThread);
    }

    @Override
    @Transactional
    public ThreadDto updateThreadInEvent(ThreadCreateDto threadCreateDto, UUID eventId, UUID threadId, String jwtToken) throws RuntimeException{
        Optional<Event> eventOptional = eventRepository.findById(eventId);
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
    @Transactional
    public ThreadDto createReplyInThread(ThreadReplayCreateDto threadReplayCreateDto, UUID eventId, UUID threadId, String jwtToken) throws RuntimeException{
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

        if (!threadOptional.get().getOwner().equals(replayingUser))
            notificationService.notifyThreadOwner(threadOptional.get() ,replayingUser.getFullName());

        return threadMapper.mapThreadToThreadDto(threadOptional.get());
    }

    @Override
    @Transactional
    public ThreadDto updateThreadReplyInEvent(ThreadReplayCreateDto threadReplayUpdateDto, UUID eventId, UUID threadId, UUID threadReplyId, String jwtToken) throws RuntimeException{
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
        if (threadReplyOptional.isEmpty())
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

    @Override
    @Transactional
    public List<EventWithoutUsersDto> searchEvents(List<String> words, List<String> tags, String cityName) {
        if((words == null || words.isEmpty())  &&  (tags == null || tags.isEmpty()))
            throw new NoSearchParametersPresentException("You have not provide any search parameters.");

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
            throw  new NoSearchResultException("No event have matched your search parameters.");

        List<EventWithoutUsersDto> foundEventsDtoList = new ArrayList<>();
        foundEvents.forEach(event-> foundEventsDtoList.add(eventMapper.mapEventToEventWithoutUsersDto(event)));

        return foundEventsDtoList;
    }



    @Override
    @Transactional
    public EventWithUsersDto uploadFileToEvent(MultipartFile uploadedFile, UUID eventId, String jwtToken) throws RuntimeException, IOException {
        if (uploadedFile.isEmpty())
            throw new EmptyUploadedFileException("Uploaded file is empty.");
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException("There is no event with that id."));
        User user = userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).get();
        if (!event.isUserAttending(user))
            throw new NotAttenderException("You are not attending this event.");

        if (!isFileCorrect(uploadedFile))
            throw new FileTypeNotAllowedException("You can not upload this type of files.");


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

        return eventMapper.mapEventToEventWithUsersDto(event);
    }

    @Override
    public File getFile(UUID id, UUID eventId, String jwtToken) {
        File fileToBeServed = fileRepository.findById(id).orElseThrow(() -> new FileNotFoundException("There is no file with that id"));
        if(!fileToBeServed.getEvent().isUserAttending(userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).get()))
            throw new NotAttenderException("You are not attending this event");
        return fileToBeServed;
    }

    public boolean removeAttenderFromEvent(Long eventId, String jwtToken){
        return true;
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

}
