package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.config.properties.FrontendProperties;
import com.mazurek.eventOrganizer.testData.builders.FrontendPropertiesTestBuilder;
import com.mazurek.eventOrganizer.utils.NotificationResourceLinkResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("Email notification sender condition integration tests:")
class EmailNotificationSenderConditionIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EmailSenderTestConfiguration.class);

    @Test
    void whenEmailIsDisabledShouldNotCreateSender() {
        contextRunner
                .withPropertyValues("app.notifications.email.enabled=false")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(EmailNotificationSender.class));
    }

    @Test
    void whenEmailIsDisabledOrUnconfiguredShouldNotRequireDependencies() {
        ApplicationContextRunner senderOnlyRunner = new ApplicationContextRunner()
                .withUserConfiguration(EmailSenderOnlyTestConfiguration.class);

        senderOnlyRunner
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(EmailNotificationSender.class));

        senderOnlyRunner
                .withPropertyValues("app.notifications.email.enabled=false")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(EmailNotificationSender.class));
    }

    @Test
    void whenEmailIsEnabledShouldCreateSender() {
        contextRunner
                .withPropertyValues("app.notifications.email.enabled=true")
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(EmailNotificationSender.class));
    }

    @Test
    void whenEmailIsEnabledWithoutClientShouldRejectContext() {
        new ApplicationContextRunner()
                .withUserConfiguration(EmailSenderOnlyTestConfiguration.class)
                .withPropertyValues("app.notifications.email.enabled=true")
                .withBean(NotificationResourceLinkResolver.class, () -> mock(NotificationResourceLinkResolver.class))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class);
                });
    }

    @Test
    void whenEmailIsEnabledWithoutLinkResolverShouldRejectContext() {
        new ApplicationContextRunner()
                .withUserConfiguration(EmailSenderOnlyTestConfiguration.class)
                .withPropertyValues("app.notifications.email.enabled=true")
                .withBean(NotificationEmailClient.class, () -> mock(NotificationEmailClient.class))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(EmailNotificationSender.class)
    static class EmailSenderTestConfiguration {

        @Bean
        NotificationEmailClient notificationEmailClient() {
            return mock(NotificationEmailClient.class);
        }

        @Bean
        NotificationResourceLinkResolver notificationResourceLinkResolver() {
            FrontendProperties properties = new FrontendPropertiesTestBuilder()
                    .url(URI.create("https://localhost:5173"))
                    .build();
            return new NotificationResourceLinkResolver(properties);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import(EmailNotificationSender.class)
    static class EmailSenderOnlyTestConfiguration {
    }
}
