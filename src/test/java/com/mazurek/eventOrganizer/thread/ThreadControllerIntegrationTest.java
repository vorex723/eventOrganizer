package com.mazurek.eventOrganizer.thread;

import tools.jackson.databind.ObjectMapper;
import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationRequest;
import com.mazurek.eventOrganizer.event.EventService;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotEventAttenderException;
import com.mazurek.eventOrganizer.exception.thread.NotThreadOwnerException;
import com.mazurek.eventOrganizer.exception.thread.ThreadNotFoundInEventException;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestDataInitializer;
import com.mazurek.eventOrganizer.testData.builders.AuthenticationRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ThreadCreateDtoTestBuilder;
import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadDto;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ThreadController integration tests:")
public class ThreadControllerIntegrationTest {

    private final AuthenticationRequest firstUserAuthRequest =
            AuthenticationRequestTestBuilder.authenticationRequestForFirstUser().build();
    private final AuthenticationRequest secondUserAuthRequest =
            AuthenticationRequestTestBuilder.authenticationRequestForSecondUser().build();

    @Autowired
    private EventService eventService;
    @Autowired
    private ThreadRepository threadRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private TestDataInitializer testDataInitializer;
    @Autowired
    private DeletionService deletionService;

    private UUID savedEventId;
    private String firstUserJwt;
    private String secondUserJwt;
    private ThreadCreateDto threadCreateDto;
    private ThreadCreateDto threadUpdateDto;

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();
        savedEventId = testDataInitializer.setupFirstEvent();

