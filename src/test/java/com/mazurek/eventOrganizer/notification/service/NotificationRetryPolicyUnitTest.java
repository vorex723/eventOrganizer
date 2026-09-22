package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.config.properties.NotificationProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationRetryPolicyUnitTest {

    private final NotificationRetryPolicy retryPolicy =
            new NotificationRetryPolicy(new NotificationProperties());

    @Test
    void returnsConfiguredIncreasingRetryDelays() {
        assertThat(retryPolicy.delayAfterFailedAttempt(1)).isEqualTo(Duration.ofMinutes(1));
        assertThat(retryPolicy.delayAfterFailedAttempt(2)).isEqualTo(Duration.ofMinutes(5));
        assertThat(retryPolicy.delayAfterFailedAttempt(3)).isEqualTo(Duration.ofMinutes(15));
        assertThat(retryPolicy.delayAfterFailedAttempt(4)).isEqualTo(Duration.ofMinutes(30));
        assertThat(retryPolicy.delayAfterFailedAttempt(5)).isEqualTo(Duration.ofMinutes(60));
    }

    @Test
    void marksSixthAttemptAsExhausted() {
        assertThat(retryPolicy.attemptsExhausted(5)).isFalse();
        assertThat(retryPolicy.attemptsExhausted(6)).isTrue();
        assertThatThrownBy(() -> retryPolicy.delayAfterFailedAttempt(6))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
