package com.mazurek.eventOrganizer.auth.email;

import com.mazurek.eventOrganizer.config.properties.MailProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Profile("production")
@RequiredArgsConstructor
public class AuthEmailConfigurationValidator {

    private final MailProperties mailProperties;
    private final Environment environment;

    @PostConstruct
    void validate() {
        if (!StringUtils.hasText(environment.getProperty("spring.mail.host"))
                || !StringUtils.hasText(environment.getProperty("spring.mail.username"))
                || !StringUtils.hasText(environment.getProperty("spring.mail.password"))
                || !StringUtils.hasText(mailProperties.getFromAddress())
                || !StringUtils.hasText(mailProperties.getActivationBaseUrl())
                || !StringUtils.hasText(mailProperties.getPasswordResetBaseUrl())
                || !StringUtils.hasText(mailProperties.getEmailChangeBaseUrl())) {
            throw new IllegalStateException(
                    "Auth emails require complete SMTP, sender, activation-link, password-reset-link, and email-change-link configuration."
            );
        }
    }
}
