package com.mazurek.eventOrganizer.config;

import com.mazurek.eventOrganizer.config.properties.FrontendProperties;
import com.mazurek.eventOrganizer.testData.builders.FrontendPropertiesTestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FirebaseNotificationConfigurationValidatorUnitTest contracts:")
class FirebaseNotificationConfigurationValidatorUnitTest {

    @Test
    void whenFirebaseUsesNonHttpsFrontendShouldRejectConfiguration() {
        FrontendProperties frontendProperties = new FrontendPropertiesTestBuilder()
                .url(URI.create("http://localhost:5173"))
                .build();

        assertThatThrownBy(() -> new FirebaseNotificationConfigurationValidator(
                frontendProperties
        ).validate()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void whenFrontendUrlIsAbsoluteHttpsShouldAcceptConfiguration() {
        FrontendProperties frontendProperties = new FrontendPropertiesTestBuilder()
                .url(URI.create("https://localhost:5173"))
                .build();

        new FirebaseNotificationConfigurationValidator(frontendProperties).validate();
    }
}
