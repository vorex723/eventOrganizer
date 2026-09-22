package com.mazurek.eventOrganizer.notification.delivery;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Profile({"local", "test"})
public class NotificationEmailClientTestImpl implements NotificationEmailClient {

    private final AtomicReference<NotificationSendResult> result =
            new AtomicReference<>(NotificationSendResult.sent(null));

    @Override
    public NotificationSendResult send(String recipientEmail, String title, String body) {
        return result.get();
    }

    public void configureResult(NotificationSendResult notificationSendResult) {
        result.set(Objects.requireNonNull(notificationSendResult));
    }

    public void reset() {
        result.set(NotificationSendResult.sent(null));
    }
}
