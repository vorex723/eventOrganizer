package com.mazurek.eventOrganizer.jwt;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants;
import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenUnitTest {

    @ParameterizedTest(name = "expiration offset {0} ns: expired={1}")
    @CsvSource({"-1, false", "0, true", "1, true"})
    void expirationIsInclusive(long offsetNanos, boolean expectedExpired) {
        RefreshToken token = new RefreshToken();
        token.setExpiryDate(TimeConstants.NOW);

        assertThat(token.isExpired(TimeConstants.NOW.plusNanos(offsetNanos))).isEqualTo(expectedExpired);
    }
}
