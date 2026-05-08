package com.mazurek.eventOrganizer.conversation;

import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.config.properties.PaginationProperties;
import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipant;
import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipantRepository;
import com.mazurek.eventOrganizer.conversation.direct.DirectConversationPair;
import com.mazurek.eventOrganizer.conversation.direct.DirectConversationPairRepository;
import com.mazurek.eventOrganizer.conversation.dto.DirectMessageResponseDto;
import com.mazurek.eventOrganizer.conversation.dto.MessagePageDto;
import com.mazurek.eventOrganizer.conversation.dto.SendDirectMessageDto;
import com.mazurek.eventOrganizer.conversation.message.Message;
import com.mazurek.eventOrganizer.conversation.message.MessageRepository;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import com.mazurek.eventOrganizer.exception.conversation.ConversationNotFoundException;
import com.mazurek.eventOrganizer.exception.conversation.ConversationParticipantNotFound;
import com.mazurek.eventOrganizer.exception.conversation.MessagingYourselfException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.testData.builders.ConversationTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.MessageTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.SendDirectMessageDtoTestBuilder;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import com.mazurek.eventOrganizer.utils.EncryptionUtils;
import com.mazurek.eventOrganizer.testData.TestConstants.*;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Clock;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.PaginationConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Profile("test")
@DisplayName("ConversationServiceImpl unit tests:")
public class ConversationServiceImplUnitTest {

    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ConversationCreationService conversationCreationService;
    @Mock
    private DirectConversationPairRepository directConversationPairRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EncryptionUtils encryptionUtils;
    @Mock
    private PaginationProperties paginationProperties;
    @Mock
    private ConversationParticipantRepository participantRepository;
    @Mock
    private Clock clock;

    @InjectMocks
    private ConversationServiceImpl conversationService;

    private User firstUser;
    private User secondUser;
    private Optional<User> secondUserOptional;


    @BeforeEach
    public void setUp() {
        lenient().when(clock.instant()).thenReturn(TimeConstants.NOW);
        lenient().when(paginationProperties.getDefaultPageSize()).thenReturn(PaginationConstants.DEFAULT_PAGE_SIZE);

        firstUser = UserTestBuilder.firstUser().build();
        secondUser = UserTestBuilder.secondUser().build();
        secondUserOptional = Optional.of(secondUser);
    }

    @Nested
    @DisplayName("Send direct message tests:")
    class SendDirectMessageTests {

        private SendDirectMessageDto sendDirectMessageDto;
        private Conversation conversation;
        private Optional<Conversation> conversationOptional;
        private ConversationParticipant senderParticipant;
        private ConversationParticipant recipientParticipant;
        private String encryptedFirstMessageContent;

        @BeforeEach
        public void setUp() {
            sendDirectMessageDto = SendDirectMessageDtoTestBuilder.firstDirectMessage().build();
            encryptedFirstMessageContent = MessageConstants.ENCRYPTED_FIRST_MESSAGE_CONTENT;

            conversation = ConversationTestBuilder.firstConversation()
                    .createdAt(TimeConstants.TWO_HOURS_AGO)
                    .lastActiveAt(TimeConstants.ONE_HOUR_AGO)
                    .build();
            conversationOptional = Optional.of(conversation);

            senderParticipant = findParticipant(firstUser);
            senderParticipant.setLastReadAt(TimeConstants.TWO_HOURS_AGO);
            senderParticipant.setLastReadMessageId(null);

            recipientParticipant = findParticipant(secondUser);
            recipientParticipant.setLastReadAt(null);
            recipientParticipant.setLastReadMessageId(null);
        }

        private ConversationParticipant findParticipant(User user) {
            return conversation.getParticipants().stream()
                    .filter(participant -> participant.getUser().getId().equals(user.getId()))
                    .findFirst()
                    .orElseThrow();
        }

        private void setupSuccessfulMocks() {
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(userRepository.findById(UserConstants.SECOND_USER_ID)).thenReturn(secondUserOptional);
            when(directConversationPairRepository.findConversationByUsers(any(), any())).thenReturn(conversationOptional);
            setupMessageSaveMocks();
        }

