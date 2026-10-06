package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.notification.dto.UpdateNotificationPreferenceDto;

import static com.mazurek.eventOrganizer.testData.TestConstants.DeliveryFixtureConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class UpdateNotificationPreferenceDtoTestBuilder {
    private NotificationResourceType resourceType = DeliveryFixtureConstants.RESOURCE_TYPE;
    private NotificationChannel channel = DeliveryFixtureConstants.CHANNEL;
    private boolean enabled = DeliveryFixtureConstants.ENABLED;

    public UpdateNotificationPreferenceDtoTestBuilder resourceType(NotificationResourceType resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    public UpdateNotificationPreferenceDtoTestBuilder channel(NotificationChannel channel) {
        this.channel = channel;
        return this;
    }

    public UpdateNotificationPreferenceDtoTestBuilder enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }


    public UpdateNotificationPreferenceDto build() {
        return new UpdateNotificationPreferenceDto(
                resourceType,
                channel,
                enabled
        );
    }
}
