package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.auth.dto.AuthenticationResponse;
import com.mazurek.eventOrganizer.auth.AuthenticationServiceImpl;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityService;
import com.mazurek.eventOrganizer.exception.auth.UserNotAuthenticatedException;
import com.mazurek.eventOrganizer.exception.user.*;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.jwt.JwtUtils;
import com.mazurek.eventOrganizer.jwt.RefreshToken;
import com.mazurek.eventOrganizer.jwt.RefreshTokenService;
import com.mazurek.eventOrganizer.testData.builders.CityTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.RefreshTokenTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.RoleTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ChangeUserDetailsDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ChangeUserEmailDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ChangeUserPasswordDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.RegisterFcmTokenRequestTestBuilder;
import com.mazurek.eventOrganizer.user.dto.*;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService unit tests:")
class UserServiceUnitTest {


    @Mock private UserRepository userRepository;
    @Mock private AuthenticationServiceImpl authenticationService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private JwtUtils jwtUtils;
    @Mock private CityService cityService;
    @Mock private Clock clock;
    private BCryptPasswordEncoder passwordEncoder = Mockito.spy(new BCryptPasswordEncoder());
    private UserService userService;

    private User user;
    private Optional<User> userOptional;

    private City cityWarsaw;
    private Role ROLE_USER;

    private DeviceType deviceType;
    private String deviceInfo;

    private RefreshToken refreshToken;


    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(TimeConstants.NOW);
        userService = new UserServiceImpl(userRepository, authenticationService, refreshTokenService, jwtUtils, cityService, passwordEncoder, clock);

        ROLE_USER = RoleTestBuilder.userRole().build();

        cityWarsaw = CityTestBuilder.warsaw().build();
        Instant userCreateAccountTime = TimeConstants.ONE_WEEK_AGO;

        user = UserTestBuilder.firstUser()
                .homeCity(cityWarsaw)
                .password(passwordEncoder.encode(UserConstants.USER_PASSWORD))
                .createdAt(userCreateAccountTime)
                .lastCredentialsChangeTime(userCreateAccountTime)
                .roles(new HashSet<>(Set.of(ROLE_USER)))
                .build();

        userOptional = Optional.of(user);

