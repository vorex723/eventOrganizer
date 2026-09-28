package com.mazurek.eventOrganizer.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.notifications.delivery",
        name = "worker-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class NotificationDeliveryWorker {

    private final NotificationDeliveryService notificationDeliveryService;

    @Scheduled(fixedDelayString = "${app.notifications.delivery.poll-delay:5s}")
    public void processPendingDeliveries() {
        notificationDeliveryService.processPendingDeliveries();
    }
}
