package com.mazurek.eventOrganizer.auth.email;

import com.mazurek.eventOrganizer.config.properties.MailProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LocalAuthEmailSinkTest {

    @Test
    void recordsStableAuthenticationLinksForLocalDevelopment() {
        MailProperties properties = new MailProperties();
        properties.setActivationBaseUrl("http://localhost:8080/api/v1/auth/activate/");
        properties.setPasswordResetBaseUrl("https://localhost:5173/reset-password?token=");
        properties.setEmailChangeBaseUrl("http://localhost:8080/api/v1/auth/change-email/");
        LocalAuthEmailSink sink = new LocalAuthEmailSink(
                properties,
                Clock.fixed(Instant.parse("2026-09-23T12:00:00Z"), ZoneOffset.UTC)
        );

        sink.record(AuthEmailType.ACCOUNT_ACTIVATION, "person@example.com", "token-value");
        sink.record(AuthEmailType.PASSWORD_RESET, "person@example.com", "token-value");
        sink.record(AuthEmailType.EMAIL_CHANGE_CONFIRMATION, "person@example.com", "token-value");

        assertThat(sink.recent())
                .extracting(LocalAuthEmail::link)
                .containsExactly(
                        "http://localhost:8080/api/v1/auth/change-email/token-value",
                        "https://localhost:5173/reset-password?token=token-value",
                        "http://localhost:8080/api/v1/auth/activate/token-value"
                );
        assertThat(sink.recent())
                .extracting(LocalAuthEmail::recipientEmail)
                .containsOnly("person@example.com");
    }
}
