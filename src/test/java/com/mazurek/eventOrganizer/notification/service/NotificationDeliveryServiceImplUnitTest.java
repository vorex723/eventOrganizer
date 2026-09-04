package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.exception.notification.NotificationDeliveryNotFoundException;
import com.mazurek.eventOrganizer.exception.notification.NotificationDeliveryNotProcessableException;
import com.mazurek.eventOrganizer.exception.notification.NotificationSendFailException;
import com.mazurek.eventOrganizer.notification.delivery.NotificationSendRequest;
import com.mazurek.eventOrganizer.notification.delivery.NotificationSendResult;
import com.mazurek.eventOrganizer.notification.delivery.NotificationSenderDispatcher;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationDelivery;
import com.mazurek.eventOrganizer.notification.domain.NotificationDeliveryStatus;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeliveryRepository;
import com.mazurek.eventOrganizer.testData.builders.NotificationTestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static com.mazurek.eventOrganizer.notification.domain.NotificationChannel.EMAIL;
import static com.mazurek.eventOrganizer.notification.domain.NotificationChannel.PUSH_MOBILE;
import static com.mazurek.eventOrganizer.notification.domain.NotificationDeliveryStatus.FAILED;
import static com.mazurek.eventOrganizer.notification.domain.NotificationDeliveryStatus.PENDING;
import static com.mazurek.eventOrganizer.notification.domain.NotificationDeliveryStatus.SENT;
import static com.mazurek.eventOrganizer.notification.domain.NotificationResourceType.CONVERSATION;
import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants.NOW;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.FIRST_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationDeliveryServiceImpl unit tests:")
class NotificationDeliveryServiceImplUnitTest {

    @Mock
    private Clock clock;
    @Mock
    private NotificationDeliveryRepository notificationDeliveryRepository;
    @Mock
    private NotificationPreferenceService notificationPreferenceService;
    @Mock
    private NotificationSenderDispatcher notificationSenderDispatcher;
    @Mock
    private TransactionTemplate transactionTemplate;
    @InjectMocks
    private NotificationDeliveryServiceImpl notificationDeliveryService;

    private Notification notification;
    private UUID deliveryId;

