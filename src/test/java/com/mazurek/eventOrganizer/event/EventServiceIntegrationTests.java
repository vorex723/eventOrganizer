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
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.tag.TagNotFoundException;
import com.mazurek.eventOrganizer.exception.user.UserAlreadyExistException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.jwt.JwtUtil;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.tag.TagRepository;
import com.mazurek.eventOrganizer.thread.ThreadReplyRepository;
import com.mazurek.eventOrganizer.thread.ThreadRepository;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
public class EventServiceIntegrationTests {


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

    private final String EVENT_CITY_NOT_EXISTING = "Warszawa";



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
    @Transactional
    class CoreEventTests{

        @Nested
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

                assertEquals(1, eventOwner.getUserEvents().size());
                assertEquals(eventOwner.getUserEvents().get(0).getId(), eventDto.getId());
                assertEquals(EVENT_NAME, eventDto.getName());
                assertEquals(EVENT_SHORT_DESCRIPTION, eventDto.getShortDescription());
                assertEquals(EVENT_LONG_DESCRIPTION, eventDto.getLongDescription());
                assertTrue(eventDto.getEventStartDate().isEqual(EVENT_START_DATE.withSecond(0).withNano(0)));
                assertEquals(EVENT_CITY, eventDto.getCity());
                assertEquals(EVENT_EXACT_ADDRESS, eventDto.getExactAddress());
                assertEquals(eventOwner.getId(), eventDto.getOwner().getId());
                assertEquals(eventOwner.getFirstName(), eventDto.getOwner().getFirstName());
                assertEquals(eventOwner.getLastName(), eventDto.getOwner().getLastName());
                assertEquals(eventOwner.getHomeCity().getName(), eventDto.getOwner().getHomeCity());
                eventCreateDto.getTags().forEach(tag -> assertTrue(eventDto.getTags().contains(tag.toLowerCase())));
            }
        }
    }


}
