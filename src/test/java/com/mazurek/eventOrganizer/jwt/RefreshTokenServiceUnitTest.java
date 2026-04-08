package com.mazurek.eventOrganizer.jwt;

import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenExpiredException;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenRevokedException;
import com.mazurek.eventOrganizer.testData.builders.RefreshTokenTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Stream;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService unit tests:")
class RefreshTokenServiceUnitTest {

    private User user;
    private BCryptPasswordEncoder passwordEncoder = Mockito.spy(new BCryptPasswordEncoder());

    private DeviceType deviceType;

    @Mock private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;
    private Clock clock;
    private Instant fixedInstant;

    @BeforeEach
    void setUp() {
        fixedInstant = Instant.parse("2025-01-01T10:15:30Z");
        clock = Clock.fixed(fixedInstant, ZoneOffset.UTC);
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, JwtConstants.REFRESH_TOKEN_EXPIRATION_SHORT, JwtConstants.REFRESH_TOKEN_EXPIRATION_LONG, clock);

        Instant userCreateAccountTime = fixedInstant.minusSeconds(60);

        user = UserTestBuilder.firstUser()
                .roles(Set.of())
                .activated(false)
                .banned(false)
                .password(passwordEncoder.encode(UserConstants.USER_PASSWORD))
                .createdAt(userCreateAccountTime)
                .lastCredentialsChangeTime(userCreateAccountTime)
                .build();

