package com.mazurek.eventOrganizer.jwt;

import com.mazurek.eventOrganizer.auth.AuthUserLockService;
import com.mazurek.eventOrganizer.config.properties.JwtProperties;
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
    @Mock private AuthUserLockService authUserLockService;

    private RefreshTokenService refreshTokenService;
    private Clock clock;

    private JwtProperties jwtProperties(long shortExpiration, long longExpiration) {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setAccessExpiration(JwtConstants.ACCESS_TOKEN_EXPIRATION_30_SECONDS);
        jwtProperties.setRefreshShortExpiration(shortExpiration);
        jwtProperties.setRefreshLongExpiration(longExpiration);
        jwtProperties.setSecret(JwtConstants.TEST_SECRET_BASE64);
        return jwtProperties;
    }

    @BeforeEach
    void setUp() {
        clock = TimeConstants.FIXED_CLOCK;
        refreshTokenService = new RefreshTokenService(
                refreshTokenRepository,
                jwtProperties(JwtConstants.REFRESH_TOKEN_EXPIRATION_SHORT, JwtConstants.REFRESH_TOKEN_EXPIRATION_LONG),
                clock, authUserLockService);

        Instant userCreateAccountTime = TimeConstants.NOW.minusSeconds(60);

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

            IssuedRefreshToken issued = refreshTokenService.issueRefreshToken(user, deviceType);

            verify(refreshTokenRepository, times(1).description("Expected to save new refresh token")).save(refreshTokenArgumentCaptor.capture());

            RefreshToken capturedToken = refreshTokenArgumentCaptor.getValue();
            Instant tokenCreateDateTime = capturedToken.getCreatedAt();
            Instant tokenLastUsedAtDateTime = capturedToken.getLastUsedAt();
            Instant tokenExpiryDateTime = capturedToken.getExpiryDate();

            assertThat(issued.rawToken()).as("Expected new raw token to not be null.").isNotBlank();
            assertThat(capturedToken.getTokenHash()).isEqualTo(RefreshTokenHash.sha256(issued.rawToken()));
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

            refreshTokenService.issueRefreshToken(user, deviceType);

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

            IssuedRefreshToken first = refreshTokenService.issueRefreshToken(user, deviceType);
            IssuedRefreshToken second = refreshTokenService.issueRefreshToken(user, deviceType);

            verify(refreshTokenRepository, times(2)).save(captor.capture());

            List<RefreshToken> capturedTokens = captor.getAllValues();
            assertThat(first.rawToken()).isNotEqualTo(second.rawToken());
            assertThat(capturedTokens.get(0).getTokenHash()).isNotEqualTo(capturedTokens.get(1).getTokenHash());
        }

        @Test
        @DisplayName("When creating refresh token for different users should associate correct user")
        public void whenCreatingRefreshTokenForDifferentUsersShouldAssociateCorrectUser(){
            User anotherUser = UserTestBuilder.secondUser()
                    .id(UUID.randomUUID())
                    .build();

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

            refreshTokenService.issueRefreshToken(user, deviceType);
            refreshTokenService.issueRefreshToken(anotherUser, deviceType);

            verify(refreshTokenRepository, times(2)).save(captor.capture());

            assertThat(captor.getAllValues().get(0).getUser()).isEqualTo(user);
            assertThat(captor.getAllValues().get(1).getUser()).isEqualTo(anotherUser);
        }
    }

    @Nested
    @DisplayName("Use refresh token tests:")
    class UseRefreshTokenTests {

        private RefreshToken refreshToken;
        private Optional<RefreshToken> refreshTokenOptional;
        private String refreshTokenString = UUID.randomUUID().toString();

        @BeforeEach
        void setUp() {
            Instant tokenCreateDate = TimeConstants.NOW.minusSeconds(30);

            refreshToken = RefreshTokenTestBuilder.firstRefreshTokenForUser(user)
                    .rawToken(refreshTokenString)
                    .deviceType(DeviceType.MOBILE_ANDROID)
                    .createdAt(tokenCreateDate)
                    .lastUsedAt(tokenCreateDate)
                    .expiryDate(tokenCreateDate.plusMillis(JwtConstants.REFRESH_TOKEN_EXPIRATION_LONG))
                    .build();

            refreshTokenOptional = Optional.of(refreshToken);
            when(refreshTokenRepository.findUserIdByTokenHash(RefreshTokenHash.sha256(refreshTokenString)))
                    .thenReturn(Optional.of(user.getId()));
            when(authUserLockService.lockById(user.getId())).thenReturn(Optional.of(user));
        }

        @Test
        @DisplayName("When using refresh token should load its hash under a write lock")
        public void whenUsingRefreshTokenShouldLoadHashUnderWriteLock(){
            String tokenHash = RefreshTokenHash.sha256(refreshTokenString);
            when(refreshTokenRepository.findWithLockByTokenHash(tokenHash)).thenReturn(refreshTokenOptional);

            refreshTokenService.useRefreshToken(refreshTokenString);

            verify(refreshTokenRepository).findWithLockByTokenHash(tokenHash);
        }

        @Test
        @DisplayName("When using refresh token should reject an unknown token")
        public void whenUsingRefreshTokenShouldRejectUnknownToken(){
            when(refreshTokenRepository.findWithLockByTokenHash(RefreshTokenHash.sha256(refreshTokenString)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService.useRefreshToken(refreshTokenString))
                    .isInstanceOf(RefreshTokenNotFoundException.class);
        }

        @Test
        @DisplayName("When using refresh token should reject a revoked token")
        public void whenUsingRefreshTokenShouldRejectRevokedToken(){
            refreshToken.setRevoked(true);
            when(refreshTokenRepository.findWithLockByTokenHash(RefreshTokenHash.sha256(refreshTokenString)))
                    .thenReturn(refreshTokenOptional);

            assertThatThrownBy(() -> refreshTokenService.useRefreshToken(refreshTokenString))
                    .isInstanceOf(RefreshTokenRevokedException.class);
        }

        @Test
        @DisplayName("When using refresh token should reject an expired token")
        public void whenUsingRefreshTokenShouldRejectExpiredToken(){
            refreshToken.setExpiryDate(TimeConstants.NOW.minusSeconds(100));
            when(refreshTokenRepository.findWithLockByTokenHash(RefreshTokenHash.sha256(refreshTokenString)))
                    .thenReturn(refreshTokenOptional);

            assertThatThrownBy(() -> refreshTokenService.useRefreshToken(refreshTokenString))
                    .isInstanceOf(RefreshTokenExpiredException.class);
        }

        @Test
        @DisplayName("When using a mobile refresh token should update lastUsedAt")
        public void whenUsingMobileRefreshTokenShouldUpdateLastUsedAt(){
            refreshTokenService = new RefreshTokenService(
                    refreshTokenRepository,
                    jwtProperties(JwtConstants.REFRESH_TOKEN_EXPIRATION_SHORT, JwtConstants.REFRESH_TOKEN_EXPIRATION_LONG),
                    Clock.offset(clock, Duration.ofSeconds(5)), authUserLockService);
            when(refreshTokenRepository.findWithLockByTokenHash(RefreshTokenHash.sha256(refreshTokenString)))
                    .thenReturn(refreshTokenOptional);
            Instant tokenLastUsedAtBefore = refreshToken.getLastUsedAt();

            refreshTokenService.useRefreshToken(refreshTokenString);

            assertThat(refreshToken.getLastUsedAt()).isEqualTo(TimeConstants.NOW.plusSeconds(5));
            assertThat(refreshToken.getLastUsedAt()).isAfter(tokenLastUsedAtBefore);
        }

        @Test
        @DisplayName("When using a mobile refresh token should persist its updated timestamp and return the same raw token")
        public void whenUsingMobileRefreshTokenShouldSaveAndReturnSameRawToken(){
            when(refreshTokenRepository.findWithLockByTokenHash(RefreshTokenHash.sha256(refreshTokenString)))
                    .thenReturn(refreshTokenOptional);
            when(refreshTokenRepository.save(refreshToken)).thenReturn(refreshToken);

            RefreshTokenUse result = refreshTokenService.useRefreshToken(refreshTokenString);

            verify(refreshTokenRepository).save(refreshToken);
            assertThat(result.refreshToken()).isSameAs(refreshToken);
            assertThat(result.rawToken()).isEqualTo(refreshTokenString);
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
            Instant tokenCreateDate = TimeConstants.NOW.minusSeconds(30);

            refreshToken = RefreshTokenTestBuilder.firstRefreshTokenForUser(user)
                    .rawToken(refreshTokenString)
                    .deviceType(deviceType)
                    .createdAt(tokenCreateDate)
                    .lastUsedAt(tokenCreateDate)
                    .expiryDate(tokenCreateDate.plusMillis(expiration))
                    .build();

            refreshTokenOptional = Optional.of(refreshToken);
        }

        @Test
        @DisplayName("When revoking refresh token should resolve its hash under a write lock")
        public void whenRevokingRefreshTokenShouldResolveItsHashUnderWriteLock(){
            String tokenHash = RefreshTokenHash.sha256(refreshTokenString);
            when(refreshTokenRepository.findWithLockByTokenHash(tokenHash)).thenReturn(refreshTokenOptional);

            refreshTokenService.revokeRefreshToken(refreshTokenString);

            verify(refreshTokenRepository).findWithLockByTokenHash(tokenHash);
        }

        @Test
        @DisplayName("When revoking refresh token should throw RefreshTokenNotFoundException if given token does not exist")
        public void whenRevokingRefreshTokenShouldThrowRefreshTokenNotFoundExceptionIfGivenTokenDoesNotExist(){
            when(refreshTokenRepository.findWithLockByTokenHash(RefreshTokenHash.sha256(refreshTokenString)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService.revokeRefreshToken(refreshTokenString))
                    .as("Expected to throw RefreshTokenNotFoundException if requested token does not exist.")
                    .isInstanceOf(RefreshTokenNotFoundException.class);
            verify(refreshTokenRepository, never().description("Expected to not make any change in database.")).save(any(RefreshToken.class));
        }
        @Test
        @DisplayName("When revoking refresh token should mark refresh token as revoked and save that change in database")
        public void whenRevokingRefreshTokenShouldMarkRefreshTokenAsRevokedAndSaveThatChangeInDatabase(){
            when(refreshTokenRepository.findWithLockByTokenHash(RefreshTokenHash.sha256(refreshTokenString)))
                    .thenReturn(refreshTokenOptional);
            when(refreshTokenRepository.save(refreshToken)).thenReturn(refreshToken);

            RefreshToken revokedToken = refreshTokenService.revokeRefreshToken(refreshTokenString);

            assertThat(refreshToken.isRevoked()).as("Expected to mark requested refresh token as revoked.").isTrue();
            assertThat(revokedToken).isSameAs(refreshToken);
            assertThat(revokedToken.getTokenHash()).isEqualTo(RefreshTokenHash.sha256(refreshTokenString));
            assertThat(revokedToken.getTokenHash()).isNotEqualTo(refreshTokenString);
            verify(refreshTokenRepository,times(1).description("Expected to save updated refresh token in database")).save(refreshToken);
        }

        @Test
        @DisplayName("When revoking an already revoked refresh token should remain idempotent")
        public void whenRevokingAlreadyRevokedRefreshTokenShouldRemainIdempotent() {
            refreshToken.setRevoked(true);
            when(refreshTokenRepository.findWithLockByTokenHash(RefreshTokenHash.sha256(refreshTokenString)))
                    .thenReturn(refreshTokenOptional);
            when(refreshTokenRepository.save(refreshToken)).thenReturn(refreshToken);

            RefreshToken revokedToken = refreshTokenService.revokeRefreshToken(refreshTokenString);

            assertThat(revokedToken).isSameAs(refreshToken);
            assertThat(revokedToken.isRevoked()).isTrue();
            verify(refreshTokenRepository).save(refreshToken);
        }

    }

    @Nested
    @DisplayName("Secure refresh token lifecycle tests:")
    class SecureRefreshTokenLifecycleTests {

        @Test
        @DisplayName("When issuing a refresh token should persist only its SHA-256 hash")
        void whenIssuingRefreshTokenShouldPersistOnlyHash() {
            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

            IssuedRefreshToken issued = refreshTokenService.issueRefreshToken(user, DeviceType.WEB);

            verify(refreshTokenRepository).save(captor.capture());
            RefreshToken stored = captor.getValue();
            assertThat(issued.rawToken()).isNotBlank();
            assertThat(stored.getTokenHash()).isEqualTo(RefreshTokenHash.sha256(issued.rawToken()));
            assertThat(stored.getTokenHash()).isNotEqualTo(issued.rawToken());
            assertThat(stored.getFamilyId()).isNotNull();
        }

        @Test
        @DisplayName("When a rotated web credential is reused should revoke its entire family")
        void whenRotatedWebCredentialIsReusedShouldRevokeFamily() {
            String rawToken = UUID.randomUUID().toString();
            RefreshToken revokedToken = RefreshTokenTestBuilder.revokedRefreshTokenForUser(user)
                    .rawToken(rawToken)
                    .deviceType(DeviceType.WEB)
                    .build();
            UUID familyId = UUID.randomUUID();
            revokedToken.setFamilyId(familyId);
            when(refreshTokenRepository.findUserIdByTokenHash(RefreshTokenHash.sha256(rawToken)))
                    .thenReturn(Optional.of(user.getId()));
            when(authUserLockService.lockById(user.getId())).thenReturn(Optional.of(user));
            when(refreshTokenRepository.findWithLockByTokenHash(RefreshTokenHash.sha256(rawToken)))
                    .thenReturn(Optional.of(revokedToken));

            assertThatThrownBy(() -> refreshTokenService.useRefreshToken(rawToken))
                    .isInstanceOf(RefreshTokenRevokedException.class);

            verify(refreshTokenRepository).revokeAllByFamilyId(familyId);
            verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
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

            assertThat(capturedInstant).isEqualTo(TimeConstants.NOW);
        }
    }

}
