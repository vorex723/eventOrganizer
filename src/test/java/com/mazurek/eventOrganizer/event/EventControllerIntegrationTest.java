package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.exception.ApiErrorCode;
import tools.jackson.databind.ObjectMapper;
import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationRequest;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.dto.EventDto;
import com.mazurek.eventOrganizer.exception.event.EventAlreadyHadPlaceException;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.EventOwnerAlreadyAttendsEventException;
import com.mazurek.eventOrganizer.exception.event.EventOwnerMustAttendEventException;
import com.mazurek.eventOrganizer.exception.event.AlreadyAttendingEventException;
import com.mazurek.eventOrganizer.exception.event.NotEventAttendeeException;
import com.mazurek.eventOrganizer.exception.event.NotEventOwnerException;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.tag.TagRepository;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestDataInitializer;
import com.mazurek.eventOrganizer.testData.builders.AuthenticationRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.EventCreateDtoTestBuilder;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Locale;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("EventController integration tests:")
public class EventControllerIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private TestDataInitializer testDataInitializer;
    @Autowired
    private DeletionService deletionService;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private String firstUserJwt;
    private String secondUserJwt;
    private EventCreateDto eventCreateDto;

    private final AuthenticationRequest firstUserAuthRequest =
            AuthenticationRequestTestBuilder.authenticationRequestForFirstUser().build();
    private final AuthenticationRequest secondUserAuthRequest =
            AuthenticationRequestTestBuilder.authenticationRequestForSecondUser().build();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        assertThat(TestTransaction.isActive())
                .as("HTTP workflows must not join a test transaction")
                .isFalse();
        deletionService.deleteAllSafe();
        assertDatabaseIsClean();
        authHelper.setupRolesAndUsers();

        firstUserJwt = AuthConstants.JWT_PREFIX + authenticationService.authenticate(firstUserAuthRequest, DeviceType.WEB).getAccessToken();
        secondUserJwt = AuthConstants.JWT_PREFIX + authenticationService.authenticate(secondUserAuthRequest, DeviceType.WEB).getAccessToken();

        eventCreateDto = EventCreateDtoTestBuilder.firstEvent().build();
    }

    @AfterEach
    void tearDown() {
        try {
            assertThat(TestTransaction.isActive())
                    .as("Cleanup must commit outside a test transaction")
                    .isFalse();
            deletionService.deleteAllSafe();
            assertDatabaseIsClean();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void assertDatabaseIsClean() {
        assertSoftly(softly -> {
            softly.assertThat(eventRepository.count()).as("Committed workflow cleanup: events").isZero();
            softly.assertThat(userRepository.count()).as("Committed workflow cleanup: users").isZero();
            softly.assertThat(tagRepository.count()).as("Committed workflow cleanup: tags").isZero();
            softly.assertThat(cityRepository.count()).as("Committed workflow cleanup: cities").isZero();
        });
    }

    // Only fixture preparation needs a managed lazy collection; HTTP actions run outside this transaction.
    private void addSecondUserAsAttendee(UUID eventId) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Event event = requirePresent(eventRepository.findById(eventId), "Expected event to exist");
            User attendee = requirePresent(
                    userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL),
                    "Expected second user to exist after auth setup");
            event.addAttendee(attendee);
        });
    }

    private String toJsonTimestamp(Instant instant) {
        return instant.toString();
    }

    private Set<String> findTagNamesByEventId(UUID eventId) {
        return new HashSet<>(jdbcTemplate.queryForList("""
                select t.name
                from tags t
                join event_tag et on t.id = et.tag_id
                where et.event_id = ?
                """, String.class, eventId));
    }

    private boolean eventHasAttendee(UUID eventId, UUID userId) {
        Boolean exists = jdbcTemplate.queryForObject("""
                select count(*) > 0
                from event_user
                where event_id = ? and user_id = ?
                """, Boolean.class, eventId, userId);
        return Boolean.TRUE.equals(exists);
    }

    // ===========================================================================================
    // POST /api/v1/events
    // ===========================================================================================

    @Nested
    @DisplayName("Create event tests: POST /api/v1/events")
    class CreateEventTests {

        @Test
        @DisplayName("When creating event should return HTTP 401 Unauthorized if there is no Authorization header")
        public void whenCreatingEventShouldReturnUnauthorizedIfThereIsNoAuthorizationHeader() throws Exception {
            var beforeWrite = eventWriteState();

            mockMvc.perform(post(ApiConstants.EVENTS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(eventCreateDto)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));

            assertThat(eventWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When creating event should return HTTP 400 Bad Request on every data validation error")
        public void whenCreatingEventShouldReturnBadRequestOnEveryDataValidationError() throws Exception {
            eventCreateDto.setName(EventConstants.WRONG_NAME);
            eventCreateDto.setShortDescription(EventConstants.WRONG_SHORT_DESCRIPTION_TOO_SHORT);
            eventCreateDto.setLongDescription(EventConstants.WRONG_LONG_DESCRIPTION_TOO_SHORT);
            eventCreateDto.setCityExternalId(UserConstants.INVALID_CITY_NAME);
            eventCreateDto.setExactAddress(EventConstants.WRONG_EXACT_ADDRESS);
            eventCreateDto.setEventStartDate(null);

            var beforeWrite = eventWriteState();

            mockMvc.perform(post(ApiConstants.EVENTS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(eventCreateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_FAILED))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.errors").isMap())
                    .andExpect(jsonPath("$.errors.name").hasJsonPath())
                    .andExpect(jsonPath("$.errors.shortDescription").hasJsonPath())
                    .andExpect(jsonPath("$.errors.longDescription").hasJsonPath())
                    .andExpect(jsonPath("$.errors.cityExternalId").hasJsonPath())
                    .andExpect(jsonPath("$.errors.exactAddress").hasJsonPath())
                    .andExpect(jsonPath("$.errors.eventStartDate").hasJsonPath())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            assertThat(eventWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When creating event should return HTTP 400 Bad Request if event metadata is invalid")
        public void whenCreatingEventShouldReturnBadRequestIfEventMetadataIsInvalid() throws Exception {
            eventCreateDto.setEventStartDate(TimeConstants.NOW.plus(1, ChronoUnit.HOURS));
            eventCreateDto.setTags(Set.of(EventConstants.WRONG_TAG_NAME));

            var beforeWrite = eventWriteState();

            mockMvc.perform(post(ApiConstants.EVENTS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(eventCreateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_FAILED))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.errors.eventStartDate").hasJsonPath())
                    .andExpect(jsonPath("$.errors['tags[]']").hasJsonPath());

            assertThat(eventWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When creating event exactly 48 hours ahead should return HTTP 400 Bad Request")
        public void whenCreatingEventExactlyFortyEightHoursAheadShouldReturnBadRequest() throws Exception {
            eventCreateDto.setEventStartDate(TimeConstants.TWO_DAYS_FROM_NOW);

            var beforeWrite = eventWriteState();

            mockMvc.perform(post(ApiConstants.EVENTS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(eventCreateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_FAILED))
                    .andExpect(jsonPath("$.errors.eventStartDate").hasJsonPath());

            assertThat(eventWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When creating event with unlimited capacity should return HTTP 201 Created")
        public void whenCreatingEventWithUnlimitedCapacityShouldReturnCreated() throws Exception {
            eventCreateDto.setMaxAttendees(null);

            MvcResult result = mockMvc.perform(post(ApiConstants.EVENTS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(eventCreateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.maxAttendees").value(org.hamcrest.Matchers.nullValue()))
                    .andReturn();

            EventDto response = objectMapper.readValue(result.getResponse().getContentAsString(), EventDto.class);
            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(requirePresent(eventRepository.findById(response.getId()),
                    "Expected committed unlimited event after HTTP create").getMaxAttendees()).isNull();
        }

        @Test
        @DisplayName("When creating event should return HTTP 400 Bad Request if tags are null")
        public void whenCreatingEventShouldReturnBadRequestIfTagsAreNull() throws Exception {
            eventCreateDto.setTags(null);

            var beforeWrite = eventWriteState();

            mockMvc.perform(post(ApiConstants.EVENTS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(eventCreateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_FAILED))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.errors.tags").hasJsonPath());

            assertThat(eventWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When creating event should return HTTP 201 Created on success")
        public void whenCreatingEventShouldReturnCreatedOnSuccess() throws Exception {
            mockMvc.perform(post(ApiConstants.EVENTS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(eventCreateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("When creating event should return HTTP 201 Created with created event dto and correct data")
        public void whenCreatingEventShouldReturnDtoOfCreatedEventWithCorrectData() throws Exception {
            Instant expectedStartDate = TimeConstants.ONE_WEEK_FROM_NOW;
            UUID ownerId = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL),
                    "Expected owner before checking created event response").getId();
            UUID cityId = requirePresent(cityRepository.findByExternalId(eventCreateDto.getCityExternalId()),
                    "Expected existing city before HTTP event creation").getId();

            mockMvc.perform(post(ApiConstants.EVENTS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(eventCreateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").hasJsonPath())
                    .andExpect(jsonPath("$.name").value(eventCreateDto.getName()))
                    .andExpect(jsonPath("$.shortDescription").value(eventCreateDto.getShortDescription()))
                    .andExpect(jsonPath("$.longDescription").value(eventCreateDto.getLongDescription()))
                    .andExpect(jsonPath("$.cityExternalId").value(eventCreateDto.getCityExternalId()))
                    .andExpect(jsonPath("$.cityId").value(cityId.toString()))
                    .andExpect(jsonPath("$.exactAddress").value(eventCreateDto.getExactAddress()))
                    .andExpect(jsonPath("$.timeZone").value(com.mazurek.eventOrganizer.testData.TestCityData.timeZoneId(eventCreateDto.getCityExternalId())))
                    .andExpect(jsonPath("$.tags", hasSize(eventCreateDto.getTags().size())))
                    .andExpect(jsonPath("$.tags", hasItems(TagConstants.FIRST_TAG_NAME, TagConstants.SECOND_TAG_NAME)))
                    .andExpect(jsonPath("$.eventStartDate").value(toJsonTimestamp(expectedStartDate)))
                    .andExpect(jsonPath("$.createDate").value(toJsonTimestamp(TimeConstants.NOW)))
                    .andExpect(jsonPath("$.lastUpdate").value(toJsonTimestamp(TimeConstants.NOW)))
                    .andExpect(jsonPath("$.owner.id").value(ownerId.toString()));
        }

        @Test
        @DisplayName("When creating event should persist event with correct owner, city and tags")
        public void whenCreatingEventShouldPersistEventWithCorrectOwnerCityAndTags() throws Exception {
            MvcResult mvcResult = mockMvc.perform(post(ApiConstants.EVENTS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(eventCreateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isCreated())
                    .andReturn();

            EventDto createdEvent = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), EventDto.class);
            assertThat(createdEvent).isNotNull();
            assertThat(createdEvent.getId()).isNotNull();
            Event savedEvent = requirePresent(
                    eventRepository.findById(createdEvent.getId()),
                    "Expected created event to be persisted");
            User owner = requirePresent(
                    userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL),
                    "Expected first user to exist after auth setup");

            assertThat(savedEvent.getOwner().getId()).isEqualTo(owner.getId());
            assertThat(savedEvent.getCity().getExternalId()).isEqualTo(eventCreateDto.getCityExternalId());
            assertThat(savedEvent.getCity().getTimeZoneId()).isEqualTo(
                    com.mazurek.eventOrganizer.testData.TestCityData.timeZoneId(eventCreateDto.getCityExternalId()));
            assertThat(findTagNamesByEventId(createdEvent.getId())).isEqualTo(eventCreateDto.getTags());
            assertThat(savedEvent).extracting(Event::getName, Event::getShortDescription,
                            Event::getLongDescription, Event::getExactAddress, Event::getEventStartDate,
                            Event::getCreateDate, Event::getLastUpdate, Event::getMaxAttendees, Event::getAttendeeCount)
                    .containsExactly(eventCreateDto.getName(), eventCreateDto.getShortDescription(),
                            eventCreateDto.getLongDescription(), eventCreateDto.getExactAddress(),
                            TimeConstants.ONE_WEEK_FROM_NOW, TimeConstants.NOW, TimeConstants.NOW,
                            eventCreateDto.getMaxAttendees(), 0);
        }
    }

    // ===========================================================================================
    // GET /api/v1/events/{eventId}
    // ===========================================================================================

    @Nested
    @DisplayName("Get event by id tests: GET /api/v1/events/{eventId}")
    class GetEventByIdTests {

        private UUID savedEventId;

        @BeforeEach
        void setUp() {
            savedEventId = testDataInitializer.setupFirstEvent();
        }

        @Test
        @DisplayName("When getting event by id without authentication should return public details without attendee identities")
        public void whenGettingEventByIdWithoutAuthenticationShouldReturnPublicDetails() throws Exception {
            testDataInitializer.addSecondUserToAttendees(savedEventId);

            mockMvc.perform(get(ApiConstants.EVENT_BY_ID_URL, savedEventId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(savedEventId.toString()))
                    .andExpect(jsonPath("$.attendeeCount").value(1))
                    .andExpect(jsonPath("$.owner.homeCity").doesNotHaveJsonPath())
                    .andExpect(jsonPath("$.attendees").doesNotExist());
        }

        @Test
        @DisplayName("When getting event by id should return HTTP 404 Not Found if event does not exist")
        public void whenGettingEventByIdShouldReturnNotFoundIfEventDoesNotExist() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_BY_ID_URL, EventConstants.NOT_EXISTING_EVENT_ID)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.EVENT_NOT_FOUND))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(EventNotFoundException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When getting event by id should return HTTP 200 OK if event exists")
        public void whenGettingEventByIdShouldReturnOkIfEventExists() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_BY_ID_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("When getting event by id should return HTTP 200 OK with event dto and correct data")
        public void whenGettingEventByIdShouldReturnDtoOfEventWithCorrectData() throws Exception {
            User owner = requirePresent(
                    userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL),
                    "Expected first user to exist after auth setup");
            owner.setTimeZone("Asia/Tokyo");
            userRepository.saveAndFlush(owner);
            Event storedEvent = requirePresent(eventRepository.findById(savedEventId),
                    "Expected event to exist after setup");
            assertThat(owner.getTimeZone()).isNotEqualTo(storedEvent.getCity().getTimeZoneId());
            Instant expectedStartDate = TimeConstants.ONE_WEEK_FROM_NOW.truncatedTo(ChronoUnit.MINUTES);

            mockMvc.perform(get(ApiConstants.EVENT_BY_ID_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(savedEventId.toString()))
                    .andExpect(jsonPath("$.name").value(EventConstants.FIRST_EVENT_NAME))
                    .andExpect(jsonPath("$.shortDescription").value(EventConstants.FIRST_EVENT_SHORT_DESC))
                    .andExpect(jsonPath("$.longDescription").value(EventConstants.FIRST_EVENT_LONG_DESC))
                    .andExpect(jsonPath("$.city").value(CitiesConstants.WARSAW_NAME))
                    .andExpect(jsonPath("$.cityId").value(storedEvent.getCity().getId().toString()))
                    .andExpect(jsonPath("$.cityExternalId").value(storedEvent.getCity().getExternalId()))
                    .andExpect(jsonPath("$.exactAddress").value(EventConstants.FIRST_EVENT_ADDRESS))
                    .andExpect(jsonPath("$.timeZone").value(storedEvent.getCity().getTimeZoneId()))
                    .andExpect(jsonPath("$.tags", hasItems(TagConstants.FIRST_TAG_NAME, TagConstants.SECOND_TAG_NAME)))
                    .andExpect(jsonPath("$.owner.id").value(owner.getId().toString()))
                    .andExpect(jsonPath("$.attendeeCount").value(0))
                    .andExpect(jsonPath("$.attendees").doesNotExist())
                    .andExpect(jsonPath("$.eventStartDate").value(toJsonTimestamp(expectedStartDate)))
                    .andExpect(jsonPath("$.createDate").isNotEmpty())
                    .andExpect(jsonPath("$.lastUpdate").isNotEmpty());
        }
    }

    // ===========================================================================================
    // PUT /api/v1/events/{eventId}
    // ===========================================================================================

    @Nested
    @DisplayName("Update event tests: PUT /api/v1/events/{eventId}")
    class UpdateEventTests {

        private UUID savedEventId;
        private EventCreateDto updateEventDto;

        @BeforeEach
        void setUp() {
            savedEventId = testDataInitializer.setupFirstEvent();
            updateEventDto = EventCreateDtoTestBuilder.updatedEvent().build();
        }

        @Test
        @DisplayName("When updating event should return HTTP 401 Unauthorized if there is no Authorization header")
        public void whenUpdatingEventShouldReturnUnauthorizedIfThereIsNoAuthorizationHeader() throws Exception {
            var beforeWrite = eventWriteState();

            mockMvc.perform(put(ApiConstants.EVENT_BY_ID_URL, savedEventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateEventDto)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));

            assertThat(eventWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When updating event should return HTTP 404 Not Found if event does not exist")
        public void whenUpdatingEventShouldReturnNotFoundIfEventDoesNotExist() throws Exception {
            var beforeWrite = eventWriteState();

            mockMvc.perform(put(ApiConstants.EVENT_BY_ID_URL, EventConstants.NOT_EXISTING_EVENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateEventDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.EVENT_NOT_FOUND))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(EventNotFoundException.DEFAULT_MESSAGE));

            assertThat(eventWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When updating event should return HTTP 409 Conflict if event had place")
        public void whenUpdatingEventShouldReturnConflictIfEventHadPlace() throws Exception {
            Event savedEvent = requirePresent(
                    eventRepository.findById(savedEventId),
                    "Expected event to exist after creation");
            savedEvent.setEventStartDate(TimeConstants.ONE_WEEK_AGO);
            eventRepository.save(savedEvent);

            var beforeWrite = eventWriteState();

            mockMvc.perform(put(ApiConstants.EVENT_BY_ID_URL, savedEventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateEventDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.EVENT_ALREADY_STARTED))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                    .andExpect(jsonPath("$.message").value(EventAlreadyHadPlaceException.DEFAULT_MESSAGE));

            Event unchangedEvent = requirePresent(eventRepository.findById(savedEventId), "Expected rejected update to preserve event");
            assertThat(unchangedEvent.getName()).isEqualTo(savedEvent.getName());
            assertThat(unchangedEvent.getEventStartDate()).isEqualTo(TimeConstants.ONE_WEEK_AGO);
            assertThat(unchangedEvent.getLastUpdate()).isEqualTo(savedEvent.getLastUpdate());
            assertThat(findTagNamesByEventId(savedEventId)).isEqualTo(eventCreateDto.getTags());

            assertThat(eventWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When updating event should return HTTP 403 Forbidden if performing user does not own event")
        public void whenUpdatingEventShouldReturnForbiddenIfPerformingUserDoesNotOwnEvent() throws Exception {
            var beforeWrite = eventWriteState();

            mockMvc.perform(put(ApiConstants.EVENT_BY_ID_URL, savedEventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateEventDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.NOT_EVENT_OWNER))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.message").value(NotEventOwnerException.DEFAULT_MESSAGE));

            assertThat(eventWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When updating event should return HTTP 400 Bad Request if request body is invalid")
        public void whenUpdatingEventShouldReturnBadRequestIfRequestBodyIsInvalid() throws Exception {
            updateEventDto.setName(EventConstants.WRONG_NAME);
            updateEventDto.setShortDescription(EventConstants.WRONG_SHORT_DESCRIPTION_TOO_LONG);
            updateEventDto.setLongDescription(EventConstants.WRONG_LONG_DESCRIPTION_TOO_SHORT);
            updateEventDto.setCityExternalId(UserConstants.INVALID_CITY_NAME);
            updateEventDto.setExactAddress(EventConstants.WRONG_EXACT_ADDRESS);

            var beforeWrite = eventWriteState();

            mockMvc.perform(put(ApiConstants.EVENT_BY_ID_URL, savedEventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateEventDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_FAILED))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.errors").hasJsonPath())
                    .andExpect(jsonPath("$.errors.name").hasJsonPath())
                    .andExpect(jsonPath("$.errors.shortDescription").hasJsonPath())
                    .andExpect(jsonPath("$.errors.longDescription").hasJsonPath())
                    .andExpect(jsonPath("$.errors.cityExternalId").hasJsonPath())
                    .andExpect(jsonPath("$.errors.exactAddress").hasJsonPath());

            assertThat(eventWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When updating event should return HTTP 400 Bad Request if event metadata is invalid")
        public void whenUpdatingEventShouldReturnBadRequestIfEventMetadataIsInvalid() throws Exception {
            updateEventDto.setEventStartDate(TimeConstants.NOW.plus(1, ChronoUnit.HOURS));
            updateEventDto.setTags(Set.of(EventConstants.WRONG_TAG_NAME));

            var beforeWrite = eventWriteState();

            mockMvc.perform(put(ApiConstants.EVENT_BY_ID_URL, savedEventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateEventDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_FAILED))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.errors.eventStartDate").hasJsonPath())
                    .andExpect(jsonPath("$.errors['tags[]']").hasJsonPath());

            assertThat(eventWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When updating event exactly 48 hours ahead should return HTTP 400 Bad Request")
        public void whenUpdatingEventExactlyFortyEightHoursAheadShouldReturnBadRequest() throws Exception {
            updateEventDto.setEventStartDate(TimeConstants.TWO_DAYS_FROM_NOW);

            var beforeWrite = eventWriteState();

            mockMvc.perform(put(ApiConstants.EVENT_BY_ID_URL, savedEventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateEventDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_FAILED))
                    .andExpect(jsonPath("$.errors.eventStartDate").hasJsonPath());

            assertThat(eventWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When updating event to unlimited capacity should return HTTP 200 OK")
        public void whenUpdatingEventToUnlimitedCapacityShouldReturnOk() throws Exception {
            updateEventDto.setMaxAttendees(null);

            mockMvc.perform(put(ApiConstants.EVENT_BY_ID_URL, savedEventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateEventDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.maxAttendees").value(org.hamcrest.Matchers.nullValue()));
            assertThat(requirePresent(eventRepository.findById(savedEventId),
                    "Expected committed unlimited capacity after HTTP update").getMaxAttendees()).isNull();
        }

        @Test
        @DisplayName("When updating event should return HTTP 400 Bad Request if tags are null")
        public void whenUpdatingEventShouldReturnBadRequestIfTagsAreNull() throws Exception {
            updateEventDto.setTags(null);

            var beforeWrite = eventWriteState();

            mockMvc.perform(put(ApiConstants.EVENT_BY_ID_URL, savedEventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateEventDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_FAILED))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.errors.tags").hasJsonPath());

            assertThat(eventWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When updating event should return HTTP 200 OK with updated data on success")
        public void whenUpdatingEventShouldReturnOkWithUpdatedDataOnSuccess() throws Exception {
            Event oldEvent = requirePresent(
                    eventRepository.findById(savedEventId),
                    "Expected event to exist before update");
            oldEvent.setCreateDate(TimeConstants.ONE_WEEK_AGO);
            oldEvent.setLastUpdate(TimeConstants.ONE_HOUR_AGO);
            eventRepository.saveAndFlush(oldEvent);

            mockMvc.perform(put(ApiConstants.EVENT_BY_ID_URL, savedEventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateEventDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(savedEventId.toString()))
                    .andExpect(jsonPath("$.name").value(updateEventDto.getName()))
                    .andExpect(jsonPath("$.shortDescription").value(updateEventDto.getShortDescription()))
                    .andExpect(jsonPath("$.longDescription").value(updateEventDto.getLongDescription()))
                    .andExpect(jsonPath("$.cityExternalId").value(updateEventDto.getCityExternalId()))
                    .andExpect(jsonPath("$.exactAddress").value(EventConstants.EVENT_UPDATE_EXACT_ADDRESS))
                    .andExpect(jsonPath("$.tags", hasSize(TagConstants.EVENT_UPDATE_TAGS.size())))
                    .andExpect(jsonPath("$.tags", hasItems(TagConstants.THIRD_TAG_NAME, TagConstants.SECOND_TAG_NAME)))
                    .andExpect(jsonPath("$.eventStartDate").value(toJsonTimestamp(updateEventDto.getEventStartDate().truncatedTo(ChronoUnit.MINUTES))))
                    .andExpect(jsonPath("$.createDate").value(toJsonTimestamp(oldEvent.getCreateDate())))
                    .andExpect(jsonPath("$.lastUpdate").value(toJsonTimestamp(TimeConstants.NOW)))
                    .andExpect(jsonPath("$.timeZone").value(
                            com.mazurek.eventOrganizer.testData.TestCityData.timeZoneId(updateEventDto.getCityExternalId())));
            Event storedEvent = requirePresent(eventRepository.findById(savedEventId),
                    "Expected committed event after HTTP update");
            assertThat(storedEvent).isNotSameAs(oldEvent);
            assertThat(storedEvent).extracting(Event::getName, Event::getShortDescription,
                            Event::getLongDescription, Event::getExactAddress, Event::getEventStartDate,
                            Event::getCreateDate, Event::getLastUpdate, Event::getMaxAttendees,
                            event -> event.getCity().getExternalId(), event -> event.getOwner().getId())
                    .containsExactly(updateEventDto.getName(), updateEventDto.getShortDescription(),
                            updateEventDto.getLongDescription(), updateEventDto.getExactAddress(),
                            TimeConstants.EVENT_UPDATE_START_DATE, TimeConstants.ONE_WEEK_AGO,
                            TimeConstants.NOW, updateEventDto.getMaxAttendees(),
                            updateEventDto.getCityExternalId(), oldEvent.getOwner().getId());
            assertThat(findTagNamesByEventId(savedEventId)).isEqualTo(TagConstants.EVENT_UPDATE_TAGS);
        }

        @Test
        @DisplayName("When updating event with sub-minute precision should return HTTP 400 without writes")
        public void whenUpdatingEventWithSubMinutePrecisionShouldReturnBadRequestWithoutWrites() throws Exception {
            updateEventDto = EventCreateDtoTestBuilder.updatedEvent()
                    .eventStartDate(TimeConstants.EVENT_UPDATE_START_DATE.plusSeconds(30).plusNanos(123_456_789))
                    .build();
            var beforeWrite = eventWriteState();
            mockMvc.perform(put(ApiConstants.EVENT_BY_ID_URL, savedEventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateEventDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.errors.eventStartDate")
                            .value("Date and time must be specified to minute precision."));
            assertThat(eventWriteState())
                    .as("Sub-minute HTTP input must not change persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When updating event should not remove old tags from database after removing them from event")
        public void whenUpdatingEventShouldNotRemoveOldTagsFromDatabaseAfterRemovingThemFromEvent() throws Exception {
            mockMvc.perform(put(ApiConstants.EVENT_BY_ID_URL, savedEventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateEventDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk());

            TagConstants.DEFAULT_EVENT_TAGS.forEach(tagName ->
                    assertThat(tagRepository.findByIgnoreCaseName(tagName)).isPresent());
        }

        @Test
        @DisplayName("When updating event should not remove old city from database after changing it in event")
        public void whenUpdatingEventShouldNotRemoveOldCityFromDatabaseAfterChangingItInEvent() throws Exception {
            mockMvc.perform(put(ApiConstants.EVENT_BY_ID_URL, savedEventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateEventDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk());

            assertThat(cityRepository.findByExternalId(
                    com.mazurek.eventOrganizer.testData.TestCityData.externalId(CitiesConstants.WARSAW_NAME))).isPresent();
            assertThat(cityRepository.findByExternalId(
                    com.mazurek.eventOrganizer.testData.TestCityData.externalId(EventConstants.EVENT_UPDATE_CITY))).isPresent();
        }
    }

    // ===========================================================================================
    // GET /api/v1/events
    // ===========================================================================================

    @Nested
    @DisplayName("Get events tests: GET /api/v1/events")
    class GetEventsTests {

        private void createEventsForPagination(int amount) throws Exception {
            for (int index = 0; index < amount; index++) {
                EventCreateDto dto = EventCreateDtoTestBuilder.firstEvent()
                        .name(EventConstants.FIRST_EVENT_NAME + "-" + index)
                        .build();

                mockMvc.perform(post(ApiConstants.EVENTS_URL)
                                .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isCreated());
            }
        }

        @Test
        @DisplayName("When getting events without authentication should return the public overview page")
        public void whenGettingEventsWithoutAuthenticationShouldReturnOverviewPage() throws Exception {
            createEventsForPagination(1);
            mockMvc.perform(get(ApiConstants.EVENTS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events", hasSize(1)))
                    .andExpect(jsonPath("$.events[0].owner.id").isNotEmpty())
                    .andExpect(jsonPath("$.events[0].owner.homeCity").doesNotHaveJsonPath());
        }

        @Test
        @DisplayName("When getting events should return HTTP 200 OK with empty page if no events exist")
        public void whenGettingEventsShouldReturnOkWithEmptyPageIfNoEventsExist() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENTS_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.events", hasSize(0)))
                    .andExpect(jsonPath("$.pageNumber").value(PaginationConstants.PAGE_ZERO))
                    .andExpect(jsonPath("$.pageSize").value(PaginationConstants.EVENT_PAGE_SIZE))
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.totalPages").value(0))
                    .andExpect(jsonPath("$.lastPage").value(true));
        }

        @Test
        @DisplayName("When getting events should return HTTP 200 OK with event overview page")
        public void whenGettingEventsShouldReturnOkWithEventOverviewPage() throws Exception {
            UUID firstEventId = testDataInitializer.setupFirstEvent();
            EventCreateDto secondEvent = EventCreateDtoTestBuilder.secondEvent()
                    .longDescription(EventConstants.FIRST_EVENT_LONG_DESC)
                    .eventStartDate(TimeConstants.ONE_WEEK_FROM_NOW.plus(1, ChronoUnit.DAYS))
                    .cityExternalId("test:new york").build();
            MvcResult created = mockMvc.perform(post(ApiConstants.EVENTS_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(secondEvent)))
                    .andExpect(status().isCreated()).andReturn();
            UUID secondEventId = objectMapper.readValue(created.getResponse().getContentAsString(), EventDto.class).getId();
            Event firstStoredEvent = requirePresent(eventRepository.findById(firstEventId),
                    "Expected first event to exist after setup");
            Event secondStoredEvent = requirePresent(eventRepository.findById(secondEventId),
                    "Expected second event to exist after setup");
            assertThat(secondStoredEvent.getCity().getTimeZoneId())
                    .isNotEqualTo(firstStoredEvent.getCity().getTimeZoneId());

            mockMvc.perform(get(ApiConstants.EVENTS_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.events", hasSize(2)))
                    .andExpect(jsonPath("$.events[*].id", contains(secondEventId.toString(), firstEventId.toString())))
                    .andExpect(jsonPath("$.events[*].name", hasItems(EventConstants.FIRST_EVENT_NAME, EventConstants.SECOND_EVENT_NAME)))
                    .andExpect(jsonPath("$.events[*].shortDescription", hasItems(EventConstants.FIRST_EVENT_SHORT_DESC, EventConstants.SECOND_EVENT_SHORT_DESC)))
                    .andExpect(jsonPath("$.events[?(@.id == '" + firstEventId + "')].timeZone",
                            contains(firstStoredEvent.getCity().getTimeZoneId())))
                    .andExpect(jsonPath("$.events[?(@.id == '" + secondEventId + "')].timeZone",
                            contains(secondStoredEvent.getCity().getTimeZoneId())))
                    .andExpect(jsonPath("$.events[*].attendeeCount", everyItem(is(0))))
                    .andExpect(jsonPath("$.pageNumber").value(PaginationConstants.PAGE_ZERO))
                    .andExpect(jsonPath("$.pageSize").value(PaginationConstants.EVENT_PAGE_SIZE))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.lastPage").value(true));
        }

        @Test
        @DisplayName("When getting events should return HTTP 200 OK with correct page when page parameter is provided")
        public void whenGettingEventsShouldReturnOkWithCorrectPageWhenPageParameterIsProvided() throws Exception {
            testDataInitializer.setupFirstEvent();
            testDataInitializer.setupEventBySecondUser();

            mockMvc.perform(get(ApiConstants.EVENTS_URL)
                            .param("page", "0")
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events", hasSize(2)))
                    .andExpect(jsonPath("$.pageNumber").value(PaginationConstants.PAGE_ZERO));
        }

        @Test
        @DisplayName("When getting events should return HTTP 400 Bad Request if page parameter is negative")
        public void whenGettingEventsShouldReturnBadRequestIfPageParameterIsNegative() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENTS_URL)
                            .param("page", String.valueOf(PaginationConstants.PAGE_MINUS_ONE))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.INVALID_PAGE_NUMBER))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()));
        }

        @Test
        @DisplayName("When getting events should respect page number and page size")
        public void whenGettingEventsShouldRespectPageNumberAndPageSize() throws Exception {
            createEventsForPagination(PaginationConstants.EVENT_PAGE_SIZE + 1);
            assertThat(eventRepository.findAll()).extracting(Event::getEventStartDate)
                    .containsOnly(TimeConstants.ONE_WEEK_FROM_NOW);
            // Primary sort keys are equal; PostgreSQL supplies an independent UUID order oracle.
            List<String> expectedIds = jdbcTemplate.queryForList("SELECT id FROM events ORDER BY id DESC", UUID.class)
                    .stream().map(UUID::toString).toList();
            assertThat(expectedIds).hasSize(PaginationConstants.EVENT_PAGE_SIZE + 1).doesNotHaveDuplicates();

            mockMvc.perform(get(ApiConstants.EVENTS_URL)
                            .param("page", String.valueOf(PaginationConstants.PAGE_ZERO))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events", hasSize(PaginationConstants.EVENT_PAGE_SIZE)))
                    .andExpect(jsonPath("$.events[*].id", contains(expectedIds.subList(0, PaginationConstants.EVENT_PAGE_SIZE).toArray())))
                    .andExpect(jsonPath("$.pageNumber").value(PaginationConstants.PAGE_ZERO))
                    .andExpect(jsonPath("$.pageSize").value(PaginationConstants.EVENT_PAGE_SIZE))
                    .andExpect(jsonPath("$.totalElements").value(PaginationConstants.EVENT_PAGE_SIZE + 1))
                    .andExpect(jsonPath("$.totalPages").value(2))
                    .andExpect(jsonPath("$.lastPage").value(false));

            mockMvc.perform(get(ApiConstants.EVENTS_URL)
                            .param("page", String.valueOf(PaginationConstants.PAGE_ONE))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events", hasSize(1)))
                    .andExpect(jsonPath("$.events[0].id").value(expectedIds.get(PaginationConstants.EVENT_PAGE_SIZE)))
                    .andExpect(jsonPath("$.pageNumber").value(PaginationConstants.PAGE_ONE))
                    .andExpect(jsonPath("$.pageSize").value(PaginationConstants.EVENT_PAGE_SIZE))
                    .andExpect(jsonPath("$.totalElements").value(PaginationConstants.EVENT_PAGE_SIZE + 1))
                    .andExpect(jsonPath("$.totalPages").value(2))
                    .andExpect(jsonPath("$.lastPage").value(true));
        }
    }

    // ===========================================================================================
    // GET /api/v1/events/{eventId}/attendees
    // ===========================================================================================

    @Nested
    @DisplayName("Get event attendees tests: GET /api/v1/events/{eventId}/attendees")
    class GetEventAttendeesTests {

        private UUID savedEventId;

        @BeforeEach
        void setUp() {
            savedEventId = testDataInitializer.setupFirstEvent();
        }

        @Test
        @DisplayName("When getting attendees should return HTTP 401 Unauthorized without authentication")
        void whenGettingAttendeesShouldReturnUnauthorizedWithoutAuthentication() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_ATTENDEES_URL, savedEventId))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));
        }

        @Test
        @DisplayName("When getting attendees should return HTTP 404 Not Found for missing event")
        void whenGettingAttendeesShouldReturnNotFoundForMissingEvent() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_ATTENDEES_URL, EventConstants.NOT_EXISTING_EVENT_ID)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.EVENT_NOT_FOUND))
                    .andExpect(jsonPath("$.message").value(EventNotFoundException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When getting attendees should allow owner and return an empty page")
        void whenGettingAttendeesShouldAllowOwner() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_ATTENDEES_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.attendees", hasSize(0)))
                    .andExpect(jsonPath("$.pageNumber").value(PaginationConstants.PAGE_ZERO))
                    .andExpect(jsonPath("$.pageSize").value(PaginationConstants.DEFAULT_PAGE_SIZE))
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.totalPages").value(0))
                    .andExpect(jsonPath("$.lastPage").value(true));
        }

        @Test
        @DisplayName("When getting attendees should reject an authenticated outsider")
        void whenGettingAttendeesShouldRejectOutsider() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_ATTENDEES_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.NOT_EVENT_ATTENDEE))
                    .andExpect(jsonPath("$.message").value(NotEventAttendeeException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When getting attendees should allow attendee and exclude the owner")
        void whenGettingAttendeesShouldAllowAttendeeAndExcludeOwner() throws Exception {
            testDataInitializer.addSecondUserToAttendees(savedEventId);
            User owner = requirePresent(
                    userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL),
                    "Expected event owner to exist"
            );
            User attendee = requirePresent(
                    userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL),
                    "Expected attendee to exist"
            );

            mockMvc.perform(get(ApiConstants.EVENT_ATTENDEES_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.attendees", hasSize(1)))
                    .andExpect(jsonPath("$.attendees[0].id").value(attendee.getId().toString()))
                    .andExpect(jsonPath("$.attendees[0].homeCity").doesNotHaveJsonPath())
                    .andExpect(jsonPath("$.attendees[*].id", not(hasItem(owner.getId().toString()))))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("When getting attendees should return HTTP 400 Bad Request for negative page")
        void whenGettingAttendeesShouldRejectNegativePage() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_ATTENDEES_URL, savedEventId)
                            .param("page", String.valueOf(PaginationConstants.PAGE_MINUS_ONE))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.INVALID_PAGE_NUMBER));
        }
    }

    // ===========================================================================================
    // POST /api/v1/events/{eventId}/attend
    // ===========================================================================================

    @Nested
    @DisplayName("Attend event tests: POST /api/v1/events/{eventId}/attend")
    class AttendEventTests {

        private UUID savedEventId;

        @BeforeEach
        void setUp() {
            savedEventId = testDataInitializer.setupFirstEvent();
        }

        @Test
        @DisplayName("When attending event should return HTTP 401 Unauthorized if there is no Authorization header")
        public void whenAttendingEventShouldReturnUnauthorizedIfThereIsNoAuthorizationHeader() throws Exception {
            var beforeWrite = eventWriteState();

            mockMvc.perform(post(ApiConstants.EVENT_ATTEND_URL, savedEventId))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));

            assertThat(eventWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When attending event should return HTTP 404 Not Found if event does not exist")
        public void whenAttendingEventShouldReturnNotFoundIfEventDoesNotExist() throws Exception {
            var beforeWrite = eventWriteState();

            mockMvc.perform(post(ApiConstants.EVENT_ATTEND_URL, EventConstants.NOT_EXISTING_EVENT_ID)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.EVENT_NOT_FOUND))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(EventNotFoundException.DEFAULT_MESSAGE));

            assertThat(eventWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When attending event should return HTTP 409 Conflict if event already had place")
        public void whenAttendingEventShouldReturnConflictIfEventAlreadyHadPlace() throws Exception {
            Event savedEvent = requirePresent(
                    eventRepository.findById(savedEventId),
                    "Expected event to exist before attendance update");
            savedEvent.setEventStartDate(TimeConstants.ONE_WEEK_AGO);
            eventRepository.save(savedEvent);

            var beforeWrite = eventWriteState();

            mockMvc.perform(post(ApiConstants.EVENT_ATTEND_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.EVENT_ALREADY_STARTED))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                    .andExpect(jsonPath("$.message").value(EventAlreadyHadPlaceException.DEFAULT_MESSAGE));

            assertThat(eventWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When attending event should return HTTP 409 Conflict if event owner performs attend action")
        public void whenAttendingEventShouldReturnConflictIfEventOwnerPerformsAttendAction() throws Exception {
            var beforeWrite = eventWriteState();

            mockMvc.perform(post(ApiConstants.EVENT_ATTEND_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.EVENT_OWNER_CANNOT_ATTEND))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                    .andExpect(jsonPath("$.message").value(EventOwnerAlreadyAttendsEventException.DEFAULT_MESSAGE));

            assertThat(eventWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When attending event should return HTTP 409 Conflict if user is already attending event")
        public void whenAttendingEventShouldReturnConflictIfUserIsAlreadyAttendingEvent() throws Exception {
            addSecondUserAsAttendee(savedEventId);

            var beforeWrite = eventWriteState();

            mockMvc.perform(post(ApiConstants.EVENT_ATTEND_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.ALREADY_ATTENDING_EVENT))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                    .andExpect(jsonPath("$.message").value(AlreadyAttendingEventException.DEFAULT_MESSAGE));

            assertThat(eventWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When attending event should return HTTP 204 No Content and persist attending relationship")
        public void whenAttendingEventShouldReturnNoContentAndPersistAttendingRelationship() throws Exception {
            mockMvc.perform(post(ApiConstants.EVENT_ATTEND_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            User attendee = requirePresent(
                    userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL),
                    "Expected second user to exist");

            assertThat(eventHasAttendee(savedEventId, attendee.getId())).isTrue();
            assertThat(requirePresent(eventRepository.findById(savedEventId), "Expected committed attendance")
                    .getAttendeeCount()).isEqualTo(1);
        }
    }

    // ===========================================================================================
    // DELETE /api/v1/events/{eventId}/attend
    // ===========================================================================================

    @Nested
    @DisplayName("Leave event tests: DELETE /api/v1/events/{eventId}/attend")
    class LeaveEventTests {

        private UUID savedEventId;

        @BeforeEach
        void setUp() {
            savedEventId = testDataInitializer.setupFirstEvent();
        }

        @Test
        @DisplayName("When leaving event should return HTTP 401 Unauthorized if there is no Authorization header")
        public void whenLeavingEventShouldReturnUnauthorizedIfThereIsNoAuthorizationHeader() throws Exception {
            var beforeWrite = eventWriteState();

            mockMvc.perform(delete(ApiConstants.EVENT_ATTEND_URL, savedEventId))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));

            assertThat(eventWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When leaving event should return HTTP 409 Conflict if event owner tries to leave event")
        public void whenLeavingEventShouldReturnConflictIfEventOwnerTriesToLeaveEvent() throws Exception {
            var beforeWrite = eventWriteState();

            mockMvc.perform(delete(ApiConstants.EVENT_ATTEND_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.EVENT_OWNER_CANNOT_LEAVE))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                    .andExpect(jsonPath("$.message").value(EventOwnerMustAttendEventException.DEFAULT_MESSAGE));

            assertThat(eventWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When leaving event should return HTTP 404 Not Found if event does not exist")
        public void whenLeavingEventShouldReturnNotFoundIfEventDoesNotExist() throws Exception {
            var beforeWrite = eventWriteState();

            mockMvc.perform(delete(ApiConstants.EVENT_ATTEND_URL, EventConstants.NOT_EXISTING_EVENT_ID)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.EVENT_NOT_FOUND))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(EventNotFoundException.DEFAULT_MESSAGE));

            assertThat(eventWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When leaving event should return HTTP 409 Conflict if event already had place")
        public void whenLeavingEventShouldReturnConflictIfEventAlreadyHadPlace() throws Exception {
            Event savedEvent = requirePresent(
                    eventRepository.findById(savedEventId),
                    "Expected event to exist before update");
            savedEvent.setEventStartDate(TimeConstants.ONE_WEEK_AGO);
            eventRepository.save(savedEvent);

            var beforeWrite = eventWriteState();

            mockMvc.perform(delete(ApiConstants.EVENT_ATTEND_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.EVENT_ALREADY_STARTED))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                    .andExpect(jsonPath("$.message").value(EventAlreadyHadPlaceException.DEFAULT_MESSAGE));

            assertThat(eventWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When leaving event should return HTTP 403 Forbidden if user is not attending event")
        public void whenLeavingEventShouldReturnForbiddenIfUserIsNotAttendingEvent() throws Exception {
            var beforeWrite = eventWriteState();

            mockMvc.perform(delete(ApiConstants.EVENT_ATTEND_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.NOT_EVENT_ATTENDEE))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.message").value(NotEventAttendeeException.DEFAULT_MESSAGE));

            assertThat(eventWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When leaving event should return HTTP 204 No Content and remove attending relationship")
        public void whenLeavingEventShouldReturnNoContentAndRemoveAttendingRelationship() throws Exception {
            addSecondUserAsAttendee(savedEventId);

            mockMvc.perform(delete(ApiConstants.EVENT_ATTEND_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            User updatedUser = requirePresent(
                    userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL),
                    "Expected updated second user to exist");

            assertThat(eventHasAttendee(savedEventId, updatedUser.getId())).isFalse();
            assertThat(requirePresent(eventRepository.findById(savedEventId), "Expected committed departure")
                    .getAttendeeCount()).isZero();
        }
    }

    // Independent committed reads: no managed entity snapshot or test-level transaction.
    private Map<String, List<Map<String, Object>>> eventWriteState() {
        return Map.of(
                "events", jdbcTemplate.queryForList("SELECT * FROM events ORDER BY id"),
                "attendees", jdbcTemplate.queryForList("SELECT * FROM event_user ORDER BY event_id, user_id"),
                "eventTags", jdbcTemplate.queryForList("SELECT * FROM event_tag ORDER BY event_id, tag_id"),
                "tags", jdbcTemplate.queryForList("SELECT * FROM tags ORDER BY id"),
                "cities", jdbcTemplate.queryForList("SELECT * FROM cities ORDER BY id"),
                "notifications", jdbcTemplate.queryForList("SELECT * FROM notifications ORDER BY id"),
                "deliveries", jdbcTemplate.queryForList("SELECT * FROM notification_deliveries ORDER BY id")
        );
    }
}