        Instant tokenCreateDate = TimeConstants.NOW;
        refreshToken = RefreshTokenTestBuilder.firstRefreshTokenForUser(user)
                .id(JwtConstants.TOKEN_ID_ONE)
                .createdAt(tokenCreateDate)
                .lastUsedAt(tokenCreateDate)
                .expiryDate(tokenCreateDate.plusMillis(JwtConstants.REFRESH_TOKEN_EXPIRATION_SHORT))
                .deviceType(DeviceType.WEB)
                .build();
    }

    @AfterEach
    void tearDown(){
    }


    @Test
    @DisplayName("When getting user by id should load user from database")
    public void whenGettingUserByIdShouldLoadUserFromDatabase(){
        when(userRepository.findById(UserConstants.FIRST_USER_ID)).thenReturn(userOptional);
        userService.getUserById(UserConstants.FIRST_USER_ID);
        verify(userRepository,times(1)).findById(UserConstants.FIRST_USER_ID);
    }

    @Test
    @DisplayName("When getting user by id should throw UserNotFoundException if user is not present in the database")
    void whenGettingUserByIdShouldThrowUserNotFoundExceptionIfUserIsNotPresentInDatabase(){
       assertThatThrownBy(() -> userService.getUserById(UUID.randomUUID()))
               .isInstanceOf(UserNotFoundException.class);
    }


    /*
    ********************************************************************************************************************
    *                                       CHANGE USER PASSWORD TESTS
    ********************************************************************************************************************
    */


    @Nested
    @DisplayName("Change user password tests:")
    class ChangeUserPasswordTests {

        private final String USER_PASSWORD_NEW = UserConstants.NEW_PASSWORD;

        private ChangeUserPasswordDto changeUserPasswordDto;

        @BeforeEach
        void setUp() {

            deviceType = DeviceType.WEB;
            deviceInfo = DeviceConstants.USER_AGENT_UNKNOWN;

            changeUserPasswordDto = ChangeUserPasswordDtoTestBuilder.validChange()
                    .newPassword(USER_PASSWORD_NEW)
                    .newPasswordConfirmation(USER_PASSWORD_NEW)
                    .password(UserConstants.USER_PASSWORD)
                    .build();

        }

        private void setupSuccessfulPasswordChangeMocks(){
            when(authenticationService.getCurrentUser()).thenReturn(user);
            when(jwtUtils.generateAccessToken(user)).thenReturn(JwtConstants.ACCESS_TOKEN);
            when(refreshTokenService.createRefreshToken(user, deviceType)).thenReturn(refreshToken);
            when(jwtUtils.getAccessTokenExpiration()).thenReturn(JwtConstants.ACCESS_TOKEN_EXPIRATION_30_MINUTES);
        }

        @Test
        @DisplayName("When changing password should load user using authentication service")
        void whenChangingPasswordShouldLoadUserUsingAuthenticationService(){
            setupSuccessfulPasswordChangeMocks();

            userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            verify(authenticationService,times(1)).getCurrentUser();
        }

        @Test
        @DisplayName("When changing password should throw UserNotAuthenticatedException if user is not authenticated")
        void whenChangingPasswordShouldThrowExceptionIfUserNotAuthenticated(){
            when(authenticationService.getCurrentUser()).thenThrow(new UserNotAuthenticatedException());

            assertThatThrownBy(() -> userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo))
                    .isInstanceOf(UserNotAuthenticatedException.class);

            verify(userRepository, never()).save(any());
            verify(refreshTokenService, never()).revokeAllUserTokens(any());
        }

        @Test
        @DisplayName("When changing password should throw InvalidPasswordException if old password is wrong")
        void whenChangingPasswordShouldThrowInvalidPasswordExceptionIfOldPasswordIsWrong(){
            when(authenticationService.getCurrentUser()).thenReturn(user);

            changeUserPasswordDto.setPassword(UserConstants.WRONG_USER_PASSWORD);

            assertThatThrownBy(() -> userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo))
                    .isInstanceOf(InvalidPasswordException.class);
            verify(userRepository, never()).save(user);
        }

        @Test
        @DisplayName("When changing password should throw NotMatchingPasswordsException if new password is different than confirmation")
        void whenChangingPasswordShouldThrowNotMatchingPasswordsExceptionIfNewPasswordIsDifferentThanConfirmation(){
            when(authenticationService.getCurrentUser()).thenReturn(user);

            changeUserPasswordDto.setNewPassword(UserConstants.WRONG_USER_PASSWORD);

           assertThatThrownBy(() -> userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo))
                   .isInstanceOf(NotMatchingPasswordsException.class);
            verify(userRepository, never()).save(user);
        }

        @Test
        @DisplayName("When changing password should encode password with PasswordEncoder")
        void whenChangingPasswordShouldEncodePasswordWithPasswordEncoder(){
            setupSuccessfulPasswordChangeMocks();

            userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            verify(passwordEncoder, times(1)).encode(changeUserPasswordDto.getNewPassword());
        }


        @Test
        @DisplayName("When changing password should set correct password hash on user")
        void whenChangingPasswordShouldSetCorrectPasswordHashOnUser() {
            setupSuccessfulPasswordChangeMocks();

            userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            assertThat(passwordEncoder.matches(USER_PASSWORD_NEW, user.getPassword())).isTrue();

        }

        @Test
        @DisplayName("When changing password should update last credentials change time field")
        void whenChangingPasswordShouldUpdateLastCredentialsChangeTimeField() {
            setupSuccessfulPasswordChangeMocks();

            userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            assertThat(user.getLastCredentialsChangeTime()).isEqualTo(TimeConstants.NOW);
        }

        @Test
        @DisplayName("When changing password should save updated user")
        void whenChangingPasswordShouldSaveUpdatedUser() {
            setupSuccessfulPasswordChangeMocks();

            userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            verify(userRepository,times(1)).save(user);
        }

        @Test
        @DisplayName("When changing password should generate new access token for user")
        void whenChangingPasswordShouldGenerateNewAccessTokenForUser(){
             setupSuccessfulPasswordChangeMocks();

            userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            verify(jwtUtils, times(1)).generateAccessToken(user);
        }

        @Test
        @DisplayName("When changing password should create new refresh token for user")
        void whenChangingPasswordShouldCreateNewRefreshTokenForUser(){
            setupSuccessfulPasswordChangeMocks();

            userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            verify(refreshTokenService, times(1)).createRefreshToken(user, deviceType);
        }
        @Test
        @DisplayName("When changing password should revoke all user refresh tokens")
        void whenChangingPasswordShouldRevokeAllUserRefreshTokens(){
            setupSuccessfulPasswordChangeMocks();

            userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            verify(refreshTokenService, times(1)).revokeAllUserTokens(UserConstants.FIRST_USER_ID);
        }
        @Test
        @DisplayName("When changing password should revoke tokens after saving user")
        void whenChangingPasswordShouldRevokeTokensAfterSavingUser(){
            setupSuccessfulPasswordChangeMocks();

            InOrder inOrder = inOrder(userRepository, refreshTokenService);

            userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            inOrder.verify(userRepository).save(user);
            inOrder.verify(refreshTokenService).revokeAllUserTokens(UserConstants.FIRST_USER_ID);
        }

        @Test
        @DisplayName("When changing password should create new tokens after revoking old ones")
        void whenChangingPasswordShouldCreateNewTokensAfterRevokingOldOnes(){
            setupSuccessfulPasswordChangeMocks();

            InOrder inOrder = inOrder(refreshTokenService, jwtUtils);

            userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            inOrder.verify(refreshTokenService).revokeAllUserTokens(UserConstants.FIRST_USER_ID);
            inOrder.verify(jwtUtils).generateAccessToken(user);
            inOrder.verify(refreshTokenService).createRefreshToken(user, deviceType);
        }
        @Test
        @DisplayName("When changing password should return correct tokens on success")
        void whenChangingPasswordShouldReturnCorrectTokensOnSuccess(){
            setupSuccessfulPasswordChangeMocks();

            AuthenticationResponse response = userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response).isNotNull();
                softly.assertThat(response.getAccessToken()).isEqualTo(JwtConstants.ACCESS_TOKEN);
                softly.assertThat(response.getRefreshToken()).isEqualTo(refreshToken.getToken());
                softly.assertThat(response.getAccessTokenExpiration()).isEqualTo(JwtConstants.ACCESS_TOKEN_EXPIRATION_30_MINUTES);
            });
        }

    }



    /*
    ********************************************************************************************************************
    *                                      CHANGE USER EMAIL TESTS
    ********************************************************************************************************************
    */

    @Nested
    @DisplayName("Change user email tests:")
    class ChangeUserEmailTests {
        private ChangeUserEmailDto changeEmailDto;

        @BeforeEach
        void setUp() {
            deviceType = DeviceType.WEB;
            deviceInfo = DeviceConstants.USER_AGENT_UNKNOWN;
            
            changeEmailDto = ChangeUserEmailDtoTestBuilder.validChange()
                    .password(UserConstants.USER_PASSWORD)
                    .newEmail(UserConstants.SECOND_USER_EMAIL)
                    .newEmailConfirmation(UserConstants.SECOND_USER_EMAIL)
                    .build();

        }
        private void setupSuccessfulEmailChangeMocks(){
            when(authenticationService.getCurrentUser()).thenReturn(user);
            when(userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL)).thenReturn(Optional.empty());
            when(jwtUtils.generateAccessToken(user)).thenReturn(JwtConstants.ACCESS_TOKEN);
            when(refreshTokenService.createRefreshToken(user, deviceType)).thenReturn(refreshToken);
            when(jwtUtils.getAccessTokenExpiration()).thenReturn(JwtConstants.ACCESS_TOKEN_EXPIRATION_30_MINUTES);
        }

        @Test
        @DisplayName("When changing user email should load user using authentication service")
        void whenChangingUserEmailShouldLoadUserUsingAuthenticationService(){
            setupSuccessfulEmailChangeMocks();

            userService.changeEmail(changeEmailDto, deviceType, deviceInfo);

            verify(authenticationService,times(1)).getCurrentUser();
        }

        @Test
        @DisplayName("When changing user email should throw UserNotAuthenticatedException if user is not authenticated")
        void whenChangingUserEmailShouldThrowExceptionIfUserNotAuthenticated(){
            when(authenticationService.getCurrentUser()).thenThrow(new UserNotAuthenticatedException());

            assertThatThrownBy(() -> userService.changeEmail(changeEmailDto, deviceType, deviceInfo))
                    .isInstanceOf(UserNotAuthenticatedException.class);

            verify(userRepository, never()).save(any());
            verify(refreshTokenService, never()).revokeAllUserTokens(any());
        }

        @Test
        @DisplayName("When changing user email should throw InvalidPasswordException if provided password is wrong")
        public void whenChangingUserEmailShouldThrowInvalidPasswordExceptionIfProvidedPasswordIsWrong(){
            when(authenticationService.getCurrentUser()).thenReturn(user);

            changeEmailDto.setPassword(UserConstants.WRONG_USER_PASSWORD);

            assertThatThrownBy(() -> userService.changeEmail(changeEmailDto, deviceType, deviceInfo))
                    .isInstanceOf(InvalidPasswordException.class);
            verify(userRepository, never()).save(any(User.class));
            verify(refreshTokenService, never()).revokeAllUserTokens(any(UUID.class));
            verify(jwtUtils, never()).generateAccessToken(any(User.class));
            verify(refreshTokenService, never()).createRefreshToken(any(User.class), any(DeviceType.class));
        }

        @Test
        @DisplayName("When changing user email should throw SameEmailException")
        void whenChangingUserEmailShouldThrowSameEmailExceptionIfNewEmailIsExactlyTheSameAsOldEmail(){
            when(authenticationService.getCurrentUser()).thenReturn(user);

            changeEmailDto.setNewEmail(UserConstants.FIRST_USER_EMAIL);
            changeEmailDto.setNewEmailConfirmation(UserConstants.FIRST_USER_EMAIL);

            assertThatThrownBy(() -> userService.changeEmail(changeEmailDto, deviceType, deviceInfo))
                    .isInstanceOf(SameEmailException.class);
            verify(userRepository, never()).save(any(User.class));
            verify(refreshTokenService, never()).revokeAllUserTokens(any(UUID.class));
            verify(jwtUtils, never()).generateAccessToken(any(User.class));
            verify(refreshTokenService, never()).createRefreshToken(any(User.class), any(DeviceType.class));
        }

        @Test
        @DisplayName("When changing user email should throw NotMatchingEmailsException if new email and confirmation are different")
        void whenChangingUserEmailShouldThrowNotMatchingEmailsExceptionIfNewEmailAndConfirmationAreDifferent(){
            when(authenticationService.getCurrentUser()).thenReturn(user);

            changeEmailDto.setNewEmailConfirmation(InvalidInputConstants.DIFFERENT_EMAIL);

            assertThatThrownBy(() -> userService.changeEmail(changeEmailDto, deviceType, deviceInfo))
                    .isInstanceOf(NotMatchingEmailsException.class);
            verify(userRepository, never()).save(any(User.class));
            verify(refreshTokenService, never()).revokeAllUserTokens(any(UUID.class));
            verify(jwtUtils, never()).generateAccessToken(any(User.class));
            verify(refreshTokenService, never()).createRefreshToken(any(User.class), any(DeviceType.class));
        }


        @Test
        @DisplayName("When changing user email should throw UserAlreadyExistException if email is already in database")
        void whenChangingUserEmailShouldThrowUserAlreadyExistExceptionIfEmailIsAlreadyInDatabase(){
            when(authenticationService.getCurrentUser()).thenReturn(user);
            when(userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL)).thenReturn(userOptional);

            assertThatThrownBy(() -> userService.changeEmail(changeEmailDto, deviceType, deviceInfo))
                    .isInstanceOf(UserAlreadyExistException.class);
            verify(userRepository, never()).save(any(User.class));
            verify(refreshTokenService, never()).revokeAllUserTokens(any(UUID.class));
            verify(jwtUtils, never()).generateAccessToken(any(User.class));
            verify(refreshTokenService, never()).createRefreshToken(any(User.class), any(DeviceType.class));
        }

        @Test
        @DisplayName("When changing user email should throw SameEmailException regardless of case")
        void whenChangingUserEmailShouldThrowUserAlreadyExistExceptionRegardlessOfCase(){
            when(authenticationService.getCurrentUser()).thenReturn(user);

            String userEmailInUpperCase = UserConstants.FIRST_USER_EMAIL.toUpperCase();

            changeEmailDto.setNewEmail(userEmailInUpperCase);
            changeEmailDto.setNewEmailConfirmation(userEmailInUpperCase);


            assertThatThrownBy(() -> userService.changeEmail(changeEmailDto, deviceType, deviceInfo))
                    .isInstanceOf(SameEmailException.class);
        }

        @Test
        @DisplayName("When changing user email should accept confirmation with different case")
        void whenChangingUserEmailShouldAcceptConfirmationWithDifferentCase(){
            setupSuccessfulEmailChangeMocks();

            changeEmailDto.setNewEmailConfirmation(UserConstants.SECOND_USER_EMAIL.toUpperCase());

            assertThatCode(() -> userService.changeEmail(changeEmailDto, deviceType, deviceInfo))
                    .doesNotThrowAnyException();
            assertThat(user.getEmail()).isEqualTo(UserConstants.SECOND_USER_EMAIL);
        }

        @Test
        @DisplayName("When changing user email should update user email")
        void whenChangingUserEmailShouldUpdateUserEmail(){
           setupSuccessfulEmailChangeMocks();

            userService.changeEmail(changeEmailDto, deviceType, deviceInfo);

            assertThat(user.getEmail()).isEqualTo(UserConstants.SECOND_USER_EMAIL);
        }
        @Test
        @DisplayName("When changing user email should convert email to lowercase before saving")
        void whenChangingUserEmailShouldConvertEmailToLowercaseBeforeSaving(){
            when(authenticationService.getCurrentUser()).thenReturn(user);
            when(jwtUtils.generateAccessToken(user)).thenReturn(JwtConstants.ACCESS_TOKEN);
            when(refreshTokenService.createRefreshToken(user, deviceType)).thenReturn(refreshToken);
            when(jwtUtils.getAccessTokenExpiration()).thenReturn(JwtConstants.ACCESS_TOKEN_EXPIRATION_30_MINUTES);

            String mixedCaseEmail = UserConstants.SECOND_USER_EMAIL.substring(0,5).toUpperCase() + UserConstants.SECOND_USER_EMAIL.substring(5);
            changeEmailDto.setNewEmail(mixedCaseEmail);
            changeEmailDto.setNewEmailConfirmation(mixedCaseEmail);

            when(userRepository.findByIgnoreCaseEmail(mixedCaseEmail)).thenReturn(Optional.empty());

            userService.changeEmail(changeEmailDto, deviceType, deviceInfo);

            assertThat(user.getEmail())
                    .as("Expected email to be converted to lowercase")
                    .isEqualTo(UserConstants.SECOND_USER_EMAIL);
        }

        @Test
        @DisplayName("When changing user email should update last credentials change time field")
        void whenChangingUserEmailShouldUpdateLastCredentialsChangeTimeField(){
            setupSuccessfulEmailChangeMocks();

            userService.changeEmail(changeEmailDto, deviceType, deviceInfo);

            assertThat(user.getLastCredentialsChangeTime()).isEqualTo(TimeConstants.NOW);
        }

        @Test
        @DisplayName("When changing user email should save updated user in database")
        void whenChangingUserEmailShouldSaveUpdatedUserInDatabase(){
           setupSuccessfulEmailChangeMocks();

            userService.changeEmail(changeEmailDto, deviceType, deviceInfo);

            ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository,times(1)).save(userArgumentCaptor.capture());

            User capturedUser = userArgumentCaptor.getValue();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(capturedUser.getEmail()).isEqualTo(UserConstants.SECOND_USER_EMAIL);
                softly.assertThat(capturedUser.getLastCredentialsChangeTime()).isEqualTo(TimeConstants.NOW);
            });
        }

        @Test
        @DisplayName("When changing user email should revoke all user refresh tokens")
        void whenChangingUserEmailShouldRevokeAllUserRefreshTokens(){
            setupSuccessfulEmailChangeMocks();

            userService.changeEmail(changeEmailDto, deviceType, deviceInfo);

            verify(refreshTokenService,times(1)).revokeAllUserTokens(UserConstants.FIRST_USER_ID);
        }

        @Test
        @DisplayName("When changing user email should generate new access token")
        void whenChangingUserEmailShouldGenerateNewAccessToken(){
            setupSuccessfulEmailChangeMocks();

            userService.changeEmail(changeEmailDto, deviceType, deviceInfo);

            verify(jwtUtils,times(1)).generateAccessToken(user);
        }
        @Test
        @DisplayName("When changing user email should create new refresh token")
        void whenChangingUserEmailShouldCreateNewRefreshToken(){
            setupSuccessfulEmailChangeMocks();

            userService.changeEmail(changeEmailDto, deviceType, deviceInfo);

            verify(refreshTokenService,times(1)).createRefreshToken(user, deviceType);
        }
        @Test
        @DisplayName("When changing user email should revoke tokens after saving user")
        void whenChangingUserEmailShouldRevokeTokensAfterSavingUser(){
            setupSuccessfulEmailChangeMocks();

            InOrder inOrder = inOrder(userRepository, refreshTokenService);

            userService.changeEmail(changeEmailDto, deviceType, deviceInfo);

            inOrder.verify(userRepository).save(user);
            inOrder.verify(refreshTokenService).revokeAllUserTokens(UserConstants.FIRST_USER_ID);
        }

        @Test
        @DisplayName("When changing email should create new tokens after revoking old ones")
        void whenChangingUserEmailShouldCreateNewTokensAfterRevokingOldOnes(){
            setupSuccessfulEmailChangeMocks();

            InOrder inOrder = inOrder(refreshTokenService, jwtUtils);

            userService.changeEmail(changeEmailDto, deviceType, deviceInfo);

            inOrder.verify(refreshTokenService).revokeAllUserTokens(UserConstants.FIRST_USER_ID);
            inOrder.verify(jwtUtils).generateAccessToken(user);
            inOrder.verify(refreshTokenService).createRefreshToken(user, deviceType);
        }

        @Test
        @DisplayName("When changing user email should return correct tokens")
        void whenChangingUserEmailShouldReturnCorrectTokens(){
            setupSuccessfulEmailChangeMocks();

            AuthenticationResponse response = userService.changeEmail(changeEmailDto, deviceType, deviceInfo);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getAccessToken()).isEqualTo(JwtConstants.ACCESS_TOKEN);
                softly.assertThat(response.getRefreshToken()).isEqualTo(refreshToken.getToken());
                softly.assertThat(response.getAccessTokenExpiration()).isEqualTo(JwtConstants.ACCESS_TOKEN_EXPIRATION_30_MINUTES);
            });
        }

    }

