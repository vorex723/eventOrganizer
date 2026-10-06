package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.testData.builders.NotificationDeliveryTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.NotificationSendResultTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.NotificationTestBuilder;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationDelivery;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.utils.NotificationResourceLinkResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailNotificationSenderUnitTest contracts:")
class EmailNotificationSenderUnitTest {

    @Mock
    private NotificationEmailClient notificationEmailClient;
    @Mock
    private NotificationResourceLinkResolver notificationResourceLinkResolver;
    @InjectMocks
    private EmailNotificationSender sender;

    @Test
    void whenSendingEmailShouldUseSnapshottedAddressAndResourceLink() {
        Notification notification = notification();
        NotificationDelivery delivery = delivery(notification, "original@example.com");
        String link = "https://localhost:5173/events/" + notification.getResourceId();
        NotificationSendResult expected = new NotificationSendResultTestBuilder()
                .outcome(NotificationSendOutcome.SENT)
                .providerMessageId("smtp-id")
                .errorMessage(null)
                .build();
        when(notificationResourceLinkResolver.resolve(notification)).thenReturn(link);
        when(notificationEmailClient.send(
                "original@example.com",
                notification.getTitle(),
                com.mazurek.eventOrganizer.testData.TestConstants.NotificationTemplateConstants.PRIVATE_MESSAGE_BODY + "\n\nOpen details:\n" + link
        )).thenReturn(expected);

        NotificationSendResult result = sender.send(delivery);

        assertThat(sender.supportedChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(result).isEqualTo(expected);
        verify(notificationEmailClient).send(
                "original@example.com",
                notification.getTitle(),
                com.mazurek.eventOrganizer.testData.TestConstants.NotificationTemplateConstants.PRIVATE_MESSAGE_BODY + "\n\nOpen details:\n" + link
        );
    }

    @Test
    void whenEmailSnapshotIsMissingShouldFailWithoutSending() {
        NotificationDelivery delivery = delivery(notification(), null);

        NotificationSendResult result = sender.send(delivery);

        assertThat(result).isNotNull();
        assertThat(result.outcome()).isEqualTo(NotificationSendOutcome.PERMANENT_FAILURE);
        assertThat(result.errorMessage()).isEqualTo("Notification email delivery has no target snapshot.");
        verifyNoInteractions(notificationEmailClient, notificationResourceLinkResolver);
    }

    private static NotificationDelivery delivery(Notification notification, String targetEmail) {
        return new NotificationDeliveryTestBuilder().id(null)
                .notification(notification)
                .channel(NotificationChannel.EMAIL)
                .targetEmail(targetEmail)
                .targetKey(null)
                .status(null)
                .createdAt(null)
                .build();
    }

    private static Notification notification() {
        return new NotificationTestBuilder()
                .resourceType(NotificationResourceType.EVENT)
                .resourceId(UUID.randomUUID())
                .build();
    }
}
