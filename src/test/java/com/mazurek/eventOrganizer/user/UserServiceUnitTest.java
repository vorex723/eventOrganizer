package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.auth.AuthenticationResponse;
import com.mazurek.eventOrganizer.auth.AuthenticationServiceImpl;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.city.CityService;
import com.mazurek.eventOrganizer.exception.auth.UserNotAuthenticatedException;
import com.mazurek.eventOrganizer.exception.user.*;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.jwt.JwtUtils;
import com.mazurek.eventOrganizer.jwt.RefreshToken;
import com.mazurek.eventOrganizer.jwt.RefreshTokenService;
import com.mazurek.eventOrganizer.user.dto.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {


    @Mock private UserRepository userRepository;
    @Mock private AuthenticationServiceImpl authenticationService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private JwtUtils jwtUtils;
    @Mock private CityService cityService;
    private BCryptPasswordEncoder passwordEncoder = Mockito.spy(new BCryptPasswordEncoder());
    private UserService userService;

    private User user;
    private Optional<User> userOptional;
    private final UUID USER_ID = UUID.randomUUID();
    private final String USER_EMAIL = "example@dot.com";
    private final String USER_NEW_EMAIL = "witam@witam.pl";
    private final String USER_PASSWORD = "Password123!";
    private final String USER_NAME = "Andrew";
    private final String USER_LAST_NAME = "Golota";
    private final String USER_TIME_ZONE = "Europe/Warsaw";

    private City cityRzeszow;
    private final String CITY_RZESZOW_NAME = "Rzeszow".toLowerCase(Locale.ROOT);
    private final UUID CITY_ID = UUID.randomUUID();

    private Role ROLE_USER;
    private final Long ROLE_USER_ID = 1L;
    private final String ROLE_USER_NAME = "ROLE_USER";

    private DeviceType deviceType;
    private String deviceInfo;

    private RefreshToken refreshToken;
    private final Long REFRESH_TOKEN_ID = 1L;
    private final Long REFRESH_TOKEN_EXPIRATION = 86400000L;
    private final Long ACCESS_TOKEN_EXPIRATION = 1800000L;

    private final String JWT_ACCESS_TOKEN = "SampleJwtAccessToken";


    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, authenticationService, refreshTokenService, jwtUtils, cityService, passwordEncoder);

        ROLE_USER = new Role(ROLE_USER_ID, ROLE_USER_NAME);

        cityRzeszow = new City(CITY_ID, CITY_RZESZOW_NAME,new HashSet<>(), new HashSet<>());
        Instant userCreateAccountTime = Instant.now();

        user = User.builder()
                .id(USER_ID)
                .email(USER_EMAIL)
                .roles(new HashSet<>(Set.of(ROLE_USER)))
                .firstName(USER_NAME)
                .lastName(USER_LAST_NAME)
                .homeCity(cityRzeszow)
                .password(passwordEncoder.encode(USER_PASSWORD))
                .timeZone(USER_TIME_ZONE)
                .createdAt(userCreateAccountTime)
                .lastCredentialsChangeTime(userCreateAccountTime)
                .build();

        userOptional = Optional.of(user);

        cityRzeszow.addResident(user);

        Instant tokenCreateDate = Instant.now();
        refreshToken = new RefreshToken();
        refreshToken.setId(REFRESH_TOKEN_ID);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setDeviceType(deviceType);
        refreshToken.setCreatedAt(tokenCreateDate);
        refreshToken.setExpiryDate(tokenCreateDate.plusMillis(REFRESH_TOKEN_EXPIRATION));
        refreshToken.setLastUsedAt(tokenCreateDate);
    }

    @AfterEach
    void tearDown(){
    }


    @Test
    @DisplayName("When getting user by id should load user from database")
    public void whenGettingUserByIdShouldLoadUserFromDatabase(){
        when(userRepository.findById(USER_ID)).thenReturn(userOptional);
        userService.getUserById(USER_ID);
        verify(userRepository,times(1)).findById(USER_ID);
    }

    @Test
    @DisplayName("When getting user by id should throw UserNotFoundException if user is not present in database.")
    void whenGettingUserByIdShouldThrowUserNotFoundExceptionIfUserIsNotPresentInDatabase(){
       assertThrows(UserNotFoundException.class, () -> userService.getUserById(UUID.randomUUID()));
    }


    /*
    ********************************************************************************************************************
    *                                       CHANGE USER PASSWORD TESTS
    ********************************************************************************************************************
    */


    @Nested
    @DisplayName("Change user password test")
    class ChangeUserPasswordTests{

        private final String USER_PASSWORD_NEW = "newPassword";

        private ChangeUserPasswordDto changeUserPasswordDto;

        @BeforeEach
        void setUp() {

            deviceType = DeviceType.WEB;
            deviceInfo = "UserAgentSample";

            changeUserPasswordDto = ChangeUserPasswordDto.builder()
                    .newPassword(USER_PASSWORD_NEW)
                    .newPasswordConfirmation(USER_PASSWORD_NEW)
                    .password(USER_PASSWORD)
                    .build();

        }

        private void setupSuccessfulPasswordChangeMocks(){
            when(authenticationService.getCurrentUser()).thenReturn(user);
            when(jwtUtils.generateAccessToken(user)).thenReturn(JWT_ACCESS_TOKEN);
            when(refreshTokenService.createRefreshToken(user, deviceType)).thenReturn(refreshToken);
            when(jwtUtils.getAccessTokenExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);
        }

        @Test
        @DisplayName("When changing password should load user using authentication service")
        void whenChangingPasswordShouldLoadUserUsingAuthenticationService(){
            setupSuccessfulPasswordChangeMocks();

            userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            verify(authenticationService,times(1)).getCurrentUser();
        }

        @Test
        @DisplayName("When changing password should throw exception if user not authenticated")
        void whenChangingPasswordShouldThrowExceptionIfUserNotAuthenticated(){
            when(authenticationService.getCurrentUser()).thenThrow(new UserNotAuthenticatedException());

            assertThrows(UserNotAuthenticatedException.class,
                    () -> userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo));

            verify(userRepository, never()).save(any());
            verify(refreshTokenService, never()).revokeAllUserTokens(any());
        }

        @Test
        @DisplayName("When changing password should throw InvalidPasswordExceptionIfOldPasswordIsWrong")
        void whenChangingPasswordShouldThrowInvalidPasswordExceptionIfOldPasswordIsWrong(){
            when(authenticationService.getCurrentUser()).thenReturn(user);

            changeUserPasswordDto.setPassword("wrongPassword");

            assertThrows(InvalidPasswordException.class, () -> userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo));
            verify(userRepository, never()).save(user);
        }

        @Test
        @DisplayName("When changing password should throw NotMatchingPasswordsException if new password is different than confirmation")
        void whenChangingPasswordShouldThrowNotMatchingPasswordsExceptionIfNewPasswordIsDifferentThanConfirmation(){
            when(authenticationService.getCurrentUser()).thenReturn(user);

            changeUserPasswordDto.setNewPassword("wrongPassword");

           assertThrows(NotMatchingPasswordsException.class, () -> userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo));
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

            assertTrue(passwordEncoder.matches(USER_PASSWORD_NEW, user.getPassword()));

        }

        @Test
        @DisplayName("When changing password should update last credentials change time filed")
        void whenChangingPasswordShouldUpdateLastCredentialsChangeTimeField() {
            setupSuccessfulPasswordChangeMocks();


            Instant lastCredentialChangeTime = user.getLastCredentialsChangeTime();
            userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            assertTrue(lastCredentialChangeTime.isBefore(user.getLastCredentialsChangeTime()));
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

            verify(refreshTokenService, times(1)).revokeAllUserTokens(USER_ID);
        }
        @Test
        @DisplayName("When changing password should revoke tokens after saving user")
        void whenChangingPasswordShouldRevokeTokensAfterSavingUser(){
            setupSuccessfulPasswordChangeMocks();

            InOrder inOrder = inOrder(userRepository, refreshTokenService);

            userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            inOrder.verify(userRepository).save(user);
            inOrder.verify(refreshTokenService).revokeAllUserTokens(USER_ID);
        }

        @Test
        @DisplayName("When changing password should create new tokens after revoking old ones")
        void whenChangingPasswordShouldCreateNewTokensAfterRevokingOldOnes(){
            setupSuccessfulPasswordChangeMocks();

            InOrder inOrder = inOrder(refreshTokenService, jwtUtils);

            userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            inOrder.verify(refreshTokenService).revokeAllUserTokens(USER_ID);
            inOrder.verify(jwtUtils).generateAccessToken(user);
            inOrder.verify(refreshTokenService).createRefreshToken(user, deviceType);
        }
        @Test
        @DisplayName("When changing password should return correct tokens on success")
        void whenChangingPasswordShouldReturnCorrectTokensOnSuccess(){
            setupSuccessfulPasswordChangeMocks();

            AuthenticationResponse response = userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            assertAll("Returned AuthenticationResponse assertions",
                    () -> assertNotNull(response),
                    () -> assertEquals(JWT_ACCESS_TOKEN, response.getAccessToken()),
                    () -> assertEquals(refreshToken.getToken(), response.getRefreshToken()),
                    () -> assertEquals(ACCESS_TOKEN_EXPIRATION, response.getAccessTokenExpiration())
                    );
        }

    }



    /*
    ********************************************************************************************************************
    *                                      CHANGE USER EMAIL TESTS
    ********************************************************************************************************************
    */

    @Nested
    @DisplayName("Change user email tests")
    class ChangeUserEmailTest{
        private ChangeUserEmailDto changeEmailDto;

        @BeforeEach
        void setUp() {
            deviceType = DeviceType.WEB;
            deviceInfo = "UserAgentSample";
            
            changeEmailDto = ChangeUserEmailDto.builder()
                    .password(USER_PASSWORD)
                    .newEmail(USER_NEW_EMAIL)
                    .newEmailConfirmation(USER_NEW_EMAIL)
                    .build();

        }
        private void setupSuccessfulEmailChangeMocks(){
            when(authenticationService.getCurrentUser()).thenReturn(user);
            when(userRepository.findByIgnoreCaseEmail(USER_NEW_EMAIL)).thenReturn(Optional.empty());
            when(jwtUtils.generateAccessToken(user)).thenReturn(JWT_ACCESS_TOKEN);
            when(refreshTokenService.createRefreshToken(user, deviceType)).thenReturn(refreshToken);
            when(jwtUtils.getAccessTokenExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);
        }

        @Test
        @DisplayName("When changing user email should load user using authentication service")
        void whenChangingUserEmailShouldLoadUserUsingAuthenticationService(){
            setupSuccessfulEmailChangeMocks();

            userService.changeEmail(changeEmailDto, deviceType, deviceInfo);

            verify(authenticationService,times(1)).getCurrentUser();
        }

        @Test
        @DisplayName("When changing user email should throw exception if user not authenticated")
        void whenChangingUserEmailShouldThrowExceptionIfUserNotAuthenticated(){
            when(authenticationService.getCurrentUser()).thenThrow(new UserNotAuthenticatedException());

            assertThrows(UserNotAuthenticatedException.class,
                    () -> userService.changeEmail(changeEmailDto, deviceType, deviceInfo));

            verify(userRepository, never()).save(any());
            verify(refreshTokenService, never()).revokeAllUserTokens(any());
        }

        @Test
        @DisplayName("When changing user email should throw InvalidPasswordException if provided password is wrong")
        public void whenChangingUserEmailShouldThrowInvalidPasswordExceptionIfProvidedPasswordIsWrong(){
            when(authenticationService.getCurrentUser()).thenReturn(user);

            changeEmailDto.setPassword("wrongPassword");

            assertThrows(InvalidPasswordException.class, () -> userService.changeEmail(changeEmailDto, deviceType, deviceInfo));
            verify(userRepository, never()).save(any(User.class));
            verify(refreshTokenService, never()).revokeAllUserTokens(any(UUID.class));
            verify(jwtUtils, never()).generateAccessToken(any(User.class));
            verify(refreshTokenService, never()).createRefreshToken(any(User.class), any(DeviceType.class));
        }

        @Test
        @DisplayName("When changing user email should throw SameEmailException")
        void whenChangingUserEmailShouldThrowSameEmailExceptionIfNewEmailIsExactlyTheSameAsOldEmail(){
            when(authenticationService.getCurrentUser()).thenReturn(user);

            changeEmailDto.setNewEmail(USER_EMAIL);
            changeEmailDto.setNewEmailConfirmation(USER_EMAIL);

            assertThrows(SameEmailException.class, () -> userService.changeEmail(changeEmailDto, deviceType, deviceInfo));
            verify(userRepository, never()).save(any(User.class));
            verify(refreshTokenService, never()).revokeAllUserTokens(any(UUID.class));
            verify(jwtUtils, never()).generateAccessToken(any(User.class));
            verify(refreshTokenService, never()).createRefreshToken(any(User.class), any(DeviceType.class));
        }

        @Test
        @DisplayName("When changing user email should throw NotMatchingEmailsException if new email and confirmation are different")
        void whenChangingUserEmailShouldThrowNotMatchingEmailsExceptionIfNewEmailAndConfirmationAreDifferent(){
            when(authenticationService.getCurrentUser()).thenReturn(user);

            changeEmailDto.setNewEmailConfirmation("wrongEmail@example.com");

            assertThrows(NotMatchingEmailsException.class, () -> userService.changeEmail(changeEmailDto, deviceType, deviceInfo));
            verify(userRepository, never()).save(any(User.class));
            verify(refreshTokenService, never()).revokeAllUserTokens(any(UUID.class));
            verify(jwtUtils, never()).generateAccessToken(any(User.class));
            verify(refreshTokenService, never()).createRefreshToken(any(User.class), any(DeviceType.class));
        }


        @Test
        @DisplayName("When changing user email should throw UserAlreadyExistException if email is already in database")
        void whenChangingUserEmailShouldThrowUserAlreadyExistExceptionIfEmailIsAlreadyInDatabase(){
            when(authenticationService.getCurrentUser()).thenReturn(user);
            when(userRepository.findByIgnoreCaseEmail(USER_NEW_EMAIL)).thenReturn(userOptional);

            assertThrows(UserAlreadyExistException.class, () -> userService.changeEmail(changeEmailDto, deviceType, deviceInfo));
            verify(userRepository, never()).save(any(User.class));
            verify(refreshTokenService, never()).revokeAllUserTokens(any(UUID.class));
            verify(jwtUtils, never()).generateAccessToken(any(User.class));
            verify(refreshTokenService, never()).createRefreshToken(any(User.class), any(DeviceType.class));
        }

        @Test
        @DisplayName("When changing user email should throw SameEmailException regardless of case")
        void whenChangingUserEmailShouldThrowUserAlreadyExistExceptionRegardlessOfCase(){
            when(authenticationService.getCurrentUser()).thenReturn(user);

            String userEmailInUpperCase = USER_EMAIL.toUpperCase();

            changeEmailDto.setNewEmail(userEmailInUpperCase);
            changeEmailDto.setNewEmailConfirmation(userEmailInUpperCase);


            assertThrows(SameEmailException.class,
                    () -> userService.changeEmail(changeEmailDto, deviceType, deviceInfo));
        }

        @Test
        @DisplayName("When changing user email should accept confirmation with different case")
        void whenChangingUserEmailShouldAcceptConfirmationWithDifferentCase(){
            setupSuccessfulEmailChangeMocks();

            changeEmailDto.setNewEmailConfirmation(USER_NEW_EMAIL.toUpperCase());

            assertDoesNotThrow(() -> userService.changeEmail(changeEmailDto, deviceType, deviceInfo));
            assertEquals(USER_NEW_EMAIL, user.getEmail());
        }

        @Test
        @DisplayName("When changing user email should update user email")
        void whenChangingUserEmailShouldUpdateUserEmail(){
           setupSuccessfulEmailChangeMocks();

            userService.changeEmail(changeEmailDto, deviceType, deviceInfo);

            assertEquals(USER_NEW_EMAIL, user.getEmail());
        }
        @Test
        @DisplayName("When changing user email should convert email to lowercase before saving")
        void whenChangingUserEmailShouldConvertEmailToLowercaseBeforeSaving(){
            when(authenticationService.getCurrentUser()).thenReturn(user);
            when(jwtUtils.generateAccessToken(user)).thenReturn(JWT_ACCESS_TOKEN);
            when(refreshTokenService.createRefreshToken(user, deviceType)).thenReturn(refreshToken);
            when(jwtUtils.getAccessTokenExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);

            String mixedCaseEmail = USER_NEW_EMAIL.substring(0,5).toUpperCase() + USER_NEW_EMAIL.substring(5);
            changeEmailDto.setNewEmail(mixedCaseEmail);
            changeEmailDto.setNewEmailConfirmation(mixedCaseEmail);

            when(userRepository.findByIgnoreCaseEmail(mixedCaseEmail)).thenReturn(Optional.empty());

            userService.changeEmail(changeEmailDto, deviceType, deviceInfo);

            assertEquals(USER_NEW_EMAIL, user.getEmail(),
                    "Expected email to be converted to lowercase");
        }

        @Test
        @DisplayName("When changing user email should update last credentials change time field")
        void whenChangingUserEmailShouldUpdateLastCredentialsChangeTimeField(){
            setupSuccessfulEmailChangeMocks();

            Instant lastCredentialsUpdate = user.getLastCredentialsChangeTime();
            userService.changeEmail(changeEmailDto, deviceType, deviceInfo);

            assertTrue(lastCredentialsUpdate.isBefore(user.getLastCredentialsChangeTime()));
        }

        @Test
        @DisplayName("When changing user email should save updated user in database")
        void whenChangingUserEmailShouldSaveUpdatedUserInDatabase(){
           setupSuccessfulEmailChangeMocks();

            userService.changeEmail(changeEmailDto, deviceType, deviceInfo);

            ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository,times(1)).save(userArgumentCaptor.capture());

            User capturedUser = userArgumentCaptor.getValue();

            assertAll("Captured user to save assertions:",
                    () -> assertEquals(USER_NEW_EMAIL, capturedUser.getEmail()),
                    () -> assertTrue(capturedUser.getLastCredentialsChangeTime().isAfter(user.getCreatedAt()))
                    );
        }

        @Test
        @DisplayName("When changing user email should revoke all user refresh tokens")
        void whenChangingUserEmailShouldRevokeAllUserRefreshTokens(){
            setupSuccessfulEmailChangeMocks();

            userService.changeEmail(changeEmailDto, deviceType, deviceInfo);

            verify(refreshTokenService,times(1)).revokeAllUserTokens(USER_ID);
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
            inOrder.verify(refreshTokenService).revokeAllUserTokens(USER_ID);
        }

        @Test
        @DisplayName("When changing email should create new tokens after revoking old ones")
        void whenChangingUserEmailShouldCreateNewTokensAfterRevokingOldOnes(){
            setupSuccessfulEmailChangeMocks();

            InOrder inOrder = inOrder(refreshTokenService, jwtUtils);

            userService.changeEmail(changeEmailDto, deviceType, deviceInfo);

            inOrder.verify(refreshTokenService).revokeAllUserTokens(USER_ID);
            inOrder.verify(jwtUtils).generateAccessToken(user);
            inOrder.verify(refreshTokenService).createRefreshToken(user, deviceType);
        }

        @Test
        @DisplayName("When changing user emails should return correct tokens")
        void whenChangingUserEmailShouldReturnCorrectTokens(){
            setupSuccessfulEmailChangeMocks();

            AuthenticationResponse response = userService.changeEmail(changeEmailDto, deviceType, deviceInfo);

            assertAll("Authentication response tokens assertions",
                    () -> assertEquals(JWT_ACCESS_TOKEN, response.getAccessToken()),
                    () -> assertEquals(refreshToken.getToken(), response.getRefreshToken()),
                    () -> assertEquals(ACCESS_TOKEN_EXPIRATION, response.getAccessTokenExpiration())
                    );
        }

    }

