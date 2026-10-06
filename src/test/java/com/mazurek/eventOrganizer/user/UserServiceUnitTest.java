package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.auth.AuthUserLockService;
import com.mazurek.eventOrganizer.auth.AuthenticationServiceImpl;
import com.mazurek.eventOrganizer.auth.EmailChangeService;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationResponse;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityServiceImpl;
import com.mazurek.eventOrganizer.exception.auth.UserNotAuthenticatedException;
import com.mazurek.eventOrganizer.exception.user.*;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.jwt.IssuedRefreshToken;
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
import com.mazurek.eventOrganizer.user.dto.*;
import com.mazurek.eventOrganizer.testData.builders.IssuedRefreshTokenTestBuilder;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService unit tests:")
class UserServiceUnitTest {


    @Mock private UserRepository userRepository;
    @Mock private AuthenticationServiceImpl authenticationService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private AccountSessionInvalidationService accountSessionInvalidationService;
    @Mock private JwtUtils jwtUtils;
    @Mock private CityServiceImpl cityService;
    private final Clock clock = TimeConstants.FIXED_CLOCK;
    @Mock private EmailChangeService emailChangeService;
    @Mock private AuthUserLockService authUserLockService;
    private BCryptPasswordEncoder passwordEncoder = Mockito.spy(new BCryptPasswordEncoder());
    private UserService userService;

    private User user;
    private Optional<User> userOptional;

