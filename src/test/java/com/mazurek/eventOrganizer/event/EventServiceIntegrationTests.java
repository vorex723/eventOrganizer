package com.mazurek.eventOrganizer.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mazurek.eventOrganizer.auth.AuthenticationRequest;
import com.mazurek.eventOrganizer.auth.AuthenticationServiceImpl;
import com.mazurek.eventOrganizer.auth.ActivationTokenRepository;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.file.*;
import com.mazurek.eventOrganizer.jwt.JwtUtils;
import com.mazurek.eventOrganizer.tag.TagRepository;
import com.mazurek.eventOrganizer.thread.ThreadReplyRepository;
import com.mazurek.eventOrganizer.thread.ThreadRepository;
import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadReplyCreateDto;
import com.mazurek.eventOrganizer.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.ZonedDateTime;
import java.util.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class EventServiceIntegrationTests {


    private final String THREAD_REPLY_CONTENT = "this is reply in thread, let's see how it works";
    private final String THREAD_REPLY_CONTENT_UPDATE = "this is updated content for thread reply" ;
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

    private final String FILE_NAME_USER = "File name";


    private final UUID NOT_EXISTING_EVENT_ID = UUID.randomUUID();
    private final UUID NOT_EXISTING_EVENT_THREAD_ID = UUID.randomUUID();
    private final UUID NOT_EXISTING_EVENT_THREAD_REPLY_ID = UUID.randomUUID();

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
    private ActivationTokenRepository activationTokenRepository;
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
    private FileRepository fileRepository;
    @Autowired
    EntityManager entityManager;
    @Autowired
    JwtUtils jwtUtils;

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
            @DisplayName("When creating event should use existing tags and setup relationship with them.")
            public void whenCreatingEventShouldUseExistingTagsAndSetupRelationshipsWithThem() throws Exception {
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
    @DisplayName("Event attending tests: ")
    class EventAttenderTests{
        private UUID savedEventId;

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

            savedEventId = eventService.createEvent(eventCreateDto, firstUserJwt).getId();
        }

        @Nested
        @DisplayName("Event add attender tests:")
        @Transactional
        class EventAddAttenderTests{

            @Test
            @DisplayName("When adding attender to event should throw EventNotFoundException if event with given id does not exist")
            public void whenAddingAttenderToEventShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist(){
                assertThrows(EventNotFoundException.class, () -> eventService.addAttenderToEvent(NOT_EXISTING_EVENT_ID, secondUserJwt), "Expected to throw EventNotFoundException.");
            }

            @Test
            @DisplayName("When adding attender to event should throw EventAlreadyHadPlaceException if event start date is in the past")
            public void whenAddingAttenderToEventShouldThrowEventAlreadyHadPlaceExceptionIfEventStartDateIsInThePast(){
                Event testEvent = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new);
                testEvent.setEventStartDate(ZonedDateTime.now().minusDays(7));
                eventRepository.save(testEvent);

                assertThrows(EventAlreadyHadPlaceException.class, () -> eventService.addAttenderToEvent(savedEventId, secondUserJwt), "Expected to throw EventAlreadyHadPlaceException.");
            }

            @Test
            @DisplayName("When adding attender to event should throw EventOwnerAlreadyAttendsEventException if event owner performs attending event action")
            public void whenAddingAttenderToEventShouldThrowEventOwnerAlreadyAttendsEventExceptionIfEventOwnerPerformsAttendingEventAction(){
                assertThrows(EventOwnerAlreadyAttendsEventException.class, () -> eventService.addAttenderToEvent(savedEventId, firstUserJwt), "Expected to throw EventOwnerAlreadyAttendsEventException.");
            }

            @Test
            @DisplayName("When adding attender to event should throw EventOwnerAlreadyAttendsEventException if event owner performs attending event action")
            public void whenAddingAttenderToEventShouldThrowUserAlreadyAttendsEventExceptionIfAlreadyAttendingUserPerformsAttendingAction(){
                Event testEvent = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new);
                testEvent.addAttendingUser(userRepository.findByEmail(SECOND_USER_EMAIL).orElseThrow(UserNotFoundException::new));
                eventRepository.save(testEvent);

                assertThrows(AlreadyAttendingEventException.class, () -> eventService.addAttenderToEvent(savedEventId, secondUserJwt), "Expected to throw UserAlreadyAttendsEventException.");
            }

            @Test
            @DisplayName("When adding attender to event should persist new relationship in database.")
            public void whenAddingAttenderToEventShouldPersistNewRelationshipInDatabase(){
                eventService.addAttenderToEvent(savedEventId,secondUserJwt);

                Event testEvent = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new);
                User newAttender = userRepository.findByEmail(SECOND_USER_EMAIL).orElseThrow(UserNotFoundException::new);
                assertTrue(testEvent.getAttendingUsers().contains(newAttender), "Expected to event attendingUsers field to contain performing user.");
                assertTrue(newAttender.getAttendingEvents().contains(testEvent), "Expected to user attendingEvents field to contain event on which was action performed.");
            }

        }

        @Nested
        @DisplayName("Event remove attender tests:")
        @Transactional
        class EventRemoveAttenderTests{
            @BeforeEach
            void setUp() {
                eventService.addAttenderToEvent(savedEventId, secondUserJwt);
            }
            @Test
            @DisplayName("When removing attender from event should throw EventNotFoundException if event with given id does not exist.")
            public void whenRemovingAttenderFromEventShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist(){
                assertThrows(EventNotFoundException.class, () -> eventService.removeAttenderFromEvent(NOT_EXISTING_EVENT_ID, secondUserJwt), "Expected to throw EventNotFoundException when event with given id does not exist.");
            }
            @Test
            @DisplayName("When removing attender from event should throw EventAlreadyHadPlaceException if event start date is in the past.")
            public void whenRemovingAttenderFromEventShouldThrowEventAlreadyHadPlaceExceptionIfEventStartDateIsInThePast(){
                ZonedDateTime pastEventStartDate = ZonedDateTime.now().minusDays(2);
                Event testEvent = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new);
                testEvent.setEventStartDate(pastEventStartDate);
                eventRepository.save(testEvent);

                assertThrows(EventAlreadyHadPlaceException.class, () -> eventService.removeAttenderFromEvent(savedEventId, secondUserJwt), "Expected to throw EventAlreadyHadPlaceException if event start date is in the past");
            }

            @Test
            @DisplayName("When removing attender from event should throw EventOwnerMustAttendEventException if event owner tries to perform \"Don\'t attend\" action")
            public void whenRemovingAttenderFromEventShouldThrowEventOwnerMustAttendEventExceptionIfEventOwnerTriesToPerformDontAttendAction(){
                assertThrows(EventOwnerMustAttendEventException.class, () -> eventService.removeAttenderFromEvent(savedEventId, firstUserJwt), "Expected to throw EventOwnerMustAttendEventException if event owner tries to perform \"Don\'t attend\" action.");
            }

            @Test
            @DisplayName("When removing attender from event should throw NotEventAttenderException if performing user don\'t attend event.")
            public void whenRemovingAttenderFromEventShouldThrowNotEventAttenderExceptionIfPerformingUserDontAttendEvent(){
                Event testEvent = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new);
                User performingUser = userRepository.findByEmail(SECOND_USER_EMAIL).orElseThrow(UserNotFoundException::new);

                testEvent.getAttendingUsers().remove(performingUser);
                performingUser.removeAttendingEvent(testEvent);
                userRepository.save(performingUser);
                eventRepository.save(testEvent);

                assertThrows(NotEventAttenderException.class, () -> eventService.removeAttenderFromEvent(savedEventId, secondUserJwt));
            }

            @Test
            @DisplayName("When removing attender from event should remove performing user from event attendingUsers field and persist this change.")
            public void whenRemovingAttenderFromEventShouldRemovePerformingUserFromEventAttendingUsersFieldAndPersistThisChange(){
                eventService.removeAttenderFromEvent(savedEventId, secondUserJwt);

                Event testEvent = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new);
                User performingUser = userRepository.findByEmail(SECOND_USER_EMAIL).orElseThrow(UserNotFoundException::new);
                assertFalse(testEvent.getAttendingUsers().contains(performingUser), "Expected to performing user not be contained in attendingUsers field");
            }

            @Test
            @DisplayName("When removing attender from event should remove threads created by performing user from user\'s threads field but leave them in event.")
            public void whenRemovingAttenderFromEventShouldRemoveThreadsCreatedByPerformingUserFromUsersThreadsFieldButLeaveThemInEvent(){
                threadCreateDto = new ThreadCreateDto(THREAD_NAME, THREAD_CONTENT);
                UUID threadToRemoveFromUserId = eventService.createThreadInEvent(threadCreateDto, savedEventId, secondUserJwt).getId();

                User performingUser = userRepository.findByEmail(SECOND_USER_EMAIL).orElseThrow(UserNotFoundException::new);
                Thread threadToRemoveFromUser = threadRepository.findById(threadToRemoveFromUserId).orElseThrow(ThreadNotFoundException::new);
                Event testEvent = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new);

                eventService.removeAttenderFromEvent(savedEventId, secondUserJwt);

                assertFalse(performingUser.getThreads().contains(threadToRemoveFromUser), "Expected to remove event threads belonging to event which user don\'t attend anymore from performing user threads.");
                assertTrue(threadToRemoveFromUser.isUserOwner(performingUser), "Expected to persist original thread owner.");
                assertEquals(testEvent,threadToRemoveFromUser.getEvent(), "Expected Thread - Event relationship to not change.");
                assertTrue(testEvent.getThreads().contains(threadToRemoveFromUser), "Expected Thread - Event relationship to not change.");
            }

            @Test
            @DisplayName("When removing attender from event should remove all thread replies from performing user thread replies field")
            public void whenRemovingAttenderFromEventShouldRemoveAllThreadRepliesFromPerformingUserThreadRepliesField(){
                threadCreateDto = new ThreadCreateDto(THREAD_NAME, THREAD_CONTENT);
                threadReplyCreateDto = new ThreadReplyCreateDto(THREAD_REPLY_CONTENT);
                UUID performerThreadId = eventService.createThreadInEvent(threadCreateDto, savedEventId, secondUserJwt).getId();
                UUID eventOwnerThreadId = eventService.createThreadInEvent(threadCreateDto, savedEventId, firstUserJwt).getId();

                Thread performerThread= threadRepository.findById(performerThreadId).orElseThrow(ThreadNotFoundException::new);
                Thread eventOwnerThread= threadRepository.findById(eventOwnerThreadId).orElseThrow(ThreadNotFoundException::new);
                User performingUser = userRepository.findByEmail(SECOND_USER_EMAIL).orElseThrow(UserNotFoundException::new);
                Event testEvent = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new);

                UUID performerThreadReplyId = eventService.createReplyInThread(threadReplyCreateDto,savedEventId,performerThreadId,secondUserJwt).getId();
                UUID eventOwnerThreadReplyId = eventService.createReplyInThread(threadReplyCreateDto,savedEventId,eventOwnerThreadId,secondUserJwt).getId();
                ThreadReply performerThreadReply = threadReplyRepository.findById(performerThreadReplyId).orElseThrow(ThreadReplyNotFoundException::new);
                ThreadReply eventOwnerThreadReply = threadReplyRepository.findById(eventOwnerThreadReplyId).orElseThrow(ThreadReplyNotFoundException::new);

                eventService.removeAttenderFromEvent(savedEventId, secondUserJwt);

                assertFalse(performingUser.getThreadReplies().contains(eventOwnerThreadReply), "Expected performing user threadReplies to not contain this event thread replies.");
                assertFalse(performingUser.getThreadReplies().contains(performerThreadReply), "Expected performing user threadReplies to not contain this event thread replies.");
                assertTrue(performerThread.getReplies().contains(performerThreadReply), "Expected thread to remain performing user replies.");
                assertTrue(eventOwnerThread.getReplies().contains(eventOwnerThreadReply), "Expected thread to remain performing user replies.");
                assertTrue(performerThreadReply.isReplier(performingUser), "Expected the replier to remain the original owner.");
                assertTrue(eventOwnerThreadReply.isReplier(performingUser), "Expected the replier to remain the original owner.");
                assertTrue(performerThread.getReplies().contains(performerThreadReply), "Expected to not remove thread replies of user which is not attending anymore.");
                assertTrue(eventOwnerThread.getReplies().contains(eventOwnerThreadReply), "Expected to not remove thread replies of user which is not attending anymore.");

            }

            @Test
            @DisplayName("When removing attender from event should remove all user files from user files field but leave them in event.")
            public void whenRemovingAttenderFromEventShouldRemoveAllUserFilesFromUserFilesFieldButLeaveThemInEvent(){
                User performingUser = userRepository.findByEmail(SECOND_USER_EMAIL).orElseThrow(UserNotFoundException::new);
                Event testEvent = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new);
                File testFile = fileRepository.save(
                        File.builder()
                                .event(testEvent)
                                .owner(performingUser)
                                .userFileName(FILE_NAME_USER)
                                .contentType(null)
                                .content(null)
                                .build());

                testEvent.addFile(testFile);
                eventRepository.save(testEvent);

                eventService.removeAttenderFromEvent(savedEventId, secondUserJwt);

                assertFalse(performingUser.getFiles().contains(testFile), "Expected the file to be removed from user files field.");
                assertTrue(testEvent.getFiles().contains(testFile), "Expected the file to be attached to event.");
                assertEquals(testEvent,testFile.getEvent(), "Expected to event filed in file to not change.");
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
                assertThrows(EventNotFoundException.class, () -> eventService.createThreadInEvent(threadCreateDto, NOT_EXISTING_EVENT_ID, secondUserJwt), "Should throw EventNotFoundException if event with given id does not exist.");
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
                assertThrows(EventNotFoundException.class, () -> eventService.updateThreadInEvent(threadUpdateDto, NOT_EXISTING_EVENT_ID, savedThreadId, firstUserJwt), "Should throw EventNotFoundException if event with given id does not exist.");
            }

            @Test
            @DisplayName("When updating thread in event should throw ThreadNotFoundInEventException if thread does not exist")
            public void whenUpdatingThreadInEventShouldThrowThreadNotFoundInEventExceptionIfThreadDoesNotExist() throws Exception {
                assertThrows(ThreadNotFoundInEventException.class, () -> eventService.updateThreadInEvent(threadUpdateDto, savedEventId, NOT_EXISTING_EVENT_THREAD_ID, firstUserJwt), "Should throw EventNotFoundException if event with given id does not exist.");
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
            @DisplayName("When updating thread in event should throw NotThreadOwnerException if performing user do not own thread with given id.")
            public void whenUpdatingThreadInEventShouldThrowNotThreadOwnerExceptionIfPerformingUserDoNotOwnThreadWithGivenId(){
                eventService.addAttenderToEvent(savedEventId, secondUserJwt);
                assertThrows(NotThreadOwnerException.class, () -> eventService.updateThreadInEvent(threadUpdateDto, savedEventId, savedThreadId, secondUserJwt), "Should throw NotThreadOwnerException if user is trying to modify others events.");

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
                assertThrows(EventNotFoundException.class, () -> eventService.createReplyInThread(threadReplyCreateDto, NOT_EXISTING_EVENT_ID, savedThreadId, firstUserJwt), "Should throw EventNotFoundException if event with given id does not exist.");
            }

            @Test
            @DisplayName("When creating thread reply in event thread should throw NotEventAttenderException if user is not attending event with given id")
            public void whenCreatingThreadReplyInEventThreadShouldThrowNotEventAttenderExceptionIfUserIsNotAttendingEventWithGivenId() {
                assertThrows(NotEventAttenderException.class, () -> eventService.createReplyInThread(threadReplyCreateDto, savedEventId, savedThreadId, secondUserJwt));
            }

            @Test
            @DisplayName("When creating thread reply in event thread should throw ThreadNotFoundInEventException if thread with given id does not exist")
            public void whenCreatingThreadReplyInEventThreadShouldThrowThreadNotFoundInEvenExceptionIfThreadWithGivenIdDoesNotExist() {
                assertThrows(ThreadNotFoundInEventException.class, () -> eventService.createReplyInThread(threadReplyCreateDto, savedEventId, NOT_EXISTING_EVENT_THREAD_ID, firstUserJwt));
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
                        () -> eventService.updateThreadReplyInEventThread(threadReplyUpdateDto, NOT_EXISTING_EVENT_ID, savedThreadId, savedThreadReplyId, firstUserJwt),
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
                        () -> eventService.updateThreadReplyInEventThread(threadReplyUpdateDto, savedEventId, NOT_EXISTING_EVENT_THREAD_ID, savedThreadReplyId,firstUserJwt),
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
                        () -> eventService.updateThreadReplyInEventThread(threadReplyUpdateDto, savedEventId, savedThreadId, NOT_EXISTING_EVENT_THREAD_REPLY_ID,firstUserJwt),
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

    @Nested
    @DisplayName("Event file tests:")
    class EventFileTests{

        private UUID savedEventId;

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

            savedEventId = eventService.createEvent(eventCreateDto, firstUserJwt).getId();
        }

        @Nested
        @Transactional
        @DisplayName("Upload file test:")
        class UploadFileTests{

            private FileUploadDto fileUploadDto;
            private MockMultipartFile mockMultipartFileJPEG = new MockMultipartFile(
                    "file",
                    "allowed.jpeg",
                    "image/jpeg",
                    TestFileContentFactory.jpeg()
            );

            @BeforeEach
            void setUp() {

                fileUploadDto = new FileUploadDto(FILE_NAME_USER, mockMultipartFileJPEG);
            }

            @Test
            @DisplayName("When uploading file should throw EventNotFoundException if event with given id does not exist.")
            public void whenUploadingFileShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() throws IOException {
                assertThrows(EventNotFoundException.class, () -> eventService.uploadFileToEvent(fileUploadDto, NOT_EXISTING_EVENT_ID, firstUserJwt), "Excepted to throw EventNotFoundException if event with given id does not exist.");
            }

            @Test
            @DisplayName("When uploading file should throw NotEventAttenderException if performing user is not attending event.")
            public void whenUploadingFileShouldThrowNotEventAttenderExceptionIfPerformingUserIsNotAttendingEvent(){
                assertThrows(NotEventAttenderException.class, () -> eventService.uploadFileToEvent(fileUploadDto, savedEventId, secondUserJwt), "Expected to throw NotEventAttenderException if user is not attending event.");
            }

            @ParameterizedTest
            @MethodSource("com.mazurek.eventOrganizer.event.EventServiceIntegrationTests$EventFileTests#disallowedFileProvider")
            @DisplayName("When uploading file should throw FileTypeNotAllowedException if file is not on whitelist.")
            public void whenUploadingFileShouldThrowFileTypeNotAllowedExceptionIfFileIsNotOnWhitelist(TestFileData testFileData){

                MockMultipartFile file = new MockMultipartFile(
                        "file",
                        "allowed" + testFileData.extension(),
                        testFileData.contentType(),
                        testFileData.bytes()
                );

                fileUploadDto = new FileUploadDto("Malware file", file);

                assertThrows(FileTypeNotAllowedException.class, () -> eventService.uploadFileToEvent(fileUploadDto, savedEventId, firstUserJwt), "Expected to throw FileTypeNotAllowedException, all files are malformed.");
            }

            @ParameterizedTest
            @MethodSource("com.mazurek.eventOrganizer.event.EventServiceIntegrationTests$EventFileTests#allowedFileProvider")
            @DisplayName("When uploading file should not throw FileTypeNotAllowedException if file is on whitelist.")
            public void whenUploadingFileShouldNotThrowFileTypeNotAllowedExceptionIfFileIsOnWhitelist(TestFileData testFileData){

                MockMultipartFile file = new MockMultipartFile(
                        "file",
                        "allowed" + testFileData.extension(),
                        testFileData.contentType(),
                        testFileData.bytes()
                );

                fileUploadDto = new FileUploadDto("Malware file", file);

                assertDoesNotThrow(() -> eventService.uploadFileToEvent(fileUploadDto, savedEventId, firstUserJwt), "Expected to not throw any exception, especially FileTypeNotAllowedException.");
            }

            @Test
            @DisplayName("When uploading file should save file-event relationship")
            public void whenUploadingFileShouldSaveFileEventRelationship() throws IOException {
                UUID savedFileId = eventService.uploadFileToEvent(fileUploadDto ,savedEventId, firstUserJwt).getId();

                Event event = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new);
                File file = fileRepository.findById(savedFileId).orElseThrow(FileNotFoundException::new);

                assertEquals(event,file.getEvent(), "Expected to file event field to be set on correct event.");
                assertTrue(event.getFiles().contains(file), "Expected to event files field to contain new file.");

            }

            @Test
            @DisplayName("When uploading file should save file-user relationship")
            public void whenUploadingFileShoulddSaveFileUserRelationship() throws IOException {
                UUID savedFileId = eventService.uploadFileToEvent(fileUploadDto ,savedEventId, firstUserJwt).getId();

                User user = userRepository.findByEmail(FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);
                File file = fileRepository.findById(savedFileId).orElseThrow(FileNotFoundException::new);

                assertEquals(user ,file.getOwner(), "Expected to file owner field to be set on correct user.");
                assertTrue(user.getFiles().contains(file), "Expected to user files field to contain new file.");

            }
        }

        @Nested
        @Transactional
        @DisplayName("Get file overview by id tests:")
        class GetFileOverviewByIdTests{
            private UUID savedFileId;
            private ZonedDateTime fileUploadDateTime;
            private final UUID notExistingFileId = UUID.randomUUID();
            private final String FILE_NAME_ORIGINAL = "image.png";


            @BeforeEach
            void setUp() {
                Event testEvent = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new);
                User fileOwner = userRepository.findByEmail(FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);

                fileUploadDateTime = ZonedDateTime.now().withSecond(0).withNano(0);
                File fileToSave = File.builder()
                        .event(testEvent)
                        .content(TestFileContentFactory.png())
                        .contentType(ContentType.IMAGE_PNG.getMimeType())
                        .originalFileName(FILE_NAME_ORIGINAL)
                        .userFileName(FILE_NAME_USER)
                        .uploadDateTime(fileUploadDateTime)
                        .owner(fileOwner)
                        .event(testEvent)
                        .build();

                File savedFile = fileRepository.save(fileToSave);

                testEvent.addFile(savedFile);
                eventRepository.save(testEvent);

                fileOwner.addFile(savedFile);
                userRepository.save(fileOwner);

                savedFileId = savedFile.getId();
            }

            @Test
            @DisplayName("When getting file overview by id should throw FileNotFoundInEventException if file with given id does not exist")
            public void whenGettingFileOverviewByIdShouldThrowFileNotFoundInEventExceptionIfFileWithGivenIdDoesNotExist(){
                assertThrows(FileNotFoundInEventException.class, () -> eventService.getFileOverviewById(notExistingFileId, savedEventId, firstUserJwt), "Expected to throw FileNotFoundInEvent");
            }

            @Test
            @DisplayName("When getting file overview by id should throw EventNotFoundException if event with given id does not exist")
            public void whenGettingFileOverviewByIdShouldThrowFileNotFoundInEventIfEventWithGivenIdDoesNotExist(){
                assertThrows(EventNotFoundException.class, () -> eventService.getFileOverviewById(savedFileId, NOT_EXISTING_EVENT_ID, firstUserJwt), "Expected to throw EventNotFoundException if event with given id does not exist.");
            }

            @Test
            @DisplayName("When getting file overview by id should throw FileNotFoundInEventException if file and event exist but are not related")
            public void whenGettingFileOverviewByIdShouldThrowFileNotFoundInEventExceptionIfFileAndEventExistButAreNotRelated(){

                eventCreateDto = EventCreateDto.builder()
                        .name(EVENT_NAME)
                        .shortDescription(EVENT_SHORT_DESCRIPTION)
                        .longDescription(EVENT_LONG_DESCRIPTION)
                        .eventStartDate(EVENT_START_DATE)
                        .city(EVENT_CITY)
                        .exactAddress(EVENT_EXACT_ADDRESS)
                        .tags(Arrays.stream(EVENT_TAGS).toList())
                        .build();

                UUID secondEventId  = eventService.createEvent(eventCreateDto, firstUserJwt).getId();

                assertThrows(FileNotFoundInEventException.class, () -> eventService.getFileOverviewById(savedFileId, secondEventId, firstUserJwt), "Expected to throw FileNotFoundInEventException if file and event exist but are not related");
            }

            @Test
            @DisplayName("When getting file overview by id should throw NotEventAttenderException if user is not attending event with given id")
            public void whenGettingFileOverviewByIdShouldThrowNotEventAttenderExceptionIfUserIsNotAttendingEventWithGivenId(){
                assertThrows(NotEventAttenderException.class, () -> eventService.getFileOverviewById(savedFileId, savedEventId, secondUserJwt), "Expected to throw NotEventAttenderException if performing user is not attending event in any form.");
            }

            @Test
            @DisplayName("When getting file overview by id should return correct data")
            public void whenGettingFileOverviewByIdShouldReturnCorrectData(){
                File fileToServe = fileRepository.findById(savedFileId).orElseThrow(FileNotFoundException::new);
                FileOverviewDto returnedFileOverviewDto = eventService.getFileOverviewById(savedFileId, savedEventId, firstUserJwt);


                assertAll("Data verification assertions: ",
                        () -> assertEquals(savedFileId, returnedFileOverviewDto.getId()),
                        () -> assertEquals(fileToServe.getUserFileName(), returnedFileOverviewDto.getUserFileName()),
                        () -> assertEquals(fileToServe.getOriginalFileName(), returnedFileOverviewDto.getOriginalFilename()),
                        () -> assertEquals(fileToServe.getContentType(), returnedFileOverviewDto.getFileContentType()),
                        () -> assertEquals(fileToServe.getOwner().getId(), returnedFileOverviewDto.getOwner().getId()),
                        () -> assertEquals(fileToServe.getUploadDateTime(), returnedFileOverviewDto.getUploadDateTime())
                );

            }

        }

        @Nested
        @Transactional
        @DisplayName("Get file data by id tests:")
        class getFileDataByIdTests{
            private UUID savedFileId;
            private ZonedDateTime fileUploadDateTime;
            private final UUID notExistingFileId = UUID.randomUUID();
            private final String FILE_NAME_ORIGINAL = "image.png";

            @BeforeEach
             void setUp() {
                Event testEvent = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new);
                User fileOwner = userRepository.findByEmail(FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);

                fileUploadDateTime = ZonedDateTime.now().withSecond(0).withNano(0);
                File fileToSave = File.builder()
                        .event(testEvent)
                        .content(TestFileContentFactory.png())
                        .contentType(ContentType.IMAGE_PNG.getMimeType())
                        .originalFileName(FILE_NAME_ORIGINAL)
                        .userFileName(FILE_NAME_USER)
                        .uploadDateTime(fileUploadDateTime)
                        .owner(fileOwner)
                        .event(testEvent)
                        .build();

                File savedFile = fileRepository.save(fileToSave);

                testEvent.addFile(savedFile);
                eventRepository.save(testEvent);

                fileOwner.addFile(savedFile);
                userRepository.save(fileOwner);

                savedFileId = savedFile.getId();
            }

            @Test
            @DisplayName("When getting file data by id should throw FileNotFoundInEventException if file with given id does not exist")
            public void whenGettingFileDataByIdShouldThrowFileNotFoundInEventExceptionIfFileWithGivenIdDoesNotExist(){
                assertThrows(FileNotFoundInEventException.class, () -> eventService.getFileDataById(notExistingFileId, savedEventId, firstUserJwt), "Expected to throw FileNotFoundInEvent");
            }

            @Test
            @DisplayName("When getting file data by id should throw EventNotFoundException if event with given id does not exist")
            public void whenGettingFileDataByIdShouldThrowFileNotFoundInEventIfEventWithGivenIdDoesNotExist(){
                assertThrows(EventNotFoundException.class, () -> eventService.getFileDataById(savedFileId, NOT_EXISTING_EVENT_ID, firstUserJwt), "Expected to throw EventNotFoundException if event with given id does not exist.");
            }

            @Test
            @DisplayName("When getting file data by id should throw FileNotFoundInEventException if file and event exist but are not related")
            public void whenGettingFileDataByIdShouldThrowFileNotFoundInEventExceptionIfFileAndEventExistButAreNotRelated(){

                eventCreateDto = EventCreateDto.builder()
                        .name(EVENT_NAME)
                        .shortDescription(EVENT_SHORT_DESCRIPTION)
                        .longDescription(EVENT_LONG_DESCRIPTION)
                        .eventStartDate(EVENT_START_DATE)
                        .city(EVENT_CITY)
                        .exactAddress(EVENT_EXACT_ADDRESS)
                        .tags(Arrays.stream(EVENT_TAGS).toList())
                        .build();

                UUID secondEventId  = eventService.createEvent(eventCreateDto, firstUserJwt).getId();

                assertThrows(FileNotFoundInEventException.class, () -> eventService.getFileDataById(savedFileId, secondEventId, firstUserJwt), "Expected to throw FileNotFoundInEventException if file and event exist but are not related");
            }

            @Test
            @DisplayName("When getting file data by id should throw NotEventAttenderException if user is not attending event with given id")
            public void whenGettingFileDataByIdShouldThrowNotEventAttenderExceptionIfUserIsNotAttendingEventWithGivenId(){
                assertThrows(NotEventAttenderException.class, () -> eventService.getFileDataById(savedFileId, savedEventId, secondUserJwt), "Expected to throw NotEventAttenderException if performing user is not attending event in any form.");
            }

            @Test
            @DisplayName("When getting file data by id should return correct file")
            public void whenGettingFileDataByIdShouldReturnCorrectFile(){
                File expectedFile = fileRepository.findById(savedFileId).orElseThrow(FileNotFoundException::new);
                File returnedFile = eventService.getFileDataById(savedFileId, savedEventId, firstUserJwt);
                assertAll("Returned data verification:",
                        () -> assertEquals(expectedFile.getId(), returnedFile.getId(), "Expected returned file to be have correct id."),
                        () -> assertEquals(expectedFile.getContent(), returnedFile.getContent()),
                        () -> assertEquals(expectedFile.getEvent(), returnedFile.getEvent()),
                        () -> assertEquals(expectedFile.getContentType(), returnedFile.getContentType()),
                        () -> assertEquals(expectedFile.getUserFileName(), returnedFile.getUserFileName()),
                        () -> assertEquals(expectedFile.getOriginalFileName(), returnedFile.getOriginalFileName()),
                        () -> assertEquals(expectedFile.getOwner(), returnedFile.getOwner()),
                        () -> assertEquals(expectedFile.getUploadDateTime(), returnedFile.getUploadDateTime())
                        );

            }

        }

        @Nested
        @Transactional
        @DisplayName("Get file overview page by event id tests:")
        class GetFileOverviewPageByEventIdTests {

            private final int PAGE_NUMBER_ZERO = 0;
            private final int PAGE_NUMBER_ONE = 1;
            private final int PAGE_ZERO_ELEMENTS_COUNT = 20;
            private final int PAGE_ONE_ELEMENTS_COUNT = 10;
            private final int TOTAL_ELEMENTS_ZERO = 0;
            private final int TOTAL_ELEMENTS_FIVE = 5;
            private final int TOTAL_ELEMENTS_TWENTY = 20;
            private final int TOTAL_ELEMENTS_THIRTY = 30;
            private final int DEFAULT_PAGE_SIZE = 20;


            @Test
            @DisplayName("When getting file overview page should throw InvalidPageNumberException if page number is negative")
            public void whenGettingFileOverviewPageShouldThrowInvalidPageNumberExceptionIfPageNumberIsNegative() {
                int negativePageNumber = -1;
                assertThrows(InvalidPageNumberException.class, () -> eventService.getFileOverviewPageByEventId(savedEventId, negativePageNumber, firstUserJwt));

            }

            @Test
            @DisplayName("When getting file overview page should throw EventNotFoundException if event with given id does not exist")
            public void whenGettingFileOverviewPageShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
                assertThrows(EventNotFoundException.class, () -> eventService.getFileOverviewPageByEventId(NOT_EXISTING_EVENT_ID, PAGE_NUMBER_ZERO, firstUserJwt));
            }

            @Test
            @DisplayName("When getting file overview page should throw NotEventAttenderException if user is not attending event with given id")
            public void whenGettingFileOverviewPageShouldThrowNotEventAttenderExceptionIfUserIsNotAttendingEventWithGivenId() {
                assertThrows(NotEventAttenderException.class, () -> eventService.getFileOverviewPageByEventId(savedEventId, PAGE_NUMBER_ZERO, secondUserJwt));
            }

            @Test
            @DisplayName("When getting file overview page should return empty file overview page if there is no file in event")
            public void whenGettingFileOverviewPageShouldReturnEmptyFileOverviewPageIfThereIsNoFileInEvent() {

                FileOverviewPageDto returnedPage = eventService.getFileOverviewPageByEventId(savedEventId, PAGE_NUMBER_ZERO, firstUserJwt);
                assertAll("Empty page verifications: ",
                        () -> assertTrue(returnedPage.fileOverviews().isEmpty(), "Expected to be empty."),
                        () -> assertTrue(returnedPage.lastPage(), "Expected to be marked as last page."),
                        () -> assertEquals(TOTAL_ELEMENTS_ZERO, returnedPage.totalElements(), "Expected total elements to be zero."),
                        () -> assertEquals(DEFAULT_PAGE_SIZE, returnedPage.pageSize(), "Expected default page size of " + DEFAULT_PAGE_SIZE)
                );
            }

            @Test
            @DisplayName("When getting file overview page should return file overview page with five files")
            public void whenGettingFileOverviewPageShouldReturnFileOverviewPageWithFiveFiles() {

                Set<UUID> savedFilesIds = prepareAndUploadFiles(TOTAL_ELEMENTS_FIVE);

                FileOverviewPageDto returnedPage = eventService.getFileOverviewPageByEventId(savedEventId, PAGE_NUMBER_ZERO, firstUserJwt);
                assertAll("Five files page verifications: ",
                        () -> assertEquals(PAGE_NUMBER_ZERO, returnedPage.pageNumber(), "Expected returned page to have the same number as requested"),
                        () -> assertEquals(TOTAL_ELEMENTS_FIVE, returnedPage.fileOverviews().size(), "Expected to contain " + TOTAL_ELEMENTS_FIVE + " elements"),
                        () -> assertTrue(returnedPage.lastPage(), "Expected to be marked as last page."),
                        () -> assertEquals(TOTAL_ELEMENTS_FIVE, returnedPage.totalElements(), "Expected total elements to be " + TOTAL_ELEMENTS_FIVE),
                        () -> assertEquals(DEFAULT_PAGE_SIZE, returnedPage.pageSize(), "Expected default page size of " + DEFAULT_PAGE_SIZE),
                        () -> assertEquals(savedFilesIds, returnedPage.fileOverviews().stream().map(FileOverviewDto::getId).collect(Collectors.toUnmodifiableSet()), "Expected to contain correct file overviews")
                );

            }
            @Test
            @DisplayName("When getting file overview page should return file overview page with files sorted from the oldest to the newest")
            public void whenGettingFileOverviewPageShouldReturnFileOverviewPageWithFilesSortedFromTheOldestToTheNewest() {

                Set<UUID> savedFilesIds = prepareAndUploadFiles(TOTAL_ELEMENTS_FIVE);

                List<File> uploadedFiles = fileRepository.findAll();
                AtomicInteger minutesAmount = new AtomicInteger(1);
                uploadedFiles.forEach(file -> file.setUploadDateTime(file.getUploadDateTime().plusMinutes(minutesAmount.getAndIncrement())));

                FileOverviewPageDto returnedPage = eventService.getFileOverviewPageByEventId(savedEventId, PAGE_NUMBER_ZERO, firstUserJwt);

                assertAll("Five files page verifications: ",
                        () -> assertEquals(PAGE_NUMBER_ZERO, returnedPage.pageNumber(), "Expected returned page to have the same number as requested"),
                        () -> assertEquals(TOTAL_ELEMENTS_FIVE, returnedPage.fileOverviews().size(), "Expected to contain " + TOTAL_ELEMENTS_FIVE + " elements"),
                        () -> assertTrue(returnedPage.lastPage(), "Expected to be marked as last page."),
                        () -> assertEquals(TOTAL_ELEMENTS_FIVE, returnedPage.totalElements(), "Expected total elements to be " + TOTAL_ELEMENTS_FIVE),
                        () -> assertEquals(DEFAULT_PAGE_SIZE, returnedPage.pageSize(), "Expected default page size of " + DEFAULT_PAGE_SIZE),
                        () -> assertEquals(savedFilesIds, returnedPage.fileOverviews().stream().map(FileOverviewDto::getId).collect(Collectors.toUnmodifiableSet()), "Expected to contain correct file overviews"),
                        () -> {
                            List<ZonedDateTime> uploadTimes = returnedPage.fileOverviews().stream().map(FileOverviewDto::getUploadDateTime).toList();
                            List<ZonedDateTime> sortedUploadTimes = uploadTimes.stream().sorted().toList();
                            assertEquals(sortedUploadTimes, uploadTimes, "Expected files to be sorted from oldest to newest by uploadDateTime");
                        }
                );

            }

            @Test
            @DisplayName("When getting file overview page should return twenty file overviews only on one page")
            public void whenGettingFileOverviewPageShouldReturnTwentyFileOverviewsOnlyOnOnePage() {

                Set<UUID> savedFilesIds = prepareAndUploadFiles(TOTAL_ELEMENTS_TWENTY);

                FileOverviewPageDto returnedPage = eventService.getFileOverviewPageByEventId(savedEventId, PAGE_NUMBER_ZERO, firstUserJwt);
                assertAll("Twenty file overviews verifications: ",
                        () -> assertEquals(PAGE_NUMBER_ZERO, returnedPage.pageNumber(), "Expected returned page to have the same number as requested"),
                        () -> assertEquals(TOTAL_ELEMENTS_TWENTY, returnedPage.fileOverviews().size(), "Expected to contain " + TOTAL_ELEMENTS_TWENTY + " elements"),
                        () -> assertTrue(returnedPage.lastPage(), "Expected to be marked as last page."),
                        () -> assertEquals(TOTAL_ELEMENTS_TWENTY, returnedPage.totalElements(), "Expected total elements to be " + TOTAL_ELEMENTS_TWENTY),
                        () -> assertEquals(DEFAULT_PAGE_SIZE, returnedPage.pageSize(), "Expected default page size of " + DEFAULT_PAGE_SIZE),
                        () -> assertEquals(savedFilesIds, returnedPage.fileOverviews().stream().map(FileOverviewDto::getId).collect(Collectors.toUnmodifiableSet()), "Expected to contain correct file overviews")
                );

            }

            @Test
            @DisplayName("When getting file overview page should return full file overview page and provide info if there is more than twenty files in event")
            public void whenGettingFileOverviewPageShouldReturnFullFileOverviewPageAndProvideInfoIfThereIsMoreThanTwentyFilesInEvent() {

                Set<UUID> savedFilesIds = prepareAndUploadFiles(TOTAL_ELEMENTS_THIRTY);

                FileOverviewPageDto returnedPageZero = eventService.getFileOverviewPageByEventId(savedEventId, PAGE_NUMBER_ZERO, firstUserJwt);

                assertAll("File overviews page zero verifications: ",
                        () -> assertEquals(PAGE_NUMBER_ZERO, returnedPageZero.pageNumber(), "Expected returned page to have the same number as requested"),
                        () -> assertEquals(PAGE_ZERO_ELEMENTS_COUNT, returnedPageZero.fileOverviews().size(), "Expected to contain " + PAGE_ZERO_ELEMENTS_COUNT + " elements"),
                        () -> assertFalse(returnedPageZero.lastPage(), "Expected to not be marked as last page."),
                        () -> assertEquals(TOTAL_ELEMENTS_THIRTY, returnedPageZero.totalElements(), "Expected total elements to be " + TOTAL_ELEMENTS_THIRTY),
                        () -> assertEquals(DEFAULT_PAGE_SIZE, returnedPageZero.pageSize(), "Expected default page size of " + DEFAULT_PAGE_SIZE)

                );

                FileOverviewPageDto returnedPageOne = eventService.getFileOverviewPageByEventId(savedEventId, PAGE_NUMBER_ONE, firstUserJwt);
                assertAll("File overviews page one verifications: ",
                        () -> assertEquals(PAGE_NUMBER_ONE, returnedPageOne.pageNumber(), "Expected returned page to have the same number as requested"),
                        () -> assertEquals(PAGE_ONE_ELEMENTS_COUNT, returnedPageOne.fileOverviews().size(), "Expected to contain " + PAGE_ONE_ELEMENTS_COUNT + " elements"),
                        () -> assertTrue(returnedPageOne.lastPage(), "Expected to be marked as last page."),
                        () -> assertEquals(TOTAL_ELEMENTS_THIRTY, returnedPageOne.totalElements(), "Expected total elements to be " + TOTAL_ELEMENTS_THIRTY),
                        () -> assertEquals(DEFAULT_PAGE_SIZE, returnedPageOne.pageSize(), "Expected default page size of " + DEFAULT_PAGE_SIZE)
                );
                Set<UUID> pageZeroIds = returnedPageZero.fileOverviews().stream().map(FileOverviewDto::getId).collect(Collectors.toUnmodifiableSet());
                Set<UUID> pageOneIds = returnedPageOne.fileOverviews().stream().map(FileOverviewDto::getId).collect(Collectors.toUnmodifiableSet());

                assertAll("Cross-page verifications",
                        () -> {
                            Set<UUID> intersection = new HashSet<>(pageZeroIds);
                            intersection.retainAll(pageOneIds);
                            assertTrue(intersection.isEmpty(),
                                    "Expected to not overlap file overviews between pages.");
                        },
                        () -> assertEquals(TOTAL_ELEMENTS_THIRTY, pageZeroIds.size() + pageOneIds.size(),
                                "Expected to contain exactly " + TOTAL_ELEMENTS_THIRTY + " unique IDs"),
                        () -> {
                            Set<UUID> allReturnedIds = new HashSet<>(pageZeroIds);
                            allReturnedIds.addAll(pageOneIds);
                            assertEquals(savedFilesIds, allReturnedIds,
                                    "Expected to contain the same thirty unique IDs, as the ones retrieved from saving files.");
                        }
                );
            }

            @Test
            @DisplayName("When getting file overview page should return empty file overviews page if requested page number exceeds total page number available")
            public void whenGettingFileOverviewPageShouldReturnEmptyFileOverviewsPageIfRequestedPageNumberExceedsTotalPageNumberAvailable() {

                Set<UUID> savedFilesIds = prepareAndUploadFiles(TOTAL_ELEMENTS_TWENTY);

                FileOverviewPageDto returnedPageZero = eventService.getFileOverviewPageByEventId(savedEventId, PAGE_NUMBER_ZERO, firstUserJwt);
                assertAll("Twenty file overviews verifications: ",
                        () -> assertEquals(PAGE_NUMBER_ZERO, returnedPageZero.pageNumber(), "Expected returned page to have the same number as requested"),
                        () -> assertEquals(TOTAL_ELEMENTS_TWENTY, returnedPageZero.fileOverviews().size(), "Expected to contain " + TOTAL_ELEMENTS_TWENTY + " elements"),
                        () -> assertTrue(returnedPageZero.lastPage(), "Expected to be marked as last page."),
                        () -> assertEquals(TOTAL_ELEMENTS_TWENTY, returnedPageZero.totalElements(), "Expected total elements to be " + TOTAL_ELEMENTS_TWENTY),
                        () -> assertEquals(DEFAULT_PAGE_SIZE, returnedPageZero.pageSize(), "Expected default page size of " + DEFAULT_PAGE_SIZE),
                        () -> assertEquals(savedFilesIds, returnedPageZero.fileOverviews().stream().map(FileOverviewDto::getId).collect(Collectors.toUnmodifiableSet()), "Expected to contain correct file overviews")
                );

                FileOverviewPageDto returnedPageOne = eventService.getFileOverviewPageByEventId(savedEventId, PAGE_NUMBER_ONE, firstUserJwt);
                assertAll("Over limit page verifications: ",
                        () -> assertEquals(PAGE_NUMBER_ONE, returnedPageOne.pageNumber(), "Expected returned page to have the same number as requested"),
                        () -> assertTrue(returnedPageOne.fileOverviews().isEmpty() , "Expected to return empty file overviews page"),
                        () -> assertTrue(returnedPageOne.lastPage(), "Expected to be marked as last page."),
                        () -> assertEquals(TOTAL_ELEMENTS_TWENTY, returnedPageOne.totalElements(), "Expected total elements to be " + TOTAL_ELEMENTS_TWENTY),
                        () -> assertEquals(DEFAULT_PAGE_SIZE, returnedPageOne.pageSize(), "Expected default page size of " + DEFAULT_PAGE_SIZE)
                );

            }

            private Set<UUID> prepareAndUploadFiles(int fileAmount) {
                AtomicInteger fileCounter = new AtomicInteger(0);

                List<MockMultipartFile> testFilesMocks = Stream.generate(EventFileTests::allowedFileProvider)
                        .flatMap(stream -> stream)
                        .limit(fileAmount)
                        .map(allowedFile ->
                                new MockMultipartFile(
                                        "file",
                                        "allowed_" + fileCounter.getAndIncrement() + allowedFile.extension(),
                                        allowedFile.contentType(),
                                        allowedFile.bytes()
                                )
                        ).toList();
                fileCounter.set(0);

                Set<FileUploadDto> fileUploadDtoSet = testFilesMocks.stream()
                        .map(mock -> FileUploadDto.builder()
                                .file(mock)
                                .userFileName("user_filename_" + fileCounter.getAndIncrement())
                                .build()
                        ).collect(Collectors.toUnmodifiableSet());

                return fileUploadDtoSet.stream().map(dto -> {
                            try {
                                return eventService.uploadFileToEvent(dto, savedEventId, firstUserJwt).getId();
                            } catch (IOException e) {
                                fail("File upload failed: " + e.getMessage());
                                return null;
                            }
                        })
                        .collect(Collectors.toUnmodifiableSet());
            }

        }


    }

}
