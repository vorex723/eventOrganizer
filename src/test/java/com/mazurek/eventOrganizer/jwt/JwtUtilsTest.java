package com.mazurek.eventOrganizer.jwt;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.config.properties.JwtProperties;
import com.mazurek.eventOrganizer.testData.builders.CityTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.RoleTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.User;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtUtils unit tests:")
class JwtUtilsTest {

    private JwtUtils jwtUtils;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private User user;


    private Role roleUser;
    private Role roleAdmin;

    private City cityWarsaw;
    private Clock clock;

    private JwtProperties jwtProperties(long accessExpiration, String secret) {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setAccessExpiration(accessExpiration);
        jwtProperties.setRefreshShortExpiration(JwtConstants.REFRESH_TOKEN_EXPIRATION_SHORT);
        jwtProperties.setRefreshLongExpiration(JwtConstants.REFRESH_TOKEN_EXPIRATION_LONG);
        jwtProperties.setSecret(secret);
        return jwtProperties;
    }

    @BeforeEach
    void setUp() {
        clock = TimeConstants.FIXED_CLOCK;
        roleUser = RoleTestBuilder.userRole().build();
        roleAdmin = RoleTestBuilder.adminRole().build();

        cityWarsaw = CityTestBuilder.warsaw().build();

        Instant userCreateAccountTime = TimeConstants.NOW.minusSeconds(60);
        user = UserTestBuilder.firstUser()
                .roles(Set.of(roleUser))
                .activated(true)
                .banned(false)
                .homeCity(cityWarsaw)
                .password(passwordEncoder.encode(UserConstants.USER_PASSWORD))
                .createdAt(userCreateAccountTime)
                .lastCredentialsChangeTime(userCreateAccountTime)
                .build();

        jwtUtils = new JwtUtils(jwtProperties(JwtConstants.ACCESS_TOKEN_EXPIRATION_30_SECONDS, JwtConstants.TEST_SECRET_BASE64), clock);
    }

    @Nested
    @DisplayName("Generate access token tests:")
    class GenerateAccessTokenTests {

