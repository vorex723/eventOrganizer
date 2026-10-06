package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.testData.builders.NotificationSendResultTestBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Profile("test")
public class TestNotificationEmailClient implements NotificationEmailClient {

    private final AtomicReference<NotificationSendResult> result =
            new AtomicReference<>(new NotificationSendResultTestBuilder()
                    .outcome(NotificationSendOutcome.SENT)
                    .providerMessageId(null)
                    .errorMessage(null)
                    .build());

    @Override
    public NotificationSendResult send(String recipientEmail, String title, String body) {
        return result.get();
    }

    public void configureResult(NotificationSendResult notificationSendResult) {
        result.set(Objects.requireNonNull(notificationSendResult));
    }

    public void reset() {
        result.set(new NotificationSendResultTestBuilder()
                .outcome(NotificationSendOutcome.SENT)
                .providerMessageId(null)
                .errorMessage(null)
                .build());
    }
}
