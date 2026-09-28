package com.mazurek.eventOrganizer.config;

import com.mazurek.eventOrganizer.config.properties.FrontendProperties;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FirebaseNotificationConfigurationValidatorUnitTest {

    @Test
    void rejectsNonHttpsFrontendUrlWhenFirebaseIsEnabled() {
        FrontendProperties frontendProperties = new FrontendProperties();
        frontendProperties.setUrl(URI.create("http://localhost:5173"));

        assertThatThrownBy(() -> new FirebaseNotificationConfigurationValidator(
                frontendProperties
        ).validate()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void acceptsAbsoluteHttpsFrontendUrl() {
        FrontendProperties frontendProperties = new FrontendProperties();
        frontendProperties.setUrl(URI.create("https://localhost:5173"));

        new FirebaseNotificationConfigurationValidator(frontendProperties).validate();
    }
}
