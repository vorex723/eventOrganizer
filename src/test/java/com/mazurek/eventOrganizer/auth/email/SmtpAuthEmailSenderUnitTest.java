package com.mazurek.eventOrganizer.auth.email;

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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmtpAuthEmailSenderUnitTest {

    @Mock
    private JavaMailSender javaMailSender;

    private SmtpAuthEmailSender sender;

    @BeforeEach
    void setUp() {
        MailProperties properties = new MailProperties();
        properties.setFromAddress("no-reply@example.com");
        properties.setActivationBaseUrl("https://api.example.com/api/v1/auth/activate/");
        properties.setPasswordResetBaseUrl("https://app.example.com/reset-password?token=");
        sender = new SmtpAuthEmailSender(javaMailSender, properties);
    }

    @Test
    void sendsActivationEmailWithConfiguredLink() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(message);

        AuthEmailSendResult result = sender.send(
                AuthEmailType.ACCOUNT_ACTIVATION,
                "user@example.com",
                "11111111-1111-4111-8111-111111111111"
        );

        assertThat(result.outcome()).isEqualTo(AuthEmailSendOutcome.SENT);
        assertThat(message.getSubject()).isEqualTo("Account activation");
        assertThat(message.getContent().toString())
                .contains("https://api.example.com/api/v1/auth/activate/11111111-1111-4111-8111-111111111111");
        verify(javaMailSender).send(message);
    }

    @Test
    void sendsPasswordResetEmailWithConfiguredFrontendLink() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(message);

        AuthEmailSendResult result = sender.send(
                AuthEmailType.PASSWORD_RESET,
                "user@example.com",
                "22222222-2222-4222-8222-222222222222"
        );

        assertThat(result.outcome()).isEqualTo(AuthEmailSendOutcome.SENT);
        assertThat(message.getSubject()).isEqualTo("Password reset");
        assertThat(message.getContent().toString())
                .contains("https://app.example.com/reset-password?token=22222222-2222-4222-8222-222222222222");
        verify(javaMailSender).send(message);
    }

    @Test
    void mapsAuthenticationFailureToPermanentFailure() {
        when(javaMailSender.createMimeMessage())
                .thenThrow(new MailAuthenticationException("invalid credentials"));

        AuthEmailSendResult result = sender.send(AuthEmailType.PASSWORD_RESET, "user@example.com", "token");

        assertThat(result.outcome()).isEqualTo(AuthEmailSendOutcome.PERMANENT_FAILURE);
    }

    @Test
    void mapsTransportFailureToRetryableFailure() {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(message);
        doThrow(new MailSendException("SMTP unavailable")).when(javaMailSender).send(message);

        AuthEmailSendResult result = sender.send(AuthEmailType.PASSWORD_RESET, "user@example.com", "token");

        assertThat(result.outcome()).isEqualTo(AuthEmailSendOutcome.RETRYABLE_FAILURE);
    }
}
