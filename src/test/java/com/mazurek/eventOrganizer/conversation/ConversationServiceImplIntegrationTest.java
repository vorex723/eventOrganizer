package com.mazurek.eventOrganizer.conversation;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.ActivationToken;
import com.mazurek.eventOrganizer.auth.ActivationTokenRepository;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipant;
import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipantRepository;
import com.mazurek.eventOrganizer.conversation.direct.DirectConversationPair;
import com.mazurek.eventOrganizer.conversation.direct.DirectConversationPairRepository;
import com.mazurek.eventOrganizer.conversation.dto.ConversationOverviewDto;
import com.mazurek.eventOrganizer.conversation.dto.ConversationOverviewPageDto;
import com.mazurek.eventOrganizer.conversation.dto.DirectMessageResponseDto;
import com.mazurek.eventOrganizer.conversation.dto.MessagePageDto;
import com.mazurek.eventOrganizer.conversation.dto.SendDirectMessageDto;
import com.mazurek.eventOrganizer.conversation.message.Message;
import com.mazurek.eventOrganizer.conversation.message.MessageRepository;
import com.mazurek.eventOrganizer.exception.auth.ActivationTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import com.mazurek.eventOrganizer.exception.conversation.ConversationNotFoundException;
import com.mazurek.eventOrganizer.exception.conversation.MessagingYourselfException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.jwt.JwtUserDetails;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.builders.dto.RegisterRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.SendDirectMessageDtoTestBuilder;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import com.mazurek.eventOrganizer.utils.EncryptionUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Profile("test")
@DisplayName("ConversationService integration tests:")
public class ConversationServiceImplIntegrationTest {

    @Autowired
    private ConversationService conversationService;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private ActivationTokenRepository activationTokenRepository;
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
    private AuthHelper authHelper;
    @Autowired
    private DeletionService deletionService;
    @Autowired
    private EncryptionUtils encryptionUtils;

    private User firstUser;
    private User secondUser;

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();

