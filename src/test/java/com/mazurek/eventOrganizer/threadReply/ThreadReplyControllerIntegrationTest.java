package com.mazurek.eventOrganizer.threadReply;

import tools.jackson.databind.ObjectMapper;
import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationRequest;
import com.mazurek.eventOrganizer.event.EventService;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotEventAttenderException;
import com.mazurek.eventOrganizer.exception.thread.NotThreadReplyOwnerException;
import com.mazurek.eventOrganizer.exception.thread.ReplyNotFoundInThreadException;
import com.mazurek.eventOrganizer.exception.thread.ThreadNotFoundInEventException;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestDataInitializer;
import com.mazurek.eventOrganizer.testData.builders.AuthenticationRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ThreadReplyCreateDtoTestBuilder;
import com.mazurek.eventOrganizer.thread.ThreadRepository;
import com.mazurek.eventOrganizer.threadReply.dto.ThreadReplyCreateDto;
import com.mazurek.eventOrganizer.threadReply.dto.ThreadReplyDto;
import com.mazurek.eventOrganizer.threadReply.dto.ThreadReplyPageDto;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;
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

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ThreadReplyController integration tests:")
public class ThreadReplyControllerIntegrationTest {

    private final AuthenticationRequest firstUserAuthRequest =
            AuthenticationRequestTestBuilder.authenticationRequestForFirstUser().build();
    private final AuthenticationRequest secondUserAuthRequest =
            AuthenticationRequestTestBuilder.authenticationRequestForSecondUser().build();

    @Autowired
    private EventService eventService;
    @Autowired
    private ThreadRepository threadRepository;
    @Autowired
    private ThreadReplyRepository threadReplyRepository;
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
    private ThreadReplyCreateDto threadReplyCreateDto;
    private ThreadReplyCreateDto threadReplyUpdateDto;

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();
        savedEventId = testDataInitializer.setupFirstEvent();

        firstUserJwt = generateJwt(firstUserAuthRequest);
        secondUserJwt = generateJwt(secondUserAuthRequest);
        threadReplyCreateDto = ThreadReplyCreateDtoTestBuilder.firstReply().build();
        threadReplyUpdateDto = ThreadReplyCreateDtoTestBuilder.firstReplyUpdate()
                .replyContent(ThreadReplyConstants.CONTROLLER_THREAD_REPLY_CONTENT_UPDATE)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
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

    // ===========================================================================================
    // POST /api/v1/events/{eventId}/threads/{threadId}/replies
    // ===========================================================================================

    @Nested
    @DisplayName("Create reply tests: POST /api/v1/events/{eventId}/threads/{threadId}/replies")
    class CreateReplyInThreadTests {

        private UUID savedThreadId;

        @BeforeEach
        void setUp() {
            savedThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);
        }

