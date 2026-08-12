package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.notification.domain.NotificationTemplate;
import com.mazurek.eventOrganizer.notification.repository.NotificationRepository;
import com.mazurek.eventOrganizer.testData.TestConstants.*;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static com.mazurek.eventOrganizer.testData.TestFailureHelper.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationCommandServiceImpl unit tests:")
public class NotificationCommandServiceImplUnitTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationTemplateService notificationTemplateService;
    @Mock
    private Clock clock;

    @InjectMocks
    private NotificationCommandServiceImpl notificationCommandService;

    @Nested
    @DisplayName("Notify private message tests:")
    class NotifyPrivateMessageTests {
        private final UUID conversationId = ConversationConstants.FIRST_CONVERSATION_ID;
        private final UUID recipientId = UserConstants.SECOND_USER_ID;
        private final String senderFullName = UserConstants.FIRST_USER_FULL_NAME;
        private final NotificationTemplate notificationTemplate = new NotificationTemplate(
                NotificationTemplateConstants.PRIVATE_MESSAGE_TITLE,
                NotificationTemplateConstants.PRIVATE_MESSAGE_BODY);

        private void setupSuccessfulMocks() {
            when(notificationTemplateService.buildPrivateMessage(senderFullName)).thenReturn(notificationTemplate);
            when(clock.instant()).thenReturn(TimeConstants.NOW);
        }

        @Test
        @DisplayName("When notifying private message should build private message template")
        public void whenNotifyingPrivateMessageShouldBuildPrivateMessageTemplate() {
            setupSuccessfulMocks();

            notificationCommandService.notifyPrivateMessage(
                    conversationId,
                    recipientId,
                    senderFullName
            );

            verify(notificationTemplateService, times(1))
                    .buildPrivateMessage(senderFullName);
        }

        @Test
        @DisplayName("When notifying private message should save only one notification")
        public void whenNotifyingPrivateMessageShouldSaveOnlyOneNotification() {
            setupSuccessfulMocks();

            notificationCommandService.notifyPrivateMessage(
                    conversationId,
                    recipientId,
                    senderFullName
            );

            ArgumentCaptor<Collection<Notification>> referenceArgumentCaptor = ArgumentCaptor.forClass(Collection.class);
            verify(notificationRepository, times(1)).saveAll(referenceArgumentCaptor.capture());
            Collection<Notification> notifications = referenceArgumentCaptor.getValue();

            assertThat(notifications).hasSize(1);
        }

        @Test
        @DisplayName("When notifying private message should create correct resource reference in notification")
        public void whenNotifyingPrivateMessageShouldCreateCorrectResourceReferenceInNotification() {
            setupSuccessfulMocks();

            ArgumentCaptor<Collection<Notification>> referenceArgumentCaptor = ArgumentCaptor.forClass(Collection.class);

            notificationCommandService.notifyPrivateMessage(
                    conversationId,
                    recipientId,
                    senderFullName
            );

            verify(notificationRepository, times(1)).saveAll(referenceArgumentCaptor.capture());
            Collection<Notification> notifications = referenceArgumentCaptor.getValue();

            Notification notification = requirePresent(notifications.stream().findFirst(), "Should contain exactly one element.");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(notification.getResourceId()).isNotNull();
                softly.assertThat(notification.getResourceId()).isEqualTo(conversationId);
                softly.assertThat(notification.getResourceType()).isNotNull();
                softly.assertThat(notification.getResourceType()).isEqualTo(NotificationResourceType.CONVERSATION);
                softly.assertThat(notification.getParentResourceId()).isNull();
                softly.assertThat(notification.getParentResourceType()).isNull();
            });

        }

        @Test
        @DisplayName("When notifying private message should contain template body and title")
        public void whenNotifyingPrivateMessageShouldContainTemplateBodyAndTitle() {
            setupSuccessfulMocks();

            ArgumentCaptor<Collection<Notification>> referenceArgumentCaptor = ArgumentCaptor.forClass(Collection.class);

            notificationCommandService.notifyPrivateMessage(
                    conversationId,
                    recipientId,
                    senderFullName
            );

            verify(notificationRepository, times(1)).saveAll(referenceArgumentCaptor.capture());
            Collection<Notification> notifications = referenceArgumentCaptor.getValue();

            Notification notification = requirePresent(notifications.stream().findFirst(), "Should contain exactly one element.");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(notification.getTitle()).isEqualTo(notificationTemplate.title());
                softly.assertThat(notification.getBody()).isEqualTo(notificationTemplate.body());
            });
        }

        @Test
        @DisplayName("When notifying private message should contain correct recipient id and timestamps")
        public void whenNotifyingPrivateMessageShouldContainCorrectRecipientIdAndTimestamps() {
            setupSuccessfulMocks();

            ArgumentCaptor<Collection<Notification>> referenceArgumentCaptor = ArgumentCaptor.forClass(Collection.class);

            notificationCommandService.notifyPrivateMessage(
                    conversationId,
                    recipientId,
                    senderFullName
            );

            verify(notificationRepository, times(1)).saveAll(referenceArgumentCaptor.capture());
            Collection<Notification> notifications = referenceArgumentCaptor.getValue();

            Notification notification = requirePresent(notifications.stream().findFirst(), "Should contain exactly one element.");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(notification.getRecipientId()).isEqualTo(recipientId);
                softly.assertThat(notification.getCreatedAt()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(notification.getReadAt()).isNull();
            });

            verify(clock, times(1)).instant();
        }

    }

    @Nested
    @DisplayName("Notify thread reply tests:")
    class NotifyThreadReplyTests {
        private final UUID eventId = EventConstants.FIRST_EVENT_ID;
        private final UUID threadId = ThreadConstants.FIRST_THREAD_ID;
        private final UUID recipientId = UserConstants.SECOND_USER_ID;
        private final String replierFullName = UserConstants.FIRST_USER_FULL_NAME;
        private final NotificationTemplate notificationTemplate = new NotificationTemplate(
                NotificationTemplateConstants.THREAD_REPLY_TITLE,
                NotificationTemplateConstants.THREAD_REPLY_BODY);

        private void setupSuccessfulMocks() {
            when(notificationTemplateService.buildThreadReply(replierFullName)).thenReturn(notificationTemplate);
            when(clock.instant()).thenReturn(TimeConstants.NOW);
        }

        @Test
        @DisplayName("When notifying thread reply should build thread reply template")
        public void whenNotifyingThreadReplyShouldBuildThreadReplyTemplate() {
            setupSuccessfulMocks();

            notificationCommandService.notifyThreadReply(
                    eventId,
                    threadId,
                    recipientId,
                    replierFullName
            );

            verify(notificationTemplateService, times(1))
                    .buildThreadReply(replierFullName);
        }

        @Test
        @DisplayName("When notifying thread reply should save only one notification")
        public void whenNotifyingThreadReplyShouldSaveOnlyOneNotification() {
            setupSuccessfulMocks();

            notificationCommandService.notifyThreadReply(
                    eventId,
                    threadId,
                    recipientId,
                    replierFullName
            );

            ArgumentCaptor<Collection<Notification>> notificationsArgumentCaptor =
                    ArgumentCaptor.forClass(Collection.class);
            verify(notificationRepository, times(1)).saveAll(notificationsArgumentCaptor.capture());

            assertThat(notificationsArgumentCaptor.getValue()).hasSize(1);
        }

        @Test
        @DisplayName("When notifying thread reply should create correct nested resource reference")
        public void whenNotifyingThreadReplyShouldCreateCorrectNestedResourceReference() {
            setupSuccessfulMocks();

            notificationCommandService.notifyThreadReply(
                    eventId,
                    threadId,
                    recipientId,
                    replierFullName
            );

            ArgumentCaptor<Collection<Notification>> notificationsArgumentCaptor =
                    ArgumentCaptor.forClass(Collection.class);
            verify(notificationRepository, times(1)).saveAll(notificationsArgumentCaptor.capture());

            Notification notification = requirePresent(
                    notificationsArgumentCaptor.getValue().stream().findFirst(),
                    "Should contain exactly one element."
            );

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(notification.getResourceType()).isEqualTo(NotificationResourceType.THREAD);
                softly.assertThat(notification.getResourceId()).isEqualTo(threadId);
                softly.assertThat(notification.getParentResourceType()).isEqualTo(NotificationResourceType.EVENT);
                softly.assertThat(notification.getParentResourceId()).isEqualTo(eventId);
            });
        }

        @Test
        @DisplayName("When notifying thread reply should contain template body and title")
        public void whenNotifyingThreadReplyShouldContainTemplateBodyAndTitle() {
            setupSuccessfulMocks();

            notificationCommandService.notifyThreadReply(
                    eventId,
                    threadId,
                    recipientId,
                    replierFullName
            );

            ArgumentCaptor<Collection<Notification>> notificationsArgumentCaptor =
                    ArgumentCaptor.forClass(Collection.class);
            verify(notificationRepository, times(1)).saveAll(notificationsArgumentCaptor.capture());

            Notification notification = requirePresent(
                    notificationsArgumentCaptor.getValue().stream().findFirst(),
                    "Should contain exactly one element."
            );

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(notification.getTitle()).isEqualTo(notificationTemplate.title());
                softly.assertThat(notification.getBody()).isEqualTo(notificationTemplate.body());
            });
        }

        @Test
        @DisplayName("When notifying thread reply should contain correct recipient id and timestamps")
        public void whenNotifyingThreadReplyShouldContainCorrectRecipientIdAndTimestamps() {
            setupSuccessfulMocks();

            notificationCommandService.notifyThreadReply(
                    eventId,
                    threadId,
                    recipientId,
                    replierFullName
            );

            ArgumentCaptor<Collection<Notification>> notificationsArgumentCaptor =
                    ArgumentCaptor.forClass(Collection.class);
            verify(notificationRepository, times(1)).saveAll(notificationsArgumentCaptor.capture());

            Notification notification = requirePresent(
                    notificationsArgumentCaptor.getValue().stream().findFirst(),
                    "Should contain exactly one element."
            );

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(notification.getRecipientId()).isEqualTo(recipientId);
                softly.assertThat(notification.getCreatedAt()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(notification.getReadAt()).isNull();
            });

            verify(clock, times(1)).instant();
        }
    }

    @Nested
    @DisplayName("Notify event update tests:")
    class NotifyEventUpdateTests {
        private final UUID eventId = EventConstants.FIRST_EVENT_ID;
        private final Collection<UUID> recipientIds = List.of(
                UserConstants.SECOND_USER_ID,
                UserConstants.THIRD_USER_ID
        );
        private final String eventName = EventConstants.FIRST_EVENT_NAME;
        private final NotificationTemplate notificationTemplate = new NotificationTemplate(
                NotificationTemplateConstants.EVENT_UPDATE_TITLE,
                NotificationTemplateConstants.EVENT_UPDATE_BODY);

        private void setupSuccessfulMocks() {
            when(notificationTemplateService.buildEventUpdate(eventName)).thenReturn(notificationTemplate);
            when(clock.instant()).thenReturn(TimeConstants.NOW);
        }

        @Test
        @DisplayName("When notifying event update should build event update template")
        public void whenNotifyingEventUpdateShouldBuildEventUpdateTemplate() {
            setupSuccessfulMocks();

            notificationCommandService.notifyEventUpdated(eventId, recipientIds, eventName);

            verify(notificationTemplateService, times(1)).buildEventUpdate(eventName);
        }

        @Test
        @DisplayName("When notifying event update should save one notification per recipient")
        public void whenNotifyingEventUpdateShouldSaveOneNotificationPerRecipient() {
            setupSuccessfulMocks();

            notificationCommandService.notifyEventUpdated(eventId, recipientIds, eventName);

            ArgumentCaptor<Collection<Notification>> notificationsArgumentCaptor =
                    ArgumentCaptor.forClass(Collection.class);
            verify(notificationRepository, times(1)).saveAll(notificationsArgumentCaptor.capture());

            assertThat(notificationsArgumentCaptor.getValue()).hasSize(recipientIds.size());
        }

        @Test
        @DisplayName("When notifying event update should create correct direct resource reference")
        public void whenNotifyingEventUpdateShouldCreateCorrectDirectResourceReference() {
            setupSuccessfulMocks();

            notificationCommandService.notifyEventUpdated(eventId, recipientIds, eventName);

            ArgumentCaptor<Collection<Notification>> notificationsArgumentCaptor =
                    ArgumentCaptor.forClass(Collection.class);
            verify(notificationRepository, times(1)).saveAll(notificationsArgumentCaptor.capture());
            Collection<Notification> notifications = notificationsArgumentCaptor.getValue();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(notifications)
                        .extracting(Notification::getResourceType)
                        .containsOnly(NotificationResourceType.EVENT);
                softly.assertThat(notifications)
                        .extracting(Notification::getResourceId)
                        .containsOnly(eventId);
                softly.assertThat(notifications)
                        .extracting(Notification::getParentResourceType)
                        .containsOnlyNulls();
                softly.assertThat(notifications)
                        .extracting(Notification::getParentResourceId)
                        .containsOnlyNulls();
            });
        }

        @Test
        @DisplayName("When notifying event update should contain template body and title")
        public void whenNotifyingEventUpdateShouldContainTemplateBodyAndTitle() {
            setupSuccessfulMocks();

            notificationCommandService.notifyEventUpdated(eventId, recipientIds, eventName);

            ArgumentCaptor<Collection<Notification>> notificationsArgumentCaptor =
                    ArgumentCaptor.forClass(Collection.class);
            verify(notificationRepository, times(1)).saveAll(notificationsArgumentCaptor.capture());
            Collection<Notification> notifications = notificationsArgumentCaptor.getValue();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(notifications)
                        .extracting(Notification::getTitle)
                        .containsOnly(notificationTemplate.title());
                softly.assertThat(notifications)
                        .extracting(Notification::getBody)
                        .containsOnly(notificationTemplate.body());
            });
        }

        @Test
        @DisplayName("When notifying event update should contain correct recipient ids and timestamps")
        public void whenNotifyingEventUpdateShouldContainCorrectRecipientIdsAndTimestamps() {
            setupSuccessfulMocks();

            notificationCommandService.notifyEventUpdated(eventId, recipientIds, eventName);

            ArgumentCaptor<Collection<Notification>> notificationsArgumentCaptor =
                    ArgumentCaptor.forClass(Collection.class);
            verify(notificationRepository, times(1)).saveAll(notificationsArgumentCaptor.capture());
            Collection<Notification> notifications = notificationsArgumentCaptor.getValue();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(notifications)
                        .extracting(Notification::getRecipientId)
                        .containsExactlyInAnyOrderElementsOf(recipientIds);
                softly.assertThat(notifications)
                        .extracting(Notification::getCreatedAt)
                        .containsOnly(TimeConstants.NOW);
                softly.assertThat(notifications)
                        .extracting(Notification::getReadAt)
                        .containsOnlyNulls();
            });

            verify(clock, times(1)).instant();
        }

        @Test
        @DisplayName("When notifying event update should remove duplicate recipient ids")
        public void whenNotifyingEventUpdateShouldRemoveDuplicateRecipientIds() {
            setupSuccessfulMocks();
            Collection<UUID> duplicateRecipientIds = List.of(
                    UserConstants.SECOND_USER_ID,
                    UserConstants.THIRD_USER_ID,
                    UserConstants.SECOND_USER_ID
            );

            notificationCommandService.notifyEventUpdated(eventId, duplicateRecipientIds, eventName);

            ArgumentCaptor<Collection<Notification>> notificationsArgumentCaptor =
                    ArgumentCaptor.forClass(Collection.class);
            verify(notificationRepository, times(1)).saveAll(notificationsArgumentCaptor.capture());
            Collection<Notification> notifications = notificationsArgumentCaptor.getValue();

            assertThat(notifications)
                    .extracting(Notification::getRecipientId)
                    .containsExactlyInAnyOrderElementsOf(recipientIds);
        }

        @Test
        @DisplayName("When notifying event update without recipients should not save notifications")
        public void whenNotifyingEventUpdateWithoutRecipientsShouldNotSaveNotifications() {
            setupSuccessfulMocks();

            notificationCommandService.notifyEventUpdated(eventId, List.of(), eventName);

            verify(notificationRepository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("Notify new event file tests:")
    class NotifyNewEventFileTests {
        private final UUID eventId = EventConstants.FIRST_EVENT_ID;
        private final UUID fileId = FileConstants.FIRST_FILE_ID;
        private final Collection<UUID> recipientIds = List.of(
                UserConstants.SECOND_USER_ID,
                UserConstants.THIRD_USER_ID
        );
        private final String uploaderFullName = UserConstants.FIRST_USER_FULL_NAME;
        private final NotificationTemplate notificationTemplate = new NotificationTemplate(
                NotificationTemplateConstants.NEW_EVENT_FILE_TITLE,
                NotificationTemplateConstants.NEW_EVENT_FILE_BODY);

        private void setupSuccessfulMocks() {
            when(notificationTemplateService.buildNewEventFile(uploaderFullName)).thenReturn(notificationTemplate);
            when(clock.instant()).thenReturn(TimeConstants.NOW);
        }

        @Test
        @DisplayName("When notifying new event file should build new event file template")
        public void whenNotifyingNewEventFileShouldBuildNewEventFileTemplate() {
            setupSuccessfulMocks();

            notificationCommandService.notifyNewEventFile(eventId, fileId, recipientIds, uploaderFullName);

            verify(notificationTemplateService, times(1)).buildNewEventFile(uploaderFullName);
        }

        @Test
        @DisplayName("When notifying new event file should save one notification per recipient")
        public void whenNotifyingNewEventFileShouldSaveOneNotificationPerRecipient() {
            setupSuccessfulMocks();

            notificationCommandService.notifyNewEventFile(eventId, fileId, recipientIds, uploaderFullName);

            ArgumentCaptor<Collection<Notification>> notificationsArgumentCaptor =
                    ArgumentCaptor.forClass(Collection.class);
            verify(notificationRepository, times(1)).saveAll(notificationsArgumentCaptor.capture());

            assertThat(notificationsArgumentCaptor.getValue()).hasSize(recipientIds.size());
        }

        @Test
        @DisplayName("When notifying new event file should create correct nested resource reference")
        public void whenNotifyingNewEventFileShouldCreateCorrectNestedResourceReference() {
            setupSuccessfulMocks();

            notificationCommandService.notifyNewEventFile(eventId, fileId, recipientIds, uploaderFullName);

            ArgumentCaptor<Collection<Notification>> notificationsArgumentCaptor =
                    ArgumentCaptor.forClass(Collection.class);
            verify(notificationRepository, times(1)).saveAll(notificationsArgumentCaptor.capture());
            Collection<Notification> notifications = notificationsArgumentCaptor.getValue();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(notifications)
                        .extracting(Notification::getResourceType)
                        .containsOnly(NotificationResourceType.FILE);
                softly.assertThat(notifications)
                        .extracting(Notification::getResourceId)
                        .containsOnly(fileId);
                softly.assertThat(notifications)
                        .extracting(Notification::getParentResourceType)
                        .containsOnly(NotificationResourceType.EVENT);
                softly.assertThat(notifications)
                        .extracting(Notification::getParentResourceId)
                        .containsOnly(eventId);
            });
        }

        @Test
        @DisplayName("When notifying new event file should contain template body and title")
        public void whenNotifyingNewEventFileShouldContainTemplateBodyAndTitle() {
            setupSuccessfulMocks();

            notificationCommandService.notifyNewEventFile(eventId, fileId, recipientIds, uploaderFullName);

            ArgumentCaptor<Collection<Notification>> notificationsArgumentCaptor =
                    ArgumentCaptor.forClass(Collection.class);
            verify(notificationRepository, times(1)).saveAll(notificationsArgumentCaptor.capture());
            Collection<Notification> notifications = notificationsArgumentCaptor.getValue();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(notifications)
                        .extracting(Notification::getTitle)
                        .containsOnly(notificationTemplate.title());
                softly.assertThat(notifications)
                        .extracting(Notification::getBody)
                        .containsOnly(notificationTemplate.body());
            });
        }

        @Test
        @DisplayName("When notifying new event file should contain correct recipient ids and timestamps")
        public void whenNotifyingNewEventFileShouldContainCorrectRecipientIdsAndTimestamps() {
            setupSuccessfulMocks();

            notificationCommandService.notifyNewEventFile(eventId, fileId, recipientIds, uploaderFullName);

            ArgumentCaptor<Collection<Notification>> notificationsArgumentCaptor =
                    ArgumentCaptor.forClass(Collection.class);
            verify(notificationRepository, times(1)).saveAll(notificationsArgumentCaptor.capture());
            Collection<Notification> notifications = notificationsArgumentCaptor.getValue();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(notifications)
                        .extracting(Notification::getRecipientId)
                        .containsExactlyInAnyOrderElementsOf(recipientIds);
                softly.assertThat(notifications)
                        .extracting(Notification::getCreatedAt)
                        .containsOnly(TimeConstants.NOW);
                softly.assertThat(notifications)
                        .extracting(Notification::getReadAt)
                        .containsOnlyNulls();
            });

            verify(clock, times(1)).instant();
        }

        @Test
        @DisplayName("When notifying new event file should remove duplicate recipient ids")
        public void whenNotifyingNewEventFileShouldRemoveDuplicateRecipientIds() {
            setupSuccessfulMocks();
            Collection<UUID> duplicateRecipientIds = List.of(
                    UserConstants.SECOND_USER_ID,
                    UserConstants.THIRD_USER_ID,
                    UserConstants.SECOND_USER_ID
            );

            notificationCommandService.notifyNewEventFile(
                    eventId,
                    fileId,
                    duplicateRecipientIds,
                    uploaderFullName
            );

            ArgumentCaptor<Collection<Notification>> notificationsArgumentCaptor =
                    ArgumentCaptor.forClass(Collection.class);
            verify(notificationRepository, times(1)).saveAll(notificationsArgumentCaptor.capture());
            Collection<Notification> notifications = notificationsArgumentCaptor.getValue();

            assertThat(notifications)
                    .extracting(Notification::getRecipientId)
                    .containsExactlyInAnyOrderElementsOf(recipientIds);
        }

        @Test
        @DisplayName("When notifying new event file without recipients should not save notifications")
        public void whenNotifyingNewEventFileWithoutRecipientsShouldNotSaveNotifications() {
            setupSuccessfulMocks();

            notificationCommandService.notifyNewEventFile(eventId, fileId, List.of(), uploaderFullName);

            verify(notificationRepository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("Notify new event thread tests:")
    class NotifyNewEventThreadTests {
        private final UUID eventId = EventConstants.FIRST_EVENT_ID;
        private final UUID threadId = ThreadConstants.FIRST_THREAD_ID;
        private final Collection<UUID> recipientIds = List.of(
                UserConstants.SECOND_USER_ID,
                UserConstants.THIRD_USER_ID
        );
        private final String creatorFullName = UserConstants.FIRST_USER_FULL_NAME;
        private final NotificationTemplate notificationTemplate = new NotificationTemplate(
                NotificationTemplateConstants.NEW_EVENT_THREAD_TITLE,
                NotificationTemplateConstants.NEW_EVENT_THREAD_BODY);

        private void setupSuccessfulMocks() {
            when(notificationTemplateService.buildNewEventThread(creatorFullName)).thenReturn(notificationTemplate);
            when(clock.instant()).thenReturn(TimeConstants.NOW);
        }

        @Test
        @DisplayName("When notifying new event thread should build new event thread template")
        public void whenNotifyingNewEventThreadShouldBuildNewEventThreadTemplate() {
            setupSuccessfulMocks();

            notificationCommandService.notifyNewEventThread(eventId, threadId, recipientIds, creatorFullName);

            verify(notificationTemplateService, times(1)).buildNewEventThread(creatorFullName);
        }

        @Test
        @DisplayName("When notifying new event thread should save one notification per recipient")
        public void whenNotifyingNewEventThreadShouldSaveOneNotificationPerRecipient() {
            setupSuccessfulMocks();

            notificationCommandService.notifyNewEventThread(eventId, threadId, recipientIds, creatorFullName);

            ArgumentCaptor<Collection<Notification>> notificationsArgumentCaptor =
                    ArgumentCaptor.forClass(Collection.class);
            verify(notificationRepository, times(1)).saveAll(notificationsArgumentCaptor.capture());

            assertThat(notificationsArgumentCaptor.getValue()).hasSize(recipientIds.size());
        }

        @Test
        @DisplayName("When notifying new event thread should create correct nested resource reference")
        public void whenNotifyingNewEventThreadShouldCreateCorrectNestedResourceReference() {
            setupSuccessfulMocks();

            notificationCommandService.notifyNewEventThread(eventId, threadId, recipientIds, creatorFullName);

            ArgumentCaptor<Collection<Notification>> notificationsArgumentCaptor =
                    ArgumentCaptor.forClass(Collection.class);
            verify(notificationRepository, times(1)).saveAll(notificationsArgumentCaptor.capture());
            Collection<Notification> notifications = notificationsArgumentCaptor.getValue();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(notifications)
                        .extracting(Notification::getResourceType)
                        .containsOnly(NotificationResourceType.THREAD);
                softly.assertThat(notifications)
                        .extracting(Notification::getResourceId)
                        .containsOnly(threadId);
                softly.assertThat(notifications)
                        .extracting(Notification::getParentResourceType)
                        .containsOnly(NotificationResourceType.EVENT);
                softly.assertThat(notifications)
                        .extracting(Notification::getParentResourceId)
                        .containsOnly(eventId);
            });
        }

        @Test
        @DisplayName("When notifying new event thread should contain template body and title")
        public void whenNotifyingNewEventThreadShouldContainTemplateBodyAndTitle() {
            setupSuccessfulMocks();

            notificationCommandService.notifyNewEventThread(eventId, threadId, recipientIds, creatorFullName);

            ArgumentCaptor<Collection<Notification>> notificationsArgumentCaptor =
                    ArgumentCaptor.forClass(Collection.class);
            verify(notificationRepository, times(1)).saveAll(notificationsArgumentCaptor.capture());
            Collection<Notification> notifications = notificationsArgumentCaptor.getValue();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(notifications)
                        .extracting(Notification::getTitle)
                        .containsOnly(notificationTemplate.title());
                softly.assertThat(notifications)
                        .extracting(Notification::getBody)
                        .containsOnly(notificationTemplate.body());
            });
        }

        @Test
        @DisplayName("When notifying new event thread should contain correct recipient ids and timestamps")
        public void whenNotifyingNewEventThreadShouldContainCorrectRecipientIdsAndTimestamps() {
            setupSuccessfulMocks();

            notificationCommandService.notifyNewEventThread(eventId, threadId, recipientIds, creatorFullName);

            ArgumentCaptor<Collection<Notification>> notificationsArgumentCaptor =
                    ArgumentCaptor.forClass(Collection.class);
            verify(notificationRepository, times(1)).saveAll(notificationsArgumentCaptor.capture());
            Collection<Notification> notifications = notificationsArgumentCaptor.getValue();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(notifications)
                        .extracting(Notification::getRecipientId)
                        .containsExactlyInAnyOrderElementsOf(recipientIds);
                softly.assertThat(notifications)
                        .extracting(Notification::getCreatedAt)
                        .containsOnly(TimeConstants.NOW);
                softly.assertThat(notifications)
                        .extracting(Notification::getReadAt)
                        .containsOnlyNulls();
            });

            verify(clock, times(1)).instant();
        }

        @Test
        @DisplayName("When notifying new event thread should remove duplicate recipient ids")
        public void whenNotifyingNewEventThreadShouldRemoveDuplicateRecipientIds() {
            setupSuccessfulMocks();
            Collection<UUID> duplicateRecipientIds = List.of(
                    UserConstants.SECOND_USER_ID,
                    UserConstants.THIRD_USER_ID,
                    UserConstants.SECOND_USER_ID
            );

            notificationCommandService.notifyNewEventThread(
                    eventId,
                    threadId,
                    duplicateRecipientIds,
                    creatorFullName
            );

            ArgumentCaptor<Collection<Notification>> notificationsArgumentCaptor =
                    ArgumentCaptor.forClass(Collection.class);
            verify(notificationRepository, times(1)).saveAll(notificationsArgumentCaptor.capture());
            Collection<Notification> notifications = notificationsArgumentCaptor.getValue();

            assertThat(notifications)
                    .extracting(Notification::getRecipientId)
                    .containsExactlyInAnyOrderElementsOf(recipientIds);
        }

        @Test
        @DisplayName("When notifying new event thread without recipients should not save notifications")
        public void whenNotifyingNewEventThreadWithoutRecipientsShouldNotSaveNotifications() {
            setupSuccessfulMocks();

            notificationCommandService.notifyNewEventThread(eventId, threadId, List.of(), creatorFullName);

            verify(notificationRepository, never()).saveAll(any());
        }
    }

}
