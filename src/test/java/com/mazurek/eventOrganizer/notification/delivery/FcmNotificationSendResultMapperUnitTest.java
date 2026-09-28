package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmSendResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FcmNotificationSendResultMapperUnitTest {

    @ParameterizedTest
    @MethodSource("fcmResults")
    void mapsEveryFcmOutcomeToTheCorrespondingDeliveryOutcome(
            FcmSendResult fcmResult,
            NotificationSendOutcome expectedOutcome
    ) {
        NotificationSendResult result = FcmNotificationSendResultMapper.map(fcmResult);

        assertThat(result.outcome()).isEqualTo(expectedOutcome);
        assertThat(result.errorMessage()).isEqualTo(fcmResult.errorMessage());
        assertThat(result.providerMessageId()).isNull();
    }

    private static Stream<Arguments> fcmResults() {
        return Stream.of(
                Arguments.of(FcmSendResult.successful(2), NotificationSendOutcome.SENT),
                Arguments.of(FcmSendResult.noTargets(), NotificationSendOutcome.SKIPPED),
                Arguments.of(
                        FcmSendResult.retryableFailure(2, "FCM is temporarily unavailable."),
                        NotificationSendOutcome.RETRYABLE_FAILURE
                ),
                Arguments.of(
                        FcmSendResult.permanentFailure(2, "FCM request is invalid."),
                        NotificationSendOutcome.PERMANENT_FAILURE
                )
        );
    }
}