        firstUserJwt = generateJwt(firstUserAuthRequest);
        secondUserJwt = generateJwt(secondUserAuthRequest);
        threadCreateDto = ThreadCreateDtoTestBuilder.firstThread().build();
        threadUpdateDto = ThreadCreateDtoTestBuilder.firstThreadUpdate().build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
    }

    private String createThreadEndpoint(UUID eventId) {
        return ApiConstants.EVENT_THREADS_URL.replace("{eventId}", eventId.toString());
    }

    private String updateThreadEndpoint(UUID eventId, UUID threadId) {
        return ApiConstants.EVENT_THREAD_BY_ID_URL
                .replace("{eventId}", eventId.toString())
                .replace("{threadId}", threadId.toString());
    }

    private String generateJwt(AuthenticationRequest authenticationRequest) {
        return AuthConstants.JWT_PREFIX +
                authenticationService.authenticate(authenticationRequest, DeviceType.WEB).getAccessToken();
    }

    private void addSecondUserAsEventAttender(UUID eventId) {
        authHelper.setupSecurityContextForSecondUser();
        eventService.addAttenderToEvent(eventId);
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Create thread tests: POST /api/v1/events/{eventId}/threads")
    class CreateNewThreadInEventTests {

        @Test
        @DisplayName("When creating thread should return HTTP 403 Forbidden if there is no Authorization header")
        public void whenCreatingThreadInEventShouldReturnHttpForbiddenIfThereIsNoAuthorizationHeader() throws Exception {
            mockMvc.perform(
                            post(createThreadEndpoint(savedEventId))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(threadCreateDto))
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When creating thread should return HTTP 403 Forbidden if Authorization header is empty")
        public void whenCreatingThreadInEventShouldReturnHttpForbiddenIfAuthorizationHeaderIsEmpty() throws Exception {
            mockMvc.perform(
                            post(createThreadEndpoint(savedEventId))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(threadCreateDto))
                                    .header(ApiConstants.AUTHORIZATION_HEADER, "")
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When creating thread should return HTTP 400 Bad Request if request body is missing")
        public void whenCreatingThreadInEventShouldReturnHttpBadRequestIfRequestBodyIsMissing() throws Exception {
            mockMvc.perform(
                            post(createThreadEndpoint(savedEventId))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("When creating thread should return HTTP 404 Not Found if event does not exist")
        public void whenCreatingThreadInEventShouldReturnHttpNotFoundIfEventDoesNotExist() throws Exception {
            mockMvc.perform(
                            post(createThreadEndpoint(EventConstants.NOT_EXISTING_EVENT_ID))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(threadCreateDto))
                                    .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                    )
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value(EventNotFoundException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When creating thread should return HTTP 400 Bad Request with validation errors for invalid payload")
        public void whenCreatingThreadInEventShouldReturnHttpBadRequestWithErrorsIfDataForCreationIsInvalid() throws Exception {
            threadCreateDto.setName(ThreadConstants.FIRST_THREAD_NAME.substring(0, 5));
            threadCreateDto.setContent(ThreadConstants.FIRST_THREAD_CONTENT.substring(0, 10));

            mockMvc.perform(
                            post(createThreadEndpoint(savedEventId))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(threadCreateDto))
                                    .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.errors").hasJsonPath())
                    .andExpect(jsonPath("$.errors.name").hasJsonPath())
                    .andExpect(jsonPath("$.errors.content").hasJsonPath());
        }

        @Test
        @DisplayName("When creating thread should return HTTP 403 Forbidden if user is not attending event")
        public void whenCreatingThreadInEventShouldReturnHttpBadRequestIfUserIsNotAttendingEvent() throws Exception {
            mockMvc.perform(
                            post(createThreadEndpoint(savedEventId))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(threadCreateDto))
                                    .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt)
                    )
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.message").value(NotEventAttenderException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When creating thread should return HTTP 201 Created on success")
        public void whenCreatingThreadInEventShouldReturnHttpCreatedOnSuccess() throws Exception {
            mockMvc.perform(
                            post(createThreadEndpoint(savedEventId))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(threadCreateDto))
                                    .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                    )
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("When creating thread should return HTTP 201 Created with created dto and correct data")
        public void whenCreatingThreadInEventShouldReturnDtoOfCreatedThreadWithCorrectData() throws Exception {
            User threadOwner = requirePresent(
                    userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL),
                    "Expected first user to exist after auth setup");

            mockMvc.perform(
                            post(createThreadEndpoint(savedEventId))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(threadCreateDto))
                                    .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                    )
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").hasJsonPath())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.eventId").hasJsonPath())
                    .andExpect(jsonPath("$.eventId").value(savedEventId.toString()))

                    .andExpect(jsonPath("$.name").hasJsonPath())
                    .andExpect(jsonPath("$.name").value(threadCreateDto.getName()))

                    .andExpect(jsonPath("$.content").hasJsonPath())
                    .andExpect(jsonPath("$.content").value(threadCreateDto.getContent()))

                    .andExpect(jsonPath("$.createDate").hasJsonPath())
                    .andExpect(jsonPath("$.createDate").isNotEmpty())
                    .andExpect(jsonPath("$.lastUpdate").hasJsonPath())
                    .andExpect(jsonPath("$.lastUpdate").isNotEmpty())
                    .andExpect(jsonPath("$.editCounter").hasJsonPath())
                    .andExpect(jsonPath("$.editCounter").value(0))

                    .andExpect(jsonPath("$.owner").hasJsonPath())
                    .andExpect(jsonPath("$.owner").isNotEmpty())
                    .andExpect(jsonPath("$.owner.id").hasJsonPath())
                    .andExpect(jsonPath("$.owner.id").value(threadOwner.getId().toString()));
        }

        @Test
        @DisplayName("When creating thread should persist all data with relationships on success")
        public void whenCreatingThreadInEventShouldSaveAllDataWithRelationshipsOnSuccess() throws Exception {
            MvcResult mvcResult = mockMvc.perform(
                            post(createThreadEndpoint(savedEventId))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(threadCreateDto))
                                    .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                    )
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            ThreadDto returnedThreadDto = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ThreadDto.class);
            User threadOwner = requirePresent(
                    userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL),
                    "Expected first user to exist after thread creation");

            assertThat(threadRepository.findById(returnedThreadDto.getId())).isPresent();

            Thread createdThread = requirePresent(
                    threadRepository.findById(returnedThreadDto.getId()),
                    "Expected created thread to be persisted");
            assertThat(createdThread.getEvent().getId()).isEqualTo(savedEventId);
            assertThat(createdThread.getOwner().getId()).isEqualTo(threadOwner.getId());
            assertThat(createdThread.getName()).isEqualTo(threadCreateDto.getName());
            assertThat(createdThread.getContent()).isEqualTo(threadCreateDto.getContent());
            assertThat(createdThread.getEditCounter()).isZero();
            assertThat(createdThread.getLastUpdate()).isEqualTo(createdThread.getCreateDate());
        }
    }

    @Nested
    @DisplayName("Update thread tests: PUT /api/v1/events/{eventId}/threads/{threadId}")
    class UpdateThreadInEventTests {

        private UUID savedThreadId;

        @BeforeEach
        void setUp() {
            savedThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);
        }

        @Test
        @DisplayName("When updating thread should return HTTP 403 Forbidden if there is no Authorization header")
        public void whenUpdatingThreadInEventShouldReturnHttpForbiddenIfThereIsNoAuthorizationHeader() throws Exception {
            mockMvc.perform(
                            put(updateThreadEndpoint(savedEventId, savedThreadId))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(threadUpdateDto))
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When updating thread should return HTTP 403 Forbidden if Authorization header is empty")
        public void whenUpdatingThreadInEventShouldReturnHttpForbiddenIfAuthorizationHeaderIsEmpty() throws Exception {
            mockMvc.perform(
                            put(updateThreadEndpoint(savedEventId, savedThreadId))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(threadUpdateDto))
                                    .header(ApiConstants.AUTHORIZATION_HEADER, "")
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When updating thread should return HTTP 400 Bad Request if request body is missing")
        public void whenUpdatingThreadInEventShouldReturnHttpBadRequestIfThereIsNoRequestBody() throws Exception {
            mockMvc.perform(
                            put(updateThreadEndpoint(savedEventId, savedThreadId))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("When updating thread should return HTTP 404 Not Found if event does not exist")
        public void whenUpdatingThreadInEventShouldReturnHttpNotFoundIfThereIsNoEventWithThisId() throws Exception {
            mockMvc.perform(
                            put(updateThreadEndpoint(EventConstants.NOT_EXISTING_EVENT_ID, savedThreadId))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(threadUpdateDto))
                                    .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                    )
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value(EventNotFoundException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When updating thread should return HTTP 404 Not Found if thread does not exist in event")
        public void whenUpdatingThreadInEventShouldReturnHttpNotFoundIfThreadDoesNotExistInEvent() throws Exception {
            mockMvc.perform(
                            put(updateThreadEndpoint(savedEventId, ThreadConstants.THIRD_THREAD_ID))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(threadUpdateDto))
                                    .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                    )
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value(ThreadNotFoundInEventException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When updating thread should return HTTP 404 Not Found if thread belongs to different event")
        public void whenUpdatingThreadInEventShouldReturnHttpNotFoundIfThreadBelongsToDifferentEvent() throws Exception {
            UUID secondEventId = testDataInitializer.setupEventByFirstUser();
            UUID threadFromSecondEventId = testDataInitializer.setupThreadInEventByFirstUser(secondEventId);

            mockMvc.perform(
                            put(updateThreadEndpoint(savedEventId, threadFromSecondEventId))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(threadUpdateDto))
                                    .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                    )
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value(ThreadNotFoundInEventException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When updating thread should return HTTP 400 Bad Request with validation errors for invalid payload")
        public void whenUpdatingThreadInEventShouldReturnHttpBadRequestWithErrorsIfDataForUpdateIsInvalid() throws Exception {
            threadUpdateDto.setName(ThreadConstants.FIRST_THREAD_NAME_UPDATE.substring(0, 5));
            threadUpdateDto.setContent(ThreadConstants.FIRST_THREAD_CONTENT_UPDATE.substring(0, 10));

            mockMvc.perform(
                            put(updateThreadEndpoint(savedEventId, savedThreadId))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(threadUpdateDto))
                                    .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.errors").hasJsonPath())
                    .andExpect(jsonPath("$.errors.name").hasJsonPath())
                    .andExpect(jsonPath("$.errors.content").hasJsonPath());
        }

        @Test
        @DisplayName("When updating thread should return HTTP 403 Forbidden if user is not attending event")
        public void whenUpdatingThreadInEventShouldReturnHttpBadRequestIfUserIsNotAttendingEvent() throws Exception {
            mockMvc.perform(
                            put(updateThreadEndpoint(savedEventId, savedThreadId))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(threadUpdateDto))
                                    .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt)
                    )
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.message").value(NotEventAttenderException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When updating thread should return HTTP 403 Forbidden if user is attending but not thread owner")
        public void whenUpdatingThreadInEventShouldReturnHttpBadRequestIfUserIsNotThreadOwner() throws Exception {
            addSecondUserAsEventAttender(savedEventId);

            mockMvc.perform(
                            put(updateThreadEndpoint(savedEventId, savedThreadId))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(threadUpdateDto))
                                    .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt)
                    )
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.message").value(NotThreadOwnerException.MESSAGE));
        }

        @Test
        @DisplayName("When updating thread should return HTTP 200 OK on success")
        public void whenUpdatingThreadInEventShouldReturnHttpOkOnSuccess() throws Exception {
            mockMvc.perform(
                            put(updateThreadEndpoint(savedEventId, savedThreadId))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(threadUpdateDto))
                                    .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("When updating thread should return HTTP 200 OK with updated dto and correct data")
        public void whenUpdatingThreadInEventShouldReturnUpdatedDataOnSuccess() throws Exception {
            Instant beforeUpdateLastUpdate = requirePresent(
                    threadRepository.findById(savedThreadId),
                    "Expected thread to exist before update")
                    .getLastUpdate();
            User threadOwner = requirePresent(
                    userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL),
                    "Expected first user to exist after auth setup");

            MvcResult mvcResult = mockMvc.perform(
                            put(updateThreadEndpoint(savedEventId, savedThreadId))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(threadUpdateDto))
                                    .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            ThreadDto updatedThread = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ThreadDto.class);

            assertThat(updatedThread.getId()).isEqualTo(savedThreadId);
            assertThat(updatedThread.getEventId()).isEqualTo(savedEventId);
            assertThat(updatedThread.getName()).isEqualTo(threadUpdateDto.getName());
            assertThat(updatedThread.getContent()).isEqualTo(threadUpdateDto.getContent());
            assertThat(updatedThread.getEditCounter()).isEqualTo(1);
            assertThat(updatedThread.getOwner().getId()).isEqualTo(threadOwner.getId());
            assertThat(updatedThread.getLastUpdate()).isAfterOrEqualTo(beforeUpdateLastUpdate);
        }

        @Test
        @DisplayName("When updating thread should persist all updated data in database")
        public void whenUpdatingThreadInEventShouldPersistAllUpdatedDataInDatabase() throws Exception {
            Thread threadBeforeUpdate = requirePresent(
                    threadRepository.findById(savedThreadId),
                    "Expected thread to exist before persistence assertions");
            Instant beforeUpdateCreateDate = threadBeforeUpdate.getCreateDate();
            Instant beforeUpdateLastUpdate = threadBeforeUpdate.getLastUpdate();
            Integer beforeUpdateEditCounter = threadBeforeUpdate.getEditCounter();
            User threadOwner = requirePresent(
                    userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL),
                    "Expected first user to exist after auth setup");

            mockMvc.perform(
                            put(updateThreadEndpoint(savedEventId, savedThreadId))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(threadUpdateDto))
                                    .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                    )
                    .andExpect(status().isOk());

            Thread updatedThread = requirePresent(
                    threadRepository.findById(savedThreadId),
                    "Expected updated thread to exist");

            assertThat(updatedThread.getName()).isEqualTo(threadUpdateDto.getName());
            assertThat(updatedThread.getContent()).isEqualTo(threadUpdateDto.getContent());
            assertThat(updatedThread.getEditCounter()).isEqualTo(beforeUpdateEditCounter + 1);
            assertThat(updatedThread.getCreateDate()).isEqualTo(beforeUpdateCreateDate);
            assertThat(updatedThread.getLastUpdate()).isAfterOrEqualTo(beforeUpdateLastUpdate);
            assertThat(updatedThread.getEvent().getId()).isEqualTo(savedEventId);
            assertThat(updatedThread.getOwner().getId()).isEqualTo(threadOwner.getId());
        }
    }

}
