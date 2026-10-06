package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.exception.ApiErrorCode;
import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestCityData;
import com.mazurek.eventOrganizer.testData.TestDataInitializer;
import com.mazurek.eventOrganizer.testData.builders.CityTestBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.CitiesConstants.*;
import static com.mazurek.eventOrganizer.testData.TestConstants.PaginationConstants.DEFAULT_PAGE_SIZE;
import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("City Controller Integration Test:")
class CityControllerIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private AuthHelper authHelper;
    @Autowired private TestDataInitializer initializer;
    @Autowired private DeletionService deletion;
    @Autowired private CityRepository repository;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        deletion.deleteAllSafe();
        authHelper.setupRolesAndUsers();
        eventId = initializer.setupFirstEvent();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletion.deleteAllSafe();
    }

    private UUID warsawId() {
        return requirePresent(repository.findByExternalId(TestCityData.externalId(WARSAW_NAME)),
                "Expected persisted Warsaw city after event setup").getId();
    }

    @Test
    void whenReadingAnonymousDetailsShouldUseLocalUuidAndIncludeGeography() throws Exception {
        mvc.perform(get("/api/v1/cities/{cityId}", warsawId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(warsawId().toString()))
                .andExpect(jsonPath("$.name").value(WARSAW_NAME))
                .andExpect(jsonPath("$.externalId").value(TestCityData.externalId(WARSAW_NAME)))
                .andExpect(jsonPath("$.countryCode").value("PL"))
                .andExpect(jsonPath("$.adminArea").value(DEFAULT_ADMIN_AREA))
                .andExpect(jsonPath("$.latitude").value(DEFAULT_LATITUDE))
                .andExpect(jsonPath("$.longitude").value(DEFAULT_LONGITUDE))
                .andExpect(jsonPath("$.eventCount").value(1));
    }

    @Test
    void whenCityUuidIsMissingShouldReturnDomainNotFound() throws Exception {
        mvc.perform(get("/api/v1/cities/{cityId}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CITY_NOT_FOUND"));
    }

    @Test
    void whenCityNameIsUsedAsIdentifierShouldRejectRequest() throws Exception {
        mvc.perform(get("/api/v1/cities/warsaw"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.MALFORMED_REQUEST));
    }

    @Test
    void whenReadingAnonymousEventListShouldUseCityUuid() throws Exception {
        mvc.perform(get("/api/v1/cities/{cityId}/events", warsawId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()").value(1))
                .andExpect(jsonPath("$.events[0].id").value(eventId.toString()))
                .andExpect(jsonPath("$.events[0].cityId").value(warsawId().toString()))
                .andExpect(jsonPath("$.events[0].attendeeCount").value(0))
                .andExpect(jsonPath("$.events[0].timeZone").value(DEFAULT_TIME_ZONE_ID))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(DEFAULT_PAGE_SIZE))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.lastPage").value(true));
    }

    @Test
    void whenCityEventListDoesNotExistShouldReturnNotFound() throws Exception {
        mvc.perform(get("/api/v1/cities/{cityId}/events", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.CITY_NOT_FOUND));
    }

    @Test
    void whenCityEventPageIsNegativeShouldRejectRequest() throws Exception {
        mvc.perform(get("/api/v1/cities/{cityId}/events", warsawId()).param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAGE_NUMBER"));
    }

    @Test
    void whenSearchingAnonymouslyShouldReturnResultsWithoutPersistingCities() throws Exception {
        long count = repository.count();
        mvc.perform(get("/api/v1/cities/search").param("q", "New York").param("countryBias", "PL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].externalId").value("test:new york"))
                .andExpect(jsonPath("$[0].displayName").value("new york"))
                .andExpect(jsonPath("$[0].countryCode").value("PL"));
        assertThat(repository.count()).isEqualTo(count);
        assertThat(repository.findByExternalId("test:new york")).isEmpty();
    }

    @Test
    void whenSearchInputIsInvalidShouldRejectMissingQueryAndInvalidCountryBias() throws Exception {
        mvc.perform(get("/api/v1/cities/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_FAILED));
        mvc.perform(get("/api/v1/cities/search").param("q", "Wars").param("countryBias", "POL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.INVALID_ARGUMENT));
    }

    @Test
    void whenSearchQueryIsShortShouldReturnEmptyList() throws Exception {
        mvc.perform(get("/api/v1/cities/search").param("q", "W"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void whenCitiesShareNameShouldKeepTheirUuidAndExternalIdDistinct() throws Exception {
        City first = repository.saveAndFlush(CityTestBuilder.warsaw().id(null).name("Cambridge")
                .externalId("test:cambridge-gb").countryCode("GB").timeZoneId("Europe/London").build());
        City second = repository.saveAndFlush(CityTestBuilder.warsaw().id(null).name("Cambridge")
                .externalId("test:cambridge-us").countryCode("US").timeZoneId("America/New_York").build());
        mvc.perform(get("/api/v1/cities/{cityId}", first.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(first.getId().toString()))
                .andExpect(jsonPath("$.name").value("Cambridge"))
                .andExpect(jsonPath("$.externalId").value("test:cambridge-gb"))
                .andExpect(jsonPath("$.countryCode").value("GB"));
        mvc.perform(get("/api/v1/cities/{cityId}", second.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(second.getId().toString()))
                .andExpect(jsonPath("$.name").value("Cambridge"))
                .andExpect(jsonPath("$.externalId").value("test:cambridge-us"))
                .andExpect(jsonPath("$.countryCode").value("US"));
        assertThat(first.getId()).isNotEqualTo(second.getId());
        assertThat(requirePresent(repository.findByExternalId("test:cambridge-gb"),
                "Expected independently persisted Cambridge/GB").getId()).isEqualTo(first.getId());
        assertThat(requirePresent(repository.findByExternalId("test:cambridge-us"),
                "Expected independently persisted Cambridge/US").getId()).isEqualTo(second.getId());
    }
}
