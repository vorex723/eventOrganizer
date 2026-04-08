package com.mazurek.eventOrganizer.jwt;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenExpiredException;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenRevokedException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.exception.user.UserRoleNotFoundException;
import com.mazurek.eventOrganizer.testData.builders.CityTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.RefreshTokenTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.RoleTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.RoleRepository;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("RefreshTokenService integration tests:")
public class RefreshTokenServiceIntegrationTest {
    @Value("${app.jwt.refresh-short-expiration:86400000}")
    private Long shortRefreshTokenExpiration;
    @Value("${app.jwt.refresh-long-expiration:2592000000}")
    private Long longRefreshTokenExpiration;

    private DeviceType deviceType;

    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private DeletionService deletionService;

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        SecurityContextHolder.clearContext();
        if (roleRepository.findByName(RoleConstants.ROLE_USER_NAME).isEmpty()){
            Role roleUser = RoleTestBuilder.userRole().id(null).build();
            roleRepository.save(roleUser);
        }
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
    }

    private Role getUserRole() {
        return roleRepository.findByName(RoleConstants.ROLE_USER_NAME)
                .orElseThrow(UserRoleNotFoundException::new);
    }

    private City persistCity(String cityName) {
        return cityRepository.findByIgnoreCaseName(cityName)
                .orElseGet(() -> cityRepository.save(new CityTestBuilder()
                        .id(null)
                        .name(cityName.toLowerCase(Locale.ROOT))
                        .build()));
    }

    private User persistActiveUser(UserTestBuilder userBuilder, City city) {
        Instant userCreateDateTime = Instant.now();

        return userRepository.save(userBuilder
                .id(null)
                .homeCity(city)
                .password(passwordEncoder.encode(UserConstants.USER_PASSWORD))
                .createdAt(userCreateDateTime)
                .lastCredentialsChangeTime(userCreateDateTime)
                .roles(Set.of(getUserRole()))
                .activated(true)
                .banned(false)
                .build());
    }

    private RefreshToken persistRefreshToken(User user, DeviceType deviceType, Instant createdAt, Instant expiryDate) {
        return persistRefreshToken(user, UUID.randomUUID().toString(), deviceType, createdAt, expiryDate, false);
    }

    private RefreshToken persistRefreshToken(User user, String token, DeviceType deviceType, Instant createdAt, Instant expiryDate, boolean revoked) {
        return refreshTokenRepository.save(RefreshTokenTestBuilder.firstRefreshTokenForUser(user)
                .id(null)
                .token(token)
                .deviceType(deviceType)
                .createdAt(createdAt)
                .lastUsedAt(createdAt)
                .expiryDate(expiryDate)
                .revoked(revoked)
                .build());
    }

    @Nested
    @DisplayName("Create refresh token tests:")
    class CreateRefreshTokenTests {

        @BeforeEach
        void setUp(){
            City cityRzeszow = persistCity(CitiesConstants.WARSAW_NAME);
            persistActiveUser(UserTestBuilder.firstUser(), cityRzeszow);

            deviceType = DeviceType.WEB;
        }

        static Stream<DeviceType> deviceTypes() {
            return Stream.of(DeviceType.values());
        }

        @Test
        @DisplayName("When creating refresh token should create non-revoked and non-expired token")
        public void whenCreatingRefreshTokenShouldCreateNonRevokedAndNonExpiredToken(){
            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow();

            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, deviceType);

            assertThat(refreshToken.isRevoked()).as("Expected new token to not be revoked").isFalse();
            assertThat(refreshToken.isExpired()).as("Expected new token to not be expired").isFalse();
        }

        @Test
        @DisplayName("When creating refresh token should save new token in database")
        public void whenCreatingRefreshTokenShouldSaveNewTokenInDatabase(){
            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);

            RefreshToken returnedRefreshToken = refreshTokenService.createRefreshToken(user, deviceType);
            Optional<RefreshToken> persistedRefreshTokenOptional = refreshTokenRepository.findByToken(returnedRefreshToken.getToken());
            assertThat(persistedRefreshTokenOptional).as("Expected token to be persisted in database.").isPresent();
            RefreshToken refreshToken = persistedRefreshTokenOptional.get();

            assertThat(refreshToken.getUser()).as("Expected to set correct user on refresh token.").isEqualTo(user);
            assertThat(refreshToken.getDeviceType()).as("Expected to set correct device type.").isEqualTo(deviceType);
            assertThat(refreshToken.getLastUsedAt())
                    .as("Expected to set the same created at and last used at timestamps.")
                    .isEqualTo(refreshToken.getCreatedAt());
            assertThat(refreshToken.getExpiryDate())
                    .as("Expected expiry date to token to be after it's creation.")
                    .isAfter(refreshToken.getCreatedAt());
        }

        @ParameterizedTest(name = "Expiration time test for deviceType={0}")
        @MethodSource("deviceTypes")
        @DisplayName("When creating refresh token should set correct expiration of token based on device type")
        public void whenCreatingRefreshTokenShouldSetCorrectExpirationOfTokenBasedOnDeviceType(DeviceType deviceTypeParam){
            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);

            RefreshToken returnedRefreshToken = refreshTokenService.createRefreshToken(user, deviceTypeParam);
            long expiryDateMills = returnedRefreshToken.getExpiryDate().toEpochMilli();
            long createDateMills = returnedRefreshToken.getCreatedAt().toEpochMilli();
            if (deviceTypeParam.shouldRotateRefreshToken())
                assertThat(expiryDateMills - createDateMills).isEqualTo(shortRefreshTokenExpiration);
            else {
                assertThat(expiryDateMills - createDateMills).isEqualTo(longRefreshTokenExpiration);
            }
        }
    }

    @Nested
    @DisplayName("Verify and get refresh token tests:")
    class VerifyAndGetRefreshTokenTests {

        private User user;
        private RefreshToken refreshToken;

        @BeforeEach
        void setUp() {
            City cityRzeszow = persistCity(CitiesConstants.WARSAW_NAME);
            user = persistActiveUser(UserTestBuilder.firstUser(), cityRzeszow);

            deviceType = DeviceType.WEB;

            Instant tokenCreateDate = Instant.now();
            refreshToken = persistRefreshToken(
                    user,
                    deviceType,
                    tokenCreateDate,
                    tokenCreateDate.plusMillis(shortRefreshTokenExpiration)
            );
        }

        @Test
        @DisplayName("When verifying refresh token should throw RefreshTokenNotFoundException if token does not exist")
        public void whenVerifyingRefreshTokenShouldThrowRefreshTokenNotFoundExceptionIfTokenDoesNotExist(){
            String nonExistentToken = UUID.randomUUID().toString();

            assertThatThrownBy(() -> refreshTokenService.verifyAndGetRefreshToken(nonExistentToken))
                    .as("Expected to throw RefreshTokenNotFoundException for non-existent token")
                    .isInstanceOf(RefreshTokenNotFoundException.class);
        }

        @Test
        @DisplayName("When verifying refresh token should throw RefreshTokenRevokedException if token is revoked")
        public void whenVerifyingRefreshTokenShouldThrowRefreshTokenRevokedExceptionIfTokenIsRevoked(){
            refreshToken.setRevoked(true);
            refreshTokenRepository.saveAndFlush(refreshToken);

            assertThatThrownBy(() -> refreshTokenService.verifyAndGetRefreshToken(refreshToken.getToken()))
                    .as("Expected to throw RefreshTokenRevokedException for revoked token")
                    .isInstanceOf(RefreshTokenRevokedException.class);
        }

        @Test
        @DisplayName("When verifying refresh token should throw RefreshTokenExpiredException if token is expired")
        public void whenVerifyingRefreshTokenShouldThrowRefreshTokenExpiredExceptionIfTokenIsExpired(){
            refreshToken.setExpiryDate(Instant.now().minusSeconds(100));
            refreshTokenRepository.saveAndFlush(refreshToken);

            assertThatThrownBy(() -> refreshTokenService.verifyAndGetRefreshToken(refreshToken.getToken()))
                    .as("Expected to throw RefreshTokenExpiredException for expired token")
                    .isInstanceOf(RefreshTokenExpiredException.class);
        }

        @Test
        @DisplayName("When verifying refresh token should update lastUsedAt timestamp")
        public void whenVerifyingRefreshTokenShouldUpdateLastUsedAtTimestamp() {
            Instant originalLastUsedAt = refreshToken.getLastUsedAt().minusSeconds(10);
            refreshToken.setLastUsedAt(originalLastUsedAt);
            refreshTokenRepository.saveAndFlush(refreshToken);

            refreshTokenService.verifyAndGetRefreshToken(refreshToken.getToken());

            RefreshToken updatedToken = refreshTokenRepository.findByToken(refreshToken.getToken())
                    .orElseThrow();

            assertThat(updatedToken.getLastUsedAt())
                    .as("Expected lastUsedAt to be updated to a later time")
                    .isAfter(originalLastUsedAt);
        }

        @Test
        @DisplayName("When verifying refresh token should return correct refresh token")
        public void whenVerifyingRefreshTokenShouldReturnCorrectRefreshToken(){
            RefreshToken returnedToken = refreshTokenService.verifyAndGetRefreshToken(refreshToken.getToken());

            assertThat(returnedToken.getToken()).isEqualTo(refreshToken.getToken());
            assertThat(returnedToken.getUser()).isEqualTo(refreshToken.getUser());
            assertThat(returnedToken.getDeviceType()).isEqualTo(refreshToken.getDeviceType());
            assertThat(returnedToken.isRevoked()).isFalse();
            assertThat(returnedToken.isExpired()).isFalse();
        }
    }

    @Nested
    @DisplayName("Revoke refresh token tests:")
    class RevokeRefreshTokenTests {

        private User user;
        private RefreshToken refreshToken;

        @BeforeEach
        void setUp(){
            City cityRzeszow = persistCity(CitiesConstants.WARSAW_NAME);
            user = persistActiveUser(UserTestBuilder.firstUser(), cityRzeszow);

            deviceType = DeviceType.WEB;

            Instant tokenCreateDate = Instant.now();
            refreshToken = persistRefreshToken(
                    user,
                    deviceType,
                    tokenCreateDate,
                    tokenCreateDate.plusMillis(shortRefreshTokenExpiration)
            );
        }

        @Test
        @DisplayName("When revoking refresh token should throw RefreshTokenNotFoundException if token does not exist")
        public void whenRevokingRefreshTokenShouldThrowRefreshTokenNotFoundExceptionIfTokenDoesNotExist(){
            String nonExistentToken = UUID.randomUUID().toString();

            assertThatThrownBy(() -> refreshTokenService.revokeRefreshToken(nonExistentToken))
                    .as("Expected to throw RefreshTokenNotFoundException for non-existent token")
                    .isInstanceOf(RefreshTokenNotFoundException.class);
        }

        @Test
        @DisplayName("When revoking refresh token should mark token as revoked in database")
        public void whenRevokingRefreshTokenShouldMarkTokenAsRevokedInDatabase(){
            assertThat(refreshToken.isRevoked()).as("Token should not be revoked initially").isFalse();

            refreshTokenService.revokeRefreshToken(refreshToken.getToken());

            RefreshToken revokedToken = refreshTokenRepository.findByToken(refreshToken.getToken())
                    .orElseThrow();

            assertThat(revokedToken.isRevoked()).as("Expected token to be marked as revoked").isTrue();
        }

        @Test
        @DisplayName("When revoking refresh token should persist revocation in database")
        public void whenRevokingRefreshTokenShouldPersistRevocationInDatabase(){
            refreshTokenService.revokeRefreshToken(refreshToken.getToken());

            // Clear cache and fetch fresh from database
            refreshTokenRepository.flush();
            RefreshToken persistedToken = refreshTokenRepository.findByToken(refreshToken.getToken())
                    .orElseThrow();

            assertThat(persistedToken.isRevoked())
                    .as("Expected revocation to be persisted in database")
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("Revoke all user tokens tests:")
    class RevokeAllUserTokensTests {

        private User user;

        @BeforeEach
        void setUp() {
            City cityRzeszow = persistCity(CitiesConstants.WARSAW_NAME);
            user = persistActiveUser(UserTestBuilder.firstUser(), cityRzeszow);
        }

        @Test
        @DisplayName("When revoking all user tokens should revoke all tokens for given user")
        public void whenRevokingAllUserTokensShouldRevokeAllTokensForGivenUser(){
            // Create multiple tokens for user with different device types
            RefreshToken webToken = refreshTokenService.createRefreshToken(user, DeviceType.WEB);
            RefreshToken desktopToken = refreshTokenService.createRefreshToken(user, DeviceType.DESKTOP);
            RefreshToken mobileToken = refreshTokenService.createRefreshToken(user, DeviceType.MOBILE_ANDROID);

            refreshTokenService.revokeAllUserTokens(user.getId());

            RefreshToken webTokenAfter = refreshTokenRepository.findByToken(webToken.getToken()).orElseThrow();
            RefreshToken desktopTokenAfter = refreshTokenRepository.findByToken(desktopToken.getToken()).orElseThrow();
            RefreshToken mobileTokenAfter = refreshTokenRepository.findByToken(mobileToken.getToken()).orElseThrow();

            assertThat(webTokenAfter.isRevoked()).isTrue();
            assertThat(desktopTokenAfter.isRevoked()).isTrue();
            assertThat(mobileTokenAfter.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("When revoking all user tokens should not affect other users tokens")
        public void whenRevokingAllUserTokensShouldNotAffectOtherUsersTokens() {
            // Create another user
            City anotherCity = persistCity(CitiesConstants.KRAKOW_NAME);
            User anotherUser = persistActiveUser(
                    UserTestBuilder.secondUser()
                            .firstName("John")
                            .lastName("Doe")
                            .email("john@example.com"),
                    anotherCity
            );

            RefreshToken userToken = refreshTokenService.createRefreshToken(user, DeviceType.WEB);
            RefreshToken anotherUserToken = refreshTokenService.createRefreshToken(anotherUser, DeviceType.WEB);

            refreshTokenService.revokeAllUserTokens(user.getId());

            RefreshToken userTokenAfter = refreshTokenRepository.findByToken(userToken.getToken()).orElseThrow();
            RefreshToken anotherUserTokenAfter = refreshTokenRepository.findByToken(anotherUserToken.getToken()).orElseThrow();

            assertThat(userTokenAfter.isRevoked()).as("First user's token should be revoked").isTrue();
            assertThat(anotherUserTokenAfter.isRevoked()).as("Another user's token should not be revoked").isFalse();
        }

        @Test
        @DisplayName("When revoking all user tokens should not throw any exception if user has no tokens")
        public void whenRevokingAllUserTokensShouldNotThrowExceptionIfUserHasNoTokens(){
            assertThatCode(() -> refreshTokenService.revokeAllUserTokens(user.getId()))
                    .as("Expected to not throw exception when user has no tokens")
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Revoke all user web tokens tests:")
    class RevokeAllUserWebTokensTests {

        private User user;

        @BeforeEach
        void setUp() {
            City cityRzeszow = persistCity(CitiesConstants.WARSAW_NAME);
            user = persistActiveUser(UserTestBuilder.firstUser(), cityRzeszow);
        }

        @Test
        @DisplayName("When revoking all user web tokens should revoke only rotational device type tokens")
        public void whenRevokingAllUserWebTokensShouldRevokeOnlyRotationalDeviceTypeTokens(){
            // Create tokens for rotational devices (WEB, DESKTOP)
            RefreshToken webToken = refreshTokenService.createRefreshToken(user, DeviceType.WEB);
            RefreshToken desktopToken = refreshTokenService.createRefreshToken(user, DeviceType.DESKTOP);

            // Create tokens for non-rotational devices (MOBILE)
            RefreshToken mobileAndroidToken = refreshTokenService.createRefreshToken(user, DeviceType.MOBILE_ANDROID);
            RefreshToken mobileIosToken = refreshTokenService.createRefreshToken(user, DeviceType.MOBILE_IOS);

            refreshTokenService.revokeAllUserWebTokens(user.getId());

            RefreshToken webTokenAfter = refreshTokenRepository.findByToken(webToken.getToken()).orElseThrow();
            RefreshToken desktopTokenAfter = refreshTokenRepository.findByToken(desktopToken.getToken()).orElseThrow();
            RefreshToken mobileAndroidTokenAfter = refreshTokenRepository.findByToken(mobileAndroidToken.getToken()).orElseThrow();
            RefreshToken mobileIosTokenAfter = refreshTokenRepository.findByToken(mobileIosToken.getToken()).orElseThrow();

            assertThat(webTokenAfter.isRevoked()).as("WEB token should be revoked").isTrue();
            assertThat(desktopTokenAfter.isRevoked()).as("DESKTOP token should be revoked").isTrue();
            assertThat(mobileAndroidTokenAfter.isRevoked()).as("MOBILE_ANDROID token should not be revoked").isFalse();
            assertThat(mobileIosTokenAfter.isRevoked()).as("MOBILE_IOS token should not be revoked").isFalse();
        }

        @Test
        @DisplayName("When revoking all user web tokens should not affect other users tokens")
        public void whenRevokingAllUserWebTokensShouldNotAffectOtherUsersTokens() {
            City anotherCity = persistCity(CitiesConstants.KRAKOW_NAME);
            User anotherUser = persistActiveUser(
                    UserTestBuilder.secondUser()
                            .firstName("John")
                            .lastName("Doe")
                            .email("john@example.com"),
                    anotherCity
            );

            RefreshToken userWebToken = refreshTokenService.createRefreshToken(user, DeviceType.WEB);
            RefreshToken anotherUserWebToken = refreshTokenService.createRefreshToken(anotherUser, DeviceType.WEB);

            refreshTokenService.revokeAllUserWebTokens(user.getId());

            RefreshToken userTokenAfter = refreshTokenRepository.findByToken(userWebToken.getToken()).orElseThrow();
            RefreshToken anotherUserTokenAfter = refreshTokenRepository.findByToken(anotherUserWebToken.getToken()).orElseThrow();

            assertThat(userTokenAfter.isRevoked()).as("First user's web token should be revoked").isTrue();
            assertThat(anotherUserTokenAfter.isRevoked()).as("Another user's web token should not be revoked").isFalse();
        }

        @Test
        @DisplayName("When revoking all user web tokens should not throw any exception if user has no web tokens")
        public void whenRevokingAllUserWebTokensShouldNotThrowExceptionIfUserHasNoWebTokens(){
            assertThatCode(() -> refreshTokenService.revokeAllUserWebTokens(user.getId()))
                    .as("Expected to not throw exception when user has no web tokens")
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Revoke all user mobile tokens tests:")
    class RevokeAllUserMobileTokensTests {

        private User user;

        @BeforeEach
        void setUp() {
            City cityRzeszow = persistCity(CitiesConstants.WARSAW_NAME);
            user = persistActiveUser(UserTestBuilder.firstUser(), cityRzeszow);
        }

        @Test
        @DisplayName("When revoking all user mobile tokens should revoke only non-rotational device type tokens")
        public void whenRevokingAllUserMobileTokensShouldRevokeOnlyNonRotationalDeviceTypeTokens() {
            // Create tokens for rotational devices (WEB, DESKTOP)
            RefreshToken webToken = refreshTokenService.createRefreshToken(user, DeviceType.WEB);
            RefreshToken desktopToken = refreshTokenService.createRefreshToken(user, DeviceType.DESKTOP);

            // Create tokens for non-rotational devices (MOBILE)
            RefreshToken mobileAndroidToken = refreshTokenService.createRefreshToken(user, DeviceType.MOBILE_ANDROID);
            RefreshToken mobileIosToken = refreshTokenService.createRefreshToken(user, DeviceType.MOBILE_IOS);

            refreshTokenService.revokeAllUserMobileTokens(user.getId());

            RefreshToken webTokenAfter = refreshTokenRepository.findByToken(webToken.getToken()).orElseThrow();
            RefreshToken desktopTokenAfter = refreshTokenRepository.findByToken(desktopToken.getToken()).orElseThrow();
            RefreshToken mobileAndroidTokenAfter = refreshTokenRepository.findByToken(mobileAndroidToken.getToken()).orElseThrow();
            RefreshToken mobileIosTokenAfter = refreshTokenRepository.findByToken(mobileIosToken.getToken()).orElseThrow();

            assertThat(webTokenAfter.isRevoked()).as("WEB token should not be revoked").isFalse();
            assertThat(desktopTokenAfter.isRevoked()).as("DESKTOP token should not be revoked").isFalse();
            assertThat(mobileAndroidTokenAfter.isRevoked()).as("MOBILE_ANDROID token should be revoked").isTrue();
            assertThat(mobileIosTokenAfter.isRevoked()).as("MOBILE_IOS token should be revoked").isTrue();
        }

        @Test
        @DisplayName("When revoking all user mobile tokens should not affect other users tokens")
        public void whenRevokingAllUserMobileTokensShouldNotAffectOtherUsersTokens(){
            City anotherCity = persistCity(CitiesConstants.KRAKOW_NAME);
            User anotherUser = persistActiveUser(
                    UserTestBuilder.secondUser()
                            .firstName("John")
                            .lastName("Doe")
                            .email("john@example.com"),
                    anotherCity
            );


            RefreshToken userAndroidToken = refreshTokenService.createRefreshToken(user, DeviceType.MOBILE_ANDROID);
            RefreshToken anotherUserAndroidToken = refreshTokenService.createRefreshToken(anotherUser, DeviceType.MOBILE_ANDROID);

            refreshTokenService.revokeAllUserMobileTokens(user.getId());

            RefreshToken userTokenAfter = refreshTokenRepository.findByToken(userAndroidToken.getToken()).orElseThrow();
            RefreshToken anotherUserTokenAfter = refreshTokenRepository.findByToken(anotherUserAndroidToken.getToken()).orElseThrow();

            assertThat(userTokenAfter.isRevoked()).as("First user's Android token should be revoked").isTrue();
            assertThat(anotherUserTokenAfter.isRevoked()).as("Another user's Android token should not be revoked").isFalse();
        }

        @Test
        @DisplayName("When revoking all user mobile tokens should not throw any exception if user has no mobile tokens")
        public void whenRevokingAllUserMobileTokensShouldNotThrowExceptionIfUserHasNoMobileTokens(){
            assertThatCode(() -> refreshTokenService.revokeAllUserMobileTokens(user.getId()))
                    .as("Expected to not throw exception when user has no mobile tokens")
                    .doesNotThrowAnyException();
        }

    }

    @Nested
    @DisplayName("Cleanup expired tokens tests:")
    class CleanupExpiredTokensTests {

        private User user;
        private RefreshToken expiredToken;
        private RefreshToken validToken;

        @BeforeEach
        void setUp() {
            City cityRzeszow = persistCity(CitiesConstants.WARSAW_NAME);
            user = persistActiveUser(UserTestBuilder.firstUser(), cityRzeszow);

            Instant now = Instant.now();

            expiredToken = persistRefreshToken(
                    user,
                    DeviceType.WEB,
                    now.minusSeconds(7200),
                    now.minusSeconds(3600)
            );

            validToken = persistRefreshToken(
                    user,
                    DeviceType.WEB,
                    now.minusSeconds(60),
                    now.plusSeconds(3600)
            );
        }

        @Test
        @DisplayName("When cleaning up expired tokens should delete expired tokens and keep non-expired tokens")
        public void whenCleaningUpExpiredTokensShouldDeleteExpiredTokensAndKeepNonExpiredTokens() {
            refreshTokenService.cleanupExpiredTokens();

            assertThat(refreshTokenRepository.findByToken(expiredToken.getToken()))
                    .as("Expired token should be deleted")
                    .isEmpty();
            assertThat(refreshTokenRepository.findByToken(validToken.getToken()))
                    .as("Non-expired token should remain in database")
                    .isPresent();
        }
    }
}
