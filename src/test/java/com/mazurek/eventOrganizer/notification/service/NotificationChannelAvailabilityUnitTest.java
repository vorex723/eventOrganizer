package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.config.properties.FirebaseProperties;
import com.mazurek.eventOrganizer.config.properties.NotificationProperties;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.testData.builders.FirebasePropertiesTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.NotificationPropertiesTestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationChannelAvailabilityUnitTest contracts:")
class NotificationChannelAvailabilityUnitTest {

    @Test
    void whenFirebaseIsDisabledShouldMakeBothPushChannelsUnavailable() {
        FirebaseProperties firebaseProperties = new FirebasePropertiesTestBuilder()
                .enabled(false)
                .serviceAccountLocation(null)
                .build();
        NotificationProperties notificationProperties = new NotificationPropertiesTestBuilder().build();
        NotificationChannelAvailability availability = new NotificationChannelAvailability(
                firebaseProperties,
                notificationProperties
        );

        assertThat(availability.isAvailable(NotificationChannel.PUSH_MOBILE)).isFalse();
        assertThat(availability.isAvailable(NotificationChannel.PUSH_WEB)).isFalse();
        assertThat(availability.isAvailable(NotificationChannel.EMAIL)).isFalse();
    }

    @Test
    void whenFirebaseIsEnabledShouldExposePushWithoutDeferredEmail() {
        FirebaseProperties firebaseProperties = new FirebasePropertiesTestBuilder()
                .enabled(true)
                .serviceAccountLocation(null)
                .build();
        NotificationProperties notificationProperties = new NotificationPropertiesTestBuilder().build();
        NotificationChannelAvailability availability = new NotificationChannelAvailability(
                firebaseProperties,
                notificationProperties
        );

        assertThat(availability.isAvailable(NotificationChannel.PUSH_MOBILE)).isTrue();
        assertThat(availability.isAvailable(NotificationChannel.PUSH_WEB)).isTrue();
        assertThat(availability.isAvailable(NotificationChannel.EMAIL)).isFalse();
    }
}
