package com.mazurek.eventOrganizer.validators;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.mazurek.eventOrganizer.testData.TestConstants.InvalidInputConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("ValidTimeZoneValidator unit tests:")
class ValidTimeZoneValidatorUnitTest {

    private ValidTimeZoneValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ValidTimeZoneValidator();
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
        @DisplayName("When value is blank should return true")
        void whenValueIsBlankShouldReturnTrue() {
            boolean result = validator.isValid(InvalidInputConstants.BLANK_VALUE, mock(ConstraintValidatorContext.class));

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("When value is valid time zone should return true")
        void whenValueIsValidTimeZoneShouldReturnTrue() {
            boolean result = validator.isValid(UserConstants.FIRST_USER_TIMEZONE, mock(ConstraintValidatorContext.class));

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("When value is invalid time zone should return false")
        void whenValueIsInvalidTimeZoneShouldReturnFalse() {
            boolean result = validator.isValid(InvalidInputConstants.INVALID_TIME_ZONE, mock(ConstraintValidatorContext.class));

            assertThat(result).isFalse();
        }
    }
}
