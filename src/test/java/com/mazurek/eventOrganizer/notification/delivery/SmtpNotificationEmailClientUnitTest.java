package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.config.properties.MailProperties;
import com.mazurek.eventOrganizer.testData.builders.MailPropertiesTestBuilder;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("SmtpNotificationEmailClientUnitTest contracts:")
class SmtpNotificationEmailClientUnitTest {

    @Mock
    private JavaMailSender javaMailSender;

    private SmtpNotificationEmailClient client;

    @BeforeEach
    void setUp() {
        MailProperties mailProperties = new MailPropertiesTestBuilder()
                .activationBaseUrl(null)
                .passwordResetBaseUrl(null)
                .emailChangeBaseUrl(null)
                .build();
        client = new SmtpNotificationEmailClient(javaMailSender, mailProperties);
    }

    @Test
    void whenSendingSmtpShouldCreateUtf8PlainTextMessage() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(message);

        NotificationSendResult result = client.send(
                "recipient@example.com",
                "Wydarzenie — aktualizacja",
                "Nowe szczegóły\nhttps://localhost:5173/events/1"
        );

        assertThat(result).isNotNull();
        assertThat(result.outcome()).isEqualTo(NotificationSendOutcome.SENT);
        assertThat(message.getSubject()).isEqualTo("Wydarzenie — aktualizacja");
        assertThat(message.getFrom()).hasSize(1);
        assertThat(message.getFrom()[0].toString()).isEqualTo(com.mazurek.eventOrganizer.testData.TestConstants.PropertyFixtureConstants.FROM_ADDRESS);
        assertThat(message.getAllRecipients()).hasSize(1);
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("recipient@example.com");
        assertThat(message.getContent()).isEqualTo("Nowe szczegóły\nhttps://localhost:5173/events/1");
        message.saveChanges();
        assertThat(message.getContentType()).contains("text/plain", "charset=UTF-8");
        verify(javaMailSender).send(message);
    }

    @Test
    void whenSmtpAuthenticationFailsShouldReturnPermanentFailure() {
        when(javaMailSender.createMimeMessage())
                .thenThrow(new MailAuthenticationException("bad credentials"));

        NotificationSendResult result = client.send("recipient@example.com", "Title", "Body");

        assertThat(result.outcome()).isEqualTo(NotificationSendOutcome.PERMANENT_FAILURE);
        assertThat(result.errorMessage()).isEqualTo("SMTP configuration is invalid.");
        org.mockito.Mockito.verify(javaMailSender, org.mockito.Mockito.never()).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
    }

    @Test
    void whenSmtpIsUnavailableShouldReturnRetryableFailure() {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(message);
        org.mockito.Mockito.doThrow(new MailSendException("SMTP is unavailable"))
                .when(javaMailSender)
                .send(message);

        NotificationSendResult result = client.send("recipient@example.com", "Title", "Body");

        assertThat(result.outcome()).isEqualTo(NotificationSendOutcome.RETRYABLE_FAILURE);
        assertThat(result.errorMessage()).isEqualTo("SMTP provider is temporarily unavailable.");
    }
}
