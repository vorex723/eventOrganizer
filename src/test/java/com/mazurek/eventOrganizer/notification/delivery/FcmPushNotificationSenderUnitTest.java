package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.testData.builders.FcmSendResultTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.NotificationDeliveryTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.NotificationTestBuilder;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmSendOutcome;
import static com.mazurek.eventOrganizer.testData.TestConstants.SendResultConstants.FCM_NO_TARGETS_ERROR_MESSAGE;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationDelivery;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmApiClient;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmSendResult;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeviceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmPushNotificationSenderUnitTest contracts:")
class FcmPushNotificationSenderUnitTest {

    private static final String INSTALLATION_ID = "snapshotted-installation";

    @Mock
    private FcmApiClient fcmApiClient;
    @Mock
    private NotificationDeviceRepository notificationDeviceRepository;

    @ParameterizedTest
    @MethodSource("sendCases")
    void whenSendingPushShouldUseSnapshotAndMapOutcome(
            NotificationChannel channel,
            FcmSendResult fcmResult,
            NotificationSendOutcome expectedOutcome
    ) {
        NotificationDelivery delivery = delivery(channel, UUID.randomUUID(), INSTALLATION_ID);
        stubFcmResult(channel, delivery, fcmResult);
        NotificationSender sender = sender(channel);

        NotificationSendResult result = sender.send(delivery);

        assertThat(sender.supportedChannel()).isEqualTo(channel);
        assertThat(result).isNotNull();
        assertThat(result.outcome()).isEqualTo(expectedOutcome);
        assertThat(result.errorMessage()).isEqualTo(fcmResult.errorMessage());
        verifyTargetedSend(channel, delivery);
        verifyNoMoreInteractions(fcmApiClient);
        verifyNoInteractions(notificationDeviceRepository);
    }

    @ParameterizedTest
    @EnumSource(value = NotificationChannel.class, names = {"PUSH_MOBILE", "PUSH_WEB"})
    void whenPushSnapshotIsMissingShouldFailWithoutSending(NotificationChannel channel) {
        NotificationDelivery delivery = delivery(channel, UUID.randomUUID(), null);
        NotificationSender sender = sender(channel);

        NotificationSendResult result = sender.send(delivery);

        assertThat(result.outcome()).isEqualTo(NotificationSendOutcome.PERMANENT_FAILURE);
        assertThat(result.errorMessage()).contains("no target snapshot");
        verifyNoInteractions(fcmApiClient, notificationDeviceRepository);
    }

    @ParameterizedTest
    @EnumSource(value = NotificationChannel.class, names = {"PUSH_MOBILE", "PUSH_WEB"})
    void whenInstallationIsInvalidShouldRemoveOnlySnapshottedDevice(NotificationChannel channel) {
        UUID deviceId = UUID.randomUUID();
        NotificationDelivery delivery = delivery(channel, deviceId, INSTALLATION_ID);
        FcmSendResult invalidTarget = new FcmSendResultTestBuilder()
                .outcome(FcmSendOutcome.NO_TARGETS)
                .targetCount(1)
                .successCount(0)
                .invalidTargetCount(1)
                .errorMessage("Invalid installation.")
                .build();
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
    void whenDeviceSnapshotIsMissingShouldNotRemoveDevices(NotificationChannel channel) {
        NotificationDelivery delivery = delivery(channel, null, INSTALLATION_ID);
        stubFcmResult(channel, delivery, new FcmSendResultTestBuilder()
                .outcome(FcmSendOutcome.NO_TARGETS)
                .targetCount(1)
                .successCount(0)
                .invalidTargetCount(1)
                .errorMessage("Invalid installation.")
                .build());
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
                        Arguments.of(channel, new FcmSendResultTestBuilder()
                                .outcome(FcmSendOutcome.SENT)
                                .targetCount(1)
                                .successCount(1)
                                .build(), NotificationSendOutcome.SENT),
                        Arguments.of(channel, new FcmSendResultTestBuilder()
                                .outcome(FcmSendOutcome.NO_TARGETS)
                                .targetCount(0)
                                .successCount(0)
                                .errorMessage(FCM_NO_TARGETS_ERROR_MESSAGE)
                                .build(), NotificationSendOutcome.SKIPPED),
                        Arguments.of(channel, new FcmSendResultTestBuilder()
                                .outcome(FcmSendOutcome.RETRYABLE_FAILURE)
                                .targetCount(1)
                                .successCount(0)
                                .retryableFailureCount(1)
                                .errorMessage("FCM is temporarily unavailable.")
                                .build(),
                                NotificationSendOutcome.RETRYABLE_FAILURE),
                        Arguments.of(channel, new FcmSendResultTestBuilder()
                                .outcome(FcmSendOutcome.PERMANENT_FAILURE)
                                .targetCount(1)
                                .successCount(0)
                                .permanentFailureCount(1)
                                .errorMessage("FCM request is invalid.")
                                .build(),
                                NotificationSendOutcome.PERMANENT_FAILURE)
                ));
    }

    private static NotificationDelivery delivery(NotificationChannel channel, UUID deviceId, String installationId) {
        return new NotificationDeliveryTestBuilder().id(null)
                .notification(notification())
                .channel(channel)
                .targetKey("device:" + deviceId)
                .targetDeviceId(deviceId)
                .targetInstallationId(installationId)
                .targetEmail(null)
                .status(null)
                .createdAt(null)
                .build();
    }

    private static Notification notification() {
        return new NotificationTestBuilder()
                .resourceType(NotificationResourceType.EVENT)
                .resourceId(UUID.randomUUID())
                .build();
    }
}
