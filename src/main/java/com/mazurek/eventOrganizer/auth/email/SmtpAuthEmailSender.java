package com.mazurek.eventOrganizer.auth.email;

import com.mazurek.eventOrganizer.config.properties.MailProperties;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class SmtpAuthEmailSender implements AuthEmailSender {

    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;

    @Override
    public AuthEmailSendResult send(AuthEmailType type, String recipientEmail, String rawToken) {
        AuthEmailContent content = content(type, rawToken);
        try {
            MimeMessageHelper message = new MimeMessageHelper(
                    javaMailSender.createMimeMessage(),
                    false,
                    StandardCharsets.UTF_8.name()
            );
            message.setTo(recipientEmail);
            message.setFrom(mailProperties.getFromAddress());
            message.setSubject(content.subject());
            message.setText(content.htmlBody(), true);
            javaMailSender.send(message.getMimeMessage());
            return AuthEmailSendResult.sent(null);
        } catch (MailAuthenticationException | MailParseException exception) {
            log.error("Auth email cannot be sent because SMTP configuration is invalid.", exception);
            return AuthEmailSendResult.permanentFailure("SMTP configuration is invalid.");
        } catch (MessagingException | IllegalArgumentException exception) {
            log.warn("Auth email contains invalid message data.", exception);
            return AuthEmailSendResult.permanentFailure("Auth email message is invalid.");
        } catch (MailException exception) {
            log.warn("SMTP provider is temporarily unavailable for auth email.", exception);
            return AuthEmailSendResult.retryableFailure("SMTP provider is temporarily unavailable.");
        }
    }

    private AuthEmailContent content(AuthEmailType type, String rawToken) {
        return switch (type) {
            case ACCOUNT_ACTIVATION -> new AuthEmailContent(
                    "Account activation",
                    "<p><b>You can activate your account by opening this link:</b></p>"
                            + "<p><a href=\"" + mailProperties.getActivationBaseUrl() + rawToken
                            + "\">Activate account.</a></p>"
            );
            case PASSWORD_RESET -> new AuthEmailContent(
                    "Password reset",
                    "<p><b>You can reset your password by opening this link:</b></p>"
                            + "<p><a href=\"" + mailProperties.getPasswordResetBaseUrl() + rawToken
                            + "\">Reset password.</a></p>"
            );
        };
    }

    private record AuthEmailContent(String subject, String htmlBody) {
    }
}
