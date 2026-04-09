package com.mazurek.eventOrganizer.validators;

import com.mazurek.eventOrganizer.testData.TestConstants;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("MinFutureDateOffsetValidator unit tests:")
class MinFutureDateOffsetValidatorUnitTest {

    private MinFutureDateOffsetValidator validator;

    @BeforeEach
    void setUp() {
        validator = new MinFutureDateOffsetValidator(TestConstants.TimeConstants.FIXED_CLOCK);
        MinFutureDateOffset annotation = mock(MinFutureDateOffset.class);
        when(annotation.hours()).thenReturn(TestConstants.ValidationConstants.THREAD_EDIT_WINDOW_HOURS);
        validator.initialize(annotation);
    }

    @Nested
    @DisplayName("Validation tests:")
    class IsValidTests {

        @Test
        @DisplayName("When value is null should return true")
        void whenValueIsNullShouldReturnTrue() {
            boolean result = validator.isValid(null, mock(ConstraintValidatorContext.class));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("When value is after required offset should return true")
        void whenValueIsAfterRequiredOffsetShouldReturnTrue() {
            Instant validInstant = TestConstants.TimeConstants.NOW.plus(TestConstants.ValidationConstants.THREAD_EDIT_WINDOW_HOURS + 1L, ChronoUnit.HOURS);
            boolean result = validator.isValid(validInstant, mock(ConstraintValidatorContext.class));
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("When value is exactly on required offset should return false")
        void whenValueIsExactlyOnRequiredOffsetShouldReturnFalse() {
            Instant thresholdInstant = TestConstants.TimeConstants.NOW.plus(TestConstants.ValidationConstants.THREAD_EDIT_WINDOW_HOURS, ChronoUnit.HOURS);
            boolean result = validator.isValid(thresholdInstant, mock(ConstraintValidatorContext.class));
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("When value is before required offset should return false")
        void whenValueIsBeforeRequiredOffsetShouldReturnFalse() {
            Instant invalidInstant = TestConstants.TimeConstants.NOW.plus(TestConstants.ValidationConstants.THREAD_EDIT_WINDOW_HOURS - 1L, ChronoUnit.HOURS);
            boolean result = validator.isValid(invalidInstant, mock(ConstraintValidatorContext.class));
            assertThat(result).isFalse();
        }
    }
}