/*
     ********************************************************************************************************************
     *                                       CHANGE USER DETAILS TESTS
     ********************************************************************************************************************
     */

    @Nested
    @DisplayName("Change user details tests")
    class ChangeUserDetails{

        private ChangeUserDetailsDto changeUserDetailsDto;
        private final String USER_FIRST_NAME_NEW = "andrzej";
        private final String USER_LAST_NAME_NEW = "Konieczny";
        private final String USER_HOME_CITY_NEW_KRAKOW = "krakow";

        private City cityKrakow;

        @BeforeEach
        void setUp() {
            changeUserDetailsDto = ChangeUserDetailsDto.builder()
                    .firstName(USER_FIRST_NAME_NEW)
                    .lastName(USER_LAST_NAME_NEW)
                    .homeCity(USER_HOME_CITY_NEW_KRAKOW)
                    .build();
            cityKrakow = City.builder().id(UUID.randomUUID()).name(USER_HOME_CITY_NEW_KRAKOW).events(new HashSet<>()).residents(new HashSet<>()).build();
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
        @DisplayName("When updating user details should throw exception if user not authenticated")
        void whenUpdatingUserDetailsShouldThrowExceptionIfUserNotAuthenticated(){
            when(authenticationService.getCurrentUser()).thenThrow(new UserNotAuthenticatedException());

            assertThrows(UserNotAuthenticatedException.class,
                    () -> userService.changeDetails(changeUserDetailsDto));

            verify(cityService, never()).getCityByNameOrCreate(any());
        }

        @Test
        @DisplayName("When updating user datils should update user first name and last name")
        void whenUpdatingUserDetailsShouldUpdateUserFirstNameAndLastName() {
            changeUserDetailsDto.setHomeCity(CITY_RZESZOW_NAME);
            when(authenticationService.getCurrentUser()).thenReturn(user);
            when(userRepository.save(user)).thenReturn(user);

            UserProfileDto result = userService.changeDetails(changeUserDetailsDto);

            assertEquals(USER_FIRST_NAME_NEW, user.getFirstName());
            assertEquals(USER_LAST_NAME_NEW, user.getLastName());
            assertEquals(cityRzeszow, user.getHomeCity());

            assertEquals(USER_FIRST_NAME_NEW, result.getFirstName());
            assertEquals(USER_LAST_NAME_NEW, result.getLastName());
            assertEquals(CITY_RZESZOW_NAME, result.getHomeCity());

            verify(cityService, never()).getCityByNameOrCreate(any());
        }

        @Test
        @DisplayName("When updating user datils should update user home city")
        void whenUpdatingUserDetailsShouldUpdateUserHomeCity() {
            setupSuccessfulUserDetailsChangeMocks();

            UserProfileDto result = userService.changeDetails(changeUserDetailsDto);

            assertEquals(USER_FIRST_NAME_NEW, user.getFirstName());
            assertEquals(USER_LAST_NAME_NEW, user.getLastName());
            assertEquals(cityKrakow, user.getHomeCity());

            assertEquals(USER_FIRST_NAME_NEW, result.getFirstName());
            assertEquals(USER_LAST_NAME_NEW, result.getLastName());
            assertEquals(USER_HOME_CITY_NEW_KRAKOW, result.getHomeCity());

            verify(cityService, times(1)).getCityByNameOrCreate(any());
        }
        @Test
        @DisplayName("When updating user details should not change city if name differs only in case")
        void whenUpdatingUserDetailsShouldNotChangeCityIfNameDiffersOnlyInCase(){
            when(authenticationService.getCurrentUser()).thenReturn(user);
            when(userRepository.save(user)).thenReturn(user);

            changeUserDetailsDto.setHomeCity(CITY_RZESZOW_NAME.toUpperCase());

            UserProfileDto result = userService.changeDetails(changeUserDetailsDto);

            assertEquals(cityRzeszow, user.getHomeCity(), "Expected city to remain the same");
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

            assertEquals(USER_HOME_CITY_NEW_KRAKOW, cityNameCaptor.getValue(),
                    "Expected to pass correct city name to city service");
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
    @DisplayName("Ban user tests:")
    class BanUserTests{

        @Test
        @DisplayName("When banning user should")
        public void whenBanningUserShouldLoadUserFromDatabase(){
            when(userRepository.findById(USER_ID)).thenReturn(userOptional);

            userService.banUser(USER_ID);

            verify(userRepository, times(1)).findById(USER_ID);
        }

        @Test
        @DisplayName("When banning user should")
        public void whenBanningUserShouldThrowUserNotFoundExceptionIfUserWithGivenIdDoesNotExist(){
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class ,() -> userService.banUser(USER_ID));

            verify(userRepository, never()).save(any(User.class));
            verify(refreshTokenService, never()).revokeAllUserTokens(any(UUID.class));
        }

        @Test
        @DisplayName("When banning user should set user banned field to true")
        public void whenBanningUserShouldSetUserBannedFieldToTrue(){
            when(userRepository.findById(USER_ID)).thenReturn(userOptional);

            userService.banUser(USER_ID);

            assertTrue(user.isBanned());
        }
        @Test
        @DisplayName("When banning user should save banned user in database")
        public void whenBanningUserShouldSaveBannedUserInDatabase(){
            when(userRepository.findById(USER_ID)).thenReturn(userOptional);

            userService.banUser(USER_ID);

            verify(userRepository, times(1)).save(user);
        }
        @Test
        @DisplayName("When banning user should revoke all user refresh tokens")
        public void whenBanningUserShouldRevokeAllUserRefreshTokens(){
            when(userRepository.findById(USER_ID)).thenReturn(userOptional);

            userService.banUser(USER_ID);

            verify(refreshTokenService, times(1)).revokeAllUserTokens(USER_ID);
        }
    }

}