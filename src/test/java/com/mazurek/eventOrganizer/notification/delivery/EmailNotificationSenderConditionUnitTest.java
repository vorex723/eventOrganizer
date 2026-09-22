package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.config.properties.FrontendProperties;
import com.mazurek.eventOrganizer.config.properties.MailProperties;
import com.mazurek.eventOrganizer.user.UserRepository;
import com.mazurek.eventOrganizer.utils.NotificationResourceLinkResolver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.env.MockEnvironment;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class EmailNotificationSenderConditionUnitTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EmailSenderTestConfiguration.class);

    @Test
    void doesNotCreateEmailSenderWhenEmailNotificationsAreDisabled() {
        contextRunner
                .withPropertyValues("app.notifications.email.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(EmailNotificationSender.class));
    }

    @Test
    void createsEmailSenderWhenEmailNotificationsAreEnabled() {
        contextRunner
                .withPropertyValues("app.notifications.email.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(EmailNotificationSender.class));
    }

    @Test
    void rejectsEnabledProductionEmailWithoutSmtpHost() {
        MailProperties mailProperties = new MailProperties();
        mailProperties.setFromAddress("notifications@example.com");

        NotificationEmailConfigurationValidator validator =
                new NotificationEmailConfigurationValidator(mailProperties, new MockEnvironment());

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.mail.host");
    }

    @Configuration(proxyBeanMethods = false)
    @Import(EmailNotificationSender.class)
    static class EmailSenderTestConfiguration {

        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        NotificationEmailClient notificationEmailClient() {
            return mock(NotificationEmailClient.class);
        }

        @Bean
        NotificationResourceLinkResolver notificationResourceLinkResolver() {
            FrontendProperties properties = new FrontendProperties();
            properties.setUrl(URI.create("https://localhost:5173"));
            return new NotificationResourceLinkResolver(properties);
        }
    }
}
