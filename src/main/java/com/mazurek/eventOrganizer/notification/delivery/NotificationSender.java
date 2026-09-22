package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;

public interface NotificationSender {
    NotificationChannel supportedChannel();
    NotificationSendResult send(Notification notification);
}
