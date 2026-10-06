package com.mazurek.eventOrganizer.conversation;

import com.mazurek.eventOrganizer.exception.ApiErrorCode;
import com.mazurek.eventOrganizer.testData.builders.ConversationParticipantTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.ConversationTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.MarkConversationReadDtoTestBuilder;
import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationRequest;
import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipant;
import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipantRepository;
import com.mazurek.eventOrganizer.conversation.direct.DirectConversationPair;
import com.mazurek.eventOrganizer.conversation.direct.DirectConversationPairRepository;
import com.mazurek.eventOrganizer.conversation.dto.ConversationDetailsDto;
import com.mazurek.eventOrganizer.conversation.dto.ConversationParticipantDto;
import com.mazurek.eventOrganizer.conversation.dto.DirectMessageResponseDto;
import com.mazurek.eventOrganizer.conversation.dto.SendConversationMessageDto;
import com.mazurek.eventOrganizer.conversation.dto.SendDirectMessageDto;
import com.mazurek.eventOrganizer.conversation.message.Message;
import com.mazurek.eventOrganizer.conversation.message.MessageRepository;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import com.mazurek.eventOrganizer.exception.conversation.ConversationNotFoundException;
import com.mazurek.eventOrganizer.exception.conversation.MessagingYourselfException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestPersistenceQueries;
import com.mazurek.eventOrganizer.testData.builders.AuthenticationRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.SendConversationMessageDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.SendDirectMessageDtoTestBuilder;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import com.mazurek.eventOrganizer.utils.EncryptionUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ConversationController integration tests:")
public class ConversationControllerIntegrationTest {

    @Autowired
    private TestPersistenceQueries testPersistenceQueries;

    private final AuthenticationRequest firstUserAuthRequest =
            AuthenticationRequestTestBuilder.authenticationRequestForFirstUser().build();
    private final AuthenticationRequest secondUserAuthRequest =
            AuthenticationRequestTestBuilder.authenticationRequestForSecondUser().build();

    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private DirectConversationPairRepository directConversationPairRepository;
    @Autowired
    private ConversationParticipantRepository conversationParticipantRepository;
    @Autowired
    private MessageRepository messageRepository;
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
    private DeletionService deletionService;
    @Autowired
    private EncryptionUtils encryptionUtils;

    private User firstUser;
    private User secondUser;
    private String firstUserJwt;
    private String secondUserJwt;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();

        firstUser = userRepository.findByEmail(UserConstants.FIRST_USER_EMAIL)
                .orElseThrow(UserNotFoundException::new);
        secondUser = userRepository.findByEmail(UserConstants.SECOND_USER_EMAIL)
                .orElseThrow(UserNotFoundException::new);

        firstUserJwt = generateJwt(firstUserAuthRequest);
        secondUserJwt = generateJwt(secondUserAuthRequest);
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

    private DirectMessageResponseDto sendDirectMessage(String jwt, SendDirectMessageDto dto) throws Exception {
        MvcResult mvcResult = mockMvc.perform(
                        post(ApiConstants.DIRECT_CONVERSATIONS_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto))
                                .header(ApiConstants.AUTHORIZATION_HEADER, jwt)
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        return readDirectMessageResponse(mvcResult);
    }

