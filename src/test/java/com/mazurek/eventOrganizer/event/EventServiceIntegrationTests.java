package com.mazurek.eventOrganizer.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mazurek.eventOrganizer.auth.AuthenticationRequest;
import com.mazurek.eventOrganizer.auth.AuthenticationServiceImpl;
import com.mazurek.eventOrganizer.auth.RegisterRequest;
import com.mazurek.eventOrganizer.auth.VerificationTokenRepository;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.dto.EventDto;
import com.mazurek.eventOrganizer.exception.city.CityNotFoundException;
import com.mazurek.eventOrganizer.exception.event.EventAlreadyHadPlaceException;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotEventAttenderException;
import com.mazurek.eventOrganizer.exception.event.NotEventOwnerException;
import com.mazurek.eventOrganizer.exception.tag.TagNotFoundException;
import com.mazurek.eventOrganizer.exception.thread.*;
import com.mazurek.eventOrganizer.exception.user.UserAlreadyExistException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.jwt.JwtUtil;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.tag.TagRepository;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.thread.ThreadReply;
import com.mazurek.eventOrganizer.thread.ThreadReplyRepository;
import com.mazurek.eventOrganizer.thread.ThreadRepository;
import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadReplyCreateDto;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class EventServiceIntegrationTests {


    private final String THREAD_REPLY_CONTENT = "this is reply in thread, let's see how it works";
    private final  String THREAD_REPLY_CONTENT_UPDATE = "this is updated content for thread reply" ;
    private final String FIRST_USER_EMAIL = "testowe.andrzej.testowe+usr1@gmail.com";
    private final String FIRST_USER_FIRST_NAME = "Andrzej";
    private final String FIRST_USER_LAST_NAME = "Kotarski";
    private final String FIRST_USER_HOME_CITY = "Rzeszow";

    private final String SECOND_USER_EMAIL = "testowe.andrzej.testowe+usr2@gmail.com";
    private final String SECOND_USER_FIRST_NAME = "Jaroslaw";
    private final String SECOND_USER_LAST_NAME = "Kielbasa";
    private final String SECOND_USER_HOME_CITY = "Krakow";

    private final String USER_PASSWORD = "passwo0rD#";

    private final String EVENT_NAME = "FIRST EVENT";
    private final String EVENT_SHORT_DESCRIPTION = "FIRST event short description";
    private final String EVENT_LONG_DESCRIPTION = "FIRST event long description. It have to contain at least 250 characters so you have to be a little more descriptive about it. " +
            "Don't get mad, it have to be like this to prevent some abusive users from creating them for no reason. FIRST event long description. It have to contain at least 250 characters so you have to be a little more descriptive about it" +
            "Don't get mad, it have to be like this to prevent some abusive users from creating them for no reason.";
    private final ZonedDateTime EVENT_START_DATE = ZonedDateTime.now().plusDays(7).withSecond(0).withNano(0);
    private final String EVENT_CITY = "Rzeszow";
    private final String EVENT_EXACT_ADDRESS = "Ul. Moniuszki 8";
    private final String[] EVENT_TAGS = {"JAVA", "spring", "tech"};

    private final String EVENT_CITY_NOT_EXISTING = "Warszawa";

    private final String THREAD_NAME = "First thread name";
    private final String THREAD_CONTENT = "First thread content, it should not";

    private UUID wrongEventId = UUID.randomUUID();
    private UUID wrongThreadId = UUID.randomUUID();
    private UUID wrongThreadReplyId = UUID.randomUUID();

    private final AuthenticationRequest  firstUserAuthRequest = new AuthenticationRequest(FIRST_USER_EMAIL, USER_PASSWORD);
    private final AuthenticationRequest  secondUserAuthRequest = new AuthenticationRequest(SECOND_USER_EMAIL, USER_PASSWORD);

    private String firstUserJwt;
    private String secondUserJwt;
    private EventCreateDto eventCreateDto;
    private EventCreateDto eventUpdateDto;

    private ThreadCreateDto threadCreateDto;
    private ThreadCreateDto threadUpdateDto;

    private ThreadReplyCreateDto threadReplyCreateDto;
    private ThreadReplyCreateDto threadReplyUpdateDto;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthenticationServiceImpl authenticationService;
    @Autowired
    private VerificationTokenRepository verificationTokenRepository;
    @Autowired
    private EventService eventService;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private ThreadRepository threadRepository;
    @Autowired
    private ThreadReplyRepository threadReplyRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    EntityManager entityManager;
    @Autowired
    JwtUtil jwtUtil;

    @PostConstruct
    void beforeAll() {

        final RegisterRequest firstUserRegisterRequest = RegisterRequest.builder()
                .firstName(FIRST_USER_FIRST_NAME)
                .lastName(FIRST_USER_LAST_NAME)
                .email(FIRST_USER_EMAIL)
                .emailConfirmation(FIRST_USER_EMAIL)
                .password(USER_PASSWORD)
                .passwordConfirmation(USER_PASSWORD)
                .homeCity(FIRST_USER_HOME_CITY)
                .build();
        final RegisterRequest secondUserRegisterRequest = RegisterRequest.builder()
                .firstName(SECOND_USER_FIRST_NAME)
                .lastName(SECOND_USER_LAST_NAME)
                .email(SECOND_USER_EMAIL)
                .emailConfirmation(SECOND_USER_EMAIL)
                .password(USER_PASSWORD)
                .passwordConfirmation(USER_PASSWORD)
                .homeCity(SECOND_USER_HOME_CITY)
                .build();

        try {
            authenticationService.register(firstUserRegisterRequest);
            authenticationService.activateAccount(verificationTokenRepository.findByUserEmail(FIRST_USER_EMAIL).get().getId());
        } catch (UserAlreadyExistException userAlreadyExistException) {
            System.out.println("First user already exits, processing to tests.");
        }

        try {
            authenticationService.register(secondUserRegisterRequest);
            authenticationService.activateAccount(verificationTokenRepository.findByUserEmail(SECOND_USER_EMAIL).get().getId());
        } catch (UserAlreadyExistException userAlreadyExistException) {
            System.out.println("Second user already exits, processing to tests.");
        }

        firstUserJwt = authenticationService.authenticate(firstUserAuthRequest).getToken();
        secondUserJwt = authenticationService.authenticate(secondUserAuthRequest).getToken();
    }

    @Nested
    @DisplayName("Event core tests:")
    class CoreEventTests{

        @BeforeEach
        void setUp() {
            eventCreateDto = EventCreateDto.builder()
                    .name(EVENT_NAME)
                    .shortDescription(EVENT_SHORT_DESCRIPTION)
                    .longDescription(EVENT_LONG_DESCRIPTION)
                    .eventStartDate(EVENT_START_DATE)
                    .city(EVENT_CITY)
                    .exactAddress(EVENT_EXACT_ADDRESS)
                    .tags(Arrays.stream(EVENT_TAGS).toList())
                    .build();
        }

        @Nested
        @Transactional
        @DisplayName("Create event tests:")
        class CreateEventTests{

            @Test
            @DisplayName("When creating event should save it with correct core data.")
            public void whenCreatingEventShouldSaveItWithCorrectData () throws Exception {
                EventDto eventDto = eventService.createEvent(eventCreateDto, firstUserJwt);

                assertTrue(eventRepository.findById(eventDto.getId()).isPresent());

                Event savedEvent = eventRepository.findById(eventDto.getId()).get();

                assertEquals(EVENT_NAME, savedEvent.getName());
                assertEquals(EVENT_SHORT_DESCRIPTION, savedEvent.getShortDescription());
                assertEquals(EVENT_LONG_DESCRIPTION, savedEvent.getLongDescription());
                assertTrue(savedEvent.getEventStartDate().isEqual(EVENT_START_DATE.withSecond(0).withNano(0)));
                assertEquals(eventCreateDto.getEventStartDate().getZone(), ZoneId.of(savedEvent.getTimeZoneId()));
                assertEquals(EVENT_EXACT_ADDRESS, savedEvent.getExactAddress());
                assertEquals(savedEvent.getCreateDate(),savedEvent.getLastUpdate());
            }

            @Test
            @DisplayName("When creating event should save dates with zeroed seconds and nanos")
            public void whenCreatingEventShouldSaveDatesWithZeroedSecondsAndNanos() throws Exception {
                ZonedDateTime dateWithSeconds = EVENT_START_DATE.withSecond(30).withNano(500);
                eventCreateDto.setEventStartDate(dateWithSeconds);

                EventDto eventDto = eventService.createEvent(eventCreateDto, firstUserJwt);
                Event savedEvent = eventRepository.findById(eventDto.getId())
                        .orElseThrow(EventNotFoundException::new);

                assertEquals(0, savedEvent.getEventStartDate().getSecond());
                assertEquals(0, savedEvent.getEventStartDate().getNano());
            }
            @Test
            @DisplayName("When creating event with different case city/tags names should treat them as same")
            public void whenCreatingEventWithDifferentCaseNamesShouldTreatThemAsSame() throws Exception {
                eventCreateDto.setCity(EVENT_CITY.toUpperCase());
                eventCreateDto.setTags(Arrays.asList(EVENT_TAGS[0].toUpperCase()));

                EventDto eventDto = eventService.createEvent(eventCreateDto, firstUserJwt);
                Event savedEvent = eventRepository.findById(eventDto.getId())
                        .orElseThrow(EventNotFoundException::new);

                assertEquals(EVENT_CITY.toLowerCase(), savedEvent.getCity().getName());
                assertTrue(savedEvent.getTags().stream()
                        .anyMatch(tag -> tag.getName().equals(EVENT_TAGS[0].toLowerCase())));
            }

            @Test
            @DisplayName("When creating event with empty tags list should save event without tags")
            public void whenCreatingEventWithEmptyTagsShouldSaveEventWithoutTags() throws Exception {
                eventCreateDto.setTags(new ArrayList<>());
                EventDto eventDto = eventService.createEvent(eventCreateDto, firstUserJwt);

                Event savedEvent = eventRepository.findById(eventDto.getId())
                        .orElseThrow(EventNotFoundException::new);

                assertTrue(savedEvent.getTags().isEmpty());
            }



            @Test
            @DisplayName("When creating event should use existing city and setup relationship if it exists.")
            public void whenCreatingEventShouldUseExistingCityAndSetupRelationShipIfItExists() throws Exception {
                assertTrue(cityRepository.findByIgnoreCaseName(EVENT_CITY).isPresent());

                EventDto eventDto = eventService.createEvent(eventCreateDto, firstUserJwt);

                City exitstingCity = cityRepository.findByIgnoreCaseName(EVENT_CITY).get();
                Event savedEvent = eventRepository.findById(eventDto.getId()).get();

                assertEquals(exitstingCity, savedEvent.getCity());
                assertTrue(exitstingCity.getEvents().contains(savedEvent));

                assertTrue(cityRepository.findByIgnoreCaseName(EVENT_CITY).isPresent());
            }
            @Test
            @DisplayName("When creating event should create city if it does not exist and setup relationship.")
            public void whenCreatingEventShouldCreateCityIfItDoesNotExistAndSetupRelationship () throws Exception {
                eventCreateDto.setCity(EVENT_CITY_NOT_EXISTING);

                assertTrue(cityRepository.findByIgnoreCaseName(EVENT_CITY_NOT_EXISTING).isEmpty());

                EventDto eventDto = eventService.createEvent(eventCreateDto, firstUserJwt);

                Event savedEvent = eventRepository.findById(eventDto.getId()).orElseThrow(EventNotFoundException::new);

                Optional<City> newCityOptional = cityRepository.findByIgnoreCaseName(EVENT_CITY_NOT_EXISTING);

                assertTrue(newCityOptional.isPresent());

                assertEquals(savedEvent.getCity(), newCityOptional.get());
                assertTrue(newCityOptional.get().getEvents().contains(savedEvent));

            }

            @Test
            @DisplayName("When creating event should create new tags and save them with relationships if they do not exist.")
            public void whenCreatingEventShouldCreateNewTagsAndSaveThemIfTheyDoNotExist() throws Exception {
                eventCreateDto.getTags().forEach(tag -> assertTrue(tagRepository.findByIgnoreCaseName(tag).isEmpty()));

                EventDto eventDto = eventService.createEvent(eventCreateDto, firstUserJwt);
                Event savedEvent = eventRepository.findById(eventDto.getId()).orElseThrow(EventNotFoundException::new);

                eventCreateDto.getTags().forEach(tag -> assertTrue(tagRepository.findByIgnoreCaseName(tag).isPresent()));

                Tag firstTag = tagRepository.findByIgnoreCaseName(EVENT_TAGS[0]).orElseThrow(TagNotFoundException::new);
                Tag secondTag = tagRepository.findByIgnoreCaseName(EVENT_TAGS[1]).orElseThrow(TagNotFoundException::new);
                Tag thirdTag = tagRepository.findByIgnoreCaseName(EVENT_TAGS[2]).orElseThrow(TagNotFoundException::new);

                assertTrue(savedEvent.getTags().contains(firstTag));
                assertTrue(savedEvent.getTags().contains(secondTag));
                assertTrue(savedEvent.getTags().contains(thirdTag));
                assertTrue(firstTag.getEvents().contains(savedEvent));
                assertTrue(secondTag.getEvents().contains(savedEvent));
                assertTrue(thirdTag.getEvents().contains(savedEvent));
            }
            @Test
            @DisplayName("When creating event should use and setup relationship with existing tags.")
            public void whenCreatingEventShouldCreateNewTagsAndSaveThemIfTheyDoNotExist2() throws Exception {
                tagRepository.save(new Tag(EVENT_TAGS[0]));
                tagRepository.save(new Tag(EVENT_TAGS[1]));

                EventDto eventDto = eventService.createEvent(eventCreateDto, firstUserJwt);
                Event savedEvent = eventRepository.findById(eventDto.getId()).orElseThrow(EventNotFoundException::new);

                Tag firstTag = tagRepository.findByIgnoreCaseName(EVENT_TAGS[0]).orElseThrow(TagNotFoundException::new);
                Tag secondTag = tagRepository.findByIgnoreCaseName(EVENT_TAGS[1]).orElseThrow(TagNotFoundException::new);
                Tag thirdTag = tagRepository.findByIgnoreCaseName(EVENT_TAGS[2]).orElseThrow(TagNotFoundException::new);

                assertTrue(savedEvent.getTags().contains(firstTag));
                assertTrue(savedEvent.getTags().contains(secondTag));
                assertTrue(savedEvent.getTags().contains(thirdTag));
                assertTrue(firstTag.getEvents().contains(savedEvent));
                assertTrue(secondTag.getEvents().contains(savedEvent));
                assertTrue(thirdTag.getEvents().contains(savedEvent));

            }


            @Test
            @DisplayName("When creating event should add it to user events and make him event owner.")
            public void whenCreatingEventShouldAddItToUserEventsAndMakeHimEventOwner() throws Exception {
                EventDto eventDto = eventService.createEvent(eventCreateDto, firstUserJwt);

                Event savedEvent = eventRepository.findById(eventDto.getId()).orElseThrow(EventNotFoundException::new);
                User eventOwner = userRepository.findByEmail(FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);

                assertTrue(eventOwner.getUserEvents().contains(savedEvent));
                assertEquals(eventOwner, savedEvent.getOwner());
            }

            @Test
            @DisplayName("When creating event should return EventDto with correct data.")
            public void whenCreatingEventShouldReturnEventDtoWithCorrectData() throws Exception {
                User eventOwner = userRepository.findByEmail(FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);
                EventDto eventDto = eventService.createEvent(eventCreateDto, firstUserJwt);

                assertAll(
                        () -> assertEquals(1, eventOwner.getUserEvents().size()),
                        () -> assertEquals(eventOwner.getUserEvents().get(0).getId(), eventDto.getId()),
                        () -> assertEquals(EVENT_NAME, eventDto.getName()),
                        () -> assertEquals(EVENT_SHORT_DESCRIPTION, eventDto.getShortDescription()),
                        () -> assertEquals(EVENT_LONG_DESCRIPTION, eventDto.getLongDescription()),
                        () -> assertTrue(eventDto.getEventStartDate().isEqual(EVENT_START_DATE.withSecond(0).withNano(0))),
                        () -> assertEquals(EVENT_CITY, eventDto.getCity()),
                        () -> assertEquals(EVENT_EXACT_ADDRESS, eventDto.getExactAddress()),
                        () -> assertEquals(eventOwner.getId(), eventDto.getOwner().getId()),
                        () -> assertEquals(eventOwner.getFirstName(), eventDto.getOwner().getFirstName()),
                        () -> assertEquals(eventOwner.getLastName(), eventDto.getOwner().getLastName()),
                        () -> assertEquals(eventOwner.getHomeCity().getName(), eventDto.getOwner().getHomeCity()),
                        () -> eventCreateDto.getTags().forEach(tag -> assertTrue(eventDto.getTags().contains(tag.toLowerCase()))));
            }
        }

        @Nested
        @Transactional
        @DisplayName("Update event tests:")
        class UpdateEventTests
        {
            private UUID savedEventId;

            private final String EVENT_NAME_UPDATE = "FIRST EVENT UPDATED";
            private final String EVENT_SHORT_DESCRIPTION_UPDATE = "UPDATE FIRST event short description";
            private final String EVENT_LONG_DESCRIPTION_UPDATE = "UPDATE FIRST event long description. It have to contain at least 250 characters so you have to be a little more descriptive about it. " +
                    "Don't get mad, it have to be like this to prevent some abusive users from creating them for no reason. FIRST event long description. It have to contain at least 250 characters so you have to be a little more descriptive about it" +
                    "Don't get mad, it have to be like this to prevent some abusive users from creating them for no reason.";
            private final ZonedDateTime EVENT_START_DATE_UPDATE = ZonedDateTime.now().plusDays(10).withSecond(0).withNano(0);
            private final String EVENT_CITY_UPDATE = "Krakow";
            private final String EVENT_EXACT_ADDRESS_UPDATE = "Ul. Kosciuszki 2";
            private final String[] EVENT_TAGS_UPDATE = {"ai", "java"};


            @BeforeEach
            void setUp() {
                eventUpdateDto = EventCreateDto.builder()
                        .name(EVENT_NAME_UPDATE)
                        .shortDescription(EVENT_SHORT_DESCRIPTION_UPDATE)
                        .longDescription(EVENT_LONG_DESCRIPTION_UPDATE)
                        .eventStartDate(EVENT_START_DATE_UPDATE)
                        .city(EVENT_CITY_UPDATE)
                        .exactAddress(EVENT_EXACT_ADDRESS_UPDATE)
                        .tags(Arrays.stream(EVENT_TAGS_UPDATE).toList())
                        .build();

                savedEventId = eventService.createEvent(eventCreateDto, firstUserJwt).getId();
            }
            @Test
            @DisplayName("When updating event should throw EventAlreadyHadPlaceException if event had place.")
            public void whenUpdatingEventShouldThrowEventAlreadyHadPlaceExceptionIfEventHadPlace() throws Exception {
                Event eventToUpdate = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new);
                eventToUpdate.setEventStartDate(ZonedDateTime.now().minusDays(10).withSecond(0).withNano(0));
                eventRepository.save(eventToUpdate);

                assertThrows(EventAlreadyHadPlaceException.class , () -> eventService.updateEvent(eventUpdateDto, savedEventId, firstUserJwt), "Should throw EventAlreadyHadPlaceException if event had place." );
            }

            @Test
            @DisplayName("When updating event should throw NotEventOwnerException if user tries to change not their event.")
            public void whenUpdatingEventShouldThrowNotEventOwnerExceptionIfUserTriesToChangeNotTheirEvent() throws Exception {

                assertThrows(NotEventOwnerException.class , () -> eventService.updateEvent(eventUpdateDto, savedEventId, secondUserJwt), "Should throw NotEventOwnerException if user tries to change not their event.");
            }


            @Test
            @DisplayName("When updating event should update it with correct data.")
            public void whenUpdatingEventShouldUpdateItWithCorrectData() throws Exception {
                ZonedDateTime LastChangeBeforeUpdate = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new).getLastUpdate();
                EventDto eventDto = eventService.updateEvent(eventUpdateDto, savedEventId, firstUserJwt);
                Event savedEvent = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new);

                assertAll(
                        () -> assertEquals(EVENT_NAME_UPDATE, savedEvent.getName(), "Should update event name."),
                        () -> assertEquals(EVENT_SHORT_DESCRIPTION_UPDATE, savedEvent.getShortDescription(), "Should update event short description."),
                        () -> assertEquals(EVENT_LONG_DESCRIPTION_UPDATE, savedEvent.getLongDescription(), "Should update event long description."),
                        () -> assertTrue(savedEvent.getEventStartDate().isEqual(EVENT_START_DATE_UPDATE.withSecond(0).withNano(0)),"Should update event start date."),
                        () -> assertEquals(eventUpdateDto.getEventStartDate().getZone(), ZoneId.of(savedEvent.getTimeZoneId()), "Should update event timezone id."),
                        () -> assertEquals(EVENT_EXACT_ADDRESS_UPDATE, savedEvent.getExactAddress(), "Should update event exact address."),
                        () -> assertTrue(LastChangeBeforeUpdate.isBefore(savedEvent.getLastUpdate()), "Last update field should be changed.")
                );
            }

            @Test
            @DisplayName("When updating event should update dates with zeroed seconds and nanos")
            public void whenUpdatingEventShouldUpdateDatesWithZeroedSecondsAndNanos() throws Exception {
                ZonedDateTime dateWithSeconds = EVENT_START_DATE_UPDATE.withSecond(30).withNano(500);
                eventUpdateDto.setEventStartDate(dateWithSeconds);
                eventService.updateEvent(eventUpdateDto, savedEventId, firstUserJwt);
                Event savedEvent = eventRepository.findById(savedEventId)
                        .orElseThrow(EventNotFoundException::new);

                assertEquals(0, savedEvent.getEventStartDate().getSecond(),"Seconds should be zeroed in event start date.");
                assertEquals(0, savedEvent.getEventStartDate().getNano(), "Nanos should be zeroed in event start date.");
            }


            @Test
            @DisplayName("When updating event should update city and setup relationships")
            public void whenUpdatingEventShouldUpdateCityAndSetupRelationships() throws Exception {
                EventDto eventDto = eventService.updateEvent(eventUpdateDto, savedEventId, firstUserJwt);

                Event savedEvent = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new);
                City oldCity = cityRepository.findByIgnoreCaseName(EVENT_CITY).orElseThrow(CityNotFoundException::new);
                City newCity = cityRepository.findByIgnoreCaseName(EVENT_CITY_UPDATE).orElseThrow(CityNotFoundException::new);

                assertAll(
                        () -> assertFalse(oldCity.getEvents().contains(savedEvent),"Old city should not contain event."),
                        () -> assertTrue(newCity.getEvents().contains(savedEvent), "New city should contain event."),
                        () -> assertNotEquals(oldCity, savedEvent.getCity(), "City in event should not be set to old city."),
                        () -> assertEquals(newCity, savedEvent.getCity(), "City in event should be set to new city.")
                );
            }

            @Test
            @DisplayName("When updating event should remove old tags from event and add new ones.")
            public void whenUpdatingEventShouldRemoveOldTagsFromEventAndAddNewOnes() throws Exception {
                final String firstNewTagName = "web-dev";
                final String secondNewTagName = "ai";
                eventUpdateDto.setTags(Arrays.asList(firstNewTagName, secondNewTagName));

                EventDto eventDto = eventService.updateEvent(eventUpdateDto, savedEventId, firstUserJwt);

                Event savedEvent = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new);

                Tag firstOldTag = tagRepository.findByIgnoreCaseName(EVENT_TAGS[0]).orElseThrow(TagNotFoundException::new);
                Tag secondOldTag = tagRepository.findByIgnoreCaseName(EVENT_TAGS[1]).orElseThrow(TagNotFoundException::new);
                Tag thirdOldTag = tagRepository.findByIgnoreCaseName(EVENT_TAGS[2]).orElseThrow(TagNotFoundException::new);

                Tag firstNewTag = tagRepository.findByIgnoreCaseName(firstNewTagName).orElseThrow(TagNotFoundException::new);
                Tag secondNewTag = tagRepository.findByIgnoreCaseName(secondNewTagName).orElseThrow(TagNotFoundException::new);

                assertAll("Verifying tags in event: ",
                        () -> assertFalse(savedEvent.getTags().contains(firstOldTag), "Event shouldn't contain old tag 1"),
                        () -> assertFalse(savedEvent.getTags().contains(secondOldTag), "Event shouldn't contain old tag 2"),
                        () -> assertFalse(savedEvent.getTags().contains(thirdOldTag), "Event shouldn't contain old tag 3"),
                        () -> assertTrue(savedEvent.getTags().contains(firstNewTag), "Event should contain new tag 1"),
                        () -> assertTrue(savedEvent.getTags().contains(secondNewTag), "Event should contain new tag 2")
                );

                assertAll("Verifying event in tags: ",
                        () -> assertFalse(firstOldTag.getEvents().contains(savedEvent), "Old tag 1 shouldn't contain event"),
                        () -> assertFalse(secondOldTag.getEvents().contains(savedEvent), "Old tag 2 shouldn't contain event"),
                        () -> assertFalse(thirdOldTag.getEvents().contains(savedEvent), "Old tag 3 shouldn't contain event"),
                        () -> assertTrue(firstNewTag.getEvents().contains(savedEvent), "New tag 1 should contain event"),
                        () -> assertTrue(secondNewTag.getEvents().contains(savedEvent), "New tag 2 should contain event")
                );
            }

            @Test
            @DisplayName("When updating event should retain old tag if not removed and add new one if added.")

            public void whenUpdatingEventShouldRetainOldTagIfNotRemovedAndAddNewOneIfAdded() throws Exception {
                final String firstNewTagName = "web-dev";
                List<String> tagNames = new ArrayList<>();
                tagNames.addAll(Arrays.asList(EVENT_TAGS));
                tagNames.add(firstNewTagName);
                eventUpdateDto.setTags(tagNames);


                EventDto eventDto = eventService.updateEvent(eventUpdateDto, savedEventId, firstUserJwt);

                Event savedEvent = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new);

                Tag firstOldTag = tagRepository.findByIgnoreCaseName(EVENT_TAGS[0]).orElseThrow(TagNotFoundException::new);
                Tag secondOldTag = tagRepository.findByIgnoreCaseName(EVENT_TAGS[1]).orElseThrow(TagNotFoundException::new);
                Tag thirdOldTag = tagRepository.findByIgnoreCaseName(EVENT_TAGS[2]).orElseThrow(TagNotFoundException::new);
                Tag newTag = tagRepository.findByIgnoreCaseName(firstNewTagName).orElseThrow(TagNotFoundException::new);

                assertEquals(eventUpdateDto.getTags().size(), savedEvent.getTags().size(), "Amount of tags should be the same in update dto and saved event.");

                assertAll("Event containing tag verification: ",
                        () -> assertTrue(savedEvent.getTags().contains(firstOldTag), "Event should contain first old tag."),
                        () -> assertTrue(savedEvent.getTags().contains(secondOldTag), "Event should contain second old tag."),
                        () -> assertTrue(savedEvent.getTags().contains(thirdOldTag), "Event should contain third old tag."),
                        () -> assertTrue(savedEvent.getTags().contains(newTag), "Event should contain new tag.")
                );

                assertAll("Tag containing event verification: ",
                        ()-> assertTrue(firstOldTag.getEvents().contains(savedEvent), "First old tag should still contain event."),
                        ()-> assertTrue(secondOldTag.getEvents().contains(savedEvent), "Second old tag should still contain event."),
                        ()-> assertTrue(thirdOldTag.getEvents().contains(savedEvent), "Third old tag should still contain event."),
                        ()-> assertTrue(newTag.getEvents().contains(savedEvent), "New tag should contain event.")
                );
            }

        }
    }

    @Nested
    @DisplayName("Event thread tests: ")
    class EventThreadTests {
        private UUID savedEventId;
        private UUID savedThreadId;

        @BeforeEach
        void setUp() {
            eventCreateDto = EventCreateDto.builder()
                    .name(EVENT_NAME)
                    .shortDescription(EVENT_SHORT_DESCRIPTION)
                    .longDescription(EVENT_LONG_DESCRIPTION)
                    .eventStartDate(EVENT_START_DATE)
                    .city(EVENT_CITY)
                    .exactAddress(EVENT_EXACT_ADDRESS)
                    .tags(Arrays.stream(EVENT_TAGS).toList())
                    .build();

            threadCreateDto = ThreadCreateDto.builder()
                    .name(THREAD_NAME)
                    .content(THREAD_CONTENT)
                    .build();
        }

        @Nested
        @Transactional
        @DisplayName("Event thread create tests: ")
        class EventThreadCreateTests {
            @BeforeEach
            void setUp() {
                savedEventId = eventService.createEvent(eventCreateDto, firstUserJwt).getId();
            }

            @Test
            @DisplayName("When creating thread in event should throw EventNotFoundException if event with given id does not exist")
            public void whenCreatingThreadInEventShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
                assertThrows(EventNotFoundException.class, () -> eventService.createThreadInEvent(threadCreateDto, wrongEventId, secondUserJwt), "Should throw EventNotFoundException if event with given id does not exist.");
            }

            @Test
            @DisplayName("When creating thread in event should throw NotEventAttenderException if user is not attending event")
            public void whenCreatingThreadInEventShouldThrowNotEventAttenderExceptionIfUserIsNotAttendingEvent() throws Exception {
                assertThrows(NotEventAttenderException.class, () -> eventService.createThreadInEvent(threadCreateDto, savedEventId, secondUserJwt), "Should throw NotEventAttenderException if user is not attending event.");
            }


            @Test
            @DisplayName("When creating thread in event should save it with correct data")
            public void whenCreatingThreadInEventShouldSaveItWithCorrectData() throws Exception {
                savedThreadId = eventService.createThreadInEvent(threadCreateDto, savedEventId, firstUserJwt).getId();

                Thread savedThread = threadRepository.findById(savedThreadId).orElseThrow(ThreadNotFoundException::new);

                assertAll("Saved thread data verification: ",
                        () -> assertEquals(threadCreateDto.getName(), savedThread.getName(), "Saved thread name should be the same as in thread create dto."),
                        () -> assertEquals(threadCreateDto.getContent(), savedThread.getContent(), "Saved thread content should be the same as in thread create dto."),
                        () -> assertEquals(savedThread.getCreateDate(), savedThread.getLastUpdate(), "Saved thread create date should be the same as in last update."),
                        () -> assertEquals(0, savedThread.getEditCounter(), "Edit counter should be zeroed.")
                );
            }

            @Test
            @DisplayName("When creating thread in event should save all relationships in database")
            public void whenCreatingThreadInEventShouldSaveAllRelationshipsInDatabase() throws Exception {

                savedThreadId = eventService.createThreadInEvent(threadCreateDto, savedEventId, firstUserJwt).getId();
                User threadOwner = userRepository.findByEmail(FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);
                Event event = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new);
                Thread savedThread = threadRepository.findById(savedThreadId).orElseThrow(ThreadNotFoundException::new);

                assertAll("Thread relationships verification",
                        () -> assertTrue(threadOwner.getThreads().contains(savedThread), "Thread owner should contain saved thread."),
                        () -> assertEquals(threadOwner, savedThread.getOwner(), "Thread owner field should be set to correct user."),
                        () -> assertEquals(event, savedThread.getEvent(), "Event should be the same as in event field."),
                        () -> assertTrue(event.containsThread(savedThread), "Event should contain saved thread.")
                );
            }


        }

        @Nested
        @Transactional
        @DisplayName("Event thread update tests: ")
        class EventThreadUpdateTests {
            private UUID savedEventId;
            private UUID savedThreadId;

            @BeforeEach
            void setUp() {
                threadUpdateDto = ThreadCreateDto.builder()
                        .name("update thread name")
                        .content("updated thread content, have to be different from original one.")
                        .build();

                savedEventId = eventService.createEvent(eventCreateDto, firstUserJwt).getId();
                savedThreadId = eventService.createThreadInEvent(threadCreateDto, savedEventId, firstUserJwt).getId();
            }

            @Test
            @DisplayName("When updating thread in event should throw EventNotFoundException if event with given id does not exist")
            public void whenUpdatingThreadInEventShouldThrowEventNotFoundIfEventWithGivenIdDoesNotExist() {
                assertThrows(EventNotFoundException.class, () -> eventService.updateThreadInEvent(threadUpdateDto, wrongEventId, savedThreadId, firstUserJwt), "Should throw EventNotFoundException if event with given id does not exist.");
            }

            @Test
            @DisplayName("When updating thread in event should throw ThreadNotFoundInEventException if thread does not exist")
            public void whenUpdatingThreadInEventShouldThrowThreadNotFoundInEventExceptionIfThreadDoesNotExist() throws Exception {
                assertThrows(ThreadNotFoundInEventException.class, () -> eventService.updateThreadInEvent(threadUpdateDto, savedEventId, wrongThreadId, firstUserJwt), "Should throw EventNotFoundException if event with given id does not exist.");
            }

            @Test
            @DisplayName("When updating thread in event should throw ThreadNotFoundInEventException if thread exist but is not related with event with given id")
            public void whenUpdatingThreadInEventShouldThrowThreadNotFoundInEventExceptionIfThreadExistButIsNotRelatedWithEventWithGivenId() throws Exception {
                UUID secondEventId = eventService.createEvent(eventCreateDto, firstUserJwt).getId();

                assertThrows(ThreadNotFoundInEventException.class, () -> eventService.updateThreadInEvent(threadUpdateDto, secondEventId, savedThreadId, firstUserJwt), "Should throw EventNotFoundException if event with given id does not exist.");
            }

            @Test
            @DisplayName("When updating thread in event should throw NotEventAttenderException if user is not attending event anymore")
            public void whenUpdatingThreadInEventShouldThrowNotEventAttenderExceptionIfUserIsNotAttendingEvent() throws Exception {
                eventService.addAttenderToEvent(savedEventId, secondUserJwt);
                UUID secondEventThreadId = eventService.createThreadInEvent(threadCreateDto, savedEventId, secondUserJwt).getId();
                eventService.removeAttenderFromEvent(savedEventId, secondUserJwt);

                assertThrows(NotEventAttenderException.class, () -> eventService.updateThreadInEvent(threadUpdateDto, savedEventId, savedThreadId, secondUserJwt), "Should throw NotEventAttenderException if user is not attending event.");

                Thread savedThread = threadRepository.findById(secondEventThreadId).orElseThrow(ThreadNotFoundException::new);

                assertEquals(userRepository.findByEmail(SECOND_USER_EMAIL).orElseThrow(UserNotFoundException::new), savedThread.getOwner(), "Thread owner should not change.");
            }

            @Test
            @DisplayName("When updating thread in event should save updated data")
            public void whenUpdatingThreadInEventShouldSaveUpdatedData() throws Exception {
                Thread beforeUpdateThread = threadRepository.findById(savedThreadId).orElseThrow(ThreadNotFoundException::new);
                ZonedDateTime beforeUpdateThreadLastUpdate = beforeUpdateThread.getLastUpdate();
                ZonedDateTime beforeUpdateThreadCreateDate = beforeUpdateThread.getCreateDate();

                eventService.updateThreadInEvent(threadUpdateDto, savedEventId, savedThreadId, firstUserJwt);
                Thread updatedThread = threadRepository.findById(savedThreadId).orElseThrow(ThreadNotFoundException::new);

                assertAll("Thread data verification: ",
                        () -> assertEquals(threadUpdateDto.getName(), updatedThread.getName(), "Saved thread name should be the same as in thread update dto."),
                        () -> assertEquals(threadUpdateDto.getContent(), updatedThread.getContent(), "Saved thread content should be the same as in thread update dto."),
                        () -> assertEquals(1, updatedThread.getEditCounter(), "Edit counter should be incremented by one."),
                        () -> assertTrue(beforeUpdateThreadCreateDate.isEqual(updatedThread.getCreateDate()), "Create date should not change"),
                        () -> assertTrue(updatedThread.getLastUpdate().isAfter(beforeUpdateThreadLastUpdate), "Last update date should be after old last update date.")
                );
            }


        }

        @Nested
        @Transactional
        @DisplayName("Event thread reply create tests: ")
        class EventThreadReplyCreateTests {

            private UUID savedEventId;
            private UUID savedThreadId;

            @BeforeEach
            void setUp() {
                savedEventId = eventService.createEvent(eventCreateDto, firstUserJwt).getId();
                savedThreadId = eventService.createThreadInEvent(threadCreateDto, savedEventId, firstUserJwt).getId();

                threadReplyCreateDto = new ThreadReplyCreateDto(THREAD_REPLY_CONTENT);
            }

            @Test
            @DisplayName("When creating thread reply in event thread should throw EventNotFoundException if event with given id does not exist")
            public void whenCreatingThreadReplyInEventThreadShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
                assertThrows(EventNotFoundException.class, () -> eventService.createReplyInThread(threadReplyCreateDto, wrongEventId, savedThreadId, firstUserJwt), "Should throw EventNotFoundException if event with given id does not exist.");
            }

            @Test
            @DisplayName("When creating thread reply in event thread should throw NotEventAttenderException if user is not attending event with given id")
            public void whenCreatingThreadReplyInEventThreadShouldThrowNotEventAttenderExceptionIfUserIsNotAttendingEventWithGivenId() {
                assertThrows(NotEventAttenderException.class, () -> eventService.createReplyInThread(threadReplyCreateDto, savedEventId, savedThreadId, secondUserJwt));
            }

            @Test
            @DisplayName("When creating thread reply in event thread should throw ThreadNotFoundInEventException if thread with given id does not exist")
            public void whenCreatingThreadReplyInEventThreadShouldThrowThreadNotFoundInEvenExceptionIfThreadWithGivenIdDoesNotExist() {
                assertThrows(ThreadNotFoundInEventException.class, () -> eventService.createReplyInThread(threadReplyCreateDto, savedEventId, wrongThreadId, firstUserJwt));
            }

            @Test
            @DisplayName("When creating thread reply in event thread should throw ThreadNotFoundInEventException if thread with given id exists but is not related to event with given id")
            public void whenCreatingThreadReplyInEventThreadShouldThrowThreadNotFoundInEvenExceptionIfThreadWithGivenIdExistsButIsNotRelatedToEventWithGivenId() {
                UUID secondSavedEventId = eventService.createEvent(eventCreateDto, firstUserJwt).getId();
                UUID secondSavedThreadId = eventService.createThreadInEvent(threadCreateDto, secondSavedEventId, firstUserJwt).getId();

                assertThrows(ThreadNotFoundInEventException.class, () -> eventService.createReplyInThread(threadReplyCreateDto, savedEventId, secondSavedThreadId, firstUserJwt), "Thread with given id and event with given id are not related, should not allow for creating thread reply");
                assertThrows(ThreadNotFoundInEventException.class, () -> eventService.createReplyInThread(threadReplyCreateDto, secondSavedEventId, savedThreadId, firstUserJwt), "Thread with given id and event with given id are not related, should not allow for creating thread reply");
            }

            @Test
            @DisplayName("When creating thread reply in event thread should save it with correct data and relationships in database")
            public void whenCreatingThreadReplyInEventThreadShouldSaveItWithCorrectDataAndRelationshipsInDatabase() {
                UUID savedThreadReplyId = eventService.createReplyInThread(threadReplyCreateDto, savedEventId, savedThreadId, firstUserJwt).getId();

                User threadReplyOwner = userRepository.findByEmail(FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);
                Thread thread = threadRepository.findById(savedThreadId).orElseThrow(ThreadNotFoundException::new);
                ThreadReply savedThreadReply = threadReplyRepository.findById(savedThreadReplyId).orElseThrow(ThreadReplyNotFoundException::new);

                assertAll("Saved thread reply data verification: ",
                        () -> assertEquals(threadReplyCreateDto.getReplyContent(), savedThreadReply.getContent(), "Content of the thread reply have to be the same as in dto"),
                        () -> assertEquals(thread, savedThreadReply.getThread(), "Thread in thread reply have to be set to the one with given id"),
                        () -> assertTrue(thread.containsReply(savedThreadReply), "Thread have to contain new thread reply"),
                        () -> assertEquals(threadReplyOwner, savedThreadReply.getReplier(), "Creator of thread reply have to be set to the one making request"),
                        () -> assertTrue(threadReplyOwner.getThreadReplies().contains(savedThreadReply), "User have to have new reply in his replies"),
                        () -> assertTrue(savedThreadReply.getReplyDate().isEqual(savedThreadReply.getLastUpdate()), "Thread reply create/reply date time have to be the same as last update date time")
                );

            }
        }

        @Nested
        @Transactional
        @DisplayName("Event thread reply update tests: ")
        class EventThreadReplyUpdateTests {

            private UUID savedEventId;
            private UUID savedThreadId;
            private UUID savedThreadReplyId;

            @BeforeEach
            void setUp() {
                savedEventId = eventService.createEvent(eventCreateDto, firstUserJwt).getId();
                savedThreadId = eventService.createThreadInEvent(threadCreateDto, savedEventId, firstUserJwt).getId();
                savedThreadReplyId = eventService.createReplyInThread(new ThreadReplyCreateDto(THREAD_REPLY_CONTENT), savedEventId, savedThreadId, firstUserJwt).getId();

                threadReplyCreateDto = new ThreadReplyCreateDto(THREAD_REPLY_CONTENT);
                threadReplyUpdateDto = new ThreadReplyCreateDto(THREAD_REPLY_CONTENT_UPDATE);
            }

            @Test
            @DisplayName("When updating thread reply should throw EventNotFoundException if event with given id does not exist.")
            public void whenUpdatingThreadReplyShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist(){
                assertThrows(EventNotFoundException.class,
                        () -> eventService.updateThreadReplyInEventThread(threadReplyUpdateDto, wrongEventId, savedThreadId, savedThreadReplyId, firstUserJwt),
                        "");
            }
            @Test
            @DisplayName("When updating thread reply should throw NotEventAttenderException if user is not attending event anymore.")
            public void whenUpdatingThreadReplyShouldThrowNotEventAttenderExceptionIfUserIsNotAttendingEventAnymore(){
                eventService.addAttenderToEvent(savedEventId, secondUserJwt);
                assertTrue(eventRepository.findById(savedEventId).get().isUserAttending(userRepository.findByEmail(SECOND_USER_EMAIL).get()),
                        "User have to be attending event while creating reply");

                UUID secondThreadReplyId = eventService.createReplyInThread(threadReplyCreateDto, savedEventId, savedThreadId, secondUserJwt).getId();
                eventService.removeAttenderFromEvent(savedEventId, secondUserJwt);

                assertFalse(eventRepository.findById(savedEventId).get().isUserAttending(userRepository.findByEmail(SECOND_USER_EMAIL).get()),
                        "User can not be attending this event after creating reply in thread to make test viable");

                assertThrows(NotEventAttenderException.class,
                        () ->  eventService.updateThreadReplyInEventThread(threadReplyUpdateDto, savedEventId, savedThreadId, secondThreadReplyId, secondUserJwt),
                        "If user is not attending event anymore should throw NotEventAttenderException ");
            }

            @Test
            @DisplayName("When updating thread reply should throw ThreadNotFoundInEventException if thread with given id does not exist.")
            public void whenUpdatingThreadReplyShouldThrowThreadNotFoundInEventExceptionIfThreadWithGivenIdDoesNotExist(){
                assertThrows(ThreadNotFoundInEventException.class,
                        () -> eventService.updateThreadReplyInEventThread(threadReplyUpdateDto, savedEventId, wrongThreadId, savedThreadReplyId,firstUserJwt),
                        "" );
            }
            @Test
            @DisplayName("When updating thread reply should throw ThreadNotFoundInEventException if thread with given id is not related with event with given id.")
            public void whenUpdatingThreadReplyShouldThrowThreadNotFoundInEventExceptionIfThreadWithGivenIdIsNotRelatedWithEventWithGivenId(){
                UUID secondSavedEventId = eventService.createEvent(eventCreateDto, firstUserJwt).getId();

                assertThrows(ThreadNotFoundInEventException.class,
                        () -> eventService.updateThreadReplyInEventThread(threadReplyUpdateDto, secondSavedEventId, savedThreadId, savedThreadReplyId,firstUserJwt),
                        "When updating event thread reply should throw ThreadNotFoundInEventException if event and thread are not related.");
            }

            @Test
            @DisplayName("When updating thread reply should throw ReplyNotFoundInThreadException if thread reply with given id does not exist.")
            public void whenUpdatingThreadReplyShouldThrowReplyNotFoundInThreadException(){
                assertThrows(ReplyNotFoundInThreadException.class,
                        () -> eventService.updateThreadReplyInEventThread(threadReplyUpdateDto, savedEventId, savedThreadId, wrongThreadReplyId,firstUserJwt),
                        "Did not throw ReplyNotFoundInThreadException. ");
            }

            @Test
            @DisplayName("When updating thread reply should throw ReplyNotFoundInThread if thread reply with given id is not related with thread with given id.")
            public void whenUpdatingThreadReplyShouldThrowThreadNotFoundInEventExceptionIfThreadWithGivenIdIsNotRelatedWithEventWithGivenI1d(){
                UUID secondSavedThreadId = eventService.createThreadInEvent(threadCreateDto, savedEventId, firstUserJwt).getId();
                UUID secondThreadReplyId  = eventService.createReplyInThread(threadReplyCreateDto, savedEventId, secondSavedThreadId, firstUserJwt).getId();

                assertThrows(ReplyNotFoundInThreadException.class,
                        () -> eventService.updateThreadReplyInEventThread(threadReplyUpdateDto, savedEventId, secondSavedThreadId, savedThreadReplyId,firstUserJwt),
                        "Thread and thread reply are not related, should not allow to update thread reply");
                assertThrows(ReplyNotFoundInThreadException.class,
                        () -> eventService.updateThreadReplyInEventThread(threadReplyUpdateDto, savedEventId, savedThreadId, secondThreadReplyId, firstUserJwt),
                        "Thread and thread reply are not related, should not allow to update thread reply.");
            }
            @Test
            @DisplayName("When updating thread reply should throw NotThreadReplyOwnerException if user tries to modify not his reply. ")
            public void whenUpdatingThreadReplyShouldThrowNotThreadReplyOwnerExceptionIfUserTriesToModifyNotHisReply(){
                eventService.addAttenderToEvent(savedEventId, secondUserJwt);

                assertThrows(NotThreadReplyOwnerException.class,
                        () -> eventService.updateThreadReplyInEventThread(threadReplyUpdateDto, savedEventId, savedThreadId, savedThreadReplyId, secondUserJwt),
                        "Expected NotThreadReplyOwnerException when non-owner tries to modify thread reply." );

            }

            @Test
            @DisplayName("When updating thread reply should update thread reply content.")
            public void whenUpdatingThreadReplyShouldUpdateThreadReplyContent(){
                eventService.updateThreadReplyInEventThread(threadReplyUpdateDto, savedEventId, savedThreadId, savedThreadReplyId, firstUserJwt);

                ThreadReply updatedThreadReply  = threadReplyRepository.findById(savedThreadReplyId).orElseThrow(ThreadReplyNotFoundException::new);

                assertEquals(threadReplyUpdateDto.getReplyContent(), updatedThreadReply.getContent(), "Expected thread reply content to be set as received from user.");
            }
            @Test
            @DisplayName("When updating thread reply should increment edit counter.")
            public void whenUpdatingThreadReplyShouldIncrementEditCounter(){
                ThreadReply oldThreadReply = threadReplyRepository.findById(savedThreadReplyId).orElseThrow(ThreadReplyNotFoundException::new);

                int oldEditCounter = oldThreadReply.getEditCounter();

                eventService.updateThreadReplyInEventThread(threadReplyUpdateDto, savedEventId, savedThreadId, savedThreadReplyId, firstUserJwt);

                ThreadReply updatedThreadReply  = threadReplyRepository.findById(savedThreadReplyId).orElseThrow(ThreadReplyNotFoundException::new);

                assertNotEquals(oldEditCounter, updatedThreadReply.getEditCounter(), "Expected edit counter after update to not be equal to edit counter before update.");
                assertTrue(updatedThreadReply.getEditCounter() > oldEditCounter, "Expected to updated thread reply edit counter be higher than before update.");
                assertEquals(1,updatedThreadReply.getEditCounter() - oldEditCounter, "Expected difference in edit counter between old and updated thread reply to be exactly 1");
            }
            @Test
            @DisplayName("When updating thread reply should save updated content in database.")
            public void whenUpdatingThreadReplyShouldUpdateLastUpdateDate(){

                ZonedDateTime oldLastUpdate = threadReplyRepository.findById(savedThreadReplyId).map(ThreadReply::getLastUpdate).orElseThrow(ThreadReplyNotFoundException::new);

                eventService.updateThreadReplyInEventThread(threadReplyUpdateDto, savedEventId, savedThreadId, savedThreadReplyId, firstUserJwt);

                ThreadReply updatedThreadReply  = threadReplyRepository.findById(savedThreadReplyId).orElseThrow(ThreadReplyNotFoundException::new);

                assertNotEquals(oldLastUpdate, updatedThreadReply.getLastUpdate(), "Expected lastUpdate field to not be the same before and after update.");
                assertTrue(oldLastUpdate.isBefore(updatedThreadReply.getLastUpdate()), "Expected old lastUpdate value to be before the one after update.");
            }

        }

    }
}
