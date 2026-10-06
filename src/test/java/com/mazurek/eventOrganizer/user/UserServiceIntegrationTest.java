package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationResponse;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.exception.auth.UserNotAuthenticatedException;
import com.mazurek.eventOrganizer.exception.user.*;
import com.mazurek.eventOrganizer.jwt.*;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestPersistenceQueries;
import com.mazurek.eventOrganizer.user.dto.ChangeUserDetailsDto;
import com.mazurek.eventOrganizer.user.dto.ChangeUserPasswordDto;
import com.mazurek.eventOrganizer.user.dto.CurrentUserDto;
import com.mazurek.eventOrganizer.user.dto.UserProfileDto;
import com.mazurek.eventOrganizer.testData.builders.CityTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.RefreshTokenTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ChangeUserDetailsDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ChangeUserPasswordDtoTestBuilder;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import com.mazurek.eventOrganizer.testData.TestConstants.*;

import java.util.*;

import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("UserService integration tests:")
public class UserServiceIntegrationTest {

    @Autowired
    private TestPersistenceQueries testPersistenceQueries;

    @Autowired
    private UserService userService;
    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private DeletionService deletionService;

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();

        authHelper.setupRolesAndUsers();
        authHelper.setupSecurityContextForFirstUser();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
    }

    @Nested
    @DisplayName("Get user by id tests:")
    class GetUserByIdTests {

        @Test
        @DisplayName("When getting user by id should return user profile dto with correct data")
        public void whenGettingUserByIdShouldReturnDtoWithCorrectData() {
            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL)
                    .orElseThrow(UserNotFoundException::new);

            UserProfileDto userProfileDto = userService.getUserById(user.getId());

            assertThat(userProfileDto)
                    .extracting(UserProfileDto::getId,
                            UserProfileDto::getFirstName,
                            UserProfileDto::getLastName)
                    .containsExactly(user.getId(),
                            user.getFirstName(),
                            user.getLastName());
        }

        @Test
        @DisplayName("When getting user by id should throw UserNotFoundException if user does not exist")
        public void whenGettingUserByIdShouldThrowUserNotFoundExceptionIfUserDoesNotExist() {
            assertThatThrownBy(() -> userService.getUserById(UserConstants.NOT_EXISTING_USER_ID))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get current user tests:")
    class GetCurrentUserTests {

        @Test
        @DisplayName("When getting current user should return complete private account data")
        void whenGettingCurrentUserShouldReturnCompletePrivateAccountData() {
            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL)
                    .orElseThrow(UserNotFoundException::new);

            CurrentUserDto result = userService.getCurrentUser();
            assertThat(result).isNotNull();

            assertThat(result)
                    .extracting(
                            CurrentUserDto::getId,
                            CurrentUserDto::getFirstName,
                            CurrentUserDto::getLastName,
                            CurrentUserDto::getEmail,
                            CurrentUserDto::getHomeCity,
                            CurrentUserDto::getHomeCityId,
                            CurrentUserDto::getHomeCityExternalId,
                            CurrentUserDto::getTimeZone
                    )
                    .containsExactly(
                            user.getId(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getEmail(),
                            user.getHomeCity().getName(),
                            user.getHomeCity().getId(),
                            user.getHomeCity().getExternalId(),
                            user.getTimeZone()
                    );
        }

        @Test
        @DisplayName("When getting current user should reject an unauthenticated request")
        void whenGettingCurrentUserShouldRejectUnauthenticatedRequest() {
            SecurityContextHolder.clearContext();

            assertThatThrownBy(userService::getCurrentUser)
                    .isInstanceOf(UserNotAuthenticatedException.class);
        }
    }

    @Nested
    @DisplayName("Change user details tests:")
    class ChangeUserDetailsTests {

        private final String NEW_FIRST_NAME = UserConstants.SECOND_USER_FIRST_NAME;
        private final String NEW_LAST_NAME = UserConstants.SECOND_USER_LAST_NAME;
        private final String NEW_CITY_NAME = CitiesConstants.KRAKOW_NAME;

        private ChangeUserDetailsDto changeUserDetailsDto;

        @BeforeEach
        void setUp() {

            changeUserDetailsDto = ChangeUserDetailsDtoTestBuilder.validUpdate()
                    .firstName(NEW_FIRST_NAME)
                    .lastName(NEW_LAST_NAME)
                    .homeCityExternalId(CitiesConstants.KRAKOW_EXTERNAL_ID)
                    .build();
        }

        @Test
        @DisplayName("When changing user details should throw UserNotAuthenticatedException if user is not authenticated")
        public void whenChangingUserDetailsShouldThrowExceptionIfUserNotAuthenticated() {
            SecurityContextHolder.clearContext();

            assertThatThrownBy(() -> userService.changeDetails(changeUserDetailsDto))
                    .isInstanceOf(UserNotAuthenticatedException.class);
        }

        @Test
        @DisplayName("When changing user details should not change city for the same external identifier")
        public void whenChangingUserDetailsShouldNotChangeCityForTheSameExternalIdentifier() {

            changeUserDetailsDto.setHomeCityExternalId(com.mazurek.eventOrganizer.testData.TestCityData.externalId(CitiesConstants.WARSAW_NAME));

            City originalCity = requirePresent(cityRepository.findByExternalId(com.mazurek.eventOrganizer.testData.TestCityData.externalId(CitiesConstants.WARSAW_NAME)), "Expected city record in whenChangingUserDetailsShouldNotChangeCityForTheSameExternalIdentifier");

            userService.changeDetails(changeUserDetailsDto);

            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL)
                    .orElseThrow(UserNotFoundException::new);

            assertThat(user.getHomeCity().getId()).isEqualTo(originalCity.getId());

            assertThat(cityRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("When changing user details should update user first name and last name")
        public void whenChangingUserDetailsShouldUpdateUserFirstNameAndLastName() {
            changeUserDetailsDto.setHomeCityExternalId(com.mazurek.eventOrganizer.testData.TestCityData.externalId(CitiesConstants.WARSAW_NAME));

            userService.changeDetails(changeUserDetailsDto);

            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);

            assertThat(user)
                    .extracting(User::getFirstName, User::getLastName, User::getTimeZone)
                    .containsExactly(NEW_FIRST_NAME, NEW_LAST_NAME, UserConstants.SECOND_USER_TIMEZONE);

        }

        @Test
        @DisplayName("When changing user details should update user city")
        public void whenChangingUserDetailsShouldUpdateCity() {
            userService.changeDetails(changeUserDetailsDto);

            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);

            assertThat(cityRepository.findByExternalId(com.mazurek.eventOrganizer.testData.TestCityData.externalId(NEW_CITY_NAME)))
                    .isPresent();

            assertThat(user.getHomeCity().getName())
                    .isEqualTo(NEW_CITY_NAME.toLowerCase(Locale.ROOT));

        }

        @Test
        @DisplayName("When changing user details should create new city if it doesn't exist")
        public void whenChangingUserDetailsShouldCreateNewCityIfItDoesntExist() {
            assertThat(cityRepository.findByExternalId(com.mazurek.eventOrganizer.testData.TestCityData.externalId(NEW_CITY_NAME))).isEmpty();

            long cityCountBefore = cityRepository.count();

            userService.changeDetails(changeUserDetailsDto);


            assertThat(cityRepository.count()).isEqualTo(cityCountBefore + 1);
            assertThat(cityRepository.findByExternalId(com.mazurek.eventOrganizer.testData.TestCityData.externalId(NEW_CITY_NAME))).isPresent();
        }

        @Test
        @DisplayName("When changing user details should reuse existing city instead of creating duplicate")
        public void whenChangingUserDetailsShouldReuseExistingCityInsteadOfCreatingDuplicate() {

            City existingCity = cityRepository.save(CityTestBuilder.krakow()
                    .id(null)
                    .name(NEW_CITY_NAME.toLowerCase(Locale.ROOT))
                    .build());
            long cityCountBefore = cityRepository.count();

            userService.changeDetails(changeUserDetailsDto);

            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL)
                    .orElseThrow(UserNotFoundException::new);


            assertThat(cityRepository.count()).isEqualTo(cityCountBefore);
            assertThat(user.getHomeCity().getId()).isEqualTo(existingCity.getId());
        }

        @Test
        @DisplayName("When changing user details should persist changes in database")
        public void whenChangingUserDetailsShouldPersistChangesInDatabase() {
            userService.changeDetails(changeUserDetailsDto);

            // No test transaction: this read reloads the committed profile.

            User reloadedUser = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL)
                    .orElseThrow(UserNotFoundException::new);

            assertThat(reloadedUser)
                    .extracting(User::getFirstName, User::getLastName, User::getTimeZone)
                    .containsExactly(NEW_FIRST_NAME, NEW_LAST_NAME, UserConstants.SECOND_USER_TIMEZONE);

            assertThat(reloadedUser.getHomeCity().getName())
                    .isEqualToIgnoringCase(NEW_CITY_NAME);
        }

        @Test
        @DisplayName("When changing user details should return updated user profile dto")
        public void whenChangingUserDetailsShouldReturnUpdatedUserProfileDto() {
            CurrentUserDto result = userService.changeDetails(changeUserDetailsDto);
            assertThat(result).isNotNull();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getFirstName())
                        .isEqualTo(NEW_FIRST_NAME);
                softly.assertThat(result.getLastName())
                        .isEqualTo(NEW_LAST_NAME);
                softly.assertThat(result.getHomeCity())
                        .isEqualTo(NEW_CITY_NAME.toLowerCase(Locale.ROOT));
                softly.assertThat(result.getEmail())
                        .isEqualTo(UserConstants.FIRST_USER_EMAIL);
                softly.assertThat(result.getTimeZone())
                        .isEqualTo(UserConstants.SECOND_USER_TIMEZONE);
            });

        }

    }

    @Nested
    @DisplayName("Change password tests:")
    class ChangePasswordTests {
        private final String NEW_PASSWORD = UserConstants.NEW_PASSWORD;
        private final DeviceType deviceType = DeviceType.WEB;

        private ChangeUserPasswordDto changeUserPasswordDto;

        @BeforeEach
        void setUp() {
            changeUserPasswordDto = ChangeUserPasswordDtoTestBuilder.validChange()
                    .newPassword(NEW_PASSWORD)
                    .newPasswordConfirmation(NEW_PASSWORD)
                    .password(UserConstants.USER_PASSWORD)
                    .build();
        }

        @Test
        @DisplayName("When changing password should update password hash in database")
        public void whenChangingPasswordShouldUpdatePasswordHashInDatabase(){
            User userBefore = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL), "Expected user record in whenChangingPasswordShouldUpdatePasswordHashInDatabase");
            String oldPasswordHash = userBefore.getPassword();

            userService.changePassword(changeUserPasswordDto, deviceType);

            User userAfter = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL), "Expected user record in whenChangingPasswordShouldUpdatePasswordHashInDatabase");

            assertThat(userAfter).isNotSameAs(userBefore);
            assertThat(userAfter.getPassword()).isNotEqualTo(oldPasswordHash);
            assertThat(userAfter.getSecurityVersion()).isEqualTo(userBefore.getSecurityVersion() + 1);
        }

        @Test
        @DisplayName("When changing password should persist password hash that works with new password")
        public void whenChangingPasswordShouldPersistPasswordHashThatWorksWithNewPassword(){
            userService.changePassword(changeUserPasswordDto, deviceType);

            User userAfter = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL), "Expected user record in whenChangingPasswordShouldPersistPasswordHashThatWorksWithNewPassword");

            assertThat(passwordEncoder.matches(NEW_PASSWORD, userAfter.getPassword())).isTrue();
        }

        @Test
        @DisplayName("When changing password should make old password invalid")
        public void whenChangingPasswordShouldMakeOldPasswordInvalid(){
            userService.changePassword(changeUserPasswordDto, deviceType);

            User userAfter = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL), "Expected user record in whenChangingPasswordShouldMakeOldPasswordInvalid");

            assertThat(passwordEncoder.matches(UserConstants.USER_PASSWORD, userAfter.getPassword())).isFalse();
        }

        @Test
        @DisplayName("When changing password should update last credentials change time in database")
        public void whenChangingPasswordShouldUpdateLastCredentialsChangeTimeInDatabase(){
            userService.changePassword(changeUserPasswordDto, deviceType);

            User userAfter = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL), "Expected user record in whenChangingPasswordShouldUpdateLastCredentialsChangeTimeInDatabase");

            assertThat(userAfter.getLastCredentialsChangeTime()).isEqualTo(TimeConstants.NOW);
        }

        @Test
        @DisplayName("When changing password should revoke all existing refresh tokens in database")
        public void whenChangingPasswordShouldRevokeAllExistingRefreshTokensInDatabase(){
            User user = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL), "Expected user record in whenChangingPasswordShouldRevokeAllExistingRefreshTokensInDatabase");

            IssuedRefreshToken oldToken1 = refreshTokenService.issueRefreshToken(user, DeviceType.WEB);
            IssuedRefreshToken oldToken2 = refreshTokenService.issueRefreshToken(user, DeviceType.MOBILE_ANDROID);

            userService.changePassword(changeUserPasswordDto, deviceType);

            RefreshToken token1After = requirePresent(testPersistenceQueries
                    .findRefreshTokenByHash(RefreshTokenTestBuilder.hashOf(oldToken1.rawToken())), "Expected refresh token record in whenChangingPasswordShouldRevokeAllExistingRefreshTokensInDatabase");
            RefreshToken token2After = requirePresent(testPersistenceQueries
                    .findRefreshTokenByHash(RefreshTokenTestBuilder.hashOf(oldToken2.rawToken())), "Expected refresh token record in whenChangingPasswordShouldRevokeAllExistingRefreshTokensInDatabase");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(token1After.isRevoked()).isTrue();
                softly.assertThat(token2After.isRevoked()).isTrue();
            });
        }

        @Test
        @DisplayName("When changing password should create new refresh token with correct properties")
        public void whenChangingPasswordShouldCreateNewRefreshTokenWithCorrectProperties(){
            AuthenticationResponse response = userService.changePassword(changeUserPasswordDto, deviceType);

            RefreshToken newToken = requirePresent(testPersistenceQueries
                    .findRefreshTokenByHash(RefreshTokenTestBuilder.hashOf(response.getRefreshToken())), "Expected refresh token record in whenChangingPasswordShouldCreateNewRefreshTokenWithCorrectProperties");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(newToken.getDeviceType()).isEqualTo(deviceType);
                softly.assertThat(newToken.isRevoked()).isFalse();
                softly.assertThat(newToken.isExpired(TimeConstants.NOW)).isFalse();
            });
        }

        @Test
        @DisplayName("When changing password should return valid access token")
        public void whenChangingPasswordShouldReturnValidAccessToken(){
            User beforeChange = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL), "Expected account before issuing replacement credentials");
            AuthenticationResponse response = userService.changePassword(changeUserPasswordDto, deviceType);

            assertThat(jwtUtils.isTokenValid(response.getAccessToken())).isTrue();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(jwtUtils.extractUsername(response.getAccessToken())).isEqualTo(UserConstants.FIRST_USER_EMAIL);
                softly.assertThat(jwtUtils.extractUserId(response.getAccessToken())).isEqualTo(beforeChange.getId());
                softly.assertThat(jwtUtils.extractSecurityVersion(response.getAccessToken())).isEqualTo(beforeChange.getSecurityVersion() + 1);
            });
        }

        @Test
        @DisplayName("When changing password should return complete authentication response")
        public void whenChangingPasswordShouldReturnCompleteAuthenticationResponse(){
            AuthenticationResponse response = userService.changePassword(changeUserPasswordDto, deviceType);

            assertThat(response).isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getAccessToken()).isNotBlank();
                softly.assertThat(response.getRefreshToken()).isNotBlank();
                softly.assertThat(response.getAccessTokenExpiration()).isPositive();
            });
        }
    }

    @Nested
    @DisplayName("Ban user tests:")
    class BanUserTests {

        @Test
        @DisplayName("When banning user should set banned flag and revoke all user refresh tokens")
        public void whenBanningUserShouldSetBannedFlagAndRevokeAllUserRefreshTokens() {
            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);
            IssuedRefreshToken firstToken = refreshTokenService.issueRefreshToken(user, DeviceType.WEB);
            IssuedRefreshToken secondToken = refreshTokenService.issueRefreshToken(user, DeviceType.MOBILE_ANDROID);

            userService.banUser(user.getId());

            User bannedUser = userRepository.findById(user.getId()).orElseThrow(UserNotFoundException::new);
            RefreshToken firstTokenAfter = requirePresent(testPersistenceQueries
                    .findRefreshTokenByHash(RefreshTokenTestBuilder.hashOf(firstToken.rawToken())), "Expected refresh token record in whenBanningUserShouldSetBannedFlagAndRevokeAllUserRefreshTokens");
            RefreshToken secondTokenAfter = requirePresent(testPersistenceQueries
                    .findRefreshTokenByHash(RefreshTokenTestBuilder.hashOf(secondToken.rawToken())), "Expected refresh token record in whenBanningUserShouldSetBannedFlagAndRevokeAllUserRefreshTokens");

            assertThat(bannedUser.isBanned()).isTrue();
            assertThat(bannedUser).isNotSameAs(user);
            assertThat(bannedUser.getSecurityVersion()).isEqualTo(user.getSecurityVersion() + 1);
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(firstTokenAfter.isRevoked()).isTrue();
                softly.assertThat(secondTokenAfter.isRevoked()).isTrue();
            });
        }

        @Test
        @DisplayName("When banning user and user does not exist should throw UserNotFoundException")
        public void whenBanningUserShouldThrowUserNotFoundExceptionIfUserDoesNotExist() {
            assertThatThrownBy(() -> userService.banUser(UserConstants.NOT_EXISTING_USER_ID))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

}
