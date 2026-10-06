package com.mazurek.eventOrganizer.tag;

import com.mazurek.eventOrganizer.exception.ApiErrorCode;
import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationRequest;
import com.mazurek.eventOrganizer.exception.tag.TagNotFoundException;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestDataInitializer;
import com.mazurek.eventOrganizer.testData.builders.AuthenticationRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.TagTestBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.UUID;
import com.mazurek.eventOrganizer.testData.TestPersistenceQueries;
import static org.assertj.core.api.Assertions.assertThat;
import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("TagController integration tests:")
public class TagControllerIntegrationTest {

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
    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TestPersistenceQueries persistenceQueries;

    private String firstUserJwt;
    private UUID firstEventId;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();
        firstEventId = testDataInitializer.setupFirstEvent();

        firstUserJwt = AuthConstants.JWT_PREFIX +
                authenticationService.authenticate(firstUserAuthRequest, DeviceType.WEB).getAccessToken();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
    }

    @Nested
    @DisplayName("Get tag by name tests: GET /api/v1/tags/{tagName}")
    class GetTagByNameTests {

        @Test
        @DisplayName("When getting tag by name without authentication should return tag details")
        public void whenGettingTagByNameWithoutAuthenticationShouldReturnTag() throws Exception {
            var beforeRead = persistenceQueries.tagState();
            Tag storedTag = requirePresent(tagRepository.findByIgnoreCaseName(TagConstants.FIRST_TAG_NAME), "First event tag must exist");
            mockMvc.perform(get(ApiConstants.TAG_BY_NAME_URL, TagConstants.FIRST_TAG_NAME))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(storedTag.getId().toString()))
                    .andExpect(jsonPath("$.eventCount").value(1))
                    .andExpect(jsonPath("$.name").value(TagConstants.FIRST_TAG_NAME));
            assertThat(persistenceQueries.tagState()).as("GET must not mutate tag/event rows").isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting tag by name should return HTTP 404 Not Found if tag does not exist")
        public void whenGettingTagByNameShouldReturnNotFoundIfTagDoesNotExist() throws Exception {
            var beforeRead = persistenceQueries.tagState();
            mockMvc.perform(get(ApiConstants.TAG_BY_NAME_URL, TagConstants.FOURTH_TAG_NAME)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.TAG_NOT_FOUND))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(TagNotFoundException.DEFAULT_MESSAGE));
            assertThat(persistenceQueries.tagState()).as("GET must not mutate tag/event rows").isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting tag by name should return HTTP 200 OK with tag dto and correct data")
        public void whenGettingTagByNameShouldReturnTagDtoWithCorrectData() throws Exception {
            var beforeRead = persistenceQueries.tagState();
            Tag storedTag = requirePresent(tagRepository.findByIgnoreCaseName(TagConstants.FIRST_TAG_NAME), "First event tag must exist");
            mockMvc.perform(get(ApiConstants.TAG_BY_NAME_URL, TagConstants.FIRST_TAG_NAME)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.name").value(TagConstants.FIRST_TAG_NAME))
                    .andExpect(jsonPath("$.id").value(storedTag.getId().toString()))
                    .andExpect(jsonPath("$.eventCount").value(1));
            assertThat(persistenceQueries.tagState()).as("GET must not mutate tag/event rows").isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting tag by encoded multi-word name should decode the path segment")
        public void whenGettingTagByEncodedMultiWordNameShouldDecodePathSegment() throws Exception {
            Tag savedTag = tagRepository.saveAndFlush(new TagTestBuilder()
                    .id(null)
                    .name("web development")
                    .build());
            var beforeRead = persistenceQueries.tagState();

            mockMvc.perform(get(URI.create("/api/v1/tags/web%20development"))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(savedTag.getId().toString()))
                    .andExpect(jsonPath("$.eventCount").value(0))
                    .andExpect(jsonPath("$.name").value("web development"));
            assertThat(persistenceQueries.tagState()).as("GET must not mutate tag/event rows").isEqualTo(beforeRead);
        }
    }

    @Test
    @DisplayName("When getting tag events without authentication should return event overviews")
    void whenGettingTagEventsWithoutAuthenticationShouldReturnEvents() throws Exception {
        var beforeRead = persistenceQueries.tagState();
        mockMvc.perform(get("/api/v1/tags/{tagName}/events", TagConstants.FIRST_TAG_NAME))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.events.length()").value(1))
                .andExpect(jsonPath("$.events[0].id").value(firstEventId.toString()))
                .andExpect(jsonPath("$.events[0].name").value(EventConstants.FIRST_EVENT_NAME))
                .andExpect(jsonPath("$.events[0].attendeeCount").value(0));
        assertThat(persistenceQueries.tagState()).as("GET must not mutate tag/event rows").isEqualTo(beforeRead);
    }
}