/*
     ********************************************************************************************************************
     *                                       CHANGE USER DETAILS TESTS
     ********************************************************************************************************************
     */

    @Nested
    @DisplayName("Change user details tests:")
    class ChangeUserDetails{

        private ChangeUserDetailsDto changeUserDetailsDto;
        private final String USER_FIRST_NAME_NEW = UserConstants.SECOND_USER_FIRST_NAME;
        private final String USER_LAST_NAME_NEW = UserConstants.SECOND_USER_LAST_NAME;
        private final String USER_HOME_CITY_NEW_KRAKOW = CitiesConstants.KRAKOW_NAME;

        private City cityKrakow;

        @BeforeEach
        void setUp() {
            changeUserDetailsDto = ChangeUserDetailsDtoTestBuilder.validUpdate()
                    .firstName(USER_FIRST_NAME_NEW)
                    .lastName(USER_LAST_NAME_NEW)
                    .homeCity(USER_HOME_CITY_NEW_KRAKOW)
                    .build();
            cityKrakow = CityTestBuilder.krakow().build();
        }

        private void setupSuccessfulUserDetailsChangeMocks(){
            when(authenticationService.getCurrentUser()).thenReturn(user);
            when(cityService.getCityByNameOrCreate(USER_HOME_CITY_NEW_KRAKOW)).thenReturn(cityKrakow);
            when(userRepository.save(user)).thenReturn(user);

        }

        @Test
        @DisplayName("When updating user details should load current user")
        void whenUpdatingUserDetailsShouldLoadCurrentUser(){
            setupSuccessfulUserDetailsChangeMocks();

            userService.changeDetails(changeUserDetailsDto);

            verify(authenticationService, times(1)).getCurrentUser();
        }

        @Test
        @DisplayName("When updating user details should throw UserNotAuthenticatedException if user is not authenticated")
        void whenUpdatingUserDetailsShouldThrowExceptionIfUserNotAuthenticated(){
            when(authenticationService.getCurrentUser()).thenThrow(new UserNotAuthenticatedException());

            assertThatThrownBy(() -> userService.changeDetails(changeUserDetailsDto))
                    .isInstanceOf(UserNotAuthenticatedException.class);

            verify(cityService, never()).getCityByNameOrCreate(any());
        }

        @Test
        @DisplayName("When updating user details should update user first name and last name")
        void whenUpdatingUserDetailsShouldUpdateUserFirstNameAndLastName() {
            changeUserDetailsDto.setHomeCity(CitiesConstants.WARSAW_NAME);
            when(authenticationService.getCurrentUser()).thenReturn(user);
            when(userRepository.save(user)).thenReturn(user);

            UserProfileDto result = userService.changeDetails(changeUserDetailsDto);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(user.getFirstName()).isEqualTo(USER_FIRST_NAME_NEW);
                softly.assertThat(user.getLastName()).isEqualTo(USER_LAST_NAME_NEW);
                softly.assertThat(user.getHomeCity()).isEqualTo(cityWarsaw);
            });
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getFirstName()).isEqualTo(USER_FIRST_NAME_NEW);
                softly.assertThat(result.getLastName()).isEqualTo(USER_LAST_NAME_NEW);
                softly.assertThat(result.getHomeCity()).isEqualTo(CitiesConstants.WARSAW_NAME);
            });

            verify(cityService, never()).getCityByNameOrCreate(any());
        }

        @Test
        @DisplayName("When updating user details should update user home city")
        void whenUpdatingUserDetailsShouldUpdateUserHomeCity() {
            setupSuccessfulUserDetailsChangeMocks();

            UserProfileDto result = userService.changeDetails(changeUserDetailsDto);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(user.getFirstName()).isEqualTo(USER_FIRST_NAME_NEW);
                softly.assertThat(user.getLastName()).isEqualTo(USER_LAST_NAME_NEW);
                softly.assertThat(user.getHomeCity()).isEqualTo(cityKrakow);
            });
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getFirstName()).isEqualTo(USER_FIRST_NAME_NEW);
                softly.assertThat(result.getLastName()).isEqualTo(USER_LAST_NAME_NEW);
                softly.assertThat(result.getHomeCity()).isEqualTo(USER_HOME_CITY_NEW_KRAKOW);
            });

            verify(cityService, times(1)).getCityByNameOrCreate(any());
        }
        @Test
        @DisplayName("When updating user details should not change city if name differs only in case")
        void whenUpdatingUserDetailsShouldNotChangeCityIfNameDiffersOnlyInCase(){
            when(authenticationService.getCurrentUser()).thenReturn(user);
            when(userRepository.save(user)).thenReturn(user);

            changeUserDetailsDto.setHomeCity(CitiesConstants.WARSAW_NAME.toUpperCase());

            UserProfileDto result = userService.changeDetails(changeUserDetailsDto);

            assertThat(user.getHomeCity()).as("Expected city to remain the same").isEqualTo(cityWarsaw);
            verify(cityService, never().description("Expected to not call city service for same city with different case"))
                    .getCityByNameOrCreate(any());
        }

        @Test
        @DisplayName("When updating user details should pass correct city name to city service")
        void whenUpdatingUserDetailsShouldPassCorrectCityNameToCityService(){
            setupSuccessfulUserDetailsChangeMocks();

            userService.changeDetails(changeUserDetailsDto);

            ArgumentCaptor<String> cityNameCaptor = ArgumentCaptor.forClass(String.class);
            verify(cityService, times(1)).getCityByNameOrCreate(cityNameCaptor.capture());

            assertThat(cityNameCaptor.getValue())
                    .as("Expected to pass correct city name to city service")
                    .isEqualTo(USER_HOME_CITY_NEW_KRAKOW);
        }

        @Test
        @DisplayName("When updating user details should save it in database")
        void whenUpdatingUserDetailsShouldSaveItInDatabase(){
            setupSuccessfulUserDetailsChangeMocks();

            userService.changeDetails(changeUserDetailsDto);

            verify(userRepository, times(1)).save(user);
        }
    }

    @Nested
    @DisplayName("Register FCM token tests:")
    class RegisterFcmTokenTests {

        @Test
        @DisplayName("When registering user fcm token should load current user and save updated token")
        void whenRegisteringUserFcmTokenShouldLoadCurrentUserAndSaveUpdatedToken() {
            RegisterFcmTokenRequest registerFcmTokenRequest = RegisterFcmTokenRequestTestBuilder.updatedFirstUserToken().build();
            when(authenticationService.getCurrentUser()).thenReturn(user);

            boolean registrationResult = userService.registerUserFcmToken(registerFcmTokenRequest);

            assertThat(registrationResult).isTrue();
            assertThat(user.getFcmAndroidToken()).isEqualTo(UserConstants.FIRST_USER_NEW_FCM_TOKEN);
            verify(authenticationService, times(1)).getCurrentUser();
            verify(userRepository, times(1)).save(user);
        }

        @Test
        @DisplayName("When registering user fcm token should return false if user is not authenticated")
        void whenRegisteringUserFcmTokenShouldReturnFalseIfUserIsNotAuthenticated() {
            RegisterFcmTokenRequest registerFcmTokenRequest = RegisterFcmTokenRequestTestBuilder.firstUserToken().build();
            when(authenticationService.getCurrentUser()).thenThrow(new UserNotAuthenticatedException());

            boolean registrationResult = userService.registerUserFcmToken(registerFcmTokenRequest);

            assertThat(registrationResult).isFalse();
            verify(userRepository, never()).save(any(User.class));
        }
    }


    @Nested
    @DisplayName("Ban user tests:")
    class BanUserTests {

        @Test
        @DisplayName("When banning user should load user from database")
        public void whenBanningUserShouldLoadUserFromDatabase(){
            when(userRepository.findById(UserConstants.FIRST_USER_ID)).thenReturn(userOptional);

            userService.banUser(UserConstants.FIRST_USER_ID);

            verify(userRepository, times(1)).findById(UserConstants.FIRST_USER_ID);
        }

        @Test
        @DisplayName("When banning user should throw UserNotFoundException if user with given id does not exist")
        public void whenBanningUserShouldThrowUserNotFoundExceptionIfUserWithGivenIdDoesNotExist(){
            when(userRepository.findById(UserConstants.FIRST_USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.banUser(UserConstants.FIRST_USER_ID))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userRepository, never()).save(any(User.class));
            verify(refreshTokenService, never()).revokeAllUserTokens(any(UUID.class));
        }

        @Test
        @DisplayName("When banning user should set user banned field to true")
        public void whenBanningUserShouldSetUserBannedFieldToTrue(){
            when(userRepository.findById(UserConstants.FIRST_USER_ID)).thenReturn(userOptional);

            userService.banUser(UserConstants.FIRST_USER_ID);

            assertThat(user.isBanned()).isTrue();
        }
        @Test
        @DisplayName("When banning user should save banned user in database")
        public void whenBanningUserShouldSaveBannedUserInDatabase(){
            when(userRepository.findById(UserConstants.FIRST_USER_ID)).thenReturn(userOptional);

            userService.banUser(UserConstants.FIRST_USER_ID);

            verify(userRepository, times(1)).save(user);
        }
        @Test
        @DisplayName("When banning user should revoke all user refresh tokens")
        public void whenBanningUserShouldRevokeAllUserRefreshTokens(){
            when(userRepository.findById(UserConstants.FIRST_USER_ID)).thenReturn(userOptional);

            userService.banUser(UserConstants.FIRST_USER_ID);

            verify(refreshTokenService, times(1)).revokeAllUserTokens(UserConstants.FIRST_USER_ID);
        }
    }

}
