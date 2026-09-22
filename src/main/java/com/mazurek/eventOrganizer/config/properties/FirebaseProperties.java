package com.mazurek.eventOrganizer.config.properties;

import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.firebase")
public class FirebaseProperties {

    private boolean enabled;

    private String serviceAccountLocation;

    @AssertTrue(message = "app.firebase.service-account-location is required when Firebase is enabled")
    public boolean isValidServiceAccountLocation() {
        return !enabled || StringUtils.hasText(serviceAccountLocation);
    }
}