    @BeforeEach
    void setUp() {
        notification = NotificationTestBuilder.privateMessageNotification().build();
        deliveryId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Create deliveries tests:")
    class CreateDeliveriesTests {

        @Test
        @DisplayName("When multiple channels are enabled should create one pending delivery per channel")
        void whenMultipleChannelsAreEnabledShouldCreateOnePendingDeliveryPerChannel() {
            when(notificationPreferenceService.getEnabledExternalChannels(
                    FIRST_USER_ID,
                    CONVERSATION
            )).thenReturn(Set.of(PUSH_MOBILE, EMAIL));
            when(clock.instant()).thenReturn(NOW);

            notificationDeliveryService.createDeliveries(notification);

            List<NotificationDelivery> savedDeliveries = captureSavedDeliveries();
            verify(notificationPreferenceService).getEnabledExternalChannels(
                    FIRST_USER_ID,
                    CONVERSATION
            );
            verify(clock).instant();
            assertThat(savedDeliveries)
                    .extracting(
                            NotificationDelivery::getChannel,
                            NotificationDelivery::getStatus,
                            NotificationDelivery::getAttemptCount,
                            NotificationDelivery::getCreatedAt
                    )
                    .containsExactlyInAnyOrder(
                            tuple(PUSH_MOBILE, PENDING, 0, NOW),
                            tuple(EMAIL, PENDING, 0, NOW)
                    );
            assertThat(savedDeliveries).allSatisfy(delivery -> {
                assertThat(delivery.getId()).isNull();
                assertThat(delivery.getNotification()).isSameAs(notification);
                assertThat(delivery.getNextAttemptAt()).isNull();
                assertThat(delivery.getSentAt()).isNull();
                assertThat(delivery.getProviderMessageId()).isNull();
                assertThat(delivery.getLastError()).isNull();
            });
            assertThat(notification.getDeliveries())
                    .containsExactlyInAnyOrderElementsOf(savedDeliveries);
        }

        @Test
        @DisplayName("When one channel is enabled should create delivery only for that channel")
        void whenOneChannelIsEnabledShouldCreateDeliveryOnlyForThatChannel() {
            when(notificationPreferenceService.getEnabledExternalChannels(
                    FIRST_USER_ID,
                    CONVERSATION
            )).thenReturn(Set.of(EMAIL));
            when(clock.instant()).thenReturn(NOW);

            notificationDeliveryService.createDeliveries(notification);

            assertThat(captureSavedDeliveries())
                    .extracting(NotificationDelivery::getChannel)
                    .containsExactly(EMAIL);
            assertThat(notification.getDeliveries())
                    .extracting(NotificationDelivery::getChannel)
                    .containsExactly(EMAIL);
        }

        @Test
        @DisplayName("When no channels are enabled should save no deliveries")
        void whenNoChannelsAreEnabledShouldSaveNoDeliveries() {
            when(notificationPreferenceService.getEnabledExternalChannels(
                    FIRST_USER_ID,
                    CONVERSATION
            )).thenReturn(Set.of());

            notificationDeliveryService.createDeliveries(notification);

            assertThat(captureSavedDeliveries()).isEmpty();
            assertThat(notification.getDeliveries()).isEmpty();
        }

        @Test
        @DisplayName("When resolving preferences fails should propagate exception without creating deliveries")
        void whenResolvingPreferencesFailsShouldPropagateExceptionWithoutCreatingDeliveries() {
            IllegalStateException failure = new IllegalStateException("Preference resolution failed.");
            when(notificationPreferenceService.getEnabledExternalChannels(
                    FIRST_USER_ID,
                    CONVERSATION
            )).thenThrow(failure);

            assertThatThrownBy(() -> notificationDeliveryService.createDeliveries(notification))
                    .isSameAs(failure);

            verifyNoInteractions(clock, notificationDeliveryRepository);
            assertThat(notification.getDeliveries()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Process delivery tests:")
    class ProcessDeliveryTests {

        @Test
        @DisplayName("When a pending delivery is sent should mark it sent and forward the notification data")
        void whenPendingDeliveryIsSentShouldMarkItSentAndForwardNotificationData() {
            NotificationDelivery delivery = deliveryWith(PENDING, 0);
            String providerMessageId = "provider-message-id";
            givenDeliveryIsFound(delivery);
            when(notificationSenderDispatcher.send(any(), any()))
                    .thenReturn(NotificationSendResult.sent(providerMessageId));
            when(clock.instant()).thenReturn(NOW);

            notificationDeliveryService.processDelivery(deliveryId);

            ArgumentCaptor<NotificationSendRequest> requestCaptor =
                    ArgumentCaptor.forClass(NotificationSendRequest.class);
            verify(notificationSenderDispatcher).send(eq(PUSH_MOBILE), requestCaptor.capture());
            assertThat(requestCaptor.getValue()).isEqualTo(new NotificationSendRequest(
                    notification.getId(),
                    notification.getRecipientId(),
                    notification.getTitle(),
                    notification.getBody(),
                    notification.getResourceType(),
                    notification.getResourceId(),
                    Map.of()
            ));
            assertThat(delivery.getStatus()).isEqualTo(SENT);
            assertThat(delivery.getAttemptCount()).isOne();
            assertThat(delivery.getProviderMessageId()).isEqualTo(providerMessageId);
            assertThat(delivery.getSentAt()).isEqualTo(NOW);
            assertThat(delivery.getLastError()).isNull();
            assertThat(delivery.getNextAttemptAt()).isNull();
        }

        @Test
        @DisplayName("When sender returns failure should mark a pending delivery failed with one-hour retry delay")
        void whenSenderReturnsFailureShouldMarkPendingDeliveryFailedWithOneHourRetryDelay() {
            NotificationDelivery delivery = deliveryWith(PENDING, 0);
            String errorMessage = "Provider unavailable.";
            givenDeliveryIsFound(delivery);
            when(notificationSenderDispatcher.send(any(), any()))
                    .thenReturn(NotificationSendResult.failed(errorMessage));
            when(clock.instant()).thenReturn(NOW);

            notificationDeliveryService.processDelivery(deliveryId);

            assertThat(delivery.getStatus()).isEqualTo(FAILED);
            assertThat(delivery.getAttemptCount()).isOne();
            assertThat(delivery.getLastError()).isEqualTo(errorMessage);
            assertThat(delivery.getNextAttemptAt()).isEqualTo(NOW.plus(1, ChronoUnit.HOURS));
            assertThat(delivery.getSentAt()).isNull();
            assertThat(delivery.getProviderMessageId()).isNull();
        }

        @Test
        @DisplayName("When sender throws send failure should mark delivery failed with one-hour retry delay")
        void whenSenderThrowsSendFailureShouldMarkDeliveryFailedWithOneHourRetryDelay() {
            NotificationDelivery delivery = deliveryWith(PENDING, 0);
            String errorMessage = "Provider request failed.";
            givenDeliveryIsFound(delivery);
            when(notificationSenderDispatcher.send(any(), any()))
                    .thenThrow(new NotificationSendFailException(errorMessage));
            when(clock.instant()).thenReturn(NOW);

            notificationDeliveryService.processDelivery(deliveryId);

            assertThat(delivery.getStatus()).isEqualTo(FAILED);
            assertThat(delivery.getAttemptCount()).isOne();
            assertThat(delivery.getLastError()).isEqualTo(errorMessage);
            assertThat(delivery.getNextAttemptAt()).isEqualTo(NOW.plus(1, ChronoUnit.HOURS));
        }

        @Test
        @DisplayName("When fifth attempt fails should schedule retry after three hours")
        void whenFifthAttemptFailsShouldScheduleRetryAfterThreeHours() {
            NotificationDelivery delivery = deliveryWith(FAILED, 4);
            String errorMessage = "Provider unavailable.";
            givenDeliveryIsFound(delivery);
            when(notificationSenderDispatcher.send(any(), any()))
                    .thenReturn(NotificationSendResult.failed(errorMessage));
            when(clock.instant()).thenReturn(NOW);

            notificationDeliveryService.processDelivery(deliveryId);

            assertThat(delivery.getStatus()).isEqualTo(FAILED);
            assertThat(delivery.getAttemptCount()).isEqualTo(5);
            assertThat(delivery.getLastError()).isEqualTo(errorMessage);
            assertThat(delivery.getNextAttemptAt()).isEqualTo(NOW.plus(3, ChronoUnit.HOURS));
        }

        @Test
        @DisplayName("When delivery does not exist should propagate not found exception")
        void whenDeliveryDoesNotExistShouldPropagateNotFoundException() {
            when(notificationDeliveryRepository.findById(deliveryId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationDeliveryService.processDelivery(deliveryId))
                    .isInstanceOf(NotificationDeliveryNotFoundException.class);

            verifyNoInteractions(notificationSenderDispatcher, clock);
        }

        @ParameterizedTest
        @EnumSource(
                value = NotificationDeliveryStatus.class,
                names = {"SENT", "DEAD", "SKIPPED", "PROCESSING"}
        )
        @DisplayName("When delivery is not processable should propagate exception without sending")
        void whenDeliveryIsNotProcessableShouldPropagateExceptionWithoutSending(
                NotificationDeliveryStatus status
        ) {
            NotificationDelivery delivery = deliveryWith(status, 3);
            givenDeliveryIsFound(delivery);

            assertThatThrownBy(() -> notificationDeliveryService.processDelivery(deliveryId))
                    .isInstanceOf(NotificationDeliveryNotProcessableException.class);

            assertThat(delivery.getAttemptCount()).isEqualTo(3);
            verifyNoInteractions(notificationSenderDispatcher, clock);
        }

        @Test
        @DisplayName("When sender throws unexpected runtime exception should propagate it")
        void whenSenderThrowsUnexpectedRuntimeExceptionShouldPropagateIt() {
            NotificationDelivery delivery = deliveryWith(PENDING, 0);
            IllegalStateException failure = new IllegalStateException("Unexpected sender failure.");
            givenDeliveryIsFound(delivery);
            when(notificationSenderDispatcher.send(any(), any())).thenThrow(failure);

            assertThatThrownBy(() -> notificationDeliveryService.processDelivery(deliveryId))
                    .isSameAs(failure);
        }
    }

    @Nested
    @DisplayName("Process pending deliveries tests:")
    class ProcessPendingDeliveriesTests {

        private NotificationDeliveryServiceImpl notificationDeliveryServiceSpy;

        @BeforeEach
        void setUpProcessPendingDeliveries() {
            notificationDeliveryServiceSpy = spy(notificationDeliveryService);
        }

        private void givenTransactionTemplateExecutesCallback() {
            doAnswer(invocation -> {
                Consumer<TransactionStatus> callback = invocation.getArgument(0);
                callback.accept(mock(TransactionStatus.class));
                return null;
            }).when(transactionTemplate).executeWithoutResult(any());
        }

        @Test
        @DisplayName("When no delivery is processable should not start transactions or process deliveries")
        void whenNoDeliveryIsProcessableShouldNotStartTransactionsOrProcessDeliveries() {
            when(clock.instant()).thenReturn(NOW);
            when(notificationDeliveryRepository.findProcessableDeliveryIds(eq(NOW), any(Pageable.class)))
                    .thenReturn(Page.empty());

            notificationDeliveryServiceSpy.processPendingDeliveries();

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(notificationDeliveryRepository).findProcessableDeliveryIds(eq(NOW), pageableCaptor.capture());
            assertThat(pageableCaptor.getValue()).isEqualTo(PageRequest.of(0, 100));
            verifyNoInteractions(transactionTemplate);
            verify(notificationDeliveryServiceSpy, never()).processDelivery(any());
        }

        @Test
        @DisplayName("When deliveries are processable should process every ID in its own transaction callback")
        void whenDeliveriesAreProcessableShouldProcessEveryIdInItsOwnTransactionCallback() {
            UUID firstDeliveryId = UUID.randomUUID();
            UUID secondDeliveryId = UUID.randomUUID();
            UUID thirdDeliveryId = UUID.randomUUID();
            givenProcessableDeliveryIds(firstDeliveryId, secondDeliveryId, thirdDeliveryId);
            givenTransactionTemplateExecutesCallback();
            doNothing().when(notificationDeliveryServiceSpy).processDelivery(any());

            notificationDeliveryServiceSpy.processPendingDeliveries();

            verify(transactionTemplate, times(3)).executeWithoutResult(any());
            InOrder inOrder = inOrder(notificationDeliveryServiceSpy);
            inOrder.verify(notificationDeliveryServiceSpy).processDelivery(firstDeliveryId);
            inOrder.verify(notificationDeliveryServiceSpy).processDelivery(secondDeliveryId);
            inOrder.verify(notificationDeliveryServiceSpy).processDelivery(thirdDeliveryId);
        }

        @Test
        @DisplayName("When processing individual deliveries fails should continue with later deliveries")
        void whenProcessingIndividualDeliveriesFailsShouldContinueWithLaterDeliveries() {
            UUID missingDeliveryId = UUID.randomUUID();
            UUID notProcessableDeliveryId = UUID.randomUUID();
            UUID failingDeliveryId = UUID.randomUUID();
            UUID laterDeliveryId = UUID.randomUUID();
            givenProcessableDeliveryIds(
                    missingDeliveryId,
                    notProcessableDeliveryId,
                    failingDeliveryId,
                    laterDeliveryId
            );
            givenTransactionTemplateExecutesCallback();
            doThrow(new NotificationDeliveryNotFoundException())
                    .when(notificationDeliveryServiceSpy).processDelivery(missingDeliveryId);
            doThrow(new NotificationDeliveryNotProcessableException())
                    .when(notificationDeliveryServiceSpy).processDelivery(notProcessableDeliveryId);
            doThrow(new IllegalStateException("Unexpected processing failure."))
                    .when(notificationDeliveryServiceSpy).processDelivery(failingDeliveryId);
            doNothing().when(notificationDeliveryServiceSpy).processDelivery(laterDeliveryId);

            notificationDeliveryServiceSpy.processPendingDeliveries();

            verify(transactionTemplate, times(4)).executeWithoutResult(any());
            verify(notificationDeliveryServiceSpy).processDelivery(laterDeliveryId);
        }

        @Test
        @DisplayName("When selecting processable deliveries fails should propagate without starting transactions")
        void whenSelectingProcessableDeliveriesFailsShouldPropagateWithoutStartingTransactions() {
            IllegalStateException failure = new IllegalStateException("Could not select deliveries.");
            when(clock.instant()).thenReturn(NOW);
            when(notificationDeliveryRepository.findProcessableDeliveryIds(eq(NOW), any(Pageable.class)))
                    .thenThrow(failure);

            assertThatThrownBy(notificationDeliveryServiceSpy::processPendingDeliveries)
                    .isSameAs(failure);

            verifyNoInteractions(transactionTemplate);
            verify(notificationDeliveryServiceSpy, never()).processDelivery(any());
        }

        private void givenProcessableDeliveryIds(UUID... deliveryIds) {
            when(clock.instant()).thenReturn(NOW);
            when(notificationDeliveryRepository.findProcessableDeliveryIds(eq(NOW), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(deliveryIds)));
        }
    }

    @SuppressWarnings("unchecked")
    private List<NotificationDelivery> captureSavedDeliveries() {
        ArgumentCaptor<List<NotificationDelivery>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationDeliveryRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private NotificationDelivery deliveryWith(NotificationDeliveryStatus status, int attemptCount) {
        return NotificationDelivery.builder()
                .id(deliveryId)
                .notification(notification)
                .channel(PUSH_MOBILE)
                .status(status)
                .attemptCount(attemptCount)
                .createdAt(NOW)
                .build();
    }

    private void givenDeliveryIsFound(NotificationDelivery delivery) {
        when(notificationDeliveryRepository.findById(deliveryId)).thenReturn(Optional.of(delivery));
    }
}
