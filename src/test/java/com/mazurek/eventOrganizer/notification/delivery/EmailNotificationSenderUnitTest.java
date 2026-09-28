package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import com.mazurek.eventOrganizer.utils.NotificationResourceLinkResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailNotificationSenderUnitTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationEmailClient notificationEmailClient;
    @Mock
    private NotificationResourceLinkResolver notificationResourceLinkResolver;
    @InjectMocks
    private EmailNotificationSender sender;

    @Test
    void sendsPlainTextEmailWithTitleAndResourceLink() {
        Notification notification = notification();
        User recipient = UserTestBuilder.firstUser().build();
        String link = "https://localhost:5173/events/" + notification.getResourceId();
        NotificationSendResult expected = NotificationSendResult.sent("smtp-id");
        when(userRepository.findById(notification.getRecipientId())).thenReturn(Optional.of(recipient));
        when(notificationResourceLinkResolver.resolve(notification)).thenReturn(link);
        when(notificationEmailClient.send(
                recipient.getEmail(),
                notification.getTitle(),
                "Body\n\nOpen details:\n" + link
        )).thenReturn(expected);

        NotificationSendResult result = sender.send(notification);

        assertThat(sender.supportedChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(result).isEqualTo(expected);
        verify(notificationEmailClient).send(
                recipient.getEmail(),
                notification.getTitle(),
                "Body\n\nOpen details:\n" + link
        );
    }

    @Test
    void returnsPermanentFailureWhenRecipientNoLongerExists() {
        Notification notification = notification();
        when(userRepository.findById(notification.getRecipientId())).thenReturn(Optional.empty());

        NotificationSendResult result = sender.send(notification);

        assertThat(result.outcome()).isEqualTo(NotificationSendOutcome.PERMANENT_FAILURE);
        assertThat(result.errorMessage()).isEqualTo("Notification email recipient no longer exists.");
        verify(notificationEmailClient, never()).send(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
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
