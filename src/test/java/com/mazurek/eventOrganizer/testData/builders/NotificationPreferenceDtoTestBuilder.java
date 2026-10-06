package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.notification.dto.NotificationPreferenceDto;

import static com.mazurek.eventOrganizer.testData.TestConstants.DeliveryFixtureConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class NotificationPreferenceDtoTestBuilder {
    private NotificationResourceType resourceType = DeliveryFixtureConstants.RESOURCE_TYPE;
    private NotificationChannel channel = DeliveryFixtureConstants.CHANNEL;
    private boolean enabled = DeliveryFixtureConstants.ENABLED;

    public NotificationPreferenceDtoTestBuilder resourceType(NotificationResourceType resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    public NotificationPreferenceDtoTestBuilder channel(NotificationChannel channel) {
        this.channel = channel;
        return this;
    }

    public NotificationPreferenceDtoTestBuilder enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }


    public NotificationPreferenceDto build() {
        return new NotificationPreferenceDto(
                resourceType,
                channel,
                enabled
        );
    }
}
