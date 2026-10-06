package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.exception.ApiErrorCode;
import com.mazurek.eventOrganizer.testData.builders.ConversationParticipantTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.ConversationTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.DeleteCurrentUserDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.DirectConversationPairTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.EventTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.FileTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.MessageTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.NotificationTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.ThreadReplyTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.ThreadTestBuilder;
import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationRequest;
import com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryRepository;
import com.mazurek.eventOrganizer.conversation.Conversation;
import com.mazurek.eventOrganizer.conversation.ConversationRepository;
import com.mazurek.eventOrganizer.conversation.ConversationType;
import com.mazurek.eventOrganizer.conversation.direct.DirectConversationPairRepository;
import com.mazurek.eventOrganizer.conversation.message.Message;
import com.mazurek.eventOrganizer.conversation.message.MessageRepository;
import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipant;
import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipantRepository;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.event.EventRepository;
import com.mazurek.eventOrganizer.file.File;
import com.mazurek.eventOrganizer.file.FileRepository;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.jwt.RefreshTokenRepository;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.notification.repository.NotificationRepository;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestFileContentFactory;
import com.mazurek.eventOrganizer.testData.builders.AuthenticationRequestTestBuilder;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.thread.ThreadRepository;
import com.mazurek.eventOrganizer.threadReply.ThreadReply;
import com.mazurek.eventOrganizer.threadReply.ThreadReplyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.ApiConstants.AUTHORIZATION_HEADER;
import static com.mazurek.eventOrganizer.testData.TestConstants.ApiConstants.CONVERSATION_BY_ID_URL;
import static com.mazurek.eventOrganizer.testData.TestConstants.ApiConstants.CONVERSATION_MESSAGES_URL;
import static com.mazurek.eventOrganizer.testData.TestConstants.ApiConstants.CURRENT_USER_URL;
import static com.mazurek.eventOrganizer.testData.TestConstants.ApiConstants.EVENT_BY_ID_URL;
import static com.mazurek.eventOrganizer.testData.TestConstants.ApiConstants.EVENT_FILE_BY_ID_URL;
import static com.mazurek.eventOrganizer.testData.TestConstants.ApiConstants.EVENT_THREAD_BY_ID_URL;
import static com.mazurek.eventOrganizer.testData.TestConstants.ApiConstants.EVENT_THREAD_REPLIES_URL;
import static com.mazurek.eventOrganizer.testData.TestConstants.AuthConstants.JWT_PREFIX;
import static com.mazurek.eventOrganizer.testData.TestConstants.ConversationConstants.FIRST_GROUP_CONVERSATION_NAME;
import static com.mazurek.eventOrganizer.testData.TestConstants.EventConstants.FIRST_EVENT_ADDRESS;
import static com.mazurek.eventOrganizer.testData.TestConstants.EventConstants.FIRST_EVENT_LONG_DESC;
import static com.mazurek.eventOrganizer.testData.TestConstants.EventConstants.FIRST_EVENT_NAME;
import static com.mazurek.eventOrganizer.testData.TestConstants.EventConstants.FIRST_EVENT_SHORT_DESC;
import static com.mazurek.eventOrganizer.testData.TestConstants.MessageConstants.FIRST_MESSAGE_CONTENT;
import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants.NOW;
import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants.ONE_WEEK_AGO;
import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants.ONE_WEEK_FROM_NOW;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.FIRST_USER_EMAIL;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.FIRST_USER_FULL_NAME;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.FIRST_USER_TIMEZONE;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.SECOND_USER_EMAIL;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.USER_PASSWORD;
import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Account deletion integration tests:")
class AccountDeletionIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AuthenticationService authenticationService;
    @Autowired private AuthHelper authHelper;
    @Autowired private DeletionService deletionService;
    @Autowired private UserRepository userRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private FileRepository fileRepository;
    @Autowired private ThreadRepository threadRepository;
    @Autowired private ThreadReplyRepository threadReplyRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private ConversationParticipantRepository participantRepository;
    @Autowired private DirectConversationPairRepository directConversationPairRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private AuthEmailDeliveryRepository authEmailDeliveryRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private User firstUser;
    private User secondUser;
    private String firstUserJwt;
    private String secondUserJwt;

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();
        firstUser = requirePresent(userRepository.findByIgnoreCaseEmail(FIRST_USER_EMAIL), "Expected user record in setUp");
        secondUser = requirePresent(userRepository.findByIgnoreCaseEmail(SECOND_USER_EMAIL), "Expected user record in setUp");
        firstUserJwt = authenticate(AuthenticationRequestTestBuilder.authenticationRequestForFirstUser().build());
        secondUserJwt = authenticate(AuthenticationRequestTestBuilder.authenticationRequestForSecondUser().build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
    }

    @Test
    @DisplayName("When deleting account should remove active data and preserve anonymized history")
    void whenDeletingAccountShouldRemoveActiveDataAndPreserveAnonymizedHistory() throws Exception {
        DeletionFixture fixture = persistDeletionFixture();

        assertThat(fixture).as("Expected committed deletion fixture").isNotNull();

        List<Long> secondUserRefreshTokenIds = refreshTokenRepository.findAll().stream()
                .filter(token -> token.getUser().getId().equals(secondUser.getId()))
                .map(token -> token.getId()).toList();
        List<UUID> secondUserEmailDeliveryIds = authEmailDeliveryRepository.findAll().stream()
                .filter(delivery -> delivery.getUserId().equals(secondUser.getId()))
                .map(delivery -> delivery.getId()).toList();
        assertThat(secondUserRefreshTokenIds).as("Expected the other user's refresh session before deletion").isNotEmpty();
        assertThat(secondUserEmailDeliveryIds).as("Expected the other user's durable email records before deletion").isNotEmpty();

        mockMvc.perform(delete(CURRENT_USER_URL)
                        .header(AUTHORIZATION_HEADER, firstUserJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeleteCurrentUserDtoTestBuilder().password(USER_PASSWORD).build())))
                .andExpect(status().isNoContent());

        assertPersistenceState(fixture, secondUserRefreshTokenIds, secondUserEmailDeliveryIds);

        assertRetainedHttpContracts(
                fixture.pastEventId(),
                fixture.fileId(),
                fixture.threadId(),
                fixture.directConversationId(),
                fixture.directMessageId(),
                fixture.groupConversationId());
    }

    private DeletionFixture persistDeletionFixture() {
        return new TransactionTemplate(transactionManager).execute(status -> {
            User managedFirstUser = requirePresent(userRepository.findById(firstUser.getId()), "Expected user record in persistDeletionFixture");
            User managedSecondUser = requirePresent(userRepository.findById(secondUser.getId()), "Expected user record in persistDeletionFixture");

            Event pastEvent = persistEvent(managedFirstUser, ONE_WEEK_AGO, "Past event");
            pastEvent.addAttendee(managedSecondUser);
            eventRepository.saveAndFlush(pastEvent);

            Event upcomingOwnedEvent = persistEvent(managedFirstUser, ONE_WEEK_FROM_NOW, "Upcoming owned event");
            Event attendedEvent = persistEvent(managedSecondUser, ONE_WEEK_FROM_NOW, "Attended event");
            attendedEvent.addAttendee(managedFirstUser);
            eventRepository.saveAndFlush(attendedEvent);

            File historicalFile = persistFile(pastEvent, managedFirstUser);
            Thread historicalThread = persistThread(pastEvent, managedFirstUser);
            ThreadReply historicalReply = persistReply(historicalThread, managedFirstUser);
            Conversation directConversation = persistConversation(
                    ConversationType.DIRECT, null, managedFirstUser, managedSecondUser);
            directConversationPairRepository.saveAndFlush(
                    new DirectConversationPairTestBuilder()
                            .conversation(directConversation)
                            .firstUserId(managedFirstUser.getId())
                            .secondUserId(managedSecondUser.getId())
                            .build());
            Message directMessage = messageRepository.saveAndFlush(MessageTestBuilder.firstMessage().id(null)
                    .conversation(directConversation)
                    .sender(managedFirstUser)
                    .senderNameAtCreation(FIRST_USER_FULL_NAME)
                    .sentDate(NOW)
                    .encryptionKeyId("default")
                    .content(FIRST_MESSAGE_CONTENT)
                    .build());
            Conversation groupConversation = persistConversation(
                    ConversationType.GROUP,
                    FIRST_GROUP_CONVERSATION_NAME,
                    managedFirstUser,
                    managedSecondUser);

            Notification deletedUserNotification = persistNotification(managedFirstUser.getId(), pastEvent.getId());
            Notification removedEventNotification = persistNotification(
                    managedSecondUser.getId(), upcomingOwnedEvent.getId());

            return new DeletionFixture(
                    pastEvent.getId(),
                    upcomingOwnedEvent.getId(),
                    attendedEvent.getId(),
                    historicalFile.getId(),
                    historicalThread.getId(),
                    historicalReply.getId(),
                    directConversation.getId(),
                    directMessage.getId(),
                    groupConversation.getId(),
                    deletedUserNotification.getId(),
                    removedEventNotification.getId());
        });
    }

    private void assertPersistenceState(DeletionFixture fixture, List<Long> retainedRefreshTokenIds,
                                        List<UUID> retainedEmailDeliveryIds) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            assertThat(userRepository.existsById(firstUser.getId())).isFalse();
            assertThat(userRepository.existsById(secondUser.getId())).isTrue();
            assertThat(eventRepository.existsById(fixture.upcomingEventId())).isFalse();
            assertThat(notificationRepository.existsById(fixture.removedEventNotificationId())).isFalse();
            assertThat(notificationRepository.existsById(fixture.deletedUserNotificationId())).isFalse();

            Event retainedPastEvent = requirePresent(eventRepository.findById(fixture.pastEventId()), "Expected event record in assertPersistenceState");
            Event retainedAttendedEvent = requirePresent(eventRepository.findById(fixture.attendedEventId()), "Expected event record in assertPersistenceState");
            assertThat(retainedPastEvent.getOwner()).isNull();
            assertThat(retainedAttendedEvent.getAttendeeCount()).isZero();
            assertThat(retainedAttendedEvent.getAttendees()).isEmpty();

            File retainedFile = requirePresent(fileRepository.findById(fixture.fileId()), "Expected file record in assertPersistenceState");
            Thread retainedThread = requirePresent(threadRepository.findById(fixture.threadId()), "Expected thread record in assertPersistenceState");
            ThreadReply retainedReply = requirePresent(threadReplyRepository.findById(fixture.replyId()), "Expected thread reply record in assertPersistenceState");
            assertThat(retainedFile.getOwner()).isNull();
            assertThat(retainedThread.getOwner()).isNull();
            assertThat(retainedReply.getReplier()).isNull();

            Message retainedMessage = requirePresent(messageRepository.findById(fixture.directMessageId()), "Expected message record in assertPersistenceState");
            assertThat(retainedMessage.getSender()).isNull();
            assertThat(retainedMessage.getSenderNameAtCreation()).isEqualTo(FIRST_USER_FULL_NAME);
            assertThat(retainedMessage.getContent()).isEqualTo(FIRST_MESSAGE_CONTENT);
            assertThat(conversationRepository.existsById(fixture.directConversationId())).isTrue();
            assertThat(conversationRepository.existsById(fixture.groupConversationId())).isTrue();
            assertThat(directConversationPairRepository.findAll()).isEmpty();

            assertDeletedParticipantState(fixture.directConversationId(), false);
            assertDeletedParticipantState(fixture.groupConversationId(), true);
            var retainedRefreshTokens = refreshTokenRepository.findAll();
            assertThat(retainedRefreshTokens)
                    .allMatch(token -> token.getUser().getId().equals(secondUser.getId()));
            assertThat(retainedRefreshTokens).extracting(token -> token.getId())
                    .containsExactlyInAnyOrderElementsOf(retainedRefreshTokenIds);
            var retainedEmailDeliveries = authEmailDeliveryRepository.findAll();
            assertThat(retainedEmailDeliveries)
                    .allMatch(delivery -> delivery.getUserId().equals(secondUser.getId()));
            assertThat(retainedEmailDeliveries).extracting(delivery -> delivery.getId())
                    .containsExactlyInAnyOrderElementsOf(retainedEmailDeliveryIds);
        });
    }

    private String authenticate(AuthenticationRequest request) {
        return JWT_PREFIX + authenticationService.authenticate(request, DeviceType.WEB).getAccessToken();
    }

    private Event persistEvent(User owner, Instant startDate, String name) {
        Event event = EventTestBuilder.firstEvent().id(null).city(owner.getHomeCity()).owner(owner)
                .name(name)
                .shortDescription(FIRST_EVENT_SHORT_DESC)
                .longDescription(FIRST_EVENT_LONG_DESC)
                .createDate(NOW)
                .lastUpdate(NOW)
                .eventStartDate(startDate)
                .maxAttendees(100)
                .exactAddress(FIRST_EVENT_ADDRESS)
                .build();
        return eventRepository.saveAndFlush(event);
    }

    private File persistFile(Event event, User owner) {
        File file = FileTestBuilder.jpgFile().id(null).event(null).owner(null)
                .userFileName("historical-file")
                .originalFileName("historical-file.jpg")
                .content(TestFileContentFactory.jpg())
                .contentType("image/jpeg")
                .uploadDateTime(NOW)
                .build();
        event.addFile(file);
        file.setOwner(owner);
        return fileRepository.saveAndFlush(file);
    }

    private Thread persistThread(Event event, User owner) {
        Thread thread = ThreadTestBuilder.firstThread().id(null)
                .event(event)
                .owner(owner)
                .name("Historical thread")
                .content("Historical thread content")
                .createDate(NOW)
                .lastUpdate(NOW)
                .lastActivity(NOW)
                .build();
        return threadRepository.saveAndFlush(thread);
    }

    private ThreadReply persistReply(Thread thread, User replier) {
        return threadReplyRepository.saveAndFlush(ThreadReplyTestBuilder.firstReply().id(null)
                .thread(thread)
                .replier(replier)
                .content("Historical reply")
                .replyDate(NOW)
                .lastUpdate(NOW)
                .build());
    }

    private Conversation persistConversation(ConversationType type, String name, User first, User second) {
        Conversation conversation = new ConversationTestBuilder().id(null)
                .type(type)
                .name(name)
                .createdAt(NOW)
                .lastActiveAt(NOW)
                .buildWithoutParticipants();
        conversation.addParticipant(participant(first, conversation));
        conversation.addParticipant(participant(second, conversation));
        return conversationRepository.saveAndFlush(conversation);
    }

    private ConversationParticipant participant(User user, Conversation conversation) {
        return new ConversationParticipantTestBuilder().id(null)
                .user(user)
                .userNameAtJoin(user.getFullName())
                .conversation(conversation)
                .joinedAt(NOW)
                .lastReadAt(null)
                .lastReadMessageId(null)
                .build();
    }

    private Notification persistNotification(UUID recipientId, UUID eventId) {
        return notificationRepository.saveAndFlush(new NotificationTestBuilder().id(null)
                .recipientId(recipientId)
                .title(FIRST_EVENT_NAME)
                .body("Event notification")
                .resourceType(NotificationResourceType.EVENT)
                .resourceId(eventId)
                .createdAt(NOW)
                .build());
    }

    private void assertDeletedParticipantState(UUID conversationId, boolean expectedToHaveLeft) {
        ConversationParticipant participant = requirePresent(participantRepository.findAll().stream()
                .filter(candidate -> candidate.getConversation().getId().equals(conversationId))
                .filter(candidate -> FIRST_USER_FULL_NAME.equals(candidate.getUserNameAtJoin()))
                .findFirst(), "Expected participant record in assertDeletedParticipantState");

        assertThat(participant.getUser()).isNull();
        if (expectedToHaveLeft) {
            assertThat(participant.getLeftAt()).isEqualTo(NOW);
        } else {
            assertThat(participant.getLeftAt()).isNull();
        }
    }

    private void assertRetainedHttpContracts(
            UUID pastEventId,
            UUID fileId,
            UUID threadId,
            UUID directConversationId,
            Long messageId,
            UUID groupConversationId
    ) throws Exception {
        mockMvc.perform(get(EVENT_BY_ID_URL, pastEventId)
                        .header(AUTHORIZATION_HEADER, secondUserJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner.id").doesNotExist())
                .andExpect(jsonPath("$.owner.firstName").value("Deleted"))
                .andExpect(jsonPath("$.owner.lastName").value("user"));

        mockMvc.perform(get(EVENT_FILE_BY_ID_URL, pastEventId, fileId)
                        .header(AUTHORIZATION_HEADER, secondUserJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner.id").doesNotExist())
                .andExpect(jsonPath("$.owner.firstName").value("Deleted"))
                .andExpect(jsonPath("$.owner.lastName").value("user"));

        mockMvc.perform(get(EVENT_THREAD_BY_ID_URL, pastEventId, threadId)
                        .header(AUTHORIZATION_HEADER, secondUserJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner.id").doesNotExist())
                .andExpect(jsonPath("$.owner.firstName").value("Deleted"))
                .andExpect(jsonPath("$.owner.lastName").value("user"));

        mockMvc.perform(get(EVENT_THREAD_REPLIES_URL, pastEventId, threadId)
                        .header(AUTHORIZATION_HEADER, secondUserJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replies[0].replier.id").doesNotExist())
                .andExpect(jsonPath("$.replies[0].replier.firstName").value("Deleted"))
                .andExpect(jsonPath("$.replies[0].replier.lastName").value("user"));

        mockMvc.perform(get(CONVERSATION_BY_ID_URL, directConversationId)
                        .header(AUTHORIZATION_HEADER, secondUserJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(FIRST_USER_FULL_NAME))
                .andExpect(jsonPath("$.participants[?(@.userId == null)].fullName").value(hasItem(FIRST_USER_FULL_NAME)));

        mockMvc.perform(get(CONVERSATION_MESSAGES_URL, directConversationId)
                        .header(AUTHORIZATION_HEADER, secondUserJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].id").value(messageId))
                .andExpect(jsonPath("$.messages[0].senderId").doesNotExist())
                .andExpect(jsonPath("$.messages[0].senderName").value(FIRST_USER_FULL_NAME));

        mockMvc.perform(get(CONVERSATION_BY_ID_URL, groupConversationId)
                        .header(AUTHORIZATION_HEADER, secondUserJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants.length()").value(1));

        mockMvc.perform(post(CONVERSATION_MESSAGES_URL, directConversationId)
                        .header(AUTHORIZATION_HEADER, secondUserJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Cannot be delivered\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiErrorCode.CONVERSATION_NOT_FOUND));
    }

    private record DeletionFixture(
            UUID pastEventId,
            UUID upcomingEventId,
            UUID attendedEventId,
            UUID fileId,
            UUID threadId,
            UUID replyId,
            UUID directConversationId,
            Long directMessageId,
            UUID groupConversationId,
            UUID deletedUserNotificationId,
            UUID removedEventNotificationId
    ) {
    }
}
