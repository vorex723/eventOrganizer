package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationDelivery;
import com.mazurek.eventOrganizer.utils.NotificationResourceLinkResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notifications.email", name = "enabled", havingValue = "true")
public class EmailNotificationSender implements NotificationSender {

    private final NotificationEmailClient emailClient;
    private final NotificationResourceLinkResolver notificationResourceLinkResolver;

    @Override
    public NotificationChannel supportedChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public NotificationSendResult send(NotificationDelivery delivery) {
        if (delivery.getTargetEmail() == null) {
            return NotificationSendResult.permanentFailure(
                    "Notification email delivery has no target snapshot."
            );
        }

        return emailClient.send(
                delivery.getTargetEmail(),
                delivery.getNotification().getTitle(),
                emailBody(delivery.getNotification())
        );
    }

    private String emailBody(Notification notification) {
        return "%s%n%nOpen details:%n%s".formatted(
                notification.getBody(),
                notificationResourceLinkResolver.resolve(notification)
        );
    }
}
