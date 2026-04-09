package com.mazurek.eventOrganizer.event;

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
import com.mazurek.eventOrganizer.exception.event.NotEventAttenderException;
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
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
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

    private String firstUserJwt;
    private String secondUserJwt;
    private EventCreateDto eventCreateDto;

    private final AuthenticationRequest firstUserAuthRequest =
            AuthenticationRequestTestBuilder.authenticationRequestForFirstUser().build();
    private final AuthenticationRequest secondUserAuthRequest =
            AuthenticationRequestTestBuilder.authenticationRequestForSecondUser().build();

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();

        firstUserJwt = AuthConstants.JWT_PREFIX + authenticationService.authenticate(firstUserAuthRequest, DeviceType.WEB).getAccessToken();
        secondUserJwt = AuthConstants.JWT_PREFIX + authenticationService.authenticate(secondUserAuthRequest, DeviceType.WEB).getAccessToken();

        eventCreateDto = EventCreateDtoTestBuilder.firstEvent().build();
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) return value;
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    private String toJsonTimestamp(Instant instant) {
        try {
            return objectMapper.writeValueAsString(instant).replaceAll("\"", "");
        } catch (Exception e) {
            throw new AssertionError("Failed to serialize Instant to JSON timestamp", e);
        }
    }

    private Set<String> findTagNamesByEventId(UUID eventId) {
        return new HashSet<>(jdbcTemplate.queryForList("""
                select t.name
                from tags t
                join event_tag et on t.id = et.tag_id
                where et.event_id = ?
                """, String.class, eventId));
    }

    private boolean eventHasAttender(UUID eventId, UUID userId) {
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
    @Transactional
    class CreateEventTests {

        @Test
        @DisplayName("When creating event should return HTTP 403 Forbidden if there is no Authorization header")
        public void whenCreatingEventShouldReturnForbiddenIfThereIsNoAuthorizationHeader() throws Exception {
            mockMvc.perform(post(ApiConstants.EVENTS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(eventCreateDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When creating event should return HTTP 400 Bad Request on every data validation error")
        public void whenCreatingEventShouldReturnBadRequestOnEveryDataValidationError() throws Exception {
            eventCreateDto.setName(EventConstants.WRONG_NAME);
            eventCreateDto.setShortDescription(EventConstants.WRONG_SHORT_DESCRIPTION_TOO_SHORT);
            eventCreateDto.setLongDescription(EventConstants.WRONG_LONG_DESCRIPTION_TOO_SHORT);
            eventCreateDto.setCity(UserConstants.INVALID_CITY_NAME);
            eventCreateDto.setExactAddress(EventConstants.WRONG_EXACT_ADDRESS);
            eventCreateDto.setEventStartDate(null);

            mockMvc.perform(post(ApiConstants.EVENTS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(eventCreateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.errors").isMap())
                    .andExpect(jsonPath("$.errors.name").hasJsonPath())
                    .andExpect(jsonPath("$.errors.shortDescription").hasJsonPath())
                    .andExpect(jsonPath("$.errors.longDescription").hasJsonPath())
                    .andExpect(jsonPath("$.errors.city").hasJsonPath())
                    .andExpect(jsonPath("$.errors.exactAddress").hasJsonPath())
                    .andExpect(jsonPath("$.errors.eventStartDate").hasJsonPath())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("When creating event should return HTTP 400 Bad Request if event metadata is invalid")
        public void whenCreatingEventShouldReturnBadRequestIfEventMetadataIsInvalid() throws Exception {
            eventCreateDto.setEventStartDate(TimeConstants.NOW.plus(1, ChronoUnit.HOURS));
            eventCreateDto.setTags(Set.of(EventConstants.WRONG_TAG_NAME));
            eventCreateDto.setTimeZone(InvalidInputConstants.INVALID_TIME_ZONE);

            mockMvc.perform(post(ApiConstants.EVENTS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(eventCreateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.errors.eventStartDate").hasJsonPath())
                    .andExpect(jsonPath("$.errors['tags[]']").hasJsonPath())
                    .andExpect(jsonPath("$.errors.timeZone").hasJsonPath());
        }

        @Test
        @DisplayName("When creating event should return HTTP 400 Bad Request if tags are null")
        public void whenCreatingEventShouldReturnBadRequestIfTagsAreNull() throws Exception {
            eventCreateDto.setTags(null);

            mockMvc.perform(post(ApiConstants.EVENTS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(eventCreateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.errors.tags").hasJsonPath());
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
            Instant expectedStartDate = eventCreateDto.getEventStartDate().truncatedTo(ChronoUnit.MINUTES);

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
                    .andExpect(jsonPath("$.city").value(capitalize(eventCreateDto.getCity())))
                    .andExpect(jsonPath("$.exactAddress").value(eventCreateDto.getExactAddress()))
                    .andExpect(jsonPath("$.timeZone").value(eventCreateDto.getTimeZone()))
                    .andExpect(jsonPath("$.tags", hasSize(eventCreateDto.getTags().size())))
                    .andExpect(jsonPath("$.tags", hasItems(TagConstants.FIRST_TAG_NAME, TagConstants.SECOND_TAG_NAME)))
                    .andExpect(jsonPath("$.eventStartDate").value(toJsonTimestamp(expectedStartDate)))
                    .andExpect(jsonPath("$.createDate").isNotEmpty())
                    .andExpect(jsonPath("$.lastUpdate").isNotEmpty())
                    .andExpect(jsonPath("$.owner.id").hasJsonPath());
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
            Event savedEvent = requirePresent(
                    eventRepository.findById(createdEvent.getId()),
                    "Expected created event to be persisted");
            User owner = requirePresent(
                    userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL),
                    "Expected first user to exist after auth setup");

            assertThat(savedEvent.getOwner().getId(), equalTo(owner.getId()));
            assertThat(savedEvent.getCity().getName(), equalTo(eventCreateDto.getCity().toLowerCase()));
            assertThat(savedEvent.getTimeZoneId(), equalTo(eventCreateDto.getTimeZone()));
            eventRepository.flush();
            assertThat(findTagNamesByEventId(createdEvent.getId()), equalTo(eventCreateDto.getTags()));
        }
    }

    // ===========================================================================================
    // GET /api/v1/events/{eventId}
    // ===========================================================================================

    @Nested
    @DisplayName("Get event by id tests: GET /api/v1/events/{eventId}")
    @Transactional
    class GetEventByIdTests {

        private UUID savedEventId;

        @BeforeEach
        void setUp() {
            savedEventId = testDataInitializer.setupFirstEvent();
        }

        @Test
        @DisplayName("When getting event by id should return HTTP 403 Forbidden if there is no Authorization header")
        public void whenGettingEventByIdShouldReturnForbiddenIfThereIsNoAuthorizationHeader() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_BY_ID_URL, savedEventId))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When getting event by id should return HTTP 404 Not Found if event does not exist")
        public void whenGettingEventByIdShouldReturnNotFoundIfEventDoesNotExist() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_BY_ID_URL, EventConstants.NOT_EXISTING_EVENT_ID)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isNotFound())
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
            Instant expectedStartDate = TimeConstants.ONE_WEEK_FROM_NOW.truncatedTo(ChronoUnit.MINUTES);

            mockMvc.perform(get(ApiConstants.EVENT_BY_ID_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(savedEventId.toString()))
                    .andExpect(jsonPath("$.name").value(EventConstants.FIRST_EVENT_NAME))
                    .andExpect(jsonPath("$.shortDescription").value(EventConstants.FIRST_EVENT_SHORT_DESC))
                    .andExpect(jsonPath("$.longDescription").value(EventConstants.FIRST_EVENT_LONG_DESC))
                    .andExpect(jsonPath("$.city").value(capitalize(CitiesConstants.WARSAW_NAME)))
                    .andExpect(jsonPath("$.exactAddress").value(EventConstants.FIRST_EVENT_ADDRESS))
                    .andExpect(jsonPath("$.timeZone").value(UserConstants.FIRST_USER_TIMEZONE))
                    .andExpect(jsonPath("$.tags", hasItems(TagConstants.FIRST_TAG_NAME, TagConstants.SECOND_TAG_NAME)))
                    .andExpect(jsonPath("$.owner.id").value(owner.getId().toString()))
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
    @Transactional
    class UpdateEventTests {

        private UUID savedEventId;
        private EventCreateDto updateEventDto;

        @BeforeEach
        void setUp() {
            savedEventId = testDataInitializer.setupFirstEvent();
            updateEventDto = EventCreateDtoTestBuilder.updatedEvent().build();
        }

        @Test
        @DisplayName("When updating event should return HTTP 403 Forbidden if there is no Authorization header")
        public void whenUpdatingEventShouldReturnForbiddenIfThereIsNoAuthorizationHeader() throws Exception {
            mockMvc.perform(put(ApiConstants.EVENT_BY_ID_URL, savedEventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateEventDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When updating event should return HTTP 404 Not Found if event does not exist")
        public void whenUpdatingEventShouldReturnNotFoundIfEventDoesNotExist() throws Exception {
            mockMvc.perform(put(ApiConstants.EVENT_BY_ID_URL, EventConstants.NOT_EXISTING_EVENT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateEventDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(EventNotFoundException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When updating event should return HTTP 409 Conflict if event had place")
        public void whenUpdatingEventShouldReturnConflictIfEventHadPlace() throws Exception {
            Event savedEvent = requirePresent(
                    eventRepository.findById(savedEventId),
                    "Expected event to exist after creation");
            savedEvent.setEventStartDate(TimeConstants.ONE_WEEK_AGO);
            eventRepository.save(savedEvent);

            mockMvc.perform(put(ApiConstants.EVENT_BY_ID_URL, savedEventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateEventDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                    .andExpect(jsonPath("$.message").value(EventAlreadyHadPlaceException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When updating event should return HTTP 403 Forbidden if performing user does not own event")
        public void whenUpdatingEventShouldReturnForbiddenIfPerformingUserDoesNotOwnEvent() throws Exception {
            mockMvc.perform(put(ApiConstants.EVENT_BY_ID_URL, savedEventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateEventDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.message").value(NotEventOwnerException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When updating event should return HTTP 400 Bad Request if request body is invalid")
        public void whenUpdatingEventShouldReturnBadRequestIfRequestBodyIsInvalid() throws Exception {
            updateEventDto.setName(EventConstants.WRONG_NAME);
            updateEventDto.setShortDescription(EventConstants.WRONG_SHORT_DESCRIPTION_TOO_LONG);
            updateEventDto.setLongDescription(EventConstants.WRONG_LONG_DESCRIPTION_TOO_SHORT);
            updateEventDto.setCity(UserConstants.INVALID_CITY_NAME);
            updateEventDto.setExactAddress(EventConstants.WRONG_EXACT_ADDRESS);

            mockMvc.perform(put(ApiConstants.EVENT_BY_ID_URL, savedEventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateEventDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.errors").hasJsonPath())
                    .andExpect(jsonPath("$.errors.name").hasJsonPath())
                    .andExpect(jsonPath("$.errors.shortDescription").hasJsonPath())
                    .andExpect(jsonPath("$.errors.longDescription").hasJsonPath())
                    .andExpect(jsonPath("$.errors.city").hasJsonPath())
                    .andExpect(jsonPath("$.errors.exactAddress").hasJsonPath());
        }

        @Test
        @DisplayName("When updating event should return HTTP 400 Bad Request if event metadata is invalid")
        public void whenUpdatingEventShouldReturnBadRequestIfEventMetadataIsInvalid() throws Exception {
            updateEventDto.setEventStartDate(TimeConstants.NOW.plus(1, ChronoUnit.HOURS));
            updateEventDto.setTags(Set.of(EventConstants.WRONG_TAG_NAME));
            updateEventDto.setTimeZone(InvalidInputConstants.INVALID_TIME_ZONE);

            mockMvc.perform(put(ApiConstants.EVENT_BY_ID_URL, savedEventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateEventDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.errors.eventStartDate").hasJsonPath())
                    .andExpect(jsonPath("$.errors['tags[]']").hasJsonPath())
                    .andExpect(jsonPath("$.errors.timeZone").hasJsonPath());
        }

        @Test
        @DisplayName("When updating event should return HTTP 400 Bad Request if tags are null")
        public void whenUpdatingEventShouldReturnBadRequestIfTagsAreNull() throws Exception {
            updateEventDto.setTags(null);

            mockMvc.perform(put(ApiConstants.EVENT_BY_ID_URL, savedEventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateEventDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.errors.tags").hasJsonPath());
        }

        @Test
        @DisplayName("When updating event should return HTTP 200 OK with updated data on success")
        public void whenUpdatingEventShouldReturnOkWithUpdatedDataOnSuccess() throws Exception {
            Event oldEvent = requirePresent(
                    eventRepository.findById(savedEventId),
                    "Expected event to exist before update");

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
                    .andExpect(jsonPath("$.city").value(capitalize(updateEventDto.getCity())))
                    .andExpect(jsonPath("$.exactAddress").value(EventConstants.EVENT_UPDATE_EXACT_ADDRESS))
                    .andExpect(jsonPath("$.tags", hasSize(TagConstants.EVENT_UPDATE_TAGS.size())))
                    .andExpect(jsonPath("$.tags", hasItems(TagConstants.THIRD_TAG_NAME, TagConstants.SECOND_TAG_NAME)))
                    .andExpect(jsonPath("$.eventStartDate").value(toJsonTimestamp(updateEventDto.getEventStartDate().truncatedTo(ChronoUnit.MINUTES))))
                    .andExpect(jsonPath("$.createDate").value(toJsonTimestamp(oldEvent.getCreateDate())))
                    .andExpect(jsonPath("$.lastUpdate").hasJsonPath());
        }

        @Test
        @DisplayName("When updating event should truncate event start date to minutes")
        public void whenUpdatingEventShouldTruncateEventStartDateToMinutes() throws Exception {
            mockMvc.perform(put(ApiConstants.EVENT_BY_ID_URL, savedEventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateEventDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventStartDate")
                            .value(toJsonTimestamp(updateEventDto.getEventStartDate().truncatedTo(ChronoUnit.MINUTES))));
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
                    assertThat(tagRepository.findByIgnoreCaseName(tagName).isPresent(), equalTo(true)));
        }

        @Test
        @DisplayName("When updating event should not remove old city from database after changing it in event")
        public void whenUpdatingEventShouldNotRemoveOldCityFromDatabaseAfterChangingItInEvent() throws Exception {
            mockMvc.perform(put(ApiConstants.EVENT_BY_ID_URL, savedEventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateEventDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk());

            assertThat(cityRepository.findByIgnoreCaseName(CitiesConstants.WARSAW_NAME).isPresent(), equalTo(true));
            assertThat(cityRepository.findByIgnoreCaseName(EventConstants.EVENT_UPDATE_CITY).isPresent(), equalTo(true));
        }
    }

    // ===========================================================================================
    // GET /api/v1/events
    // ===========================================================================================

    @Nested
    @DisplayName("Get events tests: GET /api/v1/events")
    @Transactional
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
        @DisplayName("When getting events should return HTTP 403 Forbidden if there is no Authorization header")
        public void whenGettingEventsShouldReturnForbiddenIfThereIsNoAuthorizationHeader() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENTS_URL))
                    .andExpect(status().isForbidden());
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
            UUID secondEventId = testDataInitializer.setupEventBySecondUser();

            mockMvc.perform(get(ApiConstants.EVENTS_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.events", hasSize(2)))
                    .andExpect(jsonPath("$.events[*].id", hasItems(firstEventId.toString(), secondEventId.toString())))
                    .andExpect(jsonPath("$.events[*].name", hasItems(EventConstants.FIRST_EVENT_NAME, EventConstants.SECOND_EVENT_NAME)))
                    .andExpect(jsonPath("$.events[*].shortDescription", hasItems(EventConstants.FIRST_EVENT_SHORT_DESC, EventConstants.SECOND_EVENT_SHORT_DESC)))
                    .andExpect(jsonPath("$.events[*].amountOfAttenders", everyItem(is(0))))
                    .andExpect(jsonPath("$.pageNumber").value(PaginationConstants.PAGE_ZERO))
                    .andExpect(jsonPath("$.pageSize").value(PaginationConstants.EVENT_PAGE_SIZE))
                    .andExpect(jsonPath("$.totalElements").value(2))
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
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()));
        }

        @Test
        @DisplayName("When getting events should respect page number and page size")
        public void whenGettingEventsShouldRespectPageNumberAndPageSize() throws Exception {
            createEventsForPagination(PaginationConstants.EVENT_PAGE_SIZE + 1);

            mockMvc.perform(get(ApiConstants.EVENTS_URL)
                            .param("page", String.valueOf(PaginationConstants.PAGE_ZERO))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events", hasSize(PaginationConstants.EVENT_PAGE_SIZE)))
                    .andExpect(jsonPath("$.pageNumber").value(PaginationConstants.PAGE_ZERO))
                    .andExpect(jsonPath("$.pageSize").value(PaginationConstants.EVENT_PAGE_SIZE))
                    .andExpect(jsonPath("$.totalElements").value(PaginationConstants.EVENT_PAGE_SIZE + 1))
                    .andExpect(jsonPath("$.lastPage").value(false));

            mockMvc.perform(get(ApiConstants.EVENTS_URL)
                            .param("page", String.valueOf(PaginationConstants.PAGE_ONE))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events", hasSize(1)))
                    .andExpect(jsonPath("$.pageNumber").value(PaginationConstants.PAGE_ONE))
                    .andExpect(jsonPath("$.pageSize").value(PaginationConstants.EVENT_PAGE_SIZE))
                    .andExpect(jsonPath("$.totalElements").value(PaginationConstants.EVENT_PAGE_SIZE + 1))
                    .andExpect(jsonPath("$.lastPage").value(true));
        }
    }

    // ===========================================================================================
    // POST /api/v1/events/{eventId}/attend
    // ===========================================================================================

    @Nested
    @DisplayName("Attend event tests: POST /api/v1/events/{eventId}/attend")
    @Transactional
    class AttendEventTests {

        private UUID savedEventId;

        @BeforeEach
        void setUp() {
            savedEventId = testDataInitializer.setupFirstEvent();
        }

        @Test
        @DisplayName("When attending event should return HTTP 403 Forbidden if there is no Authorization header")
        public void whenAttendingEventShouldReturnForbiddenIfThereIsNoAuthorizationHeader() throws Exception {
            mockMvc.perform(post(ApiConstants.EVENT_ATTEND_URL, savedEventId))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When attending event should return HTTP 404 Not Found if event does not exist")
        public void whenAttendingEventShouldReturnNotFoundIfEventDoesNotExist() throws Exception {
            mockMvc.perform(post(ApiConstants.EVENT_ATTEND_URL, EventConstants.NOT_EXISTING_EVENT_ID)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(EventNotFoundException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When attending event should return HTTP 409 Conflict if event already had place")
        public void whenAttendingEventShouldReturnConflictIfEventAlreadyHadPlace() throws Exception {
            Event savedEvent = requirePresent(
                    eventRepository.findById(savedEventId),
                    "Expected event to exist before attendance update");
            savedEvent.setEventStartDate(TimeConstants.ONE_WEEK_AGO);
            eventRepository.save(savedEvent);

            mockMvc.perform(post(ApiConstants.EVENT_ATTEND_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                    .andExpect(jsonPath("$.message").value(EventAlreadyHadPlaceException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When attending event should return HTTP 409 Conflict if event owner performs attend action")
        public void whenAttendingEventShouldReturnConflictIfEventOwnerPerformsAttendAction() throws Exception {
            mockMvc.perform(post(ApiConstants.EVENT_ATTEND_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                    .andExpect(jsonPath("$.message").value(EventOwnerAlreadyAttendsEventException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When attending event should return HTTP 409 Conflict if user is already attending event")
        public void whenAttendingEventShouldReturnConflictIfUserIsAlreadyAttendingEvent() throws Exception {
            Event event = requirePresent(
                    eventRepository.findById(savedEventId),
                    "Expected event to exist");
            User attender = requirePresent(
                    userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL),
                    "Expected second user to exist after auth setup");
            event.addAttendingUser(attender);
            eventRepository.save(event);

            mockMvc.perform(post(ApiConstants.EVENT_ATTEND_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                    .andExpect(jsonPath("$.message").value(AlreadyAttendingEventException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When attending event should return HTTP 200 OK and persist attending relationship")
        public void whenAttendingEventShouldReturnOkAndPersistAttendingRelationship() throws Exception {
            mockMvc.perform(post(ApiConstants.EVENT_ATTEND_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isOk());

            eventRepository.flush();
            User attender = requirePresent(
                    userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL),
                    "Expected second user to exist");

            assertThat(eventHasAttender(savedEventId, attender.getId()), equalTo(true));
        }
    }

    // ===========================================================================================
    // DELETE /api/v1/events/{eventId}/attend
    // ===========================================================================================

    @Nested
    @DisplayName("Leave event tests: DELETE /api/v1/events/{eventId}/attend")
    @Transactional
    class LeaveEventTests {

        private UUID savedEventId;

        @BeforeEach
        void setUp() {
            savedEventId = testDataInitializer.setupFirstEvent();
        }

        @Test
        @DisplayName("When leaving event should return HTTP 403 Forbidden if there is no Authorization header")
        public void whenLeavingEventShouldReturnForbiddenIfThereIsNoAuthorizationHeader() throws Exception {
            mockMvc.perform(delete(ApiConstants.EVENT_ATTEND_URL, savedEventId))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When leaving event should return HTTP 409 Conflict if event owner tries to leave event")
        public void whenLeavingEventShouldReturnConflictIfEventOwnerTriesToLeaveEvent() throws Exception {
            mockMvc.perform(delete(ApiConstants.EVENT_ATTEND_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                    .andExpect(jsonPath("$.message").value(EventOwnerMustAttendEventException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When leaving event should return HTTP 404 Not Found if event does not exist")
        public void whenLeavingEventShouldReturnNotFoundIfEventDoesNotExist() throws Exception {
            mockMvc.perform(delete(ApiConstants.EVENT_ATTEND_URL, EventConstants.NOT_EXISTING_EVENT_ID)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(EventNotFoundException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When leaving event should return HTTP 409 Conflict if event already had place")
        public void whenLeavingEventShouldReturnConflictIfEventAlreadyHadPlace() throws Exception {
            Event savedEvent = requirePresent(
                    eventRepository.findById(savedEventId),
                    "Expected event to exist before update");
            savedEvent.setEventStartDate(TimeConstants.ONE_WEEK_AGO);
            eventRepository.save(savedEvent);

            mockMvc.perform(delete(ApiConstants.EVENT_ATTEND_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                    .andExpect(jsonPath("$.message").value(EventAlreadyHadPlaceException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When leaving event should return HTTP 403 Forbidden if user is not attending event")
        public void whenLeavingEventShouldReturnForbiddenIfUserIsNotAttendingEvent() throws Exception {
            mockMvc.perform(delete(ApiConstants.EVENT_ATTEND_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.message").value(NotEventAttenderException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When leaving event should return HTTP 200 OK and remove attending relationship")
        public void whenLeavingEventShouldReturnOkAndRemoveAttendingRelationship() throws Exception {
            Event event = requirePresent(
                    eventRepository.findById(savedEventId),
                    "Expected event to exist");
            User attender = requirePresent(
                    userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL),
                    "Expected second user to exist after auth setup");
            event.addAttendingUser(attender);
            eventRepository.save(event);
            userRepository.save(attender);

            mockMvc.perform(delete(ApiConstants.EVENT_ATTEND_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isOk());

            eventRepository.flush();
            User updatedUser = requirePresent(
                    userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL),
                    "Expected updated second user to exist");

            assertThat(eventHasAttender(savedEventId, updatedUser.getId()), equalTo(false));
        }
    }
}
