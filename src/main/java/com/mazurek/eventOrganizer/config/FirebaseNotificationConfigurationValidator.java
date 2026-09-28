package com.mazurek.eventOrganizer.config;

import com.mazurek.eventOrganizer.config.properties.FrontendProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.firebase", name = "enabled", havingValue = "true")
public class FirebaseNotificationConfigurationValidator {

    private final FrontendProperties frontendProperties;

    @PostConstruct
    void validate() {
        if (!frontendProperties.isValidUrl()) {
            throw new IllegalStateException(
                    "app.frontend.url must be an absolute HTTPS URL without a query or fragment when Firebase is enabled."
            );
        }
    }
}
