package com.mazurek.eventOrganizer.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mazurek.eventOrganizer.auth.*;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotEventAttenderException;
import com.mazurek.eventOrganizer.exception.thread.NotThreadReplyOwnerException;
import com.mazurek.eventOrganizer.exception.thread.ReplyNotFoundInThreadException;
import com.mazurek.eventOrganizer.exception.thread.ThreadNotFoundInEventException;
import com.mazurek.eventOrganizer.exception.user.UserAlreadyExistException;
import com.mazurek.eventOrganizer.jwt.JwtUtil;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.tag.TagRepository;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.thread.ThreadReply;
import com.mazurek.eventOrganizer.thread.ThreadReplyRepository;
import com.mazurek.eventOrganizer.thread.ThreadRepository;
import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadReplyCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadReplyDto;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
public class EventControllerIntegrationTest {


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
    private final String JWT_PREFIX = "Bearer ";



    private final AuthenticationRequest  firstUserAuthRequest = new AuthenticationRequest(FIRST_USER_EMAIL, USER_PASSWORD);
    private final AuthenticationRequest  secondUserAuthRequest = new AuthenticationRequest(SECOND_USER_EMAIL, USER_PASSWORD);

    private String firstUserJwt;
    private String secondUserJwt;
    private EventCreateDto eventCreateDto;

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
    JwtUtil jwtUtil;

    @Autowired
    private MockMvc mockMvc;

    @PostConstruct
    void beforeAll() {
        eventCreateDto = EventCreateDto.builder()
                .name(EVENT_NAME)
                .shortDescription(EVENT_SHORT_DESCRIPTION)
                .longDescription(EVENT_LONG_DESCRIPTION)
                .eventStartDate(EVENT_START_DATE)
                .city(EVENT_CITY)
                .exactAddress(EVENT_EXACT_ADDRESS)
                .tags(Arrays.stream(EVENT_TAGS).toList())
                .build();

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

        try{
            authenticationService.register(firstUserRegisterRequest);
            authenticationService.activateAccount(verificationTokenRepository.findByUserEmail(FIRST_USER_EMAIL).get().getId());
        } catch (UserAlreadyExistException userAlreadyExistException){
            System.out.println("First user already exits, processing to tests.");
        }

        try{
            authenticationService.register(secondUserRegisterRequest);
            authenticationService.activateAccount(verificationTokenRepository.findByUserEmail(SECOND_USER_EMAIL).get().getId());
        } catch (UserAlreadyExistException userAlreadyExistException){
            System.out.println("Second user already exits, processing to tests.");
        }


        firstUserJwt = JWT_PREFIX + authenticationService.authenticate(firstUserAuthRequest).getToken();
        secondUserJwt = JWT_PREFIX + authenticationService.authenticate(secondUserAuthRequest).getToken();
    }

    @Nested
    @DisplayName("Event core tests:")
    class CoreEventTests{
        @Nested
        @DisplayName("Create event tests:")
        @Transactional
        class CreateEventTests{

            @BeforeEach
            void setUp() {
            }

