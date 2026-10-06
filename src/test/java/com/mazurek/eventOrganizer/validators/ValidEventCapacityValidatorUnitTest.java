package com.mazurek.eventOrganizer.validators;

import com.mazurek.eventOrganizer.config.properties.CommunityProperties;
import com.mazurek.eventOrganizer.testData.builders.CommunityPropertiesTestBuilder;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("ValidEventCapacityValidatorUnitTest contracts:")
class ValidEventCapacityValidatorUnitTest {

    private ValidEventCapacityValidator validator;

    @BeforeEach
    void setUp() {
        CommunityProperties properties = new CommunityPropertiesTestBuilder()
                .maxAttendees(1_000)
                .build();
        validator = new ValidEventCapacityValidator(properties);
    }

    @Test
    void whenCapacityIsUnlimitedOrWithinConfiguredRangeShouldAcceptIt() {
        assertThat(validator.isValid(null, mock(ConstraintValidatorContext.class))).isTrue();
        assertThat(validator.isValid(1, mock(ConstraintValidatorContext.class))).isTrue();
        assertThat(validator.isValid(1_000, mock(ConstraintValidatorContext.class))).isTrue();
    }

    @Test
    void whenCapacityIsOutsideConfiguredRangeShouldRejectIt() {
        assertThat(validator.isValid(0, mock(ConstraintValidatorContext.class))).isFalse();
        assertThat(validator.isValid(1_001, mock(ConstraintValidatorContext.class))).isFalse();
    }
}
