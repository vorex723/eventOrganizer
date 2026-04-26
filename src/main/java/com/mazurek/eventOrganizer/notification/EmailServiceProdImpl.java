package com.mazurek.eventOrganizer.notification;

import com.mazurek.eventOrganizer.config.properties.MailProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Profile("production")
@Slf4j
public class EmailServiceProdImpl implements EmailService {

    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;

    @Override
    public void sendActivationEmail(String userEmail, UUID tokenID) {
        String body =
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<body>" +
                        "<p> <b>You can activate your account by opening this link:</b></p>" +
                        "<p><a href=\"" + mailProperties.getActivationBaseUrl() + tokenID + "\">Activate account.</a></p>" +
                        "</body>" +
                        "</html>";

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper messageHelper = new MimeMessageHelper(message, true);

            message.setContent(body, "text/html; charset=utf-8");
            messageHelper.setTo(userEmail);
            messageHelper.setFrom(mailProperties.getFromAddress());
            messageHelper.setSubject("Account activation.");

            javaMailSender.send(message);
        } catch (Exception exception) {
            log.error("Failed to send activation email to {}", userEmail, exception);
        }
    }
}