    private DirectMessageResponseDto readDirectMessageResponse(MvcResult mvcResult) throws Exception {
        return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), DirectMessageResponseDto.class);
    }

    private ResultActions expectErrorJson(
            ResultActions resultActions,
            HttpStatus expectedStatus,
            String expectedCode,
            String expectedMessage
    ) throws Exception {
        return resultActions
                .andExpect(status().is(expectedStatus.value()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(expectedStatus.value()))
                .andExpect(jsonPath("$.code").value(expectedCode))
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    private ResultActions expectValidationErrorJson(ResultActions resultActions, String... fieldNames) throws Exception {
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_FAILED))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.errors").hasJsonPath());

        for (String fieldName : fieldNames) {
            resultActions.andExpect(jsonPath("$.errors." + fieldName).hasJsonPath());
        }

        return resultActions;
    }

    private ConversationParticipant findParticipant(UUID conversationId, User user) {
        return requirePresent(testPersistenceQueries.findConversationParticipant(conversationId, user.getId()), "Expected required conversation record in findParticipant");
    }

    private Message getNewestMessage(List<Message> messages) {
        return requirePresent(messages.stream()
                .max(Comparator.comparing(Message::getId)), "Expected required conversation record in getNewestMessage");
    }

    private void assertParticipantReadMetadata(
            UUID conversationId,
            User user,
            Instant expectedLastReadAt,
            Long expectedLastReadMessageId
    ) {
        ConversationParticipant participant = findParticipant(conversationId, user);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(participant.getLastReadAt()).isEqualTo(expectedLastReadAt);
            softly.assertThat(participant.getLastReadMessageId()).isEqualTo(expectedLastReadMessageId);
        });
    }

    @Nested
    @DisplayName("Send message to conversation tests: POST /api/v1/conversations/{conversationId}/messages")
    class SendMessageToConversationTests {

        private SendConversationMessageDto sendConversationMessageDto;

        @BeforeEach
        void setUp() {
            sendConversationMessageDto = SendConversationMessageDtoTestBuilder.firstConversationMessage().build();
        }

        @Test
        @DisplayName("When sending message to conversation should return HTTP 401 Unauthorized if there is no Authorization header")
        void whenSendingMessageToConversationShouldReturnHttpUnauthorizedIfThereIsNoAuthorizationHeader() throws Exception {
            var beforeWrite = testPersistenceQueries.conversationState();
            postConversationMessageWithoutAuth(ConversationConstants.FIRST_CONVERSATION_ID, sendConversationMessageDto)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));

            assertThat(testPersistenceQueries.conversationState())
                    .as("Rejected operation must preserve committed conversation state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When sending message to conversation should return HTTP 401 Unauthorized if Authorization header is empty")
        void whenSendingMessageToConversationShouldReturnHttpUnauthorizedIfAuthorizationHeaderIsEmpty() throws Exception {
            var beforeWrite = testPersistenceQueries.conversationState();
            postConversationMessage("", ConversationConstants.FIRST_CONVERSATION_ID, sendConversationMessageDto)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));

            assertThat(testPersistenceQueries.conversationState())
                    .as("Rejected operation must preserve committed conversation state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When sending message to conversation should return HTTP 401 Unauthorized if token is malformed")
        void whenSendingMessageToConversationShouldReturnHttpUnauthorizedIfTokenIsMalformed() throws Exception {
            var beforeWrite = testPersistenceQueries.conversationState();
            postConversationMessage(
                    AuthConstants.JWT_PREFIX + "invalid-token",
                    ConversationConstants.FIRST_CONVERSATION_ID,
                    sendConversationMessageDto)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.INVALID_ACCESS_TOKEN));

            assertThat(testPersistenceQueries.conversationState())
                    .as("Rejected operation must preserve committed conversation state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When sending message to conversation should return HTTP 400 Bad Request if conversation id is malformed")
        void whenSendingMessageToConversationShouldReturnHttpBadRequestIfConversationIdIsMalformed() throws Exception {
            var beforeWrite = testPersistenceQueries.conversationState();
            postConversationMessage(firstUserJwt, "not-a-uuid", sendConversationMessageDto)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.MALFORMED_REQUEST));

            assertThat(testPersistenceQueries.conversationState())
                    .as("Rejected operation must preserve committed conversation state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When sending message to conversation should return HTTP 400 Bad Request if request body is missing")
        void whenSendingMessageToConversationShouldReturnHttpBadRequestIfRequestBodyIsMissing() throws Exception {
            var beforeWrite = testPersistenceQueries.conversationState();
            postConversationMessageWithoutBody(firstUserJwt, ConversationConstants.FIRST_CONVERSATION_ID)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.MALFORMED_REQUEST));

            assertThat(testPersistenceQueries.conversationState())
                    .as("Rejected operation must preserve committed conversation state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When sending message to conversation should return HTTP 400 Bad Request if request body is malformed")
        void whenSendingMessageToConversationShouldReturnHttpBadRequestIfRequestBodyIsMalformed() throws Exception {
            var beforeWrite = testPersistenceQueries.conversationState();
            postConversationMessageWithRawContent(firstUserJwt, ConversationConstants.FIRST_CONVERSATION_ID, "{")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.MALFORMED_REQUEST));

            assertThat(testPersistenceQueries.conversationState())
                    .as("Rejected operation must preserve committed conversation state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When sending message to conversation should return HTTP 400 Bad Request with validation errors for invalid payload")
        void whenSendingMessageToConversationShouldReturnHttpBadRequestWithValidationErrorsIfPayloadIsInvalid() throws Exception {
            SendConversationMessageDto invalidDto = SendConversationMessageDtoTestBuilder.firstConversationMessage()
                    .content(" ")
                    .build();

            var beforeWrite = testPersistenceQueries.conversationState();
            expectValidationErrorJson(
                    postConversationMessage(firstUserJwt, ConversationConstants.FIRST_CONVERSATION_ID, invalidDto),
                    "content");

            assertNoMessagesCreated();

            assertThat(testPersistenceQueries.conversationState())
                    .as("Rejected operation must preserve committed conversation state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When sending message to conversation should return HTTP 400 Bad Request if content is too long")
        void whenSendingMessageToConversationShouldReturnHttpBadRequestIfContentIsTooLong() throws Exception {
            SendConversationMessageDto invalidDto = SendConversationMessageDtoTestBuilder.firstConversationMessage()
                    .content("a".repeat(2501))
                    .build();

            var beforeWrite = testPersistenceQueries.conversationState();
            expectValidationErrorJson(
                    postConversationMessage(firstUserJwt, ConversationConstants.FIRST_CONVERSATION_ID, invalidDto),
                    "content");

            assertNoMessagesCreated();

            assertThat(testPersistenceQueries.conversationState())
                    .as("Rejected operation must preserve committed conversation state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When sending message to conversation should return HTTP 404 Not Found if conversation does not exist")
        void whenSendingMessageToConversationShouldReturnHttpNotFoundIfConversationDoesNotExist() throws Exception {
            var beforeWrite = testPersistenceQueries.conversationState();
            expectErrorJson(
                    postConversationMessage(firstUserJwt, ConversationConstants.FIRST_CONVERSATION_ID, sendConversationMessageDto),
                    HttpStatus.NOT_FOUND,
                    ApiErrorCode.CONVERSATION_NOT_FOUND,
                    ConversationNotFoundException.DEFAULT_MESSAGE);

            assertNoMessagesCreated();

            assertThat(testPersistenceQueries.conversationState())
                    .as("Rejected operation must preserve committed conversation state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When sending message to conversation should return HTTP 404 Not Found if user is not participant")
        void whenSendingMessageToConversationShouldReturnHttpNotFoundIfUserIsNotParticipant() throws Exception {
            Conversation conversation = createGroupConversation(firstUser);

            var beforeWrite = testPersistenceQueries.conversationState();
            expectErrorJson(
                    postConversationMessage(secondUserJwt, conversation.getId(), sendConversationMessageDto),
                    HttpStatus.NOT_FOUND,
                    ApiErrorCode.CONVERSATION_NOT_FOUND,
                    ConversationNotFoundException.DEFAULT_MESSAGE);

            assertNoMessagesCreated();

            assertThat(testPersistenceQueries.conversationState())
                    .as("Rejected operation must preserve committed conversation state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When sending message to conversation should return HTTP 404 Not Found if participant has left conversation")
        void whenSendingMessageToConversationShouldReturnHttpNotFoundIfParticipantHasLeftConversation() throws Exception {
            Conversation conversation = createGroupConversation(firstUser, secondUser);
            ConversationParticipant secondParticipant = findParticipant(conversation.getId(), secondUser);
            secondParticipant.setLeftAt(TimeConstants.ONE_HOUR_AGO);
            conversationParticipantRepository.save(secondParticipant);

            var beforeWrite = testPersistenceQueries.conversationState();
            expectErrorJson(
                    postConversationMessage(secondUserJwt, conversation.getId(), sendConversationMessageDto),
                    HttpStatus.NOT_FOUND,
                    ApiErrorCode.CONVERSATION_NOT_FOUND,
                    ConversationNotFoundException.DEFAULT_MESSAGE);

            assertNoMessagesCreated();

            assertThat(testPersistenceQueries.conversationState())
                    .as("Rejected operation must preserve committed conversation state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When sending message to direct conversation should return HTTP 201 Created with decrypted message data")
        void whenSendingMessageToDirectConversationShouldReturnHttpCreatedWithDecryptedMessageData() throws Exception {
            DirectMessageResponseDto directMessageResponse = createDirectConversation();

            assertThat(directMessageResponse).isNotNull();
            assertThat(directMessageResponse.conversationId()).isNotNull();
            assertThat(directMessageResponse.message()).isNotNull();
            SendConversationMessageDto secondMessageDto = SendConversationMessageDtoTestBuilder.secondConversationMessage().build();

            expectConversationMessageCreatedJson(
                    postConversationMessage(secondUserJwt, directMessageResponse.conversationId(), secondMessageDto),
                    secondUser,
                    MessageConstants.SECOND_MESSAGE_CONTENT);

            Message newestMessage = getNewestMessage(messageRepository.findAll());

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(messageRepository.findAll()).hasSize(2);
                softly.assertThat(newestMessage.getContent()).isNotEqualTo(MessageConstants.SECOND_MESSAGE_CONTENT);
                softly.assertThat(encryptionUtils.decryptConversationMessage(
                        newestMessage.getContent(), newestMessage.getEncryptionKeyId()))
                        .contains(MessageConstants.SECOND_MESSAGE_CONTENT);
            });
            assertParticipantReadMetadata(
                    directMessageResponse.conversationId(),
                    firstUser,
                    TimeConstants.NOW,
                    directMessageResponse.message().getId());
            assertParticipantReadMetadata(directMessageResponse.conversationId(), secondUser, TimeConstants.NOW, newestMessage.getId());
        }

        @Test
        @DisplayName("When sending message to group conversation should return HTTP 201 Created with decrypted message data")
        void whenSendingMessageToGroupConversationShouldReturnHttpCreatedWithDecryptedMessageData() throws Exception {
            Conversation conversation = createGroupConversation(firstUser, secondUser);

            expectConversationMessageCreatedJson(
                    postConversationMessage(firstUserJwt, conversation.getId(), sendConversationMessageDto),
                    firstUser,
                    MessageConstants.FIRST_MESSAGE_CONTENT);

            Message savedMessage = findOnlyMessage();
            Conversation updatedConversation = requirePresent(conversationRepository.findById(conversation.getId()), "Expected required conversation record in whenSendingMessageToGroupConversationShouldReturnHttpCreatedWithDecryptedMessageData");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(messageRepository.findAll()).hasSize(1);
                softly.assertThat(updatedConversation.getLastActiveAt()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(savedMessage.getContent()).isNotEqualTo(MessageConstants.FIRST_MESSAGE_CONTENT);
                softly.assertThat(encryptionUtils.decryptConversationMessage(
                        savedMessage.getContent(), savedMessage.getEncryptionKeyId()))
                        .contains(MessageConstants.FIRST_MESSAGE_CONTENT);
            });
            assertParticipantReadMetadata(conversation.getId(), firstUser, TimeConstants.NOW, savedMessage.getId());
            assertParticipantReadMetadata(conversation.getId(), secondUser, null, null);
        }

        private ResultActions postConversationMessage(String jwt, Object conversationId, SendConversationMessageDto dto) throws Exception {
            return mockMvc.perform(
                    post(ApiConstants.CONVERSATION_MESSAGES_URL, conversationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, jwt)
            );
        }

        private ResultActions postConversationMessageWithoutAuth(Object conversationId, SendConversationMessageDto dto) throws Exception {
            return mockMvc.perform(
                    post(ApiConstants.CONVERSATION_MESSAGES_URL, conversationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto))
            );
        }

        private ResultActions postConversationMessageWithRawContent(String jwt, Object conversationId, String content) throws Exception {
            return mockMvc.perform(
                    post(ApiConstants.CONVERSATION_MESSAGES_URL, conversationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(content)
                            .header(ApiConstants.AUTHORIZATION_HEADER, jwt)
            );
        }

        private ResultActions postConversationMessageWithoutBody(String jwt, Object conversationId) throws Exception {
            return mockMvc.perform(
                    post(ApiConstants.CONVERSATION_MESSAGES_URL, conversationId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(ApiConstants.AUTHORIZATION_HEADER, jwt)
            );
        }

        private ResultActions expectConversationMessageCreatedJson(
                ResultActions resultActions,
                User sender,
                String content
        ) throws Exception {
            return resultActions
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.content").value(content))
                    .andExpect(jsonPath("$.sentDate").value(TimeConstants.NOW.toString()))
                    .andExpect(jsonPath("$.senderId").value(sender.getId().toString()));
        }

        private DirectMessageResponseDto createDirectConversation() throws Exception {
            SendDirectMessageDto sendDirectMessageDto = SendDirectMessageDtoTestBuilder.firstDirectMessage()
                    .recipientId(secondUser.getId())
                    .build();

            return sendDirectMessage(firstUserJwt, sendDirectMessageDto);
        }

        private Conversation createGroupConversation(User... participants) {
            Conversation conversation = conversationRepository.save(new ConversationTestBuilder().id(null)
                    .type(ConversationType.GROUP)
                    .name(ConversationConstants.FIRST_GROUP_CONVERSATION_NAME)
                    .createdAt(TimeConstants.TWO_HOURS_AGO)
                    .lastActiveAt(TimeConstants.ONE_HOUR_AGO)
                    .buildWithoutParticipants());

            for (User participant : participants) {
                conversationParticipantRepository.save(new ConversationParticipantTestBuilder().id(null)
                        .conversation(conversation)
                        .user(participant)
                        .joinedAt(TimeConstants.TWO_HOURS_AGO)
                        .userNameAtJoin(null)
                        .lastReadAt(null)
                        .lastReadMessageId(null)
                        .build());
            }

            return conversation;
        }

        private void assertNoMessagesCreated() {
            SoftAssertions.assertSoftly(softly ->
                    softly.assertThat(messageRepository.findAll()).isEmpty());
        }
    }

    @Nested
    @DisplayName("Send direct message tests: POST /api/v1/conversations/direct")
    class SendDirectMessageTests {

        private SendDirectMessageDto sendDirectMessageDto;

        @BeforeEach
        void setUp() {
            sendDirectMessageDto = SendDirectMessageDtoTestBuilder.firstDirectMessage()
                    .recipientId(secondUser.getId())
                    .build();
        }

        @Test
        @DisplayName("When sending direct message should return HTTP 401 Unauthorized if there is no Authorization header")
        public void whenSendingDirectMessageShouldReturnHttpUnauthorizedIfThereIsNoAuthorizationHeader() throws Exception {
            var beforeWrite = testPersistenceQueries.conversationState();
            postDirectMessageWithoutAuth(sendDirectMessageDto)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));

            assertThat(testPersistenceQueries.conversationState())
                    .as("Rejected operation must preserve committed conversation state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When sending direct message should return HTTP 401 Unauthorized if Authorization header is empty")
        public void whenSendingDirectMessageShouldReturnHttpUnauthorizedIfAuthorizationHeaderIsEmpty() throws Exception {
            var beforeWrite = testPersistenceQueries.conversationState();
            postDirectMessage("", sendDirectMessageDto)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));

            assertThat(testPersistenceQueries.conversationState())
                    .as("Rejected operation must preserve committed conversation state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When sending direct message should return HTTP 401 Unauthorized if token is malformed")
        public void whenSendingDirectMessageShouldReturnHttpUnauthorizedIfTokenIsMalformed() throws Exception {
            var beforeWrite = testPersistenceQueries.conversationState();
            postDirectMessage(AuthConstants.JWT_PREFIX + "invalid-token", sendDirectMessageDto)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.INVALID_ACCESS_TOKEN));

            assertThat(testPersistenceQueries.conversationState())
                    .as("Rejected operation must preserve committed conversation state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When sending direct message should return HTTP 400 Bad Request if request body is missing")
        public void whenSendingDirectMessageShouldReturnHttpBadRequestIfRequestBodyIsMissing() throws Exception {
            var beforeWrite = testPersistenceQueries.conversationState();
            postDirectMessageWithoutBody(firstUserJwt)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.MALFORMED_REQUEST));

            assertThat(testPersistenceQueries.conversationState())
                    .as("Rejected operation must preserve committed conversation state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When sending direct message should return HTTP 400 Bad Request if request body is malformed")
        public void whenSendingDirectMessageShouldReturnHttpBadRequestIfRequestBodyIsMalformed() throws Exception {
            var beforeWrite = testPersistenceQueries.conversationState();
            postDirectMessageWithRawContent(firstUserJwt, "{")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.MALFORMED_REQUEST));

            assertThat(testPersistenceQueries.conversationState())
                    .as("Rejected operation must preserve committed conversation state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When sending direct message should return HTTP 400 Bad Request with validation errors for invalid payload")
        public void whenSendingDirectMessageShouldReturnHttpBadRequestWithValidationErrorsIfPayloadIsInvalid() throws Exception {
            SendDirectMessageDto invalidDto = SendDirectMessageDtoTestBuilder.firstDirectMessage()
                    .recipientId(null)
                    .content(" ")
                    .build();

            var beforeWrite = testPersistenceQueries.conversationState();
            expectValidationErrorJson(
                    postDirectMessage(firstUserJwt, invalidDto),
                    "recipientId",
                    "content");

            assertNoConversationDataCreated();

            assertThat(testPersistenceQueries.conversationState())
                    .as("Rejected operation must preserve committed conversation state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When sending direct message should return HTTP 400 Bad Request if message content is too long")
        public void whenSendingDirectMessageShouldReturnHttpBadRequestIfMessageContentIsTooLong() throws Exception {
            SendDirectMessageDto invalidDto = SendDirectMessageDtoTestBuilder.firstDirectMessage()
                    .content("a".repeat(2501))
                    .build();

            var beforeWrite = testPersistenceQueries.conversationState();
            expectValidationErrorJson(
                    postDirectMessage(firstUserJwt, invalidDto),
                    "content");

            assertNoConversationDataCreated();

            assertThat(testPersistenceQueries.conversationState())
                    .as("Rejected operation must preserve committed conversation state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When sending direct message should return HTTP 400 Bad Request if recipient id is malformed")
        public void whenSendingDirectMessageShouldReturnHttpBadRequestIfRecipientIdIsMalformed() throws Exception {
            String malformedRecipientIdPayload = """
                    {"recipientId":"not-a-uuid","content":"Hello, this is the first message"}
                    """;

            var beforeWrite = testPersistenceQueries.conversationState();
            postDirectMessageWithRawContent(firstUserJwt, malformedRecipientIdPayload)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.MALFORMED_REQUEST));

            assertNoConversationDataCreated();

            assertThat(testPersistenceQueries.conversationState())
                    .as("Rejected operation must preserve committed conversation state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When sending direct message should return HTTP 400 Bad Request if user messages himself")
        public void whenSendingDirectMessageShouldReturnHttpBadRequestIfUserMessagesHimself() throws Exception {
            SendDirectMessageDto invalidDto = SendDirectMessageDtoTestBuilder.firstDirectMessage()
                    .recipientId(firstUser.getId())
                    .build();

            var beforeWrite = testPersistenceQueries.conversationState();
            expectErrorJson(
                    postDirectMessage(firstUserJwt, invalidDto),
                    HttpStatus.BAD_REQUEST,
                    ApiErrorCode.CANNOT_MESSAGE_SELF,
                    MessagingYourselfException.DEFAULT_MESSAGE);

            assertNoConversationDataCreated();

            assertThat(testPersistenceQueries.conversationState())
                    .as("Rejected operation must preserve committed conversation state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When sending direct message should return HTTP 404 Not Found if recipient does not exist")
        public void whenSendingDirectMessageShouldReturnHttpNotFoundIfRecipientDoesNotExist() throws Exception {
            SendDirectMessageDto invalidDto = SendDirectMessageDtoTestBuilder.firstDirectMessage()
                    .recipientId(UserConstants.NOT_EXISTING_USER_ID)
                    .build();

            var beforeWrite = testPersistenceQueries.conversationState();
            expectErrorJson(
                    postDirectMessage(firstUserJwt, invalidDto),
                    HttpStatus.NOT_FOUND,
                    ApiErrorCode.USER_NOT_FOUND,
                    UserNotFoundException.DEFAULT_MESSAGE);

            assertNoConversationDataCreated();

            assertThat(testPersistenceQueries.conversationState())
                    .as("Rejected operation must preserve committed conversation state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When sending direct message should return HTTP 201 Created with created conversation response")
        public void whenSendingDirectMessageShouldReturnHttpCreatedWithCreatedConversationResponse() throws Exception {
            expectDirectMessageCreatedJson(
                    postDirectMessage(firstUserJwt, sendDirectMessageDto),
                    firstUser,
                    MessageConstants.FIRST_MESSAGE_CONTENT,
                    true);
        }

        @Test
        @DisplayName("When sending direct message should persist direct conversation participants and encrypted message")
        public void whenSendingDirectMessageShouldPersistDirectConversationParticipantsAndEncryptedMessage() throws Exception {
            DirectMessageResponseDto response = sendDirectMessage(firstUserJwt, sendDirectMessageDto);

            assertThat(response).isNotNull();
            assertThat(response.conversationId()).isNotNull();
            assertThat(response.message()).isNotNull();

            assertSingleDirectConversationCreated(response);
        }

        @Test
        @DisplayName("When sending direct message should reuse existing direct conversation")
        public void whenSendingDirectMessageShouldReuseExistingDirectConversation() throws Exception {
            DirectMessageResponseDto firstResponse = sendDirectMessage(firstUserJwt, sendDirectMessageDto);

            assertThat(firstResponse).isNotNull();
            assertThat(firstResponse.conversationId()).isNotNull();
            assertThat(firstResponse.message()).isNotNull();
            SendDirectMessageDto secondMessageDto = SendDirectMessageDtoTestBuilder.firstDirectMessage()
                    .recipientId(secondUser.getId())
                    .content(MessageConstants.SECOND_MESSAGE_CONTENT)
                    .build();

            DirectMessageResponseDto secondResponse = sendDirectMessage(firstUserJwt, secondMessageDto);

            assertThat(secondResponse).isNotNull();
            assertThat(secondResponse.conversationId()).isNotNull();
            assertThat(secondResponse.message()).isNotNull();

            List<Conversation> conversations = conversationRepository.findAll();
            List<DirectConversationPair> directConversationPairs = directConversationPairRepository.findAll();
            List<Message> messages = messageRepository.findAll();
            DirectConversationPair savedDirectConversationPair = findDirectConversationPair(firstResponse.conversationId());
            Message newestMessage = getNewestMessage(messages);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(secondResponse.conversationId()).isEqualTo(firstResponse.conversationId());
                softly.assertThat(secondResponse.conversationCreated()).isFalse();
                softly.assertThat(secondResponse.message().getContent()).isEqualTo(MessageConstants.SECOND_MESSAGE_CONTENT);
                softly.assertThat(secondResponse.message().getSenderId()).isEqualTo(firstUser.getId());
            });

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(conversations).hasSize(1);
                softly.assertThat(directConversationPairs).hasSize(1);
                softly.assertThat(savedDirectConversationPair.getConversation().getId()).isEqualTo(firstResponse.conversationId());
                softly.assertThat(messages).hasSize(2);
                softly.assertThat(encryptionUtils.decryptConversationMessage(
                        newestMessage.getContent(), newestMessage.getEncryptionKeyId()))
                        .contains(MessageConstants.SECOND_MESSAGE_CONTENT);
            });
            assertParticipantReadMetadata(firstResponse.conversationId(), firstUser, TimeConstants.NOW, newestMessage.getId());
            assertParticipantReadMetadata(firstResponse.conversationId(), secondUser, null, null);
        }

        @Test
        @DisplayName("When sending direct message in inverse direction should reuse existing direct conversation")
        public void whenSendingDirectMessageInInverseDirectionShouldReuseExistingDirectConversation() throws Exception {
            DirectMessageResponseDto firstResponse = sendDirectMessage(firstUserJwt, sendDirectMessageDto);

            assertThat(firstResponse).isNotNull();
            assertThat(firstResponse.conversationId()).isNotNull();
            assertThat(firstResponse.message()).isNotNull();
            Message firstSavedMessage = findOnlyMessage();
            SendDirectMessageDto inverseMessageDto = SendDirectMessageDtoTestBuilder.inverseDirectMessage()
                    .recipientId(firstUser.getId())
                    .build();

            DirectMessageResponseDto inverseResponse = sendDirectMessage(secondUserJwt, inverseMessageDto);

            assertThat(inverseResponse).isNotNull();
            assertThat(inverseResponse.conversationId()).isNotNull();
            assertThat(inverseResponse.message()).isNotNull();

            List<Conversation> conversations = conversationRepository.findAll();
            List<DirectConversationPair> directConversationPairs = directConversationPairRepository.findAll();
            List<Message> messages = messageRepository.findAll();
            DirectConversationPair savedDirectConversationPair = findDirectConversationPair(firstResponse.conversationId());
            Message inverseSavedMessage = getNewestMessage(messages);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(inverseResponse.conversationId()).isEqualTo(firstResponse.conversationId());
                softly.assertThat(inverseResponse.conversationCreated()).isFalse();
                softly.assertThat(inverseResponse.message().getContent()).isEqualTo(MessageConstants.SECOND_MESSAGE_CONTENT);
                softly.assertThat(inverseResponse.message().getSenderId()).isEqualTo(secondUser.getId());
                softly.assertThat(conversations).hasSize(1);
                softly.assertThat(directConversationPairs).hasSize(1);
                softly.assertThat(savedDirectConversationPair.getConversation().getId()).isEqualTo(firstResponse.conversationId());
                softly.assertThat(savedDirectConversationPair.getFirstUserId()).isEqualTo(canonicalFirstUserId(firstUser, secondUser));
                softly.assertThat(savedDirectConversationPair.getSecondUserId()).isEqualTo(canonicalSecondUserId(firstUser, secondUser));
                softly.assertThat(messages).hasSize(2);
            });

            assertParticipantReadMetadata(firstResponse.conversationId(), firstUser, TimeConstants.NOW, firstSavedMessage.getId());
            assertParticipantReadMetadata(firstResponse.conversationId(), secondUser, TimeConstants.NOW, inverseSavedMessage.getId());
        }

        @Test
        @DisplayName("When sending direct message should not reuse group conversation")
        public void whenSendingDirectMessageShouldNotReuseGroupConversation() throws Exception {
            Conversation groupConversation = conversationRepository.save(new ConversationTestBuilder().id(null)
                    .type(ConversationType.GROUP)
                    .createdAt(TimeConstants.NOW)
                    .lastActiveAt(TimeConstants.NOW)
                    .buildWithoutParticipants());
            conversationParticipantRepository.save(new ConversationParticipantTestBuilder().id(null)
                    .conversation(groupConversation)
                    .user(firstUser)
                    .joinedAt(TimeConstants.NOW)
                    .userNameAtJoin(null)
                    .lastReadAt(null)
                    .lastReadMessageId(null)
                    .build());
            conversationParticipantRepository.save(new ConversationParticipantTestBuilder().id(null)
                    .conversation(groupConversation)
                    .user(secondUser)
                    .joinedAt(TimeConstants.NOW)
                    .userNameAtJoin(null)
                    .lastReadAt(null)
                    .lastReadMessageId(null)
                    .build());

            DirectMessageResponseDto response = sendDirectMessage(firstUserJwt, sendDirectMessageDto);

            assertThat(response).isNotNull();
            assertThat(response.conversationId()).isNotNull();
            assertThat(response.message()).isNotNull();

            List<Conversation> conversations = conversationRepository.findAll();
            List<Conversation> directConversations = conversations.stream()
                    .filter(conversation -> conversation.getType() == ConversationType.DIRECT)
                    .toList();
            List<Conversation> groupConversations = conversations.stream()
                    .filter(conversation -> conversation.getType() == ConversationType.GROUP)
                    .toList();
            DirectConversationPair savedDirectConversationPair = directConversationPairRepository.findAll().getFirst();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.conversationCreated()).isTrue();
                softly.assertThat(conversations).hasSize(2);
                softly.assertThat(directConversationPairRepository.findAll()).hasSize(1);
                softly.assertThat(directConversations).hasSize(1);
                softly.assertThat(groupConversations).hasSize(1);
                softly.assertThat(response.conversationId()).isEqualTo(directConversations.getFirst().getId());
                softly.assertThat(response.conversationId()).isNotEqualTo(groupConversation.getId());
                softly.assertThat(savedDirectConversationPair.getConversation().getId()).isEqualTo(response.conversationId());
                softly.assertThat(savedDirectConversationPair.getConversation().getId()).isNotEqualTo(groupConversation.getId());
            });
        }

        private ResultActions postDirectMessage(String jwt, SendDirectMessageDto dto) throws Exception {
            return mockMvc.perform(
                    post(ApiConstants.DIRECT_CONVERSATIONS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto))
                            .header(ApiConstants.AUTHORIZATION_HEADER, jwt)
            );
        }

        private ResultActions postDirectMessageWithoutAuth(SendDirectMessageDto dto) throws Exception {
            return mockMvc.perform(
                    post(ApiConstants.DIRECT_CONVERSATIONS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto))
            );
        }

        private ResultActions postDirectMessageWithRawContent(String jwt, String content) throws Exception {
            return mockMvc.perform(
                    post(ApiConstants.DIRECT_CONVERSATIONS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(content)
                            .header(ApiConstants.AUTHORIZATION_HEADER, jwt)
            );
        }

        private ResultActions postDirectMessageWithoutBody(String jwt) throws Exception {
            return mockMvc.perform(
                    post(ApiConstants.DIRECT_CONVERSATIONS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(ApiConstants.AUTHORIZATION_HEADER, jwt)
            );
        }

        private ResultActions expectDirectMessageCreatedJson(
                ResultActions resultActions,
                User sender,
                String content,
                boolean conversationCreated
        ) throws Exception {
            return resultActions
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.conversationId").hasJsonPath())
                    .andExpect(jsonPath("$.conversationId").isNotEmpty())
                    .andExpect(jsonPath("$.conversationCreated").value(conversationCreated))
                    .andExpect(jsonPath("$.message").hasJsonPath())
                    .andExpect(jsonPath("$.message.id").isNumber())
                    .andExpect(jsonPath("$.message.content").value(content))
                    .andExpect(jsonPath("$.message.sentDate").value(TimeConstants.NOW.toString()))
                    .andExpect(jsonPath("$.message.senderId").value(sender.getId().toString()));
        }

        private void assertNoConversationDataCreated() {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(conversationRepository.findAll()).isEmpty();
                softly.assertThat(conversationParticipantRepository.findAll()).isEmpty();
                softly.assertThat(directConversationPairRepository.findAll()).isEmpty();
                softly.assertThat(messageRepository.findAll()).isEmpty();
            });
        }

        private void assertSingleDirectConversationCreated(DirectMessageResponseDto response) {
            List<Conversation> conversations = conversationRepository.findAll();
            List<ConversationParticipant> participants = conversationParticipantRepository.findAll();
            List<DirectConversationPair> directConversationPairs = directConversationPairRepository.findAll();
            List<Message> messages = messageRepository.findAll();
            assertThat(response).isNotNull();
            assertThat(response.message()).isNotNull();
            assertThat(conversations).hasSize(1);
            assertThat(participants).hasSize(2);
            assertThat(directConversationPairs).hasSize(1);
            assertThat(messages).hasSize(1);
            Conversation savedConversation = conversations.getFirst();
            DirectConversationPair savedDirectConversationPair = directConversationPairs.getFirst();
            Message savedMessage = messages.getFirst();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.conversationId()).isEqualTo(savedConversation.getId());
                softly.assertThat(response.conversationCreated()).isTrue();
                softly.assertThat(response.message().getId()).isEqualTo(savedMessage.getId());
                softly.assertThat(response.message().getSenderId()).isEqualTo(firstUser.getId());
                softly.assertThat(conversations).hasSize(1);
                softly.assertThat(savedConversation.getCreatedAt()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(savedConversation.getLastActiveAt()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(participants).extracting(ConversationParticipant::getUserNameAtJoin)
                        .containsExactlyInAnyOrder(firstUser.getFullName(), secondUser.getFullName());
                softly.assertThat(savedConversation.getType()).isEqualTo(ConversationType.DIRECT);
                softly.assertThat(directConversationPairs).hasSize(1);
                softly.assertThat(participants).hasSize(2);
                softly.assertThat(messages).hasSize(1);
                softly.assertThat(savedDirectConversationPair.getConversation().getId()).isEqualTo(savedConversation.getId());
                softly.assertThat(savedDirectConversationPair.getFirstUserId()).isEqualTo(canonicalFirstUserId(firstUser, secondUser));
                softly.assertThat(savedDirectConversationPair.getSecondUserId()).isEqualTo(canonicalSecondUserId(firstUser, secondUser));
            });

            assertParticipantReadMetadata(response.conversationId(), firstUser, TimeConstants.NOW, savedMessage.getId());
            assertParticipantReadMetadata(response.conversationId(), secondUser, null, null);
            assertEncryptedMessagePersisted(savedMessage, response.message().getContent());
        }

        private void assertEncryptedMessagePersisted(Message savedMessage, String expectedContent) {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(savedMessage.getConversation().getType()).isEqualTo(ConversationType.DIRECT);
                softly.assertThat(savedMessage.getSender().getId()).isEqualTo(firstUser.getId());
                softly.assertThat(savedMessage.getSentDate()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(savedMessage.getContent()).isNotEqualTo(expectedContent);
                softly.assertThat(encryptionUtils.decryptConversationMessage(
                        savedMessage.getContent(), savedMessage.getEncryptionKeyId())).contains(expectedContent);
            });
        }

        private DirectConversationPair findDirectConversationPair(UUID conversationId) {
            return requirePresent(directConversationPairRepository.findAll().stream()
                    .filter(pair -> pair.getConversation().getId().equals(conversationId))
                    .findFirst(), "Expected required conversation record in findDirectConversationPair");
        }

        private UUID canonicalFirstUserId(User userA, User userB) {
            return userA.getId().toString().compareTo(userB.getId().toString()) < 0
                    ? userA.getId()
                    : userB.getId();
        }

        private UUID canonicalSecondUserId(User userA, User userB) {
            return userA.getId().toString().compareTo(userB.getId().toString()) < 0
                    ? userB.getId()
                    : userA.getId();
        }
    }

    @Nested
    @DisplayName("Get conversations tests: GET /api/v1/conversations")
    class GetConversationsTests {

        @Test
        @DisplayName("When getting conversations should return HTTP 401 Unauthorized if there is no Authorization header")
        public void whenGettingConversationsShouldReturnHttpUnauthorizedIfThereIsNoAuthorizationHeader() throws Exception {
            getConversationsWithoutAuth()
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));
        }

        @Test
        @DisplayName("When getting conversations should return HTTP 401 Unauthorized if Authorization header is empty")
        public void whenGettingConversationsShouldReturnHttpUnauthorizedIfAuthorizationHeaderIsEmpty() throws Exception {
            getConversations("", null)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));
        }

        @Test
        @DisplayName("When getting conversations should return HTTP 401 Unauthorized if token is malformed")
        public void whenGettingConversationsShouldReturnHttpUnauthorizedIfTokenIsMalformed() throws Exception {
            getConversations(AuthConstants.JWT_PREFIX + "invalid-token", null)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.INVALID_ACCESS_TOKEN));
        }

        @Test
        @DisplayName("When getting conversations should return HTTP 400 Bad Request if page is not a number")
        public void whenGettingConversationsShouldReturnHttpBadRequestIfPageIsNotNumber() throws Exception {
            getConversations(firstUserJwt, "not-a-number")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.MALFORMED_REQUEST));
        }

        @Test
        @DisplayName("When getting conversations should return HTTP 400 Bad Request if page is negative")
        public void whenGettingConversationsShouldReturnHttpBadRequestIfPageIsNegative() throws Exception {
            expectErrorJson(
                    getConversations(firstUserJwt, PaginationConstants.PAGE_MINUS_ONE),
                    HttpStatus.BAD_REQUEST,
                    ApiErrorCode.INVALID_PAGE_NUMBER,
                    InvalidPageNumberException.DEFAULT_MESSAGE);
        }

        @Test
        @DisplayName("When getting conversations should return empty page if user has no conversations")
        public void whenGettingConversationsShouldReturnEmptyPageIfUserHasNoConversations() throws Exception {
            expectConversationOverviewPageJson(
                    getConversations(firstUserJwt, PaginationConstants.PAGE_ZERO),
                    0,
                    PaginationConstants.PAGE_ZERO,
                    0,
                    0,
                    true);
        }

        @Test
        @DisplayName("When getting conversations without page param should return first page")
        public void whenGettingConversationsWithoutPageParamShouldReturnFirstPage() throws Exception {
            createDirectConversation();

            expectConversationOverviewPageJson(
                    getConversationsWithoutPage(firstUserJwt),
                    1,
                    PaginationConstants.PAGE_ZERO,
                    1,
                    1,
                    true);
        }

        @Test
        @DisplayName("When getting conversations should return direct conversation with other participant full name")
        public void whenGettingConversationsShouldReturnDirectConversationWithOtherParticipantFullName() throws Exception {
            DirectMessageResponseDto response = createDirectConversation();

            assertThat(response).isNotNull();
            assertThat(response.conversationId()).isNotNull();
            assertThat(response.message()).isNotNull();

            expectConversationOverviewPageJson(
                    getConversations(firstUserJwt, PaginationConstants.PAGE_ZERO),
                    1,
                    PaginationConstants.PAGE_ZERO,
                    1,
                    1,
                    true)
                    .andExpect(jsonPath("$.conversations[0].id").value(response.conversationId().toString()))
                    .andExpect(jsonPath("$.conversations[0].type").value(ConversationType.DIRECT.name()))
                    .andExpect(jsonPath("$.conversations[0].displayName").value(secondUser.getFullName()))
                    .andExpect(jsonPath("$.conversations[0].lastActiveAt").isNotEmpty());
        }

        @Test
        @DisplayName("When getting conversations as second user should return direct conversation with first user full name")
        public void whenGettingConversationsAsSecondUserShouldReturnDirectConversationWithFirstUserFullName() throws Exception {
            DirectMessageResponseDto response = createDirectConversation();

            assertThat(response).isNotNull();
            assertThat(response.conversationId()).isNotNull();
            assertThat(response.message()).isNotNull();

            expectConversationOverviewPageJson(
                    getConversations(secondUserJwt, PaginationConstants.PAGE_ZERO),
                    1,
                    PaginationConstants.PAGE_ZERO,
                    1,
                    1,
                    true)
                    .andExpect(jsonPath("$.conversations[0].id").value(response.conversationId().toString()))
                    .andExpect(jsonPath("$.conversations[0].type").value(ConversationType.DIRECT.name()))
                    .andExpect(jsonPath("$.conversations[0].displayName").value(firstUser.getFullName()))
                    .andExpect(jsonPath("$.conversations[0].lastActiveAt").isNotEmpty());
        }

        @Test
        @DisplayName("When getting conversations should return group conversation with conversation name")
        public void whenGettingConversationsShouldReturnGroupConversationWithConversationName() throws Exception {
            Conversation conversation = createGroupConversation(firstUser, secondUser);

            expectConversationOverviewPageJson(
                    getConversations(firstUserJwt, PaginationConstants.PAGE_ZERO),
                    1,
                    PaginationConstants.PAGE_ZERO,
                    1,
                    1,
                    true)
                    .andExpect(jsonPath("$.conversations[0].id").value(conversation.getId().toString()))
                    .andExpect(jsonPath("$.conversations[0].type").value(ConversationType.GROUP.name()))
                    .andExpect(jsonPath("$.conversations[0].displayName").value(ConversationConstants.FIRST_GROUP_CONVERSATION_NAME))
                    .andExpect(jsonPath("$.conversations[0].lastActiveAt").isNotEmpty());
        }

        @Test
        @DisplayName("When getting conversations should return only current user conversations")
        public void whenGettingConversationsShouldReturnOnlyCurrentUserConversations() throws Exception {
            Conversation firstUserConversation = createGroupConversation(firstUser, secondUser);
            createGroupConversation(secondUser);

            expectConversationOverviewPageJson(
                    getConversations(firstUserJwt, PaginationConstants.PAGE_ZERO),
                    1,
                    PaginationConstants.PAGE_ZERO,
                    1,
                    1,
                    true)
                    .andExpect(jsonPath("$.conversations[0].id").value(firstUserConversation.getId().toString()));
        }

        private ResultActions getConversations(String jwt, String pageNumber) throws Exception {
            var requestBuilder = get(ApiConstants.CONVERSATIONS_URL)
                    .header(ApiConstants.AUTHORIZATION_HEADER, jwt);

            if (pageNumber != null) {
                requestBuilder.param("page", pageNumber);
            }

            return mockMvc.perform(requestBuilder);
        }

        private ResultActions getConversations(String jwt, int pageNumber) throws Exception {
            return getConversations(jwt, String.valueOf(pageNumber));
        }

        private ResultActions getConversationsWithoutPage(String jwt) throws Exception {
            return getConversations(jwt, null);
        }

        private ResultActions getConversationsWithoutAuth() throws Exception {
            return mockMvc.perform(get(ApiConstants.CONVERSATIONS_URL));
        }

        private ResultActions expectConversationOverviewPageJson(
                ResultActions resultActions,
                int conversationsLength,
                int pageNumber,
                int totalElements,
                int totalPages,
                boolean lastPage
        ) throws Exception {
            return resultActions
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.conversations.length()").value(conversationsLength))
                    .andExpect(jsonPath("$.pageNumber").value(pageNumber))
                    .andExpect(jsonPath("$.pageSize").value(PaginationConstants.DEFAULT_PAGE_SIZE))
                    .andExpect(jsonPath("$.totalElements").value(totalElements))
                    .andExpect(jsonPath("$.totalPages").value(totalPages))
                    .andExpect(jsonPath("$.lastPage").value(lastPage));
        }

        private DirectMessageResponseDto createDirectConversation() throws Exception {
            SendDirectMessageDto sendDirectMessageDto = SendDirectMessageDtoTestBuilder.firstDirectMessage()
                    .recipientId(secondUser.getId())
                    .build();

            return sendDirectMessage(firstUserJwt, sendDirectMessageDto);
        }

        private Conversation createGroupConversation(User... participants) {
            Conversation conversation = conversationRepository.save(new ConversationTestBuilder().id(null)
                    .type(ConversationType.GROUP)
                    .name(ConversationConstants.FIRST_GROUP_CONVERSATION_NAME)
                    .createdAt(TimeConstants.TWO_HOURS_AGO)
                    .lastActiveAt(TimeConstants.NOW)
                    .buildWithoutParticipants());

            for (User participant : participants) {
                conversationParticipantRepository.save(new ConversationParticipantTestBuilder().id(null)
                        .conversation(conversation)
                        .user(participant)
                        .joinedAt(TimeConstants.TWO_HOURS_AGO)
                        .userNameAtJoin(null)
                        .lastReadAt(null)
                        .lastReadMessageId(null)
                        .build());
            }

            return conversation;
        }
    }

    @Nested
    @DisplayName("Get conversation tests: GET /api/v1/conversations/{conversationId}")
    class GetConversationTests {

        @Test
        @DisplayName("When getting conversation should return HTTP 401 Unauthorized if there is no Authorization header")
        public void whenGettingConversationShouldReturnHttpUnauthorizedIfThereIsNoAuthorizationHeader() throws Exception {
            getConversationWithoutAuth(ConversationConstants.FIRST_CONVERSATION_ID)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));
        }

        @Test
        @DisplayName("When getting conversation should return HTTP 401 Unauthorized if Authorization header is empty")
        public void whenGettingConversationShouldReturnHttpUnauthorizedIfAuthorizationHeaderIsEmpty() throws Exception {
            getConversation("", ConversationConstants.FIRST_CONVERSATION_ID)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));
        }

        @Test
        @DisplayName("When getting conversation should return HTTP 401 Unauthorized if token is malformed")
        public void whenGettingConversationShouldReturnHttpUnauthorizedIfTokenIsMalformed() throws Exception {
            getConversation(AuthConstants.JWT_PREFIX + "invalid-token", ConversationConstants.FIRST_CONVERSATION_ID)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.INVALID_ACCESS_TOKEN));
        }

        @Test
        @DisplayName("When getting conversation should return HTTP 400 Bad Request if conversation id is malformed")
        public void whenGettingConversationShouldReturnHttpBadRequestIfConversationIdIsMalformed() throws Exception {
            getConversation(firstUserJwt, "not-a-uuid")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.MALFORMED_REQUEST));
        }

        @Test
        @DisplayName("When getting conversation should return HTTP 404 Not Found if conversation does not exist")
        public void whenGettingConversationShouldReturnHttpNotFoundIfConversationDoesNotExist() throws Exception {
            expectErrorJson(
                    getConversation(firstUserJwt, ConversationConstants.FIRST_CONVERSATION_ID),
                    HttpStatus.NOT_FOUND,
                    ApiErrorCode.CONVERSATION_NOT_FOUND,
                    ConversationNotFoundException.DEFAULT_MESSAGE);
        }

        @Test
        @DisplayName("When getting conversation should return HTTP 404 Not Found if user is not participant")
        public void whenGettingConversationShouldReturnHttpNotFoundIfUserIsNotParticipant() throws Exception {
            Conversation conversation = createGroupConversation(firstUser);

            expectErrorJson(
                    getConversation(secondUserJwt, conversation.getId()),
                    HttpStatus.NOT_FOUND,
                    ApiErrorCode.CONVERSATION_NOT_FOUND,
                    ConversationNotFoundException.DEFAULT_MESSAGE);
        }

        @Test
        @DisplayName("When getting conversation should return HTTP 404 Not Found if participant has left conversation")
        public void whenGettingConversationShouldReturnHttpNotFoundIfParticipantHasLeftConversation() throws Exception {
            Conversation conversation = createGroupConversation(firstUser, secondUser);
            ConversationParticipant secondUserParticipant = findParticipant(conversation.getId(), secondUser);
            secondUserParticipant.setLeftAt(TimeConstants.ONE_HOUR_AGO);
            conversationParticipantRepository.save(secondUserParticipant);

            expectErrorJson(
                    getConversation(secondUserJwt, conversation.getId()),
                    HttpStatus.NOT_FOUND,
                    ApiErrorCode.CONVERSATION_NOT_FOUND,
                    ConversationNotFoundException.DEFAULT_MESSAGE);
        }

        @Test
        @DisplayName("When getting direct conversation as first user should return second user full name")
        public void whenGettingDirectConversationAsFirstUserShouldReturnSecondUserFullName() throws Exception {
            DirectMessageResponseDto directMessageResponse = createDirectConversation();

            assertThat(directMessageResponse).isNotNull();
            assertThat(directMessageResponse.conversationId()).isNotNull();
            assertThat(directMessageResponse.message()).isNotNull();
            Conversation conversation = requirePresent(conversationRepository.findById(directMessageResponse.conversationId()), "Expected required conversation record in whenGettingDirectConversationAsFirstUserShouldReturnSecondUserFullName");

            ConversationDetailsDto response = expectConversationDetailsJson(
                    getConversation(firstUserJwt, conversation.getId()),
                    conversation,
                    secondUser.getFullName(),
                    2);

            assertThat(response).isNotNull();

            assertParticipantDto(response, firstUser, findParticipant(conversation.getId(), firstUser));
            assertParticipantDto(response, secondUser, findParticipant(conversation.getId(), secondUser));
        }

        @Test
        @DisplayName("When getting direct conversation as second user should return first user full name")
        public void whenGettingDirectConversationAsSecondUserShouldReturnFirstUserFullName() throws Exception {
            DirectMessageResponseDto directMessageResponse = createDirectConversation();

            assertThat(directMessageResponse).isNotNull();
            assertThat(directMessageResponse.conversationId()).isNotNull();
            assertThat(directMessageResponse.message()).isNotNull();
            Conversation conversation = requirePresent(conversationRepository.findById(directMessageResponse.conversationId()), "Expected required conversation record in whenGettingDirectConversationAsSecondUserShouldReturnFirstUserFullName");

            ConversationDetailsDto response = expectConversationDetailsJson(
                    getConversation(secondUserJwt, conversation.getId()),
                    conversation,
                    firstUser.getFullName(),
                    2);

            assertThat(response).isNotNull();

            assertParticipantDto(response, firstUser, findParticipant(conversation.getId(), firstUser));
            assertParticipantDto(response, secondUser, findParticipant(conversation.getId(), secondUser));
        }

        @Test
        @DisplayName("When getting group conversation should return conversation name")
        public void whenGettingGroupConversationShouldReturnConversationName() throws Exception {
            Conversation conversation = createGroupConversation(firstUser, secondUser);

            ConversationDetailsDto response = expectConversationDetailsJson(
                    getConversation(firstUserJwt, conversation.getId()),
                    conversation,
                    ConversationConstants.FIRST_GROUP_CONVERSATION_NAME,
                    2);

            assertThat(response).isNotNull();

            assertParticipantDto(response, firstUser, findParticipant(conversation.getId(), firstUser));
            assertParticipantDto(response, secondUser, findParticipant(conversation.getId(), secondUser));
        }

        @Test
        @DisplayName("When getting conversation should exclude participants that left conversation")
        public void whenGettingConversationShouldExcludeParticipantsThatLeftConversation() throws Exception {
            Conversation conversation = createGroupConversation(firstUser, secondUser);
            ConversationParticipant secondUserParticipant = findParticipant(conversation.getId(), secondUser);
            secondUserParticipant.setLeftAt(TimeConstants.ONE_HOUR_AGO);
            conversationParticipantRepository.save(secondUserParticipant);

            ConversationDetailsDto response = expectConversationDetailsJson(
                    getConversation(firstUserJwt, conversation.getId()),
                    conversation,
                    ConversationConstants.FIRST_GROUP_CONVERSATION_NAME,
                    1);

            assertThat(response).isNotNull();

            assertParticipantDto(response, firstUser, findParticipant(conversation.getId(), firstUser));
            SoftAssertions.assertSoftly(softly ->
                    softly.assertThat(response.participants())
                            .extracting(ConversationParticipantDto::userId)
                            .doesNotContain(secondUser.getId()));
        }

        private ResultActions getConversation(String jwt, Object conversationId) throws Exception {
            return mockMvc.perform(
                    get(ApiConstants.CONVERSATION_BY_ID_URL, conversationId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, jwt)
            );
        }

        private ResultActions getConversationWithoutAuth(Object conversationId) throws Exception {
            return mockMvc.perform(get(ApiConstants.CONVERSATION_BY_ID_URL, conversationId));
        }

        private DirectMessageResponseDto createDirectConversation() throws Exception {
            SendDirectMessageDto sendDirectMessageDto = SendDirectMessageDtoTestBuilder.firstDirectMessage()
                    .recipientId(secondUser.getId())
                    .build();

            return sendDirectMessage(firstUserJwt, sendDirectMessageDto);
        }

        private Conversation createGroupConversation(User... participants) {
            Conversation conversation = conversationRepository.save(new ConversationTestBuilder().id(null)
                    .type(ConversationType.GROUP)
                    .name(ConversationConstants.FIRST_GROUP_CONVERSATION_NAME)
                    .createdAt(TimeConstants.TWO_HOURS_AGO)
                    .lastActiveAt(TimeConstants.ONE_HOUR_AGO)
                    .buildWithoutParticipants());

            for (User participant : participants) {
                conversationParticipantRepository.save(new ConversationParticipantTestBuilder().id(null)
                        .conversation(conversation)
                        .user(participant)
                        .joinedAt(TimeConstants.TWO_HOURS_AGO)
                        .userNameAtJoin(null)
                        .lastReadAt(null)
                        .lastReadMessageId(null)
                        .build());
            }

            return conversation;
        }

        private ConversationDetailsDto expectConversationDetailsJson(
                ResultActions resultActions,
                Conversation conversation,
                String expectedName,
                int expectedParticipantsLength
        ) throws Exception {
            MvcResult mvcResult = resultActions
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(conversation.getId().toString()))
                    .andExpect(jsonPath("$.type").value(conversation.getType().name()))
                    .andExpect(jsonPath("$.createdAt").value(conversation.getCreatedAt().toString()))
                    .andExpect(jsonPath("$.lastActiveAt").value(conversation.getLastActiveAt().toString()))
                    .andExpect(jsonPath("$.name").value(expectedName))
                    .andExpect(jsonPath("$.participants.length()").value(expectedParticipantsLength))
                    .andReturn();

            return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ConversationDetailsDto.class);
        }

        private void assertParticipantDto(
                ConversationDetailsDto response,
                User user,
                ConversationParticipant participant
        ) {
            ConversationParticipantDto participantDto = requirePresent(response.participants().stream()
                    .filter(dto -> dto.userId().equals(user.getId()))
                    .findFirst(), "Expected required conversation record in assertParticipantDto");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(participantDto.userId()).isEqualTo(user.getId());
                softly.assertThat(participantDto.fullName()).isEqualTo(user.getFullName());
                softly.assertThat(participantDto.joinedAt()).isEqualTo(participant.getJoinedAt());
                softly.assertThat(participantDto.lastReadAt()).isEqualTo(participant.getLastReadAt());
                softly.assertThat(participantDto.lastReadMessageId()).isEqualTo(participant.getLastReadMessageId());
            });
        }
    }

    @Nested
    @DisplayName("Get messages in conversation tests: GET /api/v1/conversations/{conversationId}/messages")
    class GetMessagesInConversationTests {

        @Test
        @DisplayName("When getting messages should return HTTP 401 Unauthorized if there is no Authorization header")
        public void whenGettingMessagesShouldReturnHttpUnauthorizedIfThereIsNoAuthorizationHeader() throws Exception {
            getConversationMessagesWithoutAuth(ConversationConstants.FIRST_CONVERSATION_ID)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));
        }

        @Test
        @DisplayName("When getting messages should return HTTP 401 Unauthorized if Authorization header is empty")
        public void whenGettingMessagesShouldReturnHttpUnauthorizedIfAuthorizationHeaderIsEmpty() throws Exception {
            getConversationMessages("", ConversationConstants.FIRST_CONVERSATION_ID, null)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));
        }

        @Test
        @DisplayName("When getting messages should return HTTP 401 Unauthorized if token is malformed")
        public void whenGettingMessagesShouldReturnHttpUnauthorizedIfTokenIsMalformed() throws Exception {
            getConversationMessages(
                    AuthConstants.JWT_PREFIX + "invalid-token",
                    ConversationConstants.FIRST_CONVERSATION_ID,
                    null)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.INVALID_ACCESS_TOKEN));
        }

        @Test
        @DisplayName("When getting messages should return HTTP 400 Bad Request if conversation id is malformed")
        public void whenGettingMessagesShouldReturnHttpBadRequestIfConversationIdIsMalformed() throws Exception {
            getConversationMessages(firstUserJwt, "not-a-uuid", null)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.MALFORMED_REQUEST));
        }

        @Test
        @DisplayName("When getting messages should return HTTP 400 Bad Request if page is not a number")
        public void whenGettingMessagesShouldReturnHttpBadRequestIfPageIsNotNumber() throws Exception {
            getConversationMessages(firstUserJwt, ConversationConstants.FIRST_CONVERSATION_ID, "not-a-number")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.MALFORMED_REQUEST));
        }

        @Test
        @DisplayName("When getting messages should return HTTP 400 Bad Request if page is negative")
        public void whenGettingMessagesShouldReturnHttpBadRequestIfPageIsNegative() throws Exception {
            DirectMessageResponseDto response = createConversationWithMessages(1).getFirst();

            assertThat(response).isNotNull();
            assertThat(response.conversationId()).isNotNull();
            assertThat(response.message()).isNotNull();

            expectErrorJson(
                    getConversationMessages(firstUserJwt, response.conversationId(), PaginationConstants.PAGE_MINUS_ONE),
                    HttpStatus.BAD_REQUEST,
                    ApiErrorCode.INVALID_PAGE_NUMBER,
                    InvalidPageNumberException.DEFAULT_MESSAGE);
        }

        @Test
        @DisplayName("When getting messages should return HTTP 404 Not Found if conversation does not exist")
        public void whenGettingMessagesShouldReturnHttpNotFoundIfConversationDoesNotExist() throws Exception {
            expectErrorJson(
                    getConversationMessagesWithoutPage(firstUserJwt, ConversationConstants.FIRST_CONVERSATION_ID),
                    HttpStatus.NOT_FOUND,
                    ApiErrorCode.CONVERSATION_NOT_FOUND,
                    ConversationNotFoundException.DEFAULT_MESSAGE);
        }

        @Test
        @DisplayName("When getting messages should return HTTP 404 Not Found if user is not participant")
        public void whenGettingMessagesShouldReturnHttpNotFoundIfUserIsNotParticipant() throws Exception {
            Conversation conversation = conversationRepository.save(new ConversationTestBuilder().id(null)
                    .type(ConversationType.DIRECT)
                    .createdAt(TimeConstants.NOW)
                    .lastActiveAt(TimeConstants.NOW)
                    .buildWithoutParticipants());
            conversationParticipantRepository.save(new ConversationParticipantTestBuilder().id(null)
                    .conversation(conversation)
                    .user(firstUser)
                    .joinedAt(TimeConstants.NOW)
                    .userNameAtJoin(null)
                    .lastReadAt(null)
                    .lastReadMessageId(null)
                    .build());

            expectErrorJson(
                    getConversationMessagesWithoutPage(secondUserJwt, conversation.getId()),
                    HttpStatus.NOT_FOUND,
                    ApiErrorCode.CONVERSATION_NOT_FOUND,
                    ConversationNotFoundException.DEFAULT_MESSAGE);
        }

        @Test
        @DisplayName("When getting messages should return HTTP 404 Not Found if participant has left conversation")
        public void whenGettingMessagesShouldReturnHttpNotFoundIfParticipantHasLeftConversation() throws Exception {
            DirectMessageResponseDto response = createConversationWithMessages(1).getFirst();

            assertThat(response).isNotNull();
            assertThat(response.conversationId()).isNotNull();
            assertThat(response.message()).isNotNull();
            ConversationParticipant secondParticipant = findParticipant(response.conversationId(), secondUser);
            secondParticipant.setLeftAt(TimeConstants.NOW);
            conversationParticipantRepository.save(secondParticipant);

            expectErrorJson(
                    getConversationMessagesWithoutPage(secondUserJwt, response.conversationId()),
                    HttpStatus.NOT_FOUND,
                    ApiErrorCode.CONVERSATION_NOT_FOUND,
                    ConversationNotFoundException.DEFAULT_MESSAGE);
        }

        @Test
        @DisplayName("When getting messages without page param should return first page")
        public void whenGettingMessagesWithoutPageParamShouldReturnFirstPage() throws Exception {
            DirectMessageResponseDto response = createConversationWithMessages(2).getFirst();

            assertThat(response).isNotNull();
            assertThat(response.conversationId()).isNotNull();
            assertThat(response.message()).isNotNull();

            expectMessagePageJson(
                    getConversationMessagesWithoutPage(secondUserJwt, response.conversationId()),
                    2,
                    PaginationConstants.PAGE_ZERO,
                    2,
                    1,
                    true);
        }

        @Test
        @DisplayName("When getting messages should return HTTP 200 OK with decrypted message data")
        public void whenGettingMessagesShouldReturnHttpOkWithDecryptedMessageData() throws Exception {
            DirectMessageResponseDto response = createConversationWithMessages(1).getFirst();

            assertThat(response).isNotNull();
            assertThat(response.conversationId()).isNotNull();
            assertThat(response.message()).isNotNull();
            Message savedMessage = findOnlyMessage();

            var beforeRead = testPersistenceQueries.conversationState();
            expectMessagePageJson(
                    getConversationMessages(secondUserJwt, response.conversationId(), PaginationConstants.PAGE_ZERO),
                    1,
                    PaginationConstants.PAGE_ZERO,
                    1,
                    1,
                    true)
                    .andExpect(jsonPath("$.messages[0].id").value(savedMessage.getId()))
                    .andExpect(jsonPath("$.messages[0].content").value(messageContent(0)))
                    .andExpect(jsonPath("$.messages[0].sentDate").value(TimeConstants.NOW.toString()))
                    .andExpect(jsonPath("$.messages[0].senderId").value(firstUser.getId().toString()));

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(savedMessage.getContent()).isNotEqualTo(messageContent(0));
                softly.assertThat(encryptionUtils.decryptConversationMessage(
                        savedMessage.getContent(), savedMessage.getEncryptionKeyId())).contains(messageContent(0));
            });

            assertThat(testPersistenceQueries.conversationState())
                    .as("HTTP decryption must preserve committed ciphertext and read/activity markers").isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting messages should return newest messages first with stable id order")
        public void whenGettingMessagesShouldReturnNewestMessagesFirstWithStableIdOrder() throws Exception {
            List<DirectMessageResponseDto> sentMessages = createConversationWithMessages(3);
            assertThat(sentMessages).hasSize(3).doesNotContainNull();
            DirectMessageResponseDto response = sentMessages.getFirst();

            assertThat(response).isNotNull();
            assertThat(response.conversationId()).isNotNull();
            assertThat(response.message()).isNotNull();

            expectMessagePageJson(
                    getConversationMessages(secondUserJwt, response.conversationId(), PaginationConstants.PAGE_ZERO),
                    3,
                    PaginationConstants.PAGE_ZERO,
                    3,
                    1,
                    true)
                    .andExpect(jsonPath("$.messages[0].id").value(sentMessages.get(2).message().getId()))
                    .andExpect(jsonPath("$.messages[1].id").value(sentMessages.get(1).message().getId()))
                    .andExpect(jsonPath("$.messages[2].id").value(sentMessages.getFirst().message().getId()))
                    .andExpect(jsonPath("$.messages[0].content").value(messageContent(2)))
                    .andExpect(jsonPath("$.messages[1].content").value(messageContent(1)))
                    .andExpect(jsonPath("$.messages[2].content").value(messageContent(0)));
        }

        @Test
        @DisplayName("When getting messages should not implicitly mark them read, and explicit acknowledgement should update the marker")
        public void whenReadingMessagesShouldRequireExplicitReadAcknowledgement() throws Exception {
            DirectMessageResponseDto response = createConversationWithMessages(2).getFirst();

            assertThat(response).isNotNull();
            assertThat(response.conversationId()).isNotNull();
            assertThat(response.message()).isNotNull();
            ConversationParticipant participantBeforeRead = findParticipant(response.conversationId(), secondUser);
            Message newestMessage = getNewestMessage(messageRepository.findAll());

            var beforeRead = testPersistenceQueries.conversationState();
            getConversationMessages(secondUserJwt, response.conversationId(), PaginationConstants.PAGE_ZERO)
                    .andExpect(status().isOk());

            ConversationParticipant participantAfterRead = findParticipant(response.conversationId(), secondUser);
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(participantBeforeRead.getLastReadAt()).isNull();
                softly.assertThat(participantBeforeRead.getLastReadMessageId()).isNull();
                softly.assertThat(participantAfterRead.getLastReadAt()).isNull();
                softly.assertThat(participantAfterRead.getLastReadMessageId()).isNull();
            });

            assertThat(testPersistenceQueries.conversationState()).isEqualTo(beforeRead);

            mockMvc.perform(post("/api/v1/conversations/{conversationId}/read", response.conversationId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new MarkConversationReadDtoTestBuilder().lastReadMessageId(newestMessage.getId()).build()))
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isNoContent());

            assertParticipantReadMetadata(response.conversationId(), secondUser, TimeConstants.NOW, newestMessage.getId());
        }

        @Test
        @DisplayName("When getting older messages should return second page and not update read metadata")
        public void whenGettingOlderMessagesShouldReturnSecondPageAndNotUpdateReadMetadata() throws Exception {
            DirectMessageResponseDto response = createConversationWithMessages(PaginationConstants.DEFAULT_PAGE_SIZE + 1)
                    .getFirst();

            assertThat(response).isNotNull();
            assertThat(response.conversationId()).isNotNull();
            assertThat(response.message()).isNotNull();
            ConversationParticipant participantBeforeRead = findParticipant(response.conversationId(), secondUser);
            participantBeforeRead.setLastReadAt(TimeConstants.ONE_HOUR_AGO);
            participantBeforeRead.setLastReadMessageId(response.message().getId());
            conversationParticipantRepository.save(participantBeforeRead);

            expectMessagePageJson(
                    getConversationMessages(secondUserJwt, response.conversationId(), PaginationConstants.PAGE_ONE),
                    1,
                    PaginationConstants.PAGE_ONE,
                    PaginationConstants.DEFAULT_PAGE_SIZE + 1,
                    2,
                    true)
                    .andExpect(jsonPath("$.messages[0].id").value(response.message().getId()))
                    .andExpect(jsonPath("$.messages[0].content").value(messageContent(0)));

            assertParticipantReadMetadata(
                    response.conversationId(),
                    secondUser,
                    TimeConstants.ONE_HOUR_AGO,
                    response.message().getId());
        }

        private ResultActions getConversationMessages(String jwt, Object conversationId, String pageNumber) throws Exception {
            var requestBuilder = get(ApiConstants.CONVERSATION_MESSAGES_URL, conversationId)
                    .header(ApiConstants.AUTHORIZATION_HEADER, jwt);

            if (pageNumber != null) {
                requestBuilder.param("page", pageNumber);
            }

            return mockMvc.perform(requestBuilder);
        }

        private ResultActions getConversationMessages(String jwt, UUID conversationId, int pageNumber) throws Exception {
            return getConversationMessages(jwt, conversationId, String.valueOf(pageNumber));
        }

        private ResultActions getConversationMessagesWithoutPage(String jwt, UUID conversationId) throws Exception {
            return getConversationMessages(jwt, conversationId, null);
        }

        private ResultActions getConversationMessagesWithoutAuth(UUID conversationId) throws Exception {
            return mockMvc.perform(get(ApiConstants.CONVERSATION_MESSAGES_URL, conversationId));
        }

        private ResultActions expectMessagePageJson(
                ResultActions resultActions,
                int messagesLength,
                int pageNumber,
                int totalElements,
                int totalPages,
                boolean lastPage
        ) throws Exception {
            return resultActions
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.messages.length()").value(messagesLength))
                    .andExpect(jsonPath("$.pageNumber").value(pageNumber))
                    .andExpect(jsonPath("$.pageSize").value(PaginationConstants.DEFAULT_PAGE_SIZE))
                    .andExpect(jsonPath("$.totalElements").value(totalElements))
                    .andExpect(jsonPath("$.totalPages").value(totalPages))
                    .andExpect(jsonPath("$.lastPage").value(lastPage));
        }

        private List<DirectMessageResponseDto> createConversationWithMessages(int messageAmount) throws Exception {
            List<DirectMessageResponseDto> responses = new ArrayList<>();

            for (int messageNumber = 0; messageNumber < messageAmount; messageNumber++) {
                SendDirectMessageDto sendDirectMessageDto = SendDirectMessageDtoTestBuilder.firstDirectMessage()
                        .recipientId(secondUser.getId())
                        .content(messageContent(messageNumber))
                        .build();

                responses.add(sendDirectMessage(firstUserJwt, sendDirectMessageDto));
            }

            return responses;
        }

        private String messageContent(int messageNumber) {
            return MessageConstants.FIRST_MESSAGE_CONTENT + " " + messageNumber;
        }
    }
    private Message findOnlyMessage() {
        List<Message> messages = messageRepository.findAll();
        assertThat(messages).as("Expected exactly one persisted message at this prerequisite").hasSize(1);
        return messages.getFirst();
    }
}
