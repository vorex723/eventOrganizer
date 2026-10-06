package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.config.properties.FirebaseProperties;

import static com.mazurek.eventOrganizer.testData.TestConstants.PropertyFixtureConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class FirebasePropertiesTestBuilder {
    private boolean enabled = PropertyFixtureConstants.DISABLED;
    private String serviceAccountLocation = PropertyFixtureConstants.SERVICE_ACCOUNT_LOCATION;

    public FirebasePropertiesTestBuilder enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public FirebasePropertiesTestBuilder serviceAccountLocation(String serviceAccountLocation) {
        this.serviceAccountLocation = serviceAccountLocation;
        return this;
    }

    public static FirebaseProperties copyOf(FirebaseProperties source) {
        if (source == null) return null;
        return new FirebasePropertiesTestBuilder()
                .enabled(source.isEnabled())
                .serviceAccountLocation(source.getServiceAccountLocation())
                .build();
    }


    public FirebaseProperties build() {
        FirebaseProperties value = new FirebaseProperties();
        value.setEnabled(enabled);
        value.setServiceAccountLocation(serviceAccountLocation);
        return value;
    }
}
