package com.mazurek.eventOrganizer.auth.email;

import com.mazurek.eventOrganizer.config.properties.MailProperties;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AuthEmailSenderProfileUnitTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AuthEmailSenderConfiguration.class)
            .withBean(Clock.class, Clock::systemUTC)
            .withBean(JavaMailSender.class, () -> mock(JavaMailSender.class))
            .withBean(MailProperties.class, () -> mock(MailProperties.class));

    @ParameterizedTest
    @ValueSource(strings = {"local", "production", "test"})
    void selectsOnlyTheAuthEmailSenderForTheActiveProfile(String profile) {
        contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles(profile))
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(AuthEmailSender.class);
                    switch (profile) {
                        case "local" -> assertThat(context)
                                .hasSingleBean(LocalAuthEmailSender.class)
                                .doesNotHaveBean(SmtpAuthEmailSender.class)
                                .doesNotHaveBean(TestAuthEmailSender.class);
                        case "production" -> assertThat(context)
                                .hasSingleBean(SmtpAuthEmailSender.class)
                                .doesNotHaveBean(LocalAuthEmailSender.class)
                                .doesNotHaveBean(TestAuthEmailSender.class);
                        case "test" -> assertThat(context)
                                .hasSingleBean(TestAuthEmailSender.class)
                                .doesNotHaveBean(LocalAuthEmailSender.class)
                                .doesNotHaveBean(SmtpAuthEmailSender.class);
                        default -> throw new IllegalArgumentException("Unexpected profile: " + profile);
                    }
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({LocalAuthEmailSender.class, LocalAuthEmailSink.class, SmtpAuthEmailSender.class, TestAuthEmailSender.class})
    static class AuthEmailSenderConfiguration {
    }
}
