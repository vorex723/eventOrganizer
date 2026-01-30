package com.mazurek.eventOrganizer.jwt;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenExpiredException;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenRevokedException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.exception.user.UserRoleNotFoundException;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.RoleRepository;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RefreshTokenServiceIntegrationTest {
    @Value("${jwt.refresh.expiration.short:86400000}")
    private Long shortRefreshTokenExpiration;
    @Value("${jwt.refresh.expiration.long:2592000000}")
    private Long longRefreshTokenExpiration;

    private final String USER_EMAIL = "example@dot.com";
    private final String USER_FIRST_NAME = "Andrew";
    private final String USER_LAST_NAME = "Golota";
    private final String USER_PASSWORD = "Password123!";
    private final String USER_TIME_ZONE = "Europe/Warsaw";
    private final String ROLE_USER_NAME = "ROLE_USER";

    private final String CITY_RZESZOW_NAME = "rzeszow";

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

    @BeforeEach
    void setUp() {
        if (roleRepository.findByName(ROLE_USER_NAME).isEmpty()){
            Role roleUser = new Role(ROLE_USER_NAME);
            roleRepository.save(roleUser);
        }
    }

    @Nested
    @DisplayName("Create refresh token tests:")
    class CreateRefreshTokenTest{

        @BeforeEach
        void setUp(){
            City cityRzeszow = cityRepository.save(new City(CITY_RZESZOW_NAME));
            Role roleUser = roleRepository.findByName(ROLE_USER_NAME)
                    .orElseThrow(UserRoleNotFoundException::new);

            Instant userCreateDateTime = Instant.now();

            User user = userRepository.save(User.builder()
                    .firstName(USER_FIRST_NAME)
                    .lastName(USER_LAST_NAME)
                    .email(USER_EMAIL)
                    .homeCity(cityRzeszow)
                    .password(passwordEncoder.encode(USER_PASSWORD))
                    .createdAt(userCreateDateTime)
                    .timeZone(USER_TIME_ZONE)
                    .roles(new HashSet<>(Set.of(roleUser)))
                    .lastCredentialsChangeTime(userCreateDateTime)
                    .activated(true)
                    .banned(false)
                    .build());

            cityRzeszow.addResident(user);
            cityRepository.save(cityRzeszow);

            deviceType = DeviceType.WEB;
        }

        @AfterEach
        void clean() {
            refreshTokenRepository.deleteAll();
            userRepository.deleteAll();
            cityRepository.deleteAll();
        }

        static Stream<DeviceType> deviceTypes() {
            return Stream.of(DeviceType.values());
        }

        @Test
        @DisplayName("When creating refresh token should create non-revoked and non-expired token")
        public void whenCreatingRefreshTokenShouldCreateNonRevokedAndNonExpiredToken(){
            User user = userRepository.findByIgnoreCaseEmail(USER_EMAIL).orElseThrow();

            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, deviceType);

            assertFalse(refreshToken.isRevoked(), "Expected new token to not be revoked");
            assertFalse(refreshToken.isExpired(), "Expected new token to not be expired");
        }

        @Test
        @DisplayName("When creating refresh token should save new token in database")
        public void whenCreatingRefreshTokenShouldSaveNewTokenInDatabase(){
            User user = userRepository.findByIgnoreCaseEmail(USER_EMAIL).orElseThrow(UserNotFoundException::new);

            RefreshToken returnedRefreshToken = refreshTokenService.createRefreshToken(user, deviceType);
            Optional<RefreshToken> persistedRefreshTokenOptional = refreshTokenRepository.findByToken(returnedRefreshToken.getToken());
            assertTrue(persistedRefreshTokenOptional.isPresent(), "Expected token to be persisted in database.");
            RefreshToken refreshToken = persistedRefreshTokenOptional.get();

            assertEquals(user, refreshToken.getUser(), "Expected to set correct user on refresh token.");
            assertEquals(deviceType, refreshToken.getDeviceType(), "Expected to set correct device type.");
            assertEquals(refreshToken.getCreatedAt(), refreshToken.getLastUsedAt(), "Expected to set the same created at and last used at timestamps.");
            assertTrue(refreshToken.getExpiryDate().isAfter(refreshToken.getCreatedAt()), "Expected expiry date to token to be after it's creation.");
        }

        @ParameterizedTest(name = "Expiration time test for deviceType={0}")
        @MethodSource("deviceTypes")
        @DisplayName("When creating refresh token should set correct expiration of token based on device type")
        public void whenCreatingRefreshTokenShouldSetCorrectExpirationOfTokenBasedOnDeviceType(DeviceType deviceTypeParam){
            User user = userRepository.findByIgnoreCaseEmail(USER_EMAIL).orElseThrow(UserNotFoundException::new);

            RefreshToken returnedRefreshToken = refreshTokenService.createRefreshToken(user, deviceTypeParam);
            long expiryDateMills = returnedRefreshToken.getExpiryDate().toEpochMilli();
            long createDateMills = returnedRefreshToken.getCreatedAt().toEpochMilli();
            if (deviceTypeParam.shouldRotateRefreshToken())
                assertEquals(shortRefreshTokenExpiration, expiryDateMills - createDateMills);
            else {
                assertEquals(longRefreshTokenExpiration, expiryDateMills - createDateMills);
            }
        }
    }

    @Nested
    @DisplayName("Verify and get refresh token tests:")
    class VerifyAndGetRefreshTokenTests{

        private User user;
        private RefreshToken refreshToken;

        @BeforeEach
        void setUp() {
            City cityRzeszow = cityRepository.save(new City(CITY_RZESZOW_NAME));
            Role roleUser = roleRepository.findByName(ROLE_USER_NAME)
                    .orElseThrow(UserRoleNotFoundException::new);

            Instant userCreateDateTime = Instant.now();

            user = userRepository.save(User.builder()
                    .firstName(USER_FIRST_NAME)
                    .lastName(USER_LAST_NAME)
                    .email(USER_EMAIL)
                    .homeCity(cityRzeszow)
                    .password(passwordEncoder.encode(USER_PASSWORD))
                    .createdAt(userCreateDateTime)
                    .timeZone(USER_TIME_ZONE)
                    .roles(new HashSet<>(Set.of(roleUser)))
                    .lastCredentialsChangeTime(userCreateDateTime)
                    .activated(true)
                    .banned(false)
                    .build());

            cityRzeszow.addResident(user);
            cityRepository.save(cityRzeszow);

            deviceType = DeviceType.WEB;

            Instant tokenCreateDate = Instant.now();
            refreshToken = new RefreshToken();
            refreshToken.setToken(UUID.randomUUID().toString());
            refreshToken.setUser(user);
            refreshToken.setDeviceType(deviceType);
            refreshToken.setCreatedAt(tokenCreateDate);
            refreshToken.setExpiryDate(tokenCreateDate.plusMillis(shortRefreshTokenExpiration));
            refreshToken.setLastUsedAt(tokenCreateDate);

            refreshTokenRepository.save(refreshToken);
        }

        @AfterEach
        void clean() {
            refreshTokenRepository.deleteAll();
            userRepository.deleteAll();
            cityRepository.deleteAll();
        }

        @Test
        @DisplayName("When verifying refresh token should throw RefreshTokenNotFoundException if token does not exist")
        public void whenVerifyingRefreshTokenShouldThrowRefreshTokenNotFoundExceptionIfTokenDoesNotExist(){
            String nonExistentToken = UUID.randomUUID().toString();

            assertThrows(RefreshTokenNotFoundException.class,
                    () -> refreshTokenService.verifyAndGetRefreshToken(nonExistentToken),
                    "Expected to throw RefreshTokenNotFoundException for non-existent token");
        }

        @Test
        @DisplayName("When verifying refresh token should throw RefreshTokenRevokedException if token is revoked")
        public void whenVerifyingRefreshTokenShouldThrowRefreshTokenRevokedExceptionIfTokenIsRevoked(){
            refreshToken.setRevoked(true);
            refreshTokenRepository.saveAndFlush(refreshToken);

            assertThrows(RefreshTokenRevokedException.class,
                    () -> refreshTokenService.verifyAndGetRefreshToken(refreshToken.getToken()),
                    "Expected to throw RefreshTokenRevokedException for revoked token");
        }

        @Test
        @DisplayName("When verifying refresh token should throw RefreshTokenExpiredException if token is expired")
        public void whenVerifyingRefreshTokenShouldThrowRefreshTokenExpiredExceptionIfTokenIsExpired(){
            refreshToken.setExpiryDate(Instant.now().minusSeconds(100));
            refreshTokenRepository.saveAndFlush(refreshToken);

            assertThrows(RefreshTokenExpiredException.class,
                    () -> refreshTokenService.verifyAndGetRefreshToken(refreshToken.getToken()),
                    "Expected to throw RefreshTokenExpiredException for expired token");
        }

        @Test
        @DisplayName("When verifying refresh token should update lastUsedAt timestamp")
        public void whenVerifyingRefreshTokenShouldUpdateLastUsedAtTimestamp() throws InterruptedException {
            Instant originalLastUsedAt = refreshToken.getLastUsedAt();

            Thread.sleep(100); // Ensure time difference

            refreshTokenService.verifyAndGetRefreshToken(refreshToken.getToken());

            RefreshToken updatedToken = refreshTokenRepository.findByToken(refreshToken.getToken())
                    .orElseThrow();

            assertTrue(updatedToken.getLastUsedAt().isAfter(originalLastUsedAt),
                    "Expected lastUsedAt to be updated to a later time");
        }

        @Test
        @DisplayName("When verifying refresh token should return correct refresh token")
        public void whenVerifyingRefreshTokenShouldReturnCorrectRefreshToken(){
            RefreshToken returnedToken = refreshTokenService.verifyAndGetRefreshToken(refreshToken.getToken());

            assertAll("Returned token assertions",
                    () -> assertEquals(refreshToken.getToken(), returnedToken.getToken()),
                    () -> assertEquals(refreshToken.getUser(), returnedToken.getUser()),
                    () -> assertEquals(refreshToken.getDeviceType(), returnedToken.getDeviceType()),
                    () -> assertFalse(returnedToken.isRevoked()),
                    () -> assertFalse(returnedToken.isExpired())
            );
        }
    }

    @Nested
    @DisplayName("Revoke refresh token tests:")
    class RevokeRefreshTokenTests{

        private User user;
        private RefreshToken refreshToken;

        @BeforeEach
        void setUp(){
            City cityRzeszow = cityRepository.save(new City(CITY_RZESZOW_NAME));
            Role roleUser = roleRepository.findByName(ROLE_USER_NAME)
                    .orElseThrow(UserRoleNotFoundException::new);

            Instant userCreateDateTime = Instant.now();

            user = userRepository.save(User.builder()
                    .firstName(USER_FIRST_NAME)
                    .lastName(USER_LAST_NAME)
                    .email(USER_EMAIL)
                    .homeCity(cityRzeszow)
                    .password(passwordEncoder.encode(USER_PASSWORD))
                    .createdAt(userCreateDateTime)
                    .timeZone(USER_TIME_ZONE)
                    .roles(new HashSet<>(Set.of(roleUser)))
                    .lastCredentialsChangeTime(userCreateDateTime)
                    .activated(true)
                    .banned(false)
                    .build());

            cityRzeszow.addResident(user);
            cityRepository.save(cityRzeszow);

            deviceType = DeviceType.WEB;

            Instant tokenCreateDate = Instant.now();
            refreshToken = new RefreshToken();
            refreshToken.setToken(UUID.randomUUID().toString());
            refreshToken.setUser(user);
            refreshToken.setDeviceType(deviceType);
            refreshToken.setCreatedAt(tokenCreateDate);
            refreshToken.setExpiryDate(tokenCreateDate.plusMillis(shortRefreshTokenExpiration));
            refreshToken.setLastUsedAt(tokenCreateDate);

            refreshTokenRepository.save(refreshToken);
        }

        @AfterEach
        void clean() {
            refreshTokenRepository.deleteAll();
            userRepository.deleteAll();
            cityRepository.deleteAll();
        }

        @Test
        @DisplayName("When revoking refresh token should throw RefreshTokenNotFoundException if token does not exist")
        public void whenRevokingRefreshTokenShouldThrowRefreshTokenNotFoundExceptionIfTokenDoesNotExist(){
            String nonExistentToken = UUID.randomUUID().toString();

            assertThrows(RefreshTokenNotFoundException.class,
                    () -> refreshTokenService.revokeRefreshToken(nonExistentToken),
                    "Expected to throw RefreshTokenNotFoundException for non-existent token");
        }

        @Test
        @DisplayName("When revoking refresh token should mark token as revoked in database")
        public void whenRevokingRefreshTokenShouldMarkTokenAsRevokedInDatabase(){
            assertFalse(refreshToken.isRevoked(), "Token should not be revoked initially");

            refreshTokenService.revokeRefreshToken(refreshToken.getToken());

            RefreshToken revokedToken = refreshTokenRepository.findByToken(refreshToken.getToken())
                    .orElseThrow();

            assertTrue(revokedToken.isRevoked(), "Expected token to be marked as revoked");
        }

        @Test
        @DisplayName("When revoking refresh token should persist revocation in database")
        public void whenRevokingRefreshTokenShouldPersistRevocationInDatabase(){
            refreshTokenService.revokeRefreshToken(refreshToken.getToken());

            // Clear cache and fetch fresh from database
            refreshTokenRepository.flush();
            RefreshToken persistedToken = refreshTokenRepository.findByToken(refreshToken.getToken())
                    .orElseThrow();

            assertTrue(persistedToken.isRevoked(),
                    "Expected revocation to be persisted in database");
        }
    }

    @Nested
    @DisplayName("Revoke all user tokens tests:")
    class RevokeAllUserTokensTests{

        private User user;

        @BeforeEach
        void setUp() {
            City cityRzeszow = cityRepository.save(new City(CITY_RZESZOW_NAME));
            Role roleUser = roleRepository.findByName(ROLE_USER_NAME)
                    .orElseThrow(UserRoleNotFoundException::new);

            Instant userCreateDateTime = Instant.now();

            user = userRepository.save(User.builder()
                    .firstName(USER_FIRST_NAME)
                    .lastName(USER_LAST_NAME)
                    .email(USER_EMAIL)
                    .homeCity(cityRzeszow)
                    .password(passwordEncoder.encode(USER_PASSWORD))
                    .createdAt(userCreateDateTime)
                    .timeZone(USER_TIME_ZONE)
                    .roles(new HashSet<>(Set.of(roleUser)))
                    .lastCredentialsChangeTime(userCreateDateTime)
                    .activated(true)
                    .banned(false)
                    .build());

            cityRzeszow.addResident(user);
            cityRepository.save(cityRzeszow);
        }

        @AfterEach
        void clean() {
            refreshTokenRepository.deleteAll();
            userRepository.deleteAll();
            cityRepository.deleteAll();
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

            assertAll("All tokens should be revoked",
                    () -> assertTrue(webTokenAfter.isRevoked()),
                    () -> assertTrue(desktopTokenAfter.isRevoked()),
                    () -> assertTrue(mobileTokenAfter.isRevoked())
            );
        }

        @Test
        @DisplayName("When revoking all user tokens should not affect other users tokens")
        public void whenRevokingAllUserTokensShouldNotAffectOtherUsersTokens() {
            // Create another user
            City anotherCity = cityRepository.save(new City("krakow"));
            Role roleUser = roleRepository.findByName(ROLE_USER_NAME).orElseThrow(UserRoleNotFoundException::new);

            User anotherUser = userRepository.save(User.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("john@example.com")
                    .homeCity(anotherCity)
                    .password(passwordEncoder.encode(USER_PASSWORD))
                    .createdAt(Instant.now())
                    .timeZone(USER_TIME_ZONE)
                    .roles(new HashSet<>(Set.of(roleUser)))
                    .lastCredentialsChangeTime(Instant.now())
                    .activated(true)
                    .banned(false)
                    .build());

            RefreshToken userToken = refreshTokenService.createRefreshToken(user, DeviceType.WEB);
            RefreshToken anotherUserToken = refreshTokenService.createRefreshToken(anotherUser, DeviceType.WEB);

            refreshTokenService.revokeAllUserTokens(user.getId());

            RefreshToken userTokenAfter = refreshTokenRepository.findByToken(userToken.getToken()).orElseThrow();
            RefreshToken anotherUserTokenAfter = refreshTokenRepository.findByToken(anotherUserToken.getToken()).orElseThrow();

            assertTrue(userTokenAfter.isRevoked(), "First user's token should be revoked");
            assertFalse(anotherUserTokenAfter.isRevoked(), "Another user's token should not be revoked");
        }

        @Test
        @DisplayName("When revoking all user tokens should not throw exception if user has no tokens")
        public void whenRevokingAllUserTokensShouldNotThrowExceptionIfUserHasNoTokens(){
            assertDoesNotThrow(() -> refreshTokenService.revokeAllUserTokens(user.getId()),
                    "Expected to not throw exception when user has no tokens");
        }
    }

    @Nested
    @DisplayName("Revoke all user web tokens tests:")
    class RevokeAllUserWebTokensTests{

        private User user;

        @BeforeEach
        void setUp() {
            City cityRzeszow = cityRepository.save(new City(CITY_RZESZOW_NAME));
            Role roleUser = roleRepository.findByName(ROLE_USER_NAME)
                    .orElseThrow(UserRoleNotFoundException::new);

            Instant userCreateDateTime = Instant.now();

            user = userRepository.save(User.builder()
                    .firstName(USER_FIRST_NAME)
                    .lastName(USER_LAST_NAME)
                    .email(USER_EMAIL)
                    .homeCity(cityRzeszow)
                    .password(passwordEncoder.encode(USER_PASSWORD))
                    .createdAt(userCreateDateTime)
                    .timeZone(USER_TIME_ZONE)
                    .roles(new HashSet<>(Set.of(roleUser)))
                    .lastCredentialsChangeTime(userCreateDateTime)
                    .activated(true)
                    .banned(false)
                    .build());

            cityRzeszow.addResident(user);
            cityRepository.save(cityRzeszow);
        }

        @AfterEach
        void clean() {
            refreshTokenRepository.deleteAll();
            userRepository.deleteAll();
            cityRepository.deleteAll();
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

            assertAll("Rotational tokens should be revoked, non-rotational should not",
                    () -> assertTrue(webTokenAfter.isRevoked(), "WEB token should be revoked"),
                    () -> assertTrue(desktopTokenAfter.isRevoked(), "DESKTOP token should be revoked"),
                    () -> assertFalse(mobileAndroidTokenAfter.isRevoked(), "MOBILE_ANDROID token should not be revoked"),
                    () -> assertFalse(mobileIosTokenAfter.isRevoked(), "MOBILE_IOS token should not be revoked")
            );
        }

        @Test
        @DisplayName("When revoking all user web tokens should not affect other users tokens")
        public void whenRevokingAllUserWebTokensShouldNotAffectOtherUsersTokens() {
            City anotherCity = cityRepository.save(new City("krakow"));
            Role roleUser = roleRepository.findByName(ROLE_USER_NAME).orElseThrow(UserNotFoundException::new);

            User anotherUser = userRepository.save(User.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("john@example.com")
                    .homeCity(anotherCity)
                    .password(passwordEncoder.encode(USER_PASSWORD))
                    .createdAt(Instant.now())
                    .timeZone(USER_TIME_ZONE)
                    .roles(new HashSet<>(Set.of(roleUser)))
                    .lastCredentialsChangeTime(Instant.now())
                    .activated(true)
                    .banned(false)
                    .build());

            RefreshToken userWebToken = refreshTokenService.createRefreshToken(user, DeviceType.WEB);
            RefreshToken anotherUserWebToken = refreshTokenService.createRefreshToken(anotherUser, DeviceType.WEB);

            refreshTokenService.revokeAllUserWebTokens(user.getId());

            RefreshToken userTokenAfter = refreshTokenRepository.findByToken(userWebToken.getToken()).orElseThrow();
            RefreshToken anotherUserTokenAfter = refreshTokenRepository.findByToken(anotherUserWebToken.getToken()).orElseThrow();

            assertTrue(userTokenAfter.isRevoked(), "First user's web token should be revoked");
            assertFalse(anotherUserTokenAfter.isRevoked(), "Another user's web token should not be revoked");
        }

        @Test
        @DisplayName("When revoking all user web tokens should not throw exception if user has no web tokens")
        public void whenRevokingAllUserWebTokensShouldNotThrowExceptionIfUserHasNoWebTokens(){
            assertDoesNotThrow(() -> refreshTokenService.revokeAllUserWebTokens(user.getId()),
                    "Expected to not throw exception when user has no web tokens");
        }
    }

    @Nested
    @DisplayName("Revoke all user mobile tokens tests:")
    class RevokeAllUserMobileTokensTests {

        private User user;

        @BeforeEach
        void setUp() {
            City cityRzeszow = cityRepository.save(new City(CITY_RZESZOW_NAME));
            Role roleUser = roleRepository.findByName(ROLE_USER_NAME)
                    .orElseThrow(UserRoleNotFoundException::new);

            Instant userCreateDateTime = Instant.now();

            user = userRepository.save(User.builder()
                    .firstName(USER_FIRST_NAME)
                    .lastName(USER_LAST_NAME)
                    .email(USER_EMAIL)
                    .homeCity(cityRzeszow)
                    .password(passwordEncoder.encode(USER_PASSWORD))
                    .createdAt(userCreateDateTime)
                    .timeZone(USER_TIME_ZONE)
                    .roles(new HashSet<>(Set.of(roleUser)))
                    .lastCredentialsChangeTime(userCreateDateTime)
                    .activated(true)
                    .banned(false)
                    .build());

            cityRzeszow.addResident(user);
            cityRepository.save(cityRzeszow);
        }

        @AfterEach
        void clean() {
            refreshTokenRepository.deleteAll();
            userRepository.deleteAll();
            cityRepository.deleteAll();
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

            assertAll("Non-rotational tokens should be revoked, rotational should not",
                    () -> assertFalse(webTokenAfter.isRevoked(), "WEB token should not be revoked"),
                    () -> assertFalse(desktopTokenAfter.isRevoked(), "DESKTOP token should not be revoked"),
                    () -> assertTrue(mobileAndroidTokenAfter.isRevoked(), "MOBILE_ANDROID token should be revoked"),
                    () -> assertTrue(mobileIosTokenAfter.isRevoked(), "MOBILE_IOS token should be revoked")
            );
        }

        @Test
        @DisplayName("When revoking all user mobile tokens should not affect other users tokens")
        public void whenRevokingAllUserMobileTokensShouldNotAffectOtherUsersTokens(){
            City anotherCity = cityRepository.save(new City("krakow"));
            Role roleUser = roleRepository.findByName(ROLE_USER_NAME).orElseThrow(UserRoleNotFoundException::new);

            User anotherUser = userRepository.save(User.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("john@example.com")
                    .homeCity(anotherCity)
                    .password(passwordEncoder.encode(USER_PASSWORD))
                    .createdAt(Instant.now())
                    .timeZone(USER_TIME_ZONE)
                    .roles(new HashSet<>(Set.of(roleUser)))
                    .lastCredentialsChangeTime(Instant.now())
                    .activated(true)
                    .banned(false)
                    .build());


            RefreshToken userAndroidToken = refreshTokenService.createRefreshToken(user, DeviceType.MOBILE_ANDROID);
            RefreshToken anotherUserAndroidToken = refreshTokenService.createRefreshToken(anotherUser, DeviceType.MOBILE_ANDROID);

            refreshTokenService.revokeAllUserMobileTokens(user.getId());

            RefreshToken userTokenAfter = refreshTokenRepository.findByToken(userAndroidToken.getToken()).orElseThrow();
            RefreshToken anotherUserTokenAfter = refreshTokenRepository.findByToken(anotherUserAndroidToken.getToken()).orElseThrow();

            assertTrue(userTokenAfter.isRevoked(), "First user's Android token should be revoked");
            assertFalse(anotherUserTokenAfter.isRevoked(), "Another user's Android token should not be revoked");
        }

        @Test
        @DisplayName("When revoking all user mobile tokens should not throw exception if user has no mobile tokens")
        public void whenRevokingAllUserMobileTokensShouldNotThrowExceptionIfUserHasNoMobileTokens(){
            assertDoesNotThrow(() -> refreshTokenService.revokeAllUserMobileTokens(user.getId()),
                    "Expected to not throw exception when user has no mobile tokens");
        }

    }
}
