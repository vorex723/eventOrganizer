package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.notification.domain.DevicePlatform;
import com.mazurek.eventOrganizer.notification.domain.NotificationDevice;
import java.time.Instant;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.DeliveryFixtureConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.NotificationDeviceConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class NotificationDeviceTestBuilder {
    private UUID id = NotificationDeviceConstants.FIRST_NOTIFICATION_DEVICE_ID;
    private UUID userId = UserConstants.FIRST_USER_ID;
    private DevicePlatform platform = DeliveryFixtureConstants.PLATFORM;
    private String firebaseInstallationId = NotificationDeviceConstants.FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID;
    private Instant createdAt = NotificationDeviceConstants.FIRST_NOTIFICATION_DEVICE_CREATED_AT;
    private Instant lastSeenAt = NotificationDeviceConstants.FIRST_NOTIFICATION_DEVICE_LAST_SEEN_AT;

    public NotificationDeviceTestBuilder id(UUID id) {
        this.id = id;
        return this;
    }

    public NotificationDeviceTestBuilder userId(UUID userId) {
        this.userId = userId;
        return this;
    }

    public NotificationDeviceTestBuilder platform(DevicePlatform platform) {
        this.platform = platform;
        return this;
    }

    public NotificationDeviceTestBuilder firebaseInstallationId(String firebaseInstallationId) {
        this.firebaseInstallationId = firebaseInstallationId;
        return this;
    }

    public NotificationDeviceTestBuilder createdAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public NotificationDeviceTestBuilder lastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
        return this;
    }


    public NotificationDevice build() {
        NotificationDevice value = new NotificationDevice();
        value.setId(id);
        value.setUserId(userId);
        value.setPlatform(platform);
        value.setFirebaseInstallationId(firebaseInstallationId);
        value.setCreatedAt(createdAt);
        value.setLastSeenAt(lastSeenAt);
        return value;
    }
}
