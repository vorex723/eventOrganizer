package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestCityData;
import com.mazurek.eventOrganizer.testData.TestDataInitializer;
import com.mazurek.eventOrganizer.testData.builders.CityTestBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.CitiesConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CityControllerIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private AuthHelper authHelper;
    @Autowired private TestDataInitializer initializer;
    @Autowired private DeletionService deletion;
    @Autowired private CityRepository repository;

    @BeforeEach
    void setUp() {
        deletion.deleteAllSafe();
        authHelper.setupRolesAndUsers();
        initializer.setupFirstEvent();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletion.deleteAllSafe();
    }

    private UUID warsawId() {
        return repository.findByExternalId(TestCityData.externalId(WARSAW_NAME)).orElseThrow().getId();
    }

    @Test
    void anonymousDetailsUseLocalUuidAndIncludeGeography() throws Exception {
        mvc.perform(get("/api/v1/cities/{cityId}", warsawId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(WARSAW_NAME))
                .andExpect(jsonPath("$.externalId").value(TestCityData.externalId(WARSAW_NAME)))
                .andExpect(jsonPath("$.countryCode").value("PL"))
                .andExpect(jsonPath("$.latitude").isNumber())
                .andExpect(jsonPath("$.eventCount").value(1));
    }

    @Test
    void missingUuidReturnsDomainNotFound() throws Exception {
        mvc.perform(get("/api/v1/cities/{cityId}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CITY_NOT_FOUND"));
    }

    @Test
    void namesAreNotAcceptedAsLocalIdentifiers() throws Exception {
        mvc.perform(get("/api/v1/cities/warsaw")).andExpect(status().isBadRequest());
    }

    @Test
    void anonymousEventListUsesCityUuid() throws Exception {
        mvc.perform(get("/api/v1/cities/{cityId}/events", warsawId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].cityId").value(warsawId().toString()))
                .andExpect(jsonPath("$.events[0].attendeeCount").value(0));
    }

    @Test
    void missingCityEventListReturnsNotFound() throws Exception {
        mvc.perform(get("/api/v1/cities/{cityId}/events", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void eventListRejectsNegativePage() throws Exception {
        mvc.perform(get("/api/v1/cities/{cityId}/events", warsawId()).param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAGE_NUMBER"));
    }

    @Test
    void searchIsPublicAndDoesNotPersistResults() throws Exception {
        long count = repository.count();
        mvc.perform(get("/api/v1/cities/search").param("q", "New York").param("countryBias", "PL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].externalId").value("test:new york"))
                .andExpect(jsonPath("$[0].countryCode").value("PL"));
        assertThat(repository.count()).isEqualTo(count);
    }

    @Test
    void searchRequiresQueryAndRejectsInvalidCountryBias() throws Exception {
        mvc.perform(get("/api/v1/cities/search")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/cities/search").param("q", "Wars").param("countryBias", "POL"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shortQueryReturnsEmptyList() throws Exception {
        mvc.perform(get("/api/v1/cities/search").param("q", "W"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void sameNameCitiesRemainDistinctByUuidAndExternalId() throws Exception {
        City first = repository.saveAndFlush(CityTestBuilder.warsaw().id(null).name("Cambridge")
                .externalId("test:cambridge-gb").countryCode("GB").build());
        City second = repository.saveAndFlush(CityTestBuilder.warsaw().id(null).name("Cambridge")
                .externalId("test:cambridge-us").countryCode("US").build());
        mvc.perform(get("/api/v1/cities/{cityId}", first.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.countryCode").value("GB"));
        mvc.perform(get("/api/v1/cities/{cityId}", second.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.countryCode").value("US"));
        assertThat(first.getId()).isNotEqualTo(second.getId());
    }
}
