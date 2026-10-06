package com.mazurek.eventOrganizer.validators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MinutePrecisionValidatorUnitTest contracts:")
class MinutePrecisionValidatorUnitTest {

    private static final Instant MINUTE_START = com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants.NOW
            .truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
    private final MinutePrecisionValidator validator = new MinutePrecisionValidator();

    @Test
    void whenInstantIsMinuteAlignedShouldAcceptIt() {
        assertThat(validator.isValid(MINUTE_START, null)).isTrue();
    }

    @Test
    void whenInstantHasSecondsOrNanosecondsShouldRejectIt() {
        assertThat(validator.isValid(MINUTE_START.plusSeconds(1), null)).isFalse();
        assertThat(validator.isValid(MINUTE_START.plusNanos(1), null)).isFalse();
    }
}
