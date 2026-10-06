package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.ActivationTokenRepository;
import com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryRepository;
import com.mazurek.eventOrganizer.city.cityLookupClient.CityLookupException;
import com.mazurek.eventOrganizer.testData.builders.ResolvedCityTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.RoleTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.RegisterRequestTestBuilder;
import com.mazurek.eventOrganizer.user.RoleRepository;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("City Resolution Integration Test:")
class CityResolutionIntegrationTest {
    @Autowired private CityService service;
    @Autowired private CityRepository cities;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private ActivationTokenRepository tokens;
    @Autowired private AuthEmailDeliveryRepository outbox;
    @Autowired private DeletionService deletion;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @MockitoBean private TestCityLookupClient lookup;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        deletion.deleteAllSafe();
        roles.save(RoleTestBuilder.userRole().id(null).build());
    }

    @ParameterizedTest(name = "[{index}] name={0}")
    @ValueSource(strings = {"Warsaw", "Łęczna"})
    void whenResolvingSelectedCityShouldPreserveCanonicalOrLocalNameInDatabaseAndApi(String name) throws Exception {
        when(lookup.getById("selected-name")).thenReturn(
                new ResolvedCityTestBuilder()
                        .externalId("selected-name")
                        .name(name)
                        .countryCode("PL")
                        .countryName("Poland")
                        .adminArea(null)
                        .latitude(52)
                        .longitude(21)
                        .timeZoneId("Europe/Warsaw")
                        .build());

        City first = service.resolve("selected-name");
        assertThat(first.getName()).isEqualTo(name);
        City persisted = requirePresent(cities.findById(first.getId()),
                "Expected resolved city with canonical or local name to be persisted");
        assertThat(persisted).isNotSameAs(first);
        assertThat(persisted).extracting(City::getId, City::getExternalId, City::getName, City::getCountryCode,
                        City::getAdminArea, City::getLatitude, City::getLongitude, City::getTimeZoneId)
                .containsExactly(first.getId(), "selected-name", name, "PL", null, 52.0, 21.0, "Europe/Warsaw");
        City cached = service.resolve("  selected-name  ");
        assertThat(cached.getId()).isEqualTo(first.getId());
        assertThat(cached.getName()).isEqualTo(name);
        mvc.perform(get("/api/v1/cities/{id}", first.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(first.getId().toString()))
                .andExpect(jsonPath("$.externalId").value("selected-name"))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.eventCount").value(0));

        assertThat(cities.count()).isEqualTo(1);
        verify(lookup, times(1)).getById("selected-name");
        verifyNoMoreInteractions(lookup);
    }

    @AfterEach
    void tearDown() {
        try {
            deletion.deleteAllSafe();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void whenResolvingCityAgainShouldReadPersistedProviderDataWithoutAnotherApiCall() throws Exception {
        when(lookup.getById("selected-place")).thenReturn(
                new ResolvedCityTestBuilder()
                        .externalId("selected-place")
                        .name("New York")
                        .countryCode("us")
                        .countryName("United States")
                        .adminArea(null)
                        .latitude(40.7)
                        .longitude(-74)
                        .timeZoneId("America/New_York")
                        .build());
        City first = service.resolve("selected-place");
        assertThat(service.resolve("  selected-place  ").getId()).isEqualTo(first.getId());
        mvc.perform(get("/api/v1/cities/{id}", first.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalId").value("selected-place"))
                .andExpect(jsonPath("$.name").value("New York"))
                .andExpect(jsonPath("$.countryCode").value("US"))
                .andExpect(jsonPath("$.adminArea").doesNotExist())
                .andExpect(jsonPath("$.latitude").value(40.7))
                .andExpect(jsonPath("$.longitude").value(-74.0))
                .andExpect(jsonPath("$.eventCount").value(0));
        assertThat(cities.count()).isEqualTo(1);
        City persisted = requirePresent(cities.findById(first.getId()),
                "Expected resolved New York city to be persisted before repeated reads");
        assertThat(persisted).isNotSameAs(first);
        assertThat(persisted).extracting(City::getId, City::getExternalId, City::getName, City::getCountryCode,
                        City::getAdminArea, City::getLatitude, City::getLongitude, City::getTimeZoneId)
                .containsExactly(first.getId(), "selected-place", "New York", "US", null, 40.7, -74.0, "America/New_York");
        verify(lookup, times(1)).getById("selected-place");
        verifyNoMoreInteractions(lookup);
    }

    @Test
    void whenRegistrationProviderFailsShouldReturnSanitizedErrorWithoutWrites() throws Exception {
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
        assertThat(outbox.count()).isZero();
        verify(lookup).getById(id);
        verifyNoMoreInteractions(lookup);
    }

    @Test
    void whenAutocompleteProviderFailsShouldReturnSanitizedErrorWithoutWrites() throws Exception {
        when(lookup.search("Wars", null)).thenThrow(new CityLookupException("apiKey=SECRET"));
        mvc.perform(get("/api/v1/cities/search").param("q", "Wars"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("CITY_LOOKUP_FAILED"))
                .andExpect(content().string(not(containsString("SECRET"))));
        assertThat(cities.count()).isZero();
        assertThat(users.count()).isZero();
        assertThat(tokens.count()).isZero();
        assertThat(outbox.count()).isZero();
        verify(lookup).search("Wars", null);
        verifyNoMoreInteractions(lookup);
    }
}
