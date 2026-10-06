package com.mazurek.eventOrganizer.notification.firebaseCloudMessaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FcmSendResultUnitTest contracts:")
class FcmSendResultUnitTest {

    @ParameterizedTest
    @MethodSource("resultCounts")
    void whenTargetsAreClassifiedShouldDeriveAggregateOutcome(
            int targetCount,
            int successCount,
            int invalidTargetCount,
            int retryableFailureCount,
            int permanentFailureCount,
            FcmSendOutcome expectedOutcome
    ) {
        FcmSendResult result = FcmSendResult.fromCounts(
                targetCount,
                successCount,
                invalidTargetCount,
                retryableFailureCount,
                permanentFailureCount,
                "FCM failure"
        );

        assertThat(result).isNotNull();
        assertThat(result.outcome()).isEqualTo(expectedOutcome);
        assertThat(result.targetCount()).isEqualTo(targetCount);
        assertThat(result.successCount()).isEqualTo(successCount);
        assertThat(result.invalidTargetCount()).isEqualTo(invalidTargetCount);
        assertThat(result.retryableFailureCount()).isEqualTo(retryableFailureCount);
        assertThat(result.permanentFailureCount()).isEqualTo(permanentFailureCount);
        assertThat(result.errorMessage()).isEqualTo("FCM failure");
    }

    @ParameterizedTest
    @MethodSource("invalidResults")
    void whenTargetCountsAreInvalidShouldRejectResult(
            int targetCount,
            int successCount,
            int invalidTargetCount,
            int retryableFailureCount,
            int permanentFailureCount
    ) {
        assertThatThrownBy(() -> new FcmSendResult(
                FcmSendOutcome.SENT,
                targetCount,
                successCount,
                invalidTargetCount,
                retryableFailureCount,
                permanentFailureCount,
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<Arguments> resultCounts() {
        return Stream.of(
                Arguments.of(2, 1, 1, 0, 0, FcmSendOutcome.SENT),
                Arguments.of(2, 0, 2, 0, 0, FcmSendOutcome.NO_TARGETS),
                Arguments.of(2, 0, 0, 1, 1, FcmSendOutcome.RETRYABLE_FAILURE),
                Arguments.of(2, 0, 0, 0, 2, FcmSendOutcome.PERMANENT_FAILURE)
        );
    }

    private static Stream<Arguments> invalidResults() {
        return Stream.of(
                Arguments.of(1, 0, 0, 0, 0),
                Arguments.of(1, 2, 0, 0, 0),
                Arguments.of(-1, 0, 0, 0, 0)
        );
    }
}
