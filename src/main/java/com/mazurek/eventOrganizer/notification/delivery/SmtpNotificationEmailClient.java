package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.config.properties.MailProperties;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Profile("production")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.notifications.email", name = "enabled", havingValue = "true")
public class SmtpNotificationEmailClient implements NotificationEmailClient {

    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;

    @Override
    public NotificationSendResult send(String recipientEmail, String title, String body) {
        try {
            MimeMessageHelper message = new MimeMessageHelper(
                    javaMailSender.createMimeMessage(),
                    false,
                    StandardCharsets.UTF_8.name()
            );
            message.setTo(recipientEmail);
            message.setFrom(mailProperties.getFromAddress());
            message.setSubject(title);
            message.setText(body, false);
            javaMailSender.send(message.getMimeMessage());
            return NotificationSendResult.sent(null);
        } catch (MailAuthenticationException | MailParseException exception) {
            log.error("Notification email cannot be sent because SMTP configuration is invalid.", exception);
            return NotificationSendResult.permanentFailure("SMTP configuration is invalid.");
        } catch (MessagingException | IllegalArgumentException exception) {
            log.warn("Notification email contains invalid message data.", exception);
            return NotificationSendResult.permanentFailure("Notification email message is invalid.");
        } catch (MailException exception) {
            log.warn("Notification email provider is temporarily unavailable.", exception);
            return NotificationSendResult.retryableFailure("SMTP provider is temporarily unavailable.");
        }
    }
}
