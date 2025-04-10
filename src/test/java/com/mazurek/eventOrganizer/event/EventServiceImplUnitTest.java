package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.city.CityUtils;
import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.dto.EventDto;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotAttenderException;
import com.mazurek.eventOrganizer.exception.event.NotEventOwnerException;
import com.mazurek.eventOrganizer.exception.thread.*;
import com.mazurek.eventOrganizer.file.FileRepository;
import com.mazurek.eventOrganizer.jwt.JwtUtil;
import com.mazurek.eventOrganizer.notification.NotificationService;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.tag.TagRepository;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.thread.ThreadReply;
import com.mazurek.eventOrganizer.thread.ThreadReplyRepository;
import com.mazurek.eventOrganizer.thread.ThreadRepository;
import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadReplayCreateDto;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.apache.tika.Tika;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplUnitTest {

    private final UUID FIRST_USER_ID = UUID.randomUUID();
    private final UUID SECOND_USER_ID = UUID.randomUUID();
    private final UUID CITY_RZESZOW_ID = UUID.randomUUID();
    private final UUID CITY_KRAKOW_ID = UUID.randomUUID();
    private final UUID EVENT_ID = UUID.randomUUID();
    private final UUID TAG_SPRING_ID = UUID.randomUUID();
    private final UUID TAG_JAVA_ID = UUID.randomUUID();
    private final UUID TAG_WITAM_ID = UUID.randomUUID();
    private final UUID TAG_ZEGNAM_ID = UUID.randomUUID();
    private final UUID THREAD_ID = UUID.randomUUID();
    private final UUID THREAD_REPLY_ID = UUID.randomUUID();


    public static final String JWT_STRING = "randomStringForJwt";
    private static final String REPLY_CONTENT = "This is first replay in thread";
    public static final String EVENT_OWNER_EMAIL = "example@dot.com";
    private static final String EVENT_OWNER_FIRST_NAME = "Andrew";
    private static final String EVENT_OWNER_LAST_NAME = "Golota";
    private static final String SECOND_USER_FIRST_NAME = "Andrzej";
    private static final String SECOND_USER_LAST_NAME = "Wesoly";
    public static final String SECOND_USER_EMAIL = "notExample@dot.com";
    private static final String PASSWORD_DEFAULT = "password";
    private static final String EVENT_SHORT_DESCRIPTION = "short description should be short";
    private static final String EVENT_LONG_DESCRIPTION = "long description can be quite long, and it Sho◘uld be. maybe i should put Lorem Ipsum here.";
    private static final String EVENT_NAME = "First Event";
    private static final String EVENT_EXACT_ADDRESS = "ul. Dąbrowskiego 3";
    private static final String FIRST_THREAD_NAME = "First thread ever";
    private static final String FIRST_THREAD_CONTENT = "First thread content for testing purpose. It have to be containing several words.";
    public static final String EVENT_CREATE_DTO_CITY = "Rzeszow";

    private EventService eventService;

    @Mock
    private EventRepository eventRepository;
    @Mock
    private CityRepository cityRepository;
    @Mock
    private CityUtils cityUtils;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private ThreadRepository threadRepository;
    @Mock
    private ThreadReplyRepository threadReplyRepository;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private JwtUtil jwtUtil;
    private BCryptPasswordEncoder passwordEncoder = Mockito.spy(new BCryptPasswordEncoder());
    private final Tika tikaFileTypeDetector = new Tika();


    private User eventOwner;
    private User secondUser;
    private Event event;
    private Optional<Event> eventOptional;
    private Optional<User> eventOwnerOptional;
    private Optional<User> secondUserOptional;
    private Optional<Thread> threadOptional;
    private Optional<ThreadReply> threadReplyOptional;
    private Tag tagJava;
    private Tag tagSpring;
    private Tag tagWitam;
    private Tag tagZegnam;

    private City cityKrakow;
    private City cityRzeszow;

    private EventCreateDto eventCreateDto;
    private EventCreateDto updatedEventDto;
    private ThreadCreateDto threadCreateDto;
    private ThreadReplayCreateDto threadReplayCreateDto;

    private Thread thread;
    private ThreadReply threadReply;


    /*
     ********************************************************************************************************************
     *                                       CREATING EVENT TESTS
     ********************************************************************************************************************
     */
    @Nested
    @DisplayName("Create event tests")
    class CreateEventTest {
        @BeforeEach
        void setUp() {
            eventService = new EventServiceImpl(eventRepository, cityRepository, tagRepository, userRepository, threadRepository, threadReplyRepository, fileRepository, notificationService, cityUtils, jwtUtil, tikaFileTypeDetector);

            tagJava = Tag.builder()
                    .name("java")
                    .id(TAG_JAVA_ID)
                    .events(new HashSet<>())
                    .build();

            tagSpring = Tag.builder()
                    .name("spring")
                    .id(TAG_SPRING_ID)
                    .events(new HashSet<>())
                    .build();

            cityRzeszow = City.builder()
                    .id(CITY_RZESZOW_ID)
                    .name("Rzeszow")
                    .events(new ArrayList<>())
                    .residents(new HashSet<>())
                    .build();

            eventOwner = User.builder()
                    .id(FIRST_USER_ID)
                    .email("example@dot.com")
                    .role(Role.USER)
                    .firstName(EVENT_OWNER_FIRST_NAME)
                    .lastName(EVENT_OWNER_LAST_NAME)
                    .homeCity(cityRzeszow)
                    .attendingEvents(new ArrayList<>())
                    .userEvents(new ArrayList<>())
                    .password(passwordEncoder.encode(PASSWORD_DEFAULT))
                    .lastCredentialsChangeTime(LocalDateTime.now())
                    .build();

            event = Event.builder()
                    .id(EVENT_ID)
                    .name(EVENT_NAME)
                    .shortDescription(EVENT_SHORT_DESCRIPTION)
                    .longDescription(EVENT_LONG_DESCRIPTION)
                    .createDate(ZonedDateTime.now())
                    .tags(new HashSet<>())
                    .attendingUsers(new HashSet<>())
                    .threads(new HashSet<>())
                    .city(cityRzeszow)
                    .exactAddress(EVENT_EXACT_ADDRESS)
                    .build();

            eventOptional = Optional.of(event);

            eventCreateDto = EventCreateDto.builder()
                    .name(EVENT_NAME)
                    .shortDescription(EVENT_SHORT_DESCRIPTION)
                    .longDescription(EVENT_LONG_DESCRIPTION)
                    .tags(new ArrayList<>())
                    .city(EVENT_CREATE_DTO_CITY)
                    .exactAddress(EVENT_EXACT_ADDRESS)
                    .eventStartDate(ZonedDateTime.now().plusDays(7))
                    .build();

            eventCreateDto.getTags().add("java");
            eventCreateDto.getTags().add("spring");


            event.setLastUpdate(event.getCreateDate());
            event.setOwner(eventOwner);
            event.addAttendingUser(eventOwner);

            event.addTag(tagJava);
            event.addTag(tagSpring);

        }

        @Test
        @DisplayName("When creating event should create empty set for tags if tag list in dto is empty")
        public void whenCreatingEventShouldCreateEmptySetForTagsIfTagListInDtoIsEmpty() {
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(cityUtils.resolveCity(EVENT_CREATE_DTO_CITY)).thenReturn(cityRzeszow);
            when(eventRepository.save(any())).thenReturn(event);

            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));

            ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

            eventCreateDto.getTags().clear();

            eventService.createEvent(eventCreateDto, JWT_STRING);

            verify(eventRepository, times(1)).save(eventArgumentCaptor.capture());
            Event capturedEvent = eventArgumentCaptor.getValue();
            assertNotNull(capturedEvent.getTags());
            assertNotNull(capturedEvent.getAttendingUsers());
            assertTrue(capturedEvent.getTags().isEmpty());
            assertTrue(capturedEvent.getAttendingUsers().isEmpty());
        }

        @Test
        @DisplayName("When creating event should create empty set for attempting users")
        public void whenCreatingEventShouldCreateEmptySetForAttemptingUsers() {
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(cityUtils.resolveCity(EVENT_CREATE_DTO_CITY)).thenReturn(cityRzeszow);
            when(eventRepository.save(any())).thenReturn(event);
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(eventOwner));


            ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
            eventCreateDto.getTags().clear();
            eventService.createEvent(eventCreateDto, JWT_STRING);

            verify(eventRepository, times(1)).save(eventArgumentCaptor.capture());
            Event capturedEvent = eventArgumentCaptor.getValue();
            assertNotNull(capturedEvent.getAttendingUsers());
            assertTrue(capturedEvent.getAttendingUsers().isEmpty());
        }


        @Test
        @DisplayName("When creating event should pass event object to repository with all data")
        public void whenCreatingEventShouldPassEventObjectToRepositoryWithAllData() {
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(eventRepository.save(any())).thenReturn(event);
            when(cityUtils.resolveCity(EVENT_CREATE_DTO_CITY)).thenReturn(cityRzeszow);

            when(tagRepository.findByIgnoreCaseName("java")).thenReturn(Optional.of(tagJava));
            when(tagRepository.findByIgnoreCaseName("spring")).thenReturn(Optional.of(tagSpring));

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(eventOwner));


            ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

            eventService.createEvent(eventCreateDto, JWT_STRING);

            verify(eventRepository, times(1)).save(eventArgumentCaptor.capture());

            Event capturedEvent = eventArgumentCaptor.getValue();
            assertEquals(eventCreateDto.getName(), capturedEvent.getName());
            assertEquals(eventCreateDto.getShortDescription(), capturedEvent.getShortDescription());
            assertEquals(eventCreateDto.getLongDescription(), capturedEvent.getLongDescription());
            assertEquals(eventCreateDto.getCity(), capturedEvent.getCity().getName());
            assertEquals(eventCreateDto.getExactAddress(), capturedEvent.getExactAddress());

            ArrayList<String> tagNamesFromCapturedEvent = new ArrayList<>();
            capturedEvent.getTags().forEach(tag -> tagNamesFromCapturedEvent.add(tag.getName()));
            eventCreateDto.getTags().forEach(tagName -> assertTrue(tagNamesFromCapturedEvent.contains(tagName)));

        }

        @Test
        @DisplayName("When creating event should create empty set for threads")
        public void whenCreatingEventShouldCreateEmptySetForThreads() {
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(cityUtils.resolveCity(EVENT_CREATE_DTO_CITY)).thenReturn(cityRzeszow);
            when(eventRepository.save(any())).thenReturn(event);
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(eventOwner));


            ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
            eventCreateDto.getTags().clear();
            eventService.createEvent(eventCreateDto, JWT_STRING);

            verify(eventRepository, times(1)).save(eventArgumentCaptor.capture());
            Event capturedEvent = eventArgumentCaptor.getValue();
            assertNotNull(capturedEvent.getThreads());
            assertTrue(capturedEvent.getThreads().isEmpty());
        }

    }

    /*
     ********************************************************************************************************************
     *                                       GETTING EVENT BY ID TESTS
     ********************************************************************************************************************
     */

    @Nested
    @DisplayName("Get event by id test")
    class GettingEventByIdTests {

        @BeforeEach
        void setUp() {
            eventService = new EventServiceImpl(eventRepository, cityRepository, tagRepository, userRepository, threadRepository, threadReplyRepository, fileRepository, notificationService, cityUtils, jwtUtil, tikaFileTypeDetector);

            cityRzeszow = City.builder()
                    .id(CITY_RZESZOW_ID)
                    .name("Rzeszow")
                    .residents(new HashSet<>())
                    .events(new ArrayList<>())
                    .build();

            tagJava = Tag.builder()
                    .name("java")
                    .id(TAG_JAVA_ID)
                    .events(new HashSet<>())
                    .build();

            tagSpring = Tag.builder()
                    .name("spring")
                    .id(TAG_SPRING_ID)
                    .events(new HashSet<>())
                    .build();

            event = Event.builder()
                    .id(EVENT_ID)
                    .name(EVENT_NAME)
                    .shortDescription(EVENT_SHORT_DESCRIPTION)
                    .longDescription(EVENT_LONG_DESCRIPTION)
                    .createDate(ZonedDateTime.now())
                    .tags(new HashSet<>())
                    .attendingUsers(new HashSet<>())
                    .threads(new HashSet<>())
                    .city(cityRzeszow)
                    .exactAddress(EVENT_EXACT_ADDRESS)
                    .build();

            eventOptional = Optional.of(event);


            eventOwner = User.builder()
                    .id(FIRST_USER_ID)
                    .email("example@dot.com")
                    .role(Role.USER)
                    .firstName(EVENT_OWNER_FIRST_NAME)
                    .lastName(EVENT_OWNER_LAST_NAME)
                    .homeCity(cityRzeszow)
                    .attendingEvents(new ArrayList<>())
                    .userEvents(new ArrayList<>())
                    .password(passwordEncoder.encode(PASSWORD_DEFAULT))
                    .lastCredentialsChangeTime(LocalDateTime.now())
                    .build();

            event.setLastUpdate(eventOptional.get().getCreateDate());
            event.setOwner(eventOwner);

            event.addTag(tagJava);
            event.addTag(tagSpring);
            cityRzeszow.addEvent(event);

        }

        @Test
        @DisplayName("When getting event by id should run query once")
        public void whenGettingEventByIdShouldRunQueryOnce() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);

            eventService.getEventById(EVENT_ID);

            verify(eventRepository, times(1)).findById(EVENT_ID);
        }

        @Test
        @DisplayName("When getting event by id should throw event not found exception when there is no")
        public void whenGettingEventByIdShouldThrowEvenNotFoundException() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

            assertThrows(EventNotFoundException.class, () -> eventService.getEventById(EVENT_ID));

            verify(eventRepository, times(1)).findById(EVENT_ID);
        }

        @Test
        @DisplayName("When getting event by id should map required properties it to eventDto")
        public void whenGettingEventByIdShouldMapItToEventDto() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);

            EventDto output = eventService.getEventById(EVENT_ID);

            assertEquals(event.getId(), output.getId());
            assertEquals(event.getName(), output.getName());
            assertEquals(event.getShortDescription(), output.getShortDescription());
            assertEquals(event.getLongDescription(), output.getLongDescription());
            assertEquals(event.getAttendingUsers().size(), output.getAttendingUsers().size());

        }

    }


    /*
     ********************************************************************************************************************
     *                                       UPDATING EVENT TESTS
     ********************************************************************************************************************
     */

    @Nested
    @DisplayName("Update event tests")
    class UpdatingEventTest {

        @BeforeEach
        void setUp() {
            eventService = new EventServiceImpl(eventRepository, cityRepository, tagRepository, userRepository, threadRepository, threadReplyRepository, fileRepository, notificationService, cityUtils, jwtUtil, tikaFileTypeDetector);

            tagJava = Tag.builder()
                    .name("java")
                    .id(TAG_JAVA_ID)
                    .events(new HashSet<>())
                    .build();
            tagSpring = Tag.builder()
                    .name("spring")
                    .id(TAG_SPRING_ID)
                    .events(new HashSet<>())
                    .build();

            cityRzeszow = City.builder()
                    .id(CITY_RZESZOW_ID)
                    .name("Rzeszow")
                    .events(new ArrayList<>())
                    .residents(new HashSet<>())
                    .build();

            eventOwner = User.builder()
                    .id(FIRST_USER_ID)
                    .email(EVENT_OWNER_EMAIL)
                    .role(Role.USER)
                    .firstName(EVENT_OWNER_FIRST_NAME)
                    .lastName(EVENT_OWNER_LAST_NAME)
                    .homeCity(cityRzeszow)
                    .attendingEvents(new ArrayList<>())
                    .userEvents(new ArrayList<>())
                    .password(passwordEncoder.encode(PASSWORD_DEFAULT))
                    .lastCredentialsChangeTime(LocalDateTime.now())
                    .build();

            secondUser = User.builder()
                    .id(SECOND_USER_ID)
                    .role(Role.USER)
                    .firstName(SECOND_USER_FIRST_NAME)
                    .lastName(SECOND_USER_LAST_NAME)
                    .homeCity(cityRzeszow)
                    .email(SECOND_USER_EMAIL)
                    .userEvents(new ArrayList<>())
                    .attendingEvents(new ArrayList<>())
                    .build();

            secondUserOptional = Optional.of(secondUser);

            event = Event.builder()
                    .id(EVENT_ID)
                    .name(EVENT_NAME)
                    .shortDescription(EVENT_SHORT_DESCRIPTION)
                    .longDescription(EVENT_LONG_DESCRIPTION)
                    .createDate(ZonedDateTime.now())
                    .eventStartDate(ZonedDateTime.now().plusDays(7))
                    .tags(new HashSet<>())
                    .attendingUsers(new HashSet<>())
                    .threads(new HashSet<>())
                    .city(cityRzeszow)
                    .exactAddress(EVENT_EXACT_ADDRESS)
                    .build();

            eventOptional = Optional.of(event);

            cityRzeszow.addEvent(event);
            event.setLastUpdate(event.getCreateDate());
            event.setOwner(eventOwner);
            event.addAttendingUser(eventOwner);

            event.addTag(tagJava);
            event.addTag(tagSpring);

            updatedEventDto = EventCreateDto.builder()
                    .name("changed name")
                    .shortDescription("changed short description")
                    .longDescription("changed long description")
                    .city("Krakow")
                    .exactAddress("changed exact address")
                    .tags(new ArrayList<>())
                    .eventStartDate(ZonedDateTime.now().plusDays(10))
                    .build();

            updatedEventDto.getTags().add("witam");
            updatedEventDto.getTags().add("zegnam");

            tagWitam = Tag.builder()
                    .id(TAG_WITAM_ID)
                    .name("witam")
                    .events(new HashSet<>())
                    .build();
            tagZegnam = Tag.builder()
                    .id(TAG_ZEGNAM_ID)
                    .name("zegnam")
                    .events(new HashSet<>())
                    .build();

            cityKrakow = City.builder()
                    .id(CITY_KRAKOW_ID)
                    .name("Krakow")
                    .events(new ArrayList<>())
                    .residents(new HashSet<>())
                    .build();
        }


        @DisplayName("When updating event should try to load event from database")
        @Test
        public void whenUpdatingEventShouldTryToLoadEventFromDatabase() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(cityUtils.resolveCity("Krakow")).thenReturn(cityKrakow);
            when(tagRepository.findByIgnoreCaseName("witam")).thenReturn(Optional.of(tagWitam));
            when(tagRepository.findByIgnoreCaseName("zegnam")).thenReturn(Optional.of(tagZegnam));
            when(eventRepository.save(event)).thenReturn(event);

            eventService.updateEvent(updatedEventDto, EVENT_ID, JWT_STRING);

            verify(eventRepository, times(1)).findById(EVENT_ID);
        }

        @Test
        @DisplayName("When updating event should throw event not found exception if event does not exist")
        public void whenUpdatingEventShouldThrowEventNotFoundExceptionIfEventDoesNotExist() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

            assertThrows(EventNotFoundException.class, () -> eventService.updateEvent(updatedEventDto, EVENT_ID, JWT_STRING));

        }

        @Test
        @DisplayName("When updating event should extract user email from jwt for ownership check")
        public void whenUpdatingEventShouldExtractUserEmailFromJwtForOwnershipCheck() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(tagRepository.findByIgnoreCaseName("witam")).thenReturn(Optional.of(tagWitam));
            when(tagRepository.findByIgnoreCaseName("zegnam")).thenReturn(Optional.of(tagZegnam));
            when(cityUtils.resolveCity("Krakow")).thenReturn(cityKrakow);
            when(eventRepository.save(event)).thenReturn(event);

            eventService.updateEvent(updatedEventDto, EVENT_ID, JWT_STRING);

            verify(jwtUtil, times(1)).extractUsername(JWT_STRING);

        }

        @Test
        @DisplayName("When updating event should find user with extracted email")
        public void whenUpdatingEventShouldFindUserWithExtractedEmail() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(tagRepository.findByIgnoreCaseName("witam")).thenReturn(Optional.of(tagWitam));
            when(tagRepository.findByIgnoreCaseName("zegnam")).thenReturn(Optional.of(tagZegnam));
            when(cityUtils.resolveCity("Krakow")).thenReturn(cityKrakow);
            when(eventRepository.save(event)).thenReturn(event);

            eventService.updateEvent(updatedEventDto, EVENT_ID, JWT_STRING);
            verify(userRepository, times(1)).findByEmail(EVENT_OWNER_EMAIL);

        }

        @Test
        @DisplayName("When updating event should check if is it owner performing update")
        public void whenUpdatingEventShouldCheckIfIsItOwnerPerformingUpdate() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

            assertThrows(NotEventOwnerException.class, () -> eventService.updateEvent(updatedEventDto, EVENT_ID, JWT_STRING));

        }

        @Test
        @DisplayName("When updating event should throw wrong event owner exception if user try to modify not his event")
        public void whenUpdatingEventShouldThrowWrongEventOwnerExceptionIfUserTryToModifyNotHisEvent() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(Optional.of(secondUser));

            assertThrows(NotEventOwnerException.class, () -> eventService.updateEvent(updatedEventDto, EVENT_ID, JWT_STRING));
        }

        @Test
        @DisplayName("When updating Event should update basic fields of event")
        public void whenUpdatingEventShouldUpdateFieldsOfEvent() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(anyString())).thenReturn(eventOwner.getEmail());
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(eventOwner));
            when(tagRepository.findByIgnoreCaseName("witam")).thenReturn(Optional.of(tagWitam));
            when(tagRepository.findByIgnoreCaseName("zegnam")).thenReturn(Optional.of(tagZegnam));
            when(cityUtils.resolveCity("Krakow")).thenReturn(cityKrakow);
            when(eventRepository.save(event)).thenReturn(event);

            Event eventToBeSaved = eventOptional.get();

            eventService.updateEvent(updatedEventDto, EVENT_ID, JWT_STRING);


            assertEquals(updatedEventDto.getName(), eventToBeSaved.getName());
            assertEquals(updatedEventDto.getShortDescription(), eventToBeSaved.getShortDescription());
            assertEquals(updatedEventDto.getLongDescription(), eventToBeSaved.getLongDescription());
            assertEquals(updatedEventDto.getCity(), eventToBeSaved.getCity().getName());
            assertEquals(updatedEventDto.getExactAddress(), eventToBeSaved.getExactAddress());
            assertEquals(updatedEventDto.getEventStartDate(), eventToBeSaved.getEventStartDate());
        }

        @Test
        @DisplayName("When updating event should remove tags not appearing in dto from event list of tags")
        public void whenUpdatingEventShouldRemoveTagsNotAppearingInDtoFromEventListOfTags() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(tagRepository.findByIgnoreCaseName("witam")).thenReturn(Optional.of(tagWitam));
            when(tagRepository.findByIgnoreCaseName("zegnam")).thenReturn(Optional.of(tagZegnam));
            when(cityUtils.resolveCity("Krakow")).thenReturn(cityKrakow);
            when(eventRepository.save(event)).thenReturn(event);

            ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

            eventService.updateEvent(updatedEventDto, EVENT_ID, JWT_STRING);
            verify(eventRepository).save(eventArgumentCaptor.capture());

            Event eventToBeSaved = eventArgumentCaptor.getValue();

            assertFalse(eventToBeSaved.getTags().contains(tagJava));
            assertFalse(tagJava.getEvents().contains(eventOptional.get()));

            assertFalse(eventToBeSaved.getTags().contains(tagSpring));
            assertFalse(tagSpring.getEvents().contains(eventOptional.get()));

            assertTrue(eventToBeSaved.getTags().contains(tagZegnam));
            assertTrue(tagZegnam.getEvents().contains(eventOptional.get()));

            assertTrue(eventToBeSaved.getTags().contains(tagWitam));
            assertTrue(tagWitam.getEvents().contains(eventOptional.get()));


        }

        @Test
        @DisplayName("When updating event should not remove tags appearing in dto from event list of tags")
        public void whenUpdatingEventShouldNotRemoveTagsAppearingInDtoFromEventListOfTags() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(eventRepository.save(event)).thenReturn(event);

            updatedEventDto.getTags().removeIf(tag -> tag.equals("witam"));
            updatedEventDto.getTags().add("java");

            when(tagRepository.findByIgnoreCaseName("zegnam")).thenReturn(Optional.of(tagZegnam));
            when(cityUtils.resolveCity("Krakow")).thenReturn(cityKrakow);

            ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

            eventService.updateEvent(updatedEventDto, EVENT_ID, JWT_STRING);
            verify(eventRepository).save(eventArgumentCaptor.capture());

            Event eventToBeSaved = eventArgumentCaptor.getValue();

            assertTrue(eventToBeSaved.getTags().contains(tagJava));
            assertTrue(tagJava.getEvents().contains(event));

            assertFalse(eventToBeSaved.getTags().contains(tagSpring));
            assertFalse(tagSpring.getEvents().contains(event));

            assertTrue(eventToBeSaved.getTags().contains(tagZegnam));
            assertTrue(tagZegnam.getEvents().contains(event));

            assertFalse(eventToBeSaved.getTags().contains(tagWitam));
            assertFalse(tagWitam.getEvents().contains(event));


        }

        @Test
        @DisplayName("When updating event should remove all tags if dto tag list is empty from event list of tags")
        public void whenUpdatingEventShouldRemoveAllTagsIfDtoTagListIsEmptyFromEventListOfTags() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(cityUtils.resolveCity("Krakow")).thenReturn(cityKrakow);
            when(eventRepository.save(event)).thenReturn(event);

            updatedEventDto.getTags().clear();

            ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

            eventService.updateEvent(updatedEventDto, EVENT_ID, JWT_STRING);

            verify(eventRepository).save(eventArgumentCaptor.capture());

            Event eventToBeSaved = eventArgumentCaptor.getValue();

            assertFalse(eventToBeSaved.getTags().contains(tagJava));
            assertFalse(tagJava.getEvents().contains(eventOptional.get()));

            assertFalse(eventToBeSaved.getTags().contains(tagSpring));
            assertFalse(tagSpring.getEvents().contains(eventOptional.get()));

            assertFalse(eventToBeSaved.getTags().contains(tagZegnam));
            assertFalse(tagZegnam.getEvents().contains(eventOptional.get()));

            assertFalse(eventToBeSaved.getTags().contains(tagWitam));
            assertFalse(tagWitam.getEvents().contains(eventOptional.get()));

            assertTrue(eventToBeSaved.getTags().isEmpty());

        }

        @Test
        @DisplayName("When updating event should add lacking tags from dto tag list to event tag list")
        public void whenUpdatingEventShouldAddLackingTagsFromDtoTagListToEventTagList() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(tagRepository.findByIgnoreCaseName("witam")).thenReturn(Optional.of(tagWitam));
            when(tagRepository.findByIgnoreCaseName("zegnam")).thenReturn(Optional.of(tagZegnam));
            when(cityUtils.resolveCity("Krakow")).thenReturn(cityKrakow);
            when(eventRepository.save(event)).thenReturn(event);

            updatedEventDto.getTags().add("java");
            updatedEventDto.getTags().add("spring");

            eventService.updateEvent(updatedEventDto, EVENT_ID, JWT_STRING);

            Event eventToBeSaved = eventOptional.get();

            assertTrue(eventToBeSaved.getTags().contains(tagJava));
            assertTrue(tagJava.getEvents().contains(eventOptional.get()));

            assertTrue(eventToBeSaved.getTags().contains(tagSpring));
            assertTrue(tagSpring.getEvents().contains(eventOptional.get()));

            assertTrue(eventToBeSaved.getTags().contains(tagZegnam));
            assertTrue(tagZegnam.getEvents().contains(eventOptional.get()));

            assertTrue(eventToBeSaved.getTags().contains(tagWitam));
            assertTrue(tagWitam.getEvents().contains(eventOptional.get()));

        }

        @Test
        @DisplayName("When updating event should save changes to database")
        public void whenUpdatingEventShouldSaveChangesToDatabase() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));

            when(tagRepository.findByIgnoreCaseName("witam")).thenReturn(Optional.of(tagWitam));
            when(tagRepository.findByIgnoreCaseName("zegnam")).thenReturn(Optional.of(tagZegnam));
            when(cityUtils.resolveCity("Krakow")).thenReturn(cityKrakow);
            when(eventRepository.save(event)).thenReturn(event);

            eventService.updateEvent(updatedEventDto, EVENT_ID, JWT_STRING);


            verify(eventRepository, times(1)).save(event);

        }

    }

    /*
     ********************************************************************************************************************
     *                                       ADDING ATTENDER TO EVENT TESTS
     ********************************************************************************************************************
     */

    @Nested
    @DisplayName("Adding attender to event tests")
    class AddingAttenderToEventTests {
        @BeforeEach
        void setUp() {
            eventService = new EventServiceImpl(eventRepository, cityRepository, tagRepository, userRepository, threadRepository, threadReplyRepository, fileRepository, notificationService, cityUtils, jwtUtil, tikaFileTypeDetector);

            tagJava = Tag.builder()
                    .name("java")
                    .id(TAG_JAVA_ID)
                    .events(new HashSet<>())
                    .build();
            tagSpring = Tag.builder()
                    .name("spring")
                    .id(TAG_SPRING_ID)
                    .events(new HashSet<>())
                    .build();

            cityRzeszow = City.builder()
                    .id(CITY_RZESZOW_ID)
                    .name("Rzeszow")
                    .events(new ArrayList<>())
                    .residents(new HashSet<>())
                    .build();

            eventOwner = User.builder()
                    .id(FIRST_USER_ID)
                    .email("example@dot.com")
                    .role(Role.USER)
                    .firstName(EVENT_OWNER_FIRST_NAME)
                    .lastName(EVENT_OWNER_LAST_NAME)
                    .homeCity(cityRzeszow)
                    .attendingEvents(new ArrayList<>())
                    .userEvents(new ArrayList<>())
                    .password(passwordEncoder.encode(PASSWORD_DEFAULT))
                    .lastCredentialsChangeTime(LocalDateTime.now())
                    .build();

            secondUser = User.builder()
                    .id(SECOND_USER_ID)
                    .role(Role.USER)
                    .firstName(SECOND_USER_FIRST_NAME)
                    .lastName(SECOND_USER_LAST_NAME)
                    .homeCity(cityRzeszow)
                    .email(SECOND_USER_EMAIL)
                    .userEvents(new ArrayList<>())
                    .attendingEvents(new ArrayList<>())
                    .build();
            secondUserOptional = Optional.of(secondUser);

            eventOptional = Optional.of(Event.builder()
                    .id(EVENT_ID)
                    .name(EVENT_NAME)
                    .shortDescription(EVENT_SHORT_DESCRIPTION)
                    .longDescription(EVENT_LONG_DESCRIPTION)
                    .createDate(ZonedDateTime.now())
                    .eventStartDate(ZonedDateTime.now().plusDays(7))
                    .tags(new HashSet<>())
                    .attendingUsers(new HashSet<>())
                    .threads(new HashSet<>())
                    .city(cityRzeszow)
                    .exactAddress(EVENT_EXACT_ADDRESS)
                    .build());

            eventOptional.get().setLastUpdate(eventOptional.get().getCreateDate());
            eventOptional.get().setOwner(eventOwner);
            eventOptional.get().addAttendingUser(eventOwner);

            eventOptional.get().addTag(tagJava);
            eventOptional.get().addTag(tagSpring);


        }

        @Test
        @DisplayName("When adding attender should try to load event from database")
        public void whenAddingAttenderShouldTryToLoadEventFromDatabase() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(Optional.of(secondUser));

            eventService.addAttenderToEvent(EVENT_ID, JWT_STRING);

            verify(eventRepository, times(1)).findById(EVENT_ID);

        }

        @Test
        @DisplayName("When adding attender should check if event optional is empty")
        public void whenAddingAttenderShouldCheckIfEventIsPresent() {
            Optional<Event> eventOptionalSpy = Mockito.spy(eventOptional);
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptionalSpy);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(Optional.of(secondUser));

            eventService.addAttenderToEvent(EVENT_ID, JWT_STRING);

            verify(eventOptionalSpy, times(1)).isEmpty();
        }

        @Test
        @DisplayName("When adding attender should throw EventNotFound exception if event optional is empty")
        public void whenAddingAttenderShouldThrowEventNotFoundExceptionIfEventOptionalIsEmpty() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

            assertThrows(EventNotFoundException.class, () -> eventService.addAttenderToEvent(EVENT_ID, JWT_STRING));
        }

        @Test
        @DisplayName("When adding attender should extract username from jwt")
        public void whenAddingAttenderShouldExtractUsernameFromJwt() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);

            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(Optional.of(secondUser));

            eventService.addAttenderToEvent(EVENT_ID, JWT_STRING);

            verify(jwtUtil, times(1)).extractUsername(JWT_STRING);

        }

        @Test
        @DisplayName("When adding attender should load user from database with extracted from jwt username")
        public void whenAddingAttenderShouldLoadUserFromDatabaseWithExtractedFromJwtUsername() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(Optional.of(secondUser));

            eventService.addAttenderToEvent(EVENT_ID, JWT_STRING);

        }

        @Test
        @DisplayName("When adding attender should retrieve user from optional object")
        public void whenAddingAttenderShouldRetrieveUserFromOptionalObject() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            Optional<User> secondUserOptionalSpy = Mockito.spy(Optional.of(secondUser));
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptionalSpy);

            eventService.addAttenderToEvent(EVENT_ID, JWT_STRING);

            verify(secondUserOptionalSpy, times(1)).get();

        }

        @Test
        @DisplayName("When adding attender should retrieve event from optional object")
        public void whenAddingAttenderShouldRetrieveEventFromOptionalObject() {
            Optional<Event> eventOptionalSpy = Mockito.spy(eventOptional);
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptionalSpy);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(Optional.of(secondUser));

            eventService.addAttenderToEvent(EVENT_ID, JWT_STRING);

            verify(eventOptionalSpy, times(1)).get();

        }

        @Test
        @DisplayName("When adding attender should add retrieved user to event attender list")
        public void whenAddingAttenderShouldAddRetrievedUserToEventAttenderList() {
            Event eventSpy = Mockito.spy(eventOptional.get());
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(eventSpy));
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(Optional.of(secondUser));

            eventService.addAttenderToEvent(EVENT_ID, JWT_STRING);

            verify(eventSpy, times(1)).addAttendingUser(secondUser);

            assertTrue(secondUser.getAttendingEvents().contains(eventSpy));
            assertTrue(eventSpy.getAttendingUsers().contains(secondUser));

        }

        @Test
        @DisplayName("When adding attender should save changes to database")
        public void whenAddingAttenderShouldSaveChangesToDatabase() {
            Event eventSpy = Mockito.spy(eventOptional.get());
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(eventSpy));
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(Optional.of(secondUser));

            eventService.addAttenderToEvent(EVENT_ID, JWT_STRING);

            verify(eventRepository, times(1)).save(any(Event.class));

        }

    }

    /*
     ********************************************************************************************************************
     *                                       CREATE THREAD IN EVENT
     ********************************************************************************************************************
     */
    @Nested
    @DisplayName("Thread create in event tests:")
    class ThreadCreateTests {
        @BeforeEach
        void setUp() {
            eventService = new EventServiceImpl(eventRepository, cityRepository, tagRepository, userRepository, threadRepository, threadReplyRepository, fileRepository, notificationService, cityUtils, jwtUtil, tikaFileTypeDetector);

            tagJava = Tag.builder()
                    .name("java")
                    .id(TAG_JAVA_ID)
                    .events(new HashSet<>())
                    .build();
            tagSpring = Tag.builder()
                    .name("spring")
                    .id(TAG_SPRING_ID)
                    .events(new HashSet<>())
                    .build();

            cityRzeszow = City.builder()
                    .id(CITY_RZESZOW_ID)
                    .name("Rzeszow")
                    .events(new ArrayList<>())
                    .residents(new HashSet<>())
                    .build();

            eventOwner = User.builder()
                    .id(FIRST_USER_ID)
                    .email("example@dot.com")
                    .role(Role.USER)
                    .firstName(EVENT_OWNER_FIRST_NAME)
                    .lastName(EVENT_OWNER_LAST_NAME)
                    .homeCity(cityRzeszow)
                    .attendingEvents(new ArrayList<>())
                    .userEvents(new ArrayList<>())
                    .password(passwordEncoder.encode(PASSWORD_DEFAULT))
                    .lastCredentialsChangeTime(LocalDateTime.now())
                    .build();

            secondUser = User.builder()
                    .id(SECOND_USER_ID)
                    .role(Role.USER)
                    .firstName(SECOND_USER_FIRST_NAME)
                    .lastName(SECOND_USER_LAST_NAME)
                    .homeCity(cityRzeszow)
                    .email(SECOND_USER_EMAIL)
                    .userEvents(new ArrayList<>())
                    .attendingEvents(new ArrayList<>())
                    .build();
            secondUserOptional = Optional.of(secondUser);

            eventOptional = Optional.of(Event.builder()
                    .id(EVENT_ID)
                    .name(EVENT_NAME)
                    .shortDescription(EVENT_SHORT_DESCRIPTION)
                    .longDescription(EVENT_LONG_DESCRIPTION)
                    .createDate(ZonedDateTime.now())
                    .tags(new HashSet<>())
                    .attendingUsers(new HashSet<>())
                    .threads(new HashSet<>())
                    .city(cityRzeszow)
                    .exactAddress(EVENT_EXACT_ADDRESS)
                    .build());

            eventOptional.get().setLastUpdate(eventOptional.get().getCreateDate());
            eventOptional.get().setOwner(eventOwner);
            eventOptional.get().addAttendingUser(eventOwner);
            eventOptional.get().getAttendingUsers().add(secondUser);

            eventOptional.get().addTag(tagJava);
            eventOptional.get().addTag(tagSpring);

            threadCreateDto = ThreadCreateDto.builder()
                    .name(FIRST_THREAD_NAME)
                    .content(FIRST_THREAD_CONTENT)
                    .build();

            thread = Thread.builder()
                    .id(THREAD_ID)
                    .event(eventOptional.get())
                    .owner(eventOwner)
                    .name(FIRST_THREAD_NAME)
                    .content(FIRST_THREAD_CONTENT)
                    .replies(new HashSet<>())
                    .createDate(LocalDateTime.now())
                    .editCounter(0)
                    .build();

            thread.setLastTimeEdited(thread.getCreateDate());

            threadOptional = Optional.of(thread);

            threadReply = ThreadReply.builder()
                    .id(THREAD_REPLY_ID)
                    .thread(thread)
                    .content(REPLY_CONTENT)
                    .replayDate(Calendar.getInstance().getTime())
                    .replier(secondUser)
                    .editCounter(0)
                    .build();
            threadReply.setLastEditDate(threadReply.getReplayDate());

            eventOptional.get().addThread(thread);

        }

        @Test
        @DisplayName("When creating thread should try to load event from database")
        public void whenCreatingThreadShouldTryToLoadEventFromDatabase() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.save(any())).thenReturn(thread);

            eventService.createThreadInEvent(threadCreateDto, EVENT_ID, JWT_STRING);

            verify(eventRepository, times(1)).findById(EVENT_ID);
        }

        @Test
        @DisplayName("When creating thread should throw EventNotFoundException if event was not found")
        public void whenCreatingThreadShouldThrowEventNotFoundExceptionIfEventOptionalIsEmpty() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

            assertThrows(EventNotFoundException.class, () -> eventService.createThreadInEvent(threadCreateDto, EVENT_ID, JWT_STRING));
        }

        @Test
        @DisplayName("When creating thread should extract user email from jwt")
        public void whenCreatingThreadShouldExtractUserEmailFromJwt() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.save(any())).thenReturn(thread);

            eventService.createThreadInEvent(threadCreateDto, EVENT_ID, JWT_STRING);

            verify(jwtUtil, times(1)).extractUsername(JWT_STRING);
        }

        @Test
        @DisplayName("When creating thread should find user in database")
        public void whenCreatingThreadShouldFindUserInDatabase() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.save(any())).thenReturn(thread);


            eventService.createThreadInEvent(threadCreateDto, EVENT_ID, JWT_STRING);

            verify(userRepository, times(1)).findByEmail(secondUser.getEmail());
        }

        @Test
        @DisplayName("When creating thread should check if user is attending event")
        public void whenCreatingThreadShouldCheckIfUserIsAttendingEvent() {
            when(threadRepository.save(any())).thenReturn(thread);

            eventOptional = Optional.of(Mockito.spy(eventOptional.get()));

            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

            eventService.createThreadInEvent(threadCreateDto, EVENT_ID, JWT_STRING);

            verify(eventOptional.get(), times(1)).isUserAttending(secondUser);


        }

        @Test
        @DisplayName("When creating thread should throw NotAttenderException if user is not attending event")
        public void whenCreatingThreadShouldThrowNotAttenderExceptionIfUserIsNotAttendingEvent() {
            eventOptional.get().getAttendingUsers().remove(secondUser);
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

            assertThrows(NotAttenderException.class, () -> eventService.createThreadInEvent(threadCreateDto, EVENT_ID, JWT_STRING));
        }

        @Test
        @DisplayName("When creating thread should save new thread with data from dto")
        public void whenCreatingThreadShouldSaveNewThreadWithDataFromDto() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.save(any())).thenReturn(thread);

            ArgumentCaptor<Thread> threadArgumentCaptor = ArgumentCaptor.forClass(Thread.class);

            eventService.createThreadInEvent(threadCreateDto, EVENT_ID, JWT_STRING);

            verify(threadRepository, times(1)).save(threadArgumentCaptor.capture());

            Thread threadToBeSaved = threadArgumentCaptor.getValue();
            assertEquals(threadCreateDto.getName(), threadToBeSaved.getName());
            assertEquals(threadCreateDto.getContent(), threadToBeSaved.getContent());
        }

        @Test
        @DisplayName("When creating thread should save new thread with set relationships")
        public void whenCreatingThreadShouldSaveNewThreadWithSetRelationships() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.save(any())).thenReturn(thread);

            ArgumentCaptor<Thread> threadArgumentCaptor = ArgumentCaptor.forClass(Thread.class);

            eventService.createThreadInEvent(threadCreateDto, EVENT_ID, JWT_STRING);

            verify(threadRepository, times(1)).save(threadArgumentCaptor.capture());

            Thread threadToBeSaved = threadArgumentCaptor.getValue();
            assertEquals(eventOptional.get(), threadToBeSaved.getEvent());
            assertTrue(eventOptional.get().getThreads().contains(threadToBeSaved));

        }

        @Test
        @DisplayName("When creating thread should save new thread with set create date and edit date which have to be equal")
        public void whenCreatingThreadShouldSaveNewThreadWithSetCreateDateAndEditDateWhichHaveToBeEqual() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.save(any())).thenReturn(thread);

            ArgumentCaptor<Thread> threadArgumentCaptor = ArgumentCaptor.forClass(Thread.class);

            eventService.createThreadInEvent(threadCreateDto, EVENT_ID, JWT_STRING);

            verify(threadRepository, times(1)).save(threadArgumentCaptor.capture());

            Thread threadToBeSaved = threadArgumentCaptor.getValue();
            assertNotNull(threadToBeSaved.getCreateDate());
            assertNotNull(threadToBeSaved.getLastTimeEdited());
            assertEquals(threadToBeSaved.getCreateDate(), threadToBeSaved.getLastTimeEdited());

        }

        @Test
        @DisplayName("When creating thread should save new thread with zeroed edit counter")
        public void whenCreatingThreadShouldSaveNewThreadWithZeroedEditCounter() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.save(any())).thenReturn(thread);

            ArgumentCaptor<Thread> threadArgumentCaptor = ArgumentCaptor.forClass(Thread.class);

            eventService.createThreadInEvent(threadCreateDto, EVENT_ID, JWT_STRING);

            verify(threadRepository, times(1)).save(threadArgumentCaptor.capture());

            Thread threadToBeSaved = threadArgumentCaptor.getValue();
            assertEquals(eventOptional.get(), threadToBeSaved.getEvent());
            assertTrue(eventOptional.get().getThreads().contains(threadToBeSaved));

        }

        @Test
        @DisplayName("When creating thread should create new set for replays")
        public void whenCreatingThreadShouldCreateNewSetForRepliesToSave() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.save(any())).thenReturn(thread);

            ArgumentCaptor<Thread> threadArgumentCaptor = ArgumentCaptor.forClass(Thread.class);

            eventService.createThreadInEvent(threadCreateDto, EVENT_ID, JWT_STRING);

            verify(threadRepository, times(1)).save(threadArgumentCaptor.capture());

            Thread threadToBeSaved = threadArgumentCaptor.getValue();
            assertNotNull(threadToBeSaved.getReplies());
        }
    }

    /*
     ********************************************************************************************************************
     *                                       UPDATE THREAD IN EVENT
     ********************************************************************************************************************
     */
    @Nested
    @DisplayName("Thread update tests:")
    class ThreadUpdateTests {
        @BeforeEach
        void setUp() {
            eventService = new EventServiceImpl(eventRepository, cityRepository, tagRepository, userRepository, threadRepository, threadReplyRepository, fileRepository, notificationService, cityUtils, jwtUtil, tikaFileTypeDetector);

            tagJava = Tag.builder()
                    .name("java")
                    .id(TAG_JAVA_ID)
                    .events(new HashSet<>())
                    .build();
            tagSpring = Tag.builder()
                    .name("spring")
                    .id(TAG_SPRING_ID)
                    .events(new HashSet<>())
                    .build();

            cityRzeszow = City.builder()
                    .id(CITY_RZESZOW_ID)
                    .name("Rzeszow")
                    .events(new ArrayList<>())
                    .residents(new HashSet<>())
                    .build();

            eventOwner = User.builder()
                    .id(FIRST_USER_ID)
                    .email("example@dot.com")
                    .role(Role.USER)
                    .firstName(EVENT_OWNER_FIRST_NAME)
                    .lastName(EVENT_OWNER_LAST_NAME)
                    .homeCity(cityRzeszow)
                    .attendingEvents(new ArrayList<>())
                    .userEvents(new ArrayList<>())
                    .threads(new HashSet<>())
                    .password(passwordEncoder.encode(PASSWORD_DEFAULT))
                    .lastCredentialsChangeTime(LocalDateTime.now())
                    .build();

            eventOwnerOptional = Optional.of(eventOwner);

            secondUser = User.builder()
                    .id(SECOND_USER_ID)
                    .role(Role.USER)
                    .firstName(SECOND_USER_FIRST_NAME)
                    .lastName(SECOND_USER_LAST_NAME)
                    .homeCity(cityRzeszow)
                    .email(SECOND_USER_EMAIL)
                    .userEvents(new ArrayList<>())
                    .attendingEvents(new ArrayList<>())
                    .threads(new HashSet<>())
                    .build();
            secondUserOptional = Optional.of(secondUser);

            event = Event.builder()
                    .id(EVENT_ID)
                    .name(EVENT_NAME)
                    .shortDescription(EVENT_SHORT_DESCRIPTION)
                    .longDescription(EVENT_LONG_DESCRIPTION)
                    .createDate(ZonedDateTime.now())
                    .tags(new HashSet<>())
                    .attendingUsers(new HashSet<>())
                    .threads(new HashSet<>())
                    .city(cityRzeszow)
                    .exactAddress(EVENT_EXACT_ADDRESS)
                    .build();

            eventOptional = Optional.of(event);

            event.setLastUpdate(eventOptional.get().getCreateDate());
            event.setOwner(eventOwner);
            //eventOptional.get().addAttendingUser(eventOwner);
            event.addAttendingUser(secondUser);

            eventOptional.get().addTag(tagJava);
            eventOptional.get().addTag(tagSpring);

            threadCreateDto = ThreadCreateDto.builder()
                    .name(FIRST_THREAD_NAME)
                    .content(FIRST_THREAD_CONTENT)
                    .build();

            thread = Thread.builder()
                    .id(THREAD_ID)
                    .event(eventOptional.get())
                    .owner(secondUser)
                    .name(FIRST_THREAD_NAME)
                    .content(FIRST_THREAD_CONTENT)
                    .replies(new HashSet<>())
                    .createDate(LocalDateTime.now().minusMinutes(1))
                    .editCounter(0)
                    .build();
            thread.setLastTimeEdited(thread.getCreateDate());

            threadOptional = Optional.of(thread);

            secondUser.addThread(thread);

            threadReply = ThreadReply.builder()
                    .id(THREAD_REPLY_ID)
                    .thread(thread)
                    .content(REPLY_CONTENT)
                    .replayDate(Calendar.getInstance().getTime())
                    .replier(eventOwner)
                    .editCounter(0)
                    .build();
            threadReply.setLastEditDate(threadReply.getReplayDate());
            eventOwner.getThreadReplies().add(threadReply);
        }

        @Test
        @DisplayName("When updating thread should find event by given id")
        public void whenUpdatingThreadShouldFindEventByGivenId() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);
            when(threadRepository.save(thread)).thenReturn(thread);

            eventService.updateThreadInEvent(threadCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

            verify(eventRepository, times(1)).findById(EVENT_ID);
        }

        @Test
        @DisplayName("When updating thread should throw EventNotFoundException if there is no event with given id")
        public void whenUpdatingThreadShouldThrowEventNotFoundExceptionIfThereIsNoEventWithGivenId() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

            assertThrows(EventNotFoundException.class, () -> eventService.updateThreadInEvent(threadCreateDto, EVENT_ID, THREAD_ID, JWT_STRING));

        }

        @Test
        @DisplayName("When updating thread should extract user email from jwt")
        public void whenUpdatingThreadShouldExtractUserEmailFromJwt() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);
            when(threadRepository.save(thread)).thenReturn(thread);

            eventService.updateThreadInEvent(threadCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

            verify(jwtUtil, times(1)).extractUsername(JWT_STRING);
        }

        @Test
        @DisplayName("When updating thread should try to load user from database")
        public void whenUpdatingThreadShouldTryLoadUserFromDatabase() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);
            when(threadRepository.save(thread)).thenReturn(thread);

            eventService.updateThreadInEvent(threadCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

            verify(userRepository, times(1)).findByEmail(SECOND_USER_EMAIL);
        }


        @Test
        @DisplayName("When updating thread should try to load thread by given id")
        public void whenUpdatingThreadShouldTryToLoadThreadByGivenId() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);
            when(threadRepository.save(thread)).thenReturn(thread);

            eventService.updateThreadInEvent(threadCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

            verify(threadRepository, times(1)).findById(THREAD_ID);
        }

        @Test
        @DisplayName("When updating thread should throw ThreadNotFoundException if there is no thread with given id")
        public void whenUpdatingThreadShouldThrowThreadNotFoundExceptionIfThereIsNoThreadWithGivenId() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(Optional.empty());

            assertThrows(ThreadNotFoundException.class, () -> eventService.updateThreadInEvent(threadCreateDto, EVENT_ID, THREAD_ID, JWT_STRING));
        }

        @Test
        @DisplayName("When updating thread should check if owner of thread is still attending event")
        public void whenUpdatingThreadShouldCheckIfOwnerOfThreadIsStillAttendingEvent() {
            Event eventSpy = Mockito.spy(event);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(eventSpy));
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);
            when(threadRepository.save(thread)).thenReturn(thread);

            eventService.updateThreadInEvent(threadCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

            verify(eventSpy, times(1)).isUserAttending(secondUser);
        }

        @Test
        @DisplayName("When updating thread should Throw NotAttenderException if thread owner is not attending anymore")
        public void whenUpdatingThreadShouldThrowNotAttenderExceptionIfThreadOwnerIsNotAttendingAnymore() {
            event.removeAttendingUser(secondUser);
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);

            assertThrows(NotAttenderException.class, () -> eventService.updateThreadInEvent(threadCreateDto, EVENT_ID, THREAD_ID, JWT_STRING));

        }

        @Test
        @DisplayName("When updating thread should check if user is owner of thread")
        public void whenUpdatingThreadShouldCheckIfUserIsOwnerOfThread() {
            Thread threadSpy = Mockito.spy(thread);
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(Optional.of(threadSpy));
            when(threadRepository.save(threadSpy)).thenReturn(threadSpy);

            eventService.updateThreadInEvent(threadCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

            verify(threadSpy, times(1)).isUserOwner(secondUser);
        }

        @Test
        @DisplayName("When updating thread should Throw NotThreadOwner if user do not own this thread")
        public void whenUpdatingThreadShouldThrowNotThreadOwnerExceptionIfUserDoNotOwnThisThread() {
            thread.setOwner(secondUser);
            eventOwner.getThreads().remove(thread);
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);

            assertThrows(NotThreadOwnerException.class, () -> eventService.updateThreadInEvent(threadCreateDto, EVENT_ID, THREAD_ID, JWT_STRING));
        }

        @Test
        @DisplayName("When updating thread should update only name and content of thread")
        public void whenUpdatingThreadShouldUpdateOnlyNameAndContentOfThread() {
            threadCreateDto.setName("updated name");
            threadCreateDto.setContent("updated content");

            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);
            when(threadRepository.save(thread)).thenReturn(thread);

            eventService.updateThreadInEvent(threadCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

            assertEquals(threadCreateDto.getContent(), thread.getContent());
            assertEquals(threadCreateDto.getName(), thread.getName());
        }

        @Test
        @DisplayName("When updating thread should update lastTimeEdited field")
        public void whenUpdatingThreadShouldUpdateLastTimeEditedField() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);
            when(threadRepository.save(thread)).thenReturn(thread);

            LocalDateTime lastTimeEdited = thread.getLastTimeEdited();
            eventService.updateThreadInEvent(threadCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

            assertTrue(lastTimeEdited.isBefore(thread.getLastTimeEdited()));
        }

        @Test
        @DisplayName("When updating thread should save updated entity")
        public void whenUpdatingThreadShouldSaveUpdatedEntity() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);
            when(threadRepository.save(thread)).thenReturn(thread);

            eventService.updateThreadInEvent(threadCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

            verify(threadRepository, times(1)).save(thread);
        }

    }

    /*
     ********************************************************************************************************************
     *                                       CREATING REPLAY IN THREAD
     ********************************************************************************************************************
     */
    @Nested
    @DisplayName("Create reply in thread test:")
    class CreateReplayInThreadTests {
        @BeforeEach
        void setUp() {
            eventService = new EventServiceImpl(eventRepository, cityRepository, tagRepository, userRepository, threadRepository, threadReplyRepository, fileRepository, notificationService, cityUtils, jwtUtil, tikaFileTypeDetector);

            tagJava = Tag.builder()
                    .name("java")
                    .id(TAG_JAVA_ID)
                    .events(new HashSet<>())
                    .build();
            tagSpring = Tag.builder()
                    .name("spring")
                    .id(TAG_SPRING_ID)
                    .events(new HashSet<>())
                    .build();

            cityRzeszow = City.builder()
                    .id(CITY_RZESZOW_ID)
                    .name("Rzeszow")
                    .events(new ArrayList<>())
                    .residents(new HashSet<>())
                    .build();

            eventOwner = User.builder()
                    .id(FIRST_USER_ID)
                    .email("example@dot.com")
                    .role(Role.USER)
                    .firstName(EVENT_OWNER_FIRST_NAME)
                    .lastName(EVENT_OWNER_LAST_NAME)
                    .homeCity(cityRzeszow)
                    .attendingEvents(new ArrayList<>())
                    .userEvents(new ArrayList<>())
                    .threads(new HashSet<>())
                    .password(passwordEncoder.encode(PASSWORD_DEFAULT))
                    .lastCredentialsChangeTime(LocalDateTime.now())
                    .build();

            secondUser = User.builder()
                    .id(SECOND_USER_ID)
                    .role(Role.USER)
                    .firstName(SECOND_USER_FIRST_NAME)
                    .lastName(SECOND_USER_LAST_NAME)
                    .homeCity(cityRzeszow)
                    .email(SECOND_USER_EMAIL)
                    .userEvents(new ArrayList<>())
                    .attendingEvents(new ArrayList<>())
                    .threads(new HashSet<>())
                    .build();
            secondUserOptional = Optional.of(secondUser);

            event = Event.builder()
                    .id(EVENT_ID)
                    .name(EVENT_NAME)
                    .shortDescription(EVENT_SHORT_DESCRIPTION)
                    .longDescription(EVENT_LONG_DESCRIPTION)
                    .createDate(ZonedDateTime.now())
                    .tags(new HashSet<>())
                    .attendingUsers(new HashSet<>())
                    .threads(new HashSet<>())
                    .city(cityRzeszow)
                    .exactAddress(EVENT_EXACT_ADDRESS)
                    .build();

            eventOptional = Optional.of(event);

            event.setLastUpdate(event.getCreateDate());
            event.setOwner(eventOwner);
            event.addAttendingUser(eventOwner);
            event.addAttendingUser(secondUser);

            event.addTag(tagJava);
            event.addTag(tagSpring);

            threadCreateDto = ThreadCreateDto.builder()
                    .name(FIRST_THREAD_NAME)
                    .content(FIRST_THREAD_CONTENT)
                    .build();
            threadReplayCreateDto = ThreadReplayCreateDto.builder()
                    .replyContent("Content of replay.")
                    .build();

            thread = Thread.builder()
                    .id(THREAD_ID)
                    .event(event)
                    .owner(eventOwner)
                    .name(FIRST_THREAD_NAME)
                    .content(FIRST_THREAD_CONTENT)
                    .replies(new HashSet<>())
                    .createDate(LocalDateTime.now().minusMinutes(1))
                    .editCounter(0)
                    .build();

            thread.setLastTimeEdited(thread.getCreateDate());
            event.addAttendingUser(secondUser);
            eventOwner.addThread(thread);

            threadOptional = Optional.of(thread);

            threadReply = ThreadReply.builder()
                    .id(THREAD_REPLY_ID)
                    .thread(thread)
                    .content(REPLY_CONTENT)
                    .replayDate(Calendar.getInstance().getTime())
                    .replier(secondUser)
                    .editCounter(0)
                    .build();

            threadReply.setLastEditDate(threadReply.getReplayDate());
        }

        @Test
        @DisplayName("When Creating reply in thread should find event with given id")
        public void whenCreatingReplayInThreadShouldFindEventWithGivenId() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);

            eventService.createReplyInThread(threadReplayCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

            verify(eventRepository, times(1)).findById(EVENT_ID);

        }

        @Test
        @DisplayName("When Creating reply in thread should check if event optional is empty")
        public void whenCreatingReplayInThreadShouldCheckIfEventOptionalIsEmpty() {
            Optional<Event> eventOptionalSpy = Mockito.spy(eventOptional);
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptionalSpy);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);

            eventService.createReplyInThread(threadReplayCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

            verify(eventOptionalSpy, times(1)).isEmpty();
        }

        @Test
        @DisplayName("When Creating reply in thread should")
        public void whenCreatingReplayInThreadShouldThrowEventNotFoundExceptionIfThereIsNoEventWithThisId() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

            assertThrows(EventNotFoundException.class, () -> eventService.createReplyInThread(threadReplayCreateDto, EVENT_ID, THREAD_ID, JWT_STRING));
        }

        @Test
        @DisplayName("When Creating reply in thread should retrieve event from optional object")
        public void whenCreatingReplayInThreadShouldRetrieveEventFromOptionalObject() {
            Optional<Event> eventOptionalSpy = Mockito.spy(eventOptional);
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptionalSpy);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);

            eventService.createReplyInThread(threadReplayCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

            verify(eventOptionalSpy, times(1)).get();

        }

        @Test
        @DisplayName("When Creating reply in thread should extract user email from jwt")
        public void whenCreatingReplayInThreadShouldExtractUserEmailFromJwt() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);

            eventService.createReplyInThread(threadReplayCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

            verify(jwtUtil, times(1)).extractUsername(JWT_STRING);
        }

        @Test
        @DisplayName("When Creating reply in thread should look up for user by extracted email and retrieve it from optional object")
        public void whenCreatingReplayInThreadShouldLookForUserByExtractedEmail() {
            Optional<User> secondUserOptionalSpy = Mockito.spy(secondUserOptional);
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptionalSpy);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);

            eventService.createReplyInThread(threadReplayCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

            verify(userRepository, times(1)).findByEmail(SECOND_USER_EMAIL);
            verify(secondUserOptionalSpy, times(1)).get();
        }

        @Test
        @DisplayName("When Creating reply in thread should check if replying user is attending event")
        public void whenCreatingReplayInThreadShouldCheckIfReplyingUserIsAttendingEvent() {
            Event eventSpy = Mockito.spy(eventOptional.get());
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(eventSpy));
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);

            eventService.createReplyInThread(threadReplayCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

            verify(eventSpy, times(1)).isUserAttending(secondUser);
        }

        @Test
        @DisplayName("When Creating reply in thread should throw NotAttenderException if user is not attending event")
        public void whenCreatingReplayInThreadShouldThrowNotAttenderExceptionIfUserIsNotAttendingEvent() {
            eventOptional.get().removeAttendingUser(secondUser);
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);


            assertThrows(NotAttenderException.class, () -> eventService.createReplyInThread(threadReplayCreateDto, EVENT_ID, THREAD_ID, JWT_STRING));
        }

        @Test
        @DisplayName("When Creating reply in thread should load thread with given id")
        public void whenCreatingReplayInThreadShouldLookUpThread() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);

            eventService.createReplyInThread(threadReplayCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

            verify(threadRepository, times(1)).findById(THREAD_ID);
        }

        @Test
        @DisplayName("When Creating reply in thread should check if thread optional is empty")
        public void whenCreatingReplayInThreadShouldCheckIfThreadOptionalIsEmpty() {
            Optional<Thread> threadOptionalSpy = Mockito.spy(threadOptional);
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptionalSpy);

            eventService.createReplyInThread(threadReplayCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

            verify(threadOptionalSpy, times(1)).isEmpty();
        }

        @Test
        @DisplayName("When Creating reply in thread should throw ThreadNotFoundException if there is no thread with given id")
        public void whenCreatingReplayInThreadShouldThrowThreadNotFoundExceptionIfThereIsNoThreadWithGivenId() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(Optional.empty());

            assertThrows(ThreadNotFoundException.class, () -> eventService.createReplyInThread(threadReplayCreateDto, EVENT_ID, THREAD_ID, JWT_STRING));


        }

        @Test
        @DisplayName("When Creating reply in thread should set up ne ThreadReply object and save it with all necessary fields")
        public void whenCreatingReplayInThreadShouldSetUpNewThreadReplyObjectAndSaveItWithAllNecessaryFields() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);

            ArgumentCaptor<ThreadReply> threadReplyArgumentCaptor = ArgumentCaptor.forClass(ThreadReply.class);

            eventService.createReplyInThread(threadReplayCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

            verify(threadReplyRepository, times(1)).save(threadReplyArgumentCaptor.capture());
            ThreadReply capturedReply = threadReplyArgumentCaptor.getValue();

            assertEquals(threadReplayCreateDto.getReplyContent(), capturedReply.getContent());
            assertEquals(THREAD_ID, capturedReply.getThread().getId());
            assertEquals(secondUser, capturedReply.getReplier());
            assertEquals(0, capturedReply.getEditCounter());
            assertNotNull(capturedReply.getReplayDate());
            assertNotNull(capturedReply.getLastEditDate());
            assertEquals(capturedReply.getReplayDate(), capturedReply.getLastEditDate());

        }

    }

    /*
     ********************************************************************************************************************
     *                                       UPDATING REPLAY IN THREAD
     ********************************************************************************************************************
     */
    @Nested
    @DisplayName("Update reply in thread test:")
    class UpdateReplayInThreadTests {
        @BeforeEach
        void setUp() {
            eventService = new EventServiceImpl(eventRepository, cityRepository, tagRepository, userRepository, threadRepository, threadReplyRepository, fileRepository, notificationService, cityUtils, jwtUtil, tikaFileTypeDetector);

            tagJava = Tag.builder()
                    .name("java")
                    .id(TAG_JAVA_ID)
                    .events(new HashSet<>())
                    .build();
            tagSpring = Tag.builder()
                    .name("spring")
                    .id(TAG_SPRING_ID)
                    .events(new HashSet<>())
                    .build();

            cityRzeszow = City.builder()
                    .id(CITY_RZESZOW_ID)
                    .name("Rzeszow")
                    .events(new ArrayList<>())
                    .residents(new HashSet<>())
                    .build();

            eventOwner = User.builder()
                    .id(FIRST_USER_ID)
                    .email("example@dot.com")
                    .role(Role.USER)
                    .firstName(EVENT_OWNER_FIRST_NAME)
                    .lastName(EVENT_OWNER_LAST_NAME)
                    .homeCity(cityRzeszow)
                    .attendingEvents(new ArrayList<>())
                    .userEvents(new ArrayList<>())
                    .threads(new HashSet<>())
                    .password(passwordEncoder.encode(PASSWORD_DEFAULT))
                    .lastCredentialsChangeTime(LocalDateTime.now())
                    .build();

            secondUser = User.builder()
                    .id(SECOND_USER_ID)
                    .role(Role.USER)
                    .firstName(SECOND_USER_FIRST_NAME)
                    .lastName(SECOND_USER_LAST_NAME)
                    .homeCity(cityRzeszow)
                    .email(SECOND_USER_EMAIL)
                    .userEvents(new ArrayList<>())
                    .attendingEvents(new ArrayList<>())
                    .threads(new HashSet<>())
                    .build();
            secondUserOptional = Optional.of(secondUser);

            eventOptional = Optional.of(Event.builder()
                    .id(EVENT_ID)
                    .name(EVENT_NAME)
                    .shortDescription(EVENT_SHORT_DESCRIPTION)
                    .longDescription(EVENT_LONG_DESCRIPTION)
                    .createDate(ZonedDateTime.now())
                    .tags(new HashSet<>())
                    .attendingUsers(new HashSet<>())
                    .threads(new HashSet<>())
                    .city(cityRzeszow)
                    .exactAddress(EVENT_EXACT_ADDRESS)
                    .build());

            eventOptional.get().setLastUpdate(eventOptional.get().getCreateDate());
            eventOptional.get().setOwner(eventOwner);
            eventOptional.get().addAttendingUser(eventOwner);
            eventOptional.get().getAttendingUsers().add(secondUser);

            eventOptional.get().addTag(tagJava);
            eventOptional.get().addTag(tagSpring);

            threadCreateDto = ThreadCreateDto.builder()
                    .name(FIRST_THREAD_NAME)
                    .content(FIRST_THREAD_CONTENT)
                    .build();
            threadReplayCreateDto = ThreadReplayCreateDto.builder()
                    .replyContent("Updated")
                    .build();

            thread = Thread.builder()
                    .id(THREAD_ID)
                    .event(eventOptional.get())
                    .owner(eventOwner)
                    .name(FIRST_THREAD_NAME)
                    .content(FIRST_THREAD_CONTENT)
                    .replies(new HashSet<>())
                    .createDate(LocalDateTime.now().minusMinutes(1))
                    .editCounter(0)
                    .build();
            thread.setLastTimeEdited(thread.getCreateDate());
            eventOptional.get().addAttendingUser(secondUser);
            eventOwner.addThread(thread);
            threadOptional = Optional.of(thread);
            threadReply = ThreadReply.builder()
                    .id(THREAD_REPLY_ID)
                    .thread(thread)
                    .content(REPLY_CONTENT)
                    .replayDate(Calendar.getInstance().getTime())
                    .replier(secondUser)
                    .editCounter(0)
                    .build();
            threadReply.setLastEditDate(threadReply.getReplayDate());
            thread.getReplies().add(threadReply);

            threadReplyOptional = Optional.of(threadReply);
        }

        @Test
        @DisplayName("When updating reply in thread should look up for event with given id")
        public void whenUpdatingReplyInThreadShouldLookUpForEventWithGivenId() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(THREAD_REPLY_ID)).thenReturn(threadReplyOptional);

            eventService.updateThreadReplyInEvent(threadReplayCreateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING);

            verify(eventRepository, times(1)).findById(EVENT_ID);
        }

        @Test
        @DisplayName("When updating reply in thread should check if event optional object is empty")
        public void whenUpdatingReplyInThreadShouldCheckIfEventOptionalObjectIsEmpty() {
            Optional<Event> eventOptionalSpy = Mockito.spy(eventOptional);
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptionalSpy);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(THREAD_REPLY_ID)).thenReturn(threadReplyOptional);

            eventService.updateThreadReplyInEvent(threadReplayCreateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING);

            verify(eventOptionalSpy, times(1)).isEmpty();

        }

        @Test
        @DisplayName("When updating reply in thread should throw EventNotFoundException if there is no event with given id")
        public void whenUpdatingReplyInThreadShouldThrowEventNotFoundExceptionIfThereIsNoEventWithGivenId() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

            assertThrows(EventNotFoundException.class, () -> eventService.updateThreadReplyInEvent(threadReplayCreateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING));
        }

        @Test
        @DisplayName("When updating reply in thread should retrieve event from optional object")
        public void whenUpdatingReplyInThreadShouldRetrieveEventFromOptionalObject() {
            Optional<Event> eventOptionalSpy = Mockito.spy(eventOptional);
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptionalSpy);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(THREAD_REPLY_ID)).thenReturn(threadReplyOptional);

            eventService.updateThreadReplyInEvent(threadReplayCreateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING);

            verify(eventOptionalSpy, times(1)).get();
        }

        @Test
        @DisplayName("When updating reply in thread should extract user email from jwt")
        public void whenUpdatingReplyInThreadShouldExtractUserEmailFromJwt() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(THREAD_REPLY_ID)).thenReturn(threadReplyOptional);

            eventService.updateThreadReplyInEvent(threadReplayCreateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING);

            verify(jwtUtil, times(1)).extractUsername(JWT_STRING);
        }

        @Test
        @DisplayName("When updating reply in thread should retrieve user from optional object")
        public void whenUpdatingReplyInThreadShouldRetrieveUserFromOptionalObject() {
            Optional<User> secondUserOptionalSpy = Mockito.spy(secondUserOptional);
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptionalSpy);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(THREAD_REPLY_ID)).thenReturn(threadReplyOptional);

            eventService.updateThreadReplyInEvent(threadReplayCreateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING);

            verify(secondUserOptionalSpy, times(1)).get();
        }


        @Test
        @DisplayName("When updating reply in thread should check if user is attending event")
        public void whenUpdatingReplyInThreadShouldCheckIfUserIsAttendingEvent() {
            Event eventSpy = Mockito.spy(eventOptional.get());
            eventOptional = Optional.of(eventSpy);
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(THREAD_REPLY_ID)).thenReturn(threadReplyOptional);

            eventService.updateThreadReplyInEvent(threadReplayCreateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING);

            verify(eventSpy, times(1)).isUserAttending(secondUser);
        }

        @Test
        @DisplayName("When updating reply in thread should throw NotAttenderException if user is not attending event")
        public void whenUpdatingReplyInThreadShouldThrowNotAttenderExceptionIfUserIsNotAttendingEvent() {
            eventOptional.get().removeAttendingUser(secondUser);
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

            assertThrows(NotAttenderException.class, () -> eventService.updateThreadReplyInEvent(threadReplayCreateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING));
        }


        @Test
        @DisplayName("When updating reply in thread should look up for thread with given id")
        public void whenUpdatingReplyInThreadShouldLookUpForThreadWithGivenId() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(THREAD_REPLY_ID)).thenReturn(threadReplyOptional);

            eventService.updateThreadReplyInEvent(threadReplayCreateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING);

            verify(threadRepository, times(1)).findById(THREAD_ID);
        }

        @Test
        @DisplayName("When updating reply in thread should check if thread optional object is empty")
        public void whenUpdatingReplyInThreadShouldCheckIfThreadOptionalObjectIsEmpty() {
            Optional<Thread> threadOptionalSpy = Mockito.spy(threadOptional);
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptionalSpy);
            when(threadReplyRepository.findById(THREAD_REPLY_ID)).thenReturn(threadReplyOptional);

            eventService.updateThreadReplyInEvent(threadReplayCreateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING);
            verify(threadOptionalSpy, times(1)).isEmpty();
        }

        @Test
        @DisplayName("When updating reply in thread should throw ThreadNotFoundException if there is no thread with that id")
        public void whenUpdatingReplyInThreadShouldThrowThreadNotFoundExceptionIfThereIsNoThreadWithThatId() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(Optional.empty());

            assertThrows(ThreadNotFoundException.class, () -> eventService.updateThreadReplyInEvent(threadReplayCreateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING));

        }

        @Test
        @DisplayName("When updating reply in thread should retrieve thread from optional object")
        public void whenUpdatingReplyInThreadShouldRetrieveThreadFromOptionalObject() {
            Optional<Thread> threadOptionalSpy = Mockito.spy(threadOptional);
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptionalSpy);
            when(threadReplyRepository.findById(THREAD_REPLY_ID)).thenReturn(threadReplyOptional);

            eventService.updateThreadReplyInEvent(threadReplayCreateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING);

            verify(threadOptionalSpy, times(1)).get();
        }

        @Test
        @DisplayName("When updating reply in thread should look up for thread reply with given id")
        public void whenUpdatingReplyInThreadShouldLookUpForThreadReplyWithGivenId() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(THREAD_REPLY_ID)).thenReturn(threadReplyOptional);

            eventService.updateThreadReplyInEvent(threadReplayCreateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING);

            verify(threadReplyRepository, times(1)).findById(THREAD_REPLY_ID);
        }

        @Test
        @DisplayName("When updating reply in thread should check if thread reply object is empty")
        public void whenUpdatingReplyInThreadShouldCheckIfThreadReplyObjectIsEmpty() {
            Optional<ThreadReply> threadReplyOptionalSpy = Mockito.spy(threadReplyOptional);
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(THREAD_REPLY_ID)).thenReturn(threadReplyOptionalSpy);

            eventService.updateThreadReplyInEvent(threadReplayCreateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING);

            verify(threadReplyOptionalSpy, times(1)).isEmpty();
        }

        @Test
        @DisplayName("When updating reply in thread should throw ThreadReplyNotFoundException if thread reply optional object is empty")
        public void whenUpdatingReplyInThreadShouldThrowThreadReplyNotFoundExceptionIfThreadReplyOptionalObjectIsEmpty() {
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(THREAD_REPLY_ID)).thenReturn(Optional.empty());


            assertThrows(ThreadReplyNotFoundException.class, () -> eventService.updateThreadReplyInEvent(threadReplayCreateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING));
        }

        @Test
        @DisplayName("When updating reply in thread should retrieve thread reply object from thread reply optional object")
        public void whenUpdatingReplyInThreadShouldRetrieveThreadReplyObjectFromThreadReplyOptionalObject() {
            Optional<ThreadReply> threadReplyOptionalSpy = Mockito.spy(threadReplyOptional);
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(THREAD_REPLY_ID)).thenReturn(threadReplyOptionalSpy);


            eventService.updateThreadReplyInEvent(threadReplayCreateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING);

            verify(threadReplyOptionalSpy, times(1)).get();
        }

        @Test
        @DisplayName("When updating reply in thread should check if thread with given id contains thread reply with given id")
        public void whenUpdatingReplyInThreadShouldCheckIfThreadWithGivenIdContainsReplyWithGivenId() {
            Thread threadSpy = Mockito.spy(thread);
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(Optional.of(threadSpy));
            when(threadReplyRepository.findById(THREAD_REPLY_ID)).thenReturn(threadReplyOptional);


            eventService.updateThreadReplyInEvent(threadReplayCreateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING);

            verify(threadSpy, times(1)).containsReply(threadReply);
        }

        @Test
        @DisplayName("When updating reply in thread should throw WrongThreadException if thread doesn't contain this thread reply")
        public void whenUpdatingReplyInThreadShouldThrowWrongThreadExceptionIfThreadDoesntContainThisThreadReply() {
            thread.getReplies().remove(threadReply);
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(THREAD_REPLY_ID)).thenReturn(threadReplyOptional);


            assertThrows(WrongThreadException.class, () -> eventService.updateThreadReplyInEvent(threadReplayCreateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING));
        }

        @Test
        @DisplayName("When updating reply in thread should check if performing user is owner of thread reply being updated")
        public void whenUpdatingReplyInThreadShouldCheckIfPerformingUserIsOwnerOfThreadReplyBeingUpdated() {
            ThreadReply threadReplySpy = Mockito.spy(threadReply);
            thread.getReplies().remove(threadReply);
            thread.addReplayToThread(threadReplySpy);

            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(THREAD_REPLY_ID)).thenReturn(Optional.of(threadReplySpy));

            eventService.updateThreadReplyInEvent(threadReplayCreateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING);

            verify(threadReplySpy, times(1)).isReplier(secondUser);
        }

        @Test
        @DisplayName("When updating reply in thread should check if performing user is owner of thread reply being updated")
        public void whenUpdatingReplyInThreadShouldThrowNotThreadReplyOwnerExceptionIfUserTryToUpdateNotHisReply() {
            threadReply.setReplier(eventOwner);
            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(THREAD_REPLY_ID)).thenReturn(threadReplyOptional);

            assertThrows(NotThreadReplyOwnerException.class, () -> eventService.updateThreadReplyInEvent(threadReplayCreateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING));
        }

        @Test
        @DisplayName("When updating reply in thread should update content and edit counter  in stored thread reply and save it")
        public void whenUpdatingReplyInThreadShouldUpdateContentAndEditCounterInStoredThreadReply() {
            ThreadReply threadReplySpy = Mockito.spy(threadReply);
            thread.getReplies().remove(threadReply);
            thread.addReplayToThread(threadReplySpy);

            when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(THREAD_ID)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(THREAD_REPLY_ID)).thenReturn(Optional.of(threadReplySpy));

            ArgumentCaptor<ThreadReply> threadReplyArgumentCaptor = ArgumentCaptor.forClass(ThreadReply.class);

            eventService.updateThreadReplyInEvent(threadReplayCreateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING);

            verify(threadReplyRepository, times(1)).save(threadReplyArgumentCaptor.capture());
            var updatedThreadReply = threadReplyArgumentCaptor.getValue();
            verify(threadReplySpy, times(1)).incrementEditCounter();
            assertEquals(threadReplayCreateDto.getReplyContent(), updatedThreadReply.getContent());
        }

    }

    @Nested
    @DisplayName("Search events test:")
    class SearchEventsTests {
        @BeforeEach
        void setUp() {

        }


    }

}