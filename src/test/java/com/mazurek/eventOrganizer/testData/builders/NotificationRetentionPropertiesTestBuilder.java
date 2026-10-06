package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.config.properties.NotificationProperties.Retention;
import java.time.Duration;

import static com.mazurek.eventOrganizer.testData.TestConstants.PropertyFixtureConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class NotificationRetentionPropertiesTestBuilder {
    private boolean cleanupEnabled = PropertyFixtureConstants.ENABLED;
    private Duration completedFor = PropertyFixtureConstants.RETENTION;
    private Duration deadFor = PropertyFixtureConstants.DEAD_RETENTION;
    private String cleanupCron = PropertyFixtureConstants.NOTIFICATION_CLEANUP_CRON;
    private int backlogAlertThreshold = PropertyFixtureConstants.BACKLOG_THRESHOLD;

    public NotificationRetentionPropertiesTestBuilder cleanupEnabled(boolean cleanupEnabled) {
        this.cleanupEnabled = cleanupEnabled;
        return this;
    }

    public NotificationRetentionPropertiesTestBuilder completedFor(Duration completedFor) {
        this.completedFor = completedFor;
        return this;
    }

    public NotificationRetentionPropertiesTestBuilder deadFor(Duration deadFor) {
        this.deadFor = deadFor;
        return this;
    }

    public NotificationRetentionPropertiesTestBuilder cleanupCron(String cleanupCron) {
        this.cleanupCron = cleanupCron;
        return this;
    }

    public NotificationRetentionPropertiesTestBuilder backlogAlertThreshold(int backlogAlertThreshold) {
        this.backlogAlertThreshold = backlogAlertThreshold;
        return this;
    }

    public static Retention copyOf(Retention source) {
        if (source == null) return null;
        return new NotificationRetentionPropertiesTestBuilder()
                .cleanupEnabled(source.isCleanupEnabled())
                .completedFor(source.getCompletedFor())
                .deadFor(source.getDeadFor())
                .cleanupCron(source.getCleanupCron())
                .backlogAlertThreshold(source.getBacklogAlertThreshold())
                .build();
    }


    public Retention build() {
        Retention value = new Retention();
        value.setCleanupEnabled(cleanupEnabled);
        value.setCompletedFor(completedFor);
        value.setDeadFor(deadFor);
        value.setCleanupCron(cleanupCron);
        value.setBacklogAlertThreshold(backlogAlertThreshold);
        return value;
    }
}
