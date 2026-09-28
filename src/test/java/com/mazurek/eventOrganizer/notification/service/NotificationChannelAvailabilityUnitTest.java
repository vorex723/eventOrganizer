package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.config.properties.FirebaseProperties;
import com.mazurek.eventOrganizer.config.properties.NotificationProperties;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationChannelAvailabilityUnitTest {

    @Test
    void disabledFirebaseMakesBothPushChannelsUnavailable() {
        FirebaseProperties firebaseProperties = new FirebaseProperties();
        NotificationProperties notificationProperties = new NotificationProperties();
        NotificationChannelAvailability availability = new NotificationChannelAvailability(
                firebaseProperties,
                notificationProperties
        );

        assertThat(availability.isAvailable(NotificationChannel.PUSH_MOBILE)).isFalse();
        assertThat(availability.isAvailable(NotificationChannel.PUSH_WEB)).isFalse();
        assertThat(availability.isAvailable(NotificationChannel.EMAIL)).isFalse();
    }

    @Test
    void enabledFirebaseExposesPushWhileDeferredEmailRemainsUnavailable() {
        FirebaseProperties firebaseProperties = new FirebaseProperties();
        firebaseProperties.setEnabled(true);
        NotificationProperties notificationProperties = new NotificationProperties();
        NotificationChannelAvailability availability = new NotificationChannelAvailability(
                firebaseProperties,
                notificationProperties
        );

        assertThat(availability.isAvailable(NotificationChannel.PUSH_MOBILE)).isTrue();
        assertThat(availability.isAvailable(NotificationChannel.PUSH_WEB)).isTrue();
        assertThat(availability.isAvailable(NotificationChannel.EMAIL)).isFalse();
    }
}
