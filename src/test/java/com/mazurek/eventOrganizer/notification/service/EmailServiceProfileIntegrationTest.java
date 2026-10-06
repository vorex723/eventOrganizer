package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryService;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("Email service profile integration tests:")
class EmailServiceProfileIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EmailServiceConfiguration.class)
            .withBean(UserRepository.class, () -> mock(UserRepository.class))
            .withBean(AuthEmailDeliveryService.class, () -> mock(AuthEmailDeliveryService.class));

    @ParameterizedTest(name = "[{index}] profile={0}")
    @ValueSource(strings = {"local", "production", "test"})
    void whenProfileIsSelectedShouldWireOnlyItsEmailService(String profile) {
        contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles(profile))
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(EmailService.class);
                    if (profile.equals("test")) {
                        assertThat(context)
                                .hasSingleBean(RecordingEmailService.class);
                    } else {
                        assertThat(context)
                                .hasSingleBean(EmailServiceImpl.class)
                                .doesNotHaveBean(RecordingEmailService.class);
                    }
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({EmailServiceImpl.class, RecordingEmailService.class})
    static class EmailServiceConfiguration {
    }
}
