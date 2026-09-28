package com.mazurek.eventOrganizer.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.notifications.devices",
        name = "cleanup-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class NotificationDeviceCleanupWorker {

    private final NotificationDeviceMaintenanceService notificationDeviceMaintenanceService;

    @Scheduled(cron = "${app.notifications.devices.cleanup-cron:0 0 3 * * *}")
    public void removeStaleDevices() {
        int removedDeviceCount = notificationDeviceMaintenanceService.removeStaleDevices();
        if (removedDeviceCount > 0) {
            log.info("Removed {} stale notification devices.", removedDeviceCount);
        }
    }
}
