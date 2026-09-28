package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.config.properties.MailProperties;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmtpNotificationEmailClientUnitTest {

    @Mock
    private JavaMailSender javaMailSender;

    private SmtpNotificationEmailClient client;

    @BeforeEach
    void setUp() {
        MailProperties mailProperties = new MailProperties();
        mailProperties.setFromAddress("notifications@example.com");
        client = new SmtpNotificationEmailClient(javaMailSender, mailProperties);
    }

    @Test
    void createsUtf8PlainTextMessage() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(message);

        NotificationSendResult result = client.send(
                "recipient@example.com",
                "Event updated",
                "New event details\nhttps://localhost:5173/events/1"
        );

        assertThat(result.outcome()).isEqualTo(NotificationSendOutcome.SENT);
        assertThat(message.getSubject()).isEqualTo("Event updated");
        assertThat(message.getFrom()[0].toString()).isEqualTo("notifications@example.com");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("recipient@example.com");
        assertThat(message.getContent()).isEqualTo("New event details\nhttps://localhost:5173/events/1");
        verify(javaMailSender).send(message);
    }

    @Test
    void mapsAuthenticationFailureToPermanentFailure() {
        when(javaMailSender.createMimeMessage())
                .thenThrow(new MailAuthenticationException("bad credentials"));

        NotificationSendResult result = client.send("recipient@example.com", "Title", "Body");

        assertThat(result.outcome()).isEqualTo(NotificationSendOutcome.PERMANENT_FAILURE);
    }

    @Test
    void mapsTemporaryMailFailureToRetryableFailure() {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(message);
        org.mockito.Mockito.doThrow(new MailSendException("SMTP is unavailable"))
                .when(javaMailSender)
                .send(message);

        NotificationSendResult result = client.send("recipient@example.com", "Title", "Body");

        assertThat(result.outcome()).isEqualTo(NotificationSendOutcome.RETRYABLE_FAILURE);
    }
}
