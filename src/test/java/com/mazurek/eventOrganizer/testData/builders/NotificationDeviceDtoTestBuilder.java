package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.notification.domain.DevicePlatform;
import com.mazurek.eventOrganizer.notification.dto.NotificationDeviceDto;
import java.time.Instant;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.DeliveryFixtureConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.NotificationDeviceConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class NotificationDeviceDtoTestBuilder {
    private UUID id = NotificationDeviceConstants.FIRST_NOTIFICATION_DEVICE_ID;
    private DevicePlatform platform = DeliveryFixtureConstants.PLATFORM;
    private Instant createdAt = NotificationDeviceConstants.FIRST_NOTIFICATION_DEVICE_CREATED_AT;
    private Instant lastSeenAt = NotificationDeviceConstants.FIRST_NOTIFICATION_DEVICE_LAST_SEEN_AT;

    public NotificationDeviceDtoTestBuilder id(UUID id) {
        this.id = id;
        return this;
    }

    public NotificationDeviceDtoTestBuilder platform(DevicePlatform platform) {
        this.platform = platform;
        return this;
    }

    public NotificationDeviceDtoTestBuilder createdAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public NotificationDeviceDtoTestBuilder lastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
        return this;
    }


    public NotificationDeviceDto build() {
        return new NotificationDeviceDto(
                id,
                platform,
                createdAt,
                lastSeenAt
        );
    }
}