        deviceType = DeviceType.WEB;
    }

    @Nested
    @DisplayName("Create refresh token tests:")
    class CreateRefreshTokenTests {
        static Stream<DeviceType> deviceTypes() {
            return Stream.of(DeviceType.values());
        }


        @Test
        @DisplayName("When creating refresh token should save new refresh token with correct data")
        public void whenCreatingRefreshTokenShouldSaveNewRefreshTokenWithCorrectData(){
            ArgumentCaptor<RefreshToken> refreshTokenArgumentCaptor = ArgumentCaptor.forClass(RefreshToken.class);

            refreshTokenService.createRefreshToken(user,deviceType);

            verify(refreshTokenRepository, times(1).description("Expected to save new refresh token")).save(refreshTokenArgumentCaptor.capture());

            RefreshToken capturedToken = refreshTokenArgumentCaptor.getValue();
            Instant tokenCreateDateTime = capturedToken.getCreatedAt();
            Instant tokenLastUsedAtDateTime = capturedToken.getLastUsedAt();
            Instant tokenExpiryDateTime = capturedToken.getExpiryDate();

            assertThat(capturedToken.getToken()).as("Expected new token to not be null.").isNotBlank();
            assertThat(capturedToken.getUser()).as("Expected to set passed user.").isEqualTo(user);
            assertThat(capturedToken.getDeviceType()).as("Expected to set correct device type.").isEqualTo(deviceType);
            assertThat(tokenLastUsedAtDateTime)
                    .as("Expected token create date time be the same as token last used at date time.")
                    .isEqualTo(tokenCreateDateTime);
            assertThat(tokenExpiryDateTime.toEpochMilli() - tokenCreateDateTime.toEpochMilli())
                    .as("Expected difference between token expiry date and it's create date to be equal expected expiration time")
                    .isEqualTo(JwtConstants.REFRESH_TOKEN_EXPIRATION_SHORT);

        }

        @ParameterizedTest(name = "Expiration time test for deviceType={0}")
        @MethodSource("deviceTypes")
        @DisplayName("When creating refresh token should set correct expiration times on different device types")
        public void whenCreatingRefreshTokenShouldSetCorrectExpirationTimesOnDifferentDeviceTypes(DeviceType deviceType){
            ArgumentCaptor<RefreshToken> refreshTokenArgumentCaptor = ArgumentCaptor.forClass(RefreshToken.class);

            refreshTokenService.createRefreshToken(user,deviceType);

            verify(refreshTokenRepository, times(1).description("Expected to save new refresh token")).save(refreshTokenArgumentCaptor.capture());

            RefreshToken capturedToken = refreshTokenArgumentCaptor.getValue();
            Instant tokenCreateDateTime = capturedToken.getCreatedAt();
            Instant tokenLastUsedAtDateTime = capturedToken.getLastUsedAt();
            Instant tokenExpiryDateTime = capturedToken.getExpiryDate();

            if (deviceType.shouldRotateRefreshToken())
                assertThat(tokenExpiryDateTime.toEpochMilli() - tokenCreateDateTime.toEpochMilli())
                        .as("Expected difference between token expiry date and it's create date to be equal expected expiration time")
                        .isEqualTo(JwtConstants.REFRESH_TOKEN_EXPIRATION_SHORT);
            else
                assertThat(tokenExpiryDateTime.toEpochMilli() - tokenCreateDateTime.toEpochMilli())
                        .as("Expected difference between token expiry date and it's create date to be equal expected expiration time")
                        .isEqualTo(JwtConstants.REFRESH_TOKEN_EXPIRATION_LONG);
        }

        @Test
        @DisplayName("When creating multiple refresh tokens should generate unique tokens")
        public void whenCreatingMultipleRefreshTokensShouldGenerateUniqueTokens(){
            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

            refreshTokenService.createRefreshToken(user, deviceType);
            refreshTokenService.createRefreshToken(user, deviceType);

            verify(refreshTokenRepository, times(2)).save(captor.capture());

            List<RefreshToken> capturedTokens = captor.getAllValues();
            assertThat(capturedTokens.get(0).getToken()).isNotEqualTo(capturedTokens.get(1).getToken());
        }

        @Test
        @DisplayName("When creating refresh token for different users should associate correct user")
        public void whenCreatingRefreshTokenForDifferentUsersShouldAssociateCorrectUser(){
            User anotherUser = UserTestBuilder.secondUser()
                    .id(UUID.randomUUID())
                    .build();

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

            refreshTokenService.createRefreshToken(user, deviceType);
            refreshTokenService.createRefreshToken(anotherUser, deviceType);

            verify(refreshTokenRepository, times(2)).save(captor.capture());

            assertThat(captor.getAllValues().get(0).getUser()).isEqualTo(user);
            assertThat(captor.getAllValues().get(1).getUser()).isEqualTo(anotherUser);
        }
    }

    @Nested
    @DisplayName("Verify and get refresh token tests:")
    class VerifyAndGetRefreshTokenTests {

        private RefreshToken refreshToken;
        private Optional<RefreshToken> refreshTokenOptional;
        private String refreshTokenString = UUID.randomUUID().toString();

        @BeforeEach
        void setUp() {
            Long expiration = deviceType.shouldRotateRefreshToken() ? JwtConstants.REFRESH_TOKEN_EXPIRATION_SHORT : JwtConstants.REFRESH_TOKEN_EXPIRATION_LONG;
            Instant tokenCreateDate = fixedInstant.minusSeconds(30);

            refreshToken = RefreshTokenTestBuilder.firstRefreshTokenForUser(user)
                    .token(refreshTokenString)
                    .deviceType(deviceType)
                    .createdAt(tokenCreateDate)
                    .lastUsedAt(tokenCreateDate)
                    .expiryDate(tokenCreateDate.plusMillis(expiration))
                    .build();

            refreshTokenOptional = Optional.of(refreshToken);
        }

        @Test
        @DisplayName("When verifying refresh token should load refresh token from database")
        public void whenVerifyingRefreshTokenShouldLoadRefreshTokenFromDatabase(){
            when(refreshTokenRepository.findByToken(refreshTokenString)).thenReturn(refreshTokenOptional);

            refreshTokenService.verifyAndGetRefreshToken(refreshTokenString);

            verify(refreshTokenRepository, times(1).description("Expected to load requested token from database")).findByToken(refreshTokenString);
        }

        @Test
        @DisplayName("When verifying refresh token should throw RefreshTokenNotFoundException if requested refresh token does not exist")
        public void whenVerifyingRefreshTokenShouldThrowRefreshTokenNotFoundExceptionIfRequestedRefreshTokenDoesNotExist(){
            when(refreshTokenRepository.findByToken(refreshTokenString)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService.verifyAndGetRefreshToken(refreshTokenString))
                    .isInstanceOf(RefreshTokenNotFoundException.class);

            verify(refreshTokenRepository, times(1).description("Expected to load requested token from database")).findByToken(refreshTokenString);
        }

        @Test
        @DisplayName("When verifying refresh token should throw RefreshTokenRevokedException if token was revoked")
        public void whenVerifyingRefreshTokenShouldThrowRefreshTokenRevokedExceptionIfTokenWasRevoked(){
            refreshToken.setRevoked(true);
            when(refreshTokenRepository.findByToken(refreshTokenString)).thenReturn(refreshTokenOptional);

            assertThatThrownBy(() -> refreshTokenService.verifyAndGetRefreshToken(refreshTokenString))
                    .isInstanceOf(RefreshTokenRevokedException.class);
        }

        @Test
        @DisplayName("When verifying refresh token should throw RefreshTokenExpiredException if token is expired")
        public void whenVerifyingRefreshTokenShouldThrowRefreshTokenExpiredExceptionIfTokenIsExpired(){
            refreshToken.setExpiryDate(fixedInstant.minusSeconds(100));
            when(refreshTokenRepository.findByToken(refreshTokenString)).thenReturn(refreshTokenOptional);

            assertThatThrownBy(() -> refreshTokenService.verifyAndGetRefreshToken(refreshTokenString))
                    .isInstanceOf(RefreshTokenExpiredException.class);
        }

        @Test
        @DisplayName("When verifying refresh token should update token last used at field")
        public void whenVerifyingRefreshTokenShouldUpdateTokenLastUsedAtField(){
            refreshTokenService = new RefreshTokenService(
                    refreshTokenRepository,
                    JwtConstants.REFRESH_TOKEN_EXPIRATION_SHORT,
                    JwtConstants.REFRESH_TOKEN_EXPIRATION_LONG,
                    Clock.offset(clock, Duration.ofSeconds(5)));
            when(refreshTokenRepository.findByToken(refreshTokenString)).thenReturn(refreshTokenOptional);
            Instant tokenLastUsedAtBefore = refreshToken.getLastUsedAt();

            refreshTokenService.verifyAndGetRefreshToken(refreshTokenString);

            assertThat(refreshToken.getLastUsedAt()).isEqualTo(fixedInstant.plusSeconds(5));
            assertThat(refreshToken.getLastUsedAt()).isAfter(tokenLastUsedAtBefore);
        }
        @Test
        @DisplayName("When verifying refresh token should save refreshToken after field update")
        public void whenVerifyingRefreshTokenShouldSaveRefreshTokenAfterFieldUpdate(){
            when(refreshTokenRepository.findByToken(refreshTokenString)).thenReturn(refreshTokenOptional);

            refreshTokenService.verifyAndGetRefreshToken(refreshTokenString);

            verify(refreshTokenRepository, times(1)).save(refreshToken);
        }
    }

    @Nested
    @DisplayName("Revoke refresh token tests:")
    class RevokeRefreshTokenTests {
        private RefreshToken refreshToken;
        private Optional<RefreshToken> refreshTokenOptional;
        private String refreshTokenString = UUID.randomUUID().toString();

        @BeforeEach
        void setUp() {
            Long expiration = deviceType.shouldRotateRefreshToken() ? JwtConstants.REFRESH_TOKEN_EXPIRATION_SHORT : JwtConstants.REFRESH_TOKEN_EXPIRATION_LONG;
            Instant tokenCreateDate = fixedInstant.minusSeconds(30);

            refreshToken = RefreshTokenTestBuilder.firstRefreshTokenForUser(user)
                    .token(refreshTokenString)
                    .deviceType(deviceType)
                    .createdAt(tokenCreateDate)
                    .lastUsedAt(tokenCreateDate)
                    .expiryDate(tokenCreateDate.plusMillis(expiration))
                    .build();

            refreshTokenOptional = Optional.of(refreshToken);
        }

        @Test
        @DisplayName("When revoking refresh token should load refresh token from database by provided token")
        public void whenRevokingRefreshTokenShouldLoadRefreshTokenFromDatabaseByProvidedToken(){
            when(refreshTokenRepository.findByToken(refreshTokenString)).thenReturn(refreshTokenOptional);

            refreshTokenService.revokeRefreshToken(refreshTokenString);

            verify(refreshTokenRepository, times(1).description("Expected to load refresh token from database using provided token.")).findByToken(refreshTokenString);
        }

        @Test
        @DisplayName("When revoking refresh token should throw RefreshTokenNotFoundException if given token does not exist")
        public void whenRevokingRefreshTokenShouldThrowRefreshTokenNotFoundExceptionIfGivenTokenDoesNotExist(){
            when(refreshTokenRepository.findByToken(refreshTokenString)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService.revokeRefreshToken(refreshTokenString))
                    .as("Expected to throw RefreshTokenNotFoundException if requested token does not exist.")
                    .isInstanceOf(RefreshTokenNotFoundException.class);
            verify(refreshTokenRepository, never().description("Expected to not make any change in database.")).save(any(RefreshToken.class));
        }
        @Test
        @DisplayName("When revoking refresh token should mark refresh token as revoked and save that change in database")
        public void whenRevokingRefreshTokenShouldMarkRefreshTokenAsRevokedAndSaveThatChangeInDatabase(){
            when(refreshTokenRepository.findByToken(refreshTokenString)).thenReturn(refreshTokenOptional);

            refreshTokenService.revokeRefreshToken(refreshTokenString);

            assertThat(refreshToken.isRevoked()).as("Expected to mark requested refresh token as revoked.").isTrue();
            verify(refreshTokenRepository,times(1).description("Expected to save updated refresh token in database")).save(refreshToken);
        }

    }

    @Nested
    @DisplayName("Cleanup expired tokens tests:")
    class CleanupExpiredTokensTests {

        @Test
        @DisplayName("When cleaning up expired tokens should call repository delete with current timestamp")
        public void whenCleaningUpExpiredTokensShouldCallRepositoryDeleteWithCurrentTimestamp() {
            refreshTokenService.cleanupExpiredTokens();

            ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
            verify(refreshTokenRepository, times(1))
                    .deleteExpiredTokens(instantCaptor.capture());
            Instant capturedInstant = instantCaptor.getValue();

            assertThat(capturedInstant).isEqualTo(fixedInstant);
        }
    }

}
