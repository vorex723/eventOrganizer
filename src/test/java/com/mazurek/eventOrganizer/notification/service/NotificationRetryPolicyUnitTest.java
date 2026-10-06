package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.testData.builders.NotificationPropertiesTestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NotificationRetryPolicyUnitTest contracts:")
class NotificationRetryPolicyUnitTest {

    private final NotificationRetryPolicy retryPolicy =
            new NotificationRetryPolicy(new NotificationPropertiesTestBuilder().build());

    @Test
    void whenAttemptsFailShouldReturnConfiguredIncreasingDelays() {
        assertThat(retryPolicy.delayAfterFailedAttempt(1)).isEqualTo(Duration.ofMinutes(1));
        assertThat(retryPolicy.delayAfterFailedAttempt(2)).isEqualTo(Duration.ofMinutes(5));
        assertThat(retryPolicy.delayAfterFailedAttempt(3)).isEqualTo(Duration.ofMinutes(15));
        assertThat(retryPolicy.delayAfterFailedAttempt(4)).isEqualTo(Duration.ofMinutes(30));
        assertThat(retryPolicy.delayAfterFailedAttempt(5)).isEqualTo(Duration.ofMinutes(60));
    }

    @Test
    void whenSixthAttemptIsReachedShouldExhaustRetries() {
        assertThat(retryPolicy.attemptsExhausted(5)).isFalse();
        assertThat(retryPolicy.attemptsExhausted(6)).isTrue();
        assertThatThrownBy(() -> retryPolicy.delayAfterFailedAttempt(6))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
