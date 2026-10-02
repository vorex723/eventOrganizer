package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationResponse;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static com.mazurek.eventOrganizer.testData.TestConstants.ApiConstants.*;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.FIRST_USER_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Full API smoke flow with real persistence and a deterministic, offline provider fixture. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CityTimeZoneFlowIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private RoleRepository roles;
    @Autowired private UserRepository users;
    @Autowired private CityRepository cities;
    @Autowired private RecordingEmailService emails;
    @Autowired private DeletionService deletion;
    @MockitoSpyBean private TestCityLookupClient lookup;

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
        String bearer = "Bearer " + mapper.readValue(login.getResponse().getContentAsString(), AuthenticationResponse.class).getAccessToken();
        mvc.perform(get("/api/v1/users/me").header(AUTHORIZATION_HEADER, bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.timeZone").value("America/New_York"));

        var create = EventCreateDtoTestBuilder.firstEvent().cityExternalId("test:warsaw").build();
        assertThat(mapper.readTree(mapper.writeValueAsString(create)).has("timeZone")).isFalse();
        var created = mvc.perform(post(EVENTS_URL).header(AUTHORIZATION_HEADER, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(create)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.timeZone").value("Europe/Warsaw"))
                .andReturn();
        var event = mapper.readValue(created.getResponse().getContentAsString(), EventDto.class);

        var update = EventCreateDtoTestBuilder.firstEvent().cityExternalId("test:new york").build();
        mvc.perform(put(EVENT_BY_ID_URL, event.getId()).header(AUTHORIZATION_HEADER, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(update)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.timeZone").value("America/New_York"));
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
