package com.mazurek.eventOrganizer.validators;

import com.mazurek.eventOrganizer.config.properties.CommunityProperties;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ValidEventCapacityValidatorUnitTest {

    private ValidEventCapacityValidator validator;

    @BeforeEach
    void setUp() {
        CommunityProperties properties = new CommunityProperties();
        properties.setMaxAttendees(1_000);
        validator = new ValidEventCapacityValidator(properties);
    }

    @Test
    void acceptsUnlimitedAndConfiguredFiniteCapacities() {
        assertThat(validator.isValid(null, mock(ConstraintValidatorContext.class))).isTrue();
        assertThat(validator.isValid(1, mock(ConstraintValidatorContext.class))).isTrue();
        assertThat(validator.isValid(1_000, mock(ConstraintValidatorContext.class))).isTrue();
    }

    @Test
    void rejectsFiniteCapacitiesOutsideTheConfiguredRange() {
        assertThat(validator.isValid(0, mock(ConstraintValidatorContext.class))).isFalse();
        assertThat(validator.isValid(1_001, mock(ConstraintValidatorContext.class))).isFalse();
    }
}
