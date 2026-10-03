package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationResponse;
import com.mazurek.eventOrganizer.auth.dto.RefreshTokenRequest;
import com.mazurek.eventOrganizer.event.dto.EventDto;
import com.mazurek.eventOrganizer.notification.service.RecordingEmailService;
import com.mazurek.eventOrganizer.testData.builders.AuthenticationRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ChangeUserDetailsDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.EventCreateDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.RegisterRequestTestBuilder;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.RoleRepository;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Full API smoke flow with real persistence and a deterministic, offline provider fixture. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"server.address=127.0.0.1", "app.auth.email.worker-enabled=false"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CityTimeZoneFlowIntegrationTest {
    @LocalServerPort private int port;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private RoleRepository roles;
    @Autowired private UserRepository users;
    @Autowired private CityRepository cities;
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
        roles.save(new Role("ROLE_USER"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletion.deleteAllSafe();
    }

    @Test
    void registrationEventsAndProfileRespectGeographicTimezoneAndIndependentUserPreference() throws Exception {
        try (HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            var health = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/actuator/health"))
                    .timeout(Duration.ofSeconds(5)).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(health.statusCode()).isEqualTo(200);
            assertThat(mapper.readTree(health.body()).path("status").asString()).isEqualTo("UP");
            var autocomplete = http.send(HttpRequest.newBuilder(URI.create(
                    "http://127.0.0.1:" + port + "/api/v1/cities/search?q=New%20York"))
                    .timeout(Duration.ofSeconds(5)).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(autocomplete.statusCode()).isEqualTo(200);
            assertThat(mapper.readTree(autocomplete.body()).get(0).path("externalId").asString()).isEqualTo("test:new york");
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
        assertThat(users.findByEmail(FIRST_USER_EMAIL).orElseThrow().getTimeZone()).isEqualTo("America/New_York");
        mvc.perform(post(AUTH_ACTIVATE_URL, emails.lastActivationToken(FIRST_USER_EMAIL))).andExpect(status().isOk());

        var login = mvc.perform(post(AUTH_LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(AuthenticationRequestTestBuilder.authenticationRequestForFirstUser().build())))
                .andExpect(status().isOk()).andReturn();
        var initialSession = mapper.readValue(login.getResponse().getContentAsString(), AuthenticationResponse.class);
        var refreshed = mvc.perform(post(AUTH_REFRESH_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new RefreshTokenRequest(initialSession.getRefreshToken()))))
                .andExpect(status().isOk()).andReturn();
        var refreshedSession = mapper.readValue(refreshed.getResponse().getContentAsString(), AuthenticationResponse.class);
        assertThat(refreshedSession.getRefreshToken()).isNotEqualTo(initialSession.getRefreshToken());
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

        var attendeeRegistration = RegisterRequestTestBuilder.secondUserRegisterRequest()
                .homeCityExternalId("test:warsaw").build();
        mvc.perform(post(AUTH_REGISTER_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(attendeeRegistration)))
                .andExpect(status().isCreated());
        mvc.perform(post(AUTH_ACTIVATE_URL, emails.lastActivationToken(SECOND_USER_EMAIL))).andExpect(status().isOk());
        var attendeeLogin = mvc.perform(post(AUTH_LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(AuthenticationRequestTestBuilder.authenticationRequestForSecondUser().build())))
                .andExpect(status().isOk()).andReturn();
        String attendeeBearer = "Bearer " + mapper.readValue(attendeeLogin.getResponse().getContentAsString(),
                AuthenticationResponse.class).getAccessToken();
        mvc.perform(post(EVENT_ATTEND_URL, event.getId()).header(AUTHORIZATION_HEADER, attendeeBearer))
                .andExpect(status().isNoContent());

        var update = EventCreateDtoTestBuilder.firstEvent().cityExternalId("test:new york").build();
        mvc.perform(put(EVENT_BY_ID_URL, event.getId()).header(AUTHORIZATION_HEADER, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(update)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.timeZone").value("America/New_York"));
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
        var user = users.findByEmail(FIRST_USER_EMAIL).orElseThrow();
        assertThat(user.getHomeCity().getTimeZoneId()).isEqualTo("Europe/Warsaw");
        assertThat(user.getTimeZone()).isEqualTo("Asia/Tokyo");
        mvc.perform(get("/api/v1/users/me").header(AUTHORIZATION_HEADER, bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.timeZone").value("Asia/Tokyo"));

        var changedCity = ChangeUserDetailsDtoTestBuilder.validUpdate()
                .homeCityExternalId("test:new york").timeZone("Asia/Tokyo").build();
        mvc.perform(put(USER_UPDATE_DETAILS_URL).header(AUTHORIZATION_HEADER, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(changedCity)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.timeZone").value("Asia/Tokyo"));
        var userWithChangedCity = users.findByEmail(FIRST_USER_EMAIL).orElseThrow();
        assertThat(userWithChangedCity.getHomeCity().getTimeZoneId()).isEqualTo("America/New_York");
        assertThat(userWithChangedCity.getTimeZone()).isEqualTo("Asia/Tokyo");

        verify(lookup, times(1)).getById("test:new york");
        verify(lookup, times(1)).getById("test:warsaw");
    }
}
