package com.mazurek.eventOrganizer.validators;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MinutePrecisionValidatorUnitTest {

    private final MinutePrecisionValidator validator = new MinutePrecisionValidator();

    @Test
    void acceptsMinuteAlignedInstants() {
        assertThat(validator.isValid(Instant.parse("2026-09-23T10:00:00Z"), null)).isTrue();
    }

    @Test
    void rejectsSecondsAndNanoseconds() {
        assertThat(validator.isValid(Instant.parse("2026-09-23T10:00:01Z"), null)).isFalse();
        assertThat(validator.isValid(Instant.parse("2026-09-23T10:00:00.000000001Z"), null)).isFalse();
    }
}
