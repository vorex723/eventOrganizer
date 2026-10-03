package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.ActivationTokenRepository;
import com.mazurek.eventOrganizer.city.cityLookupClient.CityLookupException;
import com.mazurek.eventOrganizer.city.cityLookupClient.ResolvedCity;
import com.mazurek.eventOrganizer.testData.builders.dto.RegisterRequestTestBuilder;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.RoleRepository;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CityResolutionIntegrationTest {
    @Autowired private CityService service;
    @Autowired private CityRepository cities;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private ActivationTokenRepository tokens;
    @Autowired private DeletionService deletion;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @MockitoBean private TestCityLookupClient lookup;

    @BeforeEach
    void setUp() {
        deletion.deleteAllSafe();
        roles.save(new Role("ROLE_USER"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Warsaw", "Łęczna"})
    void selectedCanonicalOrLocalNameIsPreservedInDatabaseAndApi(String name) throws Exception {
        when(lookup.getById("selected-name")).thenReturn(
                new ResolvedCity("selected-name", name, "PL", "Poland", null, 52, 21, "Europe/Warsaw"));

        City first = service.resolve("selected-name");
        assertThat(first.getName()).isEqualTo(name);
        assertThat(cities.findById(first.getId()).orElseThrow().getName()).isEqualTo(name);
        City cached = service.resolve("  selected-name  ");
        assertThat(cached.getId()).isEqualTo(first.getId());
        assertThat(cached.getName()).isEqualTo(name);
        mvc.perform(get("/api/v1/cities/{id}", first.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value(name));

        assertThat(cities.count()).isEqualTo(1);
        verify(lookup, times(1)).getById("selected-name");
        verifyNoMoreInteractions(lookup);
    }

    @AfterEach
    void tearDown() {
        deletion.deleteAllSafe();
    }

    @Test
    void repeatedResolveAndDetailReadUsePersistedProviderDataWithoutAnotherApiCall() throws Exception {
        when(lookup.getById("selected-place")).thenReturn(
                new ResolvedCity("selected-place", "New York", "us", "United States", null, 40.7, -74, "America/New_York"));
        City first = service.resolve("selected-place");
        assertThat(service.resolve("  selected-place  ").getId()).isEqualTo(first.getId());
        mvc.perform(get("/api/v1/cities/{id}", first.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalId").value("selected-place"))
                .andExpect(jsonPath("$.name").value("New York"))
                .andExpect(jsonPath("$.countryCode").value("US"))
                .andExpect(jsonPath("$.latitude").value(40.7));
        assertThat(cities.count()).isEqualTo(1);
        assertThat(cities.findById(first.getId()).orElseThrow().getTimeZoneId()).isEqualTo("America/New_York");
        verify(lookup, times(1)).getById("selected-place");
        verifyNoMoreInteractions(lookup);
    }

    @Test
    void providerFailureReturnsSanitizedErrorAndRegistrationDoesNotPersistAnything() throws Exception {
        String id = "test:unavailable";
        when(lookup.getById(id)).thenThrow(new CityLookupException("https://provider.invalid/?apiKey=SECRET"));
        var request = RegisterRequestTestBuilder.firstUserRegisterRequest().homeCityExternalId(id).build();
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("CITY_LOOKUP_FAILED"))
                .andExpect(content().string(not(containsString("SECRET"))));
        assertThat(cities.count()).isZero();
        assertThat(users.count()).isZero();
        assertThat(tokens.count()).isZero();
    }

    @Test
    void autocompleteProviderFailureUsesSameErrorContractWithoutWrites() throws Exception {
        when(lookup.search("Wars", null)).thenThrow(new CityLookupException("apiKey=SECRET"));
        mvc.perform(get("/api/v1/cities/search").param("q", "Wars"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("CITY_LOOKUP_FAILED"))
                .andExpect(content().string(not(containsString("SECRET"))));
        assertThat(cities.count()).isZero();
    }
}
