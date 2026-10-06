package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.testData.builders.FcmSendResultTestBuilder;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmSendOutcome;
import static com.mazurek.eventOrganizer.testData.TestConstants.SendResultConstants.FCM_NO_TARGETS_ERROR_MESSAGE;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmSendResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FcmNotificationSendResultMapperUnitTest contracts:")
class FcmNotificationSendResultMapperUnitTest {

    @ParameterizedTest
    @MethodSource("fcmResults")
    void whenMappingFcmResultShouldPreserveDeliveryOutcome(
            FcmSendResult fcmResult,
            NotificationSendOutcome expectedOutcome
    ) {
        NotificationSendResult result = FcmNotificationSendResultMapper.map(fcmResult);

        assertThat(result).isNotNull();
        assertThat(result.outcome()).isEqualTo(expectedOutcome);
        assertThat(result.errorMessage()).isEqualTo(fcmResult.errorMessage());
        assertThat(result.providerMessageId()).isNull();
    }

    private static Stream<Arguments> fcmResults() {
        return Stream.of(
                Arguments.of(new FcmSendResultTestBuilder()
                        .outcome(FcmSendOutcome.SENT)
                        .targetCount(2)
                        .successCount(2)
                        .build(), NotificationSendOutcome.SENT),
                Arguments.of(new FcmSendResultTestBuilder()
                        .outcome(FcmSendOutcome.NO_TARGETS)
                        .targetCount(0)
                        .successCount(0)
                        .errorMessage(FCM_NO_TARGETS_ERROR_MESSAGE)
                        .build(), NotificationSendOutcome.SKIPPED),
                Arguments.of(
                        new FcmSendResultTestBuilder()
                                .outcome(FcmSendOutcome.RETRYABLE_FAILURE)
                                .targetCount(2)
                                .successCount(0)
                                .retryableFailureCount(2)
                                .errorMessage("FCM is temporarily unavailable.")
                                .build(),
                        NotificationSendOutcome.RETRYABLE_FAILURE
                ),
                Arguments.of(
                        new FcmSendResultTestBuilder()
                                .outcome(FcmSendOutcome.PERMANENT_FAILURE)
                                .targetCount(2)
                                .successCount(0)
                                .permanentFailureCount(2)
                                .errorMessage("FCM request is invalid.")
                                .build(),
                        NotificationSendOutcome.PERMANENT_FAILURE
                )
        );
    }
}
