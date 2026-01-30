package com.mazurek.eventOrganizer.jwt;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private final String SECRET = "dGVzdC1zZWNyZXQtdGhhdC1pcy1sb25nLWVub3VnaC1mb3ItSFMyNTY=";
    private final long ACCESS_TOKEN_EXPIRATION = 30000; // 30 seconds for testing

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private User user;
    private final UUID USER_ID = UUID.randomUUID();
    private final String USER_EMAIL = "example@dot.com";
    private final String USER_FIRST_NAME = "Andrew";
    private final String USER_LAST_NAME = "Golota";
    private final String USER_PASSWORD = "Password123!";
    private final String USER_TIME_ZONE = "Europe/Warsaw";

    private Role roleUser;
    private Role roleAdmin;
    private final Long ROLE_USER_ID = 1L;
    private final String ROLE_USER_NAME = "ROLE_USER";
    private final Long ROLE_ADMIN_ID = 2L;
    private final String ROLE_ADMIN_NAME = "ROLE_ADMIN";

    private City cityRzeszow;
    private final UUID CITY_ID = UUID.randomUUID();
    private final String CITY_RZESZOW_NAME = "Rzeszow";

    @BeforeEach
    void setUp() {
        roleUser = new Role(ROLE_USER_ID, ROLE_USER_NAME);
        roleAdmin = new Role(ROLE_ADMIN_ID, ROLE_ADMIN_NAME);

        cityRzeszow = new City(CITY_ID, CITY_RZESZOW_NAME, new HashSet<>(), new HashSet<>());

        Instant userCreateAccountTime = Instant.now();
        user = User.builder()
                .id(USER_ID)
                .email(USER_EMAIL)
                .roles(Set.of(roleUser))
                .firstName(USER_FIRST_NAME)
                .lastName(USER_LAST_NAME)
                .activated(true)
                .banned(false)
                .homeCity(cityRzeszow)
                .password(passwordEncoder.encode(USER_PASSWORD))
                .timeZone(USER_TIME_ZONE)
                .createdAt(userCreateAccountTime)
                .lastCredentialsChangeTime(userCreateAccountTime)
                .build();

        jwtUtils = new JwtUtils(ACCESS_TOKEN_EXPIRATION, SECRET);
    }

    @Nested
    @DisplayName("Generate access token tests")
    class GenerateAccessTokenTests {

        @Test
        @DisplayName("When generating access token should generate valid non-empty token")
        void WhenGeneratingAccessTokenShouldGenerateValidNonEmptyToken() {
            String token = jwtUtils.generateAccessToken(user);

            assertNotNull(token);
            assertFalse(token.isBlank(), "Expected to not return empty token");
            assertTrue(jwtUtils.isTokenValid(token), "Expected to returned token be valid");
        }

        @Test
        @DisplayName("When generating access token should include user email as subject")
        void WhenGeneratingAccessTokenShouldIncludeUserEmailAsSubject() {
            String token = jwtUtils.generateAccessToken(user);

            String extractedEmail = jwtUtils.extractUsername(token);
            assertEquals(USER_EMAIL, extractedEmail);
        }

        @Test
        @DisplayName("When generating access token should include user ID in claims")
        void WhenGeneratingAccessTokenShouldIncludeUserIdInClaims() {
            String token = jwtUtils.generateAccessToken(user);

            UUID extractedUserId = jwtUtils.extractUserId(token);
            assertEquals(USER_ID, extractedUserId);
        }

        @Test
        @DisplayName("When generating access token should include all user roles in claims")
        void WhenGeneratingAccessTokenShouldIncludeAllUserRolesInClaims() {
            user.setRoles(Set.of(roleUser, roleAdmin));
            String token = jwtUtils.generateAccessToken(user);

            Collection<? extends GrantedAuthority> authorities = jwtUtils.extractAuthorities(token);

            assertThat(authorities)
                    .hasSize(2)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder(ROLE_USER_NAME, ROLE_ADMIN_NAME);
        }

        @Test
        @DisplayName("When generating access token should generate different tokens for different users")
        void WhenGeneratingAccessTokenShouldGenerateDifferentTokensForDifferentUsers() {
            User anotherUser = User.builder()
                    .id(UUID.randomUUID())
                    .email("another@example.com")
                    .roles(Set.of(roleUser))
                    .firstName("Jane")
                    .lastName("Doe")
                    .activated(true)
                    .banned(false)
                    .homeCity(cityRzeszow)
                    .password(passwordEncoder.encode("AnotherPass123!"))
                    .timeZone(USER_TIME_ZONE)
                    .createdAt(Instant.now())
                    .lastCredentialsChangeTime(Instant.now())
                    .build();

            String token1 = jwtUtils.generateAccessToken(user);
            String token2 = jwtUtils.generateAccessToken(anotherUser);

            assertNotEquals(token1, token2);
        }

        @Test
        @DisplayName("When generating access token should set expiration time correctly")
        void WhenGeneratingAccessTokenShouldSetExpirationTimeCorrectly() {
            String token = jwtUtils.generateAccessToken(user);

            Date expiration = jwtUtils.extractClaim(token, claims -> claims.getExpiration());
            Date issuedAt = jwtUtils.extractClaim(token, claims -> claims.getIssuedAt());

            long actualExpiration = expiration.getTime() - issuedAt.getTime();
            assertEquals(ACCESS_TOKEN_EXPIRATION, actualExpiration);
        }
    }

    @Nested
    @DisplayName("Is token valid tests")
    class IsTokenValidTests {

        @Test
        @DisplayName("When checking if token is valid should return true for valid token")
        void whenCheckingIfTokenIsValidShouldReturnTrueForValidToken() {
            String token = jwtUtils.generateAccessToken(user);

            assertTrue(jwtUtils.isTokenValid(token));
        }

        @Test
        @DisplayName("When checking if token is valid should return false for expired token")
        void whenCheckingIfTokenIsValidShouldReturnFalseForExpiredToken() throws InterruptedException {
            // Create JwtUtils with very short expiration (1ms)
            JwtUtils shortExpirationJwtUtils = new JwtUtils(1L, SECRET);
            String token = shortExpirationJwtUtils.generateAccessToken(user);

            // Wait for token to expire
            Thread.sleep(100);

            assertFalse(shortExpirationJwtUtils.isTokenValid(token));
        }

        @Test
        @DisplayName("When checking if token is valid should return false for malformed token")
        void whenCheckingIfTokenIsValidShouldReturnFalseForMalformedToken() {
            String malformedToken = "this.is.not.a.valid.jwt.token";

            assertFalse(jwtUtils.isTokenValid(malformedToken));
        }

        @Test
        @DisplayName("When checking if token is valid should return false for token with invalid signature")
        void whenCheckingIfTokenIsValidShouldReturnFalseForTokenWithInvalidSignature() {
            String token = jwtUtils.generateAccessToken(user);
            // Tamper with the token by changing a character
            String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

            assertFalse(jwtUtils.isTokenValid(tamperedToken));
        }

        @Test
        @DisplayName("When checking if token is valid should return false for token signed with different secret")
        void whenCheckingIfTokenIsValidShouldReturnFalseForTokenSignedWithDifferentSecret() {
            String differentSecret = "ZGlmZmVyZW50LXNlY3JldC10aGF0LWlzLWxvbmctZW5vdWdoLWZvci1IUzI1Ng==";
            JwtUtils differentSecretJwtUtils = new JwtUtils(ACCESS_TOKEN_EXPIRATION, differentSecret);

            String token = differentSecretJwtUtils.generateAccessToken(user);

            assertFalse(jwtUtils.isTokenValid(token));
        }

        @Test
        @DisplayName("When checking if token is valid should return false for null token")
        void whenCheckingIfTokenIsValidShouldReturnFalseForNullToken() {
            assertFalse(jwtUtils.isTokenValid(null));
        }

        @Test
        @DisplayName("When checking if token is valid should return false for empty token")
        void whenCheckingIfTokenIsValidShouldReturnFalseForEmptyToken() {
            assertFalse(jwtUtils.isTokenValid(""));
        }
    }

    @Nested
    @DisplayName("Extract claim tests")
    class ExtractClaimTests {

        @Test
        @DisplayName("When extracting claim from token should extract username correctly")
        void whenExtractingClaimFromTokenShouldExtractUsernameCorrectly() {
            String token = jwtUtils.generateAccessToken(user);

            String username = jwtUtils.extractUsername(token);

            assertEquals(USER_EMAIL, username);
        }

        @Test
        @DisplayName("When extracting claim from token should extract user ID correctly")
        void whenExtractingClaimFromTokenShouldExtractUserIdCorrectly() {
            String token = jwtUtils.generateAccessToken(user);

            UUID userId = jwtUtils.extractUserId(token);

            assertEquals(USER_ID, userId);
        }

        @Test
        @DisplayName("When extracting claim from token should extract authorities correctly")
        void whenExtractingClaimFromTokenShouldExtractAuthoritiesCorrectly() {
            String token = jwtUtils.generateAccessToken(user);

            Collection<? extends GrantedAuthority> authorities = jwtUtils.extractAuthorities(token);

            assertThat(authorities)
                    .hasSize(1)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly(ROLE_USER_NAME);
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
                    .containsExactlyInAnyOrder(ROLE_USER_NAME, ROLE_ADMIN_NAME);
        }

        @Test
        @DisplayName("When extracting claim from token should extract issued at date correctly")
        void whenExtractingClaimFromTokenShouldExtractIssuedAtDateCorrectly() {
            long beforeGeneration = System.currentTimeMillis()-1000;
            String token = jwtUtils.generateAccessToken(user);
            long afterGeneration = System.currentTimeMillis();

            Date issuedAt = jwtUtils.extractClaim(token, claims -> claims.getIssuedAt());

            assertTrue(issuedAt.getTime() >= beforeGeneration);
            assertTrue(issuedAt.getTime() <= afterGeneration);
        }

        @Test
        @DisplayName("When extracting claim from token should extract expiration date correctly")
        void whenExtractingClaimFromTokenShouldExtractExpirationDateCorrectly() {
            String token = jwtUtils.generateAccessToken(user);

            Date expiration = jwtUtils.extractClaim(token, claims -> claims.getExpiration());
            Date issuedAt = jwtUtils.extractClaim(token, claims -> claims.getIssuedAt());

            long expirationDuration = expiration.getTime() - issuedAt.getTime();
            assertEquals(ACCESS_TOKEN_EXPIRATION, expirationDuration);
        }

        @Test
        @DisplayName("When extracting claim from token should extract custom claim correctly")
        void whenExtractingClaimFromTokenShouldExtractCustomClaimCorrectly() {
            String token = jwtUtils.generateAccessToken(user);

            List<String> roles = jwtUtils.extractClaim(token, claims -> claims.get("roles", List.class));

            assertThat(roles)
                    .hasSize(1)
                    .containsExactly(ROLE_USER_NAME);
        }
    }

    @Nested
    @DisplayName("Get access token expiration tests")
    class GetAccessTokenExpirationTests {

        @Test
        @DisplayName("When returning token expiration time should return correct value")
        void whenReturningTokenExpirationTimeShouldReturnCorrectValue() {
            Long expiration = jwtUtils.getAccessTokenExpiration();

            assertEquals(ACCESS_TOKEN_EXPIRATION, expiration);
        }
    }
}