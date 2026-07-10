package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.notification.domain.Notification;

import java.util.UUID;

public interface NotificationDeliveryService {
    void createDeliveries(Notification notification);
    void processPendingDeliveries();
    void processDelivery(UUID deliveryId);
}
