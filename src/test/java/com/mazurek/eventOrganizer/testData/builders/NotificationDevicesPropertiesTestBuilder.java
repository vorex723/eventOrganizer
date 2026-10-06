package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.config.properties.NotificationProperties.Devices;
import java.time.Duration;

import static com.mazurek.eventOrganizer.testData.TestConstants.PropertyFixtureConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class NotificationDevicesPropertiesTestBuilder {
    private boolean cleanupEnabled = PropertyFixtureConstants.ENABLED;
    private Duration staleAfter = PropertyFixtureConstants.RETENTION;
    private String cleanupCron = PropertyFixtureConstants.DEVICE_CLEANUP_CRON;

    public NotificationDevicesPropertiesTestBuilder cleanupEnabled(boolean cleanupEnabled) {
        this.cleanupEnabled = cleanupEnabled;
        return this;
    }

    public NotificationDevicesPropertiesTestBuilder staleAfter(Duration staleAfter) {
        this.staleAfter = staleAfter;
        return this;
    }

    public NotificationDevicesPropertiesTestBuilder cleanupCron(String cleanupCron) {
        this.cleanupCron = cleanupCron;
        return this;
    }

    public static Devices copyOf(Devices source) {
        if (source == null) return null;
        return new NotificationDevicesPropertiesTestBuilder()
                .cleanupEnabled(source.isCleanupEnabled())
                .staleAfter(source.getStaleAfter())
                .cleanupCron(source.getCleanupCron())
                .build();
    }


    public Devices build() {
        Devices value = new Devices();
        value.setCleanupEnabled(cleanupEnabled);
        value.setStaleAfter(staleAfter);
        value.setCleanupCron(cleanupCron);
        return value;
    }
}
