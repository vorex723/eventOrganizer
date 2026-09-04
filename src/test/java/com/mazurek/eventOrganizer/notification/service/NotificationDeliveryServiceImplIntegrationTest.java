package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.exception.notification.NotificationDeliveryNotFoundException;
import com.mazurek.eventOrganizer.exception.notification.NotificationDeliveryNotProcessableException;
import com.mazurek.eventOrganizer.exception.notification.NotificationSendFailException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.notification.delivery.NotificationSendRequest;
import com.mazurek.eventOrganizer.notification.delivery.NotificationSendResult;
import com.mazurek.eventOrganizer.notification.delivery.NotificationSenderDispatcher;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationDelivery;
import com.mazurek.eventOrganizer.notification.domain.NotificationDeliveryStatus;
import com.mazurek.eventOrganizer.notification.domain.NotificationPreference;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeliveryRepository;
import com.mazurek.eventOrganizer.notification.repository.NotificationPreferenceRepository;
import com.mazurek.eventOrganizer.notification.repository.NotificationRepository;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.builders.NotificationTestBuilder;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static com.mazurek.eventOrganizer.notification.domain.NotificationChannel.EMAIL;
import static com.mazurek.eventOrganizer.notification.domain.NotificationChannel.PUSH_MOBILE;
import static com.mazurek.eventOrganizer.notification.domain.NotificationChannel.PUSH_WEB;
import static com.mazurek.eventOrganizer.notification.domain.NotificationDeliveryStatus.DEAD;
import static com.mazurek.eventOrganizer.notification.domain.NotificationDeliveryStatus.FAILED;
import static com.mazurek.eventOrganizer.notification.domain.NotificationDeliveryStatus.PENDING;
import static com.mazurek.eventOrganizer.notification.domain.NotificationDeliveryStatus.SENT;
import static com.mazurek.eventOrganizer.notification.domain.NotificationResourceType.CONVERSATION;
import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants.NOW;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.FIRST_USER_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NotificationDeliveryServiceImpl integration tests:")
class NotificationDeliveryServiceImplIntegrationTest {

    @Autowired
    private NotificationDeliveryService notificationDeliveryService;
    @Autowired
    private NotificationDeliveryRepository notificationDeliveryRepository;
    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private DeletionService deletionService;
    @MockitoBean
    private NotificationSenderDispatcher notificationSenderDispatcher;

    private UUID firstUserId;

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();

