package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.config.properties.NotificationProperties;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeviceRepository;
import com.mazurek.eventOrganizer.testData.builders.NotificationDevicesPropertiesTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.NotificationPropertiesTestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("NotificationDeviceMaintenanceServiceUnitTest contracts:")
class NotificationDeviceMaintenanceServiceUnitTest {

    @Test
    void whenRemovingStaleDevicesShouldUseConfiguredThreshold() {
        Instant now = com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants.NOW;
        NotificationProperties properties = new NotificationPropertiesTestBuilder()
                .devices(new NotificationDevicesPropertiesTestBuilder()
                        .staleAfter(Duration.ofDays(30))
                        .build())
                .build();
        NotificationDeviceRepository repository = mock(NotificationDeviceRepository.class);
        Instant cutoff = now.minus(Duration.ofDays(30));
        when(repository.deleteAllStaleBefore(cutoff)).thenReturn(3);
        NotificationDeviceMaintenanceService service = new NotificationDeviceMaintenanceService(
                com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants.FIXED_CLOCK,
                properties,
                repository
        );

        int removed = service.removeStaleDevices();

        assertThat(removed).isEqualTo(3);
        verify(repository).deleteAllStaleBefore(cutoff);
    }
}
