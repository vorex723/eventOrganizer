package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.notification.repository.NotificationRepository;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.ConversationConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.EventConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.FileConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.NotificationTemplateConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.ThreadConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NotificationCommandService integration tests:")
class NotificationCommandServiceImplIntegrationTest {

    @Autowired
    private NotificationCommandService notificationCommandService;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private DeletionService deletionService;

    private UUID firstUserId;
    private UUID secondUserId;

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();

        firstUserId = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL)
                .orElseThrow(UserNotFoundException::new)
                .getId();
        secondUserId = userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL)
                .orElseThrow(UserNotFoundException::new)
                .getId();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
    }

    @Test
    @DisplayName("When notifying private message should persist direct notification")
    void whenNotifyingPrivateMessageShouldPersistDirectNotification() {
        notificationCommandService.notifyPrivateMessage(
                ConversationConstants.FIRST_CONVERSATION_ID,
                secondUserId,
                UserConstants.FIRST_USER_FULL_NAME
        );

        Notification notification = getOnlyPersistedNotification();

        assertNotification(
                notification,
                secondUserId,
                NotificationTemplateConstants.PRIVATE_MESSAGE_TITLE,
                NotificationTemplateConstants.PRIVATE_MESSAGE_BODY,
                NotificationResourceType.CONVERSATION,
                ConversationConstants.FIRST_CONVERSATION_ID,
                null,
                null
        );
    }

    @Test
    @DisplayName("When notifying thread reply should persist nested notification")
    void whenNotifyingThreadReplyShouldPersistNestedNotification() {
        notificationCommandService.notifyThreadReply(
                EventConstants.FIRST_EVENT_ID,
                ThreadConstants.FIRST_THREAD_ID,
                secondUserId,
                UserConstants.FIRST_USER_FULL_NAME
        );

        Notification notification = getOnlyPersistedNotification();

        assertNotification(
                notification,
                secondUserId,
                NotificationTemplateConstants.THREAD_REPLY_TITLE,
                NotificationTemplateConstants.THREAD_REPLY_BODY,
                NotificationResourceType.THREAD,
                ThreadConstants.FIRST_THREAD_ID,
                NotificationResourceType.EVENT,
                EventConstants.FIRST_EVENT_ID
        );
    }

    @Test
    @DisplayName("When notifying event update should persist one direct notification per recipient")
    void whenNotifyingEventUpdateShouldPersistOneDirectNotificationPerRecipient() {
        notificationCommandService.notifyEventUpdated(
                EventConstants.FIRST_EVENT_ID,
                List.of(firstUserId, secondUserId),
                EventConstants.FIRST_EVENT_NAME
        );

        List<Notification> notifications = getPersistedNotifications();

        assertThat(notifications)
                .extracting(Notification::getRecipientId)
                .containsExactlyInAnyOrder(firstUserId, secondUserId);
        notifications.forEach(notification -> assertNotification(
                notification,
                notification.getRecipientId(),
                NotificationTemplateConstants.EVENT_UPDATE_TITLE,
                NotificationTemplateConstants.EVENT_UPDATE_BODY,
                NotificationResourceType.EVENT,
                EventConstants.FIRST_EVENT_ID,
                null,
                null
        ));
    }

    @Test
    @DisplayName("When notifying new event file should persist one nested notification per recipient")
    void whenNotifyingNewEventFileShouldPersistOneNestedNotificationPerRecipient() {
        notificationCommandService.notifyNewEventFile(
                EventConstants.FIRST_EVENT_ID,
                FileConstants.FIRST_FILE_ID,
                List.of(firstUserId, secondUserId),
                UserConstants.FIRST_USER_FULL_NAME
        );

        List<Notification> notifications = getPersistedNotifications();

        assertThat(notifications)
                .extracting(Notification::getRecipientId)
                .containsExactlyInAnyOrder(firstUserId, secondUserId);
        notifications.forEach(notification -> assertNotification(
                notification,
                notification.getRecipientId(),
                NotificationTemplateConstants.NEW_EVENT_FILE_TITLE,
                NotificationTemplateConstants.NEW_EVENT_FILE_BODY,
                NotificationResourceType.FILE,
                FileConstants.FIRST_FILE_ID,
                NotificationResourceType.EVENT,
                EventConstants.FIRST_EVENT_ID
        ));
    }

    @Test
    @DisplayName("When notifying new event thread should persist one nested notification per recipient")
    void whenNotifyingNewEventThreadShouldPersistOneNestedNotificationPerRecipient() {
        notificationCommandService.notifyNewEventThread(
                EventConstants.FIRST_EVENT_ID,
                ThreadConstants.FIRST_THREAD_ID,
                List.of(firstUserId, secondUserId),
                UserConstants.FIRST_USER_FULL_NAME
        );

        List<Notification> notifications = getPersistedNotifications();

        assertThat(notifications)
                .extracting(Notification::getRecipientId)
                .containsExactlyInAnyOrder(firstUserId, secondUserId);
        notifications.forEach(notification -> assertNotification(
                notification,
                notification.getRecipientId(),
                NotificationTemplateConstants.NEW_EVENT_THREAD_TITLE,
                NotificationTemplateConstants.NEW_EVENT_THREAD_BODY,
                NotificationResourceType.THREAD,
                ThreadConstants.FIRST_THREAD_ID,
                NotificationResourceType.EVENT,
                EventConstants.FIRST_EVENT_ID
        ));
    }

    @Test
    @DisplayName("When recipients contain duplicates should persist one notification per distinct recipient")
    void whenRecipientsContainDuplicatesShouldPersistOneNotificationPerDistinctRecipient() {
        notificationCommandService.notifyEventUpdated(
                EventConstants.FIRST_EVENT_ID,
                List.of(firstUserId, secondUserId, firstUserId, secondUserId),
                EventConstants.FIRST_EVENT_NAME
        );

        assertThat(getPersistedNotifications())
                .extracting(Notification::getRecipientId)
                .containsExactlyInAnyOrder(firstUserId, secondUserId);
    }

    @Test
    @DisplayName("When recipients are empty should not persist notifications")
    void whenRecipientsAreEmptyShouldNotPersistNotifications() {
        notificationCommandService.notifyEventUpdated(
                EventConstants.FIRST_EVENT_ID,
                List.of(),
                EventConstants.FIRST_EVENT_NAME
        );

        assertThat(notificationRepository.count()).isZero();
    }

    private Notification getOnlyPersistedNotification() {
        List<Notification> notifications = getPersistedNotifications();
        assertThat(notifications).hasSize(1);
        return notifications.getFirst();
    }

    private List<Notification> getPersistedNotifications() {
        notificationRepository.flush();
        return notificationRepository.findAll();
    }

    private void assertNotification(
            Notification notification,
            UUID recipientId,
            String title,
            String body,
            NotificationResourceType resourceType,
            UUID resourceId,
            NotificationResourceType parentResourceType,
            UUID parentResourceId
    ) {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(notification.getId()).isNotNull();
            softly.assertThat(notification.getRecipientId()).isEqualTo(recipientId);
            softly.assertThat(notification.getTitle()).isEqualTo(title);
            softly.assertThat(notification.getBody()).isEqualTo(body);
            softly.assertThat(notification.getResourceType()).isEqualTo(resourceType);
            softly.assertThat(notification.getResourceId()).isEqualTo(resourceId);
            softly.assertThat(notification.getParentResourceType()).isEqualTo(parentResourceType);
            softly.assertThat(notification.getParentResourceId()).isEqualTo(parentResourceId);
            softly.assertThat(notification.getCreatedAt()).isEqualTo(TimeConstants.NOW);
            softly.assertThat(notification.getReadAt()).isNull();
        });
    }
}
