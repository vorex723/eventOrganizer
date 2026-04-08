package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationResponse;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.exception.auth.UserNotAuthenticatedException;
import com.mazurek.eventOrganizer.exception.user.*;
import com.mazurek.eventOrganizer.jwt.*;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.user.dto.ChangeUserDetailsDto;
import com.mazurek.eventOrganizer.user.dto.ChangeUserEmailDto;
import com.mazurek.eventOrganizer.user.dto.ChangeUserPasswordDto;
import com.mazurek.eventOrganizer.user.dto.RegisterFcmTokenRequest;
import com.mazurek.eventOrganizer.user.dto.UserProfileDto;
import com.mazurek.eventOrganizer.testData.builders.CityTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ChangeUserDetailsDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ChangeUserEmailDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ChangeUserPasswordDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.RegisterFcmTokenRequestTestBuilder;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import com.mazurek.eventOrganizer.testData.TestConstants.*;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("UserService integration tests:")
public class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;
    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
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
                            UserProfileDto::getLastName,
                            UserProfileDto::getHomeCity)
                    .containsExactly(user.getId(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getHomeCity().getName());
        }

        @Test
        @DisplayName("When getting user by id should throw UserNotFoundException if user does not exist")
        public void whenGettingUserByIdShouldThrowUserNotFoundExceptionIfUserDoesNotExist() {
            assertThatThrownBy(() -> userService.getUserById(UserConstants.NOT_EXISTING_USER_ID))
                    .isInstanceOf(UserNotFoundException.class);
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
                    .homeCity(NEW_CITY_NAME)
                    .build();
        }

        @AfterEach
        void tearDown() {

        }

        @Test
        @DisplayName("When changing user details should throw UserNotAuthenticatedException if user is not authenticated")
        public void whenChangingUserDetailsShouldThrowExceptionIfUserNotAuthenticated() {
            SecurityContextHolder.clearContext();

            assertThatThrownBy(() -> userService.changeDetails(changeUserDetailsDto))
                    .isInstanceOf(UserNotAuthenticatedException.class);
        }

        @Test
        @DisplayName("When changing user details should not change city if name differs only in case")
        public void whenChangingUserDetailsShouldNotChangeCityIfNameDiffersOnlyInCase() {

            changeUserDetailsDto.setHomeCity(CitiesConstants.WARSAW_NAME.toUpperCase());

            City originalCity = cityRepository.findByIgnoreCaseName(CitiesConstants.WARSAW_NAME)
                    .orElseThrow();

            userService.changeDetails(changeUserDetailsDto);

            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL)
                    .orElseThrow(UserNotFoundException::new);

            assertThat(user.getHomeCity().getId()).isEqualTo(originalCity.getId());

            assertThat(cityRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("When changing user details should update user first name and last name")
        public void whenChangingUserDetailsShouldUpdateUserFirstNameAndLastName() {
            changeUserDetailsDto.setHomeCity(CitiesConstants.WARSAW_NAME);

            userService.changeDetails(changeUserDetailsDto);

            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);

            assertThat(user)
                    .extracting(User::getFirstName, User::getLastName)
                    .containsExactly(NEW_FIRST_NAME, NEW_LAST_NAME);

        }

        @Test
        @DisplayName("When changing user details should update user city")
        public void whenChangingUserDetailsShouldUpdateCity() {
            userService.changeDetails(changeUserDetailsDto);

            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);

            assertThat(cityRepository.findByIgnoreCaseName(NEW_CITY_NAME).isPresent())
                    .isTrue();

            assertThat(user.getHomeCity().getName())
                    .isEqualTo(NEW_CITY_NAME.toLowerCase(Locale.ROOT));

        }

        @Test
        @DisplayName("When changing user details should create new city if it doesn't exist")
        public void whenChangingUserDetailsShouldCreateNewCityIfItDoesntExist() {
            assertThat(cityRepository.findByIgnoreCaseName(NEW_CITY_NAME)).isEmpty();

            long cityCountBefore = cityRepository.count();

            userService.changeDetails(changeUserDetailsDto);


            assertThat(cityRepository.count()).isEqualTo(cityCountBefore + 1);
            assertThat(cityRepository.findByIgnoreCaseName(NEW_CITY_NAME)).isPresent();
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

            userRepository.flush();

            User reloadedUser = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL)
                    .orElseThrow(UserNotFoundException::new);

            assertThat(reloadedUser)
                    .extracting(User::getFirstName, User::getLastName)
                    .containsExactly(NEW_FIRST_NAME, NEW_LAST_NAME);

            assertThat(reloadedUser.getHomeCity().getName())
                    .isEqualToIgnoringCase(NEW_CITY_NAME);
        }

        @Test
        @DisplayName("When changing user details should return updated user profile dto")
        public void whenChangingUserDetailsShouldReturnUpdatedUserProfileDto() {
            UserProfileDto result = userService.changeDetails(changeUserDetailsDto);

            assertThat(result).isNotNull();
            assertThat(result.getFirstName())
                    .isEqualTo(NEW_FIRST_NAME);
            assertThat(result.getLastName())
                    .isEqualTo(NEW_LAST_NAME);
            assertThat(result.getHomeCity())
                    .isEqualTo(NEW_CITY_NAME.toLowerCase(Locale.ROOT));

        }

    }

    @Nested
    @DisplayName("Change password tests:")
    class ChangePasswordTests {
        private final String NEW_PASSWORD = UserConstants.NEW_PASSWORD;
        private final DeviceType deviceType = DeviceType.WEB;
        private final String deviceInfo = DeviceConstants.USER_AGENT_UNKNOWN;

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
            User userBefore = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow();
            String oldPasswordHash = userBefore.getPassword();

            userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            User userAfter = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow();

            assertThat(userAfter.getPassword()).isNotEqualTo(oldPasswordHash);
        }

        @Test
        @DisplayName("When changing password should persist password hash that works with new password")
        public void whenChangingPasswordShouldPersistPasswordHashThatWorksWithNewPassword(){
            userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            User userAfter = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow();

            assertThat(passwordEncoder.matches(NEW_PASSWORD, userAfter.getPassword())).isTrue();
        }

        @Test
        @DisplayName("When changing password should make old password invalid")
        public void whenChangingPasswordShouldMakeOldPasswordInvalid(){
            userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            User userAfter = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow();

            assertThat(passwordEncoder.matches(UserConstants.USER_PASSWORD, userAfter.getPassword())).isFalse();
        }

        @Test
        @DisplayName("When changing password should update last credentials change time in database")
        public void whenChangingPasswordShouldUpdateLastCredentialsChangeTimeInDatabase(){
            User userBefore = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow();
            Instant lastCredentialsChangeBefore = userBefore.getLastCredentialsChangeTime();

            userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            User userAfter = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow();

            assertThat(userAfter.getLastCredentialsChangeTime()).isAfter(lastCredentialsChangeBefore);
        }

        @Test
        @DisplayName("When changing password should revoke all existing refresh tokens in database")
        public void whenChangingPasswordShouldRevokeAllExistingRefreshTokensInDatabase(){
            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow();

            RefreshToken oldToken1 = refreshTokenService.createRefreshToken(user, DeviceType.WEB);
            RefreshToken oldToken2 = refreshTokenService.createRefreshToken(user, DeviceType.MOBILE_ANDROID);

            userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            RefreshToken token1After = refreshTokenRepository.findByToken(oldToken1.getToken()).orElseThrow();
            RefreshToken token2After = refreshTokenRepository.findByToken(oldToken2.getToken()).orElseThrow();

            assertThat(token1After.isRevoked()).isTrue();
            assertThat(token2After.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("When changing password should create new refresh token with correct properties")
        public void whenChangingPasswordShouldCreateNewRefreshTokenWithCorrectProperties(){
            AuthenticationResponse response = userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            RefreshToken newToken = refreshTokenRepository.findByToken(response.getRefreshToken())
                    .orElseThrow();

            assertThat(newToken.getDeviceType()).isEqualTo(deviceType);
            assertThat(newToken.isRevoked()).isFalse();
            assertThat(newToken.isExpired()).isFalse();
        }

        @Test
        @DisplayName("When changing password should return valid access token")
        public void whenChangingPasswordShouldReturnValidAccessToken(){
            AuthenticationResponse response = userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            assertThat(jwtUtils.isTokenValid(response.getAccessToken())).isTrue();
            assertThat(jwtUtils.extractUsername(response.getAccessToken())).isEqualTo(UserConstants.FIRST_USER_EMAIL);
        }

        @Test
        @DisplayName("When changing password should return complete authentication response")
        public void whenChangingPasswordShouldReturnCompleteAuthenticationResponse(){
            AuthenticationResponse response = userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isNotBlank();
            assertThat(response.getRefreshToken()).isNotBlank();
            assertThat(response.getAccessTokenExpiration()).isPositive();
        }
    }

    @Nested
    @DisplayName("Change email tests:")
    class ChangeEmailTests {
        private final String NEW_EMAIL = UserConstants.FIRST_USER_NEW_EMAIL;
        private final DeviceType deviceType = DeviceType.WEB;
        private final String deviceInfo = DeviceConstants.USER_AGENT_UNKNOWN;
        private ChangeUserEmailDto changeUserEmailDto;

        @BeforeEach
        void setUp() {
            changeUserEmailDto = ChangeUserEmailDtoTestBuilder.validChange()
                    .newEmail(NEW_EMAIL)
                    .newEmailConfirmation(NEW_EMAIL)
                    .password(UserConstants.USER_PASSWORD)
                    .build();
        }
        @Test
        @DisplayName("When changing email to existing one should fail transactionally")
        void whenChangingEmailShouldFailWhenEmailAlreadyExistsInDatabase() {
            changeUserEmailDto.setNewEmail(UserConstants.SECOND_USER_EMAIL);
            changeUserEmailDto.setNewEmailConfirmation(UserConstants.SECOND_USER_EMAIL);

            assertThatThrownBy(() ->
                    userService.changeEmail(changeUserEmailDto, deviceType, deviceInfo)
            ).isInstanceOf(UserAlreadyExistException.class);
        }

        @Test
        @DisplayName("When changing email should persist updated user")
        public void whenChangingEmailShouldPersistUpdatedUser(){
            UUID userId = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL)
                    .orElseThrow(UserNotFoundException::new)
                    .getId();

            userService.changeEmail(changeUserEmailDto, deviceType, deviceInfo);

            assertThat(userRepository.findByEmail(NEW_EMAIL).isPresent())
                    .isTrue();
            assertThat(userRepository.findByEmail(NEW_EMAIL).orElseThrow(UserNotFoundException::new).getId())
                    .isEqualTo(userId);

        }
        @Test
        @DisplayName("When changing email should not find user by old email")
        public void whenChangingEmailShouldNotFindUserByOldEmail(){
            userService.changeEmail(changeUserEmailDto, deviceType, deviceInfo);

            assertThat(userRepository.findByEmail(UserConstants.FIRST_USER_EMAIL).isEmpty())
                    .isTrue();

        }


        @Test
        @DisplayName("When changing email should update last credentials change time in database")
        public void whenChangingEmailShouldUpdateLastCredentialsChangeTimeInDatabase(){
            User userBefore = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow();
            Instant lastCredentialsChangeBefore = userBefore.getLastCredentialsChangeTime();

            userService.changeEmail(changeUserEmailDto, deviceType, deviceInfo);

            User userAfter = userRepository.findByIgnoreCaseEmail(NEW_EMAIL).orElseThrow();

            assertThat(userAfter.getLastCredentialsChangeTime()).isAfter(lastCredentialsChangeBefore);
        }

        @Test
        @DisplayName("When changing email should revoke all existing refresh tokens in database")
        public void whenChangingEmailShouldRevokeAllExistingRefreshTokensInDatabase(){
            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow();

            RefreshToken oldToken1 = refreshTokenService.createRefreshToken(user, DeviceType.WEB);
            RefreshToken oldToken2 = refreshTokenService.createRefreshToken(user, DeviceType.MOBILE_ANDROID);

            userService.changeEmail(changeUserEmailDto, deviceType, deviceInfo);

            RefreshToken token1After = refreshTokenRepository.findByToken(oldToken1.getToken()).orElseThrow();
            RefreshToken token2After = refreshTokenRepository.findByToken(oldToken2.getToken()).orElseThrow();

            assertThat(token1After.isRevoked()).isTrue();
            assertThat(token2After.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("When changing email should create new refresh token with correct properties")
        public void whenChangingEmailShouldCreateNewRefreshTokenWithCorrectProperties(){
            AuthenticationResponse response = userService.changeEmail(changeUserEmailDto, deviceType, deviceInfo);

            RefreshToken newToken = refreshTokenRepository.findByToken(response.getRefreshToken())
                    .orElseThrow();

            assertThat(newToken.getDeviceType()).isEqualTo(deviceType);
            assertThat(newToken.isRevoked()).isFalse();
            assertThat(newToken.isExpired()).isFalse();
        }

        @Test
        @DisplayName("When changing email should return valid access token")
        public void whenChangingEmailShouldReturnValidAccessToken(){
            AuthenticationResponse response = userService.changeEmail(changeUserEmailDto, deviceType, deviceInfo);

            assertThat(jwtUtils.isTokenValid(response.getAccessToken())).isTrue();
            assertThat(jwtUtils.extractUsername(response.getAccessToken())).isEqualTo(NEW_EMAIL);
        }

        @Test
        @DisplayName("When changing email should return complete authentication response")
        public void whenChangingEmailShouldReturnCompleteAuthenticationResponse(){
            AuthenticationResponse response = userService.changeEmail(changeUserEmailDto, deviceType, deviceInfo);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isNotBlank();
            assertThat(response.getRefreshToken()).isNotBlank();
            assertThat(response.getAccessTokenExpiration()).isPositive();
        }
    }


    @Nested
    @DisplayName("Register FCM token tests:")
    class RegisterFcmTokenTests {

        @Test
        @DisplayName("When registering user fcm token should return true and persist token")
        public void whenRegisteringUserFcmTokenShouldReturnTrueAndPersistToken() {
            RegisterFcmTokenRequest registerFcmTokenRequest = RegisterFcmTokenRequestTestBuilder.firstUserToken().build();

            boolean registrationResult = userService.registerUserFcmToken(registerFcmTokenRequest);

            User updatedUser = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);
            assertThat(registrationResult).isTrue();
            assertThat(updatedUser.getFcmAndroidToken()).isEqualTo(UserConstants.FIRST_USER_FCM_TOKEN);
        }

        @Test
        @DisplayName("When registering user fcm token and user is not authenticated should return false")
        public void whenRegisteringUserFcmTokenShouldReturnFalseWhenUserIsNotAuthenticated() {
            SecurityContextHolder.clearContext();
            RegisterFcmTokenRequest registerFcmTokenRequest = RegisterFcmTokenRequestTestBuilder.firstUserToken().build();

            boolean registrationResult = userService.registerUserFcmToken(registerFcmTokenRequest);

            assertThat(registrationResult).isFalse();
        }
    }

    @Nested
    @DisplayName("Ban user tests:")
    class BanUserTests {

        @Test
        @DisplayName("When banning user should set banned flag and revoke all user refresh tokens")
        public void whenBanningUserShouldSetBannedFlagAndRevokeAllUserRefreshTokens() {
            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);
            RefreshToken firstToken = refreshTokenService.createRefreshToken(user, DeviceType.WEB);
            RefreshToken secondToken = refreshTokenService.createRefreshToken(user, DeviceType.MOBILE_ANDROID);

            userService.banUser(user.getId());

            User bannedUser = userRepository.findById(user.getId()).orElseThrow(UserNotFoundException::new);
            RefreshToken firstTokenAfter = refreshTokenRepository.findByToken(firstToken.getToken()).orElseThrow();
            RefreshToken secondTokenAfter = refreshTokenRepository.findByToken(secondToken.getToken()).orElseThrow();

            assertThat(bannedUser.isBanned()).isTrue();
            assertThat(firstTokenAfter.isRevoked()).isTrue();
            assertThat(secondTokenAfter.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("When banning user and user does not exist should throw UserNotFoundException")
        public void whenBanningUserShouldThrowUserNotFoundExceptionIfUserDoesNotExist() {
            assertThatThrownBy(() -> userService.banUser(UserConstants.NOT_EXISTING_USER_ID))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

}
