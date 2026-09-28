package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.config.properties.NotificationProperties;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeviceRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationDeviceMaintenanceServiceUnitTest {

    @Test
    void removesDevicesOlderThanConfiguredThreshold() {
        Instant now = Instant.parse("2026-01-02T03:04:05Z");
        NotificationProperties properties = new NotificationProperties();
        properties.getDevices().setStaleAfter(Duration.ofDays(30));
        NotificationDeviceRepository repository = mock(NotificationDeviceRepository.class);
        Instant cutoff = now.minus(Duration.ofDays(30));
        when(repository.deleteAllStaleBefore(cutoff)).thenReturn(3);
        NotificationDeviceMaintenanceService service = new NotificationDeviceMaintenanceService(
                Clock.fixed(now, ZoneOffset.UTC),
                properties,
                repository
        );

        int removed = service.removeStaleDevices();

        assertThat(removed).isEqualTo(3);
        verify(repository).deleteAllStaleBefore(cutoff);
    }
}
