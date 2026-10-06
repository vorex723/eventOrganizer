package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.notification.domain.DevicePlatform;
import com.mazurek.eventOrganizer.notification.dto.RegisterNotificationDeviceDto;

import static com.mazurek.eventOrganizer.testData.TestConstants.DeliveryFixtureConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.NotificationDeviceConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class RegisterNotificationDeviceDtoTestBuilder {
    private DevicePlatform platform = DeliveryFixtureConstants.PLATFORM;
    private String firebaseInstallationId = NotificationDeviceConstants.FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID;

    public RegisterNotificationDeviceDtoTestBuilder platform(DevicePlatform platform) {
        this.platform = platform;
        return this;
    }

    public RegisterNotificationDeviceDtoTestBuilder firebaseInstallationId(String firebaseInstallationId) {
        this.firebaseInstallationId = firebaseInstallationId;
        return this;
    }


    public RegisterNotificationDeviceDto build() {
        return new RegisterNotificationDeviceDto(
                platform,
                firebaseInstallationId
        );
    }
}
