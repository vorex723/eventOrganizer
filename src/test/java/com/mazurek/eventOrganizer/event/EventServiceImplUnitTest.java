package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.TestFileContentFactory;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.city.CityUtils;
import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.dto.EventDto;
import com.mazurek.eventOrganizer.exception.event.*;
import com.mazurek.eventOrganizer.exception.file.EmptyUploadedFileException;
import com.mazurek.eventOrganizer.exception.file.FileNotFoundInEventException;
import com.mazurek.eventOrganizer.exception.file.FileTypeNotAllowedException;
import com.mazurek.eventOrganizer.exception.thread.*;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.file.File;
import com.mazurek.eventOrganizer.file.FileRepository;
import com.mazurek.eventOrganizer.file.FileUploadDto;
import com.mazurek.eventOrganizer.jwt.JwtUtil;
import com.mazurek.eventOrganizer.notification.NotificationServiceImpl;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.tag.TagRepository;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.thread.ThreadReply;
import com.mazurek.eventOrganizer.thread.ThreadReplyRepository;
import com.mazurek.eventOrganizer.thread.ThreadRepository;
import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadReplyCreateDto;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import com.mazurek.eventOrganizer.utils.FileUtils;
import org.apache.http.entity.ContentType;
import org.apache.tika.Tika;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
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
    private final UUID FILE_ID = UUID.randomUUID();


    private final String JWT_STRING = "randomStringForJwt";

    private final String EVENT_OWNER_EMAIL = "example@dot.com";
    private final String EVENT_OWNER_FIRST_NAME = "Andrew";
    private final String EVENT_OWNER_LAST_NAME = "Golota";

    private final String SECOND_USER_FIRST_NAME = "Andrzej";
    private final String SECOND_USER_LAST_NAME = "Wesoly";
    private final String SECOND_USER_EMAIL = "notExample@dot.com";

    private final String PASSWORD_DEFAULT = "password";

    private final String EVENT_NAME = "First Event";
    private final String EVENT_SHORT_DESCRIPTION = "short description should be short";
    private final String EVENT_LONG_DESCRIPTION = "long description can be quite long, and it Sho◘uld be. maybe i should put Lorem Ipsum here.";
    private final String EVENT_EXACT_ADDRESS = "ul. Dąbrowskiego 3";

    private final String EVENT_NAME_UPDATE = "First Event update";
    private final String EVENT_SHORT_DESCRIPTION_UPDATE = "Update short description should be short";
    private final String EVENT_LONG_DESCRIPTION_UPDATE = "Updated long description can be quite long, and it Should be. maybe i should put Lorem Ipsum here.";
    private final String EVENT_EXACT_ADDRESS_UPDATE = "ul. Updated 2";

    private final String CITY_KRAKOW_NAME = "krakow";
    private final String CITY_RZESZOW_NAME = "rzeszow";

    private final String TAG_JAVA_NAME = "java";
    private final String TAG_SPRING_NAME = "spring";
    private final String TAG_WITAM_NAME = "witam";
    private final String TAG_ZEGNAM_NAME = "zegnam";

    private final String FIRST_THREAD_NAME = "First thread ever";
    private final String FIRST_THREAD_CONTENT = "First thread content for testing purpose. It have to be containing several words.";
    private final String EVENT_CREATE_DTO_CITY = CITY_RZESZOW_NAME;
    private final String THREAD_REPLY_CONTENT = "This is first replay in thread";
    private final String THREAD_REPLY_CONTENT_UPDATE = "This is updated content of the reply in thread";
    private final String FILE_NAME = "File name";

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
    private NotificationServiceImpl notificationService;
    @Mock
    private JwtUtil jwtUtil;
    private BCryptPasswordEncoder passwordEncoder = Mockito.spy(new BCryptPasswordEncoder());
    private final Tika tikaFileTypeDetector = new Tika();
    private final FileUtils fileUtils = new FileUtils(tikaFileTypeDetector);

    private User eventOwner;
    private User secondUser;
    private Event event;

    private Tag tagJava;
    private Tag tagSpring;
    private Tag tagWitam;
    private Tag tagZegnam;

    private City cityKrakow;
    private City cityRzeszow;

    private Thread thread;
    private ThreadReply threadReply;


    private EventCreateDto eventCreateDto;
    private EventCreateDto updatedEventDto;
    private ThreadCreateDto threadCreateDto;
    private ThreadReplyCreateDto threadReplyCreateDto;

    private Optional<User> eventOwnerOptional;
    private Optional<User> secondUserOptional;

    private Optional<Event> eventOptional;
    private Optional<Tag> tagJavaOptional;
    private Optional<Tag> tagSpringOptional;
    private Optional<Tag> tagWitamOptional;
    private Optional<Tag> tagZegnamOptional;

    private Optional<City> cityRzeszowOptional;
    private Optional<City> cityKrakowOptional;

    private Optional<Thread> threadOptional;
    private Optional<ThreadReply> threadReplyOptional;


    @Nested
    @DisplayName("Event core tests: ")
    class EventCoreTests {

        @BeforeEach
        void setUp() {
            eventService = new EventServiceImpl(eventRepository, cityRepository, tagRepository, userRepository, threadRepository, threadReplyRepository, fileRepository, notificationService, jwtUtil, fileUtils);

            cityRzeszow = City.builder()
                    .id(CITY_RZESZOW_ID)
                    .name(CITY_RZESZOW_NAME)
                    .residents(new HashSet<>())
                    .events(new ArrayList<>())
                    .build();
            cityRzeszowOptional = Optional.of(cityRzeszow);

            cityKrakow = City.builder()
                    .id(CITY_KRAKOW_ID)
                    .name(CITY_KRAKOW_NAME)
                    .residents(new HashSet<>())
                    .events(new ArrayList<>())
                    .build();
            cityKrakowOptional = Optional.of(cityKrakow);

            tagJava = Tag.builder()
                    .name(TAG_JAVA_NAME)
                    .id(TAG_JAVA_ID)
                    .events(new HashSet<>())
                    .build();
            tagJavaOptional = Optional.of(tagJava);

            tagSpring = Tag.builder()
                    .name(TAG_SPRING_NAME)
                    .id(TAG_SPRING_ID)
                    .events(new HashSet<>())
                    .build();
            tagSpringOptional = Optional.of(tagSpring);

            tagWitam = Tag.builder()
                    .name(TAG_WITAM_NAME)
                    .id(TAG_WITAM_ID)
                    .events(new HashSet<>())
                    .build();
            tagWitamOptional = Optional.of(tagWitam);

            tagZegnam = Tag.builder()
                    .name(TAG_ZEGNAM_NAME)
                    .id(TAG_ZEGNAM_ID)
                    .events(new HashSet<>())
                    .build();
            tagZegnamOptional = Optional.of(tagZegnam);


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
                    .build();
            secondUserOptional = Optional.of(secondUser);

            event = Event.builder()
                    .id(EVENT_ID)
                    .name(EVENT_NAME)
                    .owner(eventOwner)
                    .shortDescription(EVENT_SHORT_DESCRIPTION)
                    .longDescription(EVENT_LONG_DESCRIPTION)
                    .createDate(ZonedDateTime.now())
                    .timeZoneId(ZonedDateTime.now().getZone().getId())
                    .eventStartDate(ZonedDateTime.now().withSecond(0).withNano(0).plusDays(7))
                    .tags(new HashSet<>())
                    .attendingUsers(new HashSet<>())
                    .threads(new HashSet<>())
                    .city(cityRzeszow)
                    .exactAddress(EVENT_EXACT_ADDRESS)
                    .build();
            event.setLastUpdate(event.getCreateDate());

            eventOptional = Optional.of(event);

            event.addTag(tagJava);
            event.addTag(tagSpring);

            cityRzeszow.addEvent(event);

            eventCreateDto = EventCreateDto.builder()
                    .name(EVENT_NAME)
                    .shortDescription(EVENT_SHORT_DESCRIPTION)
                    .longDescription(EVENT_LONG_DESCRIPTION)
                    .tags(new ArrayList<>())
                    .city(EVENT_CREATE_DTO_CITY)
                    .exactAddress(EVENT_EXACT_ADDRESS)
                    .eventStartDate(ZonedDateTime.now().plusDays(7))
                    .build();
            eventCreateDto.getTags().add(TAG_JAVA_NAME);
            eventCreateDto.getTags().add(TAG_SPRING_NAME);

        }



        @Nested
        @DisplayName("Get event by id test")
        class GettingEventByIdTests {

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


        @Nested
        @DisplayName("Create event tests")
        class CreateEventTest {

            @Test
            @DisplayName("When creating event should retrieve user from database by email extracted from jwt")
            public void whenCreatingEventShouldRetrieveUserFromDatabaseByEmailExtractedFromJwt(){
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(cityRepository.findByIgnoreCaseName(CITY_RZESZOW_NAME)).thenReturn(cityRzeszowOptional);
                when(tagRepository.findByIgnoreCaseName(TAG_JAVA_NAME)).thenReturn(tagJavaOptional);
                when(tagRepository.findByIgnoreCaseName(TAG_SPRING_NAME)).thenReturn(tagSpringOptional);
                when(eventRepository.save(any())).thenReturn(event);

                eventService.createEvent(eventCreateDto, JWT_STRING);

                verify(jwtUtil, times(1)).extractUsername(JWT_STRING);
                verify(userRepository, times(1)).findByEmail(EVENT_OWNER_EMAIL);

            }

            @Test
            @DisplayName("When creating event should throw UserNotFoundException when there is no user with extracted email")
            public void whenCreatingEventShouldThrowUserNotFoundExceptionWhenThereIsNoUserWithExtractedEmail() {
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(Optional.empty());

                assertThrows(UserNotFoundException.class, () ->  eventService.createEvent(eventCreateDto, JWT_STRING));
            }

            @Test
            @DisplayName("When creating event should create empty set for tags if tag list in dto is empty")
            public void whenCreatingEventShouldCreateEmptySetForTagsIfTagListInDtoIsEmpty() {
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(cityRepository.findByIgnoreCaseName(CITY_RZESZOW_NAME)).thenReturn(cityRzeszowOptional);
                when(eventRepository.save(any())).thenReturn(event);

                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);

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
                when(cityRepository.findByIgnoreCaseName(CITY_RZESZOW_NAME)).thenReturn(cityRzeszowOptional);
                when(eventRepository.save(any())).thenReturn(event);
                when(userRepository.findByEmail(anyString())).thenReturn(eventOwnerOptional);

                ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
                eventCreateDto.getTags().clear();
                eventService.createEvent(eventCreateDto, JWT_STRING);

                verify(eventRepository, times(1)).save(eventArgumentCaptor.capture());
                Event capturedEvent = eventArgumentCaptor.getValue();
                assertNotNull(capturedEvent.getAttendingUsers());
                assertTrue(capturedEvent.getAttendingUsers().isEmpty());
            }


            @Test
            @DisplayName("When creating event should create empty set for threads")
            public void whenCreatingEventShouldCreateEmptySetForThreads() {
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(cityRepository.findByIgnoreCaseName(CITY_RZESZOW_NAME)).thenReturn(cityRzeszowOptional);
                when(eventRepository.save(any())).thenReturn(event);
                when(userRepository.findByEmail(anyString())).thenReturn(eventOwnerOptional);


                ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
                eventCreateDto.getTags().clear();
                eventService.createEvent(eventCreateDto, JWT_STRING);

                verify(eventRepository, times(1)).save(eventArgumentCaptor.capture());
                Event capturedEvent = eventArgumentCaptor.getValue();
                assertNotNull(capturedEvent.getThreads());
                assertTrue(capturedEvent.getThreads().isEmpty());
            }

            @Test
            @DisplayName("When creating event should pass event object to repository with all data")
            public void whenCreatingEventShouldPassEventObjectToRepositoryWithAllData() {
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(cityRepository.findByIgnoreCaseName(CITY_RZESZOW_NAME)).thenReturn(cityRzeszowOptional);
                when(tagRepository.findByIgnoreCaseName(TAG_JAVA_NAME)).thenReturn(tagJavaOptional);
                when(tagRepository.findByIgnoreCaseName(TAG_SPRING_NAME)).thenReturn(tagSpringOptional);

                when(eventRepository.save(any())).thenReturn(event);
                ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

                eventService.createEvent(eventCreateDto, JWT_STRING);

                verify(eventRepository, times(1)).save(eventArgumentCaptor.capture());

                Event capturedEvent = eventArgumentCaptor.getValue();
                assertEquals(eventCreateDto.getName(), capturedEvent.getName());
                assertEquals(eventCreateDto.getShortDescription(), capturedEvent.getShortDescription());
                assertEquals(eventCreateDto.getLongDescription(), capturedEvent.getLongDescription());
                assertEquals(eventOwner, capturedEvent.getOwner());
                assertEquals(eventCreateDto.getCity(), capturedEvent.getCity().getName());
                assertEquals(eventCreateDto.getExactAddress(), capturedEvent.getExactAddress());
                assertEquals(eventCreateDto.getEventStartDate().withSecond(0).withNano(0), capturedEvent.getEventStartDate());
                assertEquals(capturedEvent.getCreateDate(), capturedEvent.getLastUpdate());


                ArrayList<String> tagNamesFromCapturedEvent = new ArrayList<>();
                capturedEvent.getTags().forEach(tag -> tagNamesFromCapturedEvent.add(tag.getName()));
                eventCreateDto.getTags().forEach(tagName -> assertTrue(tagNamesFromCapturedEvent.contains(tagName)));

            }

            @Test
            @DisplayName("When creating event should setup relationship with city .")
            public void whenCreatingEvent(){
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(cityRepository.findByIgnoreCaseName(CITY_RZESZOW_NAME)).thenReturn(cityRzeszowOptional);
                when(tagRepository.findByIgnoreCaseName(TAG_JAVA_NAME)).thenReturn(tagJavaOptional);
                when(tagRepository.findByIgnoreCaseName(TAG_SPRING_NAME)).thenReturn(tagSpringOptional);

                when(eventRepository.save(any())).thenReturn(event);
                ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

                eventService.createEvent(eventCreateDto, JWT_STRING);

                verify(eventRepository, times(1)).save(eventArgumentCaptor.capture());

                Event capturedEvent = eventArgumentCaptor.getValue();

                assertTrue(cityRzeszow.getEvents().contains(event));
                assertEquals(cityRzeszow, capturedEvent.getCity());
            }
            @Test
            @DisplayName("When creating event should setup relationship with tags.")
            public void whenCreatingEvent2(){
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(cityRepository.findByIgnoreCaseName(CITY_RZESZOW_NAME)).thenReturn(cityRzeszowOptional);
                when(tagRepository.findByIgnoreCaseName(TAG_JAVA_NAME)).thenReturn(tagJavaOptional);
                when(tagRepository.findByIgnoreCaseName(TAG_SPRING_NAME)).thenReturn(tagSpringOptional);

                when(eventRepository.save(any())).thenReturn(event);

                ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
                eventService.createEvent(eventCreateDto, JWT_STRING);

                verify(eventRepository, times(1)).save(eventArgumentCaptor.capture());
                Event capturedEvent = eventArgumentCaptor.getValue();

                assertTrue(tagJava.getEvents().contains(event));
                assertTrue(tagSpring.getEvents().contains(event));
                assertTrue(capturedEvent.getTags().contains(tagJava));
                assertTrue(capturedEvent.getTags().contains(tagSpring));


            }
            @Test
            @DisplayName("When creating event should setup relationship with user and save user.")
            public void whenCreatingEvent3(){
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(cityRepository.findByIgnoreCaseName(CITY_RZESZOW_NAME)).thenReturn(cityRzeszowOptional);
                when(tagRepository.findByIgnoreCaseName(TAG_JAVA_NAME)).thenReturn(tagJavaOptional);
                when(tagRepository.findByIgnoreCaseName(TAG_SPRING_NAME)).thenReturn(tagSpringOptional);

                when(eventRepository.save(any())).thenReturn(event);

                ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
                ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);

                eventService.createEvent(eventCreateDto, JWT_STRING);

                verify(eventRepository, times(1)).save(eventArgumentCaptor.capture());
                verify(userRepository, times(1)).save(userArgumentCaptor.capture());

                Event capturedEvent = eventArgumentCaptor.getValue();
                User capturedUser = userArgumentCaptor.getValue();

                assertEquals(eventOwner, capturedEvent.getOwner());
                assertTrue(capturedUser.getUserEvents().contains(event));
            }

        }


        @Nested
        @DisplayName("Update event tests")
        class UpdatingEventTest {

            @BeforeEach
            void setUp() {
                updatedEventDto = EventCreateDto.builder()
                        .name(EVENT_NAME_UPDATE)
                        .shortDescription(EVENT_SHORT_DESCRIPTION_UPDATE)
                        .longDescription(EVENT_LONG_DESCRIPTION_UPDATE)
                        .city(CITY_KRAKOW_NAME)
                        .exactAddress(EVENT_EXACT_ADDRESS_UPDATE)
                        .tags(new ArrayList<>())
                        .eventStartDate(ZonedDateTime.now().plusDays(10))
                        .build();

                updatedEventDto.getTags().add(TAG_WITAM_NAME);
                updatedEventDto.getTags().add(TAG_ZEGNAM_NAME);
            }

            @DisplayName("When updating event should try to load event from database")
            @Test
            public void whenUpdatingEventShouldTryToLoadEventFromDatabase() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(cityRepository.findByIgnoreCaseName(CITY_KRAKOW_NAME)).thenReturn(cityKrakowOptional);
                when(tagRepository.findByIgnoreCaseName(TAG_WITAM_NAME)).thenReturn(tagWitamOptional);
                when(tagRepository.findByIgnoreCaseName(TAG_ZEGNAM_NAME)).thenReturn(tagZegnamOptional);
                when(eventRepository.save(event)).thenReturn(event);

                eventService.updateEvent(updatedEventDto, EVENT_ID, JWT_STRING);

                verify(eventRepository, times(1)).findById(EVENT_ID);
            }

            @Test
            @DisplayName("When updating event should throw EventNotFoundException if event does not exist")
            public void whenUpdatingEventShouldThrowEventNotFoundExceptionIfEventDoesNotExist() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

                assertThrows(EventNotFoundException.class, () -> eventService.updateEvent(updatedEventDto, EVENT_ID, JWT_STRING));
            }

            @Test
            @DisplayName("When updating event should throw EventHadPlaceException if today's date is after event start date.")
            public void whenUpdatingEventShouldThrowEventHadPlaceExceptionIfTodaysDateIsAfterEventStartDate() {
                event.setEventStartDate(ZonedDateTime.now().minusDays(30));
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);

                assertThrows(EventAlreadyHadPlaceException.class, () -> eventService.updateEvent(updatedEventDto, EVENT_ID, JWT_STRING));
            }

            @Test
            @DisplayName("When updating event should extract user email from jwt for ownership check")
            public void whenUpdatingEventShouldExtractUserEmailFromJwtForOwnershipCheck() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(tagRepository.findByIgnoreCaseName(TAG_WITAM_NAME)).thenReturn(tagWitamOptional);
                when(tagRepository.findByIgnoreCaseName(TAG_ZEGNAM_NAME)).thenReturn(tagZegnamOptional);
                when(cityRepository.findByIgnoreCaseName(CITY_KRAKOW_NAME)).thenReturn(cityKrakowOptional);
                when(eventRepository.save(event)).thenReturn(event);

                eventService.updateEvent(updatedEventDto, EVENT_ID, JWT_STRING);

                verify(jwtUtil, times(1)).extractUsername(JWT_STRING);

            }

            @Test
            @DisplayName("When updating event should find user with extracted email")
            public void whenUpdatingEventShouldFindUserWithExtractedEmail() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(tagRepository.findByIgnoreCaseName(TAG_WITAM_NAME)).thenReturn(tagWitamOptional);
                when(tagRepository.findByIgnoreCaseName(TAG_ZEGNAM_NAME)).thenReturn(tagZegnamOptional);
                when(cityRepository.findByIgnoreCaseName(CITY_KRAKOW_NAME)).thenReturn(cityKrakowOptional);
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
            @DisplayName("When updating event should throw NotEventOwnerException if user try to modify not his event")
            public void whenUpdatingEventShouldThrowWrongEventOwnerExceptionIfUserTryToModifyNotHisEvent() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

                assertThrows(NotEventOwnerException.class, () -> eventService.updateEvent(updatedEventDto, EVENT_ID, JWT_STRING));
            }

            @Test
            @DisplayName("When updating Event should update basic fields of event")
            public void whenUpdatingEventShouldUpdateFieldsOfEvent() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(anyString())).thenReturn(eventOwner.getEmail());
                when(userRepository.findByEmail(anyString())).thenReturn(eventOwnerOptional);
                when(tagRepository.findByIgnoreCaseName(tagWitam.getName())).thenReturn(tagWitamOptional);
                when(tagRepository.findByIgnoreCaseName(tagZegnam.getName())).thenReturn(tagZegnamOptional);
                when(cityRepository.findByIgnoreCaseName(CITY_KRAKOW_NAME)).thenReturn(cityKrakowOptional);
                when(eventRepository.save(event)).thenReturn(event);

                ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

                eventService.updateEvent(updatedEventDto, EVENT_ID, JWT_STRING);
                verify(eventRepository).save(eventArgumentCaptor.capture());

                Event eventToBeSaved = eventArgumentCaptor.getValue();

                assertAll("Base field verification: ",
                        () -> assertEquals(updatedEventDto.getName(), eventToBeSaved.getName(), "When updating event should update name"),
                        () -> assertEquals(updatedEventDto.getShortDescription(), eventToBeSaved.getShortDescription(), "When updating event should update short description"),
                        () -> assertEquals(updatedEventDto.getLongDescription(), eventToBeSaved.getLongDescription(), "When updating event should update long description"),
                        () -> assertEquals(updatedEventDto.getCity(), eventToBeSaved.getCity().getName(), "When updating event should update city"),
                        () -> assertEquals(updatedEventDto.getExactAddress(), eventToBeSaved.getExactAddress(), "When updating event should update exact address"),
                        () -> assertEquals(updatedEventDto.getEventStartDate().withSecond(0).withNano(0), eventToBeSaved.getEventStartDate(), "When updating should update event start date"),
                        () -> assertEquals(updatedEventDto.getEventStartDate().getZone(), ZoneId.of(eventToBeSaved.getTimeZoneId()), "When updating should update event time zone id based on event start")
                );
            }

            @Test
            @DisplayName("When updating event should add tags which have not been earlier in event tags")
            public void whenUpdatingEventShouldAddTagsWhichHaveNotBeenEarlierInEventTags() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(tagRepository.findByIgnoreCaseName(TAG_WITAM_NAME)).thenReturn(tagWitamOptional);
                when(cityRepository.findByIgnoreCaseName(CITY_KRAKOW_NAME)).thenReturn(cityKrakowOptional);
                when(eventRepository.save(event)).thenReturn(event);

                updatedEventDto.getTags().clear();
                updatedEventDto.getTags().add(TAG_JAVA_NAME);
                updatedEventDto.getTags().add(TAG_SPRING_NAME);
                updatedEventDto.getTags().add(TAG_WITAM_NAME);

                ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

                eventService.updateEvent(updatedEventDto, EVENT_ID, JWT_STRING);
                verify(eventRepository).save(eventArgumentCaptor.capture());

                Event eventToBeSaved = eventArgumentCaptor.getValue();

                assertAll("Tag - event relationship verification:",
                        () -> assertEquals(3, eventToBeSaved.getTags().size(), "After update event should have 3 tags"),
                        () -> assertTrue(eventToBeSaved.getTags().contains(tagJava), "After update event should contain tag java"),
                        () -> assertTrue(eventToBeSaved.getTags().contains(tagSpring), "After update event should contain tag spring"),
                        () -> assertTrue(eventToBeSaved.getTags().contains(tagWitam), "After update event should contain tag witam"),
                        () -> assertTrue(tagJava.getEvents().contains(event), "After update tag java should contain event"),
                        () -> assertTrue(tagSpring.getEvents().contains(event), "After update tag spring should contain event"),
                        () -> assertTrue(tagWitam.getEvents().contains(event), "After update tag witam should contain event")
                );
            }

            @Test
            @DisplayName("When updating event should remove tags from event not appearing in EventUpdateDto")
            public void whenUpdatingEventShouldRemoveTagsNotAppearingInEventUpdateDto() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(tagRepository.findByIgnoreCaseName(TAG_WITAM_NAME)).thenReturn(tagWitamOptional);
                when(tagRepository.findByIgnoreCaseName(TAG_ZEGNAM_NAME)).thenReturn(tagZegnamOptional);
                when(cityRepository.findByIgnoreCaseName(CITY_KRAKOW_NAME)).thenReturn(cityKrakowOptional);
                when(eventRepository.save(event)).thenReturn(event);

                ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

                eventService.updateEvent(updatedEventDto, EVENT_ID, JWT_STRING);
                verify(eventRepository).save(eventArgumentCaptor.capture());

                Event eventToBeSaved = eventArgumentCaptor.getValue();

                assertAll("Tag - event relationship verification:",
                        () -> assertEquals(2, eventToBeSaved.getTags().size(), "After update event should have 2 tags"),
                        () -> assertFalse(eventToBeSaved.getTags().contains(tagJava), "After update event should not contain tag java"),
                        () -> assertFalse(eventToBeSaved.getTags().contains(tagSpring), "After update event should not contain tag spring"),
                        () -> assertTrue(eventToBeSaved.getTags().contains(tagZegnam), "After update event should contain tag zegnam"),
                        () -> assertTrue(eventToBeSaved.getTags().contains(tagWitam), "After update event should contain tag witam"),
                        () -> assertFalse(tagJava.getEvents().contains(event), "After update tag java should not contain event"),
                        () -> assertFalse(tagSpring.getEvents().contains(event), "After update tag spring should not contain event"),
                        () -> assertTrue(tagZegnam.getEvents().contains(event), "After update tag zegnam should contain event"),
                        () -> assertTrue(tagWitam.getEvents().contains(event), "After update tag witam should contain event")
                );
            }

            @Test
            @DisplayName("When updating event should retain tags which are in EventUpdateDto but were already assigned to event")
            public void whenUpdatingEventShouldNotRemoveTagsAppearingInDtoFromEventListOfTags() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(cityRepository.findByIgnoreCaseName(CITY_KRAKOW_NAME)).thenReturn(cityKrakowOptional);
                when(tagRepository.findByIgnoreCaseName(TAG_ZEGNAM_NAME)).thenReturn(tagZegnamOptional);
                when(eventRepository.save(event)).thenReturn(event);

                updatedEventDto.setTags(Arrays.asList(TAG_JAVA_NAME, TAG_ZEGNAM_NAME));

                ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

                eventService.updateEvent(updatedEventDto, EVENT_ID, JWT_STRING);

                verify(eventRepository).save(eventArgumentCaptor.capture());

                Event eventToBeSaved = eventArgumentCaptor.getValue();

                assertAll("Tag - event relationship verification:",
                        () -> assertEquals(2, eventToBeSaved.getTags().size()),
                        () -> assertTrue(event.getTags().contains(tagJava), "Event should contain tag java."),
                        () -> assertTrue(event.getTags().contains(tagZegnam), "Event should contain tag zegnam."),
                        () -> assertFalse(event.getTags().contains(tagSpring), "Event should not contain tag spring."),
                        () -> assertTrue(tagJava.containsEvent(event), "Tag java should contain event."),
                        () -> assertTrue(tagZegnam.containsEvent(event), "Tag zegnam should contain event."),
                        () -> assertFalse(tagSpring.containsEvent(event), "Tag spring should not contain event.")
                );
            }

            @Test
            @DisplayName("When updating event should update all tags if all of them changed")
            public void whenUpdatingEventShouldUpdateAllTagsIfAllOfThemChanged() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(tagRepository.findByIgnoreCaseName(TAG_WITAM_NAME)).thenReturn(tagWitamOptional);
                when(tagRepository.findByIgnoreCaseName(TAG_ZEGNAM_NAME)).thenReturn(tagZegnamOptional);
                when(cityRepository.findByIgnoreCaseName(CITY_KRAKOW_NAME)).thenReturn(cityKrakowOptional);
                when(eventRepository.save(event)).thenReturn(event);

                updatedEventDto.setTags(Arrays.asList(TAG_WITAM_NAME, TAG_ZEGNAM_NAME));

                eventService.updateEvent(updatedEventDto, EVENT_ID, JWT_STRING);

                assertAll("Tag - event relationship verification: ",
                        () -> assertEquals(2, event.getTags().size(), "After update event should have 2 tags"),
                        () -> assertFalse(tagSpring.getEvents().contains(event), "After update event should not contain tag spring"),
                        () -> assertFalse(tagJava.getEvents().contains(event), "After update event should not contain tag java"),
                        () -> assertTrue(tagZegnam.getEvents().contains(event), "After update event should contain tag zegnam"),
                        () -> assertTrue(tagWitam.getEvents().contains(event), "After update event should contain tag witam"),
                        () -> assertFalse(event.getTags().contains(tagSpring), "After update event should not contain tag spring"),
                        () -> assertFalse(event.getTags().contains(tagJava), "After update event should not contain tag java"),
                        () -> assertTrue(event.getTags().contains(tagZegnam), "After update event should contain tag zegnam"),
                        () -> assertTrue(event.getTags().contains(tagWitam), "After update event should contain tag witam")
                );
            }

            @Test
            @DisplayName("When updating event should remove all tags if dto tag list is empty from event list of tags")
            public void whenUpdatingEventShouldRemoveAllTagsIfDtoTagListIsEmptyFromEventListOfTags() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(cityRepository.findByIgnoreCaseName(CITY_KRAKOW_NAME)).thenReturn(cityKrakowOptional);
                when(eventRepository.save(event)).thenReturn(event);

                updatedEventDto.getTags().clear();

                ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

                eventService.updateEvent(updatedEventDto, EVENT_ID, JWT_STRING);

                verify(eventRepository).save(eventArgumentCaptor.capture());

                Event eventToBeSaved = eventArgumentCaptor.getValue();

                assertAll("Tag - event relationship verification:",
                        () -> assertTrue(eventToBeSaved.getTags().isEmpty(), "After update event should not contain any tags"),
                        () -> assertFalse(tagJava.getEvents().contains(event), "After update tag java should not contain event"),
                        () -> assertFalse(tagSpring.getEvents().contains(event), "After update tag spring should not contain event"),
                        () -> assertFalse(tagZegnam.getEvents().contains(event), "After update tag zegnam should not contain event"),
                        () -> assertFalse(tagWitam.getEvents().contains(event), "After update tag witam should not contain event")
                );
            }

            @Test
            @DisplayName("When updating event should save new tag in database if it does not exist")
            public void whenUpdatingEventShouldSaveNewTagInDatabaseIfItDoesNotExist() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(cityRepository.findByIgnoreCaseName(CITY_KRAKOW_NAME)).thenReturn(cityKrakowOptional);
                when(tagRepository.findByIgnoreCaseName(TAG_ZEGNAM_NAME)).thenReturn(tagZegnamOptional);
                when(tagRepository.findByIgnoreCaseName(TAG_WITAM_NAME)).thenReturn(Optional.empty());
                when(tagRepository.save(any(Tag.class))).thenReturn(tagWitam);
                when(eventRepository.save(event)).thenReturn(event);

                ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
                ArgumentCaptor<Tag> tagArgumentCaptor = ArgumentCaptor.forClass(Tag.class);

                eventService.updateEvent(updatedEventDto, EVENT_ID, JWT_STRING);

                verify(tagRepository, times(1)).save(tagArgumentCaptor.capture());
                verify(eventRepository).save(eventArgumentCaptor.capture());

                Tag tagToBeSaved = tagArgumentCaptor.getValue();
                Event eventToBeSaved = eventArgumentCaptor.getValue();

                assertEquals(TAG_WITAM_NAME, tagToBeSaved.getName(), "When updating event should save new tag with proper name in database if it does not exist");

                assertAll("Tag - event relationship verification:",
                        () -> assertFalse(eventToBeSaved.getTags().contains(tagJava), "After update event should not contain any tags"),
                        () -> assertFalse(eventToBeSaved.getTags().contains(tagJava), "After update event should not contain any tags"),
                        () -> assertTrue(eventToBeSaved.getTags().contains(tagWitam), "After update event should contain any tags"),
                        () -> assertTrue(eventToBeSaved.getTags().contains(tagZegnam), "After update event should contain any tags"),
                        () -> assertFalse(tagJava.getEvents().contains(event), "After update tag java should not contain event"),
                        () -> assertFalse(tagSpring.getEvents().contains(event), "After update tag spring should not contain event"),
                        () -> assertTrue(tagZegnam.getEvents().contains(event), "After update tag zegnam should contain event"),
                        () -> assertTrue(tagWitam.getEvents().contains(event), "After update tag witam should contain event")
                );
            }

            @Test
            @DisplayName("When updating event should save changes to database")
            public void whenUpdatingEventShouldSaveChangesToDatabase() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(tagRepository.findByIgnoreCaseName(TAG_WITAM_NAME)).thenReturn(tagWitamOptional);
                when(tagRepository.findByIgnoreCaseName(TAG_ZEGNAM_NAME)).thenReturn(tagZegnamOptional);
                when(cityRepository.findByIgnoreCaseName(CITY_KRAKOW_NAME)).thenReturn(cityKrakowOptional);
                when(eventRepository.save(event)).thenReturn(event);

                eventService.updateEvent(updatedEventDto, EVENT_ID, JWT_STRING);

                verify(eventRepository, times(1)).save(event);
            }
        }

    }


    @Nested
    @DisplayName("Event thread tests:")
    class EventThreadTests {
        @BeforeEach
        void setUp() {
            eventService = new EventServiceImpl(eventRepository, cityRepository, tagRepository, userRepository, threadRepository, threadReplyRepository, fileRepository, notificationService, jwtUtil, fileUtils);

            cityRzeszow = City.builder()
                    .id(CITY_RZESZOW_ID)
                    .name(CITY_RZESZOW_NAME)
                    .residents(new HashSet<>())
                    .events(new ArrayList<>())
                    .build();
            cityRzeszowOptional = Optional.of(cityRzeszow);

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
                    .build();
            secondUserOptional = Optional.of(secondUser);

            ZonedDateTime eventCreateDate = ZonedDateTime.now().minusDays(1);

            event = Event.builder()
                    .id(EVENT_ID)
                    .name(EVENT_NAME)
                    .owner(eventOwner)
                    .shortDescription(EVENT_SHORT_DESCRIPTION)
                    .longDescription(EVENT_LONG_DESCRIPTION)
                    .createDate(eventCreateDate)
                    .timeZoneId(eventCreateDate.getZone().getId())
                    .eventStartDate(eventCreateDate.withSecond(0).withNano(0).plusDays(7))
                    .lastUpdate(eventCreateDate)
                    .city(cityRzeszow)
                    .exactAddress(EVENT_EXACT_ADDRESS)
                    .build();

            eventOptional = Optional.of(event);

            cityRzeszow.addEvent(event);

            threadCreateDto = ThreadCreateDto.builder()
                    .name(FIRST_THREAD_NAME)
                    .content(FIRST_THREAD_CONTENT)
                    .build();

            ZonedDateTime threadCreateDate = ZonedDateTime.now().minusHours(1);

            thread = Thread.builder()
                    .id(THREAD_ID)
                    .event(event)
                    .owner(eventOwner)
                    .name(FIRST_THREAD_NAME)
                    .content(FIRST_THREAD_CONTENT)
                    .replies(new HashSet<>())
                    .createDate(eventCreateDate)
                    .lastUpdate(eventCreateDate)
                    .editCounter(0)
                    .build();

            threadOptional = Optional.of(thread);


        }

        @Nested
        @DisplayName("Thread create in event tests:")
        class ThreadCreateTests {
            @BeforeEach
            void setUp() {
            }

            @Test
            @DisplayName("When creating thread should try to load event from database")
            public void whenCreatingThreadShouldTryToLoadEventFromDatabase() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
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
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(threadRepository.save(any())).thenReturn(thread);

                eventService.createThreadInEvent(threadCreateDto, EVENT_ID, JWT_STRING);

                verify(jwtUtil, times(1)).extractUsername(JWT_STRING);
            }

            @Test
            @DisplayName("When creating thread should find user in database")
            public void whenCreatingThreadShouldFindUserInDatabase() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(threadRepository.save(any())).thenReturn(thread);

                eventService.createThreadInEvent(threadCreateDto, EVENT_ID, JWT_STRING);

                verify(userRepository, times(1)).findByEmail(EVENT_OWNER_EMAIL);
            }

            @Test
            @DisplayName("When creating thread should check if user is attending event")
            public void whenCreatingThreadShouldCheckIfUserIsAttendingEvent() {
                when(threadRepository.save(any())).thenReturn(thread);

                eventOptional = Optional.of(Mockito.spy(event));

                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);

                eventService.createThreadInEvent(threadCreateDto, EVENT_ID, JWT_STRING);

                verify(eventOptional.get(), times(1)).isUserAttending(eventOwner);
            }

            @Test
            @DisplayName("When creating thread should throw NotAttenderException if user is not attending event")
            public void whenCreatingThreadShouldThrowNotAttenderExceptionIfUserIsNotAttendingEvent() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

                assertThrows(NotEventAttenderException.class, () -> eventService.createThreadInEvent(threadCreateDto, EVENT_ID, JWT_STRING));
            }

            @Test
            @DisplayName("When creating thread should save new thread with data from dto")
            public void whenCreatingThreadShouldSaveNewThreadWithDataFromDto() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
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
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(threadRepository.save(any(Thread.class))).thenReturn(thread);

                ArgumentCaptor<Thread> threadArgumentCaptor = ArgumentCaptor.forClass(Thread.class);

                eventService.createThreadInEvent(threadCreateDto, EVENT_ID, JWT_STRING);

                verify(threadRepository, times(1)).save(threadArgumentCaptor.capture());

                Thread threadToBeSaved = threadArgumentCaptor.getValue();

                assertEquals(event, threadToBeSaved.getEvent());
                assertTrue(event.containsThread(thread));
            }

            @Test
            @DisplayName("When creating thread should save new thread with set create date and edit date which have to be equal")
            public void whenCreatingThreadShouldSaveNewThreadWithSetCreateDateAndEditDateWhichHaveToBeEqual() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(threadRepository.save(any())).thenReturn(thread);

                ArgumentCaptor<Thread> threadArgumentCaptor = ArgumentCaptor.forClass(Thread.class);

                eventService.createThreadInEvent(threadCreateDto, EVENT_ID, JWT_STRING);

                verify(threadRepository, times(1)).save(threadArgumentCaptor.capture());

                Thread threadToBeSaved = threadArgumentCaptor.getValue();
                assertNotNull(threadToBeSaved.getCreateDate());
                assertNotNull(threadToBeSaved.getLastUpdate());
                assertEquals(threadToBeSaved.getCreateDate(), threadToBeSaved.getLastUpdate());
            }

            @Test
            @DisplayName("When creating thread should save new thread with zeroed edit counter")
            public void whenCreatingThreadShouldSaveNewThreadWithZeroedEditCounter() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(threadRepository.save(any())).thenReturn(thread);

                ArgumentCaptor<Thread> threadArgumentCaptor = ArgumentCaptor.forClass(Thread.class);

                eventService.createThreadInEvent(threadCreateDto, EVENT_ID, JWT_STRING);

                verify(threadRepository, times(1)).save(threadArgumentCaptor.capture());

                Thread threadToBeSaved = threadArgumentCaptor.getValue();

                assertEquals(0, threadToBeSaved.getEditCounter());
            }

            @Test
            @DisplayName("When creating thread should create new set for replays")
            public void whenCreatingThreadShouldCreateNewSetForRepliesToSave() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(threadRepository.save(any())).thenReturn(thread);

                ArgumentCaptor<Thread> threadArgumentCaptor = ArgumentCaptor.forClass(Thread.class);

                eventService.createThreadInEvent(threadCreateDto, EVENT_ID, JWT_STRING);

                verify(threadRepository, times(1)).save(threadArgumentCaptor.capture());

                Thread threadToBeSaved = threadArgumentCaptor.getValue();
                assertNotNull(threadToBeSaved.getReplies());
            }

            @Test
            @DisplayName("When creating thread should save user")
            public void whenCreatingThreadShouldSaveUser() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(threadRepository.save(any())).thenReturn(thread);

                eventService.createThreadInEvent(threadCreateDto, EVENT_ID, JWT_STRING);

                verify(userRepository, times(1)).save(eventOwner);
            }
            @Test
            @DisplayName("When creating thread should save event")
            public void whenCreatingThreadShouldSaveEvent() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(threadRepository.save(any())).thenReturn(thread);

                eventService.createThreadInEvent(threadCreateDto, EVENT_ID, JWT_STRING);

                verify(eventRepository, times(1)).save(event);
            }
        }


        @Nested
        @DisplayName("Thread update tests:")
        class ThreadUpdateTests {
            ThreadCreateDto threadupdateDto;
            @BeforeEach
            void setUp() {
                threadupdateDto = ThreadCreateDto.builder()
                        .name("updated thread name")
                        .content("updated content of the thread, have to be different from original one")
                        .build();

                eventOwner.addThread(thread);
                event.addThread(thread);
            }

            @Test
            @DisplayName("When updating thread should find event by given id")
            public void whenUpdatingThreadShouldFindEventByGivenId() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(threadOptional);
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
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(threadOptional);
                when(threadRepository.save(thread)).thenReturn(thread);

                eventService.updateThreadInEvent(threadCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

                verify(jwtUtil, times(1)).extractUsername(JWT_STRING);
            }

            @Test
            @DisplayName("When updating thread should try to load user from database")
            public void whenUpdatingThreadShouldTryLoadUserFromDatabase() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(threadOptional);
                when(threadRepository.save(thread)).thenReturn(thread);

                eventService.updateThreadInEvent(threadCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

                verify(userRepository, times(1)).findByEmail(EVENT_OWNER_EMAIL);
            }


            @Test
            @DisplayName("When updating thread should try to load thread by given id")
            public void whenUpdatingThreadShouldTryToLoadThreadByGivenId() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(threadOptional);
                when(threadRepository.save(thread)).thenReturn(thread);

                eventService.updateThreadInEvent(threadCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

                verify(threadRepository, times(1)).findByIdAndEventId(THREAD_ID, EVENT_ID);
            }

            @Test
            @DisplayName("When updating thread should throw ThreadNotFoundException if there is no thread with given id")
            public void whenUpdatingThreadShouldThrowThreadNotFoundExceptionIfThereIsNoThreadWithGivenId() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(Optional.empty());

                assertThrows(ThreadNotFoundInEventException.class, () -> eventService.updateThreadInEvent(threadCreateDto, EVENT_ID, THREAD_ID, JWT_STRING));
            }

            @Test
            @DisplayName("When updating thread should throw NotEventAttenderException if user is not attending event anymore")
            public void whenUpdatingThreadShouldCheckIfOwnerOfThreadIsStillAttendingEvent() {
                eventOwner.removeThread(thread);
                secondUser.addThread(thread);
                thread.setOwner(secondUser);

                Event eventSpy = Mockito.spy(event);
                when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(eventSpy));
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(threadOptional);


                assertThrows(NotEventAttenderException.class, ()-> eventService.updateThreadInEvent(threadCreateDto, EVENT_ID, THREAD_ID, JWT_STRING));

                verify(eventSpy, times(1)).isUserAttending(secondUser);
            }

            @Test
            @DisplayName("When updating thread should Throw NotAttenderException if thread owner is not attending anymore")
            public void whenUpdatingThreadShouldThrowNotAttenderExceptionIfThreadOwnerIsNotAttendingAnymore() {
                event.removeAttendingUser(secondUser);
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(threadOptional);

                assertThrows(NotEventAttenderException.class, () -> eventService.updateThreadInEvent(threadCreateDto, EVENT_ID, THREAD_ID, JWT_STRING));

            }

            @Test
            @DisplayName("When updating thread should check if user is owner of thread")
            public void whenUpdatingThreadShouldCheckIfUserIsOwnerOfThread() {
                Thread threadSpy = Mockito.spy(thread);
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(Optional.of(threadSpy));
                when(threadRepository.save(threadSpy)).thenReturn(threadSpy);

                eventService.updateThreadInEvent(threadCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

                verify(threadSpy, times(1)).isUserOwner(eventOwner);
            }

            @Test
            @DisplayName("When updating thread should Throw NotThreadOwner if user do not own this thread")
            public void whenUpdatingThreadShouldThrowNotThreadOwnerExceptionIfUserDoNotOwnThisThread() {
                thread.setOwner(secondUser);
                eventOwner.removeThread(thread);

                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(threadOptional);

                assertThrows(NotThreadOwnerException.class, () -> eventService.updateThreadInEvent(threadCreateDto, EVENT_ID, THREAD_ID, JWT_STRING));
            }

            @Test
            @DisplayName("When updating thread should update name and content of thread")
            public void whenUpdatingThreadShouldUpdateOnlyNameAndContentOfThread() {
                threadCreateDto.setName("updated name");
                threadCreateDto.setContent("updated content");

                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(threadOptional);
                when(threadRepository.save(thread)).thenReturn(thread);

                eventService.updateThreadInEvent(threadCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

                assertEquals(threadCreateDto.getContent(), thread.getContent());
                assertEquals(threadCreateDto.getName(), thread.getName());
            }

            @Test
            @DisplayName("When updating thread should update lastUpdate field")
            public void whenUpdatingThreadShouldUpdateLastUpdateField() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(threadOptional);
                when(threadRepository.save(thread)).thenReturn(thread);

                ZonedDateTime oldLastUpdate = thread.getLastUpdate();

                eventService.updateThreadInEvent(threadCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

                assertTrue(oldLastUpdate.isBefore(thread.getLastUpdate()));
            }

            @Test
            @DisplayName("When updating thread should save updated entity")
            public void whenUpdatingThreadShouldSaveUpdatedEntity() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(threadOptional);
                when(threadRepository.save(thread)).thenReturn(thread);

                eventService.updateThreadInEvent(threadCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

                verify(threadRepository, times(1)).save(thread);
            }

        }


        @Nested
        @DisplayName("Create reply in thread test:")
        class CreateReplayInThreadTests {
            @BeforeEach
            void setUp() {
                event.addAttendingUser(secondUser);

                ZonedDateTime threadReplyCreateDate = ZonedDateTime.now();

                threadReply = ThreadReply.builder()
                        .id(THREAD_REPLY_ID)
                        .thread(thread)
                        .content(THREAD_REPLY_CONTENT)
                        .replyDate(threadReplyCreateDate)
                        .lastUpdate(threadReplyCreateDate)
                        .replier(secondUser)
                        .editCounter(0)
                        .build();

                threadReplyCreateDto = new ThreadReplyCreateDto(THREAD_REPLY_CONTENT);
            }

            @Test
            @DisplayName("When creating reply in thread should find event with given id")
            public void whenCreatingReplayInThreadShouldFindEventWithGivenId() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID,EVENT_ID)).thenReturn(threadOptional);
                when(threadReplyRepository.save(any(ThreadReply.class))).thenReturn(threadReply);

                eventService.createReplyInThread(threadReplyCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

                verify(eventRepository, times(1)).findById(EVENT_ID);

            }

            @Test
            @DisplayName("When creating reply in thread should throw EventNotFoundException if there is no event with given id")
            public void whenCreatingReplayInThreadShouldThrowEventNotFoundExceptionIfThereIsNoEventWithGivenId() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

                assertThrows(EventNotFoundException.class, () -> eventService.createReplyInThread(threadReplyCreateDto, EVENT_ID, THREAD_ID, JWT_STRING));
            }

            @Test
            @DisplayName("When creating reply in thread should extract user email from jwt")
            public void whenCreatingReplayInThreadShouldExtractUserEmailFromJwt() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(threadOptional);
                when(threadReplyRepository.save(any(ThreadReply.class))).thenReturn(threadReply);

                eventService.createReplyInThread(threadReplyCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

                verify(jwtUtil, times(1)).extractUsername(JWT_STRING);
            }

            @Test
            @DisplayName("When creating reply in thread should get user from database")
            public void whenCreatingReplayInThreadShouldLookForUserByExtractedEmail() {

                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(threadOptional);
                when(threadReplyRepository.save(any(ThreadReply.class))).thenReturn(threadReply);

                eventService.createReplyInThread(threadReplyCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

                verify(userRepository, times(1)).findByEmail(SECOND_USER_EMAIL);
            }

            @Test
            @DisplayName("When creating reply in thread should check if replying user is attending event")
            public void whenCreatingReplayInThreadShouldCheckIfReplyingUserIsAttendingEvent() {
                Event eventSpy = Mockito.spy(event);
                when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(eventSpy));
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(threadOptional);
                when(threadReplyRepository.save(any(ThreadReply.class))).thenReturn(threadReply);

                eventService.createReplyInThread(threadReplyCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

                verify(eventSpy, times(1)).isUserAttending(secondUser);
            }

            @Test
            @DisplayName("When creating reply in thread should throw NotAttenderException if user is not attending event")
            public void whenCreatingReplayInThreadShouldThrowNotAttenderExceptionIfUserIsNotAttendingEvent() {
                eventOptional.get().removeAttendingUser(secondUser);
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);


                assertThrows(NotEventAttenderException.class, () -> eventService.createReplyInThread(threadReplyCreateDto, EVENT_ID, THREAD_ID, JWT_STRING));
            }

            @Test
            @DisplayName("When creating reply in thread should load thread with given id")
            public void whenCreatingReplayInThreadShouldLookUpThread() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(threadOptional);
                when(threadReplyRepository.save(any(ThreadReply.class))).thenReturn(threadReply);

                eventService.createReplyInThread(threadReplyCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

                verify(threadRepository, times(1)).findByIdAndEventId(THREAD_ID,EVENT_ID);
            }


            @Test
            @DisplayName("When creating reply in thread should throw ThreadNotFoundException if there is no thread with given id")
            public void whenCreatingReplayInThreadShouldThrowThreadNotFoundExceptionIfThereIsNoThreadWithGivenId() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(Optional.empty());

                assertThrows(ThreadNotFoundInEventException.class, () -> eventService.createReplyInThread(threadReplyCreateDto, EVENT_ID, THREAD_ID, JWT_STRING));

            }

            @Test
            @DisplayName("When creating reply in thread should set up ne ThreadReply object and save it with all necessary fields")
            public void whenCreatingReplayInThreadShouldSetUpNewThreadReplyObjectAndSaveItWithAllNecessaryFields() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(threadOptional);
                when(threadReplyRepository.save(any(ThreadReply.class))).thenReturn(threadReply);

                ArgumentCaptor<ThreadReply> threadReplyArgumentCaptor = ArgumentCaptor.forClass(ThreadReply.class);

                eventService.createReplyInThread(threadReplyCreateDto, EVENT_ID, THREAD_ID, JWT_STRING);

                verify(threadReplyRepository, times(1)).save(threadReplyArgumentCaptor.capture());
                ThreadReply capturedReply = threadReplyArgumentCaptor.getValue();

                assertEquals(threadReplyCreateDto.getReplyContent(), capturedReply.getContent());
                assertEquals(THREAD_ID, capturedReply.getThread().getId());
                assertEquals(secondUser, capturedReply.getReplier());
                assertEquals(0, capturedReply.getEditCounter());
                assertNotNull(capturedReply.getReplyDate());
                assertNotNull(capturedReply.getLastUpdate());
                assertEquals(capturedReply.getReplyDate(), capturedReply.getLastUpdate());

            }
        }


        @Nested
        @DisplayName("Update reply in event thread test:")
        class UpdateReplayInThreadTests {

            private ThreadReplyCreateDto threadReplyUpdateDto;

            @BeforeEach
            void setUp() {
                event.addAttendingUser(secondUser);

                ZonedDateTime replyDate = ZonedDateTime.now();

                threadReply = ThreadReply.builder()
                        .id(THREAD_REPLY_ID)
                        .thread(thread)
                        .content(THREAD_REPLY_CONTENT)
                        .replyDate(replyDate)
                        .lastUpdate(replyDate)
                        .replier(secondUser)
                        .editCounter(0)
                        .build();

                thread.addReplyToThread(threadReply);

                threadReplyOptional = Optional.of(threadReply);

                threadReplyUpdateDto = new ThreadReplyCreateDto(THREAD_REPLY_CONTENT_UPDATE);
            }

            @Test
            @DisplayName("When updating reply in thread should look up for event with given id")
            public void whenUpdatingReplyInThreadShouldLookUpForEventWithGivenId() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(threadOptional);
                when(threadReplyRepository.findByIdAndThreadId(THREAD_REPLY_ID, THREAD_ID)).thenReturn(threadReplyOptional);
                when(threadReplyRepository.save(threadReply)).thenReturn(threadReply);

                eventService.updateThreadReplyInEventThread(threadReplyUpdateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING);

                verify(eventRepository, times(1).description("Expected to look for event in database only once.")).findById(EVENT_ID);
            }

            @Test
            @DisplayName("When updating reply in thread should throw EventNotFoundException if there is no event with given id")
            public void whenUpdatingReplyInThreadShouldThrowEventNotFoundExceptionIfThereIsNoEventWithGivenId() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

                assertThrows(EventNotFoundException.class, () -> eventService.updateThreadReplyInEventThread(threadReplyUpdateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING), "Expected to throw EventNotFoundException if event does not exist.");
            }

            @Test
            @DisplayName("When updating reply in thread should extract user email from jwt")
            public void whenUpdatingReplyInThreadShouldExtractUserEmailFromJwt() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(threadOptional);
                when(threadReplyRepository.findByIdAndThreadId(THREAD_REPLY_ID, THREAD_ID)).thenReturn(threadReplyOptional);
                when(threadReplyRepository.save(threadReply)).thenReturn(threadReply);

                eventService.updateThreadReplyInEventThread(threadReplyUpdateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING);

                verify(jwtUtil, times(1)).extractUsername(JWT_STRING);
            }

            @Test
            @DisplayName("When updating reply in thread should check if user is attending event")
            public void whenUpdatingReplyInThreadShouldCheckIfUserIsAttendingEvent() {
                Event eventSpy = Mockito.spy(eventOptional.get());
                eventOptional = Optional.of(eventSpy);
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(threadOptional);
                when(threadReplyRepository.findByIdAndThreadId(THREAD_REPLY_ID, THREAD_ID)).thenReturn(threadReplyOptional);
                when(threadReplyRepository.save(threadReply)).thenReturn(threadReply);

                eventService.updateThreadReplyInEventThread(threadReplyUpdateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING);

                verify(eventSpy, times(1)).isUserAttending(secondUser);
            }

            @Test
            @DisplayName("When updating reply in thread should throw NotAttenderException if user is not attending event")
            public void whenUpdatingReplyInThreadShouldThrowNotAttenderExceptionIfUserIsNotAttendingEvent() {
                eventOptional.get().removeAttendingUser(secondUser);
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

                assertThrows(NotEventAttenderException.class, () -> eventService.updateThreadReplyInEventThread(threadReplyUpdateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING));
            }


            @Test
            @DisplayName("When updating reply in thread should look up for thread with given id")
            public void whenUpdatingReplyInThreadShouldLookUpForThreadWithGivenId() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(threadOptional);
                when(threadReplyRepository.findByIdAndThreadId(THREAD_REPLY_ID, THREAD_ID)).thenReturn(threadReplyOptional);
                when(threadReplyRepository.save(threadReply)).thenReturn(threadReply);

                eventService.updateThreadReplyInEventThread(threadReplyUpdateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING);

                verify(threadRepository, times(1).description("Expected to look up for event thread in database only once using thread id and event id.")).findByIdAndEventId(THREAD_ID, EVENT_ID);
            }

            @Test
            @DisplayName("When updating reply in thread should throw ThreadNotFoundInEventException if there is no thread with that id")
            public void whenUpdatingReplyInThreadShouldThrowThreadNotFoundInEventExceptionIfThereIsNoThreadWithThatId() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(Optional.empty());

                assertThrows(ThreadNotFoundInEventException.class, () -> eventService.updateThreadReplyInEventThread(threadReplyUpdateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING), "Expected to throw ThreadNotFoundInEventException if thread with given id does not exist.");
            }

            @Test
            @DisplayName("When updating reply in thread should throw ThreadNotFoundInEventException if thread with given id and event with given id are not related.")
            public void whenUpdatingReplyInThreadShouldThrowThreadNotFoundInEventExceptionIfThreadWithGivenIdAndEventWithGivenIdAreNotRelated() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(Optional.empty());

                assertThrows(ThreadNotFoundInEventException.class, () -> eventService.updateThreadReplyInEventThread(threadReplyUpdateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING), "Expected to throw ThreadNotFoundInEventException if thread and event are not related.");
            }

            @Test
            @DisplayName("When updating reply in thread should look up for thread reply with given id")
            public void whenUpdatingReplyInThreadShouldLookUpForThreadReplyWithGivenId() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(threadOptional);
                when(threadReplyRepository.findByIdAndThreadId(THREAD_REPLY_ID, THREAD_ID)).thenReturn(threadReplyOptional);
                when(threadReplyRepository.save(threadReply)).thenReturn(threadReply);

                eventService.updateThreadReplyInEventThread(threadReplyUpdateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING);

                verify(threadReplyRepository, times(1).description("Expected to ")).findByIdAndThreadId(THREAD_REPLY_ID, THREAD_ID);
            }

            @Test
            @DisplayName("When updating reply in thread should throw ReplyNotFoundInThreadException if thread reply with given id and thread with given id are not related or it does not exist.")
            public void whenUpdatingReplyInThreadShouldThrowWrongThreadExceptionIfThreadReplyWithGivenIdAndThreadWithGivenIdAreNotRelatedOrItNotExists() {
                thread.getReplies().remove(threadReply);
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(threadOptional);
                when(threadReplyRepository.findByIdAndThreadId(THREAD_REPLY_ID, THREAD_ID)).thenReturn(Optional.empty());

                assertThrows(ReplyNotFoundInThreadException.class,
                        () -> eventService.updateThreadReplyInEventThread(threadReplyUpdateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING),
                        "Expected to throw ReplyNotFoundInThreadException if thread and reply are not related or reply does not exist.");
            }

            @Test
            @DisplayName("When updating reply in thread should check if performing user is owner of thread reply being updated")
            public void whenUpdatingReplyInThreadShouldCheckIfPerformingUserIsOwnerOfThreadReplyBeingUpdated() {
                ThreadReply threadReplySpy = Mockito.spy(threadReply);
                thread.getReplies().remove(threadReply);
                thread.addReplyToThread(threadReplySpy);


                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(threadOptional);
                when(threadReplyRepository.findByIdAndThreadId(THREAD_REPLY_ID, THREAD_ID)).thenReturn(Optional.of(threadReplySpy));
                when(threadReplyRepository.save(threadReplySpy)).thenReturn(threadReplySpy);

                eventService.updateThreadReplyInEventThread(threadReplyUpdateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING);

                verify(threadReplySpy, times(1)).isReplier(secondUser);
            }

            @Test
            @DisplayName("When updating reply in thread should check if performing user is owner of thread reply being updated")
            public void whenUpdatingReplyInThreadShouldThrowNotThreadReplyOwnerExceptionIfUserTryToUpdateNotHisReply() {
                threadReply.setReplier(eventOwner);
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(threadOptional);
                when(threadReplyRepository.findByIdAndThreadId(THREAD_REPLY_ID, THREAD_ID)).thenReturn(threadReplyOptional);

                assertThrows(NotThreadReplyOwnerException.class, () -> eventService.updateThreadReplyInEventThread(threadReplyUpdateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING));
            }

            @Test
            @DisplayName("When updating reply in thread should update fields in stored thread reply and save it")
            public void whenUpdatingReplyInThreadShouldUpdateFieldsInStoredThreadReply() {
                ThreadReply threadReplySpy = Mockito.spy(threadReply);
                thread.getReplies().remove(threadReply);
                thread.addReplyToThread(threadReplySpy);

                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
                when(threadRepository.findByIdAndEventId(THREAD_ID, EVENT_ID)).thenReturn(threadOptional);
                when(threadReplyRepository.findByIdAndThreadId(THREAD_REPLY_ID, THREAD_ID)).thenReturn(Optional.of(threadReplySpy));
                when(threadReplyRepository.save(threadReplySpy)).thenReturn(threadReplySpy);

                ArgumentCaptor<ThreadReply> threadReplyArgumentCaptor = ArgumentCaptor.forClass(ThreadReply.class);

                eventService.updateThreadReplyInEventThread(threadReplyUpdateDto, EVENT_ID, THREAD_ID, THREAD_REPLY_ID, JWT_STRING);

                verify(threadReplyRepository, times(1)).save(threadReplyArgumentCaptor.capture());
                ThreadReply capturedThreadReply = threadReplyArgumentCaptor.getValue();
                verify(threadReplySpy, times(1)).incrementEditCounter();
                assertEquals(threadReplyUpdateDto.getReplyContent(), capturedThreadReply.getContent());
                assertTrue(capturedThreadReply.getEditCounter() > threadReply.getEditCounter());
                assertTrue(capturedThreadReply.getLastUpdate().isAfter(threadReply.getLastUpdate()));
            }


        }

    }

    @Nested
    @DisplayName("Event attending tests:")
    class EventAttendingTests{
        @BeforeEach
        void setUp() {
            eventService = new EventServiceImpl(eventRepository, cityRepository, tagRepository, userRepository, threadRepository, threadReplyRepository, fileRepository, notificationService, jwtUtil, fileUtils);

            cityRzeszow = City.builder()
                    .id(CITY_RZESZOW_ID)
                    .name(CITY_RZESZOW_NAME)
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
                    .build();
            secondUserOptional = Optional.of(secondUser);

            ZonedDateTime eventCreateDate = ZonedDateTime.now();
            ZonedDateTime eventStartDate = ZonedDateTime.now().plusDays(7).withSecond(0).withNano(0);

            event = Event.builder()
                    .id(EVENT_ID)
                    .owner(eventOwner)
                    .name(EVENT_NAME)
                    .shortDescription(EVENT_SHORT_DESCRIPTION)
                    .longDescription(EVENT_LONG_DESCRIPTION)
                    .createDate(eventCreateDate)
                    .timeZoneId(eventCreateDate.getZone().getId())
                    .eventStartDate(eventStartDate)
                    .lastUpdate(eventCreateDate)
                    .city(cityRzeszow)
                    .exactAddress(EVENT_EXACT_ADDRESS)
                    .build();

            eventOptional = Optional.of(event);
            eventOwner.addUserEvent(event);

        }

        @Nested
        @DisplayName("Adding attender to event tests:")
        class AddingAttenderToEventTests {

            @Test
            @DisplayName("When adding attender should try to load event from database")
            public void whenAddingAttenderShouldTryToLoadEventFromDatabase() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

                eventService.addAttenderToEvent(EVENT_ID, JWT_STRING);

                verify(eventRepository, times(1).description("Expected to look for event in database only once.")).findById(EVENT_ID);
            }

            @Test
            @DisplayName("When adding attender should throw EventNotFound if Event with given id does not exist")
            public void whenAddingAttenderShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

                assertThrows(EventNotFoundException.class, () -> eventService.addAttenderToEvent(EVENT_ID, JWT_STRING), "Expected to throw EventNotFoundException if even does not exist.");
            }

            @Test
            @DisplayName("When adding attender should throw EventAlreadyHadPlaceException if event start date is in the past")
            public void whenAddingAttenderShouldThrowEventAlreadyHadPlaceExceptionIfEventStartDateIsInThePast(){
                ZonedDateTime eventStartDateFromPast = ZonedDateTime.now().minusDays(3);
                event.setEventStartDate(eventStartDateFromPast);
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);

                assertThrows(EventAlreadyHadPlaceException.class, () -> eventService.addAttenderToEvent(EVENT_ID, JWT_STRING));
            }

            @Test
            @DisplayName("When adding attender should throw EventOwnerAlreadyAttendsEventException if event owner performs attend action")
            public void whenAddingAttenderShouldThrowEventOwnerAlreadyAttendsEventExceptionIfEventOwnerPerformsAttendAction(){
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);

                assertThrows(EventOwnerAlreadyAttendsEventException.class, () -> eventService.addAttenderToEvent(EVENT_ID, JWT_STRING));
            }

            @Test
            @DisplayName("When adding attender should throw AlreadyAttendingEventException if attending user performs attend action more times")
            public void whenAddingAttenderShouldThrowAlreadyAttendingEventExceptionIfAttendingUserPerformsAttendActionMoreTimes(){
                event.addAttendingUser(secondUser);
                secondUser.addAttendingEvent(event);
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

                assertThrows(AlreadyAttendingEventException.class, () -> eventService.addAttenderToEvent(EVENT_ID, JWT_STRING));
            }

            @Test
            @DisplayName("When adding attender should extract username from jwt")
            public void whenAddingAttenderShouldExtractUsernameFromJwt() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);

                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

                eventService.addAttenderToEvent(EVENT_ID, JWT_STRING);

                verify(jwtUtil, times(1).description("Expected to extract user email from provided JWT.")).extractUsername(JWT_STRING);

            }

            @Test
            @DisplayName("When adding attender should load user from database with extracted from jwt username")
            public void whenAddingAttenderShouldLoadUserFromDatabaseWithExtractedFromJwtUsername() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

                eventService.addAttenderToEvent(EVENT_ID, JWT_STRING);

                verify(userRepository, times(1).description("Expected to load user from database only once.")).findByEmail(SECOND_USER_EMAIL);
            }


            @Test
            @DisplayName("When adding attender should add performing user to event attender list")
            public void whenAddingAttenderShouldAddRetrievedUserToEventAttenderList() {
                Event eventSpy = Mockito.spy(event);
                when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(eventSpy));
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

                eventService.addAttenderToEvent(EVENT_ID, JWT_STRING);

                verify(eventSpy, times(1).description("Expected to add user via event object method.")).addAttendingUser(secondUser);

                assertTrue(secondUser.getAttendingEvents().contains(eventSpy), "Expected to user attendingEvents field to contain new event.");
                assertTrue(eventSpy.getAttendingUsers().contains(secondUser), "Expected to event attendingUsers field to contain new user.");
            }

            @Test
            @DisplayName("When adding attender should save changes to database")
            public void whenAddingAttenderShouldSaveChangesToDatabase() {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

                eventService.addAttenderToEvent(EVENT_ID, JWT_STRING);

                verify(eventRepository, times(1).description("Expected to save event entity.")).save(event);
            }

        }

        @Nested
        @DisplayName("Removing attender from event tests:")
        class RemovingAttenderFromEventTests{

            @BeforeEach
            void setUp() {
                event.getAttendingUsers().add(secondUser);
                secondUser.getAttendingEvents().add(event);
            }

            @Test
            @DisplayName("When removing attender from event should try to load event with given id from database.")
            public void whenRemovingAttenderFromEventShouldTryToLoadEventWithGivenIdFromDatabase(){
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

                eventService.removeAttenderFromEvent(EVENT_ID, JWT_STRING);
                verify(eventRepository, times(1).description("Expected to load event only once.")).findById(EVENT_ID);
                verify(eventRepository, times(1).description("Expected to load event only once.")).findById(any(UUID.class));
            }

            @Test
            @DisplayName("When removing attender from event should throw EventNotFoundException if event with given id does not exist.")
            public void whenRemovingAttenderFromEventShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist(){
                when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

                assertThrows(EventNotFoundException.class, () -> eventService.removeAttenderFromEvent(EVENT_ID, JWT_STRING));
            }

            @Test
            @DisplayName("When removing attender from event should check if event had place by event object method.")
            public void whenRemovingAttenderFromEventShouldIfEventHadPlaceByEventObjectMethod(){
                Event eventSpy = Mockito.spy(event);
                when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(eventSpy));
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

                eventService.removeAttenderFromEvent(EVENT_ID, JWT_STRING);
                verify(eventSpy, times(1).description("Expected to check if event had place with event object method.")).hadPlace();
            }

            @Test
            @DisplayName("When removing attender from event should throw EventAlreadyHadPlaceException if event had place.")
            public void whenRemovingAttenderFromEventShouldThrowEventAlreadyHadPlaceExceptionIfEventHadPlace(){
                event.setEventStartDate(ZonedDateTime.now().minusDays(5));
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);

                assertThrows(EventAlreadyHadPlaceException.class, () -> eventService.removeAttenderFromEvent(EVENT_ID, JWT_STRING));
            }
            @Test
            @DisplayName("When removing attender from event should extract user email from jwt.")
            public void whenRemovingAttenderFromEventShouldExtractUserEmailFromJwt(){
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

                eventService.removeAttenderFromEvent(EVENT_ID, JWT_STRING);

                verify(jwtUtil, times(1).description("Expected to extract user email using jwtUtils from jwt.")).extractUsername(JWT_STRING);
            }

            @Test
            @DisplayName("When removing attender from event should throw EventOwnerMustAttendEventException if event owner performs \"Don\'t attend\" action.")
            public void whenRemovingAttenderFromEventShouldThrowEventOwnerMustAttendEventExceptionIfEventOwnerPerformsDontAttendAction(){
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);

                assertThrows(EventOwnerMustAttendEventException.class, () -> eventService.removeAttenderFromEvent(EVENT_ID, JWT_STRING), "Expected to throw EventOwnerMustAttendEventException if owner perform \"Don\'t attend\" action.");
            }

            @Test
            @DisplayName("When removing attender from event should check if user is attending event with event object method.")
            public void whenRemovingAttenderFromEventShouldCheckIfUserIsAttendingEventWithEventObjectMethod(){
                Event eventSpy = Mockito.spy(event);
                when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(eventSpy));
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

                eventService.removeAttenderFromEvent(EVENT_ID, JWT_STRING);

                verify(eventSpy,times(1).description("Expected to check for attendance with isUserAttending method.")).isUserAttending(secondUser);
            }
            @Test
            @DisplayName("When removing attender from event should throw NotEventAttenderException if not attending user performs \"Don\'t attend\" action.")
            public void whenRemovingAttenderFromEventShouldThrowNotEventAttenderExceptionIfNotAttendingUserPerformsDontAttendAction(){
                event.getAttendingUsers().remove(secondUser);
                secondUser.getUserEvents().remove(event);
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

                assertThrows(NotEventAttenderException.class, () -> eventService.removeAttenderFromEvent(EVENT_ID, JWT_STRING), "Expected to throw NotEventAttenderException if performing user is not attending event.");
            }

            @Test
            @DisplayName("When removing attender from event should remove performing user from attending users list.")
            public void whenRemovingAttenderFromEventShouldRemovePerformingUserFromAttendingUsersList(){
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

                eventService.removeAttenderFromEvent(EVENT_ID, JWT_STRING);
                assertFalse(event.isUserAttending(secondUser), "Expected to remove performing user from event attending users list.");
            }

            @Test
            @DisplayName("When removing attender from event should remove event from performing user attending events list.")
            public void whenRemovingAttenderFromEventShouldRemoveEventFromPerformingUserAttendingEventsList(){
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

                eventService.removeAttenderFromEvent(EVENT_ID, JWT_STRING);
                assertFalse(secondUser.getAttendingEvents().contains(event), "Expected to remove event with given id to be removed from user attending events list.");
            }

            @Test
            @DisplayName("When removing attender from event should remove all threads created by user in event from user threads field.")
            public void whenRemovingAttenderFromEventShouldRemoveAllThreadsCreatedByUserInEventFromUserThreadsField(){
                ZonedDateTime threadCreateDate = ZonedDateTime.now().minusHours(2);
                Thread testThread = Thread.builder()
                        .name(FIRST_THREAD_NAME)
                        .content(FIRST_THREAD_CONTENT)
                        .owner(secondUser)
                        .createDate(threadCreateDate)
                        .lastUpdate(threadCreateDate)
                        .id(THREAD_ID)
                        .event(event)
                        .build();
                event.addThread(testThread);
                secondUser.addThread(testThread);

                Thread secondTestThread = Thread.builder()
                        .name(FIRST_THREAD_NAME)
                        .content(FIRST_THREAD_CONTENT)
                        .owner(eventOwner)
                        .createDate(threadCreateDate)
                        .lastUpdate(threadCreateDate)
                        .id(THREAD_ID)
                        .event(event)
                        .build();
                eventOwner.addThread(secondTestThread);
                event.addThread(secondTestThread);



                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

                eventService.removeAttenderFromEvent(EVENT_ID, JWT_STRING);

                assertTrue(event.getThreads().contains(testThread), "Expected to persist event thread created by user which stopped attending.");
                assertTrue(testThread.isUserOwner(secondUser), "Expected event thread owner to remain original");
                assertFalse(secondUser.getThreads().contains(testThread), "Expected to remove thread from user threads.");
                assertTrue(event.containsThread(secondTestThread), "Expected to not remove threads that were not created by other users.");
            }

            @Test
            @DisplayName("When removing attender from event should remove all thread replies created by user in event from user thread replies field.")
            public void whenRemovingAttenderFromEventShouldRemoveAllThreadRepliesCreatedByUserInEventThreadsFromUserThreadRepliesField(){
                final UUID SECOND_THREAD_REPLY_ID = UUID.randomUUID();

                ZonedDateTime threadCreateDate = ZonedDateTime.now().minusHours(2);
                Thread testThread = Thread.builder()
                        .name(FIRST_THREAD_NAME)
                        .content(FIRST_THREAD_CONTENT)
                        .owner(secondUser)
                        .createDate(threadCreateDate)
                        .lastUpdate(threadCreateDate)
                        .id(THREAD_ID)
                        .event(event)
                        .build();
                event.addThread(testThread);
                secondUser.addThread(testThread);

                Thread secondTestThread = Thread.builder()
                        .name(FIRST_THREAD_NAME)
                        .content(FIRST_THREAD_CONTENT)
                        .owner(eventOwner)
                        .createDate(threadCreateDate)
                        .lastUpdate(threadCreateDate)
                        .id(THREAD_ID)
                        .event(event)
                        .build();
                eventOwner.addThread(secondTestThread);
                event.addThread(secondTestThread);

                ZonedDateTime threadReplyCreateDate = ZonedDateTime.now();

                ThreadReply firstThreadReply =  ThreadReply.builder()
                        .id(THREAD_REPLY_ID)
                        .thread(testThread)
                        .content(THREAD_REPLY_CONTENT)
                        .replyDate(threadReplyCreateDate)
                        .lastUpdate(threadReplyCreateDate)
                        .replier(secondUser)
                        .editCounter(0)
                        .build();
                ThreadReply secondThreadReply =  ThreadReply.builder()
                        .id(SECOND_THREAD_REPLY_ID)
                        .thread(secondTestThread)
                        .content(THREAD_REPLY_CONTENT)
                        .replyDate(threadReplyCreateDate)
                        .lastUpdate(threadReplyCreateDate)
                        .replier(secondUser)
                        .editCounter(0)
                        .build();
                testThread.addReplyToThread(firstThreadReply);
                secondTestThread.addReplyToThread(secondThreadReply);

                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

                eventService.removeAttenderFromEvent(EVENT_ID, JWT_STRING);

                assertTrue(testThread.getReplies().contains(firstThreadReply), "Expected to persist event thread reply created by user which stopped attending.");
                assertTrue(secondTestThread.getReplies().contains(secondThreadReply), "Expected to persist event thread reply created by user which stopped attending.");
                assertTrue(firstThreadReply.isReplier(secondUser), "Expected event thread replier to remain original.");
                assertTrue(secondThreadReply.isReplier(secondUser), "Expected event thread replier to remain original.");
                assertFalse(secondUser.getThreadReplies().contains(firstThreadReply), "Expected to remove thread reply from user threadReplies.");
                assertFalse(secondUser.getThreadReplies().contains(secondThreadReply), "Expected to remove thread reply from user threadReplies.");
            }

            @Test
            @DisplayName("When removing attender from event shouldRemove all files added to event by performing user from user files field.")
            public void whenRemovingAttenderFromEventShouldRemoveAllFilesAddedToEventByPerformingUserFromUserFilesField(){
                File testFile = File.builder()
                        .id(FILE_ID)
                        .userFileName(FILE_NAME)
                        .content(null)
                        .contentType(null)
                        .event(event)
                        .owner(secondUser)
                        .build();
                event.addFile(testFile);
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

                eventService.removeAttenderFromEvent(EVENT_ID, JWT_STRING);

                assertEquals(testFile.getEvent(), event, "Expected event field in file to not be changed.");
                assertTrue(event.getFiles().contains(testFile), "Expected the file to remain in event files field.");
                assertFalse(secondUser.getFiles().contains(testFile), "Expected to remove file from user files field.");
                assertEquals(secondUser,testFile.getOwner(), "Expected to leave file original file owner.");
            }

            @Test
            @DisplayName("When removing attender from event should save event using eventRepository.")
            public void whenRemovingAttenderFromEventShouldSaveEventUsingEventRepository(){
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

                eventService.removeAttenderFromEvent(EVENT_ID, JWT_STRING);
                verify(eventRepository, times(1).description("Expected to save event with eventRepository.")).save(event);
            }

            @Test
            @DisplayName("When removing attender from event should save user using userRepository.")
            public void whenRemovingAttenderFromEventShouldSaveUserUsingUserRepository(){
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

                eventService.removeAttenderFromEvent(EVENT_ID, JWT_STRING);
                verify(userRepository, times(1).description("Expected to save user using userRepository.")).save(secondUser);
            }

        }
    }

    @Nested
    @DisplayName("Event file tests:")
    class EventFileTests{

        FileUploadDto fileUploadDto;
        MockMultipartFile mockMultipartFile;
        private final String FILE_NAME = "file";
        private final String FILE_NAME_USER = "Example Photo";
        private final String FILE_NAME_ORIGINAL ="example-photo.png";
        private final ContentType FILE_CONTENT_TYPE = ContentType.IMAGE_PNG;


        @BeforeEach
        void setUp() {
            eventService = new EventServiceImpl(eventRepository, cityRepository, tagRepository, userRepository, threadRepository, threadReplyRepository, fileRepository, notificationService, jwtUtil, fileUtils);

            cityRzeszow = City.builder()
                    .id(CITY_RZESZOW_ID)
                    .name(CITY_RZESZOW_NAME)
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
                    .build();
            secondUserOptional = Optional.of(secondUser);

            ZonedDateTime eventCreateDate = ZonedDateTime.now();
            ZonedDateTime eventStartDate = ZonedDateTime.now().plusDays(7).withSecond(0).withNano(0);

            event = Event.builder()
                    .id(EVENT_ID)
                    .owner(eventOwner)
                    .name(EVENT_NAME)
                    .shortDescription(EVENT_SHORT_DESCRIPTION)
                    .longDescription(EVENT_LONG_DESCRIPTION)
                    .createDate(eventCreateDate)
                    .timeZoneId(eventCreateDate.getZone().getId())
                    .eventStartDate(eventStartDate)
                    .lastUpdate(eventCreateDate)
                    .city(cityRzeszow)
                    .exactAddress(EVENT_EXACT_ADDRESS)
                    .build();

            eventOptional = Optional.of(event);
            eventOwner.addUserEvent(event);

            mockMultipartFile = new MockMultipartFile(
                    FILE_NAME,
                    FILE_NAME_ORIGINAL,
                    FILE_CONTENT_TYPE.getMimeType(),
                    TestFileContentFactory.png()
            );

            fileUploadDto = FileUploadDto.builder()
                    .file(mockMultipartFile)
                    .userFileName(FILE_NAME_USER)
                    .build();

        }

        @Nested
        @DisplayName("Upload file tests:")
        class UploadFileTests{

            private record TestFileData(String extension, String contentType, byte[] bytes) {}

            static Stream<TestFileData> allowedFileProvider() {
                return Stream.of(
                        new TestFileData(".jpg", "image/jpeg", TestFileContentFactory.jpg()),
                        new TestFileData(".jpeg", "image/jpeg", TestFileContentFactory.jpeg()),
                        new TestFileData(".png", "image/png", TestFileContentFactory.png()),
                        new TestFileData(".pdf", "application/pdf", TestFileContentFactory.pdf()),
                        new TestFileData(".doc", "application/msword", TestFileContentFactory.doc()),
                        new TestFileData(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", TestFileContentFactory.docx()),
                        new TestFileData(".ppt", "application/vnd.ms-powerpoint", TestFileContentFactory.ppt()),
                        new TestFileData(".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation", TestFileContentFactory.pptx()),
                        new TestFileData(".odt", "application/vnd.oasis.opendocument.text", TestFileContentFactory.odt()),
                        new TestFileData(".xls", "application/vnd.ms-excel", TestFileContentFactory.xls()),
                        new TestFileData(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", TestFileContentFactory.xlsx()),
                        new TestFileData(".mp4", "video/mp4", TestFileContentFactory.mp4()),
                        new TestFileData(".avi", "video/x-msvideo", TestFileContentFactory.avi())
                );
            }

            static Stream<TestFileData> disallowedFileProvider() {
                return Stream.of(
                        new TestFileData(".exe", "application/octet-stream", new byte[]{0x4D, 0x5A, 0x50, 0x00}),
                        new TestFileData(".bat", "text/plain", "@echo off".getBytes()),
                        new TestFileData(".zip", "application/zip", new byte[]{0x50, 0x4B, 0x03, 0x04}),
                        new TestFileData(".js", "application/javascript", "alert('hack');".getBytes()),
                        new TestFileData(".sh", "application/x-sh", "echo test".getBytes())
                );
            }

            @Test
            @DisplayName("When uploading file should try to load from database event with given id")
            public void whenUploadingFileShouldTryToLoadFromDatabaseEventWithGivenId() throws IOException {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);

                eventService.uploadFileToEvent(fileUploadDto, EVENT_ID, JWT_STRING);

                verify(eventRepository, times(1).description("Expected to run query once.")).findById(EVENT_ID);
            }

            @Test
            @DisplayName("When uploading file should throw EventNotFoundException if event with given id does not exist")
            public void whenUploadingFileShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist(){
                when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

                assertThrows(EventNotFoundException.class, () -> eventService.uploadFileToEvent(fileUploadDto, EVENT_ID, JWT_STRING));
            }

            @Test
            @DisplayName("When uploading file should extract performing user email from jwt using jwtUtils")
            public void whenUploadingFileShouldExtractPerformingUserEmailFromJwtUsingJwtUtils() throws IOException {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);

                eventService.uploadFileToEvent(fileUploadDto, EVENT_ID, JWT_STRING);

                verify(jwtUtil, times(1).description("Expected to extract user email from jwt.")).extractUsername(JWT_STRING);
            }

            @Test
            @DisplayName("When uploading file should load performing user from database")
            public void whenUploadingFileShouldLoadPerformingUserFromDatabase() throws IOException {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);

                eventService.uploadFileToEvent(fileUploadDto, EVENT_ID, JWT_STRING);

                verify(userRepository, times(1).description("Expected to load performing user from database")).findByEmail(EVENT_OWNER_EMAIL);
            }

            @Test
            @DisplayName("When uploading file should throw NotEventAttenderException if performing user do not attend event with given id")
            public void whenUploadingFileShouldThrowNotEventAttenderExceptionIfPerformingUserDoNotAttendEventWithGivenId() throws IOException {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);

                assertThrows(NotEventAttenderException.class, () -> eventService.uploadFileToEvent(fileUploadDto, EVENT_ID, JWT_STRING), "Expected to throw NotEventAttenderException if user is not attending event.");
            }

            @Test
            @DisplayName("When uploading file should throw EmptyUploadedFileException if file is empty")
            public void whenUploadingFileShouldThrowEmptyUploadedFileExceptionIfFileIsEmpty() throws IOException {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                MockMultipartFile emptyFile =  new MockMultipartFile(
                        FILE_NAME,
                        FILE_NAME_ORIGINAL,
                        FILE_CONTENT_TYPE.getMimeType(),
                        new byte[0]
                );
                fileUploadDto.setFile(emptyFile);

                assertThrows(EmptyUploadedFileException.class, () -> eventService.uploadFileToEvent(fileUploadDto, EVENT_ID, JWT_STRING), "Expected to throw EmptyUploadedFileException if file has no content.");
            }

            @ParameterizedTest
            @MethodSource("allowedFileProvider")
            @DisplayName("When uploading file should not throw FileTypeNotAllowedException if detected file type is on white list")
            public void whenUploadingFileShouldNotThrowFileTypeNotAllowedExceptionIfDetectedFileTypeIsOnWhiteList(TestFileData testFileData) throws IOException {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);

                MockMultipartFile file = new MockMultipartFile(
                        "file",
                        "allowed" + testFileData.extension(),
                        testFileData.contentType(),
                        testFileData.bytes()
                );

                fileUploadDto = new FileUploadDto("Allowed file", file);

                assertDoesNotThrow(() -> eventService.uploadFileToEvent(fileUploadDto, EVENT_ID, JWT_STRING), "Expected to not throw any exception, and FileTypeNotAllowedException in particular.");
            }

            @ParameterizedTest
            @MethodSource("disallowedFileProvider")
            @DisplayName("When uploading file should throw FileTypeNotAllowedException if detected file type is not on whitelist")
            void whenUploadingFileShouldThrowFileTypeNotAllowedExceptionIfDetectedFileTypeIsNotOnWhitelist(TestFileData testFileData) throws IOException {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);

                MockMultipartFile file = new MockMultipartFile(
                        "file",
                        "malware" + testFileData.extension(),
                        testFileData.contentType(),
                        testFileData.bytes()
                );

                fileUploadDto = new FileUploadDto("Malware file", file);

                assertThrows(FileTypeNotAllowedException.class, () -> eventService.uploadFileToEvent(fileUploadDto, EVENT_ID, JWT_STRING));
            }

            @Test
            @DisplayName("When uploading file should set relationship between file on user and event")
            public void whenUploadingFileShouldSetRelationshipBetweenFileOnUserAndEvent() throws IOException {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);

                ArgumentCaptor<File> fileArgumentCaptor = ArgumentCaptor.forClass(File.class);
                eventService.uploadFileToEvent(fileUploadDto, EVENT_ID, JWT_STRING);

                verify(fileRepository, times(1).description("")).save(fileArgumentCaptor.capture());
                File capturedFile = fileArgumentCaptor.getValue();

                assertEquals(eventOwner, capturedFile.getOwner(), "Expected to set correct file owner.");
                assertTrue(eventOwner.getFiles().contains(capturedFile), "Expected to user files field to contain new file.");
                assertTrue(event.getFiles().contains(capturedFile), "Expected event files field to contain new file.");
                assertEquals(event, capturedFile.getEvent(), "Expected to event be set correct event in file.");
            }

            @Test
            @DisplayName("When uploading file should save it in database with correct data")
            public void whenUploadingFileShouldSaveItInDatabaseWithCorrectData() throws IOException {
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);

                ArgumentCaptor<File> fileArgumentCaptor = ArgumentCaptor.forClass(File.class);

                eventService.uploadFileToEvent(fileUploadDto, EVENT_ID, JWT_STRING);

                verify(fileRepository, times(1).description("Expected to save new file in database.")).save(fileArgumentCaptor.capture());

                File capturedFile = fileArgumentCaptor.getValue();
                assertEquals(FILE_NAME_ORIGINAL, capturedFile.getOriginalFileName(), "Expected original file name field to be set.");
                assertEquals(FILE_NAME_USER, capturedFile.getUserFileName(), "Expected user file name to be set");
                assertEquals(FILE_CONTENT_TYPE.getMimeType(), capturedFile.getContentType(), "Expected content type to be set.");
                assertEquals(fileUploadDto.getFile().getBytes(), capturedFile.getContent(), "Expected the content of file to be set and unchanged");
            }

        }

        @Nested
        @DisplayName("Get file by id tests:")
        class GetFileByIdTests{

            private File fileToServe;
            private Optional<File> fileToServeOptional;

            @BeforeEach
            void setUp() {
                ZonedDateTime eventCreateDate = ZonedDateTime.now();
                ZonedDateTime eventStartDate = ZonedDateTime.now().plusDays(7).withSecond(0).withNano(0);

                event = Event.builder()
                        .id(EVENT_ID)
                        .owner(eventOwner)
                        .name(EVENT_NAME)
                        .shortDescription(EVENT_SHORT_DESCRIPTION)
                        .longDescription(EVENT_LONG_DESCRIPTION)
                        .createDate(eventCreateDate)
                        .timeZoneId(eventCreateDate.getZone().getId())
                        .eventStartDate(eventStartDate)
                        .lastUpdate(eventCreateDate)
                        .city(cityRzeszow)
                        .exactAddress(EVENT_EXACT_ADDRESS)
                        .build();

                fileToServe = File.builder()
                        .id(FILE_ID)
                        .userFileName(FILE_NAME_USER)
                        .originalFileName(FILE_NAME_ORIGINAL)
                        .contentType(FILE_CONTENT_TYPE.getMimeType())
                        .content(TestFileContentFactory.png())
                        .owner(eventOwner)
                        .event(event)
                        .build();


                eventOwner.addFile(fileToServe);
                event.addFile(fileToServe);

                fileToServeOptional = Optional.of(fileToServe);
            }

            @Test
            @DisplayName("When getting file by id should look up file using fileRepository findByIdAndEventId method")
            public void whenGettingFileByIdShouldLookForFileUsingFileRepositoryFindByIdAndEventIdMethod(){
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(fileRepository.findByIdAndEventId(FILE_ID, EVENT_ID)).thenReturn(fileToServeOptional);

                eventService.getFileById(FILE_ID, EVENT_ID, JWT_STRING);

                verify(fileRepository, times(1).description("Expected to look up event via fileRepository findByIdAndEventId method")).findByIdAndEventId(FILE_ID, EVENT_ID);
            }

            @Test
            @DisplayName("When getting file by id should throw FileNotFoundInEventException if there is no file with given id in event with given id.")
            public void whenGettingFileByIdShouldThrowFileNotFoundInEventExceptionIfThereIsNoFileWithGivenIdInEventWithGivenId(){
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(fileRepository.findByIdAndEventId(FILE_ID, EVENT_ID)).thenReturn(Optional.empty());

                assertThrows(FileNotFoundInEventException.class, () -> eventService.getFileById(FILE_ID, EVENT_ID, JWT_STRING),"Expected to throw FileNotFoundInEventException if event with given id does not contain file with given id or file is not associated.");
            }

            @Test
            @DisplayName("When getting file by id should extract user email from provided jwtString")
            public void whenGettingFileByIdShouldExtractUserEmailFromProvidedJwtString(){
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(fileRepository.findByIdAndEventId(FILE_ID, EVENT_ID)).thenReturn(fileToServeOptional);

                eventService.getFileById(FILE_ID, EVENT_ID, JWT_STRING);

                verify(jwtUtil, times(1).description("Expected to extract performing user email from provided jwt using jwtUtils extractUserName method.")).extractUsername(JWT_STRING);
            }

            @Test
            @DisplayName("When getting file by id should look up performing user using extracted email via userRepository findByEmail method")
            public void whenGettingFileByIdShouldLookUpPerformingUserUsingExtractedEmailViaUserRepositoryFindByEmailMethod(){
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(fileRepository.findByIdAndEventId(FILE_ID, EVENT_ID)).thenReturn(fileToServeOptional);

                eventService.getFileById(FILE_ID, EVENT_ID, JWT_STRING);

                verify(userRepository, times(1).description("Expected to look up performing user using eventRepository findByEmail with extracted from jwt token email")).findByEmail(EVENT_OWNER_EMAIL);
            }
            @Test
            @DisplayName("When getting file by id should look up event via eventRepository findById method")
            public void whenGettingFileByIdShouldLookUpEventViaEventRepositoryFindByIdMethod(){
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(fileRepository.findByIdAndEventId(FILE_ID, EVENT_ID)).thenReturn(fileToServeOptional);

                eventService.getFileById(FILE_ID, EVENT_ID, JWT_STRING);

                verify(eventRepository, times(1).description("Expected to load event using eventRepository findById method.")).findById(EVENT_ID);
            }
            @Test
            @DisplayName("When getting file by id should throw EventNotFoundException if there is no event with given id")
            public void whenGettingFileByIdShould(){
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

                assertThrows(EventNotFoundException.class, () -> eventService.getFileById(FILE_ID, EVENT_ID, JWT_STRING), "Expected to throw EventNotFoundException if event with given id does not exist.");
            }

            @Test
            @DisplayName("When getting file by id should check if performing user is attending event with given id")
            public void whenGettingFileByIdShouldCheckIfPerformingUserIsAttendingEventWithGivenId(){
                Event eventSpy = Mockito.spy(event);

                fileToServe.setEvent(eventSpy);
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(eventSpy));
                when(fileRepository.findByIdAndEventId(FILE_ID, EVENT_ID)).thenReturn(fileToServeOptional);

                eventService.getFileById(FILE_ID, EVENT_ID, JWT_STRING);

                verify(eventSpy, times(1).description("Expected to check if performing user is attending event with given id.")).isUserAttending(eventOwner);
            }

            @Test
            @DisplayName("When getting file by id should throw NotEventAttenderException if performing user is not attending event with given id")
            public void whenGettingFileByIdShouldThrowNotEventAttenderExceptionIfPerformingUserIsNotAttendingEventWithGivenId(){
                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(SECOND_USER_EMAIL);
                when(userRepository.findByEmail(SECOND_USER_EMAIL)).thenReturn(secondUserOptional);
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);


                assertThrows(NotEventAttenderException.class, () -> eventService.getFileById(FILE_ID, EVENT_ID, JWT_STRING), "Expected to throw NotEventAttenderException if not attending user tries to download file.");
            }

            @Test
            @DisplayName("When getting file by id should return correct file entity")
            public void whenGettingFileByIdShouldReturnCorrectEntity(){

                when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(EVENT_OWNER_EMAIL);
                when(userRepository.findByEmail(EVENT_OWNER_EMAIL)).thenReturn(eventOwnerOptional);
                when(eventRepository.findById(EVENT_ID)).thenReturn(eventOptional);
                when(fileRepository.findByIdAndEventId(FILE_ID, EVENT_ID)).thenReturn(fileToServeOptional);

                File returnedFile = eventService.getFileById(FILE_ID, EVENT_ID, JWT_STRING);

                assertEquals(fileToServe, returnedFile, "Expected returned file to be the same as the one loaded from database.");
                assertEquals(fileToServe.getContent(), returnedFile.getContent(), "Expected to return file with not changed content.");
            }
        }


    }

    @Disabled
    @Nested
    @DisplayName("Search events test:")
    class SearchEventsTests {
        @BeforeEach
        void setUp() {

        }

    }

}