package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.config.properties.NotificationProperties;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class NotificationDeviceMaintenanceService {

    private final Clock clock;
    private final NotificationProperties notificationProperties;
    private final NotificationDeviceRepository notificationDeviceRepository;

    @Transactional
    public int removeStaleDevices() {
        Instant staleBefore = clock.instant()
                .minus(notificationProperties.getDevices().getStaleAfter());

        return notificationDeviceRepository.deleteAllStaleBefore(staleBefore);
    }
}
