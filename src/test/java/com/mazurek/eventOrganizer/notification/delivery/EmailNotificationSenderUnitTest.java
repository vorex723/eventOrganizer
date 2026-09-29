package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationDelivery;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.utils.NotificationResourceLinkResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailNotificationSenderUnitTest {

    @Mock
    private NotificationEmailClient notificationEmailClient;
    @Mock
    private NotificationResourceLinkResolver notificationResourceLinkResolver;
    @InjectMocks
    private EmailNotificationSender sender;

    @Test
    void sendsPlainTextEmailToSnapshottedAddressWithTitleAndResourceLink() {
        Notification notification = notification();
        NotificationDelivery delivery = delivery(notification, "original@example.com");
        String link = "https://localhost:5173/events/" + notification.getResourceId();
        NotificationSendResult expected = NotificationSendResult.sent("smtp-id");
        when(notificationResourceLinkResolver.resolve(notification)).thenReturn(link);
        when(notificationEmailClient.send(
                "original@example.com",
                notification.getTitle(),
                "Body\n\nOpen details:\n" + link
        )).thenReturn(expected);

        NotificationSendResult result = sender.send(delivery);

        assertThat(sender.supportedChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(result).isEqualTo(expected);
        verify(notificationEmailClient).send(
                "original@example.com",
                notification.getTitle(),
                "Body\n\nOpen details:\n" + link
        );
    }

    @Test
    void returnsPermanentFailureWithoutSendingWhenTargetSnapshotIsMissing() {
        NotificationDelivery delivery = delivery(notification(), null);

        NotificationSendResult result = sender.send(delivery);

        assertThat(result.outcome()).isEqualTo(NotificationSendOutcome.PERMANENT_FAILURE);
        assertThat(result.errorMessage()).isEqualTo("Notification email delivery has no target snapshot.");
        verifyNoInteractions(notificationEmailClient, notificationResourceLinkResolver);
    }

    private static NotificationDelivery delivery(Notification notification, String targetEmail) {
        return NotificationDelivery.builder()
                .notification(notification)
                .channel(NotificationChannel.EMAIL)
                .targetEmail(targetEmail)
                .build();
    }

    private static Notification notification() {
        return Notification.builder()
                .id(UUID.randomUUID())
                .recipientId(UUID.randomUUID())
                .title("Title")
                .body("Body")
                .resourceType(NotificationResourceType.EVENT)
                .resourceId(UUID.randomUUID())
                .createdAt(Instant.parse("2026-01-02T03:04:05Z"))
                .build();
    }
}
