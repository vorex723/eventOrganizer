package com.mazurek.eventOrganizer.conversation;

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
import com.mazurek.eventOrganizer.conversation.dto.SendDirectMessageDto;
import com.mazurek.eventOrganizer.conversation.message.Message;
import com.mazurek.eventOrganizer.conversation.message.MessageRepository;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import com.mazurek.eventOrganizer.exception.conversation.ConversationNotFoundException;
import com.mazurek.eventOrganizer.exception.conversation.MessagingYourselfException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.builders.AuthenticationRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.SendDirectMessageDtoTestBuilder;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import com.mazurek.eventOrganizer.utils.EncryptionUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ConversationController integration tests:")
public class ConversationControllerIntegrationTest {

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
            String expectedMessage
    ) throws Exception {
        return resultActions
                .andExpect(status().is(expectedStatus.value()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(expectedStatus.value()))
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    private ResultActions expectValidationErrorJson(ResultActions resultActions, String... fieldNames) throws Exception {
        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.errors").hasJsonPath());

        for (String fieldName : fieldNames) {
            resultActions.andExpect(jsonPath("$.errors." + fieldName).hasJsonPath());
        }

        return resultActions;
    }

    private ConversationParticipant findParticipant(UUID conversationId, User user) {
        return conversationParticipantRepository.findByConversationIdAndUserId(conversationId, user.getId())
                .orElseThrow();
    }

    private Message getNewestMessage(List<Message> messages) {
        return messages.stream()
                .max(Comparator.comparing(Message::getId))
                .orElseThrow();
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
        @DisplayName("When sending direct message should return HTTP 403 Forbidden if there is no Authorization header")
        public void whenSendingDirectMessageShouldReturnHttpForbiddenIfThereIsNoAuthorizationHeader() throws Exception {
            postDirectMessageWithoutAuth(sendDirectMessageDto)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When sending direct message should return HTTP 403 Forbidden if Authorization header is empty")
        public void whenSendingDirectMessageShouldReturnHttpForbiddenIfAuthorizationHeaderIsEmpty() throws Exception {
            postDirectMessage("", sendDirectMessageDto)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When sending direct message should return HTTP 403 Forbidden if token is malformed")
        public void whenSendingDirectMessageShouldReturnHttpForbiddenIfTokenIsMalformed() throws Exception {
            postDirectMessage(AuthConstants.JWT_PREFIX + "invalid-token", sendDirectMessageDto)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When sending direct message should return HTTP 400 Bad Request if request body is missing")
        public void whenSendingDirectMessageShouldReturnHttpBadRequestIfRequestBodyIsMissing() throws Exception {
            postDirectMessageWithoutBody(firstUserJwt)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("When sending direct message should return HTTP 400 Bad Request if request body is malformed")
        public void whenSendingDirectMessageShouldReturnHttpBadRequestIfRequestBodyIsMalformed() throws Exception {
            postDirectMessageWithRawContent(firstUserJwt, "{")
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("When sending direct message should return HTTP 400 Bad Request with validation errors for invalid payload")
        public void whenSendingDirectMessageShouldReturnHttpBadRequestWithValidationErrorsIfPayloadIsInvalid() throws Exception {
            sendDirectMessageDto.setRecipientId(null);
            sendDirectMessageDto.setContent(" ");

            expectValidationErrorJson(
                    postDirectMessage(firstUserJwt, sendDirectMessageDto),
                    "recipientId",
                    "content");

            assertNoConversationDataCreated();
        }

        @Test
        @DisplayName("When sending direct message should return HTTP 400 Bad Request if message content is too long")
        public void whenSendingDirectMessageShouldReturnHttpBadRequestIfMessageContentIsTooLong() throws Exception {
            sendDirectMessageDto.setContent("a".repeat(2501));

            expectValidationErrorJson(
                    postDirectMessage(firstUserJwt, sendDirectMessageDto),
                    "content");

            assertNoConversationDataCreated();
        }

        @Test
        @DisplayName("When sending direct message should return HTTP 400 Bad Request if recipient id is malformed")
        public void whenSendingDirectMessageShouldReturnHttpBadRequestIfRecipientIdIsMalformed() throws Exception {
            String malformedRecipientIdPayload = """
                    {"recipientId":"not-a-uuid","content":"Hello, this is the first message"}
                    """;

            postDirectMessageWithRawContent(firstUserJwt, malformedRecipientIdPayload)
                    .andExpect(status().isBadRequest());

            assertNoConversationDataCreated();
        }

        @Test
        @DisplayName("When sending direct message should return HTTP 400 Bad Request if user messages himself")
        public void whenSendingDirectMessageShouldReturnHttpBadRequestIfUserMessagesHimself() throws Exception {
            sendDirectMessageDto.setRecipientId(firstUser.getId());

            expectErrorJson(
                    postDirectMessage(firstUserJwt, sendDirectMessageDto),
                    HttpStatus.BAD_REQUEST,
                    MessagingYourselfException.DEFAULT_MESSAGE);

            assertNoConversationDataCreated();
        }

        @Test
        @DisplayName("When sending direct message should return HTTP 404 Not Found if recipient does not exist")
        public void whenSendingDirectMessageShouldReturnHttpNotFoundIfRecipientDoesNotExist() throws Exception {
            sendDirectMessageDto.setRecipientId(UserConstants.NOT_EXISTING_USER_ID);

            expectErrorJson(
                    postDirectMessage(firstUserJwt, sendDirectMessageDto),
                    HttpStatus.NOT_FOUND,
                    UserNotFoundException.DEFAULT_MESSAGE);

            assertNoConversationDataCreated();
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

            assertSingleDirectConversationCreated(response);
        }

        @Test
        @DisplayName("When sending direct message should reuse existing direct conversation")
        public void whenSendingDirectMessageShouldReuseExistingDirectConversation() throws Exception {
            DirectMessageResponseDto firstResponse = sendDirectMessage(firstUserJwt, sendDirectMessageDto);
            SendDirectMessageDto secondMessageDto = SendDirectMessageDtoTestBuilder.firstDirectMessage()
                    .recipientId(secondUser.getId())
                    .content(MessageConstants.SECOND_MESSAGE_CONTENT)
                    .build();

            DirectMessageResponseDto secondResponse = sendDirectMessage(firstUserJwt, secondMessageDto);

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
                softly.assertThat(encryptionUtils.decryptMessage(newestMessage.getContent()))
                        .isEqualTo(MessageConstants.SECOND_MESSAGE_CONTENT);
            });
            assertParticipantReadMetadata(firstResponse.conversationId(), firstUser, TimeConstants.NOW, newestMessage.getId());
            assertParticipantReadMetadata(firstResponse.conversationId(), secondUser, null, null);
        }

        @Test
        @DisplayName("When sending direct message in inverse direction should reuse existing direct conversation")
        public void whenSendingDirectMessageInInverseDirectionShouldReuseExistingDirectConversation() throws Exception {
            DirectMessageResponseDto firstResponse = sendDirectMessage(firstUserJwt, sendDirectMessageDto);
            Message firstSavedMessage = messageRepository.findAll().getFirst();
            SendDirectMessageDto inverseMessageDto = SendDirectMessageDtoTestBuilder.inverseDirectMessage()
                    .recipientId(firstUser.getId())
                    .build();

            DirectMessageResponseDto inverseResponse = sendDirectMessage(secondUserJwt, inverseMessageDto);

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
            Conversation groupConversation = conversationRepository.save(Conversation.builder()
                    .type(ConversationType.GROUP)
                    .createdAt(TimeConstants.NOW)
                    .lastActiveAt(TimeConstants.NOW)
                    .build());
            conversationParticipantRepository.save(ConversationParticipant.builder()
                    .conversation(groupConversation)
                    .user(firstUser)
                    .joinedAt(TimeConstants.NOW)
                    .build());
            conversationParticipantRepository.save(ConversationParticipant.builder()
                    .conversation(groupConversation)
                    .user(secondUser)
                    .joinedAt(TimeConstants.NOW)
                    .build());

            DirectMessageResponseDto response = sendDirectMessage(firstUserJwt, sendDirectMessageDto);

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
                    .andExpect(jsonPath("$.message.content").value(content))
                    .andExpect(jsonPath("$.message.sentDate").isNotEmpty())
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
            Conversation savedConversation = conversations.getFirst();
            DirectConversationPair savedDirectConversationPair = directConversationPairs.getFirst();
            Message savedMessage = messages.getFirst();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.conversationId()).isEqualTo(savedConversation.getId());
                softly.assertThat(response.conversationCreated()).isTrue();
                softly.assertThat(response.message().getSenderId()).isEqualTo(firstUser.getId());
                softly.assertThat(conversations).hasSize(1);
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
                softly.assertThat(encryptionUtils.decryptMessage(savedMessage.getContent())).isEqualTo(expectedContent);
            });
        }

        private DirectConversationPair findDirectConversationPair(UUID conversationId) {
            return directConversationPairRepository.findAll().stream()
                    .filter(pair -> pair.getConversation().getId().equals(conversationId))
                    .findFirst()
                    .orElseThrow();
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
        @DisplayName("When getting conversations should return HTTP 403 Forbidden if there is no Authorization header")
        public void whenGettingConversationsShouldReturnHttpForbiddenIfThereIsNoAuthorizationHeader() throws Exception {
            getConversationsWithoutAuth()
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When getting conversations should return HTTP 403 Forbidden if Authorization header is empty")
        public void whenGettingConversationsShouldReturnHttpForbiddenIfAuthorizationHeaderIsEmpty() throws Exception {
            getConversations("", null)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When getting conversations should return HTTP 403 Forbidden if token is malformed")
        public void whenGettingConversationsShouldReturnHttpForbiddenIfTokenIsMalformed() throws Exception {
            getConversations(AuthConstants.JWT_PREFIX + "invalid-token", null)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When getting conversations should return HTTP 400 Bad Request if page is not a number")
        public void whenGettingConversationsShouldReturnHttpBadRequestIfPageIsNotNumber() throws Exception {
            getConversations(firstUserJwt, "not-a-number")
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("When getting conversations should return HTTP 400 Bad Request if page is negative")
        public void whenGettingConversationsShouldReturnHttpBadRequestIfPageIsNegative() throws Exception {
            expectErrorJson(
                    getConversations(firstUserJwt, PaginationConstants.PAGE_MINUS_ONE),
                    HttpStatus.BAD_REQUEST,
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
            Conversation conversation = conversationRepository.save(Conversation.builder()
                    .type(ConversationType.GROUP)
                    .name(ConversationConstants.FIRST_GROUP_CONVERSATION_NAME)
                    .createdAt(TimeConstants.TWO_HOURS_AGO)
                    .lastActiveAt(TimeConstants.NOW)
                    .build());

            for (User participant : participants) {
                conversationParticipantRepository.save(ConversationParticipant.builder()
                        .conversation(conversation)
                        .user(participant)
                        .joinedAt(TimeConstants.TWO_HOURS_AGO)
                        .build());
            }

            return conversation;
        }
    }

    @Nested
    @DisplayName("Get conversation tests: GET /api/v1/conversations/{conversationId}")
    class GetConversationTests {

        @Test
        @DisplayName("When getting conversation should return HTTP 403 Forbidden if there is no Authorization header")
        public void whenGettingConversationShouldReturnHttpForbiddenIfThereIsNoAuthorizationHeader() throws Exception {
            getConversationWithoutAuth(ConversationConstants.FIRST_CONVERSATION_ID)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When getting conversation should return HTTP 403 Forbidden if Authorization header is empty")
        public void whenGettingConversationShouldReturnHttpForbiddenIfAuthorizationHeaderIsEmpty() throws Exception {
            getConversation("", ConversationConstants.FIRST_CONVERSATION_ID)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When getting conversation should return HTTP 403 Forbidden if token is malformed")
        public void whenGettingConversationShouldReturnHttpForbiddenIfTokenIsMalformed() throws Exception {
            getConversation(AuthConstants.JWT_PREFIX + "invalid-token", ConversationConstants.FIRST_CONVERSATION_ID)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When getting conversation should return HTTP 400 Bad Request if conversation id is malformed")
        public void whenGettingConversationShouldReturnHttpBadRequestIfConversationIdIsMalformed() throws Exception {
            getConversation(firstUserJwt, "not-a-uuid")
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("When getting conversation should return HTTP 404 Not Found if conversation does not exist")
        public void whenGettingConversationShouldReturnHttpNotFoundIfConversationDoesNotExist() throws Exception {
            expectErrorJson(
                    getConversation(firstUserJwt, ConversationConstants.FIRST_CONVERSATION_ID),
                    HttpStatus.NOT_FOUND,
                    ConversationNotFoundException.DEFAULT_MESSAGE);
        }

        @Test
        @DisplayName("When getting conversation should return HTTP 404 Not Found if user is not participant")
        public void whenGettingConversationShouldReturnHttpNotFoundIfUserIsNotParticipant() throws Exception {
            Conversation conversation = createGroupConversation(firstUser);

            expectErrorJson(
                    getConversation(secondUserJwt, conversation.getId()),
                    HttpStatus.NOT_FOUND,
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
                    ConversationNotFoundException.DEFAULT_MESSAGE);
        }

        @Test
        @DisplayName("When getting direct conversation as first user should return second user full name")
        public void whenGettingDirectConversationAsFirstUserShouldReturnSecondUserFullName() throws Exception {
            DirectMessageResponseDto directMessageResponse = createDirectConversation();
            Conversation conversation = conversationRepository.findById(directMessageResponse.conversationId()).orElseThrow();

            ConversationDetailsDto response = expectConversationDetailsJson(
                    getConversation(firstUserJwt, conversation.getId()),
                    conversation,
                    secondUser.getFullName(),
                    2);

            assertParticipantDto(response, firstUser, findParticipant(conversation.getId(), firstUser));
            assertParticipantDto(response, secondUser, findParticipant(conversation.getId(), secondUser));
        }

        @Test
        @DisplayName("When getting direct conversation as second user should return first user full name")
        public void whenGettingDirectConversationAsSecondUserShouldReturnFirstUserFullName() throws Exception {
            DirectMessageResponseDto directMessageResponse = createDirectConversation();
            Conversation conversation = conversationRepository.findById(directMessageResponse.conversationId()).orElseThrow();

            ConversationDetailsDto response = expectConversationDetailsJson(
                    getConversation(secondUserJwt, conversation.getId()),
                    conversation,
                    firstUser.getFullName(),
                    2);

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
            Conversation conversation = conversationRepository.save(Conversation.builder()
                    .type(ConversationType.GROUP)
                    .name(ConversationConstants.FIRST_GROUP_CONVERSATION_NAME)
                    .createdAt(TimeConstants.TWO_HOURS_AGO)
                    .lastActiveAt(TimeConstants.ONE_HOUR_AGO)
                    .build());

            for (User participant : participants) {
                conversationParticipantRepository.save(ConversationParticipant.builder()
                        .conversation(conversation)
                        .user(participant)
                        .joinedAt(TimeConstants.TWO_HOURS_AGO)
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
                    .andExpect(jsonPath("$.createdAt").isNotEmpty())
                    .andExpect(jsonPath("$.lastActiveAt").isNotEmpty())
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
            ConversationParticipantDto participantDto = response.participants().stream()
                    .filter(dto -> dto.userId().equals(user.getId()))
                    .findFirst()
                    .orElseThrow();

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
        @DisplayName("When getting messages should return HTTP 403 Forbidden if there is no Authorization header")
        public void whenGettingMessagesShouldReturnHttpForbiddenIfThereIsNoAuthorizationHeader() throws Exception {
            getConversationMessagesWithoutAuth(ConversationConstants.FIRST_CONVERSATION_ID)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When getting messages should return HTTP 403 Forbidden if Authorization header is empty")
        public void whenGettingMessagesShouldReturnHttpForbiddenIfAuthorizationHeaderIsEmpty() throws Exception {
            getConversationMessages("", ConversationConstants.FIRST_CONVERSATION_ID, null)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When getting messages should return HTTP 403 Forbidden if token is malformed")
        public void whenGettingMessagesShouldReturnHttpForbiddenIfTokenIsMalformed() throws Exception {
            getConversationMessages(
                    AuthConstants.JWT_PREFIX + "invalid-token",
                    ConversationConstants.FIRST_CONVERSATION_ID,
                    null)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When getting messages should return HTTP 400 Bad Request if conversation id is malformed")
        public void whenGettingMessagesShouldReturnHttpBadRequestIfConversationIdIsMalformed() throws Exception {
            getConversationMessages(firstUserJwt, "not-a-uuid", null)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("When getting messages should return HTTP 400 Bad Request if page is not a number")
        public void whenGettingMessagesShouldReturnHttpBadRequestIfPageIsNotNumber() throws Exception {
            getConversationMessages(firstUserJwt, ConversationConstants.FIRST_CONVERSATION_ID, "not-a-number")
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("When getting messages should return HTTP 400 Bad Request if page is negative")
        public void whenGettingMessagesShouldReturnHttpBadRequestIfPageIsNegative() throws Exception {
            DirectMessageResponseDto response = createConversationWithMessages(1).getFirst();

            expectErrorJson(
                    getConversationMessages(firstUserJwt, response.conversationId(), PaginationConstants.PAGE_MINUS_ONE),
                    HttpStatus.BAD_REQUEST,
                    InvalidPageNumberException.DEFAULT_MESSAGE);
        }

        @Test
        @DisplayName("When getting messages should return HTTP 404 Not Found if conversation does not exist")
        public void whenGettingMessagesShouldReturnHttpNotFoundIfConversationDoesNotExist() throws Exception {
            expectErrorJson(
                    getConversationMessagesWithoutPage(firstUserJwt, ConversationConstants.FIRST_CONVERSATION_ID),
                    HttpStatus.NOT_FOUND,
                    ConversationNotFoundException.DEFAULT_MESSAGE);
        }

        @Test
        @DisplayName("When getting messages should return HTTP 404 Not Found if user is not participant")
        public void whenGettingMessagesShouldReturnHttpNotFoundIfUserIsNotParticipant() throws Exception {
            Conversation conversation = conversationRepository.save(Conversation.builder()
                    .type(ConversationType.DIRECT)
                    .createdAt(TimeConstants.NOW)
                    .lastActiveAt(TimeConstants.NOW)
                    .build());
            conversationParticipantRepository.save(ConversationParticipant.builder()
                    .conversation(conversation)
                    .user(firstUser)
                    .joinedAt(TimeConstants.NOW)
                    .build());

            expectErrorJson(
                    getConversationMessagesWithoutPage(secondUserJwt, conversation.getId()),
                    HttpStatus.NOT_FOUND,
                    ConversationNotFoundException.DEFAULT_MESSAGE);
        }

        @Test
        @DisplayName("When getting messages should return HTTP 404 Not Found if participant has left conversation")
        public void whenGettingMessagesShouldReturnHttpNotFoundIfParticipantHasLeftConversation() throws Exception {
            DirectMessageResponseDto response = createConversationWithMessages(1).getFirst();
            ConversationParticipant secondParticipant = findParticipant(response.conversationId(), secondUser);
            secondParticipant.setLeftAt(TimeConstants.NOW);
            conversationParticipantRepository.save(secondParticipant);

            expectErrorJson(
                    getConversationMessagesWithoutPage(secondUserJwt, response.conversationId()),
                    HttpStatus.NOT_FOUND,
                    ConversationNotFoundException.DEFAULT_MESSAGE);
        }

        @Test
        @DisplayName("When getting messages without page param should return first page")
        public void whenGettingMessagesWithoutPageParamShouldReturnFirstPage() throws Exception {
            DirectMessageResponseDto response = createConversationWithMessages(2).getFirst();

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

            expectMessagePageJson(
                    getConversationMessages(secondUserJwt, response.conversationId(), PaginationConstants.PAGE_ZERO),
                    1,
                    PaginationConstants.PAGE_ZERO,
                    1,
                    1,
                    true)
                    .andExpect(jsonPath("$.messages[0].content").value(messageContent(0)))
                    .andExpect(jsonPath("$.messages[0].sentDate").isNotEmpty())
                    .andExpect(jsonPath("$.messages[0].senderId").value(firstUser.getId().toString()));

            Message savedMessage = messageRepository.findAll().getFirst();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(savedMessage.getContent()).isNotEqualTo(messageContent(0));
                softly.assertThat(encryptionUtils.decryptMessage(savedMessage.getContent())).isEqualTo(messageContent(0));
            });
        }

        @Test
        @DisplayName("When getting messages should return newest messages first with stable id order")
        public void whenGettingMessagesShouldReturnNewestMessagesFirstWithStableIdOrder() throws Exception {
            DirectMessageResponseDto response = createConversationWithMessages(3).getFirst();

            expectMessagePageJson(
                    getConversationMessages(secondUserJwt, response.conversationId(), PaginationConstants.PAGE_ZERO),
                    3,
                    PaginationConstants.PAGE_ZERO,
                    3,
                    1,
                    true)
                    .andExpect(jsonPath("$.messages[0].content").value(messageContent(2)))
                    .andExpect(jsonPath("$.messages[1].content").value(messageContent(1)))
                    .andExpect(jsonPath("$.messages[2].content").value(messageContent(0)));
        }

        @Test
        @DisplayName("When getting newest messages should persist participant read metadata")
        public void whenGettingNewestMessagesShouldPersistParticipantReadMetadata() throws Exception {
            DirectMessageResponseDto response = createConversationWithMessages(2).getFirst();
            ConversationParticipant participantBeforeRead = findParticipant(response.conversationId(), secondUser);
            Message newestMessage = getNewestMessage(messageRepository.findAll());

            getConversationMessages(secondUserJwt, response.conversationId(), PaginationConstants.PAGE_ZERO)
                    .andExpect(status().isOk());

            ConversationParticipant participantAfterRead = findParticipant(response.conversationId(), secondUser);
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(participantBeforeRead.getLastReadAt()).isNull();
                softly.assertThat(participantBeforeRead.getLastReadMessageId()).isNull();
                softly.assertThat(participantAfterRead.getLastReadAt()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(participantAfterRead.getLastReadMessageId()).isEqualTo(newestMessage.getId());
            });
        }

        @Test
        @DisplayName("When getting older messages should return second page and not update read metadata")
        public void whenGettingOlderMessagesShouldReturnSecondPageAndNotUpdateReadMetadata() throws Exception {
            DirectMessageResponseDto response = createConversationWithMessages(PaginationConstants.DEFAULT_PAGE_SIZE + 1)
                    .getFirst();
            ConversationParticipant participantBeforeRead = findParticipant(response.conversationId(), secondUser);
            participantBeforeRead.setLastReadAt(TimeConstants.ONE_HOUR_AGO);
            participantBeforeRead.setLastReadMessageId(MessageConstants.SECOND_MESSAGE_ID);
            conversationParticipantRepository.save(participantBeforeRead);

            expectMessagePageJson(
                    getConversationMessages(secondUserJwt, response.conversationId(), PaginationConstants.PAGE_ONE),
                    1,
                    PaginationConstants.PAGE_ONE,
                    PaginationConstants.DEFAULT_PAGE_SIZE + 1,
                    2,
                    true)
                    .andExpect(jsonPath("$.messages[0].content").value(messageContent(0)));

            assertParticipantReadMetadata(
                    response.conversationId(),
                    secondUser,
                    TimeConstants.ONE_HOUR_AGO,
                    MessageConstants.SECOND_MESSAGE_ID);
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
}
