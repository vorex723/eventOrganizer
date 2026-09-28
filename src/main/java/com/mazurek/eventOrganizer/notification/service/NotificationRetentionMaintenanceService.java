package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.config.properties.NotificationProperties;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeliveryRepository;
import com.mazurek.eventOrganizer.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class NotificationRetentionMaintenanceService {

    private final Clock clock;
    private final NotificationProperties notificationProperties;
    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository notificationDeliveryRepository;

    @Transactional
    public NotificationRetentionResult cleanup() {
        Instant now = clock.instant();
        NotificationProperties.Retention settings = notificationProperties.getRetention();
        Instant deadBefore = now.minus(settings.getDeadFor());
        int removedNotifications = notificationRepository.deleteCompletedBefore(
                now.minus(settings.getCompletedFor()), deadBefore
        );
        int removedDeadDeliveries = notificationDeliveryRepository.deleteDeadBefore(deadBefore);
        return new NotificationRetentionResult(removedNotifications, removedDeadDeliveries);
    }

    @Transactional(readOnly = true)
    public long dueDeliveryCount() {
        return notificationDeliveryRepository.countDueForProcessing(clock.instant());
    }

    public record NotificationRetentionResult(int removedNotifications, int removedDeadDeliveries) {
    }
}
