package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationResponse;
import com.mazurek.eventOrganizer.event.dto.EventDto;
import com.mazurek.eventOrganizer.event.EventRepository;
import com.mazurek.eventOrganizer.notification.service.RecordingEmailService;
import com.mazurek.eventOrganizer.testData.builders.AuthenticationRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.RoleTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ChangeUserDetailsDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.EventCreateDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.RefreshTokenRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.RegisterRequestTestBuilder;
import com.mazurek.eventOrganizer.user.RoleRepository;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static com.mazurek.eventOrganizer.testData.TestConstants.ApiConstants.*;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.FIRST_USER_EMAIL;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.SECOND_USER_EMAIL;
import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Full API smoke flow with real persistence and a deterministic, offline provider fixture. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"server.address=127.0.0.1", "app.auth.email.worker-enabled=false"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("City Time Zone Flow Integration Test:")
class CityTimeZoneFlowIntegrationTest {
    @LocalServerPort private int port;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private RoleRepository roles;
    @Autowired private UserRepository users;
    @Autowired private CityRepository cities;
    @Autowired private EventRepository events;
    @Autowired private RecordingEmailService emails;
    @Autowired private DeletionService deletion;
    @MockitoSpyBean private TestCityLookupClient lookup;

    @DynamicPropertySource
    static void loadActualManagementConfiguration(DynamicPropertyRegistry registry) throws IOException {
        var properties = PropertiesLoaderUtils.loadProperties(
                new FileSystemResource("src/main/resources/application.properties"));
        properties.stringPropertyNames().stream().filter(key -> key.startsWith("management."))
                .forEach(key -> registry.add(key, () -> properties.getProperty(key)));
    }

