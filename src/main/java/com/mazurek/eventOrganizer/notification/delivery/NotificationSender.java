package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationDelivery;

public interface NotificationSender {
    NotificationChannel supportedChannel();
    NotificationSendResult send(NotificationDelivery delivery);
}
