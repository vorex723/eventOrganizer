package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.city.CityUtils;
import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.mapper.EventMapper;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotAttenderException;
import com.mazurek.eventOrganizer.exception.event.NotEventOwnerException;
import com.mazurek.eventOrganizer.exception.thread.*;
import com.mazurek.eventOrganizer.file.FileRepository;
import com.mazurek.eventOrganizer.jwt.JwtUtil;
import com.mazurek.eventOrganizer.notification.NotificationService;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.tag.TagRepository;
import com.mazurek.eventOrganizer.thread.*;
import com.mazurek.eventOrganizer.thread.Thread;
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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    private UUID firstUserId = UUID.randomUUID();
    private UUID secondUserId = UUID.randomUUID();
    private UUID cityRzeszowId = UUID.randomUUID();
    private UUID cityKrakowId = UUID.randomUUID();
    private UUID eventId = UUID.randomUUID();
    private UUID tagSpringId = UUID.randomUUID();
    private UUID tagJavaId = UUID.randomUUID();
    private UUID tagWitamId = UUID.randomUUID();
    private UUID tagZegnamId = UUID.randomUUID();
    private UUID threadId = UUID.randomUUID();
    private UUID threadReplyId = UUID.randomUUID();



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
    private static final String EVENT_LONG_DESCRIPTION = "long description can be quite long, and it Should be. maybe i should put Lorem Ipsum here.";
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
    private EventMapper eventMapper;
    @Mock
    private ThreadMapper threadMapper;
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

    private final Tika tikaFileTypeDetector = new Tika();
    private User eventOwner;
    private User secondUser;
    private BCryptPasswordEncoder passwordEncoder = Mockito.spy(new BCryptPasswordEncoder());
    private Optional<Event> eventOptional;
    private Optional<User> secondUserOptional;
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
    private Optional<Thread> threadOptional;
    private Thread thread;
    private ThreadReply threadReply;
    private Optional<ThreadReply> threadReplyOptional;


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
            eventService = new EventServiceImpl(eventRepository, cityRepository, tagRepository, userRepository, threadRepository, threadReplyRepository, fileRepository, eventMapper, threadMapper, notificationService, cityUtils, jwtUtil, tikaFileTypeDetector);

            tagJava = Tag.builder()
                    .name("java")
                    .id(tagJavaId)
                    .events(new HashSet<>())
                    .build();
            tagSpring = Tag.builder()
                    .name("spring")
                    .id(tagSpringId)
                    .events(new HashSet<>())
                    .build();

            cityRzeszow = City.builder()
                    .id(cityRzeszowId)
                    .name("Rzeszow")
                    .events(new ArrayList<>())
                    .residents(new HashSet<>())
                    .build();

            eventOwner = User.builder()
                    .id(firstUserId)
                    .email("example@dot.com")
                    .role(Role.USER)
                    .firstName(EVENT_OWNER_FIRST_NAME)
                    .lastName(EVENT_OWNER_LAST_NAME)
                    .homeCity(cityRzeszow)
                    .attendingEvents(new ArrayList<>())
                    .userEvents(new ArrayList<>())
                    .password(passwordEncoder.encode(PASSWORD_DEFAULT))
                    .lastCredentialsChangeTime(Calendar.getInstance().getTimeInMillis())
                    .build();

            eventOptional = Optional.of(Event.builder()
                    .id(eventId)
                    .name(EVENT_NAME)
                    .shortDescription(EVENT_SHORT_DESCRIPTION)
                    .longDescription(EVENT_LONG_DESCRIPTION)
                    .createDate(new Date(Calendar.getInstance().getTimeInMillis()))
                    .tags(new HashSet<>())
                    .attendingUsers(new HashSet<>())
                    .threads(new HashSet<>())
                    .city(cityRzeszow)
                    .exactAddress(EVENT_EXACT_ADDRESS)
                    .build());

            eventCreateDto = EventCreateDto.builder()
                    .shortDescription(EVENT_SHORT_DESCRIPTION)
                    .longDescription(EVENT_LONG_DESCRIPTION)
                    .tags(new ArrayList<>())
                    .city(EVENT_CREATE_DTO_CITY)
                    .exactAddress(EVENT_EXACT_ADDRESS)
                    .eventStartDate(new Date(Calendar.getInstance().getTimeInMillis()+3000000))
                    .build();
            eventCreateDto.getTags().add("java");
            eventCreateDto.getTags().add("spring");


            eventOptional.get().setLastUpdate(eventOptional.get().getCreateDate());
            eventOptional.get().setOwner(eventOwner);
            eventOptional.get().addAttendingUser(eventOwner);

            eventOptional.get().addTag(tagJava);
            eventOptional.get().addTag(tagSpring);

        }

        @Test
        @DisplayName("When creating event should create empty set for tags if tag list in dto is empty")
        public void whenCreatingEventShouldCreateEmptySetForTagsIfTagListInDtoIsEmpty() {
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(cityUtils.resolveCity(eventCreateDto.getCity())).thenReturn(cityRzeszow);

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
            when(cityUtils.resolveCity(eventCreateDto.getCity())).thenReturn(cityRzeszow);

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
            when(cityUtils.resolveCity(eventCreateDto.getCity())).thenReturn(cityRzeszow);

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
            when(cityUtils.resolveCity(eventCreateDto.getCity())).thenReturn(cityRzeszow);

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
            eventService = new EventServiceImpl(eventRepository, cityRepository, tagRepository, userRepository, threadRepository, threadReplyRepository, fileRepository, eventMapper, threadMapper, notificationService, cityUtils, jwtUtil, tikaFileTypeDetector);

            tagJava = Tag.builder()
                    .name("java")
                    .id(tagJavaId)
                    .events(new HashSet<>())
                    .build();
            tagSpring = Tag.builder()
                    .name("spring")
                    .id(tagSpringId)
                    .events(new HashSet<>())
                    .build();

            eventOptional = Optional.of(Event.builder()
                    .id(eventId)
                    .name(EVENT_NAME)
                    .shortDescription(EVENT_SHORT_DESCRIPTION)
                    .longDescription(EVENT_LONG_DESCRIPTION)
                    .createDate(new Date(Calendar.getInstance().getTimeInMillis()))
                    .tags(new HashSet<>())
                    .attendingUsers(new HashSet<>())
                    .threads(new HashSet<>())
                    .city(cityRzeszow)
                    .exactAddress(EVENT_EXACT_ADDRESS)
                    .build());

            eventOwner = User.builder()
                    .id(firstUserId)
                    .email("example@dot.com")
                    .role(Role.USER)
                    .firstName(EVENT_OWNER_FIRST_NAME)
                    .lastName(EVENT_OWNER_LAST_NAME)
                    .homeCity(cityRzeszow)
                    .attendingEvents(new ArrayList<>())
                    .userEvents(new ArrayList<>())
                    .password(passwordEncoder.encode(PASSWORD_DEFAULT))
                    .lastCredentialsChangeTime(Calendar.getInstance().getTimeInMillis())
                    .build();

            eventOptional.get().setLastUpdate(eventOptional.get().getCreateDate());
            eventOptional.get().setOwner(eventOwner);
            eventOptional.get().addAttendingUser(eventOwner);

            eventOptional.get().addTag(tagJava);
            eventOptional.get().addTag(tagSpring);

        }

        @Test
        @DisplayName("When getting event by id should run query once")
        public void whenGettingEventByIdShouldRunQueryOnce() {
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);

            eventService.getEventById(eventId);

            verify(eventRepository, times(1)).findById(eventId);
        }

        @Test
        @DisplayName("When getting event by id should throw event not found exception when there is no")
        public void whenGettingEventByIdShouldThrowEvenNotFoundException() {
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            assertThrows(EventNotFoundException.class, () -> eventService.getEventById(eventId));

            verify(eventRepository, times(1)).findById(eventId);
        }

        @Test
        @DisplayName("When getting event by id should map it to eventDto")
        public void whenGettingEventByIdShouldMapItToEventDto() {
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);

            eventService.getEventById(eventId);

            verify(eventMapper, times(1)).mapEventToEventWithUsersDto(eventOptional.get());
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
            eventService = new EventServiceImpl(eventRepository, cityRepository, tagRepository, userRepository, threadRepository, threadReplyRepository, fileRepository, eventMapper, threadMapper, notificationService, cityUtils, jwtUtil, tikaFileTypeDetector);

            tagJava = Tag.builder()
                    .name("java")
                    .id(tagJavaId)
                    .events(new HashSet<>())
                    .build();
            tagSpring = Tag.builder()
                    .name("spring")
                    .id(tagSpringId)
                    .events(new HashSet<>())
                    .build();

            cityRzeszow = City.builder()
                    .id(cityRzeszowId)
                    .name("Rzeszow")
                    .events(new ArrayList<>())
                    .residents(new HashSet<>())
                    .build();

            eventOwner = User.builder()
                    .id(firstUserId)
                    .email(EVENT_OWNER_EMAIL)
                    .role(Role.USER)
                    .firstName(EVENT_OWNER_FIRST_NAME)
                    .lastName(EVENT_OWNER_LAST_NAME)
                    .homeCity(cityRzeszow)
                    .attendingEvents(new ArrayList<>())
                    .userEvents(new ArrayList<>())
                    .password(passwordEncoder.encode(PASSWORD_DEFAULT))
                    .lastCredentialsChangeTime(Calendar.getInstance().getTimeInMillis())
                    .build();

            secondUser = User.builder()
                    .id(secondUserId)
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
                    .id(eventId)
                    .name(EVENT_NAME)
                    .shortDescription(EVENT_SHORT_DESCRIPTION)
                    .longDescription(EVENT_LONG_DESCRIPTION)
                    .createDate(new Date(Calendar.getInstance().getTimeInMillis()))
                    .eventStartDate(new Date(Calendar.getInstance().getTimeInMillis()+2000000))
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

            updatedEventDto = EventCreateDto.builder()
                    .name("changed name")
                    .shortDescription("changed short description")
                    .longDescription("changed long description")
                    .city("Krakow")
                    .exactAddress("changed exact address")
                    .tags(new ArrayList<>())
                    .eventStartDate(new Date(Calendar.getInstance().getTimeInMillis()+3000000))
                    .build();
            updatedEventDto.getTags().add("witam");
            updatedEventDto.getTags().add("zegnam");

            tagWitam = Tag.builder()
                    .id(tagWitamId)
                    .name("witam")
                    .events(new HashSet<>())
                    .build();
            tagZegnam = Tag.builder()
                    .id(tagZegnamId)
                    .name("zegnam")
                    .events(new HashSet<>())
                    .build();

            cityKrakow = City.builder()
                    .id(cityKrakowId)
                    .name("Krakow")
                    .events(new ArrayList<>())
                    .residents(new HashSet<>())
                    .build();
        }


        @DisplayName("When updating event should try to load event from database")
        @Test
        public void whenUpdatingEventShouldTryToLoadEventFromDatabase() {
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(tagRepository.findByIgnoreCaseName("witam")).thenReturn(Optional.of(tagWitam));
            when(tagRepository.findByIgnoreCaseName("zegnam")).thenReturn(Optional.of(tagZegnam));

            eventService.updateEvent(updatedEventDto, eventId, JWT_STRING);

            verify(eventRepository, times(1)).findById(eventId);

        }

        @DisplayName("When updating event should check if event with this id is empty")
        @Test
        public void whenUpdatingEventShouldCheckIfEventWithThisIdIsEmpty() {
            Optional<Event> eventOptionalSpy = Mockito.spy(eventOptional);

            when(eventRepository.findById(eventId)).thenReturn(eventOptionalSpy);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(tagRepository.findByIgnoreCaseName("witam")).thenReturn(Optional.of(tagWitam));
            when(tagRepository.findByIgnoreCaseName("zegnam")).thenReturn(Optional.of(tagZegnam));

            eventService.updateEvent(updatedEventDto, eventId, JWT_STRING);

            verify(eventOptionalSpy, times(1)).isEmpty();

        }

        @Test
        @DisplayName("When updating event should throw event not found exception if event does not exist")
        public void whenUpdatingEventShouldThrowEventNotFoundExceptionIfEventDoesNotExist() {
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            assertThrows(EventNotFoundException.class, () -> eventService.updateEvent(updatedEventDto, eventId, JWT_STRING));

        }

        @Test
        @DisplayName("When updating event should extract user email from jwt for ownership check")
        public void whenUpdatingEventShouldExtractUserEmailFromJwtForOwnershipCheck() {
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(tagRepository.findByIgnoreCaseName("witam")).thenReturn(Optional.of(tagWitam));
            when(tagRepository.findByIgnoreCaseName("zegnam")).thenReturn(Optional.of(tagZegnam));

            eventService.updateEvent(updatedEventDto, eventId, JWT_STRING);

            verify(jwtUtil, times(1)).extractUsername(JWT_STRING);

        }

        @Test
        @DisplayName("When updating event should find user with extracted email")
        public void whenUpdatingEventShouldFindUserWithExtractedEmail() {
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(tagRepository.findByIgnoreCaseName("witam")).thenReturn(Optional.of(tagWitam));
            when(tagRepository.findByIgnoreCaseName("zegnam")).thenReturn(Optional.of(tagZegnam));

            eventService.updateEvent(updatedEventDto, eventId, JWT_STRING);
            verify(userRepository, times(1)).findByEmail(EVENT_OWNER_EMAIL);

        }


        @Test
        @DisplayName("When updating event should retrieve user from optional object")
        public void whenUpdatingEventShouldRetrieveUserFromOptionalObject() {
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);

            Optional<User> eventOwnerOptionalSpy = Mockito.spy(Optional.of(eventOwner));
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptionalSpy);
            when(tagRepository.findByIgnoreCaseName("witam")).thenReturn(Optional.of(tagWitam));
            when(tagRepository.findByIgnoreCaseName("zegnam")).thenReturn(Optional.of(tagZegnam));

            eventService.updateEvent(updatedEventDto, eventId, JWT_STRING);

            verify(eventOwnerOptionalSpy, times(1)).get();
        }

        @Test
        @DisplayName("When updating event should check if is it owner performing update")
        public void whenUpdatingEventShouldCheckIfIsItOwnerPerformingUpdate() {
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(tagRepository.findByIgnoreCaseName("witam")).thenReturn(Optional.of(tagWitam));
            when(tagRepository.findByIgnoreCaseName("zegnam")).thenReturn(Optional.of(tagZegnam));

            try {
                eventService.updateEvent(updatedEventDto, eventId, JWT_STRING);
            } catch (NotEventOwnerException eventOwnerException) {
                fail("Request performer have to be owner of the event");
            }

        }

        @Test
        @DisplayName("When updating event should throw wrong event owner exception if user try to modify not his event")
        public void whenUpdatingEventShouldThrowWrongEventOwnerExceptionIfUserTryToModifyNotHisEvent() {
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(Optional.of(secondUser));

            assertThrows(NotEventOwnerException.class, () -> eventService.updateEvent(updatedEventDto, eventId, JWT_STRING));
        }

        @Test
        @DisplayName("When updating Event should update basic fields of event")
        public void whenUpdatingEventShouldUpdateFieldsOfEvent() {
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(anyString())).thenReturn(eventOwner.getEmail());
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(eventOwner));

            when(tagRepository.findByIgnoreCaseName("witam")).thenReturn(Optional.of(tagWitam));
            when(tagRepository.findByIgnoreCaseName("zegnam")).thenReturn(Optional.of(tagZegnam));
            when(cityUtils.resolveCity("Krakow")).thenReturn(cityKrakow);

            Event eventToBeSaved = eventOptional.get();

            eventService.updateEvent(updatedEventDto, eventId, JWT_STRING);


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
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));

            when(tagRepository.findByIgnoreCaseName("witam")).thenReturn(Optional.of(tagWitam));
            when(tagRepository.findByIgnoreCaseName("zegnam")).thenReturn(Optional.of(tagZegnam));
            when(cityUtils.resolveCity("Krakow")).thenReturn(cityKrakow);

            Event eventToBeSaved = eventOptional.get();

            eventService.updateEvent(updatedEventDto, eventId, JWT_STRING);

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
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));

            Event event = eventOptional.get();

            updatedEventDto.getTags().removeIf(tag -> tag.equals("witam"));
            updatedEventDto.getTags().add("java");

            when(tagRepository.findByIgnoreCaseName("zegnam")).thenReturn(Optional.of(tagZegnam));
            when(cityUtils.resolveCity("Krakow")).thenReturn(cityKrakow);

            eventService.updateEvent(updatedEventDto, eventId, JWT_STRING);

            Event eventToBeSaved = eventOptional.get();


            assertTrue(eventToBeSaved.getTags().contains(tagJava));
            assertTrue(tagJava.getEvents().contains(eventOptional.get()));

            assertFalse(eventToBeSaved.getTags().contains(tagSpring));
            assertFalse(tagSpring.getEvents().contains(eventOptional.get()));

            assertTrue(eventToBeSaved.getTags().contains(tagZegnam));
            assertTrue(tagZegnam.getEvents().contains(eventOptional.get()));

            assertFalse(eventToBeSaved.getTags().contains(tagWitam));
            assertFalse(tagWitam.getEvents().contains(eventOptional.get()));


        }

        @Test
        @DisplayName("When updating event should remove all tags if dto tag list is empty from event list of tags")
        public void whenUpdatingEventShouldRemoveAllTagsIfDtoTagListIsEmptyFromEventListOfTags() {
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));


            when(cityUtils.resolveCity("Krakow")).thenReturn(cityKrakow);

            updatedEventDto.getTags().clear();


            eventService.updateEvent(updatedEventDto, eventId, JWT_STRING);

            Event eventToBeSaved = eventOptional.get();

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
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));

            when(tagRepository.findByIgnoreCaseName("witam")).thenReturn(Optional.of(tagWitam));
            when(tagRepository.findByIgnoreCaseName("zegnam")).thenReturn(Optional.of(tagZegnam));
            when(cityUtils.resolveCity("Krakow")).thenReturn(cityKrakow);

            updatedEventDto.getTags().add("java");
            updatedEventDto.getTags().add("spring");

            eventService.updateEvent(updatedEventDto, eventId, JWT_STRING);

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
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));

            when(tagRepository.findByIgnoreCaseName("witam")).thenReturn(Optional.of(tagWitam));
            when(tagRepository.findByIgnoreCaseName("zegnam")).thenReturn(Optional.of(tagZegnam));
            when(cityUtils.resolveCity("Krakow")).thenReturn(cityKrakow);


            eventService.updateEvent(updatedEventDto, eventId, JWT_STRING);

            Event eventToBeSaved = eventOptional.get();
            verify(eventRepository, times(1)).save(any(Event.class));

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
            eventService = new EventServiceImpl(eventRepository, cityRepository, tagRepository, userRepository, threadRepository, threadReplyRepository, fileRepository, eventMapper, threadMapper, notificationService, cityUtils, jwtUtil, tikaFileTypeDetector);

            tagJava = Tag.builder()
                    .name("java")
                    .id(tagJavaId)
                    .events(new HashSet<>())
                    .build();
            tagSpring = Tag.builder()
                    .name("spring")
                    .id(tagSpringId)
                    .events(new HashSet<>())
                    .build();

            cityRzeszow = City.builder()
                    .id(cityRzeszowId)
                    .name("Rzeszow")
                    .events(new ArrayList<>())
                    .residents(new HashSet<>())
                    .build();

            eventOwner = User.builder()
                    .id(firstUserId)
                    .email("example@dot.com")
                    .role(Role.USER)
                    .firstName(EVENT_OWNER_FIRST_NAME)
                    .lastName(EVENT_OWNER_LAST_NAME)
                    .homeCity(cityRzeszow)
                    .attendingEvents(new ArrayList<>())
                    .userEvents(new ArrayList<>())
                    .password(passwordEncoder.encode(PASSWORD_DEFAULT))
                    .lastCredentialsChangeTime(Calendar.getInstance().getTimeInMillis())
                    .build();

            secondUser = User.builder()
                    .id(secondUserId)
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
                    .id(eventId)
                    .name(EVENT_NAME)
                    .shortDescription(EVENT_SHORT_DESCRIPTION)
                    .longDescription(EVENT_LONG_DESCRIPTION)
                    .createDate(new Date(Calendar.getInstance().getTimeInMillis()))
                    .eventStartDate(new Date(Calendar.getInstance().getTimeInMillis()+3000000))
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
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(Optional.of(secondUser));

            eventService.addAttenderToEvent(eventId, JWT_STRING);

            verify(eventRepository, times(1)).findById(eventId);

        }

        @Test
        @DisplayName("When adding attender should check if event optional is empty")
        public void whenAddingAttenderShouldCheckIfEventIsPresent() {
            Optional<Event> eventOptionalSpy = Mockito.spy(eventOptional);
            when(eventRepository.findById(eventId)).thenReturn(eventOptionalSpy);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(Optional.of(secondUser));

            eventService.addAttenderToEvent(eventId, JWT_STRING);

            verify(eventOptionalSpy, times(1)).isEmpty();
        }

        @Test
        @DisplayName("When adding attender should throw EventNotFound exception if event optional is empty")
        public void whenAddingAttenderShouldThrowEventNotFoundExceptionIfEventOptionalIsEmpty() {
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            assertThrows(EventNotFoundException.class, () -> eventService.addAttenderToEvent(eventId, JWT_STRING));
        }

        @Test
        @DisplayName("When adding attender should extract username from jwt")
        public void whenAddingAttenderShouldExtractUsernameFromJwt() {
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);

            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(Optional.of(secondUser));

            eventService.addAttenderToEvent(eventId, JWT_STRING);

            verify(jwtUtil, times(1)).extractUsername(JWT_STRING);

        }

        @Test
        @DisplayName("When adding attender should load user from database with extracted from jwt username")
        public void whenAddingAttenderShouldLoadUserFromDatabaseWithExtractedFromJwtUsername() {
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(Optional.of(secondUser));

            eventService.addAttenderToEvent(eventId, JWT_STRING);

        }

        @Test
        @DisplayName("When adding attender should retrieve user from optional object")
        public void whenAddingAttenderShouldRetrieveUserFromOptionalObject() {
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            Optional<User> secondUserOptionalSpy = Mockito.spy(Optional.of(secondUser));
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptionalSpy);

            eventService.addAttenderToEvent(eventId, JWT_STRING);

            verify(secondUserOptionalSpy, times(1)).get();

        }

        @Test
        @DisplayName("When adding attender should retrieve event from optional object")
        public void whenAddingAttenderShouldRetrieveEventFromOptionalObject() {
            Optional<Event> eventOptionalSpy = Mockito.spy(eventOptional);
            when(eventRepository.findById(eventId)).thenReturn(eventOptionalSpy);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(Optional.of(secondUser));

            eventService.addAttenderToEvent(eventId, JWT_STRING);

            verify(eventOptionalSpy, times(1)).get();

        }

        @Test
        @DisplayName("When adding attender should add retrieved user to event attender list")
        public void whenAddingAttenderShouldAddRetrievedUserToEventAttenderList() {
            Event eventSpy = Mockito.spy(eventOptional.get());
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(eventSpy));
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(Optional.of(secondUser));

            eventService.addAttenderToEvent(eventId, JWT_STRING);

            verify(eventSpy, times(1)).addAttendingUser(secondUser);

            assertTrue(secondUser.getAttendingEvents().contains(eventSpy));
            assertTrue(eventSpy.getAttendingUsers().contains(secondUser));

        }

        @Test
        @DisplayName("When adding attender should save changes to database")
        public void whenAddingAttenderShouldSaveChangesToDatabase() {
            Event eventSpy = Mockito.spy(eventOptional.get());
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(eventSpy));
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(Optional.of(secondUser));

            eventService.addAttenderToEvent(eventId, JWT_STRING);

            verify(eventRepository, times(1)).save(any(Event.class));

        }

    }

    /*
     ********************************************************************************************************************
     *                                       UPDATE THREAD IN EVENT
     ********************************************************************************************************************
     */
    @Nested
    @DisplayName("Thread create in event tests:")
    class ThreadCreateTests {
        @BeforeEach
        void setUp() {
            eventService = new EventServiceImpl(eventRepository, cityRepository, tagRepository, userRepository, threadRepository, threadReplyRepository, fileRepository,eventMapper, threadMapper, notificationService, cityUtils, jwtUtil, tikaFileTypeDetector);

            tagJava = Tag.builder()
                    .name("java")
                    .id(tagJavaId)
                    .events(new HashSet<>())
                    .build();
            tagSpring = Tag.builder()
                    .name("spring")
                    .id(tagSpringId)
                    .events(new HashSet<>())
                    .build();

            cityRzeszow = City.builder()
                    .id(cityRzeszowId)
                    .name("Rzeszow")
                    .events(new ArrayList<>())
                    .residents(new HashSet<>())
                    .build();

            eventOwner = User.builder()
                    .id(firstUserId)
                    .email("example@dot.com")
                    .role(Role.USER)
                    .firstName(EVENT_OWNER_FIRST_NAME)
                    .lastName(EVENT_OWNER_LAST_NAME)
                    .homeCity(cityRzeszow)
                    .attendingEvents(new ArrayList<>())
                    .userEvents(new ArrayList<>())
                    .password(passwordEncoder.encode(PASSWORD_DEFAULT))
                    .lastCredentialsChangeTime(Calendar.getInstance().getTimeInMillis())
                    .build();

            secondUser = User.builder()
                    .id(secondUserId)
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
                    .id(eventId)
                    .name(EVENT_NAME)
                    .shortDescription(EVENT_SHORT_DESCRIPTION)
                    .longDescription(EVENT_LONG_DESCRIPTION)
                    .createDate(new Date(Calendar.getInstance().getTimeInMillis()))
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
                    .id(threadId)
                    .event(eventOptional.get())
                    .owner(eventOwner)
                    .name(FIRST_THREAD_NAME)
                    .content(FIRST_THREAD_CONTENT)
                    .replies(new HashSet<>())
                    .createDate(Calendar.getInstance().getTime())
                    .editCounter(0)
                    .build();
            thread.setLastTimeEdited(thread.getCreateDate());

            threadReply = ThreadReply.builder()
                    .id(threadReplyId)
                    .thread(thread)
                    .content(REPLY_CONTENT)
                    .replayDate(Calendar.getInstance().getTime())
                    .replier(secondUser)
                    .editCounter(0)
                    .build();
            threadReply.setLastEditDate(threadReply.getReplayDate());

        }

        @Test
        @DisplayName("When creating thread should try to load event from database")
        public void whenCreatingThreadShouldTryToLoadEventFromDatabase() {
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);


            eventService.createThreadInEvent(threadCreateDto, eventId, JWT_STRING);

            verify(eventRepository, times(1)).findById(eventId);
        }

        @Test
        @DisplayName("When creating thread should check if returned event optional is not empty")
        public void whenCreatingThreadShouldCheckIfReturnedEventOptionalIsNotEmpty() {
            Optional<Event> eventOptionalSpy = Mockito.spy(eventOptional);

            when(eventRepository.findById(eventId)).thenReturn(eventOptionalSpy);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

            eventService.createThreadInEvent(threadCreateDto, eventId, JWT_STRING);

            verify(eventOptionalSpy, times(1)).isEmpty();
        }

        @Test
        @DisplayName("When creating thread should throw EventNotException if event optional is empty")
        public void whenCreatingThreadShouldThrowEventNotFoundExceptionIfEventOptionalIsEmpty() {
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            assertThrows(EventNotFoundException.class, () -> eventService.createThreadInEvent(threadCreateDto, eventId, JWT_STRING));
        }

        @Test
        @DisplayName("When creating thread should retrieve event from EventOptional object")
        public void whenCreatingThreadShouldRetrieveEventFromEventOptionalObject() {
            Optional<Event> eventOptionalSpy = Mockito.spy(eventOptional);

            when(eventRepository.findById(eventId)).thenReturn(eventOptionalSpy);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

            eventService.createThreadInEvent(threadCreateDto, eventId, JWT_STRING);

            verify(eventOptionalSpy, times(1)).get();
        }

        @Test
        @DisplayName("When creating thread should extract user email from jwt")
        public void whenCreatingThreadShouldExtractUserEmailFromJwt() {
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

            eventService.createThreadInEvent(threadCreateDto, eventId, JWT_STRING);

            verify(jwtUtil, times(1)).extractUsername(JWT_STRING);
        }

        @Test
        @DisplayName("When creating thread should find user in database")
        public void whenCreatingThreadShouldFindUserInDatabase() {
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

            eventService.createThreadInEvent(threadCreateDto, eventId, JWT_STRING);

            verify(userRepository, times(1)).findByEmail(secondUser.getEmail());
        }

        @Test
        @DisplayName("When creating thread should check if user is attending event")
        public void whenCreatingThreadShouldCheckIfUserIsAttendingEvent() {

            eventOptional = Optional.of(Mockito.spy(eventOptional.get()));

            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

            eventService.createThreadInEvent(threadCreateDto, eventId, JWT_STRING);

            verify(eventOptional.get(), times(1)).isUserAttending(secondUser);


        }

        @Test
        @DisplayName("When creating thread should throw NotAttenderException if user is not attending event")
        public void whenCreatingThreadShouldThrowNotAttenderExceptionIfUserIsNotAttendingEvent() {
            eventOptional.get().getAttendingUsers().remove(secondUser);
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

            assertThrows(NotAttenderException.class, () -> eventService.createThreadInEvent(threadCreateDto, eventId, JWT_STRING));
        }

        @Test
        @DisplayName("When creating thread should save new thread with data from dto")
        public void whenCreatingThreadShouldSaveNewThreadWithDataFromDto() {
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

            ArgumentCaptor<Thread> threadArgumentCaptor = ArgumentCaptor.forClass(Thread.class);

            eventService.createThreadInEvent(threadCreateDto, eventId, JWT_STRING);

            verify(threadRepository, times(1)).save(threadArgumentCaptor.capture());

            Thread threadToBeSaved = threadArgumentCaptor.getValue();
            assertEquals(threadCreateDto.getName(), threadToBeSaved.getName());
            assertEquals(threadCreateDto.getContent(), threadToBeSaved.getContent());
        }

        @Test
        @DisplayName("When creating thread should save new thread with set relationships")
        public void whenCreatingThreadShouldSaveNewThreadWithSetRelationships() {
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

            ArgumentCaptor<Thread> threadArgumentCaptor = ArgumentCaptor.forClass(Thread.class);

            eventService.createThreadInEvent(threadCreateDto, eventId, JWT_STRING);

            verify(threadRepository, times(1)).save(threadArgumentCaptor.capture());

            Thread threadToBeSaved = threadArgumentCaptor.getValue();
            assertEquals(eventOptional.get(), threadToBeSaved.getEvent());
            assertTrue(eventOptional.get().getThreads().contains(threadToBeSaved));

        }

        @Test
        @DisplayName("When creating thread should save new thread with set create date and edit date which have to be equal")
        public void whenCreatingThreadShouldSaveNewThreadWithSetCreateDateAndEditDateWhichHaveToBeEqual() {
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

            ArgumentCaptor<Thread> threadArgumentCaptor = ArgumentCaptor.forClass(Thread.class);

            eventService.createThreadInEvent(threadCreateDto, eventId, JWT_STRING);

            verify(threadRepository, times(1)).save(threadArgumentCaptor.capture());

            Thread threadToBeSaved = threadArgumentCaptor.getValue();
            assertNotNull(threadToBeSaved.getCreateDate());
            assertNotNull(threadToBeSaved.getLastTimeEdited());
            assertEquals(threadToBeSaved.getCreateDate(), threadToBeSaved.getLastTimeEdited());

        }

        @Test
        @DisplayName("When creating thread should save new thread with zeroed edit counter")
        public void whenCreatingThreadShouldSaveNewThreadWithZeroedEditCounter() {
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

            ArgumentCaptor<Thread> threadArgumentCaptor = ArgumentCaptor.forClass(Thread.class);

            eventService.createThreadInEvent(threadCreateDto, eventId, JWT_STRING);

            verify(threadRepository, times(1)).save(threadArgumentCaptor.capture());

            Thread threadToBeSaved = threadArgumentCaptor.getValue();
            assertEquals(eventOptional.get(), threadToBeSaved.getEvent());
            assertTrue(eventOptional.get().getThreads().contains(threadToBeSaved));

        }

        @Test
        @DisplayName("When creating thread should create new set for replays")
        public void whenCreatingThreadShouldCreateNewSetForRepliesToSave() {
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

            ArgumentCaptor<Thread> threadArgumentCaptor = ArgumentCaptor.forClass(Thread.class);

            eventService.createThreadInEvent(threadCreateDto, eventId, JWT_STRING);

            verify(threadRepository, times(1)).save(threadArgumentCaptor.capture());

            Thread threadToBeSaved = threadArgumentCaptor.getValue();
            assertNotNull(threadToBeSaved.getReplies());
        }
    }

    /*
     ********************************************************************************************************************
     *                                       CREATING THREAD IN EVENT
     ********************************************************************************************************************
     */
    @Nested
    @DisplayName("Thread update tests:")
    class ThreadUpdateTests {
        @BeforeEach
        void setUp() {
            eventService = new EventServiceImpl(eventRepository, cityRepository, tagRepository, userRepository, threadRepository, threadReplyRepository, fileRepository, eventMapper, threadMapper, notificationService, cityUtils, jwtUtil, tikaFileTypeDetector);

            tagJava = Tag.builder()
                    .name("java")
                    .id(tagJavaId)
                    .events(new HashSet<>())
                    .build();
            tagSpring = Tag.builder()
                    .name("spring")
                    .id(tagSpringId)
                    .events(new HashSet<>())
                    .build();

            cityRzeszow = City.builder()
                    .id(cityRzeszowId)
                    .name("Rzeszow")
                    .events(new ArrayList<>())
                    .residents(new HashSet<>())
                    .build();

            eventOwner = User.builder()
                    .id(firstUserId)
                    .email("example@dot.com")
                    .role(Role.USER)
                    .firstName(EVENT_OWNER_FIRST_NAME)
                    .lastName(EVENT_OWNER_LAST_NAME)
                    .homeCity(cityRzeszow)
                    .attendingEvents(new ArrayList<>())
                    .userEvents(new ArrayList<>())
                    .threads(new HashSet<>())
                    .password(passwordEncoder.encode(PASSWORD_DEFAULT))
                    .lastCredentialsChangeTime(Calendar.getInstance().getTimeInMillis())
                    .build();

            secondUser = User.builder()
                    .id(secondUserId)
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
                    .id(eventId)
                    .name(EVENT_NAME)
                    .shortDescription(EVENT_SHORT_DESCRIPTION)
                    .longDescription(EVENT_LONG_DESCRIPTION)
                    .createDate(new Date(Calendar.getInstance().getTimeInMillis()))
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
                    .id(threadId)
                    .event(eventOptional.get())
                    .owner(eventOwner)
                    .name(FIRST_THREAD_NAME)
                    .content(FIRST_THREAD_CONTENT)
                    .replies(new HashSet<>())
                    .createDate(new Date(System.currentTimeMillis()-1000))
                    .editCounter(0)
                    .build();
            thread.setLastTimeEdited(thread.getCreateDate());

            threadOptional = Optional.of(thread);

            eventOwner.addThread(thread);

            threadReply = ThreadReply.builder()
                    .id(threadReplyId)
                    .thread(thread)
                    .content(REPLY_CONTENT)
                    .replayDate(Calendar.getInstance().getTime())
                    .replier(secondUser)
                    .editCounter(0)
                    .build();
            threadReply.setLastEditDate(threadReply.getReplayDate());
        }

        @Test
        @DisplayName("When updating thread should find event by given id")
        public void whenUpdatingThreadShouldFindEventByGivenId(){
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);

            eventService.updateThreadInEvent(threadCreateDto, eventId, threadId, JWT_STRING);

            verify(eventRepository,times(1)).findById(eventId);
        }
        @Test
        @DisplayName("When updating thread should check is eventOptional not empty")
        public void whenUpdatingThreadShouldCheckIfEventOptionalIsNotEmpty(){
            Optional<Event> eventOptionalSpy = Mockito.spy(eventOptional);
            when(eventRepository.findById(eventId)).thenReturn(eventOptionalSpy);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);

            eventService.updateThreadInEvent(threadCreateDto, eventId, threadId, JWT_STRING);

            verify(eventOptionalSpy,times(1)).isEmpty();
        }
        @Test
        @DisplayName("When updating thread should throw EventNotFoundException if eventOptional is empty")
        public void whenUpdatingThreadShouldThrowEventNotFoundExceptionIfEventOptionalIsEmpty(){
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            assertThrows(EventNotFoundException.class, () -> eventService.updateThreadInEvent(threadCreateDto, eventId, threadId, JWT_STRING));

        }

        @Test
        @DisplayName("When updating thread should retrieve event from event optional")
        public void whenUpdatingThreadShouldRetrieveEventFromEventOptional(){
            Optional<Event> eventOptionalSpy = Mockito.spy(eventOptional);
            when(eventRepository.findById(eventId)).thenReturn(eventOptionalSpy);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);

            eventService.updateThreadInEvent(threadCreateDto, eventId, threadId, JWT_STRING);

            verify(eventOptionalSpy,times(1)).get();
        }

        @Test
        @DisplayName("When updating thread should extract user email from jwt")
        public void whenUpdatingThreadShouldExtractUserEmailFromJwt(){
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);

            eventService.updateThreadInEvent(threadCreateDto, eventId, threadId, JWT_STRING);

            verify(jwtUtil,times(1)).extractUsername(JWT_STRING);
        }

        @Test
        @DisplayName("When updating thread should try to load user from database")
        public void whenUpdatingThreadShouldTryLoadUserFromDatabase(){
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);

            eventService.updateThreadInEvent(threadCreateDto, eventId, threadId, JWT_STRING);

            verify(userRepository,times(1)).findByEmail(EVENT_OWNER_EMAIL);
        }

        @Test
        @DisplayName("When updating thread should retrieve user from user optional")
        public void whenUpdatingThreadShouldRetrieveUserFromUserOptional(){
            Optional<User> threadOwnerOptionalSpy = Mockito.spy(Optional.of(eventOwner));
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(threadOwnerOptionalSpy);
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);

            eventService.updateThreadInEvent(threadCreateDto, eventId, threadId, JWT_STRING);

            verify(threadOwnerOptionalSpy,times(1)).get();
        }
        @Test
        @DisplayName("When updating thread should try to load thread by given id")
        public void whenUpdatingThreadShouldTryToLoadThreadByGivenId(){
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);

            eventService.updateThreadInEvent(threadCreateDto, eventId, threadId, JWT_STRING);

            verify(threadRepository,times(1)).findById(threadId);
        }
        @Test
        @DisplayName("When updating thread should check if thread optional is empty")
        public void whenUpdatingThreadShouldCheckIfThreadOptionalIsEmpty(){
            Optional<Thread> threadOptionalSpy = Mockito.spy(threadOptional);
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(threadRepository.findById(threadId)).thenReturn(threadOptionalSpy);

            eventService.updateThreadInEvent(threadCreateDto, eventId, threadId, JWT_STRING);

            verify(threadOptionalSpy,times(1)).isEmpty();
        }
        @Test
        @DisplayName("When updating thread should throw ThreadNotFoundException if thread optional is empty")
        public void whenUpdatingThreadShouldThrowThreadNotFoundExceptionIfThreadOptionalIsEmpty(){
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(threadRepository.findById(threadId)).thenReturn(Optional.empty());

            assertThrows(ThreadNotFoundException.class, () -> eventService.updateThreadInEvent(threadCreateDto, eventId, threadId, JWT_STRING));
        }
        @Test
        @DisplayName("When updating thread should retrieve thread from thread optional")
        public void whenUpdatingThreadShouldRetrieveThreadFromThreadOptional(){
            Optional<Thread> threadOptionalSpy = Mockito.spy(threadOptional);
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(threadRepository.findById(threadId)).thenReturn(threadOptionalSpy);

            eventService.updateThreadInEvent(threadCreateDto, eventId, threadId, JWT_STRING);

            verify(threadOptionalSpy,times(1)).get();
        }
        @Test
        @DisplayName("When updating thread should check if owner of thread is still attending")
        public void whenUpdatingThreadShouldCheckIfOwnerOfThreadIsStillAttending(){
            Event eventSpy = Mockito.spy(eventOptional.get());
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(eventSpy));
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);

            eventService.updateThreadInEvent(threadCreateDto, eventId, threadId, JWT_STRING);

            verify(eventSpy,times(1)).isUserAttending(eventOwner);
        }
        @Test
        @DisplayName("When updating thread should Throw NotAttenderException if thread owner is not attending anymore")
        public void whenUpdatingThreadShouldThrowNotAttenderExceptionIfThreadOwnerIsNotAttendingAnymore(){
            eventOptional.get().removeAttendingUser(eventOwner);
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);

            assertThrows(NotAttenderException.class ,()->eventService.updateThreadInEvent(threadCreateDto, eventId, threadId, JWT_STRING));

        }
        @Test
        @DisplayName("When updating thread should check if user is owner of thread")
        public void whenUpdatingThreadShouldCheckIfUserIsOwnerOfThread(){
            Thread threadSpy = Mockito.spy(thread);
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(threadRepository.findById(threadId)).thenReturn(Optional.of(threadSpy));

            eventService.updateThreadInEvent(threadCreateDto, eventId, threadId, JWT_STRING);

            verify(threadSpy,times(1)).isUserOwner(eventOwner);
        }

        @Test
        @DisplayName("When updating thread should Throw NotThreadOwner if user is not owner of thread")
        public void whenUpdatingThreadShouldThrowNotThreadOwnerExceptionIfUserDoNotOwnThisThread(){
            thread.setOwner(secondUser);
            eventOwner.getThreads().remove(thread);
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);

            assertThrows(NotThreadOwnerException.class ,()->eventService.updateThreadInEvent(threadCreateDto, eventId, threadId, JWT_STRING));
        }
        @Test
        @DisplayName("When updating thread should use setters to update main fields")
        public void whenUpdatingThreadShouldUseSettersToUpdateMainFields(){
            threadCreateDto.setName("updated name");
            threadCreateDto.setContent("updated content");

            Thread threadSpy = Mockito.spy(thread);
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(threadRepository.findById(threadId)).thenReturn(Optional.of(threadSpy));

            eventService.updateThreadInEvent(threadCreateDto, eventId, threadId, JWT_STRING);

            verify(threadSpy,times(1)).setName(threadCreateDto.getName());
            verify(threadSpy,times(1)).setContent(threadCreateDto.getContent());
            assertEquals(threadCreateDto.getContent(),threadSpy.getContent());
            assertEquals(threadCreateDto.getName(),threadSpy.getName());
        }
        @Test
        @DisplayName("When updating thread should update lastTimeEdited field")
        public void whenUpdatingThreadShouldUpdateLastTimeEditedField(){
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);

            Date lastTimeEdited = thread.getLastTimeEdited();
            eventService.updateThreadInEvent(threadCreateDto, eventId, threadId, JWT_STRING);

            assertTrue(lastTimeEdited.getTime() < thread.getLastTimeEdited().getTime());
        }

        @Test
        @DisplayName("When updating thread should save updated entity")
        public void whenUpdatingThreadShouldSaveUpdatedEntity(){
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);

            eventService.updateThreadInEvent(threadCreateDto, eventId, threadId, JWT_STRING);

            verify(threadRepository,times(1)).save(thread);
        }
        @Test
        @DisplayName("When updating thread should update lastTimeEdited field")
        public void whenUpdatingThreadShouldMapSavedThreadToDto(){
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
            when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.of(eventOwner));
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);
            when(threadRepository.save(thread)).thenReturn(thread);

            eventService.updateThreadInEvent(threadCreateDto, eventId, threadId, JWT_STRING);

            verify(threadMapper,times(1)).mapThreadToThreadDto(thread);
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
            eventService = new EventServiceImpl(eventRepository, cityRepository, tagRepository, userRepository, threadRepository, threadReplyRepository, fileRepository, eventMapper, threadMapper, notificationService, cityUtils, jwtUtil, tikaFileTypeDetector);

            tagJava = Tag.builder()
                    .name("java")
                    .id(tagJavaId)
                    .events(new HashSet<>())
                    .build();
            tagSpring = Tag.builder()
                    .name("spring")
                    .id(tagSpringId)
                    .events(new HashSet<>())
                    .build();

            cityRzeszow = City.builder()
                    .id(cityRzeszowId)
                    .name("Rzeszow")
                    .events(new ArrayList<>())
                    .residents(new HashSet<>())
                    .build();

            eventOwner = User.builder()
                    .id(firstUserId)
                    .email("example@dot.com")
                    .role(Role.USER)
                    .firstName(EVENT_OWNER_FIRST_NAME)
                    .lastName(EVENT_OWNER_LAST_NAME)
                    .homeCity(cityRzeszow)
                    .attendingEvents(new ArrayList<>())
                    .userEvents(new ArrayList<>())
                    .threads(new HashSet<>())
                    .password(passwordEncoder.encode(PASSWORD_DEFAULT))
                    .lastCredentialsChangeTime(Calendar.getInstance().getTimeInMillis())
                    .build();

            secondUser = User.builder()
                    .id(secondUserId)
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
                    .id(eventId)
                    .name(EVENT_NAME)
                    .shortDescription(EVENT_SHORT_DESCRIPTION)
                    .longDescription(EVENT_LONG_DESCRIPTION)
                    .createDate(new Date(Calendar.getInstance().getTimeInMillis()))
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
                    .replyContent("Content of replay.")
                    .build();

            thread = Thread.builder()
                    .id(threadId)
                    .event(eventOptional.get())
                    .owner(eventOwner)
                    .name(FIRST_THREAD_NAME)
                    .content(FIRST_THREAD_CONTENT)
                    .replies(new HashSet<>())
                    .createDate(new Date(System.currentTimeMillis() - 1000))
                    .editCounter(0)
                    .build();
            thread.setLastTimeEdited(thread.getCreateDate());
            eventOptional.get().addAttendingUser(secondUser);
            eventOwner.addThread(thread);
            threadOptional = Optional.of(thread);
            threadReply = ThreadReply.builder()
                    .id(threadReplyId)
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
        public void whenCreatingReplayInThreadShouldFindEventWithGivenId(){
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);

            eventService.createReplyInThread(threadReplayCreateDto,eventId,threadId,JWT_STRING);

            verify(eventRepository, times(1)).findById(eventId);

        }
        @Test
        @DisplayName("When Creating reply in thread should check if event optional is empty")
        public void whenCreatingReplayInThreadShouldCheckIfEventOptionalIsEmpty(){
            Optional<Event> eventOptionalSpy = Mockito.spy(eventOptional);
            when(eventRepository.findById(eventId)).thenReturn(eventOptionalSpy);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);

            eventService.createReplyInThread(threadReplayCreateDto,eventId,threadId,JWT_STRING);

            verify(eventOptionalSpy, times(1)).isEmpty();
        }
        @Test
        @DisplayName("When Creating reply in thread should")
        public void whenCreatingReplayInThreadShouldThrowEventNotFoundExceptionIfThereIsNoEventWithThisId(){
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            assertThrows(EventNotFoundException.class, () -> eventService.createReplyInThread(threadReplayCreateDto,eventId,threadId,JWT_STRING));
        }
        @Test
        @DisplayName("When Creating reply in thread should retrieve event from optional object")
        public void whenCreatingReplayInThreadShouldRetrieveEventFromOptionalObject(){
            Optional<Event> eventOptionalSpy = Mockito.spy(eventOptional);
            when(eventRepository.findById(eventId)).thenReturn(eventOptionalSpy);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);

            eventService.createReplyInThread(threadReplayCreateDto,eventId,threadId,JWT_STRING);

            verify(eventOptionalSpy, times(1)).get();

        }

        @Test
        @DisplayName("When Creating reply in thread should extract user email from jwt")
        public void whenCreatingReplayInThreadShouldExtractUserEmailFromJwt(){
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);

            eventService.createReplyInThread(threadReplayCreateDto,eventId,threadId,JWT_STRING);

            verify(jwtUtil,times(1)).extractUsername(JWT_STRING);
        }
        @Test
        @DisplayName("When Creating reply in thread should look up for user by extracted email and retrieve it from optional object")
        public void whenCreatingReplayInThreadShouldLookForUserByExtractedEmail(){
            Optional<User> secondUserOptionalSpy = Mockito.spy(secondUserOptional);
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptionalSpy);
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);

            eventService.createReplyInThread(threadReplayCreateDto,eventId,threadId,JWT_STRING);

            verify(userRepository, times(1)).findByEmail(SECOND_USER_EMAIL);
            verify(secondUserOptionalSpy, times(1)).get();
        }
        @Test
        @DisplayName("When Creating reply in thread should check if replying user is attending event")
        public void whenCreatingReplayInThreadShouldCheckIfReplyingUserIsAttendingEvent(){
            Event eventSpy = Mockito.spy(eventOptional.get());
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(eventSpy));
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);

            eventService.createReplyInThread(threadReplayCreateDto,eventId,threadId,JWT_STRING);

            verify(eventSpy, times(1)).isUserAttending(secondUser);
        }
        @Test
        @DisplayName("When Creating reply in thread should throw NotAttenderException if user is not attending event")
        public void whenCreatingReplayInThreadShouldThrowNotAttenderExceptionIfUserIsNotAttendingEvent(){
            eventOptional.get().removeAttendingUser(secondUser);
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);


            assertThrows(NotAttenderException.class, () -> eventService.createReplyInThread(threadReplayCreateDto,eventId,threadId,JWT_STRING));
        }
        @Test
        @DisplayName("When Creating reply in thread should load thread with given id")
        public void whenCreatingReplayInThreadShouldLookUpThread(){
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);

            eventService.createReplyInThread(threadReplayCreateDto,eventId,threadId,JWT_STRING);

            verify(threadRepository, times(1)).findById(threadId);
        }
        @Test
        @DisplayName("When Creating reply in thread should check if thread optional is empty")
        public void whenCreatingReplayInThreadShouldCheckIfThreadOptionalIsEmpty(){
            Optional<Thread>  threadOptionalSpy = Mockito.spy(threadOptional);
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(threadOptionalSpy);

            eventService.createReplyInThread(threadReplayCreateDto,eventId,threadId,JWT_STRING);

            verify(threadOptionalSpy, times(1)).isEmpty();
        }
        @Test
        @DisplayName("When Creating reply in thread should throw ThreadNotFoundException if there is no thread with given id")
        public void whenCreatingReplayInThreadShouldThrowThreadNotFoundExceptionIfThereIsNoThreadWithGivenId(){
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(Optional.empty());

            assertThrows(ThreadNotFoundException.class, () -> eventService.createReplyInThread(threadReplayCreateDto,eventId,threadId,JWT_STRING));


        }

        @Test
        @DisplayName("When Creating reply in thread should set up ne ThreadReply object and save it with all necessary fields")
        public void whenCreatingReplayInThreadShouldSetUpNewThreadReplyObjectAndSaveItWithAllNecessaryFields(){
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);

            ArgumentCaptor<ThreadReply> threadReplyArgumentCaptor = ArgumentCaptor.forClass(ThreadReply.class);

            eventService.createReplyInThread(threadReplayCreateDto,eventId,threadId,JWT_STRING);

            verify(threadReplyRepository, times(1)).save(threadReplyArgumentCaptor.capture());
            ThreadReply capturedReply  = threadReplyArgumentCaptor.getValue();

            assertEquals(threadReplayCreateDto.getReplyContent(),capturedReply.getContent());
            assertEquals(threadId, capturedReply.getThread().getId());
            assertEquals(secondUser, capturedReply.getReplier());
            assertEquals(0, capturedReply.getEditCounter());
            assertNotNull(capturedReply.getReplayDate());
            assertNotNull(capturedReply.getLastEditDate());
            assertEquals(capturedReply.getReplayDate(),capturedReply.getLastEditDate());

        }

        @Test
        @DisplayName("When Creating reply in thread should map thread to dto object")
        public void whenCreatingReplayInThreadShouldMapThreadToDtoObject(){
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);

            eventService.createReplyInThread(threadReplayCreateDto,eventId,threadId,JWT_STRING);

            verify(threadMapper, times(1)).mapThreadToThreadDto(threadOptional.get());

        }
    }
    /*
     ********************************************************************************************************************
     *                                       CREATING REPLAY IN THREAD
     ********************************************************************************************************************
     */
    @Nested
    @DisplayName("Update reply in thread test:")
    class UpdateReplayInThreadTests {
        @BeforeEach
        void setUp() {
            eventService = new EventServiceImpl(eventRepository, cityRepository, tagRepository, userRepository, threadRepository, threadReplyRepository, fileRepository, eventMapper, threadMapper, notificationService,cityUtils, jwtUtil, tikaFileTypeDetector);

            tagJava = Tag.builder()
                    .name("java")
                    .id(tagJavaId)
                    .events(new HashSet<>())
                    .build();
            tagSpring = Tag.builder()
                    .name("spring")
                    .id(tagSpringId)
                    .events(new HashSet<>())
                    .build();

            cityRzeszow = City.builder()
                    .id(cityRzeszowId)
                    .name("Rzeszow")
                    .events(new ArrayList<>())
                    .residents(new HashSet<>())
                    .build();

            eventOwner = User.builder()
                    .id(firstUserId)
                    .email("example@dot.com")
                    .role(Role.USER)
                    .firstName(EVENT_OWNER_FIRST_NAME)
                    .lastName(EVENT_OWNER_LAST_NAME)
                    .homeCity(cityRzeszow)
                    .attendingEvents(new ArrayList<>())
                    .userEvents(new ArrayList<>())
                    .threads(new HashSet<>())
                    .password(passwordEncoder.encode(PASSWORD_DEFAULT))
                    .lastCredentialsChangeTime(Calendar.getInstance().getTimeInMillis())
                    .build();

            secondUser = User.builder()
                    .id(secondUserId)
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
                    .id(eventId)
                    .name(EVENT_NAME)
                    .shortDescription(EVENT_SHORT_DESCRIPTION)
                    .longDescription(EVENT_LONG_DESCRIPTION)
                    .createDate(new Date(Calendar.getInstance().getTimeInMillis()))
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
                    .id(threadId)
                    .event(eventOptional.get())
                    .owner(eventOwner)
                    .name(FIRST_THREAD_NAME)
                    .content(FIRST_THREAD_CONTENT)
                    .replies(new HashSet<>())
                    .createDate(new Date(System.currentTimeMillis() - 1000))
                    .editCounter(0)
                    .build();
            thread.setLastTimeEdited(thread.getCreateDate());
            eventOptional.get().addAttendingUser(secondUser);
            eventOwner.addThread(thread);
            threadOptional = Optional.of(thread);
            threadReply = ThreadReply.builder()
                    .id(threadReplyId)
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
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(threadReplyId)).thenReturn(threadReplyOptional);

            eventService.updateThreadReplyInEvent(threadReplayCreateDto,eventId,threadId,threadReplyId, JWT_STRING);

            verify(eventRepository, times(1)).findById(eventId);
        }

        @Test
        @DisplayName("When updating reply in thread should check if event optional object is empty")
        public void whenUpdatingReplyInThreadShouldCheckIfEventOptionalObjectIsEmpty() {
            Optional<Event> eventOptionalSpy = Mockito.spy(eventOptional);
            when(eventRepository.findById(eventId)).thenReturn(eventOptionalSpy);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(threadReplyId)).thenReturn(threadReplyOptional);

            eventService.updateThreadReplyInEvent(threadReplayCreateDto,eventId,threadId,threadReplyId, JWT_STRING);

            verify(eventOptionalSpy, times(1)).isEmpty();

        }

        @Test
        @DisplayName("When updating reply in thread should throw EventNotFoundException if there is no event with given id")
        public void whenUpdatingReplyInThreadShouldThrowEventNotFoundExceptionIfThereIsNoEventWithGivenId() {
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            assertThrows(EventNotFoundException.class, () -> eventService.updateThreadReplyInEvent(threadReplayCreateDto,eventId,threadId,threadReplyId, JWT_STRING));
        }

        @Test
        @DisplayName("When updating reply in thread should retrieve event from optional object")
        public void whenUpdatingReplyInThreadShouldRetrieveEventFromOptionalObject() {
            Optional<Event> eventOptionalSpy = Mockito.spy(eventOptional);
            when(eventRepository.findById(eventId)).thenReturn(eventOptionalSpy);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(threadReplyId)).thenReturn(threadReplyOptional);

            eventService.updateThreadReplyInEvent(threadReplayCreateDto,eventId,threadId,threadReplyId, JWT_STRING);

            verify(eventOptionalSpy, times(1)).get();
        }
        @Test
        @DisplayName("When updating reply in thread should extract user email from jwt")
        public void whenUpdatingReplyInThreadShouldExtractUserEmailFromJwt() {
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(threadReplyId)).thenReturn(threadReplyOptional);

            eventService.updateThreadReplyInEvent(threadReplayCreateDto,eventId,threadId,threadReplyId, JWT_STRING);

            verify(jwtUtil,times(1)).extractUsername(JWT_STRING);
        }
        @Test
        @DisplayName("When updating reply in thread should retrieve user from optional object")
        public void whenUpdatingReplyInThreadShouldRetrieveUserFromOptionalObject() {
            Optional<User> secondUserOptionalSpy = Mockito.spy(secondUserOptional);
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptionalSpy);
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(threadReplyId)).thenReturn(threadReplyOptional);

            eventService.updateThreadReplyInEvent(threadReplayCreateDto,eventId,threadId,threadReplyId, JWT_STRING);

            verify(secondUserOptionalSpy,times(1)).get();
        }


        @Test
        @DisplayName("When updating reply in thread should check if user is attending event")
        public void whenUpdatingReplyInThreadShouldCheckIfUserIsAttendingEvent() {
            Event eventSpy = Mockito.spy(eventOptional.get());
            eventOptional = Optional.of(eventSpy);
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(threadReplyId)).thenReturn(threadReplyOptional);

            eventService.updateThreadReplyInEvent(threadReplayCreateDto,eventId,threadId,threadReplyId, JWT_STRING);

            verify(eventSpy, times(1)).isUserAttending(secondUser);
        }
        @Test
        @DisplayName("When updating reply in thread should throw NotAttenderException if user is not attending event")
        public void whenUpdatingReplyInThreadShouldThrowNotAttenderExceptionIfUserIsNotAttendingEvent() {
            eventOptional.get().removeAttendingUser(secondUser);
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

            assertThrows(NotAttenderException.class, () -> eventService.updateThreadReplyInEvent(threadReplayCreateDto,eventId,threadId,threadReplyId, JWT_STRING));
        }


        @Test
        @DisplayName("When updating reply in thread should look up for thread with given id")
        public void whenUpdatingReplyInThreadShouldLookUpForThreadWithGivenId() {
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(threadReplyId)).thenReturn(threadReplyOptional);

            eventService.updateThreadReplyInEvent(threadReplayCreateDto,eventId,threadId,threadReplyId, JWT_STRING);

            verify(threadRepository, times(1)).findById(threadId);
        }
        @Test
        @DisplayName("When updating reply in thread should check if thread optional object is empty")
        public void whenUpdatingReplyInThreadShouldCheckIfThreadOptionalObjectIsEmpty() {
            Optional<Thread> threadOptionalSpy = Mockito.spy(threadOptional);
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(threadOptionalSpy);
            when(threadReplyRepository.findById(threadReplyId)).thenReturn(threadReplyOptional);

            eventService.updateThreadReplyInEvent(threadReplayCreateDto,eventId,threadId,threadReplyId, JWT_STRING);
            verify(threadOptionalSpy, times(1)).isEmpty();
        }
        @Test
        @DisplayName("When updating reply in thread should throw ThreadNotFoundException if there is no thread with that id")
        public void whenUpdatingReplyInThreadShouldThrowThreadNotFoundExceptionIfThereIsNoThreadWithThatId() {
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(Optional.empty());

            assertThrows(ThreadNotFoundException.class, () -> eventService.updateThreadReplyInEvent(threadReplayCreateDto,eventId,threadId,threadReplyId, JWT_STRING));

        }
        @Test
        @DisplayName("When updating reply in thread should retrieve thread from optional object")
        public void whenUpdatingReplyInThreadShouldRetrieveThreadFromOptionalObject() {
            Optional<Thread> threadOptionalSpy = Mockito.spy(threadOptional);
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(threadOptionalSpy);
            when(threadReplyRepository.findById(threadReplyId)).thenReturn(threadReplyOptional);

            eventService.updateThreadReplyInEvent(threadReplayCreateDto,eventId,threadId,threadReplyId, JWT_STRING);

            verify(threadOptionalSpy, times(1)).get();
        }
        @Test
        @DisplayName("When updating reply in thread should look up for thread reply with given id")
        public void whenUpdatingReplyInThreadShouldLookUpForThreadReplyWithGivenId() {
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(threadReplyId)).thenReturn(threadReplyOptional);

            eventService.updateThreadReplyInEvent(threadReplayCreateDto,eventId,threadId,threadReplyId, JWT_STRING);

            verify(threadReplyRepository, times(1)).findById(threadReplyId);
        }
        @Test
        @DisplayName("When updating reply in thread should check if thread reply object is empty")
        public void whenUpdatingReplyInThreadShouldCheckIfThreadReplyObjectIsEmpty() {
            Optional<ThreadReply> threadReplyOptionalSpy = Mockito.spy(threadReplyOptional);
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(threadReplyId)).thenReturn(threadReplyOptionalSpy);

            eventService.updateThreadReplyInEvent(threadReplayCreateDto,eventId,threadId,threadReplyId, JWT_STRING);

            verify(threadReplyOptionalSpy, times(1)).isEmpty();
        }

        @Test
        @DisplayName("When updating reply in thread should throw ThreadReplyNotFoundException if thread reply optional object is empty")
        public void whenUpdatingReplyInThreadShouldThrowThreadReplyNotFoundExceptionIfThreadReplyOptionalObjectIsEmpty() {
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(threadReplyId)).thenReturn(Optional.empty());


            assertThrows(ThreadReplyNotFoundException.class, () -> eventService.updateThreadReplyInEvent(threadReplayCreateDto,eventId,threadId,threadReplyId, JWT_STRING));
        }
        @Test
        @DisplayName("When updating reply in thread should retrieve thread reply object from thread reply optional object")
        public void whenUpdatingReplyInThreadShouldRetrieveThreadReplyObjectFromThreadReplyOptionalObject() {
            Optional<ThreadReply> threadReplyOptionalSpy = Mockito.spy(threadReplyOptional);
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(threadReplyId)).thenReturn(threadReplyOptionalSpy);


            eventService.updateThreadReplyInEvent(threadReplayCreateDto,eventId,threadId,threadReplyId, JWT_STRING);

            verify(threadReplyOptionalSpy, times(1)).get();
        }
       @Test
        @DisplayName("When updating reply in thread should check if thread with given id contains thread reply with given id")
        public void whenUpdatingReplyInThreadShouldCheckIfThreadWithGivenIdContainsReplyWithGivenId() {
            Thread threadSpy = Mockito.spy(thread);
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(Optional.of(threadSpy));
            when(threadReplyRepository.findById(threadReplyId)).thenReturn(threadReplyOptional);


           eventService.updateThreadReplyInEvent(threadReplayCreateDto,eventId,threadId,threadReplyId, JWT_STRING);

            verify(threadSpy, times(1)).containsReply(threadReply);
        }
        @Test
        @DisplayName("When updating reply in thread should throw WrongThreadException if thread doesn't contain this thread reply")
        public void whenUpdatingReplyInThreadShouldThrowWrongThreadExceptionIfThreadDoesntContainThisThreadReply() {
            thread.getReplies().remove(threadReply);
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(threadReplyId)).thenReturn(threadReplyOptional);


            assertThrows(WrongThreadException.class, () -> eventService.updateThreadReplyInEvent(threadReplayCreateDto,eventId,threadId,threadReplyId, JWT_STRING));
        }

        @Test
        @DisplayName("When updating reply in thread should check if performing user is owner of thread reply being updated")
        public void whenUpdatingReplyInThreadShouldCheckIfPerformingUserIsOwnerOfThreadReplyBeingUpdated(){
            ThreadReply threadReplySpy = Mockito.spy(threadReply);
            thread.getReplies().remove(threadReply);
            thread.addReplayToThread(threadReplySpy);

            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(threadReplyId)).thenReturn(Optional.of(threadReplySpy));

            eventService.updateThreadReplyInEvent(threadReplayCreateDto,eventId,threadId,threadReplyId, JWT_STRING);

            verify(threadReplySpy, times(1)).isReplier(secondUser);
        }

        @Test
        @DisplayName("When updating reply in thread should check if performing user is owner of thread reply being updated")
        public void whenUpdatingReplyInThreadShouldThrowNotThreadReplyOwnerExceptionIfUserTryToUpdateNotHisReply(){
            threadReply.setReplier(eventOwner);
            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(threadReplyId)).thenReturn(threadReplyOptional);

            assertThrows(NotThreadReplyOwnerException.class, () -> eventService.updateThreadReplyInEvent(threadReplayCreateDto,eventId,threadId,threadReplyId, JWT_STRING));
        }
        @Test
        @DisplayName("When updating reply in thread should update content and edit counter  in stored thread reply and save it")
        public void whenUpdatingReplyInThreadShouldUpdateContentAndEditCounterInStoredThreadReply(){
            ThreadReply threadReplySpy = Mockito.spy(threadReply);
            thread.getReplies().remove(threadReply);
            thread.addReplayToThread(threadReplySpy);

            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(threadReplyId)).thenReturn(Optional.of(threadReplySpy));

            ArgumentCaptor<ThreadReply> threadReplyArgumentCaptor = ArgumentCaptor.forClass(ThreadReply.class);

            eventService.updateThreadReplyInEvent(threadReplayCreateDto,eventId,threadId,threadReplyId, JWT_STRING);

            verify(threadReplyRepository, times(1)).save(threadReplyArgumentCaptor.capture());
            var updatedThreadReply = threadReplyArgumentCaptor.getValue();
            verify(threadReplySpy, times(1)).incrementEditCounter();
            assertEquals(threadReplayCreateDto.getReplyContent(),updatedThreadReply.getContent());
        }
        @Test
        @DisplayName("When updating reply in thread should map thread to dto")
        public void whenUpdatingReplyInThreadShouldMapThreadToDto(){

            when(eventRepository.findById(eventId)).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
            when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
            when(threadRepository.findById(threadId)).thenReturn(threadOptional);
            when(threadReplyRepository.findById(threadReplyId)).thenReturn(threadReplyOptional);

            eventService.updateThreadReplyInEvent(threadReplayCreateDto,eventId,threadId,threadReplyId, JWT_STRING);

           verify(threadMapper, times(1)).mapThreadToThreadDto(thread);
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