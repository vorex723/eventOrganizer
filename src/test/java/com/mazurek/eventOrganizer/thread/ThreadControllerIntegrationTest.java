package com.mazurek.eventOrganizer.thread;

import tools.jackson.databind.ObjectMapper;
import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationRequest;
import com.mazurek.eventOrganizer.common.SortDirection;
import com.mazurek.eventOrganizer.event.EventService;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotEventAttenderException;
import com.mazurek.eventOrganizer.exception.thread.NotThreadOwnerException;
import com.mazurek.eventOrganizer.thread.dto.ThreadOverviewDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadOverviewPageDto;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    private String getThreadsEndpoint(UUID eventId) {
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
            assertThat(createdThread.getEditCount()).isZero();
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
            assertThat(updatedThread.getLastUpdate()).isEqualTo(TimeConstants.NOW);
        }

        @Test
        @DisplayName("When updating thread should persist all updated data in database")
        public void whenUpdatingThreadInEventShouldPersistAllUpdatedDataInDatabase() throws Exception {
            Thread threadBeforeUpdate = requirePresent(
                    threadRepository.findById(savedThreadId),
                    "Expected thread to exist before persistence assertions");
            Instant beforeUpdateCreateDate = threadBeforeUpdate.getCreateDate();
            Integer beforeUpdateEditCounter = threadBeforeUpdate.getEditCount();
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
            assertThat(updatedThread.getEditCount()).isEqualTo(beforeUpdateEditCounter + 1);
            assertThat(updatedThread.getCreateDate()).isEqualTo(beforeUpdateCreateDate);
            assertThat(updatedThread.getLastUpdate()).isEqualTo(TimeConstants.NOW);
            assertThat(updatedThread.getEvent().getId()).isEqualTo(savedEventId);
            assertThat(updatedThread.getOwner().getId()).isEqualTo(threadOwner.getId());
        }
    }


    @Nested
    @DisplayName("Get threads by event id tests: GET /api/v1/events/{eventId}/threads")
    class GetThreadsByEventIdTests{

        private List<UUID> prepareThreadsForEvent(int threadCount, UUID eventId) {
            return IntStream.range(0, threadCount)
                    .mapToObj(threadNumber -> testDataInitializer.setupThreadInEventByFirstUser(eventId))
                    .toList();
        }

        private void setCreateDate(UUID threadId, Instant createDate) {
            Thread thread = requirePresent(
                    threadRepository.findById(threadId),
                    "Expected thread to exist before updating createDate");
            thread.setCreateDate(createDate);
            threadRepository.saveAndFlush(thread);
        }

        private void setLastActivity(UUID threadId, Instant lastActivity) {
            Thread thread = requirePresent(
                    threadRepository.findById(threadId),
                    "Expected thread to exist before updating lastActivity");
            thread.setLastActivity(lastActivity);
            threadRepository.saveAndFlush(thread);
        }

        private void setReplyCount(UUID threadId, int replyCount) {
            Thread thread = requirePresent(
                    threadRepository.findById(threadId),
                    "Expected thread to exist before updating replyCount");
            thread.setReplyCount(replyCount);
            threadRepository.saveAndFlush(thread);
        }

        @Test
        @DisplayName("When getting threads by event id should return HTTP 403 Forbidden if there is no Authorization header")
        public void whenGettingThreadsByEventIdShouldReturnHttpForbiddenIfThereIsNoAuthorizationHeader() throws Exception {
            mockMvc.perform(get(getThreadsEndpoint(savedEventId)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When getting threads by event id should return HTTP 403 Forbidden if Authorization header is empty")
        public void whenGettingThreadsByEventIdShouldReturnHttpForbiddenIfAuthorizationHeaderIsEmpty() throws Exception {
            mockMvc.perform(get(getThreadsEndpoint(savedEventId))
                            .header(ApiConstants.AUTHORIZATION_HEADER, ""))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When getting threads by event id should return HTTP 404 Not Found if event does not exist")
        public void whenGettingThreadsByEventIdShouldReturnHttpNotFoundIfEventDoesNotExist() throws Exception {
            mockMvc.perform(get(getThreadsEndpoint(EventConstants.NOT_EXISTING_EVENT_ID))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(EventNotFoundException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When getting threads by event id should return HTTP 403 Forbidden if user is not attending event")
        public void whenGettingThreadsByEventIdShouldReturnHttpForbiddenIfUserIsNotAttendingEvent() throws Exception {
            mockMvc.perform(get(getThreadsEndpoint(savedEventId))
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.message").value(NotEventAttenderException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When getting threads by event id should return HTTP 400 Bad Request if page is negative")
        public void whenGettingThreadsByEventIdShouldReturnHttpBadRequestIfPageIsNegative() throws Exception {
            mockMvc.perform(get(getThreadsEndpoint(savedEventId))
                            .param("page", String.valueOf(PaginationConstants.PAGE_MINUS_ONE))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.message").value(InvalidPageNumberException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When getting threads by event id should return HTTP 400 Bad Request if page is not numeric")
        public void whenGettingThreadsByEventIdShouldReturnHttpBadRequestIfPageIsNotNumeric() throws Exception {
            mockMvc.perform(get(getThreadsEndpoint(savedEventId))
                            .param("page", "abc")
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("When getting threads by event id should return HTTP 400 Bad Request if sortBy is invalid")
        public void whenGettingThreadsByEventIdShouldReturnHttpBadRequestIfSortByIsInvalid() throws Exception {
            mockMvc.perform(get(getThreadsEndpoint(savedEventId))
                            .param("sortBy", "INVALID_SORT")
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("When getting threads by event id should return HTTP 400 Bad Request if direction is invalid")
        public void whenGettingThreadsByEventIdShouldReturnHttpBadRequestIfDirectionIsInvalid() throws Exception {
            mockMvc.perform(get(getThreadsEndpoint(savedEventId))
                            .param("direction", "SIDEWAYS")
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("When getting threads by event id should return HTTP 200 OK with empty page and default query params")
        public void whenGettingThreadsByEventIdShouldReturnHttpOkWithEmptyPageAndDefaultQueryParams() throws Exception {
            mockMvc.perform(get(getThreadsEndpoint(savedEventId))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.threads.length()").value(0))
                    .andExpect(jsonPath("$.pageNumber").value(PaginationConstants.PAGE_ZERO))
                    .andExpect(jsonPath("$.pageSize").value(PaginationConstants.DEFAULT_PAGE_SIZE))
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.totalPages").value(0))
                    .andExpect(jsonPath("$.lastPage").value(true));
        }

        @Test
        @DisplayName("When getting threads by event id should use default sort params on non-empty response")
        public void whenGettingThreadsByEventIdShouldUseDefaultSortParamsOnNonEmptyResponse() throws Exception {
            UUID newestThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);
            UUID middleThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);
            UUID oldestThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);

            setLastActivity(newestThreadId, TimeConstants.NOW);
            setLastActivity(middleThreadId, TimeConstants.ONE_HOUR_AGO);
            setLastActivity(oldestThreadId, TimeConstants.TWO_HOURS_AGO);

            MvcResult mvcResult = mockMvc.perform(get(getThreadsEndpoint(savedEventId))
                            .param("page", String.valueOf(PaginationConstants.PAGE_ZERO))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            ThreadOverviewPageDto output = objectMapper.readValue(
                    mvcResult.getResponse().getContentAsString(),
                    ThreadOverviewPageDto.class
            );

            assertThat(output.threads().stream().map(ThreadOverviewDto::id).toList())
                    .containsExactly(newestThreadId, middleThreadId, oldestThreadId);
        }

        @Test
        @DisplayName("When getting threads by event id should return HTTP 200 OK with threads from requested event only and correct dto fields")
        public void whenGettingThreadsByEventIdShouldReturnHttpOkWithThreadsFromRequestedEventOnlyAndCorrectDtoFields() throws Exception {
            List<UUID> savedEventThreadIds = prepareThreadsForEvent(PaginationConstants.TEN_ELEMENTS, savedEventId);
            UUID threadWithRepliesId = savedEventThreadIds.getFirst();
            int expectedReplyCount = 7;
            setReplyCount(threadWithRepliesId, expectedReplyCount);
            UUID secondEventId = testDataInitializer.setupEventByFirstUser();
            prepareThreadsForEvent(PaginationConstants.TEN_ELEMENTS, secondEventId);

            MvcResult mvcResult = mockMvc.perform(get(getThreadsEndpoint(savedEventId))
                            .param("page", String.valueOf(PaginationConstants.PAGE_ZERO))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.threads.length()").value(PaginationConstants.TEN_ELEMENTS))
                    .andExpect(jsonPath("$.pageNumber").value(PaginationConstants.PAGE_ZERO))
                    .andExpect(jsonPath("$.totalElements").value(PaginationConstants.TEN_ELEMENTS))
                    .andExpect(jsonPath("$.threads[0].replyCount").hasJsonPath())
                    .andReturn();

            ThreadOverviewPageDto output = objectMapper.readValue(
                    mvcResult.getResponse().getContentAsString(),
                    ThreadOverviewPageDto.class
            );
            Set<UUID> savedEventThreadIdSet = Set.copyOf(savedEventThreadIds);
            ThreadOverviewDto threadWithReplies = requirePresent(
                    output.threads().stream().filter(thread -> thread.id().equals(threadWithRepliesId)).findFirst(),
                    "Expected thread with custom reply count to be present in returned page");

            assertThat(output.threads()).allSatisfy(thread -> {
                assertThat(thread.eventId()).isEqualTo(savedEventId);
                assertThat(savedEventThreadIdSet).contains(thread.id());
                assertThat(thread.owner()).isNotNull();
                assertThat(thread.owner().getId()).isNotNull();
                assertThat(thread.name()).isNotBlank();
                assertThat(thread.replyCount()).isGreaterThanOrEqualTo(0);
                assertThat(thread.lastActivity()).isNotNull();
                assertThat(thread.createDate()).isNotNull();
            });
            assertThat(threadWithReplies.replyCount()).isEqualTo(expectedReplyCount);
        }

        @Test
        @DisplayName("When getting threads by event id should respect sortBy and direction query parameters")
        public void whenGettingThreadsByEventIdShouldRespectSortByAndDirectionQueryParameters() throws Exception {
            UUID newestThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);
            UUID middleThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);
            UUID oldestThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);

            setCreateDate(newestThreadId, TimeConstants.NOW);
            setCreateDate(middleThreadId, TimeConstants.ONE_HOUR_AGO);
            setCreateDate(oldestThreadId, TimeConstants.TWO_HOURS_AGO);

            MvcResult mvcResult = mockMvc.perform(get(getThreadsEndpoint(savedEventId))
                            .param("page", String.valueOf(PaginationConstants.PAGE_ZERO))
                            .param("sortBy", ThreadSortField.CREATE_DATE.name())
                            .param("direction", SortDirection.ASC.name())
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            ThreadOverviewPageDto output = objectMapper.readValue(
                    mvcResult.getResponse().getContentAsString(),
                    ThreadOverviewPageDto.class
            );

            assertThat(output.threads().stream().map(ThreadOverviewDto::id).toList())
                    .containsExactly(oldestThreadId, middleThreadId, newestThreadId);
        }

        @Test
        @DisplayName("When getting threads by event id should respect pagination across multiple pages")
        public void whenGettingThreadsByEventIdShouldRespectPaginationAcrossMultiplePages() throws Exception {
            prepareThreadsForEvent(PaginationConstants.DEFAULT_PAGE_SIZE + 1, savedEventId);

            MvcResult firstPageResult = mockMvc.perform(get(getThreadsEndpoint(savedEventId))
                            .param("page", String.valueOf(PaginationConstants.PAGE_ZERO))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();
            MvcResult secondPageResult = mockMvc.perform(get(getThreadsEndpoint(savedEventId))
                            .param("page", String.valueOf(PaginationConstants.PAGE_ONE))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            ThreadOverviewPageDto firstPage = objectMapper.readValue(
                    firstPageResult.getResponse().getContentAsString(),
                    ThreadOverviewPageDto.class
            );
            ThreadOverviewPageDto secondPage = objectMapper.readValue(
                    secondPageResult.getResponse().getContentAsString(),
                    ThreadOverviewPageDto.class
            );

            assertThat(firstPage.threads()).hasSize(PaginationConstants.DEFAULT_PAGE_SIZE);
            assertThat(firstPage.pageNumber()).isEqualTo(PaginationConstants.PAGE_ZERO);
            assertThat(firstPage.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
            assertThat(firstPage.totalElements()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE + 1L);
            assertThat(firstPage.totalPages()).isEqualTo(2);
            assertThat(firstPage.lastPage()).isFalse();

            assertThat(secondPage.threads()).hasSize(1);
            assertThat(secondPage.pageNumber()).isEqualTo(PaginationConstants.PAGE_ONE);
            assertThat(secondPage.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
            assertThat(secondPage.totalElements()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE + 1L);
            assertThat(secondPage.totalPages()).isEqualTo(2);
            assertThat(secondPage.lastPage()).isTrue();
            assertThat(firstPage.threads().stream().map(ThreadOverviewDto::id).toList())
                    .doesNotContainAnyElementsOf(secondPage.threads().stream().map(ThreadOverviewDto::id).toList());
        }
    }
}