        private void setupMessageSaveMocks() {
            when(encryptionUtils.encryptMessage(MessageConstants.FIRST_MESSAGE_CONTENT)).thenReturn(encryptedFirstMessageContent);
            when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
                Message message = invocation.getArgument(0);
                message.setId(MessageConstants.FIRST_MESSAGE_ID);
                return message;
            });
            when(encryptionUtils.decryptMessage(encryptedFirstMessageContent)).thenReturn(MessageConstants.FIRST_MESSAGE_CONTENT);
        }

        @Test
        @DisplayName("When sending direct message should throw MessagingYourselfException if user tries to message himself")
        public void whenSendingDirectMessageShouldThrowMessagingYourselfExceptionIfUserTriesToMessageHimself(){
            sendDirectMessageDto.setRecipientId(firstUser.getId());

            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            assertThatThrownBy(() -> conversationService.sendDirectMessage(sendDirectMessageDto)).isInstanceOf(MessagingYourselfException.class);

            verify(userRepository, never()).findById(any());
            verify(directConversationPairRepository, never()).findConversationByUsers(any(), any());
            verify(conversationCreationService, never()).createDirectConversation(any(), any(), any());
            verify(conversationRepository, never()).save(any());
            verify(participantRepository, never()).save(any());
            verify(messageRepository, never()).save(any());
            verify(encryptionUtils, never()).encryptMessage(any());
        }

        @Test
        @DisplayName("When sending direct message should throw UserNotFoundException if user with given id does not exist")
        public void whenSendingDirectMessageShouldThrowUserNotFoundIfUserWithGivenIdDoesNotExist(){
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(userRepository.findById(UserConstants.SECOND_USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> conversationService.sendDirectMessage(sendDirectMessageDto)).isInstanceOf(UserNotFoundException.class);

            verify(directConversationPairRepository, never()).findConversationByUsers(any(), any());
            verify(conversationCreationService, never()).createDirectConversation(any(), any(), any());
            verify(conversationRepository, never()).save(any());
            verify(participantRepository, never()).save(any());
            verify(messageRepository, never()).save(any());
            verify(encryptionUtils, never()).encryptMessage(any());
        }

        @Test
        @DisplayName("When sending direct message should load conversation by canonical pair ids")
        public void whenSendingDirectMessageShouldLoadConversationByCanonicalPairIds() {
            setupSuccessfulMocks();

            conversationService.sendDirectMessage(sendDirectMessageDto);

            ArgumentCaptor<UUID> firstUserIdCaptor = ArgumentCaptor.forClass(UUID.class);
            ArgumentCaptor<UUID> secondUserIdCaptor = ArgumentCaptor.forClass(UUID.class);

            verify(directConversationPairRepository, times(1))
                    .findConversationByUsers(firstUserIdCaptor.capture(), secondUserIdCaptor.capture());

            UUID firstUserId = firstUserIdCaptor.getValue();
            UUID secondUserId = secondUserIdCaptor.getValue();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(firstUserId).isEqualTo(UserConstants.FIRST_USER_ID);
                softly.assertThat(secondUserId).isEqualTo(UserConstants.SECOND_USER_ID);
            });
        }

        @Test
        @DisplayName("When sending direct message should append message to existing conversation")
        public void whenSendingDirectMessageShouldAppendMessageToExistingConversation() {
            setupSuccessfulMocks();

            DirectMessageResponseDto response = conversationService.sendDirectMessage(sendDirectMessageDto);

            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(messageRepository, times(1)).save(messageCaptor.capture());

            Message savedMessage = messageCaptor.getValue();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(savedMessage.getConversation()).isSameAs(conversation);
                softly.assertThat(savedMessage.getSender()).isSameAs(firstUser);
                softly.assertThat(savedMessage.getSentDate()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(savedMessage.getContent()).isEqualTo(encryptedFirstMessageContent);
            });

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(conversation.getLastActiveAt()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(senderParticipant.getLastReadAt()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(senderParticipant.getLastReadMessageId()).isEqualTo(MessageConstants.FIRST_MESSAGE_ID);
                softly.assertThat(recipientParticipant.getLastReadAt()).isNull();
                softly.assertThat(recipientParticipant.getLastReadMessageId()).isNull();
            });

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.conversationId()).isEqualTo(ConversationConstants.FIRST_CONVERSATION_ID);
                softly.assertThat(response.conversationCreated()).isFalse();
                softly.assertThat(response.message().getContent()).isEqualTo(MessageConstants.FIRST_MESSAGE_CONTENT);
                softly.assertThat(response.message().getSentDate()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(response.message().getSender().getId()).isEqualTo(UserConstants.FIRST_USER_ID);
            });

            verify(conversationRepository, never()).save(any(Conversation.class));
            verify(conversationCreationService, never()).createDirectConversation(any(), any(), any());
            verify(participantRepository, never()).save(any(ConversationParticipant.class));
            verify(encryptionUtils, times(1)).decryptMessage(MessageConstants.ENCRYPTED_FIRST_MESSAGE_CONTENT);
        }

        @Test
        @DisplayName("When sending direct message should create and reload conversation if conversation does not exist")
        public void whenSendingDirectMessageShouldCreateAndReloadConversationIfConversationDoesNotExist() {
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(userRepository.findById(UserConstants.SECOND_USER_ID)).thenReturn(secondUserOptional);
            when(directConversationPairRepository.findConversationByUsers(any(), any()))
                    .thenReturn(Optional.empty(), conversationOptional);
            when(conversationCreationService.createDirectConversation(firstUser, secondUser, TimeConstants.NOW))
                    .thenReturn(ConversationConstants.FIRST_CONVERSATION_ID);
            setupMessageSaveMocks();

            DirectMessageResponseDto response = conversationService.sendDirectMessage(sendDirectMessageDto);

            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            ArgumentCaptor<UUID> firstUserIdCaptor = ArgumentCaptor.forClass(UUID.class);
            ArgumentCaptor<UUID> secondUserIdCaptor = ArgumentCaptor.forClass(UUID.class);

            verify(conversationCreationService, times(1)).createDirectConversation(firstUser, secondUser, TimeConstants.NOW);
            verify(directConversationPairRepository, times(2))
                    .findConversationByUsers(firstUserIdCaptor.capture(), secondUserIdCaptor.capture());
            verify(messageRepository, times(1)).save(messageCaptor.capture());

            Message savedMessage = messageCaptor.getValue();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(firstUserIdCaptor.getAllValues()).containsExactly(UserConstants.FIRST_USER_ID, UserConstants.FIRST_USER_ID);
                softly.assertThat(secondUserIdCaptor.getAllValues()).containsExactly(UserConstants.SECOND_USER_ID, UserConstants.SECOND_USER_ID);
            });

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(conversation.getLastActiveAt()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(senderParticipant.getLastReadAt()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(senderParticipant.getLastReadMessageId()).isEqualTo(MessageConstants.FIRST_MESSAGE_ID);
                softly.assertThat(recipientParticipant.getLastReadAt()).isNull();
                softly.assertThat(recipientParticipant.getLastReadMessageId()).isNull();
            });

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(savedMessage.getConversation()).isSameAs(conversation);
                softly.assertThat(savedMessage.getSender()).isSameAs(firstUser);
                softly.assertThat(savedMessage.getSentDate()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(savedMessage.getContent()).isEqualTo(encryptedFirstMessageContent);
            });

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.conversationId()).isEqualTo(ConversationConstants.FIRST_CONVERSATION_ID);
                softly.assertThat(response.conversationCreated()).isTrue();
                softly.assertThat(response.message().getContent()).isEqualTo(MessageConstants.FIRST_MESSAGE_CONTENT);
                softly.assertThat(response.message().getSentDate()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(response.message().getSender().getId()).isEqualTo(UserConstants.FIRST_USER_ID);
            });

            verify(conversationRepository, never()).save(any(Conversation.class));
            verify(participantRepository, never()).save(any(ConversationParticipant.class));
            verify(directConversationPairRepository, never()).save(any(DirectConversationPair.class));
            verify(encryptionUtils, times(1)).decryptMessage(MessageConstants.ENCRYPTED_FIRST_MESSAGE_CONTENT);
        }

        @Test
        @DisplayName("When direct conversation creation loses race should reload existing conversation and send message")
        public void whenDirectConversationCreationLosesRaceShouldReloadExistingConversationAndSendMessage() {
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(userRepository.findById(UserConstants.SECOND_USER_ID)).thenReturn(secondUserOptional);
            when(directConversationPairRepository.findConversationByUsers(any(), any()))
                    .thenReturn(Optional.empty(), conversationOptional);
            when(conversationCreationService.createDirectConversation(firstUser, secondUser, TimeConstants.NOW))
                    .thenThrow(new DataIntegrityViolationException("duplicate direct conversation pair"));
            setupMessageSaveMocks();

            DirectMessageResponseDto response = conversationService.sendDirectMessage(sendDirectMessageDto);

            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(messageRepository, times(1)).save(messageCaptor.capture());

            Message savedMessage = messageCaptor.getValue();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(savedMessage.getConversation()).isSameAs(conversation);
                softly.assertThat(savedMessage.getSender()).isSameAs(firstUser);
                softly.assertThat(savedMessage.getSentDate()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(savedMessage.getContent()).isEqualTo(encryptedFirstMessageContent);
            });

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(conversation.getLastActiveAt()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(senderParticipant.getLastReadAt()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(senderParticipant.getLastReadMessageId()).isEqualTo(MessageConstants.FIRST_MESSAGE_ID);
                softly.assertThat(recipientParticipant.getLastReadAt()).isNull();
                softly.assertThat(recipientParticipant.getLastReadMessageId()).isNull();
            });

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.conversationId()).isEqualTo(ConversationConstants.FIRST_CONVERSATION_ID);
                softly.assertThat(response.conversationCreated()).isFalse();
                softly.assertThat(response.message().getContent()).isEqualTo(MessageConstants.FIRST_MESSAGE_CONTENT);
                softly.assertThat(response.message().getSentDate()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(response.message().getSender().getId()).isEqualTo(UserConstants.FIRST_USER_ID);
            });

            verify(conversationCreationService, times(1)).createDirectConversation(firstUser, secondUser, TimeConstants.NOW);
            verify(directConversationPairRepository, times(2)).findConversationByUsers(UserConstants.FIRST_USER_ID, UserConstants.SECOND_USER_ID);
            verify(conversationRepository, never()).save(any(Conversation.class));
            verify(participantRepository, never()).save(any(ConversationParticipant.class));
            verify(directConversationPairRepository, never()).save(any(DirectConversationPair.class));
            verify(encryptionUtils, times(1)).decryptMessage(MessageConstants.ENCRYPTED_FIRST_MESSAGE_CONTENT);
        }

        @Test
        @DisplayName("When direct conversation creation loses race and reload fails should throw IllegalStateException")
        public void whenDirectConversationCreationLosesRaceAndReloadFailsShouldThrowNoSuchElementException() {
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(userRepository.findById(UserConstants.SECOND_USER_ID)).thenReturn(secondUserOptional);
            when(directConversationPairRepository.findConversationByUsers(any(), any())).thenReturn(Optional.empty());
            when(conversationCreationService.createDirectConversation(firstUser, secondUser, TimeConstants.NOW))
                    .thenThrow(new DataIntegrityViolationException("duplicate direct conversation pair"));

            assertThatThrownBy(() -> conversationService.sendDirectMessage(sendDirectMessageDto))
                    .isInstanceOf(IllegalStateException.class);

            verify(conversationCreationService, times(1)).createDirectConversation(firstUser, secondUser, TimeConstants.NOW);
            verify(directConversationPairRepository, times(2)).findConversationByUsers(UserConstants.FIRST_USER_ID, UserConstants.SECOND_USER_ID);
            verify(messageRepository, never()).save(any(Message.class));
            verify(encryptionUtils, never()).encryptMessage(any());
        }

    }

    @Nested
    @DisplayName("Get messages in conversation tests:")
    class GetMessagesInConversationTests {

        private UUID conversationId;
        private int pageNumber;
        private Conversation conversation;
        private ConversationParticipant firstUserParticipant;
        private Message firstMessage;
        private Message secondMessage;
        private Page<Message> messagePage;

        @BeforeEach
        public void setUp() {
            conversationId = ConversationConstants.FIRST_CONVERSATION_ID;
            pageNumber = 0;
            conversation = ConversationTestBuilder.firstConversation()
                    .createdAt(TimeConstants.TWO_HOURS_AGO)
                    .lastActiveAt(TimeConstants.ONE_HOUR_AGO)
                    .build();
            firstUserParticipant = findParticipant(firstUser);
            firstUserParticipant.setLastReadAt(TimeConstants.TWO_HOURS_AGO);
            firstUserParticipant.setLastReadMessageId(null);
            firstMessage = MessageTestBuilder.firstMessage()
                    .conversation(conversation)
                    .content(MessageConstants.ENCRYPTED_FIRST_MESSAGE_CONTENT)
                    .build();
            secondMessage = MessageTestBuilder.inverseMessage()
                    .conversation(conversation)
                    .content(MessageConstants.ENCRYPTED_SECOND_MESSAGE_CONTENT)
                    .build();
            messagePage = createMessagePage(pageNumber, List.of(firstMessage, secondMessage), 2);
        }

        private ConversationParticipant findParticipant(User user) {
            return conversation.getParticipants().stream()
                    .filter(participant -> participant.getUser().getId().equals(user.getId()))
                    .findFirst()
                    .orElseThrow();
        }

        @Test
        @DisplayName("When getting messages should throw InvalidPageNumberException if page number is negative")
        public void whenGettingMessagesShouldThrowInvalidPageNumberExceptionIfPageNumberIsNegative() {
            assertThatThrownBy(() -> conversationService.getMessagesInConversation(conversationId, -1))
                    .isInstanceOf(InvalidPageNumberException.class);

            verify(authenticationService, never()).getCurrentUserId();
            verify(conversationRepository, never()).existsByIdAndParticipant(any(), any());
            verify(paginationProperties, never()).getDefaultPageSize();
            verify(messageRepository, never()).findByConversationId(any(), any());
            verify(encryptionUtils, never()).decryptMessage(any());
        }

        @Test
        @DisplayName("When getting messages should throw ConversationNotFoundException if user is not active participant")
        public void whenGettingMessagesShouldThrowConversationNotFoundExceptionIfUserIsNotActiveParticipant() {
            when(authenticationService.getCurrentUserId()).thenReturn(firstUser.getId());
            when(conversationRepository.existsByIdAndParticipant(conversationId, firstUser.getId())).thenReturn(false);

            assertThatThrownBy(() -> conversationService.getMessagesInConversation(conversationId, pageNumber))
                    .isInstanceOf(ConversationNotFoundException.class);

            verify(conversationRepository, times(1)).existsByIdAndParticipant(conversationId, firstUser.getId());
            verify(paginationProperties, never()).getDefaultPageSize();
            verify(messageRepository, never()).findByConversationId(any(), any());
            verify(encryptionUtils, never()).decryptMessage(any());
        }

        @Test
        @DisplayName("When getting messages should check conversation access for current user")
        public void whenGettingMessagesShouldCheckConversationAccessForCurrentUser() {
            Page<Message> emptyPage = createMessagePage(pageNumber, List.of(), 0);
            setupAccessibleConversation();
            setupParticipantLookup();
            when(messageRepository.findByConversationId(eq(conversationId), any(Pageable.class))).thenReturn(emptyPage);

            conversationService.getMessagesInConversation(conversationId, pageNumber);

            verify(authenticationService, times(1)).getCurrentUserId();
            verify(conversationRepository, times(1)).existsByIdAndParticipant(conversationId, firstUser.getId());
        }

        @Test
        @DisplayName("When getting messages should query messages with requested page and stable descending sort")
        public void whenGettingMessagesShouldQueryMessagesWithRequestedPageAndStableDescendingSort() {
            int requestedPageNumber = 2;
            Page<Message> emptyPage = createMessagePage(requestedPageNumber, List.of(), 0);
            setupAccessibleConversation();
            setupParticipantLookup();
            when(messageRepository.findByConversationId(eq(conversationId), any(Pageable.class))).thenReturn(emptyPage);

            conversationService.getMessagesInConversation(conversationId, requestedPageNumber);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(messageRepository, times(1)).findByConversationId(eq(conversationId), pageableCaptor.capture());

            Pageable capturedPageable = pageableCaptor.getValue();
            Sort.Order sentDateOrder = capturedPageable.getSort().getOrderFor("sentDate");
            Sort.Order idOrder = capturedPageable.getSort().getOrderFor("id");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(capturedPageable.getPageNumber()).isEqualTo(requestedPageNumber);
                softly.assertThat(capturedPageable.getPageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(sentDateOrder).isNotNull();
                softly.assertThat(sentDateOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
                softly.assertThat(idOrder).isNotNull();
                softly.assertThat(idOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
            });
        }

        @Test
        @DisplayName("When getting messages should throw ConversationParticipantNotFound if participant metadata can not be loaded")
        public void whenGettingMessagesShouldThrowConversationParticipantNotFoundIfParticipantMetadataCanNotBeLoaded() {
            setupAccessibleConversation();
            when(messageRepository.findByConversationId(eq(conversationId), any(Pageable.class))).thenReturn(messagePage);
            when(participantRepository.findByConversationIdAndUserId(conversationId, firstUser.getId()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> conversationService.getMessagesInConversation(conversationId, pageNumber))
                    .isInstanceOf(ConversationParticipantNotFound.class);

            verify(encryptionUtils, never()).decryptMessage(any());
            verify(messageRepository, never()).save(any(Message.class));
            verify(conversationRepository, never()).save(any(Conversation.class));
            verify(participantRepository, never()).save(any(ConversationParticipant.class));
        }

        @Test
        @DisplayName("When getting newest messages should update participant read metadata")
        public void whenGettingNewestMessagesShouldUpdateParticipantReadMetadata() {
            setupSuccessfulMocksWithMessages();

            conversationService.getMessagesInConversation(conversationId, pageNumber);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(firstUserParticipant.getLastReadAt()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(firstUserParticipant.getLastReadMessageId()).isEqualTo(firstMessage.getId());
                softly.assertThat(conversation.getLastActiveAt()).isEqualTo(TimeConstants.ONE_HOUR_AGO);
            });
        }

        @Test
        @DisplayName("When getting messages from later page should not overwrite last read message id")
        public void whenGettingMessagesFromLaterPageShouldNotOverwriteLastReadMessageId() {
            int laterPageNumber = 1;
            Long existingLastReadMessageId = MessageConstants.THIRD_MESSAGE_ID;
            Page<Message> laterMessagePage = createMessagePage(laterPageNumber, List.of(firstMessage, secondMessage), 22);
            firstUserParticipant.setLastReadMessageId(existingLastReadMessageId);
            setupAccessibleConversation();
            setupParticipantLookup();
            when(messageRepository.findByConversationId(eq(conversationId), any(Pageable.class))).thenReturn(laterMessagePage);
            when(encryptionUtils.decryptMessage(MessageConstants.ENCRYPTED_FIRST_MESSAGE_CONTENT))
                    .thenReturn(MessageConstants.FIRST_MESSAGE_CONTENT);
            when(encryptionUtils.decryptMessage(MessageConstants.ENCRYPTED_SECOND_MESSAGE_CONTENT))
                    .thenReturn(MessageConstants.SECOND_MESSAGE_CONTENT);

            MessagePageDto response = conversationService.getMessagesInConversation(conversationId, laterPageNumber);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.pageNumber()).isEqualTo(laterPageNumber);
                softly.assertThat(firstUserParticipant.getLastReadAt()).isEqualTo(TimeConstants.TWO_HOURS_AGO);
                softly.assertThat(firstUserParticipant.getLastReadMessageId()).isEqualTo(existingLastReadMessageId);
                softly.assertThat(conversation.getLastActiveAt()).isEqualTo(TimeConstants.ONE_HOUR_AGO);
            });
        }

        @Test
        @DisplayName("When getting messages should return decrypted message page dto")
        public void whenGettingMessagesShouldReturnDecryptedMessagePageDto() {
            setupSuccessfulMocksWithMessages();

            MessagePageDto response = conversationService.getMessagesInConversation(conversationId, pageNumber);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.messages()).hasSize(2);
                softly.assertThat(response.messages().getFirst().getContent()).isEqualTo(MessageConstants.FIRST_MESSAGE_CONTENT);
                softly.assertThat(response.messages().getFirst().getSender().getId()).isEqualTo(firstUser.getId());
                softly.assertThat(response.messages().get(1).getContent()).isEqualTo(MessageConstants.SECOND_MESSAGE_CONTENT);
                softly.assertThat(response.messages().get(1).getSender().getId()).isEqualTo(secondUser.getId());
            });

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.pageNumber()).isEqualTo(pageNumber);
                softly.assertThat(response.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(response.totalElements()).isEqualTo(2);
                softly.assertThat(response.totalPages()).isEqualTo(1);
                softly.assertThat(response.lastPage()).isTrue();
            });
        }

        @Test
        @DisplayName("When getting messages should decrypt every message without explicit repository saves")
        public void whenGettingMessagesShouldDecryptEveryMessageWithoutExplicitRepositorySaves() {
            setupSuccessfulMocksWithMessages();

            conversationService.getMessagesInConversation(conversationId, pageNumber);

            verify(encryptionUtils, times(1)).decryptMessage(MessageConstants.ENCRYPTED_FIRST_MESSAGE_CONTENT);
            verify(encryptionUtils, times(1)).decryptMessage(MessageConstants.ENCRYPTED_SECOND_MESSAGE_CONTENT);
            verify(encryptionUtils, never()).encryptMessage(any());
            verify(messageRepository, never()).save(any(Message.class));
            verify(conversationRepository, never()).save(any(Conversation.class));
            verify(participantRepository, never()).save(any(ConversationParticipant.class));
        }

        @Test
        @DisplayName("When getting messages should return empty page without decrypting if conversation has no messages")
        public void whenGettingMessagesShouldReturnEmptyPageWithoutDecryptingIfConversationHasNoMessages() {
            Page<Message> emptyPage = createMessagePage(pageNumber, List.of(), 0);
            firstUserParticipant.setLastReadMessageId(MessageConstants.SECOND_MESSAGE_ID);
            setupAccessibleConversation();
            setupParticipantLookup();
            when(messageRepository.findByConversationId(eq(conversationId), any(Pageable.class))).thenReturn(emptyPage);

            MessagePageDto response = conversationService.getMessagesInConversation(conversationId, pageNumber);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.messages()).isEmpty();
                softly.assertThat(response.pageNumber()).isEqualTo(pageNumber);
                softly.assertThat(response.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(response.totalElements()).isZero();
                softly.assertThat(response.totalPages()).isZero();
                softly.assertThat(response.lastPage()).isTrue();
            });

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(firstUserParticipant.getLastReadAt()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(firstUserParticipant.getLastReadMessageId()).isEqualTo(MessageConstants.SECOND_MESSAGE_ID);
                softly.assertThat(conversation.getLastActiveAt()).isEqualTo(TimeConstants.ONE_HOUR_AGO);
            });

            verify(encryptionUtils, never()).decryptMessage(any());
        }

        private void setupSuccessfulMocksWithMessages() {
            setupAccessibleConversation();
            setupParticipantLookup();
            when(messageRepository.findByConversationId(eq(conversationId), any(Pageable.class))).thenReturn(messagePage);
            when(encryptionUtils.decryptMessage(MessageConstants.ENCRYPTED_FIRST_MESSAGE_CONTENT))
                    .thenReturn(MessageConstants.FIRST_MESSAGE_CONTENT);
            when(encryptionUtils.decryptMessage(MessageConstants.ENCRYPTED_SECOND_MESSAGE_CONTENT))
                    .thenReturn(MessageConstants.SECOND_MESSAGE_CONTENT);
        }

        private void setupAccessibleConversation() {
            when(authenticationService.getCurrentUserId()).thenReturn(firstUser.getId());
            when(conversationRepository.existsByIdAndParticipant(conversationId, firstUser.getId())).thenReturn(true);
        }

        private void setupParticipantLookup() {
            when(participantRepository.findByConversationIdAndUserId(conversationId, firstUser.getId()))
                    .thenReturn(Optional.of(firstUserParticipant));
        }

        private Page<Message> createMessagePage(int pageNumber, List<Message> messages, long totalElements) {
            return new PageImpl<>(
                    messages,
                    PageRequest.of(pageNumber, PaginationConstants.DEFAULT_PAGE_SIZE),
                    totalElements
            );
        }
    }

}
