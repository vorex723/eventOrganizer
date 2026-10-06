package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationPreference;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.DeliveryFixtureConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class NotificationPreferenceTestBuilder {
    private UUID id = DeliveryFixtureConstants.PREFERENCE_ID;
    private UUID userId = UserConstants.FIRST_USER_ID;
    private NotificationResourceType resourceType = DeliveryFixtureConstants.RESOURCE_TYPE;
    private NotificationChannel channel = DeliveryFixtureConstants.CHANNEL;
    private boolean enabled = DeliveryFixtureConstants.ENABLED;

    public NotificationPreferenceTestBuilder id(UUID id) {
        this.id = id;
        return this;
    }

    public NotificationPreferenceTestBuilder userId(UUID userId) {
        this.userId = userId;
        return this;
    }

    public NotificationPreferenceTestBuilder resourceType(NotificationResourceType resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    public NotificationPreferenceTestBuilder channel(NotificationChannel channel) {
        this.channel = channel;
        return this;
    }

    public NotificationPreferenceTestBuilder enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }


    public NotificationPreference build() {
        NotificationPreference value = new NotificationPreference();
        value.setId(id);
        value.setUserId(userId);
        value.setResourceType(resourceType);
        value.setChannel(channel);
        value.setEnabled(enabled);
        return value;
    }
}
