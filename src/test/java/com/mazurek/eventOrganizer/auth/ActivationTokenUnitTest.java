package com.mazurek.eventOrganizer.auth;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ActivationTokenUnitTest {

    @Test
    void storesOnlyHashForTokenMatching() {
        UUID rawToken = UUID.randomUUID();
        ActivationToken token = new ActivationToken();
        token.issue(rawToken, 60_000, Instant.parse("2026-01-01T00:00:00Z"));

        assertThat(token.getTokenHash()).hasSize(64);
        assertThat(token.matches(rawToken)).isTrue();
        assertThat(token.matches(UUID.randomUUID())).isFalse();
    }

    @Test
    void expiresAtTheConfiguredExpirationInstant() {
        Instant expiration = Instant.parse("2026-01-01T00:00:00Z");
        ActivationToken token = new ActivationToken();
        token.issue(UUID.randomUUID(), 60_000, expiration.minusSeconds(60));

        assertThat(token.isExpired(expiration)).isTrue();
    }
}
