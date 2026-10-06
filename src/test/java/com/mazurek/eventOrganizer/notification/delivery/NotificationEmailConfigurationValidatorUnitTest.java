package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.config.properties.MailProperties;
import com.mazurek.eventOrganizer.testData.builders.MailPropertiesTestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Notification email configuration validator unit tests:")
class NotificationEmailConfigurationValidatorUnitTest {
    @Test
    void whenEnabledProductionEmailHasNoSmtpHostShouldRejectConfiguration() {
        MailProperties mailProperties = new MailPropertiesTestBuilder()
                .activationBaseUrl(null)
                .passwordResetBaseUrl(null)
                .emailChangeBaseUrl(null)
                .build();

        NotificationEmailConfigurationValidator validator =
                new NotificationEmailConfigurationValidator(mailProperties, new MockEnvironment());

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.mail.host");
    }
}
