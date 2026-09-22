package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.config.properties.MailProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Profile("production")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notifications.email", name = "enabled", havingValue = "true")
public class NotificationEmailConfigurationValidator {

    private final MailProperties mailProperties;
    private final Environment environment;

    @PostConstruct
    void validate() {
        if (!StringUtils.hasText(mailProperties.getFromAddress())
                || !StringUtils.hasText(environment.getProperty("spring.mail.host"))) {
            throw new IllegalStateException(
                    "Email notifications require app.mail.from-address and spring.mail.host."
            );
        }
    }
}
