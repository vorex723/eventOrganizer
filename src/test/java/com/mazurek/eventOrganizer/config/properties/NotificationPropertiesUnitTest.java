package com.mazurek.eventOrganizer.config.properties;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationPropertiesUnitTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void defaultConfigurationIsValid() {
        assertThat(validator.validate(new NotificationProperties())).isEmpty();
    }

    @Test
    void rejectsRetryScheduleThatDoesNotCoverEveryRetry() {
        NotificationProperties properties = new NotificationProperties();
        properties.getDelivery().setMaxAttempts(7);

        assertThat(validator.validate(properties))
                .anyMatch(violation -> violation.getPropertyPath().toString()
                        .equals("delivery.validTimingConfiguration"));
    }

    @Test
    void rejectsNonPositiveDeviceRetention() {
        NotificationProperties properties = new NotificationProperties();
        properties.getDevices().setStaleAfter(Duration.ZERO);

        assertThat(validator.validate(properties))
                .anyMatch(violation -> violation.getPropertyPath().toString()
                        .equals("devices.validStaleAfter"));
    }

    @Test
    void allowsEnablingNotificationEmail() {
        NotificationProperties properties = new NotificationProperties();
        properties.getEmail().setEnabled(true);

        assertThat(validator.validate(properties)).isEmpty();
    }
}