        firstUser = userRepository.findByEmail(UserConstants.FIRST_USER_EMAIL)
                .orElseThrow(UserNotFoundException::new);
        secondUser = userRepository.findByEmail(UserConstants.SECOND_USER_EMAIL)
                .orElseThrow(UserNotFoundException::new);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
    }

    private DirectMessageResponseDto sendMessageAsFirstUser(String content) {
        authHelper.setupSecurityContextForFirstUser();
        return conversationService.sendDirectMessage(SendDirectMessageDtoTestBuilder.firstDirectMessage()
                .recipientId(secondUser.getId())
                .content(content)
                .build());
    }

    private DirectMessageResponseDto sendMessageAsSecondUser(String content) {
        authHelper.setupSecurityContextForSecondUser();
        return conversationService.sendDirectMessage(SendDirectMessageDtoTestBuilder.inverseDirectMessage()
                .recipientId(firstUser.getId())
                .content(content)
                .build());
    }

    private MessagePageDto getMessagesAsFirstUser(UUID conversationId, int pageNumber) {
        authHelper.setupSecurityContextForFirstUser();
        return conversationService.getMessagesInConversation(conversationId, pageNumber);
    }

    private MessagePageDto getMessagesAsSecondUser(UUID conversationId, int pageNumber) {
        authHelper.setupSecurityContextForSecondUser();
        return conversationService.getMessagesInConversation(conversationId, pageNumber);
    }

    private void setupSecurityContextForUserId(UUID userId) {
        SecurityContextHolder.clearContext();
        JwtUserDetails userDetails = new JwtUserDetails(
                userId,
                UserConstants.NOT_EXISTING_USER_EMAIL,
                List.of(new SimpleGrantedAuthority(RoleConstants.ROLE_USER_NAME))
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    private User registerAndActivateThirdUser() {
        authenticationService.register(RegisterRequestTestBuilder.thirdUserRegisterRequest().build());
        ActivationToken activationToken = activationTokenRepository
                .findByIgnoreCaseUserEmail(UserConstants.THIRD_USER_EMAIL)
                .orElseThrow(ActivationTokenNotFoundException::new);
        authenticationService.activateAccount(activationToken.getToken());

        return userRepository.findByEmail(UserConstants.THIRD_USER_EMAIL)
                .orElseThrow(UserNotFoundException::new);
    }

    private ConversationParticipant findParticipant(UUID conversationId, User user) {
        return conversationParticipantRepository.findByConversationIdAndUserId(conversationId, user.getId())
                .orElseThrow();
    }

    private DirectConversationPair findDirectConversationPair(UUID conversationId) {
        return directConversationPairRepository.findAll().stream()
                .filter(pair -> pair.getConversation().getId().equals(conversationId))
                .findFirst()
                .orElseThrow();
    }

    private UUID canonicalFirstUserId(User firstUser, User secondUser) {
        return firstUser.getId().toString().compareTo(secondUser.getId().toString()) < 0
                ? firstUser.getId()
                : secondUser.getId();
    }

    private UUID canonicalSecondUserId(User firstUser, User secondUser) {
        return firstUser.getId().toString().compareTo(secondUser.getId().toString()) < 0
                ? secondUser.getId()
                : firstUser.getId();
    }

    @Nested
    @DisplayName("Send direct message tests:")
    class SendDirectMessageTests {

        private SendDirectMessageDto sendDirectMessageDto;

        @BeforeEach
        void setUp() {
            sendDirectMessageDto = SendDirectMessageDtoTestBuilder.firstDirectMessage()
                    .recipientId(secondUser.getId())
                    .build();
        }

        @Test
        @DisplayName("When sending direct message should create conversation message and participants")
        public void whenSendingDirectMessageShouldCreateConversationMessageAndParticipants() {
            DirectMessageResponseDto response = sendDirectMessageAsFirstUser(sendDirectMessageDto);

            Conversation savedConversation = findConversation(response.conversationId());
            Message savedMessage = messageRepository.findAll().getFirst();

            assertDirectMessageResponse(response, savedConversation.getId(), true, MessageConstants.FIRST_MESSAGE_CONTENT, firstUser);
            assertConversationDataCounts(1, 2, 1, 1);
            assertDirectConversation(savedConversation, TimeConstants.NOW, TimeConstants.NOW);
            assertDirectConversationPair(response.conversationId(), firstUser, secondUser);
            assertParticipant(response.conversationId(), firstUser, TimeConstants.NOW, TimeConstants.NOW, savedMessage.getId());
            assertParticipant(response.conversationId(), secondUser, TimeConstants.NOW, null, null);
            assertEncryptedMessage(savedMessage, response.conversationId(), firstUser, MessageConstants.FIRST_MESSAGE_CONTENT);
        }

        @Test
        @DisplayName("When sending direct message should reuse existing direct conversation")
        public void whenSendingDirectMessageShouldReuseExistingDirectConversation() {
            DirectMessageResponseDto firstResponse = sendDirectMessageAsFirstUser(sendDirectMessageDto);
            DirectMessageResponseDto secondResponse = sendDirectMessageAsFirstUser(
                    firstMessageDto(MessageConstants.SECOND_MESSAGE_CONTENT));

            Message newestMessage = getNewestMessage();
            Conversation conversation = findConversation(firstResponse.conversationId());

            assertDirectMessageResponse(secondResponse, firstResponse.conversationId(), false, MessageConstants.SECOND_MESSAGE_CONTENT, firstUser);
            assertConversationDataCounts(1, 2, 1, 2);
            assertDirectConversationPair(firstResponse.conversationId(), firstUser, secondUser);
            assertDirectConversation(conversation, TimeConstants.NOW, TimeConstants.NOW);
            assertParticipantReadMetadata(firstResponse.conversationId(), firstUser, TimeConstants.NOW, newestMessage.getId());
            assertParticipantReadMetadata(firstResponse.conversationId(), secondUser, null, null);
            assertEncryptedMessage(newestMessage, firstResponse.conversationId(), firstUser, MessageConstants.SECOND_MESSAGE_CONTENT);
        }

        @Test
        @DisplayName("When sending direct message in inverse direction should reuse existing direct conversation")
        public void whenSendingDirectMessageInInverseDirectionShouldReuseExistingDirectConversation() {
            DirectMessageResponseDto firstResponse = sendDirectMessageAsFirstUser(sendDirectMessageDto);
            Message firstSavedMessage = messageRepository.findAll().getFirst();

            DirectMessageResponseDto inverseResponse = sendDirectMessageAsSecondUser(
                    inverseMessageDto(MessageConstants.SECOND_MESSAGE_CONTENT));

            Message inverseSavedMessage = getNewestMessage();

            assertDirectMessageResponse(inverseResponse, firstResponse.conversationId(), false, MessageConstants.SECOND_MESSAGE_CONTENT, secondUser);
            assertConversationDataCounts(1, 2, 1, 2);
            assertDirectConversationPair(firstResponse.conversationId(), firstUser, secondUser);
            assertParticipantReadMetadata(firstResponse.conversationId(), firstUser, TimeConstants.NOW, firstSavedMessage.getId());
            assertParticipantReadMetadata(firstResponse.conversationId(), secondUser, TimeConstants.NOW, inverseSavedMessage.getId());
        }

        @Test
        @DisplayName("When sending direct message should not reuse group conversation")
        public void whenSendingDirectMessageShouldNotReuseGroupConversation() {
            Conversation groupConversation = createGroupConversation(firstUser, secondUser);

            DirectMessageResponseDto response = sendDirectMessageAsFirstUser(sendDirectMessageDto);

            List<Conversation> conversations = conversationRepository.findAll();
            List<Conversation> directConversations = findDirectConversations(conversations);
            List<Conversation> groupConversations = findGroupConversations(conversations);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.conversationCreated()).isTrue();
                softly.assertThat(conversations).hasSize(2);
                softly.assertThat(directConversations).hasSize(1);
                softly.assertThat(groupConversations).hasSize(1);
                softly.assertThat(response.conversationId()).isEqualTo(directConversations.getFirst().getId());
                softly.assertThat(response.conversationId()).isNotEqualTo(groupConversation.getId());
            });

            assertDirectConversationPair(response.conversationId(), firstUser, secondUser);
        }

        @Test
        @DisplayName("When sending direct message should reuse direct pair even if participant has left conversation")
        public void whenSendingDirectMessageShouldReuseDirectPairEvenIfParticipantHasLeftConversation() {
            DirectMessageResponseDto firstResponse = sendMessageAsFirstUser(MessageConstants.FIRST_MESSAGE_CONTENT);
            ConversationParticipant recipientParticipant =
                    ConversationServiceImplIntegrationTest.this.findParticipant(firstResponse.conversationId(), secondUser);
            recipientParticipant.setLeftAt(TimeConstants.ONE_HOUR_AGO);
            conversationParticipantRepository.save(recipientParticipant);

            DirectMessageResponseDto secondResponse = sendMessageAsFirstUser(MessageConstants.SECOND_MESSAGE_CONTENT);

            assertDirectConversationReused(secondResponse, firstResponse.conversationId());
        }

        @Test
        @DisplayName("When sending direct message should reuse direct pair even if conversation has extra participant")
        public void whenSendingDirectMessageShouldReuseDirectPairEvenIfConversationHasExtraParticipant() {
            User thirdUser = registerAndActivateThirdUser();
            DirectMessageResponseDto firstResponse = sendMessageAsFirstUser(MessageConstants.FIRST_MESSAGE_CONTENT);
            Conversation existingConversation = conversationRepository.findById(firstResponse.conversationId()).orElseThrow();
            conversationParticipantRepository.save(ConversationParticipant.builder()
                    .conversation(existingConversation)
                    .user(thirdUser)
                    .joinedAt(TimeConstants.NOW)
                    .build());

            DirectMessageResponseDto secondResponse = sendMessageAsFirstUser(MessageConstants.SECOND_MESSAGE_CONTENT);

            assertDirectConversationReused(secondResponse, firstResponse.conversationId());
        }

        @Test
        @DisplayName("When sending direct message should throw MessagingYourselfException if user tries to message himself")
        public void whenSendingDirectMessageShouldThrowMessagingYourselfExceptionIfUserTriesToMessageHimself() {
            authHelper.setupSecurityContextForFirstUser();
            sendDirectMessageDto.setRecipientId(firstUser.getId());

            assertThatThrownBy(() -> conversationService.sendDirectMessage(sendDirectMessageDto))
                    .isInstanceOf(MessagingYourselfException.class);

            assertNoConversationDataCreated();
        }

        @Test
        @DisplayName("When sending direct message should throw UserNotFoundException if recipient does not exist")
        public void whenSendingDirectMessageShouldThrowUserNotFoundExceptionIfRecipientDoesNotExist() {
            authHelper.setupSecurityContextForFirstUser();
            sendDirectMessageDto.setRecipientId(UserConstants.NOT_EXISTING_USER_ID);

            assertThatThrownBy(() -> conversationService.sendDirectMessage(sendDirectMessageDto))
                    .isInstanceOf(UserNotFoundException.class);

            assertNoConversationDataCreated();
        }

        private DirectMessageResponseDto sendDirectMessageAsFirstUser(SendDirectMessageDto dto) {
            authHelper.setupSecurityContextForFirstUser();
            return conversationService.sendDirectMessage(dto);
        }

        private DirectMessageResponseDto sendDirectMessageAsSecondUser(SendDirectMessageDto dto) {
            authHelper.setupSecurityContextForSecondUser();
            return conversationService.sendDirectMessage(dto);
        }

        private SendDirectMessageDto firstMessageDto(String content) {
            return SendDirectMessageDtoTestBuilder.firstDirectMessage()
                    .recipientId(secondUser.getId())
                    .content(content)
                    .build();
        }

        private SendDirectMessageDto inverseMessageDto(String content) {
            return SendDirectMessageDtoTestBuilder.inverseDirectMessage()
                    .recipientId(firstUser.getId())
                    .content(content)
                    .build();
        }

        private Conversation findConversation(UUID conversationId) {
            return conversationRepository.findById(conversationId).orElseThrow();
        }

        private Message getNewestMessage() {
            return messageRepository.findAll().stream()
                    .max(Comparator.comparing(Message::getId))
                    .orElseThrow();
        }

        private Conversation createGroupConversation(User firstParticipant, User secondParticipant) {
            Conversation groupConversation = conversationRepository.save(Conversation.builder()
                    .type(ConversationType.GROUP)
                    .createdAt(TimeConstants.NOW)
                    .lastActiveAt(TimeConstants.NOW)
                    .build());
            conversationParticipantRepository.save(ConversationParticipant.builder()
                    .conversation(groupConversation)
                    .user(firstParticipant)
                    .joinedAt(TimeConstants.NOW)
                    .build());
            conversationParticipantRepository.save(ConversationParticipant.builder()
                    .conversation(groupConversation)
                    .user(secondParticipant)
                    .joinedAt(TimeConstants.NOW)
                    .build());

            return groupConversation;
        }

        private List<Conversation> findDirectConversations(List<Conversation> conversations) {
            return conversations.stream()
                    .filter(conversation -> conversation.getType() == ConversationType.DIRECT)
                    .toList();
        }

        private List<Conversation> findGroupConversations(List<Conversation> conversations) {
            return conversations.stream()
                    .filter(conversation -> conversation.getType() == ConversationType.GROUP)
                    .toList();
        }

        private void assertDirectMessageResponse(
                DirectMessageResponseDto response,
                UUID conversationId,
                boolean conversationCreated,
                String content,
                User sender) {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.conversationId()).isEqualTo(conversationId);
                softly.assertThat(response.conversationCreated()).isEqualTo(conversationCreated);
                softly.assertThat(response.message().getContent()).isEqualTo(content);
                softly.assertThat(response.message().getSender().getId()).isEqualTo(sender.getId());
                softly.assertThat(response.message().getSentDate()).isEqualTo(TimeConstants.NOW);
            });
        }

        private void assertConversationDataCounts(
                int expectedConversations,
                int expectedParticipants,
                int expectedDirectPairs,
                int expectedMessages) {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(conversationRepository.findAll()).hasSize(expectedConversations);
                softly.assertThat(conversationParticipantRepository.findAll()).hasSize(expectedParticipants);
                softly.assertThat(directConversationPairRepository.findAll()).hasSize(expectedDirectPairs);
                softly.assertThat(messageRepository.findAll()).hasSize(expectedMessages);
            });
        }

        private void assertDirectConversation(Conversation conversation, Instant createdAt, Instant lastActiveAt) {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(conversation.getType()).isEqualTo(ConversationType.DIRECT);
                softly.assertThat(conversation.getCreatedAt()).isEqualTo(createdAt);
                softly.assertThat(conversation.getLastActiveAt()).isEqualTo(lastActiveAt);
            });
        }

        private void assertDirectConversationPair(UUID conversationId, User firstUser, User secondUser) {
            DirectConversationPair directConversationPair = findDirectConversationPair(conversationId);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(directConversationPairRepository.findAll()).hasSize(1);
                softly.assertThat(directConversationPair.getConversation().getId()).isEqualTo(conversationId);
                softly.assertThat(directConversationPair.getFirstUserId()).isEqualTo(canonicalFirstUserId(firstUser, secondUser));
                softly.assertThat(directConversationPair.getSecondUserId()).isEqualTo(canonicalSecondUserId(firstUser, secondUser));
            });
        }

        private void assertParticipant(
                UUID conversationId,
                User user,
                Instant joinedAt,
                Instant lastReadAt,
                Long lastReadMessageId) {
            ConversationParticipant participant = ConversationServiceImplIntegrationTest.this.findParticipant(conversationId, user);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(participant.getConversation().getId()).isEqualTo(conversationId);
                softly.assertThat(participant.getUser().getId()).isEqualTo(user.getId());
                softly.assertThat(participant.getJoinedAt()).isEqualTo(joinedAt);
                softly.assertThat(participant.getLastReadAt()).isEqualTo(lastReadAt);
                softly.assertThat(participant.getLastReadMessageId()).isEqualTo(lastReadMessageId);
            });
        }

        private void assertParticipantReadMetadata(
                UUID conversationId,
                User user,
                Instant lastReadAt,
                Long lastReadMessageId) {
            ConversationParticipant participant = ConversationServiceImplIntegrationTest.this.findParticipant(conversationId, user);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(participant.getLastReadAt()).isEqualTo(lastReadAt);
                softly.assertThat(participant.getLastReadMessageId()).isEqualTo(lastReadMessageId);
            });
        }

        private void assertEncryptedMessage(Message message, UUID conversationId, User sender, String decryptedContent) {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(message.getConversation().getId()).isEqualTo(conversationId);
                softly.assertThat(message.getSender().getId()).isEqualTo(sender.getId());
                softly.assertThat(message.getSentDate()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(message.getContent()).isNotEqualTo(decryptedContent);
                softly.assertThat(encryptionUtils.decryptMessage(message.getContent())).isEqualTo(decryptedContent);
            });
        }

        private void assertDirectConversationReused(DirectMessageResponseDto response, UUID conversationId) {
            List<Conversation> directConversations = findDirectConversations(conversationRepository.findAll());

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.conversationCreated()).isFalse();
                softly.assertThat(response.conversationId()).isEqualTo(conversationId);
                softly.assertThat(directConversations).hasSize(1);
            });

            assertDirectConversationPair(conversationId, firstUser, secondUser);
        }

        private void assertNoConversationDataCreated() {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(conversationRepository.findAll()).isEmpty();
                softly.assertThat(conversationParticipantRepository.findAll()).isEmpty();
                softly.assertThat(directConversationPairRepository.findAll()).isEmpty();
                softly.assertThat(messageRepository.findAll()).isEmpty();
            });
        }
    }

    @Nested
    @DisplayName("Get conversations tests:")
    class GetConversationsTests {

        @Test
        @DisplayName("When getting conversations should throw InvalidPageNumberException if page number is lower than zero")
        public void whenGettingConversationsShouldThrowInvalidPageNumberExceptionIfPageNumberIsLowerThanZero() {
            authHelper.setupSecurityContextForFirstUser();

            assertThatThrownBy(() -> conversationService.getConversations(PaginationConstants.PAGE_MINUS_ONE))
                    .isInstanceOf(InvalidPageNumberException.class);

            assertNoConversationDataCreated();
        }

        @Test
        @DisplayName("When getting conversations should return empty page if user has no conversations")
        public void whenGettingConversationsShouldReturnEmptyPageIfUserHasNoConversations() {
            ConversationOverviewPageDto response = getConversationsAsFirstUser(PaginationConstants.PAGE_ZERO);

            assertPageMetadata(
                    response,
                    PaginationConstants.PAGE_ZERO,
                    PaginationConstants.DEFAULT_PAGE_SIZE,
                    0,
                    0,
                    true,
                    0
            );
        }

        @Test
        @DisplayName("When getting conversations should return direct conversation with other participant full name")
        public void whenGettingConversationsShouldReturnDirectConversationWithOtherParticipantFullName() {
            DirectMessageResponseDto directMessageResponse = sendMessageAsFirstUser(MessageConstants.FIRST_MESSAGE_CONTENT);
            Conversation conversation = conversationRepository.findById(directMessageResponse.conversationId()).orElseThrow();

            ConversationOverviewPageDto response = getConversationsAsFirstUser(PaginationConstants.PAGE_ZERO);

            ConversationOverviewDto overviewDto = findOverviewById(response, conversation.getId());

            assertPageMetadata(
                    response,
                    PaginationConstants.PAGE_ZERO,
                    PaginationConstants.DEFAULT_PAGE_SIZE,
                    1,
                    1,
                    true,
                    1
            );
            assertOverview(overviewDto, conversation, secondUser.getFullName());
        }

        @Test
        @DisplayName("When getting conversations as second user should return direct conversation with first user full name")
        public void whenGettingConversationsAsSecondUserShouldReturnDirectConversationWithFirstUserFullName() {
            DirectMessageResponseDto directMessageResponse = sendMessageAsFirstUser(MessageConstants.FIRST_MESSAGE_CONTENT);
            Conversation conversation = conversationRepository.findById(directMessageResponse.conversationId()).orElseThrow();

            ConversationOverviewPageDto response = getConversationsAsSecondUser(PaginationConstants.PAGE_ZERO);

            ConversationOverviewDto overviewDto = findOverviewById(response, conversation.getId());

            assertOverview(overviewDto, conversation, firstUser.getFullName());
        }

        @Test
        @DisplayName("When getting conversations should return group conversation with conversation name")
        public void whenGettingConversationsShouldReturnGroupConversationWithConversationName() {
            Conversation conversation = createGroupConversation(firstUser, secondUser, TimeConstants.NOW);

            ConversationOverviewPageDto response = getConversationsAsFirstUser(PaginationConstants.PAGE_ZERO);

            ConversationOverviewDto overviewDto = findOverviewById(response, conversation.getId());

            assertOverview(overviewDto, conversation, ConversationConstants.FIRST_GROUP_CONVERSATION_NAME);
        }

        @Test
        @DisplayName("When getting conversations should return only current user active conversations")
        public void whenGettingConversationsShouldReturnOnlyCurrentUserActiveConversations() {
            User thirdUser = registerAndActivateThirdUser();
            Conversation firstUserConversation = createGroupConversation(firstUser, secondUser, TimeConstants.NOW);
            Conversation otherUsersConversation = createGroupConversation(secondUser, thirdUser, TimeConstants.ONE_HOUR_AGO);

            ConversationOverviewPageDto response = getConversationsAsFirstUser(PaginationConstants.PAGE_ZERO);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.conversations()).hasSize(1);
                softly.assertThat(response.conversations().getFirst().id()).isEqualTo(firstUserConversation.getId());
                softly.assertThat(response.conversations().getFirst().id()).isNotEqualTo(otherUsersConversation.getId());
                softly.assertThat(response.totalElements()).isEqualTo(1);
            });
        }

        @Test
        @DisplayName("When getting conversations should exclude conversation if current participant left")
        public void whenGettingConversationsShouldExcludeConversationIfCurrentParticipantLeft() {
            Conversation conversation = createGroupConversation(firstUser, secondUser, TimeConstants.NOW);
            ConversationParticipant firstUserParticipant = findParticipant(conversation.getId(), firstUser);
            firstUserParticipant.setLeftAt(TimeConstants.ONE_HOUR_AGO);
            conversationParticipantRepository.save(firstUserParticipant);

            ConversationOverviewPageDto firstUserResponse = getConversationsAsFirstUser(PaginationConstants.PAGE_ZERO);
            ConversationOverviewPageDto secondUserResponse = getConversationsAsSecondUser(PaginationConstants.PAGE_ZERO);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(firstUserResponse.conversations()).isEmpty();
                softly.assertThat(firstUserResponse.totalElements()).isZero();
                softly.assertThat(secondUserResponse.conversations()).hasSize(1);
                softly.assertThat(secondUserResponse.conversations().getFirst().id()).isEqualTo(conversation.getId());
            });
        }

        @Test
        @DisplayName("When getting conversations should return conversations ordered by last active at descending")
        public void whenGettingConversationsShouldReturnConversationsOrderedByLastActiveAtDescending() {
            Conversation oldestConversation = createGroupConversation(firstUser, secondUser, TimeConstants.TWO_HOURS_AGO);
            Conversation newestConversation = createGroupConversation(firstUser, secondUser, TimeConstants.NOW);
            Conversation middleConversation = createGroupConversation(firstUser, secondUser, TimeConstants.ONE_HOUR_AGO);

            ConversationOverviewPageDto response = getConversationsAsFirstUser(PaginationConstants.PAGE_ZERO);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.conversations()).hasSize(3);
                softly.assertThat(response.conversations().stream().map(ConversationOverviewDto::id).toList())
                        .containsExactly(
                                newestConversation.getId(),
                                middleConversation.getId(),
                                oldestConversation.getId()
                        );
            });
        }

        @Test
        @DisplayName("When getting conversations should return correct pagination metadata")
        public void whenGettingConversationsShouldReturnCorrectPaginationMetadata() {
            int conversationCount = PaginationConstants.DEFAULT_PAGE_SIZE + 1;
            IntStream.range(0, conversationCount)
                    .forEach(index -> createGroupConversation(
                            firstUser,
                            secondUser,
                            TimeConstants.NOW.plusSeconds(index)
                    ));

            ConversationOverviewPageDto firstPageResponse = getConversationsAsFirstUser(PaginationConstants.PAGE_ZERO);
            ConversationOverviewPageDto secondPageResponse = getConversationsAsFirstUser(PaginationConstants.PAGE_ONE);

            assertPageMetadata(
                    firstPageResponse,
                    PaginationConstants.PAGE_ZERO,
                    PaginationConstants.DEFAULT_PAGE_SIZE,
                    conversationCount,
                    2,
                    false,
                    PaginationConstants.DEFAULT_PAGE_SIZE
            );
            assertPageMetadata(
                    secondPageResponse,
                    PaginationConstants.PAGE_ONE,
                    PaginationConstants.DEFAULT_PAGE_SIZE,
                    conversationCount,
                    2,
                    true,
                    1
            );
        }

        private ConversationOverviewPageDto getConversationsAsFirstUser(int pageNumber) {
            authHelper.setupSecurityContextForFirstUser();
            return conversationService.getConversations(pageNumber);
        }

        private ConversationOverviewPageDto getConversationsAsSecondUser(int pageNumber) {
            authHelper.setupSecurityContextForSecondUser();
            return conversationService.getConversations(pageNumber);
        }

        private Conversation createGroupConversation(User firstParticipant, User secondParticipant, Instant lastActiveAt) {
            Conversation conversation = conversationRepository.save(Conversation.builder()
                    .type(ConversationType.GROUP)
                    .name(ConversationConstants.FIRST_GROUP_CONVERSATION_NAME)
                    .createdAt(TimeConstants.TWO_HOURS_AGO)
                    .lastActiveAt(lastActiveAt)
                    .build());
            addParticipant(conversation, firstParticipant);
            addParticipant(conversation, secondParticipant);
            return conversation;
        }

        private void addParticipant(Conversation conversation, User user) {
            conversationParticipantRepository.save(ConversationParticipant.builder()
                    .conversation(conversation)
                    .user(user)
                    .joinedAt(TimeConstants.TWO_HOURS_AGO)
                    .build());
        }

        private ConversationOverviewDto findOverviewById(ConversationOverviewPageDto pageDto, UUID conversationId) {
            return pageDto.conversations().stream()
                    .filter(conversationOverviewDto -> conversationOverviewDto.id().equals(conversationId))
                    .findFirst()
                    .orElseThrow();
        }

        private void assertPageMetadata(
                ConversationOverviewPageDto response,
                int expectedPageNumber,
                int expectedPageSize,
                long expectedTotalElements,
                int expectedTotalPages,
                boolean expectedLastPage,
                int expectedContentSize
        ) {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.pageNumber()).isEqualTo(expectedPageNumber);
                softly.assertThat(response.pageSize()).isEqualTo(expectedPageSize);
                softly.assertThat(response.totalElements()).isEqualTo(expectedTotalElements);
                softly.assertThat(response.totalPages()).isEqualTo(expectedTotalPages);
                softly.assertThat(response.lastPage()).isEqualTo(expectedLastPage);
                softly.assertThat(response.conversations()).hasSize(expectedContentSize);
            });
        }

        private void assertOverview(
                ConversationOverviewDto overviewDto,
                Conversation conversation,
                String expectedDisplayName
        ) {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(overviewDto.id()).isEqualTo(conversation.getId());
                softly.assertThat(overviewDto.lastActiveAt()).isEqualTo(conversation.getLastActiveAt());
                softly.assertThat(overviewDto.displayName()).isEqualTo(expectedDisplayName);
            });
        }

        private void assertNoConversationDataCreated() {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(conversationRepository.findAll()).isEmpty();
                softly.assertThat(conversationParticipantRepository.findAll()).isEmpty();
                softly.assertThat(directConversationPairRepository.findAll()).isEmpty();
                softly.assertThat(messageRepository.findAll()).isEmpty();
            });
        }
    }

    @Nested
    @DisplayName("Get messages in conversation tests:")
    class GetMessagesInConversationTests {

        @Test
        @DisplayName("When getting newest messages should persist participant read metadata without changing conversation activity")
        public void whenGettingNewestMessagesShouldPersistParticipantReadMetadataWithoutChangingConversationActivity() {
            DirectMessageResponseDto directMessageResponse = sendMessageAsFirstUser(MessageConstants.FIRST_MESSAGE_CONTENT);
            Message savedMessage = messageRepository.findAll().getFirst();

            Conversation conversation = conversationRepository.findById(directMessageResponse.conversationId()).orElseThrow();
            conversation.setLastActiveAt(TimeConstants.ONE_HOUR_AGO);
            conversationRepository.save(conversation);

            ConversationParticipant secondUserParticipant = conversationParticipantRepository
                    .findByConversationIdAndUserId(directMessageResponse.conversationId(), secondUser.getId())
                    .orElseThrow();
            secondUserParticipant.setLastReadAt(null);
            secondUserParticipant.setLastReadMessageId(null);
            conversationParticipantRepository.save(secondUserParticipant);

            SecurityContextHolder.clearContext();

            MessagePageDto response = getMessagesAsSecondUser(directMessageResponse.conversationId(), PaginationConstants.PAGE_ZERO);

            Conversation updatedConversation = conversationRepository.findById(directMessageResponse.conversationId()).orElseThrow();
            ConversationParticipant updatedSecondUserParticipant = conversationParticipantRepository
                    .findByConversationIdAndUserId(directMessageResponse.conversationId(), secondUser.getId())
                    .orElseThrow();
            ConversationParticipant updatedFirstUserParticipant = conversationParticipantRepository
                    .findByConversationIdAndUserId(directMessageResponse.conversationId(), firstUser.getId())
                    .orElseThrow();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.messages()).hasSize(1);
                softly.assertThat(response.messages().getFirst().getContent()).isEqualTo(MessageConstants.FIRST_MESSAGE_CONTENT);
                softly.assertThat(updatedSecondUserParticipant.getLastReadAt()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(updatedSecondUserParticipant.getLastReadMessageId()).isEqualTo(savedMessage.getId());
                softly.assertThat(updatedFirstUserParticipant.getLastReadAt()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(updatedFirstUserParticipant.getLastReadMessageId()).isEqualTo(savedMessage.getId());
                softly.assertThat(updatedConversation.getLastActiveAt()).isEqualTo(TimeConstants.ONE_HOUR_AGO);
            });
        }

        @Test
        @DisplayName("When getting messages should return decrypted messages while database stays encrypted")
        public void whenGettingMessagesShouldReturnDecryptedMessagesWhileDatabaseStaysEncrypted() {
            DirectMessageResponseDto directMessageResponse = sendMessageAsFirstUser(MessageConstants.FIRST_MESSAGE_CONTENT);

            MessagePageDto response = getMessagesAsSecondUser(directMessageResponse.conversationId(), PaginationConstants.PAGE_ZERO);

            Message savedMessage = messageRepository.findAll().getFirst();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.messages()).hasSize(1);
                softly.assertThat(response.messages().getFirst().getContent()).isEqualTo(MessageConstants.FIRST_MESSAGE_CONTENT);
                softly.assertThat(savedMessage.getContent()).isNotEqualTo(MessageConstants.FIRST_MESSAGE_CONTENT);
                softly.assertThat(encryptionUtils.decryptMessage(savedMessage.getContent()))
                        .isEqualTo(MessageConstants.FIRST_MESSAGE_CONTENT);
            });
        }

        @Test
        @DisplayName("When getting messages should return messages by sent date and id descending")
        public void whenGettingMessagesShouldReturnMessagesBySentDateAndIdDescending() {
            DirectMessageResponseDto firstResponse = sendMessageAsFirstUser(MessageConstants.FIRST_MESSAGE_CONTENT);
            sendMessageAsSecondUser(MessageConstants.SECOND_MESSAGE_CONTENT);
            sendMessageAsFirstUser(MessageConstants.THIRD_MESSAGE_CONTENT);

            MessagePageDto response = getMessagesAsFirstUser(firstResponse.conversationId(), PaginationConstants.PAGE_ZERO);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.messages()).hasSize(3);
                softly.assertThat(response.messages().get(0).getContent()).isEqualTo(MessageConstants.THIRD_MESSAGE_CONTENT);
                softly.assertThat(response.messages().get(1).getContent()).isEqualTo(MessageConstants.SECOND_MESSAGE_CONTENT);
                softly.assertThat(response.messages().get(2).getContent()).isEqualTo(MessageConstants.FIRST_MESSAGE_CONTENT);
            });
        }

        @Test
        @DisplayName("When getting messages should return correct first page metadata")
        public void whenGettingMessagesShouldReturnCorrectFirstPageMetadata() {
            DirectMessageResponseDto directMessageResponse = createConversationWithMessages(PaginationConstants.TWENTY_ELEMENTS + 1);

            MessagePageDto response = getMessagesAsSecondUser(directMessageResponse.conversationId(), PaginationConstants.PAGE_ZERO);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.messages()).hasSize(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(response.pageNumber()).isEqualTo(PaginationConstants.PAGE_ZERO);
                softly.assertThat(response.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(response.totalElements()).isEqualTo(PaginationConstants.TWENTY_ELEMENTS + 1);
                softly.assertThat(response.totalPages()).isEqualTo(2);
                softly.assertThat(response.lastPage()).isFalse();
            });
        }

        @Test
        @DisplayName("When getting messages should return correct second page")
        public void whenGettingMessagesShouldReturnCorrectSecondPage() {
            DirectMessageResponseDto directMessageResponse = createConversationWithMessages(PaginationConstants.TWENTY_ELEMENTS + 1);

            MessagePageDto response = getMessagesAsSecondUser(directMessageResponse.conversationId(), PaginationConstants.PAGE_ONE);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.messages()).hasSize(1);
                softly.assertThat(response.messages().getFirst().getContent()).isEqualTo(MessageConstants.FIRST_MESSAGE_CONTENT + " 1");
                softly.assertThat(response.pageNumber()).isEqualTo(PaginationConstants.PAGE_ONE);
                softly.assertThat(response.totalElements()).isEqualTo(PaginationConstants.TWENTY_ELEMENTS + 1);
                softly.assertThat(response.totalPages()).isEqualTo(2);
                softly.assertThat(response.lastPage()).isTrue();
            });
        }

        @Test
        @DisplayName("When getting older messages should not update participant read metadata")
        public void whenGettingOlderMessagesShouldNotUpdateParticipantReadMetadata() {
            DirectMessageResponseDto directMessageResponse = createConversationWithMessages(PaginationConstants.TWENTY_ELEMENTS + 1);
            ConversationParticipant secondUserParticipant = findParticipant(directMessageResponse.conversationId(), secondUser);
            secondUserParticipant.setLastReadAt(TimeConstants.ONE_HOUR_AGO);
            secondUserParticipant.setLastReadMessageId(MessageConstants.THIRD_MESSAGE_ID);
            conversationParticipantRepository.save(secondUserParticipant);

            getMessagesAsSecondUser(directMessageResponse.conversationId(), PaginationConstants.PAGE_ONE);

            ConversationParticipant updatedSecondUserParticipant = findParticipant(directMessageResponse.conversationId(), secondUser);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(updatedSecondUserParticipant.getLastReadAt()).isEqualTo(TimeConstants.ONE_HOUR_AGO);
                softly.assertThat(updatedSecondUserParticipant.getLastReadMessageId()).isEqualTo(MessageConstants.THIRD_MESSAGE_ID);
            });
        }

        @Test
        @DisplayName("When getting empty newest page should update last read time and preserve last read message id")
        public void whenGettingEmptyNewestPageShouldUpdateLastReadTimeAndPreserveLastReadMessageId() {
            Conversation conversation = conversationRepository.save(Conversation.builder()
                    .type(ConversationType.DIRECT)
                    .createdAt(TimeConstants.TWO_HOURS_AGO)
                    .lastActiveAt(TimeConstants.ONE_HOUR_AGO)
                    .build());
            conversationParticipantRepository.save(ConversationParticipant.builder()
                    .conversation(conversation)
                    .user(firstUser)
                    .joinedAt(TimeConstants.TWO_HOURS_AGO)
                    .lastReadAt(TimeConstants.ONE_HOUR_AGO)
                    .lastReadMessageId(MessageConstants.SECOND_MESSAGE_ID)
                    .build());
            conversationParticipantRepository.save(ConversationParticipant.builder()
                    .conversation(conversation)
                    .user(secondUser)
                    .joinedAt(TimeConstants.TWO_HOURS_AGO)
                    .build());

            MessagePageDto response = getMessagesAsFirstUser(conversation.getId(), PaginationConstants.PAGE_ZERO);

            ConversationParticipant updatedFirstUserParticipant = findParticipant(conversation.getId(), firstUser);
            Conversation updatedConversation = conversationRepository.findById(conversation.getId()).orElseThrow();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.messages()).isEmpty();
                softly.assertThat(updatedFirstUserParticipant.getLastReadAt()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(updatedFirstUserParticipant.getLastReadMessageId()).isEqualTo(MessageConstants.SECOND_MESSAGE_ID);
                softly.assertThat(updatedConversation.getLastActiveAt()).isEqualTo(TimeConstants.ONE_HOUR_AGO);
            });
        }

        @Test
        @DisplayName("When getting messages should throw InvalidPageNumberException if page number is negative")
        public void whenGettingMessagesShouldThrowInvalidPageNumberExceptionIfPageNumberIsNegative() {
            DirectMessageResponseDto directMessageResponse = sendMessageAsFirstUser(MessageConstants.FIRST_MESSAGE_CONTENT);

            authHelper.setupSecurityContextForSecondUser();

            assertThatThrownBy(() -> conversationService.getMessagesInConversation(
                    directMessageResponse.conversationId(),
                    PaginationConstants.PAGE_MINUS_ONE))
                    .isInstanceOf(InvalidPageNumberException.class);
        }

        @Test
        @DisplayName("When getting messages should throw ConversationNotFoundException if user is not participant")
        public void whenGettingMessagesShouldThrowConversationNotFoundExceptionIfUserIsNotParticipant() {
            DirectMessageResponseDto directMessageResponse = sendMessageAsFirstUser(MessageConstants.FIRST_MESSAGE_CONTENT);
            setupSecurityContextForUserId(UserConstants.NOT_EXISTING_USER_ID);

            assertThatThrownBy(() -> conversationService.getMessagesInConversation(
                    directMessageResponse.conversationId(),
                    PaginationConstants.PAGE_ZERO))
                    .isInstanceOf(ConversationNotFoundException.class);
        }

        @Test
        @DisplayName("When getting messages should throw ConversationNotFoundException if participant has left conversation")
        public void whenGettingMessagesShouldThrowConversationNotFoundExceptionIfParticipantHasLeftConversation() {
            DirectMessageResponseDto directMessageResponse = sendMessageAsFirstUser(MessageConstants.FIRST_MESSAGE_CONTENT);
            ConversationParticipant secondUserParticipant = findParticipant(directMessageResponse.conversationId(), secondUser);
            secondUserParticipant.setLeftAt(TimeConstants.ONE_HOUR_AGO);
            conversationParticipantRepository.save(secondUserParticipant);

            authHelper.setupSecurityContextForSecondUser();

            assertThatThrownBy(() -> conversationService.getMessagesInConversation(
                    directMessageResponse.conversationId(),
                    PaginationConstants.PAGE_ZERO))
                    .isInstanceOf(ConversationNotFoundException.class);
        }

        private DirectMessageResponseDto createConversationWithMessages(int messageCount) {
            DirectMessageResponseDto directMessageResponse = null;
            for (int messageNumber = 1; messageNumber <= messageCount; messageNumber++) {
                directMessageResponse = sendMessageAsFirstUser(MessageConstants.FIRST_MESSAGE_CONTENT + " " + messageNumber);
            }
            return directMessageResponse;
        }
    }
}