    private City cityWarsaw;
    private Role ROLE_USER;



    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                userRepository,
                authenticationService,
                refreshTokenService,
                accountSessionInvalidationService,
                jwtUtils,
                cityService,
                passwordEncoder,
                clock,
                emailChangeService,
                authUserLockService
        );

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
       assertThatThrownBy(() -> userService.getUserById(UserConstants.NOT_EXISTING_USER_ID))
               .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("When getting current user should return private account data")
    void whenGettingCurrentUserShouldReturnPrivateAccountData() {
        when(authenticationService.getCurrentUser()).thenReturn(user);

        CurrentUserDto result = userService.getCurrentUser();
        assertThat(result).isNotNull();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getId()).isEqualTo(user.getId());
            softly.assertThat(result.getFirstName()).isEqualTo(user.getFirstName());
            softly.assertThat(result.getLastName()).isEqualTo(user.getLastName());
            softly.assertThat(result.getEmail()).isEqualTo(user.getEmail());
            softly.assertThat(result.getHomeCity()).isEqualTo(user.getHomeCity().getName());
            softly.assertThat(result.getTimeZone()).isEqualTo(user.getTimeZone());
        });
        verify(authenticationService).getCurrentUser();
    }

    @Test
    @DisplayName("When getting current user should propagate authentication failure")
    void whenGettingCurrentUserShouldPropagateAuthenticationFailure() {
        when(authenticationService.getCurrentUser()).thenThrow(new UserNotAuthenticatedException());

        assertThatThrownBy(userService::getCurrentUser)
                .isInstanceOf(UserNotAuthenticatedException.class);
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
        private DeviceType deviceType;
        private RefreshToken refreshToken;
        private String rawRefreshToken;

        @BeforeEach
        void setUp() {

            deviceType = DeviceType.WEB;

            Instant tokenCreateDate = TimeConstants.NOW;
            IssuedRefreshToken issuedRefreshToken = RefreshTokenTestBuilder.firstRefreshTokenForUser(user)
                    .id(JwtConstants.TOKEN_ID_ONE)
                    .createdAt(tokenCreateDate)
                    .lastUsedAt(tokenCreateDate)
                    .expiryDate(tokenCreateDate.plusMillis(JwtConstants.REFRESH_TOKEN_EXPIRATION_SHORT))
                    .deviceType(DeviceType.WEB)
                    .buildIssued();
            refreshToken = issuedRefreshToken.refreshToken();
            rawRefreshToken = issuedRefreshToken.rawToken();

            changeUserPasswordDto = ChangeUserPasswordDtoTestBuilder.validChange()
                    .newPassword(USER_PASSWORD_NEW)
                    .newPasswordConfirmation(USER_PASSWORD_NEW)
                    .password(UserConstants.USER_PASSWORD)
                    .build();

        }

        private void setupSuccessfulPasswordChangeMocks(){
            when(authenticationService.getCurrentUser()).thenReturn(user);
            when(authUserLockService.lockById(user.getId())).thenReturn(Optional.of(user));
            when(jwtUtils.generateAccessToken(user)).thenReturn(JwtConstants.ACCESS_TOKEN);
            when(refreshTokenService.issueRefreshToken(user, deviceType))
                    .thenReturn(new IssuedRefreshTokenTestBuilder()
                            .refreshToken(refreshToken)
                            .rawToken(rawRefreshToken)
                            .build());
            when(jwtUtils.getAccessTokenExpiration()).thenReturn(JwtConstants.ACCESS_TOKEN_EXPIRATION_30_MINUTES);
        }

        @Test
        @DisplayName("When changing password should load user using authentication service")
        void whenChangingPasswordShouldLoadUserUsingAuthenticationService(){
            setupSuccessfulPasswordChangeMocks();

            userService.changePassword(changeUserPasswordDto, deviceType);

            verify(authenticationService,times(1)).getCurrentUser();
        }

        @Test
        @DisplayName("When changing password should throw UserNotAuthenticatedException if user is not authenticated")
        void whenChangingPasswordShouldThrowExceptionIfUserNotAuthenticated(){
            when(authenticationService.getCurrentUser()).thenThrow(new UserNotAuthenticatedException());

            assertThatThrownBy(() -> userService.changePassword(changeUserPasswordDto, deviceType))
                    .isInstanceOf(UserNotAuthenticatedException.class);

            verify(userRepository, never()).save(any());
            verify(accountSessionInvalidationService, never()).invalidateAll(any());
        }

        @Test
        @DisplayName("When changing password should throw InvalidPasswordException if old password is wrong")
        void whenChangingPasswordShouldThrowInvalidPasswordExceptionIfOldPasswordIsWrong(){
            when(authenticationService.getCurrentUser()).thenReturn(user);
            when(authUserLockService.lockById(user.getId())).thenReturn(Optional.of(user));

            changeUserPasswordDto.setPassword(UserConstants.WRONG_USER_PASSWORD);
            String originalPasswordHash = user.getPassword();

            assertThatThrownBy(() -> userService.changePassword(changeUserPasswordDto, deviceType))
                    .isInstanceOf(InvalidPasswordException.class);
            verify(userRepository, never()).save(user);
            assertCredentialsUnchanged(originalPasswordHash);
        }

        @Test
        @DisplayName("When changing password should throw NotMatchingPasswordsException if new password is different than confirmation")
        void whenChangingPasswordShouldThrowNotMatchingPasswordsExceptionIfNewPasswordIsDifferentThanConfirmation(){
            when(authenticationService.getCurrentUser()).thenReturn(user);
            when(authUserLockService.lockById(user.getId())).thenReturn(Optional.of(user));

            changeUserPasswordDto.setNewPassword(UserConstants.WRONG_USER_PASSWORD);
            String originalPasswordHash = user.getPassword();

           assertThatThrownBy(() -> userService.changePassword(changeUserPasswordDto, deviceType))
                   .isInstanceOf(NotMatchingPasswordsException.class);
            verify(userRepository, never()).save(user);
            assertCredentialsUnchanged(originalPasswordHash);
        }

        private void assertCredentialsUnchanged(String expectedHash) {
            assertThat(user.getPassword()).isEqualTo(expectedHash);
            assertThat(user.getLastCredentialsChangeTime()).isEqualTo(TimeConstants.ONE_WEEK_AGO);
            assertThat(user.getSecurityVersion()).isEqualTo(UserConstants.DEFAULT_SECURITY_VERSION);
            verifyNoInteractions(accountSessionInvalidationService, refreshTokenService, jwtUtils);
        }

        @Test
        @DisplayName("When changing password should encode password with PasswordEncoder")
        void whenChangingPasswordShouldEncodePasswordWithPasswordEncoder(){
            setupSuccessfulPasswordChangeMocks();

            userService.changePassword(changeUserPasswordDto, deviceType);

            verify(passwordEncoder, times(1)).encode(changeUserPasswordDto.getNewPassword());
        }


        @Test
        @DisplayName("When changing password should set correct password hash on user")
        void whenChangingPasswordShouldSetCorrectPasswordHashOnUser() {
            setupSuccessfulPasswordChangeMocks();

            userService.changePassword(changeUserPasswordDto, deviceType);

            assertThat(passwordEncoder.matches(USER_PASSWORD_NEW, user.getPassword())).isTrue();

        }

        @Test
        @DisplayName("When changing password should update last credentials change time field")
        void whenChangingPasswordShouldUpdateLastCredentialsChangeTimeField() {
            setupSuccessfulPasswordChangeMocks();

            userService.changePassword(changeUserPasswordDto, deviceType);

            assertThat(user.getLastCredentialsChangeTime()).isEqualTo(TimeConstants.NOW);
        }

        @Test
        @DisplayName("When changing password should save updated user")
        void whenChangingPasswordShouldSaveUpdatedUser() {
            setupSuccessfulPasswordChangeMocks();

            userService.changePassword(changeUserPasswordDto, deviceType);

            verify(userRepository,times(1)).save(user);
        }

        @Test
        @DisplayName("When changing password should generate new access token for user")
        void whenChangingPasswordShouldGenerateNewAccessTokenForUser(){
             setupSuccessfulPasswordChangeMocks();

            userService.changePassword(changeUserPasswordDto, deviceType);

            verify(jwtUtils, times(1)).generateAccessToken(user);
        }

        @Test
        @DisplayName("When changing password should create new refresh token for user")
        void whenChangingPasswordShouldCreateNewRefreshTokenForUser(){
            setupSuccessfulPasswordChangeMocks();

            userService.changePassword(changeUserPasswordDto, deviceType);

            verify(refreshTokenService).issueRefreshToken(user, deviceType);
        }
        @Test
        @DisplayName("When changing password should revoke all user refresh tokens")
        void whenChangingPasswordShouldRevokeAllUserRefreshTokens(){
            setupSuccessfulPasswordChangeMocks();

            userService.changePassword(changeUserPasswordDto, deviceType);

            verify(accountSessionInvalidationService).invalidateAll(user);
        }
        @Test
        @DisplayName("When changing password should revoke tokens after saving user")
        void whenChangingPasswordShouldRevokeTokensAfterSavingUser(){
            setupSuccessfulPasswordChangeMocks();

            InOrder inOrder = inOrder(userRepository, accountSessionInvalidationService);

            userService.changePassword(changeUserPasswordDto, deviceType);

            inOrder.verify(userRepository).save(user);
            inOrder.verify(accountSessionInvalidationService).invalidateAll(user);
        }

        @Test
        @DisplayName("When changing password should create new tokens after revoking old ones")
        void whenChangingPasswordShouldCreateNewTokensAfterRevokingOldOnes(){
            setupSuccessfulPasswordChangeMocks();

            InOrder inOrder = inOrder(accountSessionInvalidationService, jwtUtils, refreshTokenService);

            userService.changePassword(changeUserPasswordDto, deviceType);

            inOrder.verify(accountSessionInvalidationService).invalidateAll(user);
            inOrder.verify(jwtUtils).generateAccessToken(user);
            inOrder.verify(refreshTokenService).issueRefreshToken(user, deviceType);
        }
        @Test
        @DisplayName("When changing password should return correct tokens on success")
        void whenChangingPasswordShouldReturnCorrectTokensOnSuccess(){
            setupSuccessfulPasswordChangeMocks();

            AuthenticationResponse response = userService.changePassword(changeUserPasswordDto, deviceType);

            assertThat(response).isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getAccessToken()).isEqualTo(JwtConstants.ACCESS_TOKEN);
                softly.assertThat(response.getRefreshToken()).isEqualTo(rawRefreshToken);
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
    class ChangeUserDetailsTests{

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
                    .homeCityExternalId(CitiesConstants.KRAKOW_EXTERNAL_ID)
                    .build();
            cityKrakow = CityTestBuilder.krakow().build();
        }

        private void setupSuccessfulUserDetailsChangeMocks(){
            when(authUserLockService.lockById(user.getId())).thenReturn(userOptional);
            when(authenticationService.getCurrentUser()).thenReturn(user);
            when(cityService.resolve(changeUserDetailsDto.getHomeCityExternalId())).thenReturn(cityKrakow);
            when(userRepository.save(user)).thenReturn(user);

        }

        @Test
        @DisplayName("When updating user details should load current user")
        void whenUpdatingUserDetailsShouldLoadCurrentUser(){
            setupSuccessfulUserDetailsChangeMocks();

            userService.changeDetails(changeUserDetailsDto);

            verify(authenticationService, times(1)).getCurrentUser();
            InOrder order = inOrder(authUserLockService, cityService, userRepository);
            order.verify(authUserLockService).lockById(user.getId());
            order.verify(cityService).resolve(changeUserDetailsDto.getHomeCityExternalId());
            order.verify(userRepository).save(user);
        }

        @Test
        void whenUpdatingDetailsShouldModifyTheRefreshedLockedUserNotTheAuthenticationSnapshot() {
            User lockedUser = UserTestBuilder.firstUser()
                    .securityVersion(1)
                    .notificationPreferencesVersion(1)
                    .banned(true)
                    .build();
            when(authenticationService.getCurrentUser()).thenReturn(user);
            when(authUserLockService.lockById(user.getId())).thenReturn(Optional.of(lockedUser));
            when(cityService.resolve(changeUserDetailsDto.getHomeCityExternalId())).thenReturn(cityKrakow);
            when(userRepository.save(lockedUser)).thenReturn(lockedUser);

            userService.changeDetails(changeUserDetailsDto);

            assertThat(lockedUser.getFirstName()).isEqualTo(USER_FIRST_NAME_NEW);
            assertThat(lockedUser.getSecurityVersion()).isEqualTo(1);
            assertThat(lockedUser.getNotificationPreferencesVersion()).isEqualTo(1);
            assertThat(lockedUser.isBanned()).isTrue();
            assertThat(user.getFirstName()).isEqualTo(UserConstants.FIRST_USER_FIRST_NAME);
            verify(userRepository).save(same(lockedUser));
        }

        @Test
        void whenUserDisappearsBeforeLockingShouldNotResolveCityOrSaveProfile() {
            when(authenticationService.getCurrentUser()).thenReturn(user);
            when(authUserLockService.lockById(user.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.changeDetails(changeUserDetailsDto))
                    .isInstanceOf(UserNotFoundException.class);

            verifyNoInteractions(cityService, userRepository);
        }

        @Test
        @DisplayName("When updating user details should throw UserNotAuthenticatedException if user is not authenticated")
        void whenUpdatingUserDetailsShouldThrowExceptionIfUserNotAuthenticated(){
            when(authenticationService.getCurrentUser()).thenThrow(new UserNotAuthenticatedException());

            assertThatThrownBy(() -> userService.changeDetails(changeUserDetailsDto))
                    .isInstanceOf(UserNotAuthenticatedException.class);

            verify(cityService, never()).resolve(any());
        }

        @Test
        @DisplayName("When updating user details should update user first name and last name")
        void whenUpdatingUserDetailsShouldUpdateUserFirstNameAndLastName() {
            when(authUserLockService.lockById(user.getId())).thenReturn(userOptional);
            changeUserDetailsDto.setHomeCityExternalId(com.mazurek.eventOrganizer.testData.TestCityData.externalId(CitiesConstants.WARSAW_NAME));
            when(authenticationService.getCurrentUser()).thenReturn(user);
            when(userRepository.save(user)).thenReturn(user);

            CurrentUserDto result = userService.changeDetails(changeUserDetailsDto);
            assertThat(result).isNotNull();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(user.getFirstName()).isEqualTo(USER_FIRST_NAME_NEW);
                softly.assertThat(user.getLastName()).isEqualTo(USER_LAST_NAME_NEW);
                softly.assertThat(user.getHomeCity()).isEqualTo(cityWarsaw);
                softly.assertThat(user.getTimeZone()).isEqualTo(UserConstants.SECOND_USER_TIMEZONE);
            });
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getFirstName()).isEqualTo(USER_FIRST_NAME_NEW);
                softly.assertThat(result.getLastName()).isEqualTo(USER_LAST_NAME_NEW);
                softly.assertThat(result.getHomeCity()).isEqualTo(CitiesConstants.WARSAW_NAME);
                softly.assertThat(result.getEmail()).isEqualTo(UserConstants.FIRST_USER_EMAIL);
                softly.assertThat(result.getTimeZone()).isEqualTo(UserConstants.SECOND_USER_TIMEZONE);
            });

            verify(cityService, never()).resolve(any());
        }

        @Test
        @DisplayName("When updating user details should update user home city")
        void whenUpdatingUserDetailsShouldUpdateUserHomeCity() {
            setupSuccessfulUserDetailsChangeMocks();

            CurrentUserDto result = userService.changeDetails(changeUserDetailsDto);
            assertThat(result).isNotNull();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(user.getFirstName()).isEqualTo(USER_FIRST_NAME_NEW);
                softly.assertThat(user.getLastName()).isEqualTo(USER_LAST_NAME_NEW);
                softly.assertThat(user.getHomeCity()).isEqualTo(cityKrakow);
                softly.assertThat(user.getTimeZone()).isEqualTo(UserConstants.SECOND_USER_TIMEZONE);
            });
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getFirstName()).isEqualTo(USER_FIRST_NAME_NEW);
                softly.assertThat(result.getLastName()).isEqualTo(USER_LAST_NAME_NEW);
                softly.assertThat(result.getHomeCity()).isEqualTo(USER_HOME_CITY_NEW_KRAKOW);
                softly.assertThat(result.getEmail()).isEqualTo(UserConstants.FIRST_USER_EMAIL);
                softly.assertThat(result.getTimeZone()).isEqualTo(UserConstants.SECOND_USER_TIMEZONE);
            });

            verify(cityService, times(1)).resolve(any());
        }
        @Test
        @DisplayName("When updating user details should not change city for the same external identifier")
        void whenUpdatingUserDetailsShouldNotChangeCityForTheSameExternalIdentifier(){
            when(authUserLockService.lockById(user.getId())).thenReturn(userOptional);
            when(authenticationService.getCurrentUser()).thenReturn(user);
            when(userRepository.save(user)).thenReturn(user);

            changeUserDetailsDto.setHomeCityExternalId(com.mazurek.eventOrganizer.testData.TestCityData.externalId(CitiesConstants.WARSAW_NAME));

            CurrentUserDto result = userService.changeDetails(changeUserDetailsDto);
            assertThat(result).isNotNull();

            assertThat(user.getHomeCity()).as("Expected city to remain the same").isEqualTo(cityWarsaw);
            verify(cityService, never().description("Expected to not call city service for same external identifier"))
                    .resolve(any());
        }

        @Test
        @DisplayName("When updating user details should pass correct city identifier to city service")
        void whenUpdatingUserDetailsShouldPassCorrectCityIdentifierToCityService(){
            setupSuccessfulUserDetailsChangeMocks();

            userService.changeDetails(changeUserDetailsDto);

            ArgumentCaptor<String> cityNameCaptor = ArgumentCaptor.forClass(String.class);
            verify(cityService, times(1)).resolve(cityNameCaptor.capture());

            assertThat(cityNameCaptor.getValue())
                    .as("Expected to pass the selected external identifier to city service")
                    .isEqualTo(changeUserDetailsDto.getHomeCityExternalId());
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
    @DisplayName("Change email tests:")
    class ChangeEmailTests {

        private ChangeUserEmailDto request;

        @BeforeEach
        void setUpEmailChangeRequest() {
            request = ChangeUserEmailDtoTestBuilder.validChange()
                    .password(UserConstants.USER_PASSWORD)
                    .newEmail(UserConstants.FIRST_USER_NEW_EMAIL)
                    .newEmailConfirmation(UserConstants.FIRST_USER_NEW_EMAIL)
                    .build();
        }

        @Test
        void whenRequestingEmailChangeShouldRequestConfirmationWithoutChangingCredentialsOrSessions() {
            when(authenticationService.getCurrentUser()).thenReturn(user);
            when(authUserLockService.lockById(user.getId())).thenReturn(Optional.of(user));
            when(userRepository.findByIgnoreCaseEmail(request.getNewEmail())).thenReturn(Optional.empty());

            userService.changeEmail(request);

            verify(emailChangeService).requestChange(user, UserConstants.FIRST_USER_NEW_EMAIL);
            verifyNoInteractions(accountSessionInvalidationService, refreshTokenService, jwtUtils);
            assertThat(user.getEmail()).isEqualTo(UserConstants.FIRST_USER_EMAIL);
        }

        @Test
        void whenUnauthenticatedShouldRejectEmailChange() {
            when(authenticationService.getCurrentUser()).thenThrow(new UserNotAuthenticatedException());

            assertThatThrownBy(() -> userService.changeEmail(request))
                    .isInstanceOf(UserNotAuthenticatedException.class);
            verifyNoInteractions(emailChangeService);
        }

        @Test
        void whenCurrentPasswordIncorrectShouldRejectEmailChange() {
            when(authenticationService.getCurrentUser()).thenReturn(user);
            when(authUserLockService.lockById(user.getId())).thenReturn(Optional.of(user));
            request.setPassword(UserConstants.WRONG_USER_PASSWORD);

            assertThatThrownBy(() -> userService.changeEmail(request))
                    .isInstanceOf(InvalidPasswordException.class);
            verifyNoInteractions(emailChangeService);
        }

        @Test
        void whenEmailMatchesCurrentOrConfirmationDiffersShouldRejectEmailChange() {
            when(authenticationService.getCurrentUser()).thenReturn(user);
            when(authUserLockService.lockById(user.getId())).thenReturn(Optional.of(user));
            request.setNewEmail(UserConstants.FIRST_USER_EMAIL.toUpperCase(Locale.ROOT));
            request.setNewEmailConfirmation(UserConstants.FIRST_USER_EMAIL.toUpperCase(Locale.ROOT));

            assertThatThrownBy(() -> userService.changeEmail(request)).isInstanceOf(SameEmailException.class);

            request.setNewEmail(UserConstants.FIRST_USER_NEW_EMAIL);
            request.setNewEmailConfirmation("different@example.com");
            assertThatThrownBy(() -> userService.changeEmail(request)).isInstanceOf(NotMatchingEmailsException.class);
            verifyNoInteractions(emailChangeService);
        }

        @Test
        void whenAddressAlreadyUsedShouldRejectEmailChange() {
            when(authenticationService.getCurrentUser()).thenReturn(user);
            when(authUserLockService.lockById(user.getId())).thenReturn(Optional.of(user));
            when(userRepository.findByIgnoreCaseEmail(request.getNewEmail()))
                    .thenReturn(Optional.of(UserTestBuilder.secondUser().build()));

            assertThatThrownBy(() -> userService.changeEmail(request))
                    .isInstanceOf(UserAlreadyExistException.class);
            verifyNoInteractions(emailChangeService);
        }
    }

    @Nested
    @DisplayName("Ban user tests:")
    class BanUserTests {

        @Test
        @DisplayName("When banning user should load user from database")
        public void whenBanningUserShouldLoadUserFromDatabase(){
            when(authUserLockService.lockById(UserConstants.FIRST_USER_ID)).thenReturn(userOptional);

            userService.banUser(UserConstants.FIRST_USER_ID);

            verify(authUserLockService, times(1)).lockById(UserConstants.FIRST_USER_ID);
        }

        @Test
        @DisplayName("When banning user should throw UserNotFoundException if user with given id does not exist")
        public void whenBanningUserShouldThrowUserNotFoundExceptionIfUserWithGivenIdDoesNotExist(){
            when(authUserLockService.lockById(UserConstants.FIRST_USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.banUser(UserConstants.FIRST_USER_ID))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userRepository, never()).save(any(User.class));
            verify(accountSessionInvalidationService, never()).invalidateAll(any());
        }

        @Test
        @DisplayName("When banning user should set user banned field to true")
        public void whenBanningUserShouldSetUserBannedFieldToTrue(){
            when(authUserLockService.lockById(UserConstants.FIRST_USER_ID)).thenReturn(userOptional);

            userService.banUser(UserConstants.FIRST_USER_ID);

            assertThat(user.isBanned()).isTrue();
        }
        @Test
        @DisplayName("When banning user should save banned user in database")
        public void whenBanningUserShouldSaveBannedUserInDatabase(){
            when(authUserLockService.lockById(UserConstants.FIRST_USER_ID)).thenReturn(userOptional);

            userService.banUser(UserConstants.FIRST_USER_ID);

            verify(userRepository, times(1)).save(user);
        }
        @Test
        @DisplayName("When banning user should revoke all user refresh tokens")
        public void whenBanningUserShouldRevokeAllUserRefreshTokens(){
            when(authUserLockService.lockById(UserConstants.FIRST_USER_ID)).thenReturn(userOptional);

            userService.banUser(UserConstants.FIRST_USER_ID);

            verify(accountSessionInvalidationService).invalidateAll(user);
        }
    }

}
