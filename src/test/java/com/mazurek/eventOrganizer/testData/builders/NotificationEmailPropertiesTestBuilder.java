package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.config.properties.NotificationProperties.Email;

import static com.mazurek.eventOrganizer.testData.TestConstants.PropertyFixtureConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class NotificationEmailPropertiesTestBuilder {
    private boolean enabled = PropertyFixtureConstants.DISABLED;

    public NotificationEmailPropertiesTestBuilder enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public static Email copyOf(Email source) {
        if (source == null) return null;
        return new NotificationEmailPropertiesTestBuilder()
                .enabled(source.isEnabled())
                .build();
    }


    public Email build() {
        Email value = new Email();
        value.setEnabled(enabled);
        return value;
    }
}