        firstUserId = userRepository.findByIgnoreCaseEmail(FIRST_USER_EMAIL)
                .orElseThrow(UserNotFoundException::new)
                .getId();
    }

    @AfterEach
    void tearDown() {
        deletionService.deleteAllSafe();
    }

    @Nested
    @DisplayName("Create deliveries tests:")
    class CreateDeliveriesTests {

        @Test
        @DisplayName("When event uses default preferences should persist mobile, web, and email deliveries")
        void whenEventUsesDefaultPreferencesShouldPersistMobileWebAndEmailDeliveries() {
            Notification notification = persist(NotificationTestBuilder.eventUpdateNotification());

            notificationDeliveryService.createDeliveries(notification);

            List<NotificationDelivery> deliveries = notificationDeliveryRepository.findAll();
            assertPersistedDeliveries(
                    deliveries,
                    notification,
                    PUSH_MOBILE,
                    PUSH_WEB,
                    EMAIL
            );
        }

        @Test
        @DisplayName("When conversation uses default preferences should persist mobile and web deliveries")
        void whenConversationUsesDefaultPreferencesShouldPersistMobileAndWebDeliveries() {
            Notification notification = persist(NotificationTestBuilder.privateMessageNotification());

            notificationDeliveryService.createDeliveries(notification);

            assertPersistedDeliveries(
                    notificationDeliveryRepository.findAll(),
                    notification,
                    PUSH_MOBILE,
                    PUSH_WEB
            );
        }

        @Test
        @DisplayName("When conversation preferences are overridden should persist only enabled email delivery")
        void whenConversationPreferencesAreOverriddenShouldPersistOnlyEnabledEmailDelivery() {
            notificationPreferenceRepository.saveAllAndFlush(List.of(
                    preference(CONVERSATION, PUSH_MOBILE, false),
                    preference(CONVERSATION, PUSH_WEB, false),
                    preference(CONVERSATION, EMAIL, true)
            ));
            Notification notification = persist(NotificationTestBuilder.privateMessageNotification());

            notificationDeliveryService.createDeliveries(notification);

            assertPersistedDeliveries(
                    notificationDeliveryRepository.findAll(),
                    notification,
                    EMAIL
            );
        }

        @Test
        @DisplayName("When every external channel is disabled should preserve notification without deliveries")
        void whenEveryExternalChannelIsDisabledShouldPreserveNotificationWithoutDeliveries() {
            notificationPreferenceRepository.saveAllAndFlush(List.of(
                    preference(CONVERSATION, PUSH_MOBILE, false),
                    preference(CONVERSATION, PUSH_WEB, false)
            ));
            Notification notification = persist(NotificationTestBuilder.privateMessageNotification());

            notificationDeliveryService.createDeliveries(notification);

            assertThat(notificationDeliveryRepository.findAll()).isEmpty();
            assertThat(notificationRepository.findById(notification.getId()))
                    .isPresent();
        }
    }

    @Nested
    @DisplayName("Process delivery tests:")
    class ProcessDeliveryTests {

        @Test
        @DisplayName("When sender reports success should persist sent delivery state")
        void whenSenderReportsSuccessShouldPersistSentDeliveryState() {
            NotificationDelivery delivery = persistDelivery(PENDING, 0);
            String providerMessageId = "provider-message-id";
            when(notificationSenderDispatcher.send(any(), any()))
                    .thenReturn(NotificationSendResult.sent(providerMessageId));

            notificationDeliveryService.processDelivery(delivery.getId());

            NotificationDelivery persistedDelivery = reloadDelivery(delivery.getId());
            assertThat(persistedDelivery.getStatus()).isEqualTo(SENT);
            assertThat(persistedDelivery.getAttemptCount()).isOne();
            assertThat(persistedDelivery.getProviderMessageId()).isEqualTo(providerMessageId);
            assertThat(persistedDelivery.getSentAt()).isEqualTo(NOW);
            assertThat(persistedDelivery.getLastError()).isNull();
            assertThat(persistedDelivery.getNextAttemptAt()).isNull();
        }

        @Test
        @DisplayName("When sender reports failure should persist failed delivery state with one-hour retry delay")
        void whenSenderReportsFailureShouldPersistFailedDeliveryStateWithOneHourRetryDelay() {
            NotificationDelivery delivery = persistDelivery(PENDING, 0);
            String errorMessage = "Provider unavailable.";
            when(notificationSenderDispatcher.send(any(), any()))
                    .thenReturn(NotificationSendResult.failed(errorMessage));

            notificationDeliveryService.processDelivery(delivery.getId());

            NotificationDelivery persistedDelivery = reloadDelivery(delivery.getId());
            assertThat(persistedDelivery.getStatus()).isEqualTo(FAILED);
            assertThat(persistedDelivery.getAttemptCount()).isOne();
            assertThat(persistedDelivery.getLastError()).isEqualTo(errorMessage);
            assertThat(persistedDelivery.getNextAttemptAt()).isEqualTo(NOW.plus(1, ChronoUnit.HOURS));
        }

        @Test
        @DisplayName("When sender throws send failure should persist failed delivery state with one-hour retry delay")
        void whenSenderThrowsSendFailureShouldPersistFailedDeliveryStateWithOneHourRetryDelay() {
            NotificationDelivery delivery = persistDelivery(PENDING, 0);
            String errorMessage = "Provider request failed.";
            when(notificationSenderDispatcher.send(any(), any()))
                    .thenThrow(new NotificationSendFailException(errorMessage));

            notificationDeliveryService.processDelivery(delivery.getId());

            NotificationDelivery persistedDelivery = reloadDelivery(delivery.getId());
            assertThat(persistedDelivery.getStatus()).isEqualTo(FAILED);
            assertThat(persistedDelivery.getAttemptCount()).isOne();
            assertThat(persistedDelivery.getLastError()).isEqualTo(errorMessage);
            assertThat(persistedDelivery.getNextAttemptAt()).isEqualTo(NOW.plus(1, ChronoUnit.HOURS));
        }

        @Test
        @DisplayName("When fifth attempt fails should persist three-hour retry delay")
        void whenFifthAttemptFailsShouldPersistThreeHourRetryDelay() {
            NotificationDelivery delivery = persistDelivery(FAILED, 4);
            String errorMessage = "Provider unavailable.";
            when(notificationSenderDispatcher.send(any(), any()))
                    .thenReturn(NotificationSendResult.failed(errorMessage));

            notificationDeliveryService.processDelivery(delivery.getId());

            NotificationDelivery persistedDelivery = reloadDelivery(delivery.getId());
            assertThat(persistedDelivery.getStatus()).isEqualTo(FAILED);
            assertThat(persistedDelivery.getAttemptCount()).isEqualTo(5);
            assertThat(persistedDelivery.getLastError()).isEqualTo(errorMessage);
            assertThat(persistedDelivery.getNextAttemptAt()).isEqualTo(NOW.plus(3, ChronoUnit.HOURS));
        }

        @Test
        @DisplayName("When sender throws unexpected runtime exception should roll back delivery state")
        void whenSenderThrowsUnexpectedRuntimeExceptionShouldRollBackDeliveryState() {
            NotificationDelivery delivery = persistDelivery(PENDING, 0);
            IllegalStateException failure = new IllegalStateException("Unexpected sender failure.");
            when(notificationSenderDispatcher.send(any(), any())).thenThrow(failure);

            assertThatThrownBy(() -> notificationDeliveryService.processDelivery(delivery.getId()))
                    .isSameAs(failure);

            NotificationDelivery persistedDelivery = reloadDelivery(delivery.getId());
            assertThat(persistedDelivery.getStatus()).isEqualTo(PENDING);
            assertThat(persistedDelivery.getAttemptCount()).isZero();
            assertThat(persistedDelivery.getLastError()).isNull();
            assertThat(persistedDelivery.getNextAttemptAt()).isNull();
            assertThat(persistedDelivery.getSentAt()).isNull();
            assertThat(persistedDelivery.getProviderMessageId()).isNull();
        }

        @ParameterizedTest
        @EnumSource(
                value = NotificationDeliveryStatus.class,
                names = {"SENT", "DEAD", "SKIPPED", "PROCESSING"}
        )
        @DisplayName("When delivery is not processable should preserve persisted state without sending")
        void whenDeliveryIsNotProcessableShouldPreservePersistedStateWithoutSending(
                NotificationDeliveryStatus status
        ) {
            NotificationDelivery delivery = persistDelivery(status, 3);

            assertThatThrownBy(() -> notificationDeliveryService.processDelivery(delivery.getId()))
                    .isInstanceOf(NotificationDeliveryNotProcessableException.class);

            NotificationDelivery persistedDelivery = reloadDelivery(delivery.getId());
            assertThat(persistedDelivery.getStatus()).isEqualTo(status);
            assertThat(persistedDelivery.getAttemptCount()).isEqualTo(3);
            verifyNoInteractions(notificationSenderDispatcher);
        }

        @Test
        @DisplayName("When delivery does not exist should propagate not found exception without sending")
        void whenDeliveryDoesNotExistShouldPropagateNotFoundExceptionWithoutSending() {
            UUID unknownDeliveryId = UUID.randomUUID();

            assertThatThrownBy(() -> notificationDeliveryService.processDelivery(unknownDeliveryId))
                    .isInstanceOf(NotificationDeliveryNotFoundException.class);

            verifyNoInteractions(notificationSenderDispatcher);
        }
    }

    @Nested
    @DisplayName("Process pending deliveries tests:")
    class ProcessPendingDeliveriesTests {

        @Test
        @DisplayName("When deliveries have mixed eligibility should process only pending and due failed deliveries")
        void whenDeliveriesHaveMixedEligibilityShouldProcessOnlyPendingAndDueFailedDeliveries() {
            NotificationDelivery pendingDelivery = persistDelivery(PENDING, 0);
            NotificationDelivery dueFailedDelivery = persistDelivery(FAILED, 1, NOW);
            NotificationDelivery futureFailedDelivery = persistDelivery(
                    FAILED,
                    1,
                    NOW.plus(1, ChronoUnit.SECONDS)
            );
            NotificationDelivery failedWithoutRetryTime = persistDelivery(FAILED, 1);
            NotificationDelivery sentDelivery = persistDelivery(SENT, 2);
            NotificationDelivery deadDelivery = persistDelivery(DEAD, 2);
            NotificationDelivery skippedDelivery = persistDelivery(NotificationDeliveryStatus.SKIPPED, 2);
            NotificationDelivery processingDelivery = persistDelivery(NotificationDeliveryStatus.PROCESSING, 2);
            when(notificationSenderDispatcher.send(any(), any()))
                    .thenReturn(NotificationSendResult.sent("provider-message-id"));

            notificationDeliveryService.processPendingDeliveries();

            assertThat(reloadDelivery(pendingDelivery.getId()))
                    .extracting(NotificationDelivery::getStatus, NotificationDelivery::getAttemptCount)
                    .containsExactly(SENT, 1);
            assertThat(reloadDelivery(dueFailedDelivery.getId()))
                    .extracting(NotificationDelivery::getStatus, NotificationDelivery::getAttemptCount)
                    .containsExactly(SENT, 2);
            assertUnchanged(futureFailedDelivery, FAILED, 1);
            assertUnchanged(failedWithoutRetryTime, FAILED, 1);
            assertUnchanged(sentDelivery, SENT, 2);
            assertUnchanged(deadDelivery, DEAD, 2);
            assertUnchanged(skippedDelivery, NotificationDeliveryStatus.SKIPPED, 2);
            assertUnchanged(processingDelivery, NotificationDeliveryStatus.PROCESSING, 2);
            verify(notificationSenderDispatcher, times(2)).send(any(), any());
        }

        @Test
        @DisplayName("When sender throws send failure should persist failed state through the batch coordinator")
        void whenSenderThrowsSendFailureShouldPersistFailedStateThroughBatchCoordinator() {
            NotificationDelivery delivery = persistDelivery(PENDING, 0);
            String errorMessage = "Provider request failed.";
            when(notificationSenderDispatcher.send(any(), any()))
                    .thenThrow(new NotificationSendFailException(errorMessage));

            notificationDeliveryService.processPendingDeliveries();

            NotificationDelivery persistedDelivery = reloadDelivery(delivery.getId());
            assertThat(persistedDelivery.getStatus()).isEqualTo(FAILED);
            assertThat(persistedDelivery.getAttemptCount()).isOne();
            assertThat(persistedDelivery.getLastError()).isEqualTo(errorMessage);
            assertThat(persistedDelivery.getNextAttemptAt()).isEqualTo(NOW.plus(1, ChronoUnit.HOURS));
        }

        @Test
        @DisplayName("When one delivery fails unexpectedly should roll it back and continue with later deliveries")
        void whenOneDeliveryFailsUnexpectedlyShouldRollItBackAndContinueWithLaterDeliveries() {
            NotificationDelivery failingDelivery = persistDelivery(PENDING, 0);
            NotificationDelivery succeedingDelivery = persistDelivery(PENDING, 0);
            UUID failingNotificationId = failingDelivery.getNotification().getId();
            when(notificationSenderDispatcher.send(any(), any())).thenAnswer(invocation -> {
                NotificationSendRequest request = invocation.getArgument(1);
                if (request.notificationId().equals(failingNotificationId)) {
                    throw new IllegalStateException("Unexpected sender failure.");
                }
                return NotificationSendResult.sent("provider-message-id");
            });

            notificationDeliveryService.processPendingDeliveries();

            assertUnchanged(failingDelivery, PENDING, 0);
            assertThat(reloadDelivery(succeedingDelivery.getId()))
                    .extracting(NotificationDelivery::getStatus, NotificationDelivery::getAttemptCount)
                    .containsExactly(SENT, 1);
            verify(notificationSenderDispatcher, times(2)).send(any(), any());
        }

        @Test
        @DisplayName("When no delivery is processable should not dispatch notifications or change deliveries")
        void whenNoDeliveryIsProcessableShouldNotDispatchNotificationsOrChangeDeliveries() {
            NotificationDelivery futureFailedDelivery = persistDelivery(
                    FAILED,
                    1,
                    NOW.plus(1, ChronoUnit.SECONDS)
            );
            NotificationDelivery sentDelivery = persistDelivery(SENT, 2);
            NotificationDelivery deadDelivery = persistDelivery(DEAD, 2);
            NotificationDelivery skippedDelivery = persistDelivery(NotificationDeliveryStatus.SKIPPED, 2);
            NotificationDelivery processingDelivery = persistDelivery(NotificationDeliveryStatus.PROCESSING, 2);

            notificationDeliveryService.processPendingDeliveries();

            assertUnchanged(futureFailedDelivery, FAILED, 1);
            assertUnchanged(sentDelivery, SENT, 2);
            assertUnchanged(deadDelivery, DEAD, 2);
            assertUnchanged(skippedDelivery, NotificationDeliveryStatus.SKIPPED, 2);
            assertUnchanged(processingDelivery, NotificationDeliveryStatus.PROCESSING, 2);
            verifyNoInteractions(notificationSenderDispatcher);
        }
    }

    private Notification persist(NotificationTestBuilder builder) {
        return notificationRepository.saveAndFlush(
                builder
                        .id(null)
                        .recipientId(firstUserId)
                        .build()
        );
    }

    private NotificationDelivery persistDelivery(
            NotificationDeliveryStatus status,
            int attemptCount
    ) {
        return persistDelivery(status, attemptCount, null);
    }

    private NotificationDelivery persistDelivery(
            NotificationDeliveryStatus status,
            int attemptCount,
            Instant nextAttemptAt
    ) {
        Notification notification = persist(NotificationTestBuilder.privateMessageNotification());

        return notificationDeliveryRepository.saveAndFlush(
                NotificationDelivery.builder()
                        .notification(notification)
                        .channel(PUSH_MOBILE)
                        .status(status)
                        .attemptCount(attemptCount)
                        .nextAttemptAt(nextAttemptAt)
                        .createdAt(NOW)
                        .build()
        );
    }

    private NotificationDelivery reloadDelivery(UUID deliveryId) {
        return notificationDeliveryRepository.findById(deliveryId)
                .orElseThrow();
    }

    private void assertUnchanged(
            NotificationDelivery delivery,
            NotificationDeliveryStatus expectedStatus,
            int expectedAttemptCount
    ) {
        assertThat(reloadDelivery(delivery.getId()))
                .extracting(NotificationDelivery::getStatus, NotificationDelivery::getAttemptCount)
                .containsExactly(expectedStatus, expectedAttemptCount);
    }

    private NotificationPreference preference(
            NotificationResourceType resourceType,
            NotificationChannel channel,
            boolean enabled
    ) {
        return NotificationPreference.builder()
                .userId(firstUserId)
                .resourceType(resourceType)
                .channel(channel)
                .enabled(enabled)
                .build();
    }

    private void assertPersistedDeliveries(
            List<NotificationDelivery> deliveries,
            Notification notification,
            NotificationChannel... expectedChannels
    ) {
        assertThat(deliveries)
                .extracting(NotificationDelivery::getChannel)
                .containsExactlyInAnyOrder(expectedChannels);
        assertThat(deliveries)
                .extracting(
                        NotificationDelivery::getStatus,
                        NotificationDelivery::getAttemptCount,
                        NotificationDelivery::getCreatedAt
                )
                .containsOnly(tuple(PENDING, 0, NOW));
        assertThat(deliveries).allSatisfy(delivery -> {
            assertThat(delivery.getId()).isNotNull();
            assertThat(delivery.getNotification().getId()).isEqualTo(notification.getId());
            assertThat(delivery.getNextAttemptAt()).isNull();
            assertThat(delivery.getSentAt()).isNull();
            assertThat(delivery.getProviderMessageId()).isNull();
            assertThat(delivery.getLastError()).isNull();
        });
    }
}
