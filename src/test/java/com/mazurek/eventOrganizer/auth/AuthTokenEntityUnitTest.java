package com.mazurek.eventOrganizer.auth;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.mazurek.eventOrganizer.testData.TestConstants.ActivationTokenConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;
import static org.assertj.core.api.Assertions.assertThat;

class AuthTokenEntityUnitTest {
    private static final long EXPIRATION_MILLIS = 60_000;

    @ParameterizedTest(name = "{0}: stores only hash and honors inclusive expiry")
    @MethodSource("tokenFixtures")
    void issuanceStoresHashAndReissuanceInvalidatesThePreviousToken(TokenFixture fixture) {
        UUID original = ActivationTokenConstants.FIRST_ACTIVATION_TOKEN_UUID;
        UUID replacement = ActivationTokenConstants.SECOND_ACTIVATION_TOKEN_UUID;
        Instant now = TimeConstants.NOW;
        Instant expiration = now.plusMillis(EXPIRATION_MILLIS);

        fixture.issue().accept(original, now);

        assertThat(fixture.hash().get()).isEqualTo(AuthTokenHash.sha256(original)).hasSize(64);
        assertThat(fixture.expiration().get()).isEqualTo(expiration);
        assertThat(fixture.expired().test(expiration.minusNanos(1))).isFalse();
        assertThat(fixture.expired().test(expiration)).isTrue();
        assertThat(fixture.expired().test(expiration.plusNanos(1))).isTrue();

        fixture.issue().accept(replacement, now.plusSeconds(1));

        assertThat(fixture.hash().get()).isEqualTo(AuthTokenHash.sha256(replacement))
                .isNotEqualTo(AuthTokenHash.sha256(original));
        assertThat(fixture.expiration().get()).isEqualTo(expiration.plusSeconds(1));
    }

    @ParameterizedTest(name = "{0}: exposes no raw token state or builder API")
    @MethodSource("tokenFixtures")
    void entitiesCannotRetainOrExposeRawTokens(TokenFixture fixture) throws Exception {
        Class<?> entityType = fixture.entity().getClass();
        assertThat(Arrays.stream(entityType.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers())).toList())
                .noneMatch(field -> field.getType().equals(UUID.class) || field.getName().equals("token"));
        assertThat(Arrays.stream(entityType.getMethods()).map(method -> method.getName()).toList())
                .doesNotContain("getToken", "setToken", "regenerate");
        Class<?> builderType = entityType.getMethod("builder").getReturnType();
        assertThat(Arrays.stream(builderType.getMethods()).map(method -> method.getName()).toList())
                .doesNotContain("token");
    }

    private static Stream<TokenFixture> tokenFixtures() {
        ActivationToken activation = new ActivationToken();
        PasswordResetToken reset = new PasswordResetToken();
        EmailChangeToken emailChange = new EmailChangeToken();
        return Stream.of(
                new TokenFixture(activation,
                        (rawToken, now) -> activation.issue(rawToken, EXPIRATION_MILLIS, now),
                        activation::getTokenHash, activation::getExpirationDate, activation::isExpired),
                new TokenFixture(reset,
                        (rawToken, now) -> reset.issue(rawToken, EXPIRATION_MILLIS, now),
                        reset::getTokenHash, reset::getExpirationDate, reset::isExpired),
                new TokenFixture(emailChange,
                        (rawToken, now) -> emailChange.issue(rawToken, UserConstants.SECOND_USER_EMAIL, EXPIRATION_MILLIS, now),
                        emailChange::getTokenHash, emailChange::getExpirationDate, emailChange::isExpired)
        );
    }

    private record TokenFixture(Object entity, BiConsumer<UUID, Instant> issue, Supplier<String> hash,
                                Supplier<Instant> expiration, Predicate<Instant> expired) {
        @Override
        public String toString() {
            return entity.getClass().getSimpleName();
        }
    }
}
