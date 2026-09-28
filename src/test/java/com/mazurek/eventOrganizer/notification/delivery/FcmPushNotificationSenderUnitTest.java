package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmApiClient;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmSendResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FcmPushNotificationSenderUnitTest {

    @Mock
    private FcmApiClient fcmApiClient;

    @ParameterizedTest
    @MethodSource("fcmResults")
    void mobileSenderUsesMobileFcmCallAndMapsItsOutcome(
            FcmSendResult fcmResult,
            NotificationSendOutcome expectedOutcome
    ) {
        Notification notification = notification();
        when(fcmApiClient.sendNotificationToSingleUserMobile(notification)).thenReturn(fcmResult);
        FcmPushMobileNotificationSender sender = new FcmPushMobileNotificationSender(fcmApiClient);

        NotificationSendResult result = sender.send(notification);

        assertThat(sender.supportedChannel()).isEqualTo(NotificationChannel.PUSH_MOBILE);
        assertThat(result.outcome()).isEqualTo(expectedOutcome);
        assertThat(result.errorMessage()).isEqualTo(fcmResult.errorMessage());
        verify(fcmApiClient).sendNotificationToSingleUserMobile(notification);
    }

    @ParameterizedTest
    @MethodSource("fcmResults")
    void webSenderUsesWebFcmCallAndMapsItsOutcome(
            FcmSendResult fcmResult,
            NotificationSendOutcome expectedOutcome
    ) {
        Notification notification = notification();
        when(fcmApiClient.sendNotificationToSingleUserWeb(notification)).thenReturn(fcmResult);
        FcmWebPushNotificationSender sender = new FcmWebPushNotificationSender(fcmApiClient);

        NotificationSendResult result = sender.send(notification);

        assertThat(sender.supportedChannel()).isEqualTo(NotificationChannel.PUSH_WEB);
        assertThat(result.outcome()).isEqualTo(expectedOutcome);
        assertThat(result.errorMessage()).isEqualTo(fcmResult.errorMessage());
        verify(fcmApiClient).sendNotificationToSingleUserWeb(notification);
    }

    private static Stream<Arguments> fcmResults() {
        return Stream.of(
                Arguments.of(FcmSendResult.successful(1), NotificationSendOutcome.SENT),
                Arguments.of(FcmSendResult.noTargets(), NotificationSendOutcome.SKIPPED),
                Arguments.of(
                        FcmSendResult.retryableFailure(1, "FCM is temporarily unavailable."),
                        NotificationSendOutcome.RETRYABLE_FAILURE
                ),
                Arguments.of(
                        FcmSendResult.permanentFailure(1, "FCM request is invalid."),
                        NotificationSendOutcome.PERMANENT_FAILURE
                )
        );
    }

    private Notification notification() {
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
