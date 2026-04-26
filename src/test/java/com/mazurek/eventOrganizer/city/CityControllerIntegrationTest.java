package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationRequest;
import com.mazurek.eventOrganizer.exception.city.CityNotFoundException;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestDataInitializer;
import com.mazurek.eventOrganizer.testData.builders.AuthenticationRequestTestBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("CityController integration tests:")
public class CityControllerIntegrationTest {

    private final AuthenticationRequest firstUserAuthRequest =
            AuthenticationRequestTestBuilder.authenticationRequestForFirstUser().build();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private TestDataInitializer testDataInitializer;
    @Autowired
    private DeletionService deletionService;

    private String firstUserJwt;

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();
        testDataInitializer.setupFirstEvent();

        firstUserJwt = AuthConstants.JWT_PREFIX +
                authenticationService.authenticate(firstUserAuthRequest, DeviceType.WEB).getAccessToken();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
    }

    @Nested
    @DisplayName("Get city by name tests: GET /api/v1/cities/{cityName}")
    class GetCityByNameTests {

        @Test
        @DisplayName("When getting city by name should return HTTP 403 Forbidden if authorization header is missing")
        public void whenGettingCityByNameShouldReturnForbiddenIfAuthorizationHeaderIsMissing() throws Exception {
            mockMvc.perform(get(ApiConstants.CITY_BY_NAME_URL, CitiesConstants.WARSAW_NAME))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When getting city by name should return HTTP 404 Not Found if city does not exist")
        public void whenGettingCityByNameShouldReturnNotFoundIfCityDoesNotExist() throws Exception {
            mockMvc.perform(get(ApiConstants.CITY_BY_NAME_URL, CitiesConstants.SYSTEM_CITY_NAME)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(CityNotFoundException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When getting city by name should return HTTP 200 OK with city dto and correct data")
        public void whenGettingCityByNameShouldReturnCityDtoWithCorrectData() throws Exception {
            mockMvc.perform(get(ApiConstants.CITY_BY_NAME_URL, CitiesConstants.WARSAW_NAME)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.name").value(CitiesConstants.WARSAW_NAME))
                    .andExpect(jsonPath("$.id").isNotEmpty());
        }
    }
}