        @Test
        @DisplayName("When generating access token should generate valid non-empty token")
        void whenGeneratingAccessTokenShouldGenerateValidNonEmptyToken() {
            String token = jwtUtils.generateAccessToken(user);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(token).isNotBlank();
                softly.assertThat(jwtUtils.isTokenValid(token)).as("Expected to returned token be valid").isTrue();
            });
        }

        @Test
        @DisplayName("When generating access token should include user email as subject")
        void whenGeneratingAccessTokenShouldIncludeUserEmailAsSubject() {
            String token = jwtUtils.generateAccessToken(user);

            String extractedEmail = jwtUtils.extractUsername(token);
            assertThat(extractedEmail).isEqualTo(UserConstants.FIRST_USER_EMAIL);
        }

        @Test
        @DisplayName("When generating access token should include user ID in claims")
        void whenGeneratingAccessTokenShouldIncludeUserIdInClaims() {
            String token = jwtUtils.generateAccessToken(user);

            UUID extractedUserId = jwtUtils.extractUserId(token);
            assertThat(extractedUserId).isEqualTo(UserConstants.FIRST_USER_ID);
        }

        @Test
        @DisplayName("When generating access token should include all user roles in claims")
        void whenGeneratingAccessTokenShouldIncludeAllUserRolesInClaims() {
            user.setRoles(Set.of(roleUser, roleAdmin));
            String token = jwtUtils.generateAccessToken(user);

            Collection<? extends GrantedAuthority> authorities = jwtUtils.extractAuthorities(token);

            assertThat(authorities)
                    .hasSize(2)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder(RoleConstants.ROLE_USER_NAME, RoleConstants.ROLE_ADMIN_NAME);
        }

        @Test
        @DisplayName("When generating access token should generate different tokens for different users")
        void whenGeneratingAccessTokenShouldGenerateDifferentTokensForDifferentUsers() {
            User anotherUser = UserTestBuilder.secondUser().homeCity(cityWarsaw).build();

            String token1 = jwtUtils.generateAccessToken(user);
            String token2 = jwtUtils.generateAccessToken(anotherUser);

            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("When generating access token should set expiration time correctly")
        void whenGeneratingAccessTokenShouldSetExpirationTimeCorrectly() {
            String token = jwtUtils.generateAccessToken(user);

            Date expiration = jwtUtils.extractClaim(token, claims -> claims.getExpiration());
            Date issuedAt = jwtUtils.extractClaim(token, claims -> claims.getIssuedAt());

            long actualExpiration = expiration.getTime() - issuedAt.getTime();
            assertThat(actualExpiration).isEqualTo(JwtConstants.ACCESS_TOKEN_EXPIRATION_30_SECONDS);
        }
    }

    @Nested
    @DisplayName("Is token valid tests:")
    class IsTokenValidTests {

        @Test
        @DisplayName("When checking if token is valid should return true for valid token")
        void whenCheckingIfTokenIsValidShouldReturnTrueForValidToken() {
            String token = jwtUtils.generateAccessToken(user);

            assertThat(jwtUtils.isTokenValid(token)).isTrue();
        }

        @Test
        @DisplayName("When checking if token is valid should return false for expired token")
        void whenCheckingIfTokenIsValidShouldReturnFalseForExpiredToken() {
            JwtUtils tokenGenerator = new JwtUtils(jwtProperties(1L, JwtConstants.TEST_SECRET_BASE64), TimeConstants.FIXED_CLOCK);
            JwtUtils tokenValidator = new JwtUtils(jwtProperties(1L, JwtConstants.TEST_SECRET_BASE64), Clock.offset(TimeConstants.FIXED_CLOCK, Duration.ofMillis(2)));
            String token = tokenGenerator.generateAccessToken(user);

            assertThat(tokenValidator.isTokenValid(token)).isFalse();
        }

        @Test
        @DisplayName("When checking if token is valid should return false for malformed token")
        void whenCheckingIfTokenIsValidShouldReturnFalseForMalformedToken() {
            String malformedToken = JwtConstants.MALFORMED_TOKEN;

            assertThat(jwtUtils.isTokenValid(malformedToken)).isFalse();
        }

        @Test
        @DisplayName("When checking if token is valid should return false for token with invalid signature")
        void whenCheckingIfTokenIsValidShouldReturnFalseForTokenWithInvalidSignature() {
            String token = jwtUtils.generateAccessToken(user);
            // Tamper with the token by changing a character
            String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

            assertThat(jwtUtils.isTokenValid(tamperedToken)).isFalse();
        }

        @Test
        @DisplayName("When checking if token is valid should return false for token signed with different secret")
        void whenCheckingIfTokenIsValidShouldReturnFalseForTokenSignedWithDifferentSecret() {
            String differentSecret = JwtConstants.DIFFERENT_TEST_SECRET_BASE64;
            JwtUtils differentSecretJwtUtils = new JwtUtils(jwtProperties(JwtConstants.ACCESS_TOKEN_EXPIRATION_30_SECONDS, differentSecret), clock);

            String token = differentSecretJwtUtils.generateAccessToken(user);

            assertThat(jwtUtils.isTokenValid(token)).isFalse();
        }

        @Test
        @DisplayName("When checking if token is valid should return false for null token")
        void whenCheckingIfTokenIsValidShouldReturnFalseForNullToken() {
            assertThat(jwtUtils.isTokenValid(null)).isFalse();
        }

        @Test
        @DisplayName("When checking if token is valid should return false for empty token")
        void whenCheckingIfTokenIsValidShouldReturnFalseForEmptyToken() {
            assertThat(jwtUtils.isTokenValid("")).isFalse();
        }
    }

    @Nested
    @DisplayName("Extract claim tests:")
    class ExtractClaimTests {

        @Test
        @DisplayName("When extracting claim from token should extract username correctly")
        void whenExtractingClaimFromTokenShouldExtractUsernameCorrectly() {
            String token = jwtUtils.generateAccessToken(user);

            String username = jwtUtils.extractUsername(token);

            assertThat(username).isEqualTo(UserConstants.FIRST_USER_EMAIL);
        }

        @Test
        @DisplayName("When extracting claim from token should extract user ID correctly")
        void whenExtractingClaimFromTokenShouldExtractUserIdCorrectly() {
            String token = jwtUtils.generateAccessToken(user);

            UUID userId = jwtUtils.extractUserId(token);

            assertThat(userId).isEqualTo(UserConstants.FIRST_USER_ID);
        }

        @Test
        @DisplayName("When extracting claim from token should extract authorities correctly")
        void whenExtractingClaimFromTokenShouldExtractAuthoritiesCorrectly() {
            String token = jwtUtils.generateAccessToken(user);

            Collection<? extends GrantedAuthority> authorities = jwtUtils.extractAuthorities(token);

            assertThat(authorities)
                    .hasSize(1)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly(RoleConstants.ROLE_USER_NAME);
        }

        @Test
        @DisplayName("When extracting claim from token should extract multiple roles correctly")
        void whenExtractingClaimFromTokenShouldExtractMultipleRolesCorrectly() {
            user.setRoles(Set.of(roleUser, roleAdmin));
            String token = jwtUtils.generateAccessToken(user);

            Collection<? extends GrantedAuthority> authorities = jwtUtils.extractAuthorities(token);

            assertThat(authorities)
                    .hasSize(2)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder(RoleConstants.ROLE_USER_NAME, RoleConstants.ROLE_ADMIN_NAME);
        }

        @Test
        @DisplayName("When extracting claim from token should extract issued at date correctly")
        void whenExtractingClaimFromTokenShouldExtractIssuedAtDateCorrectly() {
            String token = jwtUtils.generateAccessToken(user);

            Date issuedAt = jwtUtils.extractClaim(token, claims -> claims.getIssuedAt());

            assertThat(issuedAt).isEqualTo(Date.from(TimeConstants.NOW));
        }

        @Test
        @DisplayName("When extracting claim from token should extract expiration date correctly")
        void whenExtractingClaimFromTokenShouldExtractExpirationDateCorrectly() {
            String token = jwtUtils.generateAccessToken(user);

            Date expiration = jwtUtils.extractClaim(token, claims -> claims.getExpiration());
            Date issuedAt = jwtUtils.extractClaim(token, claims -> claims.getIssuedAt());

            long expirationDuration = expiration.getTime() - issuedAt.getTime();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(expiration).isEqualTo(Date.from(TimeConstants.NOW.plusMillis(JwtConstants.ACCESS_TOKEN_EXPIRATION_30_SECONDS)));
                softly.assertThat(expirationDuration).isEqualTo(JwtConstants.ACCESS_TOKEN_EXPIRATION_30_SECONDS);
            });
        }

        @Test
        @DisplayName("When extracting claim from token should extract custom claim correctly")
        void whenExtractingClaimFromTokenShouldExtractCustomClaimCorrectly() {
            String token = jwtUtils.generateAccessToken(user);

            List<String> roles = jwtUtils.extractClaim(token, claims -> claims.get("roles", List.class));

            assertThat(roles)
                    .hasSize(1)
                    .containsExactly(RoleConstants.ROLE_USER_NAME);
        }
    }

    @Nested
    @DisplayName("Get access token expiration tests:")
    class GetAccessTokenExpirationTests {

        @Test
        @DisplayName("When returning token expiration time should return correct value")
        void whenReturningTokenExpirationTimeShouldReturnCorrectValue() {
            Long expiration = jwtUtils.getAccessTokenExpiration();

            assertThat(expiration).isEqualTo(JwtConstants.ACCESS_TOKEN_EXPIRATION_30_SECONDS);
        }
    }
}