    @BeforeEach
    void setUp() {
        deletion.deleteAllSafe();
        SecurityContextHolder.clearContext();
        roles.save(RoleTestBuilder.userRole().id(null).build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletion.deleteAllSafe();
    }

    @Test
    void whenRegisteringAndUpdatingEventAndProfileShouldRespectCityTimeZoneAndIndependentUserPreference() throws Exception {
        try (HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            var health = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/actuator/health"))
                    .timeout(Duration.ofSeconds(5)).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(health.statusCode()).isEqualTo(200);
            assertThat(mapper.readTree(health.body()).path("status").asString()).isEqualTo("UP");
            var autocomplete = http.send(HttpRequest.newBuilder(URI.create(
                    "http://127.0.0.1:" + port + "/api/v1/cities/search?q=New%20York"))
                    .timeout(Duration.ofSeconds(5)).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(autocomplete.statusCode()).isEqualTo(200);
            var results = mapper.readTree(autocomplete.body());
            assertThat(results.isArray()).isTrue();
            assertThat(results.size()).isEqualTo(1);
            assertThat(results.path(0).path("externalId").asString()).isEqualTo("test:new york");
        }
        mvc.perform(get("/api/v1/cities/search").param("q", "New York"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].externalId").value("test:new york"));
        assertThat(cities.count()).isZero();

        var registration = RegisterRequestTestBuilder.firstUserRegisterRequest()
                .homeCityExternalId("test:new york").build();
        assertThat(mapper.readTree(mapper.writeValueAsString(registration)).has("timeZone")).isFalse();
        mvc.perform(post(AUTH_REGISTER_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(registration)))
                .andExpect(status().isCreated());
        var registered = requirePresent(users.findByEmail(FIRST_USER_EMAIL),
                "Expected first user after New York registration");
        assertThat(registered.getTimeZone()).isEqualTo("America/New_York");
        assertThat(registered.getHomeCity().getExternalId()).isEqualTo("test:new york");
        assertThat(registered.getHomeCity().getTimeZoneId()).isEqualTo("America/New_York");
        var activation = emails.lastActivationToken(FIRST_USER_EMAIL);
        assertThat(activation).as("First user activation token").isNotNull();
        mvc.perform(post(AUTH_ACTIVATE_URL, activation)).andExpect(status().isOk());

        var login = mvc.perform(post(AUTH_LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(AuthenticationRequestTestBuilder.authenticationRequestForFirstUser().build())))
                .andExpect(status().isOk()).andReturn();
        var initialSession = mapper.readValue(login.getResponse().getContentAsString(), AuthenticationResponse.class);
        assertThat(initialSession).isNotNull();
        assertThat(initialSession.getRefreshToken()).isNotBlank();
        var refreshed = mvc.perform(post(AUTH_REFRESH_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new RefreshTokenRequestTestBuilder()
                                .refreshToken(initialSession.getRefreshToken())
                                .firebaseInstallationId(null)
                                .build())))
                .andExpect(status().isOk()).andReturn();
        var refreshedSession = mapper.readValue(refreshed.getResponse().getContentAsString(), AuthenticationResponse.class);
        assertThat(refreshedSession).isNotNull();
        assertThat(refreshedSession.getRefreshToken()).isNotBlank().isNotEqualTo(initialSession.getRefreshToken());
        assertThat(refreshedSession.getAccessToken()).isNotBlank();
        String bearer = "Bearer " + refreshedSession.getAccessToken();
        mvc.perform(get("/api/v1/users/me").header(AUTHORIZATION_HEADER, bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.timeZone").value("America/New_York"));

        var create = EventCreateDtoTestBuilder.firstEvent().cityExternalId("test:warsaw").build();
        assertThat(mapper.readTree(mapper.writeValueAsString(create)).has("timeZone")).isFalse();
        var created = mvc.perform(post(EVENTS_URL).header(AUTHORIZATION_HEADER, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(create)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.timeZone").value("Europe/Warsaw"))
                .andReturn();
        var event = mapper.readValue(created.getResponse().getContentAsString(), EventDto.class);
        assertThat(event).isNotNull();
        assertThat(event.getId()).isNotNull();
        assertThat(event.getCityExternalId()).isEqualTo("test:warsaw");
        var persistedEvent = requirePresent(events.findById(event.getId()),
                "Expected Warsaw event after HTTP creation");
        assertThat(persistedEvent.getCity().getExternalId()).isEqualTo("test:warsaw");
        assertThat(persistedEvent.getCity().getTimeZoneId()).isEqualTo("Europe/Warsaw");
        assertThat(persistedEvent.getEventStartDate()).isEqualTo(create.getEventStartDate());

        var attendeeRegistration = RegisterRequestTestBuilder.secondUserRegisterRequest()
                .homeCityExternalId("test:warsaw").build();
        mvc.perform(post(AUTH_REGISTER_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(attendeeRegistration)))
                .andExpect(status().isCreated());
        var attendeeActivation = emails.lastActivationToken(SECOND_USER_EMAIL);
        assertThat(attendeeActivation).as("Attendee activation token").isNotNull();
        mvc.perform(post(AUTH_ACTIVATE_URL, attendeeActivation)).andExpect(status().isOk());
        var attendeeLogin = mvc.perform(post(AUTH_LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(AuthenticationRequestTestBuilder.authenticationRequestForSecondUser().build())))
                .andExpect(status().isOk()).andReturn();
        var attendeeSession = mapper.readValue(attendeeLogin.getResponse().getContentAsString(), AuthenticationResponse.class);
        assertThat(attendeeSession).isNotNull();
        assertThat(attendeeSession.getAccessToken()).isNotBlank();
        String attendeeBearer = "Bearer " + attendeeSession.getAccessToken();
        mvc.perform(post(EVENT_ATTEND_URL, event.getId()).header(AUTHORIZATION_HEADER, attendeeBearer))
                .andExpect(status().isNoContent());

        var update = EventCreateDtoTestBuilder.firstEvent().cityExternalId("test:new york").build();
        mvc.perform(put(EVENT_BY_ID_URL, event.getId()).header(AUTHORIZATION_HEADER, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(update)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.timeZone").value("America/New_York"));
        var updatedEvent = requirePresent(events.findById(event.getId()),
                "Expected New York event after HTTP update");
        assertThat(updatedEvent).isNotSameAs(persistedEvent);
        assertThat(updatedEvent.getCity().getExternalId()).isEqualTo("test:new york");
        assertThat(updatedEvent.getCity().getTimeZoneId()).isEqualTo("America/New_York");
        assertThat(updatedEvent.getEventStartDate()).isEqualTo(create.getEventStartDate());
        assertThat(updatedEvent.getAttendeeCount()).isEqualTo(1);
        mvc.perform(get(NOTIFICATIONS_URL).header(AUTHORIZATION_HEADER, attendeeBearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.notifications[0].resourceId").value(event.getId().toString()))
                .andExpect(jsonPath("$.notifications[0].read").value(false));
        mvc.perform(get(NOTIFICATIONS_UNREAD_COUNT_URL).header(AUTHORIZATION_HEADER, attendeeBearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(1));
        mvc.perform(patch(NOTIFICATIONS_READ_ALL_URL).header(AUTHORIZATION_HEADER, attendeeBearer))
                .andExpect(status().isNoContent());
        mvc.perform(get(NOTIFICATIONS_UNREAD_COUNT_URL).header(AUTHORIZATION_HEADER, attendeeBearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(0));
        mvc.perform(get(EVENT_BY_ID_URL, event.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.timeZone").value("America/New_York"));
        mvc.perform(get(EVENTS_URL)).andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].timeZone").value("America/New_York"));

        var profile = ChangeUserDetailsDtoTestBuilder.validUpdate()
                .homeCityExternalId("test:warsaw").timeZone("Asia/Tokyo").build();
        mvc.perform(put(USER_UPDATE_DETAILS_URL).header(AUTHORIZATION_HEADER, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(profile)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.timeZone").value("Asia/Tokyo"));
        var user = requirePresent(users.findByEmail(FIRST_USER_EMAIL),
                "Expected first user after changing preference to Asia/Tokyo");
        assertThat(user.getHomeCity().getExternalId()).isEqualTo("test:warsaw");
        assertThat(user.getHomeCity().getTimeZoneId()).isEqualTo("Europe/Warsaw");
        assertThat(user.getTimeZone()).isEqualTo("Asia/Tokyo");
        mvc.perform(get("/api/v1/users/me").header(AUTHORIZATION_HEADER, bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.timeZone").value("Asia/Tokyo"));

        var changedCity = ChangeUserDetailsDtoTestBuilder.validUpdate()
                .homeCityExternalId("test:new york").timeZone("Asia/Tokyo").build();
        mvc.perform(put(USER_UPDATE_DETAILS_URL).header(AUTHORIZATION_HEADER, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(changedCity)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.timeZone").value("Asia/Tokyo"));
        var userWithChangedCity = requirePresent(users.findByEmail(FIRST_USER_EMAIL),
                "Expected first user after changing home city to New York");
        assertThat(userWithChangedCity).isNotSameAs(user);
        assertThat(userWithChangedCity.getHomeCity().getExternalId()).isEqualTo("test:new york");
        assertThat(userWithChangedCity.getHomeCity().getTimeZoneId()).isEqualTo("America/New_York");
        assertThat(userWithChangedCity.getTimeZone()).isEqualTo("Asia/Tokyo");
        var eventAfterProfileChanges = requirePresent(events.findById(event.getId()),
                "Expected event to retain city and start instant independently of user preference");
        assertThat(eventAfterProfileChanges.getCity().getExternalId()).isEqualTo("test:new york");
        assertThat(eventAfterProfileChanges.getCity().getTimeZoneId()).isEqualTo("America/New_York");
        assertThat(eventAfterProfileChanges.getEventStartDate()).isEqualTo(create.getEventStartDate());
        assertThat(cities.count()).isEqualTo(2);
        assertThat(events.count()).isEqualTo(1);

        verify(lookup, times(1)).getById("test:new york");
        verify(lookup, times(1)).getById("test:warsaw");
    }
}
