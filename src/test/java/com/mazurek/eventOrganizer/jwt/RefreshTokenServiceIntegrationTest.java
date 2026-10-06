package com.mazurek.eventOrganizer.jwt;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenExpiredException;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenRevokedException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.exception.user.UserRoleNotFoundException;
import com.mazurek.eventOrganizer.testData.TestPersistenceQueries;
import com.mazurek.eventOrganizer.testData.builders.CityTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.IssuedRefreshTokenTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.RefreshTokenTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.RoleTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.RoleRepository;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("RefreshTokenService integration tests:")
public class RefreshTokenServiceIntegrationTest {

    @Autowired
    private TestPersistenceQueries testPersistenceQueries;
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
        String externalId = com.mazurek.eventOrganizer.testData.TestCityData.externalId(cityName);
        return cityRepository.findByExternalId(externalId)
                .orElseGet(() -> cityRepository.save(new CityTestBuilder()
                        .id(null)
                        .externalId(externalId)
                        .name(cityName.toLowerCase(Locale.ROOT))
                        .build()));
    }

    private User persistActiveUser(UserTestBuilder userBuilder, City city) {
        Instant userCreateDateTime = TimeConstants.ONE_WEEK_AGO;

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

    private IssuedRefreshToken persistRefreshToken(User user, DeviceType deviceType, Instant createdAt, Instant expiryDate) {
        return persistRefreshToken(user, UUID.randomUUID().toString(), deviceType, createdAt, expiryDate, false);
    }

    private IssuedRefreshToken persistRefreshToken(User user, String rawToken, DeviceType deviceType, Instant createdAt, Instant expiryDate, boolean revoked) {
        RefreshToken saved = refreshTokenRepository.save(RefreshTokenTestBuilder.firstRefreshTokenForUser(user)
                .id(null)
                .rawToken(rawToken)
                .deviceType(deviceType)
                .createdAt(createdAt)
                .lastUsedAt(createdAt)
                .expiryDate(expiryDate)
                .revoked(revoked)
                .build());
        return new IssuedRefreshTokenTestBuilder()
                .refreshToken(saved)
                .rawToken(rawToken)
                .build();
    }

    private Optional<RefreshToken> findByHashOf(String rawToken) {
        return testPersistenceQueries.findRefreshTokenByHash(RefreshTokenHash.sha256(rawToken));
    }

    @Nested
    @DisplayName("Create refresh token tests:")
    class CreateRefreshTokenTests {

        @BeforeEach
        void setUp(){
            City cityWarsaw = persistCity(CitiesConstants.WARSAW_NAME);
            persistActiveUser(UserTestBuilder.firstUser(), cityWarsaw);

            deviceType = DeviceType.WEB;
        }

        static Stream<DeviceType> deviceTypes() {
            return Stream.of(DeviceType.values());
        }

        @Test
        @DisplayName("When creating refresh token should create non-revoked and non-expired token")
        public void whenCreatingRefreshTokenShouldCreateNonRevokedAndNonExpiredToken(){
            User user = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL), "Expected persisted user in whenCreatingRefreshTokenShouldCreateNonRevokedAndNonExpiredToken");

            RefreshToken refreshToken = refreshTokenService.issueRefreshToken(user, deviceType).refreshToken();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(refreshToken.isRevoked()).as("Expected new token to not be revoked").isFalse();
                softly.assertThat(refreshToken.isExpired(TimeConstants.NOW)).as("Expected new token to not be expired").isFalse();
            });
        }

        @Test
        @DisplayName("When creating refresh token should save new token in database")
        public void whenCreatingRefreshTokenShouldSaveNewTokenInDatabase(){
            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);

            IssuedRefreshToken returnedRefreshToken = refreshTokenService.issueRefreshToken(user, deviceType);
            Optional<RefreshToken> persistedRefreshTokenOptional = findByHashOf(returnedRefreshToken.rawToken());
            assertThat(persistedRefreshTokenOptional).as("Expected token to be persisted in database.").isPresent();
            RefreshToken refreshToken = persistedRefreshTokenOptional.get();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(refreshToken.getUser()).as("Expected to set correct user on refresh token.").isEqualTo(user);
                softly.assertThat(refreshToken.getDeviceType()).as("Expected to set correct device type.").isEqualTo(deviceType);
                softly.assertThat(refreshToken.getLastUsedAt())
                        .as("Expected to set the same created at and last used at timestamps.")
                        .isEqualTo(refreshToken.getCreatedAt());
                softly.assertThat(refreshToken.getExpiryDate())
                        .as("Expected expiry date to token to be after it's creation.")
                        .isAfter(refreshToken.getCreatedAt());
            });
        }

        @ParameterizedTest(name = "Expiration time test for deviceType={0}")
        @MethodSource("deviceTypes")
        @DisplayName("When creating refresh token should set correct expiration of token based on device type")
        public void whenCreatingRefreshTokenShouldSetCorrectExpirationOfTokenBasedOnDeviceType(DeviceType deviceTypeParam){
            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);

            RefreshToken returnedRefreshToken = refreshTokenService.issueRefreshToken(user, deviceTypeParam).refreshToken();
            long expectedExpiration = switch (deviceTypeParam) {
                case WEB, DESKTOP, UNKNOWN -> shortRefreshTokenExpiration;
                case MOBILE_ANDROID, MOBILE_IOS, MOBILE_OTHER, TABLET_ANDROID, TABLET_IOS, TABLET_OTHER -> longRefreshTokenExpiration;
            };
            assertThat(returnedRefreshToken.getCreatedAt()).isEqualTo(TimeConstants.NOW);
            assertThat(returnedRefreshToken.getLastUsedAt()).isEqualTo(TimeConstants.NOW);
            assertThat(returnedRefreshToken.getExpiryDate()).isEqualTo(TimeConstants.NOW.plusMillis(expectedExpiration));
        }

        @ParameterizedTest(name = "Timestamp round-trip for deviceType={0}")
        @MethodSource("deviceTypes")
        void whenReloadingRefreshTokenShouldPreserveMicrosecondTimestampsAndInclusiveExpiry(DeviceType deviceTypeParam) {
            User user = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL), "Expected persisted user in whenReloadingRefreshTokenShouldPreserveMicrosecondTimestampsAndInclusiveExpiry");
            // All values fit PostgreSQL timestamp(6); arbitrary nanoseconds are not a DB contract.
            Instant createdAt = TimeConstants.NOW.plusNanos(123_456_000);
            Instant lastUsedAt = createdAt.plusMillis(10);
            Instant expiryDate = createdAt.plusMillis(60_001);
            RefreshToken fixture = RefreshTokenTestBuilder.firstRefreshTokenForUser(user)
                    .id(null)
                    .deviceType(deviceTypeParam)
                    .createdAt(createdAt)
                    .lastUsedAt(lastUsedAt)
                    .expiryDate(expiryDate)
                    .build();
            Long id = refreshTokenRepository.saveAndFlush(fixture).getId();

            // No test transaction: this lookup starts a new persistence context after the commit.
            RefreshToken stored = requirePresent(refreshTokenRepository.findById(id), "Expected persisted refresh token in whenReloadingRefreshTokenShouldPreserveMicrosecondTimestampsAndInclusiveExpiry");

            assertThat(stored).isNotSameAs(fixture);
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(stored.getCreatedAt()).isEqualTo(createdAt);
                softly.assertThat(stored.getLastUsedAt()).isEqualTo(lastUsedAt);
                softly.assertThat(stored.getExpiryDate()).isEqualTo(expiryDate);
                softly.assertThat(stored.isExpired(expiryDate.minusNanos(1))).isFalse();
                softly.assertThat(stored.isExpired(expiryDate)).isTrue();
                softly.assertThat(stored.isExpired(expiryDate.plusNanos(1))).isTrue();
            });
        }
    }

    @Nested
    @DisplayName("Use refresh token tests:")
    class UseRefreshTokenTests {

        private User user;
        private RefreshToken refreshToken;
        private String rawToken;

        @BeforeEach
        void setUp() {
            City cityWarsaw = persistCity(CitiesConstants.WARSAW_NAME);
            user = persistActiveUser(UserTestBuilder.firstUser(), cityWarsaw);

            deviceType = DeviceType.MOBILE_ANDROID;

            Instant tokenCreateDate = TimeConstants.NOW;
            IssuedRefreshToken issued = persistRefreshToken(
                    user,
                    deviceType,
                    tokenCreateDate,
                    tokenCreateDate.plusMillis(longRefreshTokenExpiration)
            );
            refreshToken = issued.refreshToken();
            rawToken = issued.rawToken();
        }

        @Test
        @DisplayName("When using refresh token should throw RefreshTokenNotFoundException if token does not exist")
        public void whenUsingRefreshTokenShouldThrowRefreshTokenNotFoundExceptionIfTokenDoesNotExist(){
            String nonExistentToken = UUID.randomUUID().toString();

            assertThatThrownBy(() -> refreshTokenService.useRefreshToken(nonExistentToken))
                    .as("Expected to throw RefreshTokenNotFoundException for non-existent token")
                    .isInstanceOf(RefreshTokenNotFoundException.class);
            assertStoredTokenUnchanged();
        }

        @Test
        @DisplayName("When using refresh token should throw RefreshTokenRevokedException if token is revoked")
        public void whenUsingRefreshTokenShouldThrowRefreshTokenRevokedExceptionIfTokenIsRevoked(){
            refreshToken.setRevoked(true);
            refreshTokenRepository.saveAndFlush(refreshToken);

            assertThatThrownBy(() -> refreshTokenService.useRefreshToken(rawToken))
                    .as("Expected to throw RefreshTokenRevokedException for revoked token")
                    .isInstanceOf(RefreshTokenRevokedException.class);
            assertStoredTokenUnchanged();
        }

        @Test
        @DisplayName("When using refresh token should throw RefreshTokenExpiredException if token is expired")
        public void whenUsingRefreshTokenShouldThrowRefreshTokenExpiredExceptionIfTokenIsExpired(){
            refreshToken.setExpiryDate(TimeConstants.ONE_HOUR_AGO);
            refreshTokenRepository.saveAndFlush(refreshToken);

            assertThatThrownBy(() -> refreshTokenService.useRefreshToken(rawToken))
                    .as("Expected to throw RefreshTokenExpiredException for expired token")
                    .isInstanceOf(RefreshTokenExpiredException.class);
            assertStoredTokenUnchanged();
        }

        private void assertStoredTokenUnchanged() {
            // The fixture is detached; reload in a new persistence context after rejection.
            RefreshToken stored = requirePresent(findByHashOf(rawToken), "Expected original mobile token after rejected use");
            assertThat(stored).isNotSameAs(refreshToken);
            assertThat(stored)
                    .extracting(RefreshToken::getId, RefreshToken::getTokenHash, RefreshToken::getFamilyId,
                            RefreshToken::getDeviceType, RefreshToken::getCreatedAt,
                            RefreshToken::getLastUsedAt, RefreshToken::getExpiryDate, RefreshToken::isRevoked)
                    .containsExactly(refreshToken.getId(), refreshToken.getTokenHash(), refreshToken.getFamilyId(),
                            refreshToken.getDeviceType(), refreshToken.getCreatedAt(),
                            refreshToken.getLastUsedAt(), refreshToken.getExpiryDate(), refreshToken.isRevoked());
            assertThat(refreshTokenRepository.count()).isEqualTo(1);
        }

        @ParameterizedTest(name = "Expiry offset from fixed clock: {0} ns")
        @ValueSource(longs = {-1_000, 0, 1_000})
        void whenUsingTokenAtMicrosecondExpiryBoundaryShouldRespectInclusiveExpiration(long expiryOffsetNanos) {
            refreshToken.setCreatedAt(TimeConstants.ONE_HOUR_AGO);
            refreshToken.setLastUsedAt(TimeConstants.ONE_HOUR_AGO);
            Instant expiration = TimeConstants.NOW.plusNanos(expiryOffsetNanos);
            refreshToken.setExpiryDate(expiration);
            refreshTokenRepository.saveAndFlush(refreshToken);
            assertThat(requirePresent(findByHashOf(rawToken), "Expected persisted refresh token in whenUsingTokenAtMicrosecondExpiryBoundaryShouldRespectInclusiveExpiration").getExpiryDate()).isEqualTo(expiration);

            if (expiryOffsetNanos <= 0) {
                assertThatThrownBy(() -> refreshTokenService.useRefreshToken(rawToken))
                        .isInstanceOf(RefreshTokenExpiredException.class);
                assertStoredTokenUnchanged();
            } else {
                refreshTokenService.useRefreshToken(rawToken);

                RefreshToken stored = requirePresent(findByHashOf(rawToken), "Expected persisted refresh token in whenUsingTokenAtMicrosecondExpiryBoundaryShouldRespectInclusiveExpiration");
                assertThat(stored.getLastUsedAt()).isEqualTo(TimeConstants.NOW);
                assertThat(stored.isExpired(TimeConstants.NOW)).isFalse();
            }
        }

        @Test
        @DisplayName("When using a mobile refresh token should update lastUsedAt timestamp")
        public void whenUsingMobileRefreshTokenShouldUpdateLastUsedAtTimestamp() {
            Instant originalLastUsedAt = refreshToken.getLastUsedAt().minusSeconds(10);
            refreshToken.setLastUsedAt(originalLastUsedAt);
            refreshTokenRepository.saveAndFlush(refreshToken);

            refreshTokenService.useRefreshToken(rawToken);

            RefreshToken updatedToken = requirePresent(findByHashOf(rawToken), "Expected persisted refresh token in whenUsingMobileRefreshTokenShouldUpdateLastUsedAtTimestamp");

            assertThat(updatedToken.getLastUsedAt())
                    .as("Expected lastUsedAt to be updated to a later time")
                    .isAfter(originalLastUsedAt)
                    .isEqualTo(TimeConstants.NOW);
        }

        @Test
        @DisplayName("When using a mobile refresh token should return the same credential")
        public void whenUsingMobileRefreshTokenShouldReturnSameCredential(){
            RefreshTokenUse result = refreshTokenService.useRefreshToken(rawToken);
            RefreshToken returnedToken = result.refreshToken();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.rawToken()).isEqualTo(rawToken);
                softly.assertThat(returnedToken.getTokenHash()).isEqualTo(refreshToken.getTokenHash());
                softly.assertThat(returnedToken.getUser()).isEqualTo(refreshToken.getUser());
                softly.assertThat(returnedToken.getDeviceType()).isEqualTo(refreshToken.getDeviceType());
                softly.assertThat(returnedToken.isRevoked()).isFalse();
                softly.assertThat(returnedToken.isExpired(TimeConstants.NOW)).isFalse();
            });
        }

        @Test
        @DisplayName("When using a web refresh token should rotate it within the same family")
        public void whenUsingWebRefreshTokenShouldRotateWithinSameFamily() {
            IssuedRefreshToken webToken = persistRefreshToken(
                    user, DeviceType.WEB, TimeConstants.NOW,
                    TimeConstants.NOW.plusMillis(shortRefreshTokenExpiration));

            RefreshTokenUse result = refreshTokenService.useRefreshToken(webToken.rawToken());

            assertThat(result.rawToken()).isNotEqualTo(webToken.rawToken());
            assertThat(result.refreshToken().getTokenHash()).isEqualTo(RefreshTokenHash.sha256(result.rawToken()));
            assertThat(result.refreshToken().getFamilyId()).isEqualTo(webToken.refreshToken().getFamilyId());
            assertThat(requirePresent(findByHashOf(webToken.rawToken()), "Expected persisted refresh token in whenUsingWebRefreshTokenShouldRotateWithinSameFamily").isRevoked()).isTrue();
            assertThat(result.refreshToken().isRevoked()).isFalse();
            RefreshToken storedSuccessor = requirePresent(findByHashOf(result.rawToken()), "Expected persisted web successor after rotation");
            assertThat(storedSuccessor).isNotSameAs(result.refreshToken());
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(storedSuccessor.getTokenHash()).isEqualTo(result.refreshToken().getTokenHash());
                softly.assertThat(storedSuccessor.getFamilyId()).isEqualTo(webToken.refreshToken().getFamilyId());
                softly.assertThat(storedSuccessor.getUser().getId()).isEqualTo(user.getId());
                softly.assertThat(storedSuccessor.getDeviceType()).isEqualTo(DeviceType.WEB);
                softly.assertThat(storedSuccessor.getCreatedAt()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(storedSuccessor.getLastUsedAt()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(storedSuccessor.getExpiryDate()).isEqualTo(TimeConstants.NOW.plusMillis(shortRefreshTokenExpiration));
                softly.assertThat(storedSuccessor.isRevoked()).isFalse();
            });
        }
    }

    @Nested
    @DisplayName("Revoke refresh token tests:")
    class RevokeRefreshTokenTests {

        private User user;
        private RefreshToken refreshToken;
        private String rawToken;

        @BeforeEach
        void setUp(){
            City cityWarsaw = persistCity(CitiesConstants.WARSAW_NAME);
            user = persistActiveUser(UserTestBuilder.firstUser(), cityWarsaw);

            deviceType = DeviceType.WEB;

            Instant tokenCreateDate = TimeConstants.NOW;
            IssuedRefreshToken issued = persistRefreshToken(
                    user,
                    deviceType,
                    tokenCreateDate,
                    tokenCreateDate.plusMillis(shortRefreshTokenExpiration)
            );
            refreshToken = issued.refreshToken();
            rawToken = issued.rawToken();
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

            RefreshToken returnedToken = refreshTokenService.revokeRefreshToken(rawToken);

            RefreshToken revokedToken = requirePresent(findByHashOf(rawToken), "Expected persisted refresh token in whenRevokingRefreshTokenShouldMarkTokenAsRevokedInDatabase");

            assertThat(revokedToken.isRevoked()).as("Expected token to be marked as revoked").isTrue();
            assertThat(returnedToken.getUser().getId()).isEqualTo(user.getId());
            assertThat(returnedToken.getTokenHash()).isEqualTo(RefreshTokenHash.sha256(rawToken));
            assertThat(returnedToken.getTokenHash()).isNotEqualTo(rawToken);
        }

        @Test
        @DisplayName("When revoking an already revoked token should return the same user's token")
        public void whenRevokingAlreadyRevokedTokenShouldRemainIdempotent() {
            refreshTokenService.revokeRefreshToken(rawToken);

            RefreshToken returnedToken = refreshTokenService.revokeRefreshToken(rawToken);

            assertThat(returnedToken.isRevoked()).isTrue();
            assertThat(returnedToken.getUser().getId()).isEqualTo(user.getId());
        }

        @Test
        @DisplayName("When revoking refresh token should persist revocation in database")
        public void whenRevokingRefreshTokenShouldPersistRevocationInDatabase(){
            refreshTokenService.revokeRefreshToken(rawToken);

            // No test transaction: the repository query reloads after the service commit.
            RefreshToken persistedToken = requirePresent(findByHashOf(rawToken), "Expected persisted refresh token in whenRevokingRefreshTokenShouldPersistRevocationInDatabase");

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
            City cityWarsaw = persistCity(CitiesConstants.WARSAW_NAME);
            user = persistActiveUser(UserTestBuilder.firstUser(), cityWarsaw);
        }

        @Test
        @DisplayName("When revoking all user tokens should revoke all tokens for given user")
        public void whenRevokingAllUserTokensShouldRevokeAllTokensForGivenUser(){
            // Create multiple tokens for user with different device types
            IssuedRefreshToken webToken = refreshTokenService.issueRefreshToken(user, DeviceType.WEB);
            IssuedRefreshToken desktopToken = refreshTokenService.issueRefreshToken(user, DeviceType.DESKTOP);
            IssuedRefreshToken mobileToken = refreshTokenService.issueRefreshToken(user, DeviceType.MOBILE_ANDROID);

            refreshTokenService.revokeAllUserTokens(user.getId());

            RefreshToken webTokenAfter = requirePresent(findByHashOf(webToken.rawToken()), "Expected persisted refresh token in whenRevokingAllUserTokensShouldRevokeAllTokensForGivenUser");
            RefreshToken desktopTokenAfter = requirePresent(findByHashOf(desktopToken.rawToken()), "Expected persisted refresh token in whenRevokingAllUserTokensShouldRevokeAllTokensForGivenUser");
            RefreshToken mobileTokenAfter = requirePresent(findByHashOf(mobileToken.rawToken()), "Expected persisted refresh token in whenRevokingAllUserTokensShouldRevokeAllTokensForGivenUser");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(webTokenAfter.isRevoked()).isTrue();
                softly.assertThat(desktopTokenAfter.isRevoked()).isTrue();
                softly.assertThat(mobileTokenAfter.isRevoked()).isTrue();
            });
        }

        @Test
        @DisplayName("When revoking all user tokens should not affect other users tokens")
        public void whenRevokingAllUserTokensShouldNotAffectOtherUsersTokens() {
            // Create another user
            City anotherCity = persistCity(CitiesConstants.KRAKOW_NAME);
            User anotherUser = persistActiveUser(
                    UserTestBuilder.thirdUser(),
                    anotherCity
            );

            IssuedRefreshToken userToken = refreshTokenService.issueRefreshToken(user, DeviceType.WEB);
            IssuedRefreshToken anotherUserToken = refreshTokenService.issueRefreshToken(anotherUser, DeviceType.WEB);

            refreshTokenService.revokeAllUserTokens(user.getId());

            RefreshToken userTokenAfter = requirePresent(findByHashOf(userToken.rawToken()), "Expected persisted refresh token in whenRevokingAllUserTokensShouldNotAffectOtherUsersTokens");
            RefreshToken anotherUserTokenAfter = requirePresent(findByHashOf(anotherUserToken.rawToken()), "Expected persisted refresh token in whenRevokingAllUserTokensShouldNotAffectOtherUsersTokens");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(userTokenAfter.isRevoked()).as("First user's token should be revoked").isTrue();
                softly.assertThat(anotherUserTokenAfter.isRevoked()).as("Another user's token should not be revoked").isFalse();
            });
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
            City cityWarsaw = persistCity(CitiesConstants.WARSAW_NAME);
            user = persistActiveUser(UserTestBuilder.firstUser(), cityWarsaw);
        }

        @Test
        @DisplayName("When revoking all user web tokens should revoke only rotational device type tokens")
        public void whenRevokingAllUserWebTokensShouldRevokeOnlyRotationalDeviceTypeTokens(){
            // Create tokens for rotational devices (WEB, DESKTOP)
            IssuedRefreshToken webToken = refreshTokenService.issueRefreshToken(user, DeviceType.WEB);
            IssuedRefreshToken desktopToken = refreshTokenService.issueRefreshToken(user, DeviceType.DESKTOP);

            // Create tokens for non-rotational devices (MOBILE)
            IssuedRefreshToken mobileAndroidToken = refreshTokenService.issueRefreshToken(user, DeviceType.MOBILE_ANDROID);
            IssuedRefreshToken mobileIosToken = refreshTokenService.issueRefreshToken(user, DeviceType.MOBILE_IOS);

            refreshTokenService.revokeAllUserWebTokens(user.getId());

            RefreshToken webTokenAfter = requirePresent(findByHashOf(webToken.rawToken()), "Expected persisted refresh token in whenRevokingAllUserWebTokensShouldRevokeOnlyRotationalDeviceTypeTokens");
            RefreshToken desktopTokenAfter = requirePresent(findByHashOf(desktopToken.rawToken()), "Expected persisted refresh token in whenRevokingAllUserWebTokensShouldRevokeOnlyRotationalDeviceTypeTokens");
            RefreshToken mobileAndroidTokenAfter = requirePresent(findByHashOf(mobileAndroidToken.rawToken()), "Expected persisted refresh token in whenRevokingAllUserWebTokensShouldRevokeOnlyRotationalDeviceTypeTokens");
            RefreshToken mobileIosTokenAfter = requirePresent(findByHashOf(mobileIosToken.rawToken()), "Expected persisted refresh token in whenRevokingAllUserWebTokensShouldRevokeOnlyRotationalDeviceTypeTokens");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(webTokenAfter.isRevoked()).as("WEB token should be revoked").isTrue();
                softly.assertThat(desktopTokenAfter.isRevoked()).as("DESKTOP token should be revoked").isTrue();
                softly.assertThat(mobileAndroidTokenAfter.isRevoked()).as("MOBILE_ANDROID token should not be revoked").isFalse();
                softly.assertThat(mobileIosTokenAfter.isRevoked()).as("MOBILE_IOS token should not be revoked").isFalse();
            });
        }

        @Test
        @DisplayName("When revoking all user web tokens should not affect other users tokens")
        public void whenRevokingAllUserWebTokensShouldNotAffectOtherUsersTokens() {
            City anotherCity = persistCity(CitiesConstants.KRAKOW_NAME);
            User anotherUser = persistActiveUser(
                    UserTestBuilder.thirdUser(),
                    anotherCity
            );

            IssuedRefreshToken userWebToken = refreshTokenService.issueRefreshToken(user, DeviceType.WEB);
            IssuedRefreshToken anotherUserWebToken = refreshTokenService.issueRefreshToken(anotherUser, DeviceType.WEB);

            refreshTokenService.revokeAllUserWebTokens(user.getId());

            RefreshToken userTokenAfter = requirePresent(findByHashOf(userWebToken.rawToken()), "Expected persisted refresh token in whenRevokingAllUserWebTokensShouldNotAffectOtherUsersTokens");
            RefreshToken anotherUserTokenAfter = requirePresent(findByHashOf(anotherUserWebToken.rawToken()), "Expected persisted refresh token in whenRevokingAllUserWebTokensShouldNotAffectOtherUsersTokens");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(userTokenAfter.isRevoked()).as("First user's web token should be revoked").isTrue();
                softly.assertThat(anotherUserTokenAfter.isRevoked()).as("Another user's web token should not be revoked").isFalse();
            });
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
            City cityWarsaw = persistCity(CitiesConstants.WARSAW_NAME);
            user = persistActiveUser(UserTestBuilder.firstUser(), cityWarsaw);
        }

        @Test
        @DisplayName("When revoking all user mobile tokens should revoke only non-rotational device type tokens")
        public void whenRevokingAllUserMobileTokensShouldRevokeOnlyNonRotationalDeviceTypeTokens() {
            // Create tokens for rotational devices (WEB, DESKTOP)
            IssuedRefreshToken webToken = refreshTokenService.issueRefreshToken(user, DeviceType.WEB);
            IssuedRefreshToken desktopToken = refreshTokenService.issueRefreshToken(user, DeviceType.DESKTOP);

            // Create tokens for non-rotational devices (MOBILE)
            IssuedRefreshToken mobileAndroidToken = refreshTokenService.issueRefreshToken(user, DeviceType.MOBILE_ANDROID);
            IssuedRefreshToken mobileIosToken = refreshTokenService.issueRefreshToken(user, DeviceType.MOBILE_IOS);

            refreshTokenService.revokeAllUserMobileTokens(user.getId());

            RefreshToken webTokenAfter = requirePresent(findByHashOf(webToken.rawToken()), "Expected persisted refresh token in whenRevokingAllUserMobileTokensShouldRevokeOnlyNonRotationalDeviceTypeTokens");
            RefreshToken desktopTokenAfter = requirePresent(findByHashOf(desktopToken.rawToken()), "Expected persisted refresh token in whenRevokingAllUserMobileTokensShouldRevokeOnlyNonRotationalDeviceTypeTokens");
            RefreshToken mobileAndroidTokenAfter = requirePresent(findByHashOf(mobileAndroidToken.rawToken()), "Expected persisted refresh token in whenRevokingAllUserMobileTokensShouldRevokeOnlyNonRotationalDeviceTypeTokens");
            RefreshToken mobileIosTokenAfter = requirePresent(findByHashOf(mobileIosToken.rawToken()), "Expected persisted refresh token in whenRevokingAllUserMobileTokensShouldRevokeOnlyNonRotationalDeviceTypeTokens");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(webTokenAfter.isRevoked()).as("WEB token should not be revoked").isFalse();
                softly.assertThat(desktopTokenAfter.isRevoked()).as("DESKTOP token should not be revoked").isFalse();
                softly.assertThat(mobileAndroidTokenAfter.isRevoked()).as("MOBILE_ANDROID token should be revoked").isTrue();
                softly.assertThat(mobileIosTokenAfter.isRevoked()).as("MOBILE_IOS token should be revoked").isTrue();
            });
        }

        @Test
        @DisplayName("When revoking all user mobile tokens should not affect other users tokens")
        public void whenRevokingAllUserMobileTokensShouldNotAffectOtherUsersTokens(){
            City anotherCity = persistCity(CitiesConstants.KRAKOW_NAME);
            User anotherUser = persistActiveUser(
                    UserTestBuilder.thirdUser(),
                    anotherCity
            );


            IssuedRefreshToken userAndroidToken = refreshTokenService.issueRefreshToken(user, DeviceType.MOBILE_ANDROID);
            IssuedRefreshToken anotherUserAndroidToken = refreshTokenService.issueRefreshToken(anotherUser, DeviceType.MOBILE_ANDROID);

            refreshTokenService.revokeAllUserMobileTokens(user.getId());

            RefreshToken userTokenAfter = requirePresent(findByHashOf(userAndroidToken.rawToken()), "Expected persisted refresh token in whenRevokingAllUserMobileTokensShouldNotAffectOtherUsersTokens");
            RefreshToken anotherUserTokenAfter = requirePresent(findByHashOf(anotherUserAndroidToken.rawToken()), "Expected persisted refresh token in whenRevokingAllUserMobileTokensShouldNotAffectOtherUsersTokens");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(userTokenAfter.isRevoked()).as("First user's Android token should be revoked").isTrue();
                softly.assertThat(anotherUserTokenAfter.isRevoked()).as("Another user's Android token should not be revoked").isFalse();
            });
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
        private IssuedRefreshToken expiredToken;
        private IssuedRefreshToken validToken;

        @BeforeEach
        void setUp() {
            City cityWarsaw = persistCity(CitiesConstants.WARSAW_NAME);
            user = persistActiveUser(UserTestBuilder.firstUser(), cityWarsaw);

            Instant now = TimeConstants.NOW;

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

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(findByHashOf(expiredToken.rawToken()))
                        .as("Expired token should be deleted")
                        .isEmpty();
                softly.assertThat(findByHashOf(validToken.rawToken()))
                        .as("Non-expired token should remain in database")
                        .isPresent();
            });
        }
    }
}
