package com.mazurek.eventOrganizer.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mazurek.eventOrganizer.auth.*;
import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.exception.user.UserAlreadyExistException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

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
    private final ZonedDateTime EVENT_START_DATE = ZonedDateTime.now().plusDays(7);
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
    @DisplayName("Create event tests:")
    @Transactional
    class CreateEventTests{

        @BeforeEach
        void setUp() {
        }

        @Test
        @DisplayName("When creating event should return Http CREATED code on success.")
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
        @DisplayName("When creating event should return Http BAD_REQUEST code on every data validation error.")
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
        }
    }

    @Nested
    @DisplayName("Get event by id tests:")
    @Transactional
    class GetEventByIdTests{

        private final UUID eventId = UUID.randomUUID();

        private UUID savedEventId;


        @BeforeEach
        void setUp() {
            savedEventId = eventService.createEvent(eventCreateDto, firstUserJwt.substring(7)).getId();
        }

        @Test
        @DisplayName("When getting event by id should return HTTP status 404 if event does not exists")
        public void whenGettingEventByIdShouldReturnHttpStatus404IfEventDoesNotExists() throws Exception {
            String authenticationRequestJson = objectMapper.writeValueAsString(new AuthenticationRequest(FIRST_USER_EMAIL, USER_PASSWORD));

            mockMvc.perform(
                            get("/api/v1/events/"+ UUID.randomUUID())
                                    .header("Authorization", secondUserJwt))
                    .andExpect(status().isNotFound());

        }

        @Test
        @DisplayName("When getting event by id should return HTTP status 200 if event exists")
        public void whenGettingEventByIdShouldReturnHttpStatus200IfEventExists() throws Exception {

            mockMvc.perform(
                            get("/api/v1/events/"+ savedEventId)
                                    .header("Authorization", secondUserJwt)
                    )
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "application/json"))
                    .andExpect(jsonPath("$.id").value(savedEventId.toString()))
                    .andExpect(jsonPath("$.shortDescription").value(EVENT_SHORT_DESCRIPTION))
                    .andExpect(jsonPath("$.longDescription").value(EVENT_LONG_DESCRIPTION))
                    .andExpect(jsonPath("$.city").value(EVENT_CITY))
                    .andExpect(jsonPath(
                            "$.eventStartDate",
                            Matchers.equalTo(
                                    objectMapper.writeValueAsString(EVENT_START_DATE.withSecond(0).withNano(0)).replaceAll("\"",""))));

        }

    }

}
