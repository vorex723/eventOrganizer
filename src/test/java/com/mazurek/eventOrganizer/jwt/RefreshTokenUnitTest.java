package com.mazurek.eventOrganizer.jwt;

import com.mazurek.eventOrganizer.testData.builders.RefreshTokenTestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RefreshToken unit tests:")
class RefreshTokenUnitTest {

    @ParameterizedTest(name = "expiration offset {0} ns: expired={1}")
    @CsvSource({"-1, false", "0, true", "1, true"})
    void whenCheckingExpirationShouldUseInclusiveBoundary(long offsetNanos, boolean expectedExpired) {
        RefreshToken token = new RefreshTokenTestBuilder()
                .id(null)
                .rawToken(null)
                .user(null)
                .expiryDate(TimeConstants.NOW)
                .deviceType(null)
                .createdAt(null)
                .lastUsedAt(null)
                .build();

        assertThat(token.isExpired(TimeConstants.NOW.plusNanos(offsetNanos))).isEqualTo(expectedExpired);
    }
}
