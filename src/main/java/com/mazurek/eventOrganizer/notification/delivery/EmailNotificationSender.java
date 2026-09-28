package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationDelivery;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import com.mazurek.eventOrganizer.utils.NotificationResourceLinkResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notifications.email", name = "enabled", havingValue = "true")
public class EmailNotificationSender implements NotificationSender {

    private final UserRepository userRepository;
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

    /** @deprecated Delivery processing must use the immutable target on {@link NotificationDelivery}. */
    @Deprecated
    public NotificationSendResult send(Notification notification) {
        User recipient = userRepository.findById(notification.getRecipientId()).orElse(null);
        if (recipient == null) {
            return NotificationSendResult.permanentFailure("Notification email recipient no longer exists.");
        }
        return emailClient.send(recipient.getEmail(), notification.getTitle(), emailBody(notification));
    }

    private String emailBody(Notification notification) {
        return "%s%n%nOpen details:%n%s".formatted(
                notification.getBody(),
                notificationResourceLinkResolver.resolve(notification)
        );
    }
}
