package com.mazurek.eventOrganizer.notification.delivery;

public interface NotificationEmailClient {
    NotificationSendResult send(String recipientEmail, String title, String body);
}
