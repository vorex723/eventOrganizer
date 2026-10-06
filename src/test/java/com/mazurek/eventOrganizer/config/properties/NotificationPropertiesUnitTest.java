package com.mazurek.eventOrganizer.config.properties;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import com.mazurek.eventOrganizer.testData.builders.NotificationPropertiesTestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationPropertiesUnitTest contracts:")
class NotificationPropertiesUnitTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void openValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void whenConfigurationUsesProductionDefaultsShouldBeValid() {
        // Act deliberately exercises production defaults, not a test builder default.
        assertThat(validator.validate(new NotificationProperties())).isEmpty();
    }

    @Test
    void whenRetryScheduleIsIncompleteShouldRejectConfiguration() {
        NotificationProperties properties = new NotificationPropertiesTestBuilder().build();
        properties.getDelivery().setMaxAttempts(7);

        assertThat(validator.validate(properties))
                .anyMatch(violation -> violation.getPropertyPath().toString()
                        .equals("delivery.validTimingConfiguration"));
    }

    @Test
    void whenDeviceRetentionIsNotPositiveShouldRejectConfiguration() {
        NotificationProperties properties = new NotificationPropertiesTestBuilder().build();
        properties.getDevices().setStaleAfter(Duration.ZERO);

        assertThat(validator.validate(properties))
                .anyMatch(violation -> violation.getPropertyPath().toString()
                        .equals("devices.validStaleAfter"));
    }

    @Test
    void whenNotificationEmailIsEnabledShouldAcceptConfiguration() {
        NotificationProperties properties = new NotificationPropertiesTestBuilder().build();
        properties.getEmail().setEnabled(true);

        assertThat(validator.validate(properties)).isEmpty();
    }
}
