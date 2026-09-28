package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.config.properties.NotificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.notifications.retention",
        name = "cleanup-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class NotificationRetentionWorker {

    private final NotificationRetentionMaintenanceService notificationRetentionMaintenanceService;
    private final NotificationProperties notificationProperties;

    @Scheduled(cron = "${app.notifications.retention.cleanup-cron:0 15 3 * * *}")
    public void maintainRetentionAndReportBacklog() {
        NotificationRetentionMaintenanceService.NotificationRetentionResult result =
                notificationRetentionMaintenanceService.cleanup();
        if (result.removedNotifications() > 0 || result.removedDeadDeliveries() > 0) {
            log.info("Notification retention cleanup removed notifications={}, deadDeliveries={}.",
                    result.removedNotifications(), result.removedDeadDeliveries());
        }

        long dueDeliveryCount = notificationRetentionMaintenanceService.dueDeliveryCount();
        if (dueDeliveryCount >= notificationProperties.getRetention().getBacklogAlertThreshold()) {
            log.error("Notification delivery backlog threshold exceeded: dueDeliveries={}, threshold={}.",
                    dueDeliveryCount, notificationProperties.getRetention().getBacklogAlertThreshold());
        }
    }
}
