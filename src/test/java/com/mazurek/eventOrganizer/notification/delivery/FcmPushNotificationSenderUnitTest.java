package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationDelivery;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmApiClient;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmSendResult;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeviceRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FcmPushNotificationSenderUnitTest {

    private static final String INSTALLATION_ID = "snapshotted-installation";

    @Mock
    private FcmApiClient fcmApiClient;
    @Mock
    private NotificationDeviceRepository notificationDeviceRepository;

    @ParameterizedTest
    @MethodSource("sendCases")
    void sendsToSnapshottedInstallationAndMapsOutcome(
            NotificationChannel channel,
            FcmSendResult fcmResult,
            NotificationSendOutcome expectedOutcome
    ) {
        NotificationDelivery delivery = delivery(channel, UUID.randomUUID(), INSTALLATION_ID);
        stubFcmResult(channel, delivery, fcmResult);
        NotificationSender sender = sender(channel);

        NotificationSendResult result = sender.send(delivery);

        assertThat(sender.supportedChannel()).isEqualTo(channel);
        assertThat(result.outcome()).isEqualTo(expectedOutcome);
        assertThat(result.errorMessage()).isEqualTo(fcmResult.errorMessage());
        verifyTargetedSend(channel, delivery);
        verifyNoMoreInteractions(fcmApiClient);
        verifyNoInteractions(notificationDeviceRepository);
    }

    @ParameterizedTest
    @EnumSource(value = NotificationChannel.class, names = {"PUSH_MOBILE", "PUSH_WEB"})
    void rejectsDeliveryWithoutInstallationSnapshot(NotificationChannel channel) {
        NotificationDelivery delivery = delivery(channel, UUID.randomUUID(), null);
        NotificationSender sender = sender(channel);

        NotificationSendResult result = sender.send(delivery);

        assertThat(result.outcome()).isEqualTo(NotificationSendOutcome.PERMANENT_FAILURE);
        assertThat(result.errorMessage()).contains("no target snapshot");
        verifyNoInteractions(fcmApiClient, notificationDeviceRepository);
    }

    @ParameterizedTest
    @EnumSource(value = NotificationChannel.class, names = {"PUSH_MOBILE", "PUSH_WEB"})
    void removesOnlyTheSnapshottedDeviceWhenInstallationIsInvalid(NotificationChannel channel) {
        UUID deviceId = UUID.randomUUID();
        NotificationDelivery delivery = delivery(channel, deviceId, INSTALLATION_ID);
        FcmSendResult invalidTarget = FcmSendResult.fromCounts(1, 0, 1, 0, 0, "Invalid installation.");
        stubFcmResult(channel, delivery, invalidTarget);
        NotificationSender sender = sender(channel);

        NotificationSendResult result = sender.send(delivery);

        assertThat(result.outcome()).isEqualTo(NotificationSendOutcome.SKIPPED);
        assertThat(result.errorMessage()).isEqualTo("Invalid installation.");
        verifyTargetedSend(channel, delivery);
        verify(notificationDeviceRepository).deleteIfOwnedByIdAndUserIdAndFirebaseInstallationId(
                deviceId,
                delivery.getNotification().getRecipientId(),
                INSTALLATION_ID
        );
        verifyNoMoreInteractions(fcmApiClient, notificationDeviceRepository);
    }

    @ParameterizedTest
    @EnumSource(value = NotificationChannel.class, names = {"PUSH_MOBILE", "PUSH_WEB"})
    void doesNotRemoveDeviceWithoutSnapshottedDeviceId(NotificationChannel channel) {
        NotificationDelivery delivery = delivery(channel, null, INSTALLATION_ID);
        stubFcmResult(channel, delivery, FcmSendResult.fromCounts(1, 0, 1, 0, 0, "Invalid installation."));
        NotificationSender sender = sender(channel);

        NotificationSendResult result = sender.send(delivery);

        assertThat(result.outcome()).isEqualTo(NotificationSendOutcome.SKIPPED);
        verifyTargetedSend(channel, delivery);
        verifyNoInteractions(notificationDeviceRepository);
    }

    private NotificationSender sender(NotificationChannel channel) {
        return channel == NotificationChannel.PUSH_MOBILE
                ? new FcmPushMobileNotificationSender(fcmApiClient, notificationDeviceRepository)
                : new FcmWebPushNotificationSender(fcmApiClient, notificationDeviceRepository);
    }

    private void stubFcmResult(NotificationChannel channel, NotificationDelivery delivery, FcmSendResult result) {
        if (channel == NotificationChannel.PUSH_MOBILE) {
            when(fcmApiClient.sendNotificationToInstallationMobile(
                    delivery.getNotification(), delivery.getTargetInstallationId())).thenReturn(result);
        } else {
            when(fcmApiClient.sendNotificationToInstallationWeb(
                    delivery.getNotification(), delivery.getTargetInstallationId())).thenReturn(result);
        }
    }

    private void verifyTargetedSend(NotificationChannel channel, NotificationDelivery delivery) {
        if (channel == NotificationChannel.PUSH_MOBILE) {
            verify(fcmApiClient).sendNotificationToInstallationMobile(
                    delivery.getNotification(), delivery.getTargetInstallationId());
        } else {
            verify(fcmApiClient).sendNotificationToInstallationWeb(
                    delivery.getNotification(), delivery.getTargetInstallationId());
        }
    }

    private static Stream<Arguments> sendCases() {
        return Stream.of(NotificationChannel.PUSH_MOBILE, NotificationChannel.PUSH_WEB)
                .flatMap(channel -> Stream.of(
                        Arguments.of(channel, FcmSendResult.successful(1), NotificationSendOutcome.SENT),
                        Arguments.of(channel, FcmSendResult.noTargets(), NotificationSendOutcome.SKIPPED),
                        Arguments.of(channel, FcmSendResult.retryableFailure(1, "FCM is temporarily unavailable."),
                                NotificationSendOutcome.RETRYABLE_FAILURE),
                        Arguments.of(channel, FcmSendResult.permanentFailure(1, "FCM request is invalid."),
                                NotificationSendOutcome.PERMANENT_FAILURE)
                ));
    }

    private static NotificationDelivery delivery(NotificationChannel channel, UUID deviceId, String installationId) {
        return NotificationDelivery.builder()
                .notification(notification())
                .channel(channel)
                .targetKey("device:" + deviceId)
                .targetDeviceId(deviceId)
                .targetInstallationId(installationId)
                .build();
    }

    private static Notification notification() {
        return Notification.builder()
                .id(UUID.randomUUID())
                .recipientId(UUID.randomUUID())
                .title("Title")
                .body("Body")
                .resourceType(NotificationResourceType.EVENT)
                .resourceId(UUID.randomUUID())
                .createdAt(Instant.parse("2026-01-02T03:04:05Z"))
                .build();
    }
}
