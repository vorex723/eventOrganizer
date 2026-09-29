package com.mazurek.eventOrganizer.notification.delivery;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalNotificationEmailClient implements NotificationEmailClient {

    @Override
    public NotificationSendResult send(String recipientEmail, String title, String body) {
        return NotificationSendResult.sent(null);
    }
}
