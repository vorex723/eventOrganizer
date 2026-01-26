package com.mazurek.eventOrganizer.jwt;

import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenExpiredException;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenRevokedException;

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

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceUnitTest {

    private final Long SHORT_REFRESH_TOKEN_EXPIRATION = 86400000L;
    private final Long LONG_REFRESH_TOKEN_EXPIRATION = 2592000000L;

    private User user;
    private final UUID USER_ID = UUID.randomUUID();
    private final String USER_EMAIL = "example@dot.com";
    private final String USER_FIRST_NAME = "Andrew";
    private final String USER_LAST_NAME = "Golota";
    private final String USER_PASSWORD = "Password123!";
    private final String USER_TIME_ZONE = "Europe/Warsaw";
    private BCryptPasswordEncoder passwordEncoder = Mockito.spy(new BCryptPasswordEncoder());

    private DeviceType deviceType;

    @Mock private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, SHORT_REFRESH_TOKEN_EXPIRATION, LONG_REFRESH_TOKEN_EXPIRATION);

        Instant userCreateAccountTime = Instant.now();

        user = User.builder()
                .id(USER_ID)
                .email(USER_EMAIL)
                .roles(Set.of())
                .firstName(USER_FIRST_NAME)
                .lastName(USER_LAST_NAME)
                .activated(false)
                .banned(false)
                .password(passwordEncoder.encode(USER_PASSWORD))
                .timeZone(USER_TIME_ZONE)
                .createdAt(userCreateAccountTime)
                .lastCredentialsChangeTime(userCreateAccountTime)
                .build();

        deviceType = DeviceType.WEB;
    }

    @Nested
    @DisplayName("Create refresh token tests:")
    class CreateRefreshTokenTests{
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

            assertAll("Refresh token data assertions: ",
                    () -> assertNotNull(capturedToken.getToken(), "Expected new token to not be null."),
                    () -> assertFalse(capturedToken.getToken().isBlank(), "Expected new token to not be blank."),
                    () -> assertEquals(user, capturedToken.getUser(), "Expected to set passed user."),
                    () -> assertEquals(deviceType, capturedToken.getDeviceType(), "Expected to set correct device type."),
                    () -> assertEquals(tokenCreateDateTime, tokenLastUsedAtDateTime, "Expected token create date time be the same as token last used at date time."),
                    () -> assertEquals(SHORT_REFRESH_TOKEN_EXPIRATION, tokenExpiryDateTime.toEpochMilli() - tokenCreateDateTime.toEpochMilli(), "Expected difference between token expiry date and it\'s create date to be equal expected expiration time")
            );

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
                assertEquals(SHORT_REFRESH_TOKEN_EXPIRATION, tokenExpiryDateTime.toEpochMilli() - tokenCreateDateTime.toEpochMilli(), "Expected difference between token expiry date and it\'s create date to be equal expected expiration time");
            else
                assertEquals(LONG_REFRESH_TOKEN_EXPIRATION, tokenExpiryDateTime.toEpochMilli() - tokenCreateDateTime.toEpochMilli(), "Expected difference between token expiry date and it\'s create date to be equal expected expiration time");
        }

        @Test
        @DisplayName("When creating multiple refresh tokens should generate unique tokens")
        public void whenCreatingMultipleRefreshTokensShouldGenerateUniqueTokens(){
            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

            refreshTokenService.createRefreshToken(user, deviceType);
            refreshTokenService.createRefreshToken(user, deviceType);

            verify(refreshTokenRepository, times(2)).save(captor.capture());

            List<RefreshToken> capturedTokens = captor.getAllValues();
            assertNotEquals(capturedTokens.get(0).getToken(), capturedTokens.get(1).getToken());
        }

        @Test
        @DisplayName("When creating refresh token for different users should associate correct user")
        public void whenCreatingRefreshTokenForDifferentUsersShouldAssociateCorrectUser(){
            User anotherUser = User.builder()
                    .id(UUID.randomUUID())
                    .email("another@example.com")
                    .build();

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

            refreshTokenService.createRefreshToken(user, deviceType);
            refreshTokenService.createRefreshToken(anotherUser, deviceType);

            verify(refreshTokenRepository, times(2)).save(captor.capture());

            assertEquals(user, captor.getAllValues().get(0).getUser());
            assertEquals(anotherUser, captor.getAllValues().get(1).getUser());
        }
    }

    @Nested
    @DisplayName("Verify and get refresh token tests:")
    class VerifyAndGetRefreshTokenTests{

        private RefreshToken refreshToken;
        private Optional<RefreshToken> refreshTokenOptional;
        private String refreshTokenString = UUID.randomUUID().toString();

        @BeforeEach
        void setUp() {
            Long expiration = deviceType.shouldRotateRefreshToken() ? SHORT_REFRESH_TOKEN_EXPIRATION : LONG_REFRESH_TOKEN_EXPIRATION;
            Instant tokenCreateDate = Instant.now();

            refreshToken = new RefreshToken();
            refreshToken.setToken(refreshTokenString);
            refreshToken.setUser(user);
            refreshToken.setDeviceType(deviceType);
            refreshToken.setCreatedAt(tokenCreateDate);
            refreshToken.setExpiryDate(tokenCreateDate.plusMillis(expiration));
            refreshToken.setLastUsedAt(tokenCreateDate);

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

            assertThrows(RefreshTokenNotFoundException.class, () -> refreshTokenService.verifyAndGetRefreshToken(refreshTokenString));

            verify(refreshTokenRepository, times(1).description("Expected to load requested token from database")).findByToken(refreshTokenString);
        }

        @Test
        @DisplayName("When verifying refresh token should throw RefreshTokenRevokedException if token was revoked")
        public void whenVerifyingRefreshTokenShouldThrowRefreshTokenRevokedExceptionIfTokenWasRevoked(){
            refreshToken.setRevoked(true);
            when(refreshTokenRepository.findByToken(refreshTokenString)).thenReturn(refreshTokenOptional);

            assertThrows(RefreshTokenRevokedException.class, () -> refreshTokenService.verifyAndGetRefreshToken(refreshTokenString));
        }

        @Test
        @DisplayName("When verifying refresh token should throw RefreshTokenExpiredException if token is expired")
        public void whenVerifyingRefreshTokenShouldThrowRefreshTokenExpiredExceptionIfTokenIsExpired(){
            refreshToken.setExpiryDate(Instant.now().minusSeconds(100));
            when(refreshTokenRepository.findByToken(refreshTokenString)).thenReturn(refreshTokenOptional);

            assertThrows(RefreshTokenExpiredException.class, () -> refreshTokenService.verifyAndGetRefreshToken(refreshTokenString));
        }

        @Test
        @DisplayName("When verifying refresh token should update token last used at field")
        public void whenVerifyingRefreshTokenShouldUpdateTokenLastUsedAtField(){
            when(refreshTokenRepository.findByToken(refreshTokenString)).thenReturn(refreshTokenOptional);
            Instant tokenLastUsedAtBefore = refreshToken.getLastUsedAt();

            refreshTokenService.verifyAndGetRefreshToken(refreshTokenString);

            assertTrue(tokenLastUsedAtBefore.isBefore(refreshToken.getLastUsedAt()));
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
    class RevokeRefreshTokenTests{
        private RefreshToken refreshToken;
        private Optional<RefreshToken> refreshTokenOptional;
        private String refreshTokenString = UUID.randomUUID().toString();

        @BeforeEach
        void setUp() {
            Long expiration = deviceType.shouldRotateRefreshToken() ? SHORT_REFRESH_TOKEN_EXPIRATION : LONG_REFRESH_TOKEN_EXPIRATION;
            Instant tokenCreateDate = Instant.now();

            refreshToken = new RefreshToken();
            refreshToken.setToken(refreshTokenString);
            refreshToken.setUser(user);
            refreshToken.setDeviceType(deviceType);
            refreshToken.setCreatedAt(tokenCreateDate);
            refreshToken.setExpiryDate(tokenCreateDate.plusMillis(expiration));
            refreshToken.setLastUsedAt(tokenCreateDate);

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

            assertThrows(RefreshTokenNotFoundException.class, () -> refreshTokenService.revokeRefreshToken(refreshTokenString), "Expected to throw RefreshTokenNotFoundException if requested token does not exist.");
            verify(refreshTokenRepository, never().description("Expected to not make any change in database.")).save(any(RefreshToken.class));
        }
        @Test
        @DisplayName("When revoking refresh token should mark refresh token as revoked and save that change in database")
        public void whenRevokingRefreshTokenShouldMarkRefreshTokenAsRevokedAndSaveThatChangeInDatabase(){
            when(refreshTokenRepository.findByToken(refreshTokenString)).thenReturn(refreshTokenOptional);

            refreshTokenService.revokeRefreshToken(refreshTokenString);

            assertTrue(refreshToken.isRevoked(), "Expected to mark requested refresh token as revoked.");
            verify(refreshTokenRepository,times(1).description("Expected to save updated refresh token in database")).save(refreshToken);
        }

    }

}