            @Test
            @DisplayName("When creating event should return Http BadRequest code on every data validation error.")
            public void whenCreatingEventShouldReturnHttpBadRequestCodeOnEveryDataValidationError() throws Exception {

                eventCreateDto.setLongDescription("to short");
                eventCreateDto.setShortDescription("to short");
                mockMvc.perform(
                                post("/api/v1/events")
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(eventCreateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.status").value("400"))
                        .andExpect(jsonPath("$.errors").isMap())
                        .andExpect(jsonPath("$.errors.longDescription").hasJsonPath())
                        .andExpect(jsonPath("$.errors.shortDescription").hasJsonPath())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON));

                eventCreateDto.setLongDescription(EVENT_LONG_DESCRIPTION);
                eventCreateDto.setShortDescription(EVENT_SHORT_DESCRIPTION);
                eventCreateDto.setEventStartDate(ZonedDateTime.now().minusDays(7));

                mockMvc.perform(
                                post("/api/v1/events")
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(eventCreateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.status").value("400"))
                        .andExpect(jsonPath("$.errors").isMap())
                        .andExpect(jsonPath("$.errors.eventStartDate").hasJsonPath())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
            }

            @Test
            @DisplayName("When creating event should return Http Created code on success.")
            public void whenCreatingEventShouldReturnHttpCreatedCodeOnSuccess() throws Exception {

                mockMvc.perform(
                                post("/api/v1/events")
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(eventCreateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isCreated())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
            }

            @Test
            @DisplayName("When creating event should return dto of created event with correct data.")
            public void whenCreatingEventShouldReturnDtoOfCreatedEventWithCorrectData() throws Exception {

                mockMvc.perform(
                                post("/api/v1/events")
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(eventCreateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isCreated())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.id").hasJsonPath())
                        .andExpect(jsonPath("$.shortDescription").value(EVENT_SHORT_DESCRIPTION))
                        .andExpect(jsonPath("$.longDescription").value(EVENT_LONG_DESCRIPTION))
                        .andExpect(jsonPath("$.city").value(EVENT_CITY))
                        .andExpect(jsonPath("$.createDate").hasJsonPath())
                        .andExpect(jsonPath("$.createDate").isNotEmpty())
                        .andExpect(jsonPath("$.lastUpdate").hasJsonPath())
                        .andExpect(jsonPath("$.lastUpdate").isNotEmpty())
                        .andExpect(jsonPath(
                                "$.eventStartDate",
                                Matchers.equalTo(
                                        objectMapper.writeValueAsString(EVENT_START_DATE.withSecond(0).withNano(0)).replaceAll("\"",""))));
            }
        }

        @Nested
        @DisplayName("Get event by id tests:")
        @Transactional
        class GetEventByIdTests{

            private UUID savedEventId;

            @BeforeEach
            void setUp() {
                savedEventId = eventService.createEvent(eventCreateDto, firstUserJwt.substring(7)).getId();
            }

            @Test
            @DisplayName("When getting event by id should return HTTP NotFound if event does not exists")
            public void whenGettingEventByIdShouldReturnHttpNotFoundIfEventDoesNotExists() throws Exception {
                String authenticationRequestJson = objectMapper.writeValueAsString(new AuthenticationRequest(FIRST_USER_EMAIL, USER_PASSWORD));

                mockMvc.perform(
                                get("/api/v1/events/"+ UUID.randomUUID())
                                        .header("Authorization", secondUserJwt))
                        .andExpect(status().isNotFound());

            }

            @Test
            @DisplayName("When getting event by id should return http Ok if event exists")
            public void whenGettingEventByIdShouldReturnHttpOkIfEventExists() throws Exception {

                mockMvc.perform(
                                get("/api/v1/events/"+ savedEventId)
                                        .header("Authorization", secondUserJwt)
                        )
                        .andExpect(status().isOk());
            }
            @Test
            @DisplayName("When getting event by id should return dto of event with correct data.")
            public void whenGettingEventByIdShouldReturnDtoOfEventWithCorrectData() throws Exception {
                mockMvc.perform(
                                get("/api/v1/events/"+ savedEventId)
                                        .header("Authorization", secondUserJwt))
                        .andExpect(header().string("Content-Type", "application/json"))
                        .andExpect(jsonPath("$.id").value(savedEventId.toString()))
                        .andExpect(jsonPath("$.shortDescription").value(EVENT_SHORT_DESCRIPTION))
                        .andExpect(jsonPath("$.longDescription").value(EVENT_LONG_DESCRIPTION))
                        .andExpect(jsonPath("$.city").value(EVENT_CITY))
                        .andExpect(jsonPath("$.createDate").hasJsonPath())
                        .andExpect(jsonPath("$.createDate").isNotEmpty())
                        .andExpect(jsonPath("$.lastUpdate").hasJsonPath())
                        .andExpect(jsonPath("$.lastUpdate").isNotEmpty())
                        .andExpect(jsonPath(
                                "$.eventStartDate",
                                Matchers.equalTo(
                                        objectMapper.writeValueAsString(EVENT_START_DATE.withSecond(0).withNano(0)).replaceAll("\"",""))));


            }

        }

        @Nested
        @DisplayName("Update event tests:")
        @Transactional
        class UpdateEventTests{

            private UUID savedEventId;

            private final String EVENT_NAME_UPDATE = "updated event name";
            private final String EVENT_LONG_DESCRIPTION_UPDATE =
                    "long description long description long description long description long description long description long description long description " +
                            "long description long description long description long description long description long description " +
                            "long description long description long description long description long description long description ";
            private final String EVENT_SHORT_DESCRIPTION_UPDATE = "short description update short description update short description update ";
            private final ZonedDateTime EVENT_START_DATE_UPDATE = ZonedDateTime.now().plusDays(7);
            private final String EVENT_CITY_UPDATE = "Krakow";
            private final String EVENT_EXACT_ADDRESS_UPDATE = "ul. Moniuszki 8 update";
            private final String[] EVENT_TAGS_UPDATE = {"update"};

            @BeforeEach
            void setUp() {
                savedEventId = eventService.createEvent(eventCreateDto, firstUserJwt.substring(7)).getId();
            }

            @Test
            @DisplayName("When updating event should return HTTP status 404 if event does not exists.")
            public void whenUpdatingEventShouldReturnHttpStatus404IfEventDoesNotExists() throws Exception {
                mockMvc.perform(
                                put("/api/v1/events/"+ UUID.randomUUID())
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(eventCreateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isNotFound())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status").value("404"))
                        .andExpect(jsonPath("$.message").hasJsonPath())
                        .andExpect(jsonPath("$.message").value("There is no event with this id."));
            }
            @Test
            @DisplayName("When updating event should return HTTP status 400 if event had place.")
            public void whenUpdatingEventShouldReturnHttpStatus400IfEventHadPlace() throws Exception {

                Event savedEvent = eventRepository.findById(savedEventId).get();
                savedEvent.setEventStartDate(ZonedDateTime.now().minusDays(7));
                eventRepository.save(savedEvent);

                mockMvc.perform(
                                put("/api/v1/events/"+ savedEventId)
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(eventCreateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status").value("400"))
                        .andExpect(jsonPath("$.message").hasJsonPath())
                        .andExpect(jsonPath("$.message").value("Event already had place."));
            }

            @Test
            @DisplayName("When updating event should return Http status 400 if user trying to update event is not event owner")
            public void whenUpdatingEventShouldReturnHttpStatus400IfUserTryingToUpdateEventIsNotEventOwner() throws Exception {
                mockMvc.perform(
                                put("/api/v1/events/"+ savedEventId)
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(eventCreateDto))
                                        .header("Authorization", secondUserJwt)
                        )
                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status").value("400"))
                        .andExpect(jsonPath("$.message").hasJsonPath())
                        .andExpect(jsonPath("$.message").value("You are not owner of this event!"));
            }

            @Test
            @DisplayName("When updating event should return HTTP status 200 with updated data if update was successful")
            public void whenUpdatingEventShouldReturnHttpStatus200WithUpdatedDataIfUpdateWasSuccessful() throws Exception {

                Event oldEvent = eventRepository.findById(savedEventId).get();

                eventCreateDto.setName(EVENT_NAME_UPDATE);
                eventCreateDto.setEventStartDate(EVENT_START_DATE_UPDATE);
                eventCreateDto.setCity(EVENT_CITY_UPDATE);
                eventCreateDto.setExactAddress(EVENT_EXACT_ADDRESS_UPDATE);
                eventCreateDto.setTags(Arrays.stream(EVENT_TAGS_UPDATE).toList());
                eventCreateDto.setShortDescription(EVENT_SHORT_DESCRIPTION_UPDATE);
                eventCreateDto.setLongDescription(EVENT_LONG_DESCRIPTION_UPDATE);

                mockMvc.perform(
                                put("/api/v1/events/"+ savedEventId)
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(eventCreateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.id").hasJsonPath())
                        .andExpect(jsonPath("$.id").value(savedEventId.toString()))

                        .andExpect(jsonPath("$.name").hasJsonPath())
                        .andExpect(jsonPath("$.name").value(EVENT_NAME_UPDATE))

                        .andExpect(jsonPath("$.shortDescription").hasJsonPath())
                        .andExpect(jsonPath("$.shortDescription").value(EVENT_SHORT_DESCRIPTION_UPDATE))

                        .andExpect(jsonPath("$.longDescription").hasJsonPath())
                        .andExpect(jsonPath("$.longDescription").value(EVENT_LONG_DESCRIPTION_UPDATE))

                        .andExpect(jsonPath("$.city").hasJsonPath())
                        .andExpect(jsonPath("$.city").value(EVENT_CITY_UPDATE))

                        .andExpect(jsonPath("$.exactAddress").hasJsonPath())
                        .andExpect(jsonPath("$.exactAddress").value(EVENT_EXACT_ADDRESS_UPDATE))

                        .andExpect(jsonPath("$.tags").hasJsonPath())
                        .andExpect(jsonPath("$.tags").isNotEmpty())
                        .andExpect(jsonPath("$.tags", hasSize(1)))
                        .andExpect(jsonPath("$.tags[0]").value(EVENT_TAGS_UPDATE[0]))

                        .andExpect(jsonPath("$.eventStartDate").hasJsonPath())
                        .andExpect(jsonPath("$.eventStartDate").value(objectMapper.writeValueAsString(EVENT_START_DATE.withSecond(0).withNano(0)).replaceAll("\"","")))

                        .andExpect(jsonPath("$.createDate").hasJsonPath())
                        .andExpect(jsonPath("$.createDate").isNotEmpty())
                        .andExpect(jsonPath("$.createDate").value(objectMapper.writeValueAsString(oldEvent.getCreateDate()).replaceAll("\"","")))

                        .andExpect(jsonPath("$.lastUpdate").hasJsonPath())
                        .andExpect(jsonPath("$.lastUpdate").isNotEmpty());
            }

            @Test
            @DisplayName("When updating event should update time if timezone have changed.")
            public void whenUpdatingEventShouldUpdateTimeZoneIfTimeZoneHaveChange() throws Exception {

                Event oldEvent = eventRepository.findById(savedEventId).get();

                eventCreateDto.setEventStartDate(EVENT_START_DATE_UPDATE.withZoneSameInstant(ZoneId.of("Europe/London")));

                mockMvc.perform(
                                put("/api/v1/events/"+ savedEventId)
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(eventCreateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))

                        .andExpect(jsonPath("$.eventStartDate").hasJsonPath())
                        .andExpect(
                                jsonPath("$.eventStartDate")
                                        .value(objectMapper.writeValueAsString(EVENT_START_DATE_UPDATE.withSecond(0).withNano(0).withZoneSameInstant(ZoneId.of("Europe/London"))).replaceAll("\"","")));
            }

            @Test
            @DisplayName("When updating event should return HTTP status 400 with error messages if data for update are not correct.")
            public void whenUpdatingEventShouldReturnHttpStatus400WithErrorMessagesIfDataForUpdateAreNotCorrect() throws Exception{

                eventCreateDto.setName(null);
                eventCreateDto.setShortDescription(null);
                eventCreateDto.setLongDescription(null);
                eventCreateDto.setCity(null);
                eventCreateDto.setExactAddress(null);
                eventCreateDto.setEventStartDate(null);

                mockMvc.perform(
                                put("/api/v1/events/"+ savedEventId)
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(eventCreateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.status").hasJsonPath())
                        .andExpect(jsonPath("$.status").value("400"))
                        .andExpect(jsonPath("$.errors").hasJsonPath())
                        .andExpect(jsonPath("$.errors.name").hasJsonPath())
                        .andExpect(jsonPath("$.errors.shortDescription").hasJsonPath())
                        .andExpect(jsonPath("$.errors.longDescription").hasJsonPath())
                        .andExpect(jsonPath("$.errors.eventStartDate").hasJsonPath())
                        .andExpect(jsonPath("$.errors.city").hasJsonPath())
                        .andExpect(jsonPath("$.errors.exactAddress").hasJsonPath());
            }
            @Test
            @DisplayName("When updating event should not remove tags from database after removing them from event")
            public void whenUpdatingEventShouldNotRemoveTagsFromDatabaseAfterRemovingThemFromEvent ()throws Exception {

                eventCreateDto.setTags(Arrays.stream(EVENT_TAGS_UPDATE).toList());

                mockMvc.perform(
                                put("/api/v1/events/"+ savedEventId)
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(eventCreateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isOk());

                List<Tag> oldTags = new ArrayList<>();
                try {
                    Arrays.stream(EVENT_TAGS).toList()
                            .forEach(tagName -> oldTags.add(tagRepository.findByIgnoreCaseName(tagName).orElseThrow(RuntimeException::new)));
                } finally {
                    assertThat(oldTags.size(), equalTo(EVENT_TAGS.length));
                }
            }

            @Test
            @DisplayName("When updating event should not remove city from database after changing it in event.")
            public void whenUpdatingEventShouldNotRemoveCityFromDatabaseAfterChangingItInEvent ()throws Exception {

                eventCreateDto.setCity(EVENT_CITY_UPDATE);

                mockMvc.perform(
                                put("/api/v1/events/"+ savedEventId)
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(eventCreateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isOk());

                assertThat(cityRepository.findByIgnoreCaseName(EVENT_CITY).isPresent(), equalTo(true));
                assertThat(cityRepository.findByIgnoreCaseName(EVENT_CITY_UPDATE).isPresent(), equalTo(true));
            }

        }


    }

    @Nested
    @DisplayName("Event thread tests")
    class EventThreadTests{

        private final String THREAD_NAME = "test thread";
        private final String THREAD_CONTENT = "test thread message have to be longer than 20 characters, so make sure to exceed those limits";

        private final String THREAD_NAME_UPDATE = "updated thread name";
        private final String THREAD_CONTENT_UPDATE = "updated thread content updated thread content updated thread content updated thread content updated thread content ";

        private final String THREAD_REPLY_CONTENT = "test thread reply message";
        private final String THREAD_REPLY_CONTENT_UPDATE = "updated test thread reply message";

        ThreadCreateDto eventThreadCreateDto = ThreadCreateDto.builder()
                .name(THREAD_NAME)
                .content(THREAD_CONTENT)
                .build();

        ThreadCreateDto threadUpdateDto = ThreadCreateDto.builder()
                .name(THREAD_NAME_UPDATE)
                .content(THREAD_CONTENT_UPDATE)
                .build();

        @Nested
        @DisplayName("Create thread in event tests: ")
        @Transactional
        class CreateThreadInEventTests{

            private UUID savedEventId;

            @BeforeEach
            void setUp() {
                savedEventId = eventService.createEvent(eventCreateDto, firstUserJwt.substring(7)).getId();
            }

            @Test
            @DisplayName("When creating thread in event should return HTTP Forbidden if there is no Authorization Header.")
            public void whenCreatingThreadInEventShouldReturnHttpForbiddenIfThereIsNoAuthorizationHeader() throws Exception {

                mockMvc.perform(
                                post("/api/v1/events/"+ UUID.randomUUID()+"/threads")
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(eventThreadCreateDto))
                        )
                        .andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("When creating thread in event should return HTTP Forbidden if there is no Authorization Header.")
            public void whenCreatingThreadInEventShouldReturnHttpForbiddenIfAuthorizationHeaderIsEmpty() throws Exception {

                mockMvc.perform(
                                post("/api/v1/events/"+ UUID.randomUUID()+"/threads")
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(eventThreadCreateDto))
                                        .header("Authorization", "")
                        )
                        .andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("When creating thread in event should return HTTP NotFound if event does not exist.")
            public void whenCreatingThreadInEventShouldReturnHttpNotFoundIfEventDoesNotExist() throws Exception {

                mockMvc.perform(
                                post("/api/v1/events/"+ UUID.randomUUID()+"/threads")
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(eventThreadCreateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isNotFound())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status").value("404"))
                        .andExpect(jsonPath("$.message").hasJsonPath());

            }

            @Test
            @DisplayName("When creating thread in event should return HTTP BadRequest with errors if data for creation is invalid")
            public void whenCreatingThreadInEventShouldReturnHttpBadRequestWithErrorsIfDataForCreationIsInvalid() throws Exception {

                eventThreadCreateDto.setName("short");
                eventThreadCreateDto.setContent("short");

                mockMvc.perform(
                                post("/api/v1/events/"+ savedEventId+"/threads")
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(eventThreadCreateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status").value("400"))
                        .andExpect(jsonPath("$.errors").hasJsonPath())
                        .andExpect(jsonPath("$.errors.name").hasJsonPath())
                        .andExpect(jsonPath("$.errors.content").hasJsonPath());

            }

            @Test
            @DisplayName("When creating thread in event should return Http BadRequest if user is not attending event.")
            public void whenCreatingThreadInEventShouldReturnHttpBadRequestIfUserIsNotAttendingEvent() throws Exception {
                mockMvc.perform(
                                post("/api/v1/events/"+ savedEventId+"/threads")
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(eventThreadCreateDto))
                                        .header("Authorization", secondUserJwt)
                        )
                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status").hasJsonPath())
                        .andExpect(jsonPath("$.status").value("400"))
                        .andExpect(jsonPath("$.message").hasJsonPath())
                        .andExpect(jsonPath("$.message").value(NotEventAttenderException.DEFAULT_MESSAGE));
            }

            @Test
            @DisplayName("When creating thread in event should return Http Created on success.")
            public void whenCreatingThreadInEventShouldReturnHttpCreatedOnSuccess() throws Exception {
                mockMvc.perform(
                                post("/api/v1/events/"+ savedEventId+"/threads")
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(eventThreadCreateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isCreated())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
            }

            @Test
            @DisplayName("When creating thread in event should return dto of created thread with correct data.")
            public void whenCreatingThreadInEventShouldReturnDtoOfCreatedThreadWithCorrectData() throws Exception {

                User threadOwner= userRepository.findByEmail(FIRST_USER_EMAIL).orElseThrow(RuntimeException::new);

                mockMvc.perform(
                                post("/api/v1/events/"+ savedEventId+"/threads")
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(eventThreadCreateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isCreated())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.id").hasJsonPath())
                        .andExpect(jsonPath("$.id").isNotEmpty())
                        .andExpect(jsonPath("$.eventId").hasJsonPath())
                        .andExpect(jsonPath("$.eventId").value(savedEventId.toString()))

                        .andExpect(jsonPath("$.name").hasJsonPath())
                        .andExpect(jsonPath("$.name").value(THREAD_NAME))

                        .andExpect(jsonPath("$.content").hasJsonPath())
                        .andExpect(jsonPath("$.content").value(THREAD_CONTENT))

                        .andExpect(jsonPath("$.createDate").hasJsonPath())
                        .andExpect(jsonPath("$.createDate").isNotEmpty())
                        .andExpect(jsonPath("$.lastUpdate").hasJsonPath())
                        .andExpect(jsonPath("$.lastUpdate").isNotEmpty())
                        .andExpect(jsonPath("$.editCounter").hasJsonPath())
                        .andExpect(jsonPath("$.editCounter").value(0))

                        .andExpect(jsonPath("$.owner").hasJsonPath())
                        .andExpect(jsonPath("$.owner").isNotEmpty())
                        .andExpect(jsonPath("$.owner.id").hasJsonPath())
                        .andExpect(jsonPath("$.owner.id").isNotEmpty())
                        .andExpect(jsonPath("$.owner.id").value(threadOwner.getId().toString()));
            }

            @Test
            @DisplayName("When creating thread in event should save all data with relationships on success.")
            public void whenCreatingThreadInEventShouldSaveAllDataWithRelationShipsOnSuccess() throws Exception {

                MvcResult mvcResult = mockMvc.perform(
                                post("/api/v1/events/"+ savedEventId+"/threads")
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(eventThreadCreateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isCreated())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andReturn();

                User threadOwner= userRepository.findByEmail(FIRST_USER_EMAIL).orElseThrow(RuntimeException::new);
                ThreadDto returnedThreadDto = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ThreadDto.class);

                assertTrue(threadRepository.findById(returnedThreadDto.getId()).isPresent());

                Thread createdThread = threadRepository.findById(returnedThreadDto.getId()).orElseThrow(RuntimeException::new);
                assertEquals(savedEventId, createdThread.getEvent().getId());
                assertEquals(threadOwner, createdThread.getOwner());
                assertTrue(threadOwner.getThreads().contains(createdThread));
                assertEquals(THREAD_NAME, createdThread.getName());
                assertEquals(THREAD_CONTENT, createdThread.getContent());
                assertEquals(0, createdThread.getEditCounter());
                assertTrue(createdThread.getCreateDate().equals(createdThread.getLastUpdate()));
            }

        }

        @Nested
        @DisplayName("Update thread in event tests: ")
        @Transactional
        class UpdateThreadInEventTests{

            private UUID savedEventId;
            private UUID savedThreadId;

            @BeforeEach
            void setUp() {
                savedEventId = eventService.createEvent(eventCreateDto, firstUserJwt.substring(7)).getId();
                savedThreadId = eventService.createThreadInEvent(eventThreadCreateDto, savedEventId ,firstUserJwt.substring(7)).getId();

            }

            @Test
            @DisplayName("When updating thread in event should return Http Forbidden if there is no Authorization Header.")
            public void whenUpdatingThreadInEventShouldReturnHttpForbiddenIfThereIsNoAuthorizationHeader() throws Exception {

                mockMvc.perform(
                                put("/api/v1/events/"+ savedEventId +"/threads/"+ savedThreadId)
                                        .contentType(ContentType.APPLICATION_JSON.toString()))
                        .andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("When updating thread in event should return Http BadRequest if there is no request body.")
            public void whenUpdatingThreadInEventShouldReturnHttpBadRequestIfThereIsNoRequestBody() throws Exception {

                mockMvc.perform(
                                put("/api/v1/events/"+ savedEventId +"/threads/"+ savedThreadId)
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .header("Authorization", firstUserJwt))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("When updating thread in event should return Http NotFound if there is no event with this id.")
            public void whenUpdatingThreadInEventShouldReturnHttpNotFoundIfThereIsNoEventWithThisId() throws Exception {

                mockMvc.perform(
                                put("/api/v1/events/"+ UUID.randomUUID() + "/threads/"+ savedThreadId)
                                        .header("Authorization", firstUserJwt)
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(threadUpdateDto)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.status").value("404"))
                        .andExpect(jsonPath("$.message").hasJsonPath())
                        .andExpect(jsonPath("$.message").value(EventNotFoundException.DEFAULT_MESSAGE));
            }

            @Test
            @DisplayName("When updating thread in event should return Http NotFound if thread does not exist or it is not associated with given event id")
            public void whenUpdatingThreadInEventShouldReturnHttpNotFoundIfThreadDoesNotExistOrItIsNotAssociatedWithGivenEventId() throws Exception {
                UUID secondSavedEventId = eventService.createEvent(eventCreateDto, secondUserJwt.substring(7)).getId();
                UUID secondSavedThreadId = eventService.createThreadInEvent(eventThreadCreateDto, secondSavedEventId, secondUserJwt.substring(7)).getId();
                eventService.addAttenderToEvent(savedEventId, secondUserJwt.substring(7));

                mockMvc.perform(
                                put("/api/v1/events/" + savedEventId + "/threads/" + secondSavedThreadId)
                                        .header("Authorization", secondUserJwt)
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(threadUpdateDto)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.status").value("404"))
                        .andExpect(jsonPath("$.message").hasJsonPath())
                        .andExpect(jsonPath("$.message").value(ThreadNotFoundInEventException.DEFAULT_MESSAGE));

                mockMvc.perform(
                                put("/api/v1/events/" + secondSavedEventId + "/threads/" + savedThreadId)
                                        .header("Authorization", firstUserJwt)
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(threadUpdateDto)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.status").value("404"))
                        .andExpect(jsonPath("$.message").hasJsonPath())
                        .andExpect(jsonPath("$.message").value(ThreadNotFoundInEventException.DEFAULT_MESSAGE));

                mockMvc.perform(
                                put("/api/v1/events/" + savedEventId + "/threads/" + savedThreadId)
                                        .header("Authorization", firstUserJwt)
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(threadUpdateDto)))
                        .andExpect(status().isOk());

                mockMvc.perform(
                                put("/api/v1/events/" + secondSavedEventId + "/threads/" + secondSavedThreadId)
                                        .header("Authorization", secondUserJwt)
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(threadUpdateDto)))
                        .andExpect(status().isOk());


            }

            @Test
            @DisplayName("When updating thread in event should return Http BadRequest if thread owner is not attending event anymore.")
            public void whenUpdatingThreadInEventShouldReturnHttpBadRequestIfThreadOwnerIsNotAttendingEventAnymore() throws Exception {

                eventService.addAttenderToEvent(savedEventId, secondUserJwt.substring(7));
                UUID secondThreadID = eventService.createThreadInEvent(eventThreadCreateDto, savedEventId, secondUserJwt.substring(7)).getId();

                eventService.removeAttenderFromEvent(savedEventId, secondUserJwt.substring(7));

                mockMvc.perform(
                                put("/api/v1/events/"+ savedEventId + "/threads/"+ savedThreadId)
                                        .header("Authorization", secondUserJwt)
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(threadUpdateDto)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.status").value("400"))
                        .andExpect(jsonPath("$.message").hasJsonPath())
                        .andExpect(jsonPath("$.message").value(NotEventAttenderException.DEFAULT_MESSAGE));

            }

            @Test
            @DisplayName("When updating thread in event should return Http Ok on success.")
            public void whenUpdatingThreadInEventShouldReturnHttpOkOnSuccess() throws Exception {

                mockMvc.perform(
                                put("/api/v1/events/"+ savedEventId + "/threads/"+ savedThreadId)
                                        .header("Authorization", firstUserJwt)
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(threadUpdateDto)))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
            }

            @Test
            @DisplayName("When updating thread in event should return updated data on success.")
            public void whenUpdatingThreadInEventShouldReturnUpdatedDataOnSuccess() throws Exception {

                mockMvc.perform(
                                put("/api/v1/events/"+ savedEventId + "/threads/"+ savedThreadId)
                                        .header("Authorization", firstUserJwt)
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(threadUpdateDto)))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.id").hasJsonPath())
                        .andExpect(jsonPath("$.id").value(savedThreadId.toString()))
                        .andExpect(jsonPath("$.name").hasJsonPath())
                        .andExpect(jsonPath("$.name").value(THREAD_NAME_UPDATE))
                        .andExpect(jsonPath("$.content").hasJsonPath())
                        .andExpect(jsonPath("$.content").value(THREAD_CONTENT_UPDATE))
                        .andExpect(jsonPath("$.editCounter").hasJsonPath())
                        .andExpect(jsonPath("$.editCounter").value(1))
                        .andExpect(jsonPath("$.lastUpdate").hasJsonPath())
                        .andExpect(jsonPath("$.lastUpdate").value(objectMapper.writeValueAsString(threadRepository.findById(savedThreadId).orElseThrow(RuntimeException::new).getLastUpdate()).replaceAll("\"","")));
            }
        }

        @Nested
        @DisplayName("Create reply in thread tests:")
        public class CreateReplyInThreadTests{
            private UUID savedEventId;
            private UUID savedThreadId;
            private ThreadReplyCreateDto threadReplyCreateDto;

            @BeforeEach
            void setUp() {
                savedEventId = eventService.createEvent(eventCreateDto, firstUserJwt.substring(7)).getId();
                savedThreadId = eventService.createThreadInEvent(eventThreadCreateDto, savedEventId, firstUserJwt.substring(7)).getId();

                threadReplyCreateDto = ThreadReplyCreateDto.builder()
                        .replyContent(THREAD_REPLY_CONTENT)
                        .build();
            }

            @Test
            @DisplayName("When creating reply in thread should return Http Forbidden if there is no Authorization Header.")
            public void whenCreatingReplyInThreadShouldReturnHttpForbiddenIfThereIsNoAuthorizationHeader() throws Exception {
                mockMvc.perform(
                                post("/api/v1/events/"+ savedEventId + "/threads/ "+ savedThreadId + "/replies")
                                        .contentType(ContentType.APPLICATION_JSON.toString()))
                        .andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("When creating reply in thread should return Http BadRequest if there is no content.")
            public void whenCreatingReplyInThreadShouldReturnHttpBadRequestIfThereIsNoContent() throws Exception {
                mockMvc.perform(
                                post("/api/v1/events/"+ savedEventId + "/threads/" + savedThreadId + "/replies")
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .header("Authorization", firstUserJwt))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("When creating reply in thread should return Http BadRequest if user is not attending event.")
            public void whenCreatingReplyInThreadShouldReturnHttpBadRequestIfUserIsNotAttendingEvent() throws Exception {
                mockMvc.perform(
                                post("/api/v1/events/"+ savedEventId + "/threads/" + savedThreadId + "/replies")
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(threadReplyCreateDto))
                                        .header("Authorization", secondUserJwt))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.status").value("400"))
                        .andExpect(jsonPath("$.message").hasJsonPath())
                        .andExpect(jsonPath("$.message").value(NotEventAttenderException.DEFAULT_MESSAGE));
            }


            @Test
            @DisplayName("When creating reply in thread should return Http NotFound if event with given id does not exist.")
            public void whenCreatingReplyInThreadShouldReturnHttpNotFoundIfEventWithGivenIdDoesNotExists() throws Exception {

                mockMvc.perform(
                                post("/api/v1/events/"+ UUID.randomUUID() + "/threads/" + savedThreadId + "/replies")
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(threadReplyCreateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.status").value("404"))
                        .andExpect(jsonPath("$.message").hasJsonPath())
                        .andExpect(jsonPath("$.message").value(EventNotFoundException.DEFAULT_MESSAGE));
            }

            @Test
            @DisplayName("When creating reply in thread should return Http NotFound if thread with given id does not exist or is not event related.")
            public void whenCreatingReplyInThreadShouldReturnHttpNotFoundIfThreadWithGivenIdDoesNotExistsOrIsNotEventRelated() throws Exception {
                mockMvc.perform(
                                post("/api/v1/events/"+ savedEventId +"/threads/"+ UUID.randomUUID() + "/replies")
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(threadReplyCreateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.status").value("404"))
                        .andExpect(jsonPath("$.message").hasJsonPath())
                        .andExpect(jsonPath("$.message").value(ThreadNotFoundInEventException.DEFAULT_MESSAGE));
            }

            @Test
            @DisplayName("When creating reply in thread should return Http BadRequest if there was data validation error.")
            public void whenCreatingReplyInThreadShouldReturnHttpBadRequestIfThereWasDataValidationError() throws Exception {

                threadReplyCreateDto.setReplyContent("");

                mockMvc.perform(
                                post("/api/v1/events/"+ savedEventId +"/threads/"+ savedThreadId + "/replies")
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(threadReplyCreateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status").hasJsonPath())
                        .andExpect(jsonPath("$.status").value("400"))
                        .andExpect(jsonPath("$.errors").hasJsonPath())
                        .andExpect(jsonPath("$.errors.replyContent").hasJsonPath())
                        .andExpect(jsonPath("$.errors.replyContent").isNotEmpty());

            }

            @Test
            @DisplayName("When creating reply in thread should return Http Created on success.")
            public void whenCreatingReplyInThreadShouldReturnHttpCreatedOnSuccess() throws Exception {
                mockMvc.perform(
                                post("/api/v1/events/"+ savedEventId +"/threads/"+ savedThreadId + "/replies")
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(threadReplyCreateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isCreated())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            }

            @Test
            @DisplayName("When creating reply in thread should return data of created reply in thread.")
            public void whenCreatingReplyInThreadShouldReturnDataOfCreatedReplyInThread() throws Exception {
                mockMvc.perform(
                                post("/api/v1/events/"+ savedEventId +"/threads/"+ savedThreadId + "/replies")
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(threadReplyCreateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isCreated())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.id").hasJsonPath())
                        .andExpect(jsonPath("$.id").isNotEmpty())
                        .andExpect(jsonPath("$.threadId").hasJsonPath())
                        .andExpect(jsonPath("$.threadId").value(savedThreadId.toString()))
                        .andExpect(jsonPath("$.content").hasJsonPath())
                        .andExpect(jsonPath("$.content").value(THREAD_REPLY_CONTENT))
                        .andExpect(jsonPath("$.replyDate").hasJsonPath())
                        .andExpect(jsonPath("$.replyDate").isNotEmpty())
                        .andExpect(jsonPath("$.lastUpdate").hasJsonPath())
                        .andExpect(jsonPath("$.lastUpdate").isNotEmpty())
                        .andExpect(jsonPath("$.editCounter").hasJsonPath())
                        .andExpect(jsonPath("$.editCounter").value(0));
            }

            @Test
            @DisplayName("When Creating reply in thread should persist data on success.")
            public void whenCreatingReplyInThreadShouldPersistDataOnSuccess() throws Exception {
                MvcResult mvcResult =mockMvc.perform(
                                post("/api/v1/events/"+ savedEventId +"/threads/"+ savedThreadId + "/replies")
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(threadReplyCreateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isCreated())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andReturn();
                ThreadReplyDto returnedThreadReplyDto = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ThreadReplyDto.class);
                ThreadReply threadReply = threadReplyRepository.findById(returnedThreadReplyDto.getId()).orElseThrow(RuntimeException::new);

                User user = userRepository.findByEmail(FIRST_USER_EMAIL).orElseThrow(RuntimeException::new);
                Thread thread = threadRepository.findById(savedThreadId).orElseThrow(RuntimeException::new);

                assertEquals(savedThreadId, threadReply.getThread().getId());
                assertEquals(THREAD_REPLY_CONTENT, threadReply.getContent());
                assertEquals(0, threadReply.getEditCounter());
                assertEquals(threadReply.getReplayDate(), threadReply.getLastUpdate());
                assertEquals(user, threadReply.getReplier());
                assertEquals(thread.getId(), threadReply.getThread().getId());


            }

        }

        @Nested
        @DisplayName("Update reply in thread tests:")
        public class UpdateReplyInThreadTests{
            private UUID savedEventId;
            private UUID savedThreadId;
            private UUID savedReplyId;
            private final ThreadReplyCreateDto threadReplyCreateDto = new ThreadReplyCreateDto("original thread reply content");

            private ThreadReplyCreateDto threadReplyUpdateDto;

            @BeforeEach
            void setUp() {
                savedEventId = eventService.createEvent(eventCreateDto, firstUserJwt.substring(7)).getId();
                savedThreadId = eventService.createThreadInEvent(eventThreadCreateDto, savedEventId, firstUserJwt.substring(7)).getId();
                savedReplyId = eventService.createReplyInThread(threadReplyCreateDto,savedEventId, savedThreadId, firstUserJwt.substring(7)).getId();
                threadReplyUpdateDto = new ThreadReplyCreateDto(THREAD_REPLY_CONTENT_UPDATE);
            }

            @Test
            @DisplayName("When updating reply in thread should return Http Forbidden if there is no Authorization Header.")
            public void whenUpdatingReplyInThreadShouldReturnHttpForbiddenIfThereIsNoAuthorizationHeader() throws Exception {
                mockMvc.perform(
                                put("/api/v1/events/" + savedEventId + "/threads/ "+ savedThreadId + "/replies/" + savedReplyId)
                                        .contentType(ContentType.APPLICATION_JSON.toString()))
                        .andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("When updating reply in thread should return Http BadRequest if there is no content.")
            public void whenUpdatingReplyInThreadShouldReturnHttpBadRequestIfThereIsNoContent() throws Exception {
                mockMvc.perform(
                                put("/api/v1/events/" + savedEventId + "/threads/" + savedThreadId + "/replies/" + savedReplyId)
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .header("Authorization", firstUserJwt))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("When updating reply in thread should return Http BadRequest if user is not attending event.")
            public void whenUpdatingReplyInThreadShouldReturnHttpBadRequestIfUserIsNotAttendingEvent() throws Exception {
                mockMvc.perform(
                                put("/api/v1/events/" + savedEventId + "/threads/" + savedThreadId + "/replies/" + savedReplyId)
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(threadReplyUpdateDto))
                                        .header("Authorization", secondUserJwt))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.status").value("400"))
                        .andExpect(jsonPath("$.message").hasJsonPath())
                        .andExpect(jsonPath("$.message").value(NotEventAttenderException.DEFAULT_MESSAGE));
            }


            @Test
            @DisplayName("When updating reply in thread should return Http NotFound if event with given id does not exist.")
            public void whenUpdatingReplyInThreadShouldReturnHttpNotFoundIfEventWithGivenIdDoesNotExists() throws Exception {

                mockMvc.perform(
                                put("/api/v1/events/" + UUID.randomUUID() + "/threads/" + savedThreadId + "/replies/" + savedReplyId)
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(threadReplyUpdateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.status").value("404"))
                        .andExpect(jsonPath("$.message").hasJsonPath())
                        .andExpect(jsonPath("$.message").value(EventNotFoundException.DEFAULT_MESSAGE));
            }

            @Test
            @DisplayName("When updating reply in thread should return Http NotFound if thread with given id does not exist or is not event related.")
            public void whenUpdatingReplyInThreadShouldReturnHttpNotFoundIfThreadWithGivenIdDoesNotExistsOrIsNotEventRelated() throws Exception {
                mockMvc.perform(
                                put("/api/v1/events/" + savedEventId + "/threads/" + UUID.randomUUID() + "/replies/" + savedReplyId)
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(threadReplyUpdateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.status").value("404"))
                        .andExpect(jsonPath("$.message").hasJsonPath())
                        .andExpect(jsonPath("$.message").value(ThreadNotFoundInEventException.DEFAULT_MESSAGE));
            }

            @Test
            @DisplayName("When updating reply in thread should return Http NotFound if reply with given id does not exists or reply is not thread related.")
            public void whenUpdatingReplyInThreadShouldReturnHttpNotFoundIfReplyWithGivenIdDoesNotExistsOrReplyIsNotThreadRelated() throws Exception {
                mockMvc.perform(
                                put("/api/v1/events/" + savedEventId + "/threads/" + savedThreadId + "/replies/" + UUID.randomUUID())
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(threadReplyUpdateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.status").value("404"))
                        .andExpect(jsonPath("$.message").hasJsonPath())
                        .andExpect(jsonPath("$.message").value(ReplyNotFoundInThreadException.DEFAULT_MESSAGE));
            }

            @Test
            @DisplayName("When updating reply in thread should return Http BadRequest if users tries to update not his reply.")
            public void whenUpdatingReplyInThreadShouldReturnHttpBadRequestIfUserTriesToUpdateNotHisReply() throws Exception {

                eventService.addAttenderToEvent(savedEventId, secondUserJwt.substring(7));

                mockMvc.perform(
                                put("/api/v1/events/" + savedEventId + "/threads/" + savedThreadId + "/replies/" + savedReplyId)
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(threadReplyUpdateDto))
                                        .header("Authorization", secondUserJwt)
                        )
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.status").value("400"))
                        .andExpect(jsonPath("$.message").hasJsonPath())
                        .andExpect(jsonPath("$.message").value(NotThreadReplyOwnerException.DEFAULT_MESSAGE));
            }

            @Test
            @DisplayName("When updating reply in thread should return Http BadRequest if there was data validation error.")
            public void whenUpdatingReplyInThreadShouldReturnHttpBadRequestIfThereWasDataValidationError() throws Exception {

                threadReplyUpdateDto.setReplyContent("");

                mockMvc.perform(
                                put("/api/v1/events/" + savedEventId + "/threads/" + savedThreadId + "/replies/" + savedReplyId)
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(threadReplyUpdateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isBadRequest())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.status").hasJsonPath())
                        .andExpect(jsonPath("$.status").value("400"))
                        .andExpect(jsonPath("$.errors").hasJsonPath())
                        .andExpect(jsonPath("$.errors.replyContent").hasJsonPath())
                        .andExpect(jsonPath("$.errors.replyContent").isNotEmpty());

            }

            @Test
            @DisplayName("When updating reply in thread should return Http Ok on success.")
            public void whenUpdatingReplyInThreadShouldReturnHttpOkOnSuccess() throws Exception {
                mockMvc.perform(
                                put("/api/v1/events/" + savedEventId + "/threads/" + savedThreadId + "/replies/" + savedReplyId)
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(threadReplyUpdateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            }
            @Test
            @DisplayName("When updating reply in thread should return data of updated reply in thread.")
            public void whenUpdatingReplyInThreadShouldReturnDataOfUpdatedReplyInThread() throws Exception {
                mockMvc.perform(
                                put("/api/v1/events/"+ savedEventId +"/threads/"+ savedThreadId + "/replies/" + savedReplyId)
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(threadReplyUpdateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.id").hasJsonPath())
                        .andExpect(jsonPath("$.id").isNotEmpty())
                        .andExpect(jsonPath("$.threadId").hasJsonPath())
                        .andExpect(jsonPath("$.threadId").value(savedThreadId.toString()))
                        .andExpect(jsonPath("$.content").hasJsonPath())
                        .andExpect(jsonPath("$.content").value(THREAD_REPLY_CONTENT_UPDATE))
                        .andExpect(jsonPath("$.replyDate").hasJsonPath())
                        .andExpect(jsonPath("$.replyDate").isNotEmpty())
                        .andExpect(jsonPath("$.lastUpdate").hasJsonPath())
                        .andExpect(jsonPath("$.lastUpdate").isNotEmpty())
                        .andExpect(jsonPath("$.editCounter").hasJsonPath())
                        .andExpect(jsonPath("$.editCounter").value(1));
            }

            @Test
            @DisplayName("When updating reply in thread should persist updated data.")
            public void whenUpdatingReplyInThreadShouldPersistUpdatedData() throws Exception {

                ThreadReply originalReply = threadReplyRepository.findById(savedReplyId).orElseThrow(IllegalArgumentException::new);

                mockMvc.perform(
                                put("/api/v1/events/"+ savedEventId +"/threads/"+ savedThreadId + "/replies/" + savedReplyId)
                                        .contentType(ContentType.APPLICATION_JSON.toString())
                                        .content(objectMapper.writeValueAsString(threadReplyUpdateDto))
                                        .header("Authorization", firstUserJwt)
                        )
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON));

                ThreadReply updatedReply = threadReplyRepository.findById(savedReplyId).orElseThrow(IllegalArgumentException::new);

                assertEquals(updatedReply.getContent(),THREAD_REPLY_CONTENT_UPDATE);
                assertNotEquals(originalReply.getContent(),updatedReply.getContent());
                assertEquals(updatedReply.getEditCounter(),originalReply.getEditCounter()+1);
                assertTrue(originalReply.getLastUpdate().isBefore(updatedReply.getLastUpdate()));
            }
        }
    }

    @Nested
    @DisplayName("Event files tests:")
    class EventFilesTests{

        @Nested
        @DisplayName("Create thread in event tests: ")
        @Transactional
        class UploadFileToEventTests{

            private UUID savedEventId;

            @BeforeEach
            void setUp() {
                savedEventId = eventService.createEvent(eventCreateDto, firstUserJwt.substring(7)).getId();
            }




        }

    }





}
