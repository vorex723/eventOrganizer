package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationRequest;
import com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryRepository;
import com.mazurek.eventOrganizer.conversation.Conversation;
import com.mazurek.eventOrganizer.conversation.ConversationRepository;
import com.mazurek.eventOrganizer.conversation.ConversationType;
import com.mazurek.eventOrganizer.conversation.direct.DirectConversationPair;
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
import com.mazurek.eventOrganizer.user.dto.DeleteCurrentUserDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        firstUser = userRepository.findByIgnoreCaseEmail(FIRST_USER_EMAIL).orElseThrow();
        secondUser = userRepository.findByIgnoreCaseEmail(SECOND_USER_EMAIL).orElseThrow();
        firstUserJwt = authenticate(AuthenticationRequestTestBuilder.authenticationRequestForFirstUser().build());
        secondUserJwt = authenticate(AuthenticationRequestTestBuilder.authenticationRequestForSecondUser().build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
    }

    @Test
    @DisplayName("Deleting an account should remove active data and preserve anonymized history")
    void deletingAccountShouldRemoveActiveDataAndPreserveAnonymizedHistory() throws Exception {
        DeletionFixture fixture = persistDeletionFixture();

        mockMvc.perform(delete(CURRENT_USER_URL)
                        .header(AUTHORIZATION_HEADER, firstUserJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeleteCurrentUserDto(USER_PASSWORD))))
                .andExpect(status().isNoContent());

        assertPersistenceState(fixture);

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
            User managedFirstUser = userRepository.findById(firstUser.getId()).orElseThrow();
            User managedSecondUser = userRepository.findById(secondUser.getId()).orElseThrow();

            Event pastEvent = persistEvent(managedFirstUser, ONE_WEEK_AGO, "Past event");
            pastEvent.addAttendingUser(managedSecondUser);
            eventRepository.saveAndFlush(pastEvent);

            Event upcomingOwnedEvent = persistEvent(managedFirstUser, ONE_WEEK_FROM_NOW, "Upcoming owned event");
            Event attendedEvent = persistEvent(managedSecondUser, ONE_WEEK_FROM_NOW, "Attended event");
            attendedEvent.addAttendingUser(managedFirstUser);
            eventRepository.saveAndFlush(attendedEvent);

            File historicalFile = persistFile(pastEvent, managedFirstUser);
            Thread historicalThread = persistThread(pastEvent, managedFirstUser);
            ThreadReply historicalReply = persistReply(historicalThread, managedFirstUser);
            Conversation directConversation = persistConversation(
                    ConversationType.DIRECT, null, managedFirstUser, managedSecondUser);
            directConversationPairRepository.saveAndFlush(
                    DirectConversationPair.of(
                            directConversation, managedFirstUser.getId(), managedSecondUser.getId()));
            Message directMessage = messageRepository.saveAndFlush(Message.builder()
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

    private void assertPersistenceState(DeletionFixture fixture) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            assertThat(userRepository.existsById(firstUser.getId())).isFalse();
            assertThat(userRepository.existsById(secondUser.getId())).isTrue();
            assertThat(eventRepository.existsById(fixture.upcomingEventId())).isFalse();
            assertThat(notificationRepository.existsById(fixture.removedEventNotificationId())).isFalse();
            assertThat(notificationRepository.existsById(fixture.deletedUserNotificationId())).isFalse();

            Event retainedPastEvent = eventRepository.findById(fixture.pastEventId()).orElseThrow();
            Event retainedAttendedEvent = eventRepository.findById(fixture.attendedEventId()).orElseThrow();
            assertThat(retainedPastEvent.getOwner()).isNull();
            assertThat(retainedAttendedEvent.getAttendeeCount()).isZero();
            assertThat(retainedAttendedEvent.getAttendingUsers()).isEmpty();

            File retainedFile = fileRepository.findById(fixture.fileId()).orElseThrow();
            Thread retainedThread = threadRepository.findById(fixture.threadId()).orElseThrow();
            ThreadReply retainedReply = threadReplyRepository.findById(fixture.replyId()).orElseThrow();
            assertThat(retainedFile.getOwner()).isNull();
            assertThat(retainedFile.getOwnerNameAtCreation()).isEqualTo("Deleted user");
            assertThat(retainedThread.getOwner()).isNull();
            assertThat(retainedThread.getOwnerNameAtCreation()).isEqualTo("Deleted user");
            assertThat(retainedReply.getReplier()).isNull();
            assertThat(retainedReply.getReplierNameAtCreation()).isEqualTo("Deleted user");

            Message retainedMessage = messageRepository.findById(fixture.directMessageId()).orElseThrow();
            assertThat(retainedMessage.getSender()).isNull();
            assertThat(retainedMessage.getSenderNameAtCreation()).isEqualTo(FIRST_USER_FULL_NAME);
            assertThat(conversationRepository.existsById(fixture.directConversationId())).isTrue();
            assertThat(conversationRepository.existsById(fixture.groupConversationId())).isTrue();
            assertThat(directConversationPairRepository.findAll()).isEmpty();

            assertDeletedParticipantState(fixture.directConversationId(), false);
            assertDeletedParticipantState(fixture.groupConversationId(), true);
            assertThat(refreshTokenRepository.findAll())
                    .allMatch(token -> token.getUser().getId().equals(secondUser.getId()));
            assertThat(authEmailDeliveryRepository.findAll())
                    .allMatch(delivery -> delivery.getUserId().equals(secondUser.getId()));
        });
    }

    private String authenticate(AuthenticationRequest request) {
        return JWT_PREFIX + authenticationService.authenticate(request, DeviceType.WEB).getAccessToken();
    }

    private Event persistEvent(User owner, Instant startDate, String name) {
        Event event = Event.builder()
                .name(name)
                .shortDescription(FIRST_EVENT_SHORT_DESC)
                .longDescription(FIRST_EVENT_LONG_DESC)
                .createDate(NOW)
                .lastUpdate(NOW)
                .eventStartDate(startDate)
                .maxAttendees(100)
                .timeZoneId(FIRST_USER_TIMEZONE)
                .exactAddress(FIRST_EVENT_ADDRESS)
                .build();
        event.setOwner(owner);
        event.setCity(owner.getHomeCity());
        return eventRepository.saveAndFlush(event);
    }

    private File persistFile(Event event, User owner) {
        File file = File.builder()
                .userFileName("historical-file")
                .originalFileName("historical-file.jpg")
                .content(TestFileContentFactory.jpg())
                .contentType("image/jpeg")
                .uploadDateTime(NOW)
                .ownerNameAtCreation(owner.getFullName())
                .build();
        file.setEvent(event);
        file.setOwner(owner);
        return fileRepository.saveAndFlush(file);
    }

    private Thread persistThread(Event event, User owner) {
        Thread thread = Thread.builder()
                .event(event)
                .owner(owner)
                .ownerNameAtCreation(owner.getFullName())
                .name("Historical thread")
                .content("Historical thread content")
                .createDate(NOW)
                .lastUpdate(NOW)
                .lastActivity(NOW)
                .build();
        return threadRepository.saveAndFlush(thread);
    }

    private ThreadReply persistReply(Thread thread, User replier) {
        return threadReplyRepository.saveAndFlush(ThreadReply.builder()
                .thread(thread)
                .replier(replier)
                .replierNameAtCreation(replier.getFullName())
                .content("Historical reply")
                .replyDate(NOW)
                .lastUpdate(NOW)
                .build());
    }

    private Conversation persistConversation(ConversationType type, String name, User first, User second) {
        Conversation conversation = Conversation.builder()
                .type(type)
                .name(name)
                .createdAt(NOW)
                .lastActiveAt(NOW)
                .build();
        conversation.addParticipant(participant(first, conversation));
        conversation.addParticipant(participant(second, conversation));
        return conversationRepository.saveAndFlush(conversation);
    }

    private ConversationParticipant participant(User user, Conversation conversation) {
        return ConversationParticipant.builder()
                .user(user)
                .userNameAtJoin(user.getFullName())
                .conversation(conversation)
                .joinedAt(NOW)
                .build();
    }

    private Notification persistNotification(UUID recipientId, UUID eventId) {
        return notificationRepository.saveAndFlush(Notification.builder()
                .recipientId(recipientId)
                .title(FIRST_EVENT_NAME)
                .body("Event notification")
                .resourceType(NotificationResourceType.EVENT)
                .resourceId(eventId)
                .createdAt(NOW)
                .build());
    }

    private void assertDeletedParticipantState(UUID conversationId, boolean expectedToHaveLeft) {
        ConversationParticipant participant = participantRepository.findAll().stream()
                .filter(candidate -> candidate.getConversation().getId().equals(conversationId))
                .filter(candidate -> FIRST_USER_FULL_NAME.equals(candidate.getUserNameAtJoin()))
                .findFirst()
                .orElseThrow();

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
                .andExpect(status().isNotFound());
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