        @Test
        @DisplayName("When creating reply should return HTTP 403 Forbidden if there is no Authorization header")
        public void whenCreatingReplyShouldReturnForbiddenIfThereIsNoAuthorizationHeader() throws Exception {
            mockMvc.perform(post(ApiConstants.EVENT_THREAD_REPLIES_URL, savedEventId, savedThreadId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(threadReplyCreateDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When creating reply should return HTTP 403 Forbidden if Authorization header is empty")
        public void whenCreatingReplyShouldReturnForbiddenIfAuthorizationHeaderIsEmpty() throws Exception {
            mockMvc.perform(post(ApiConstants.EVENT_THREAD_REPLIES_URL, savedEventId, savedThreadId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(threadReplyCreateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, InvalidInputConstants.EMPTY_VALUE))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When creating reply should return HTTP 400 Bad Request if request body is missing")
        public void whenCreatingReplyShouldReturnBadRequestIfRequestBodyIsMissing() throws Exception {
            mockMvc.perform(post(ApiConstants.EVENT_THREAD_REPLIES_URL, savedEventId, savedThreadId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("When creating reply should return HTTP 404 Not Found if event does not exist")
        public void whenCreatingReplyShouldReturnNotFoundIfEventDoesNotExist() throws Exception {
            mockMvc.perform(post(ApiConstants.EVENT_THREAD_REPLIES_URL, EventConstants.NOT_EXISTING_EVENT_ID, savedThreadId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(threadReplyCreateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(EventNotFoundException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When creating reply should return HTTP 404 Not Found if thread does not exist in event")
        public void whenCreatingReplyShouldReturnNotFoundIfThreadDoesNotExistInEvent() throws Exception {
            mockMvc.perform(post(ApiConstants.EVENT_THREAD_REPLIES_URL, savedEventId, ThreadConstants.THIRD_THREAD_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(threadReplyCreateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(ThreadNotFoundInEventException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When creating reply should return HTTP 404 Not Found if thread belongs to different event")
        public void whenCreatingReplyShouldReturnNotFoundIfThreadBelongsToDifferentEvent() throws Exception {
            UUID secondEventId = testDataInitializer.setupEventByFirstUser();
            UUID threadFromSecondEventId = testDataInitializer.setupThreadInEventByFirstUser(secondEventId);

            mockMvc.perform(post(ApiConstants.EVENT_THREAD_REPLIES_URL, savedEventId, threadFromSecondEventId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(threadReplyCreateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(ThreadNotFoundInEventException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When creating reply should return HTTP 400 Bad Request with validation errors if payload is invalid")
        public void whenCreatingReplyShouldReturnBadRequestWithValidationErrorsIfPayloadIsInvalid() throws Exception {
            threadReplyCreateDto.setReplyContent(ThreadReplyConstants.FIRST_REPLY_CONTENT.substring(0, 5));

            mockMvc.perform(post(ApiConstants.EVENT_THREAD_REPLIES_URL, savedEventId, savedThreadId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(threadReplyCreateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.errors").hasJsonPath())
                    .andExpect(jsonPath("$.errors.replyContent").hasJsonPath());
        }

        @Test
        @DisplayName("When creating reply should return HTTP 403 Forbidden if performing user is not attending event")
        public void whenCreatingReplyShouldReturnForbiddenIfPerformingUserIsNotAttendingEvent() throws Exception {
            mockMvc.perform(post(ApiConstants.EVENT_THREAD_REPLIES_URL, savedEventId, savedThreadId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(threadReplyCreateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.message").value(NotEventAttenderException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When creating reply should return HTTP 201 Created on success")
        public void whenCreatingReplyShouldReturnCreatedOnSuccess() throws Exception {
            mockMvc.perform(post(ApiConstants.EVENT_THREAD_REPLIES_URL, savedEventId, savedThreadId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(threadReplyCreateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("When creating reply should return HTTP 201 Created with reply dto and correct data")
        public void whenCreatingReplyShouldReturnDtoWithCorrectDataOnSuccess() throws Exception {
            User replier = requirePresent(
                    userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL),
                    "Expected first user to exist after auth setup");

            mockMvc.perform(post(ApiConstants.EVENT_THREAD_REPLIES_URL, savedEventId, savedThreadId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(threadReplyCreateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.threadId").value(savedThreadId.toString()))
                    .andExpect(jsonPath("$.content").value(threadReplyCreateDto.getReplyContent()))
                    .andExpect(jsonPath("$.replyDate").isNotEmpty())
                    .andExpect(jsonPath("$.lastUpdate").isNotEmpty())
                    .andExpect(jsonPath("$.editCounter").value(0))
                    .andExpect(jsonPath("$.replier.id").value(replier.getId().toString()));
        }

        @Test
        @DisplayName("When creating reply should persist reply with correct data and relationships")
        public void whenCreatingReplyShouldPersistReplyWithCorrectDataAndRelationships() throws Exception {
            MvcResult mvcResult = mockMvc.perform(post(ApiConstants.EVENT_THREAD_REPLIES_URL, savedEventId, savedThreadId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(threadReplyCreateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isCreated())
                    .andReturn();

            ThreadReplyDto returnedDto = objectMapper.readValue(
                    mvcResult.getResponse().getContentAsString(), ThreadReplyDto.class);
            ThreadReply savedReply = requirePresent(
                    threadReplyRepository.findById(returnedDto.getId()),
                    "Expected created reply to be persisted");
            User replier = requirePresent(
                    userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL),
                    "Expected first user to exist after auth setup");
            Set<ThreadReply> threadReplies = threadReplyRepository.findByThreadId(savedThreadId);
            Set<ThreadReply> userReplies = threadReplyRepository.findByReplierId(replier.getId());

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(savedReply.getThread().getId())
                        .as("Reply should be linked to correct thread")
                        .isEqualTo(savedThreadId);
                softly.assertThat(savedReply.getContent())
                        .as("Reply content should match dto")
                        .isEqualTo(threadReplyCreateDto.getReplyContent());
                softly.assertThat(savedReply.getEditCounter())
                        .as("Edit counter should be zero on creation")
                        .isZero();
                softly.assertThat(savedReply.getReplyDate())
                        .as("Reply date and last update should be equal on creation")
                        .isEqualTo(savedReply.getLastUpdate());
                softly.assertThat(savedReply.getReplier().getId())
                        .as("Replier should be the performing user")
                        .isEqualTo(replier.getId());
                softly.assertThat(threadReplies)
                        .as("Reply should be present in thread replies")
                        .contains(savedReply);
                softly.assertThat(userReplies)
                        .as("Reply should be present in user replies")
                        .contains(savedReply);
            });
        }
    }

    // ===========================================================================================
    // PUT /api/v1/events/{eventId}/threads/{threadId}/replies/{replyId}
    // ===========================================================================================

    @Nested
    @DisplayName("Update reply tests: PUT /api/v1/events/{eventId}/threads/{threadId}/replies/{replyId}")
    class UpdateReplyInThreadTests {

        private UUID savedThreadId;
        private UUID savedReplyId;

        @BeforeEach
        void setUp() {
            savedThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);
            savedReplyId = testDataInitializer.setupThreadReplyInThreadByFirstUser(savedEventId, savedThreadId);
        }

        @Test
        @DisplayName("When updating reply should return HTTP 403 Forbidden if there is no Authorization header")
        public void whenUpdatingReplyShouldReturnForbiddenIfThereIsNoAuthorizationHeader() throws Exception {
            mockMvc.perform(put(ApiConstants.EVENT_THREAD_REPLY_BY_ID_URL, savedEventId, savedThreadId, savedReplyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(threadReplyUpdateDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When updating reply should return HTTP 403 Forbidden if Authorization header is empty")
        public void whenUpdatingReplyShouldReturnForbiddenIfAuthorizationHeaderIsEmpty() throws Exception {
            mockMvc.perform(put(ApiConstants.EVENT_THREAD_REPLY_BY_ID_URL, savedEventId, savedThreadId, savedReplyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(threadReplyUpdateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, InvalidInputConstants.EMPTY_VALUE))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When updating reply should return HTTP 400 Bad Request if request body is missing")
        public void whenUpdatingReplyShouldReturnBadRequestIfRequestBodyIsMissing() throws Exception {
            mockMvc.perform(put(ApiConstants.EVENT_THREAD_REPLY_BY_ID_URL, savedEventId, savedThreadId, savedReplyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("When updating reply should return HTTP 404 Not Found if event does not exist")
        public void whenUpdatingReplyShouldReturnNotFoundIfEventDoesNotExist() throws Exception {
            mockMvc.perform(put(ApiConstants.EVENT_THREAD_REPLY_BY_ID_URL, EventConstants.NOT_EXISTING_EVENT_ID, savedThreadId, savedReplyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(threadReplyUpdateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(EventNotFoundException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When updating reply should return HTTP 404 Not Found if thread does not exist in event")
        public void whenUpdatingReplyShouldReturnNotFoundIfThreadDoesNotExistInEvent() throws Exception {
            mockMvc.perform(put(ApiConstants.EVENT_THREAD_REPLY_BY_ID_URL, savedEventId, ThreadConstants.THIRD_THREAD_ID, savedReplyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(threadReplyUpdateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(ThreadNotFoundInEventException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When updating reply should return HTTP 404 Not Found if thread belongs to different event")
        public void whenUpdatingReplyShouldReturnNotFoundIfThreadBelongsToDifferentEvent() throws Exception {
            UUID secondEventId = testDataInitializer.setupEventByFirstUser();

            mockMvc.perform(put(ApiConstants.EVENT_THREAD_REPLY_BY_ID_URL, secondEventId, savedThreadId, savedReplyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(threadReplyUpdateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(ThreadNotFoundInEventException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When updating reply should return HTTP 404 Not Found if reply does not exist in thread")
        public void whenUpdatingReplyShouldReturnNotFoundIfReplyDoesNotExistInThread() throws Exception {
            mockMvc.perform(put(ApiConstants.EVENT_THREAD_REPLY_BY_ID_URL, savedEventId, savedThreadId, ThreadReplyConstants.THIRD_REPLY_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(threadReplyUpdateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(ReplyNotFoundInThreadException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When updating reply should return HTTP 404 Not Found if reply belongs to different thread")
        public void whenUpdatingReplyShouldReturnNotFoundIfReplyBelongsToDifferentThread() throws Exception {
            UUID secondThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);
            UUID secondReplyId = testDataInitializer.setupThreadReplyInThreadByFirstUser(savedEventId, secondThreadId);

            mockMvc.perform(put(ApiConstants.EVENT_THREAD_REPLY_BY_ID_URL, savedEventId, savedThreadId, secondReplyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(threadReplyUpdateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(ReplyNotFoundInThreadException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When updating reply should return HTTP 400 Bad Request with validation errors if payload is invalid")
        public void whenUpdatingReplyShouldReturnBadRequestWithValidationErrorsIfPayloadIsInvalid() throws Exception {
            threadReplyUpdateDto.setReplyContent(ThreadReplyConstants.CONTROLLER_THREAD_REPLY_CONTENT.substring(0, 5));

            mockMvc.perform(put(ApiConstants.EVENT_THREAD_REPLY_BY_ID_URL, savedEventId, savedThreadId, savedReplyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(threadReplyUpdateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.errors").hasJsonPath())
                    .andExpect(jsonPath("$.errors.replyContent").hasJsonPath());
        }

        @Test
        @DisplayName("When updating reply should return HTTP 403 Forbidden if performing user is not attending event")
        public void whenUpdatingReplyShouldReturnForbiddenIfPerformingUserIsNotAttendingEvent() throws Exception {
            mockMvc.perform(put(ApiConstants.EVENT_THREAD_REPLY_BY_ID_URL, savedEventId, savedThreadId, savedReplyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(threadReplyUpdateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.message").value(NotEventAttenderException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When updating reply should return HTTP 403 Forbidden if performing user is attending but does not own reply")
        public void whenUpdatingReplyShouldReturnForbiddenIfPerformingUserIsAttendingButDoesNotOwnReply() throws Exception {
            addSecondUserAsEventAttender(savedEventId);

            mockMvc.perform(put(ApiConstants.EVENT_THREAD_REPLY_BY_ID_URL, savedEventId, savedThreadId, savedReplyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(threadReplyUpdateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.message").value(NotThreadReplyOwnerException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When updating reply should return HTTP 200 OK on success")
        public void whenUpdatingReplyShouldReturnOkOnSuccess() throws Exception {
            mockMvc.perform(put(ApiConstants.EVENT_THREAD_REPLY_BY_ID_URL, savedEventId, savedThreadId, savedReplyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(threadReplyUpdateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("When updating reply should return HTTP 200 OK with updated reply dto and correct data")
        public void whenUpdatingReplyShouldReturnDtoWithCorrectUpdatedDataOnSuccess() throws Exception {
            ThreadReply replyBeforeUpdate = requirePresent(
                    threadReplyRepository.findById(savedReplyId),
                    "Expected reply to exist before update");
            User replier = requirePresent(
                    userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL),
                    "Expected first user to exist after auth setup");

            MvcResult mvcResult = mockMvc.perform(put(ApiConstants.EVENT_THREAD_REPLY_BY_ID_URL, savedEventId, savedThreadId, savedReplyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(threadReplyUpdateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andReturn();

            ThreadReplyDto updatedDto = objectMapper.readValue(
                    mvcResult.getResponse().getContentAsString(), ThreadReplyDto.class);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(updatedDto.getId())
                        .as("Returned dto should have correct reply id")
                        .isEqualTo(savedReplyId);
                softly.assertThat(updatedDto.getThreadId())
                        .as("Returned dto should have correct thread id")
                        .isEqualTo(savedThreadId);
                softly.assertThat(updatedDto.getContent())
                        .as("Returned dto should have updated content")
                        .isEqualTo(threadReplyUpdateDto.getReplyContent());
                softly.assertThat(updatedDto.getEditCounter())
                        .as("Edit counter should be incremented to one")
                        .isEqualTo(1);
                softly.assertThat(updatedDto.getReplier().getId())
                        .as("Replier should remain unchanged")
                        .isEqualTo(replier.getId());
                softly.assertThat(updatedDto.getReplyDate())
                        .as("Reply date should not change on update")
                        .isEqualTo(replyBeforeUpdate.getReplyDate());
                softly.assertThat(updatedDto.getLastUpdate())
                        .as("Last update should use the fixed application clock")
                        .isEqualTo(TimeConstants.NOW);
            });
        }

        @Test
        @DisplayName("When updating reply should persist all updated data in database")
        public void whenUpdatingReplyShouldPersistAllUpdatedDataInDatabase() throws Exception {
            ThreadReply originalReply = requirePresent(
                    threadReplyRepository.findById(savedReplyId),
                    "Expected reply to exist before persistence assertions");
            Instant originalReplyDate = originalReply.getReplyDate();
            int originalEditCounter = originalReply.getEditCounter();
            User replier = requirePresent(
                    userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL),
                    "Expected first user to exist after auth setup");

            mockMvc.perform(put(ApiConstants.EVENT_THREAD_REPLY_BY_ID_URL, savedEventId, savedThreadId, savedReplyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(threadReplyUpdateDto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk());

            ThreadReply updatedReply = requirePresent(
                    threadReplyRepository.findById(savedReplyId),
                    "Expected updated reply to exist");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(updatedReply.getContent())
                        .as("Reply content should be updated")
                        .isEqualTo(threadReplyUpdateDto.getReplyContent());
                softly.assertThat(updatedReply.getEditCounter())
                        .as("Edit counter should be incremented by one")
                        .isEqualTo(originalEditCounter + 1);
                softly.assertThat(updatedReply.getReplyDate())
                        .as("Reply date should not change on update")
                        .isEqualTo(originalReplyDate);
                softly.assertThat(updatedReply.getLastUpdate())
                        .as("Last update should use the fixed application clock")
                        .isEqualTo(TimeConstants.NOW);
                softly.assertThat(updatedReply.getThread().getId())
                        .as("Thread should remain unchanged")
                        .isEqualTo(savedThreadId);
                softly.assertThat(updatedReply.getReplier().getId())
                        .as("Replier should remain unchanged")
                        .isEqualTo(replier.getId());
            });
        }
    }

    // ===========================================================================================
    // GET /api/v1/events/{eventId}/threads/{threadId}/replies
    // ===========================================================================================

    @Nested
    @DisplayName("Get replies tests: GET /api/v1/events/{eventId}/threads/{threadId}/replies")
    class GetRepliesInThreadTests {

        private UUID savedThreadId;

        @BeforeEach
        void setUp() {
            savedThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);
        }

        private void setReplyDate(UUID replyId, Instant replyDate) {
            ThreadReply reply = requirePresent(
                    threadReplyRepository.findById(replyId),
                    "Expected reply to exist before updating replyDate");
            reply.setReplyDate(replyDate);
            threadReplyRepository.saveAndFlush(reply);
        }

        private void setLastUpdate(UUID replyId, Instant lastUpdate) {
            ThreadReply reply = requirePresent(
                    threadReplyRepository.findById(replyId),
                    "Expected reply to exist before updating lastUpdate");
            reply.setLastUpdate(lastUpdate);
            threadReplyRepository.saveAndFlush(reply);
        }

        private List<UUID> getReplyIds(ThreadReplyPageDto output) {
            return output.replies().stream()
                    .map(ThreadReplyDto::getId)
                    .toList();
        }

        @Test
        @DisplayName("When getting replies should return HTTP 403 Forbidden if there is no Authorization header")
        public void whenGettingRepliesShouldReturnForbiddenIfThereIsNoAuthorizationHeader() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_THREAD_REPLIES_URL, savedEventId, savedThreadId))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When getting replies should return HTTP 403 Forbidden if Authorization header is empty")
        public void whenGettingRepliesShouldReturnForbiddenIfAuthorizationHeaderIsEmpty() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_THREAD_REPLIES_URL, savedEventId, savedThreadId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, InvalidInputConstants.EMPTY_VALUE))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When getting replies should return HTTP 404 Not Found if event does not exist")
        public void whenGettingRepliesShouldReturnNotFoundIfEventDoesNotExist() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_THREAD_REPLIES_URL, EventConstants.NOT_EXISTING_EVENT_ID, savedThreadId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(EventNotFoundException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When getting replies should return HTTP 403 Forbidden if performing user is not attending event")
        public void whenGettingRepliesShouldReturnForbiddenIfPerformingUserIsNotAttendingEvent() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_THREAD_REPLIES_URL, savedEventId, savedThreadId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.message").value(NotEventAttenderException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When getting replies should return HTTP 404 Not Found if thread does not exist in event")
        public void whenGettingRepliesShouldReturnNotFoundIfThreadDoesNotExistInEvent() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_THREAD_REPLIES_URL, savedEventId, ThreadConstants.NOT_EXISTING_THREAD_ID)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(ThreadNotFoundInEventException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When getting replies should return HTTP 404 Not Found if thread belongs to different event")
        public void whenGettingRepliesShouldReturnNotFoundIfThreadBelongsToDifferentEvent() throws Exception {
            UUID secondEventId = testDataInitializer.setupEventByFirstUser();
            UUID secondEventThreadId = testDataInitializer.setupThreadInEventByFirstUser(secondEventId);

            mockMvc.perform(get(ApiConstants.EVENT_THREAD_REPLIES_URL, savedEventId, secondEventThreadId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(ThreadNotFoundInEventException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When getting replies should return HTTP 400 Bad Request if page is negative")
        public void whenGettingRepliesShouldReturnBadRequestIfPageIsNegative() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_THREAD_REPLIES_URL, savedEventId, savedThreadId)
                            .param("page", String.valueOf(PaginationConstants.PAGE_MINUS_ONE))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.message").value(InvalidPageNumberException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When getting replies should return HTTP 400 Bad Request if page is not numeric")
        public void whenGettingRepliesShouldReturnBadRequestIfPageIsNotNumeric() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_THREAD_REPLIES_URL, savedEventId, savedThreadId)
                            .param("page", "abc")
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("When getting replies should return HTTP 200 OK with empty page and default query params")
        public void whenGettingRepliesShouldReturnOkWithEmptyPageAndDefaultQueryParams() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_THREAD_REPLIES_URL, savedEventId, savedThreadId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.replies").isArray())
                    .andExpect(jsonPath("$.replies").isEmpty())
                    .andExpect(jsonPath("$.pageNumber").value(PaginationConstants.PAGE_ZERO))
                    .andExpect(jsonPath("$.pageSize").value(PaginationConstants.DEFAULT_PAGE_SIZE))
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.totalPages").value(0))
                    .andExpect(jsonPath("$.lastPage").value(true));
        }

        @Test
        @DisplayName("When getting replies should return HTTP 200 OK with replies from requested thread only and correct dto fields")
        public void whenGettingRepliesShouldReturnOkWithRepliesFromRequestedThreadOnlyAndCorrectDtoFields() throws Exception {
            addSecondUserAsEventAttender(savedEventId);

            UUID olderReplyId = testDataInitializer.setupThreadReplyInThreadByFirstUser(
                    savedEventId,
                    savedThreadId,
                    ThreadReplyConstants.FIRST_REPLY_CONTENT
            );
            UUID newerReplyId = testDataInitializer.setupThreadReplyInThreadBySecondUser(
                    savedEventId,
                    savedThreadId,
                    ThreadReplyConstants.SECOND_REPLY_CONTENT
            );
            UUID secondThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);
            UUID secondEventId = testDataInitializer.setupEventByFirstUser();
            UUID threadFromSecondEventId = testDataInitializer.setupThreadInEventByFirstUser(secondEventId);

            testDataInitializer.setupThreadReplyInThreadByFirstUser(savedEventId, secondThreadId, ThreadReplyConstants.THIRD_REPLY_CONTENT);
            testDataInitializer.setupThreadReplyInThreadByFirstUser(secondEventId, threadFromSecondEventId, ThreadReplyConstants.OLD_REPLY_CONTENT);

            setReplyDate(olderReplyId, TimeConstants.TWO_HOURS_AGO);
            setReplyDate(newerReplyId, TimeConstants.ONE_HOUR_AGO);
            setLastUpdate(olderReplyId, TimeConstants.TWO_HOURS_AGO);
            setLastUpdate(newerReplyId, TimeConstants.NOW);

            MvcResult mvcResult = mockMvc.perform(get(ApiConstants.EVENT_THREAD_REPLIES_URL, savedEventId, savedThreadId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.replies.length()").value(2))
                    .andExpect(jsonPath("$.pageNumber").value(PaginationConstants.PAGE_ZERO))
                    .andExpect(jsonPath("$.pageSize").value(PaginationConstants.DEFAULT_PAGE_SIZE))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.lastPage").value(true))
                    .andReturn();

            ThreadReplyPageDto output = objectMapper.readValue(
                    mvcResult.getResponse().getContentAsString(),
                    ThreadReplyPageDto.class
            );
            ThreadReplyDto firstReply = output.replies().getFirst();
            ThreadReply storedOlderReply = requirePresent(
                    threadReplyRepository.findById(olderReplyId),
                    "Expected older reply to exist for dto assertions");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(getReplyIds(output))
                        .as("Replies should be returned oldest first and only from requested thread")
                        .containsExactly(olderReplyId, newerReplyId);
                softly.assertThat(output.replies())
                        .allSatisfy(reply -> softly.assertThat(reply.getThreadId()).isEqualTo(savedThreadId));
                softly.assertThat(firstReply.getId())
                        .as("First dto should represent the oldest reply")
                        .isEqualTo(olderReplyId);
                softly.assertThat(firstReply.getContent())
                        .as("First dto should preserve content")
                        .isEqualTo(ThreadReplyConstants.FIRST_REPLY_CONTENT);
                softly.assertThat(firstReply.getReplyDate())
                        .as("First dto should preserve reply date")
                        .isEqualTo(TimeConstants.TWO_HOURS_AGO);
                softly.assertThat(firstReply.getLastUpdate())
                        .as("First dto should preserve last update")
                        .isEqualTo(TimeConstants.TWO_HOURS_AGO);
                softly.assertThat(firstReply.getEditCounter())
                        .as("First dto should preserve edit counter")
                        .isZero();
                softly.assertThat(firstReply.getReplier().getId())
                        .as("First dto should preserve replier id")
                        .isEqualTo(storedOlderReply.getReplier().getId());
            });
        }

        @Test
        @DisplayName("When getting replies should return HTTP 200 OK for event attender who does not own thread")
        public void whenGettingRepliesShouldReturnOkForEventAttenderWhoDoesNotOwnThread() throws Exception {
            addSecondUserAsEventAttender(savedEventId);
            testDataInitializer.setupThreadReplyInThreadByFirstUser(savedEventId, savedThreadId);

            mockMvc.perform(get(ApiConstants.EVENT_THREAD_REPLIES_URL, savedEventId, savedThreadId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.replies.length()").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("When getting replies should return HTTP 200 OK with replies sorted by reply date ascending")
        public void whenGettingRepliesShouldReturnOkWithRepliesSortedByReplyDateAscending() throws Exception {
            UUID newestReplyId = testDataInitializer.setupThreadReplyInThreadByFirstUser(
                    savedEventId,
                    savedThreadId,
                    ThreadReplyConstants.FIRST_REPLY_CONTENT
            );
            UUID middleReplyId = testDataInitializer.setupThreadReplyInThreadByFirstUser(
                    savedEventId,
                    savedThreadId,
                    ThreadReplyConstants.SECOND_REPLY_CONTENT
            );
            UUID oldestReplyId = testDataInitializer.setupThreadReplyInThreadByFirstUser(
                    savedEventId,
                    savedThreadId,
                    ThreadReplyConstants.THIRD_REPLY_CONTENT
            );

            setReplyDate(newestReplyId, TimeConstants.NOW);
            setReplyDate(middleReplyId, TimeConstants.ONE_HOUR_AGO);
            setReplyDate(oldestReplyId, TimeConstants.TWO_HOURS_AGO);

            MvcResult mvcResult = mockMvc.perform(get(ApiConstants.EVENT_THREAD_REPLIES_URL, savedEventId, savedThreadId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andReturn();

            ThreadReplyPageDto output = objectMapper.readValue(
                    mvcResult.getResponse().getContentAsString(),
                    ThreadReplyPageDto.class
            );

            assertThat(getReplyIds(output))
                    .containsExactly(oldestReplyId, middleReplyId, newestReplyId);
        }

        @Test
        @DisplayName("When getting replies should return HTTP 200 OK with correct pagination across multiple pages")
        public void whenGettingRepliesShouldReturnOkWithCorrectPaginationAcrossMultiplePages() throws Exception {
            testDataInitializer.setupThreadRepliesInThreadByFirstUser(
                    savedEventId,
                    savedThreadId,
                    PaginationConstants.DEFAULT_PAGE_SIZE + 1
            );

            MvcResult firstPageMvcResult = mockMvc.perform(get(ApiConstants.EVENT_THREAD_REPLIES_URL, savedEventId, savedThreadId)
                            .param("page", String.valueOf(PaginationConstants.PAGE_ZERO))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            MvcResult secondPageMvcResult = mockMvc.perform(get(ApiConstants.EVENT_THREAD_REPLIES_URL, savedEventId, savedThreadId)
                            .param("page", String.valueOf(PaginationConstants.PAGE_ONE))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            ThreadReplyPageDto firstPage = objectMapper.readValue(
                    firstPageMvcResult.getResponse().getContentAsString(),
                    ThreadReplyPageDto.class
            );
            ThreadReplyPageDto secondPage = objectMapper.readValue(
                    secondPageMvcResult.getResponse().getContentAsString(),
                    ThreadReplyPageDto.class
            );

            List<UUID> firstPageReplyIds = getReplyIds(firstPage);
            List<UUID> secondPageReplyIds = getReplyIds(secondPage);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(firstPage.replies()).hasSize(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(firstPage.pageNumber()).isEqualTo(PaginationConstants.PAGE_ZERO);
                softly.assertThat(firstPage.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(firstPage.totalElements()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE + 1L);
                softly.assertThat(firstPage.totalPages()).isEqualTo(2);
                softly.assertThat(firstPage.lastPage()).isFalse();

                softly.assertThat(secondPage.replies()).hasSize(1);
                softly.assertThat(secondPage.pageNumber()).isEqualTo(PaginationConstants.PAGE_ONE);
                softly.assertThat(secondPage.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(secondPage.totalElements()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE + 1L);
                softly.assertThat(secondPage.totalPages()).isEqualTo(2);
                softly.assertThat(secondPage.lastPage()).isTrue();

                softly.assertThat(firstPageReplyIds).doesNotContainAnyElementsOf(secondPageReplyIds);
            });
        }

        @Test
        @DisplayName("When getting replies should return HTTP 200 OK with empty page beyond last available page")
        public void whenGettingRepliesShouldReturnOkWithEmptyPageBeyondLastAvailablePage() throws Exception {
            testDataInitializer.setupThreadReplyInThreadByFirstUser(savedEventId, savedThreadId, ThreadReplyConstants.FIRST_REPLY_CONTENT);
            testDataInitializer.setupThreadReplyInThreadByFirstUser(savedEventId, savedThreadId, ThreadReplyConstants.SECOND_REPLY_CONTENT);

            mockMvc.perform(get(ApiConstants.EVENT_THREAD_REPLIES_URL, savedEventId, savedThreadId)
                            .param("page", String.valueOf(PaginationConstants.PAGE_ONE))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.replies").isArray())
                    .andExpect(jsonPath("$.replies").isEmpty())
                    .andExpect(jsonPath("$.pageNumber").value(PaginationConstants.PAGE_ONE))
                    .andExpect(jsonPath("$.pageSize").value(PaginationConstants.DEFAULT_PAGE_SIZE))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.lastPage").value(true));
        }
    }
}
