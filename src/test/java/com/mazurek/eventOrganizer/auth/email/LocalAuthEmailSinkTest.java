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
        properties.setActivationBaseUrl("https://localhost:5173/activate-account?token=");
        properties.setPasswordResetBaseUrl("https://localhost:5173/reset-password?token=");
        properties.setEmailChangeBaseUrl("https://localhost:5173/confirm-email-change?token=");
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
                        "https://localhost:5173/confirm-email-change?token=token-value",
                        "https://localhost:5173/reset-password?token=token-value",
                        "https://localhost:5173/activate-account?token=token-value"
                );
        assertThat(sink.recent())
                .extracting(LocalAuthEmail::recipientEmail)
                .containsOnly("person@example.com");
    }
}
