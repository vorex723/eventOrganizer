package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityService;
import com.mazurek.eventOrganizer.exception.auth.AccountAlreadyActivatedException;
import com.mazurek.eventOrganizer.exception.auth.ActivationTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.auth.UserNotAuthenticatedException;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenExpiredException;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenRevokedException;
import com.mazurek.eventOrganizer.exception.user.*;
import com.mazurek.eventOrganizer.jwt.*;
import com.mazurek.eventOrganizer.notification.EmailServiceProdImpl;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.RoleRepository;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceUnitTest {
    private City cityRzeszow;
    private final UUID CITY_ID = UUID.randomUUID();
    private final String CITY_RZESZOW_NAME = "Rzeszow";


    private User user;
    private Optional<User> userOptional;
    private final UUID USER_ID = UUID.randomUUID();
    private final String USER_EMAIL = "example@dot.com";
    private final String USER_FIRST_NAME = "Andrew";
    private final String USER_LAST_NAME = "Golota";
    private final String USER_PASSWORD = "Password123!";
    private final String USER_PASSWORD_WRONG = "WrongPassword123!";
    private final String USER_TIME_ZONE = "Europe/Warsaw";


    private ActivationToken activationToken;
    private Optional<ActivationToken> activationTokenOptional;
    private final Long ACTIVATION_TOKEN_ID = 1L;
    private final UUID ACTIVATION_TOKEN_UUID_TOKEN = UUID.randomUUID();
    private final long EXPIRATION_TIME = 345600;


    private Role roleUser;
    private Optional<Role> roleUserOptional;
    private final Long ROLE_USER_ID = 1L;
    private final String ROLE_USER_NAME = "ROLE_USER";


    private AuthenticationService authenticationService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private ActivationTokenRepository activationTokenRepository;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private AuthenticationManager authenticationManager;
    private BCryptPasswordEncoder passwordEncoder = Mockito.spy(new BCryptPasswordEncoder());
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private CityService cityService;
    @Mock
    private EmailServiceProdImpl emailService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationServiceImpl(userRepository, roleRepository, activationTokenRepository, refreshTokenService, emailService, authenticationManager, passwordEncoder, jwtUtils, cityService);

        roleUser = new Role(ROLE_USER_ID, ROLE_USER_NAME);
        roleUserOptional = Optional.of(roleUser);

        cityRzeszow = new City(CITY_ID, CITY_RZESZOW_NAME, new ArrayList<>(), new HashSet<>());

        Instant userCreateAccountTime = Instant.now();

        user = User.builder()
                .id(USER_ID)
                .email(USER_EMAIL)
                .roles(Set.of(roleUser))
                .firstName(USER_FIRST_NAME)
                .lastName(USER_LAST_NAME)
                .activated(false)
                .banned(false)
                .homeCity(cityRzeszow)
                .password(passwordEncoder.encode(USER_PASSWORD))
                .timeZone(USER_TIME_ZONE)
                .createdAt(userCreateAccountTime)
                .lastCredentialsChangeTime(userCreateAccountTime)
                .build();

        userOptional = Optional.of(user);

        cityRzeszow.addResident(user);
        Instant tokenExpirationDate = Instant.now().plusMillis(EXPIRATION_TIME);
        activationToken = new ActivationToken();
        activationToken.setId(ACTIVATION_TOKEN_ID);
        activationToken.setToken(ACTIVATION_TOKEN_UUID_TOKEN);
        activationToken.setUser(user);
        activationToken.setExpirationDate(tokenExpirationDate);

        activationTokenOptional = Optional.of(activationToken);
    }


    @Nested
    @DisplayName("Register new user tests:")
    class RegisterNewUserTests {
        private RegisterRequest registerRequest;

        @BeforeEach
        void setUp() {

            registerRequest = RegisterRequest.builder()
                    .email(USER_EMAIL)
                    .emailConfirmation(USER_EMAIL)
                    .firstName(USER_FIRST_NAME)
                    .lastName(USER_LAST_NAME)
                    .homeCity(CITY_RZESZOW_NAME)
                    .timeZone(USER_TIME_ZONE)
                    .password(USER_PASSWORD)
                    .passwordConfirmation(USER_PASSWORD)
                    .build();
        }

        private void setupSuccessfulRegistrationMocks() {
            when(userRepository.findByIgnoreCaseEmail(USER_EMAIL)).thenReturn(Optional.empty());
            when(cityService.getCityByNameOrCreate(CITY_RZESZOW_NAME)).thenReturn(cityRzeszow);
            when(roleRepository.findByName(ROLE_USER_NAME)).thenReturn(roleUserOptional);
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(activationTokenRepository.save(any(ActivationToken.class))).thenReturn(activationToken);
        }

        @Test
        @DisplayName("When registering should throw UserAlreadyExistException if email is in database")
        public void whenRegisteringShouldThrowUserAlreadyExistExceptionIfEmailIsInDatabase() {
            when(userRepository.findByIgnoreCaseEmail(USER_EMAIL)).thenReturn(userOptional);

            assertThrows(UserAlreadyExistException.class, () -> authenticationService.register(registerRequest));
            verify(userRepository, never()).save(any(User.class));
            verify(activationTokenRepository, never()).save(any(ActivationToken.class));
            verify(emailService, never()).sendActivationEmail(anyString(), any(UUID.class));
        }

        @Test
        @DisplayName("When registering should throw NotMatchingPasswordsException if password and confirmations are different")
        public void whenRegisteringShouldThrowNotMatchingPasswordsExceptionIfPasswordAndConfirmationAreDifferent() {
            when(userRepository.findByIgnoreCaseEmail(USER_EMAIL)).thenReturn(Optional.empty());

            registerRequest.setPasswordConfirmation("incorrectPassword");

            assertThrows(NotMatchingPasswordsException.class, () -> authenticationService.register(registerRequest));
            verify(userRepository, never()).save(any(User.class));
            verify(activationTokenRepository, never()).save(any(ActivationToken.class));
            verify(emailService, never()).sendActivationEmail(anyString(), any(UUID.class));
        }

        @Test
        @DisplayName("When registering should throw NotMatchingEmailsException if email and email confirmation are different")
        public void whenRegisteringShouldThrowNotMatchingEmailsExceptionIfEmailAndEmailConfirmationAreDifferent() {
            when(userRepository.findByIgnoreCaseEmail(USER_EMAIL)).thenReturn(Optional.empty());

            registerRequest.setEmailConfirmation("wrongEmail@example.com");

            assertThrows(NotMatchingEmailsException.class, () -> authenticationService.register(registerRequest));
            verify(userRepository, never()).save(any(User.class));
            verify(activationTokenRepository, never()).save(any(ActivationToken.class));
            verify(emailService, never()).sendActivationEmail(anyString(), any(UUID.class));
        }

        @Test
        @DisplayName("When registering should save user object with data from register request")
        public void whenRegisteringShouldSaveUserObjectWithDataFromRegisterRequest() {
            setupSuccessfulRegistrationMocks();

            ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);

            authenticationService.register(registerRequest);

            verify(userRepository, times(1)).save(userArgumentCaptor.capture());

            User capturedUser = userArgumentCaptor.getValue();

            assertAll("User provided data assertions: ",
                    () -> assertTrue(registerRequest.getEmail().equalsIgnoreCase(capturedUser.getEmail()), "Expected to contain the same email as user provided."),
                    () -> assertEquals(registerRequest.getFirstName(), capturedUser.getFirstName(), "Expected to contain the same first name as user provided."),
                    () -> assertEquals(registerRequest.getLastName(), capturedUser.getLastName(), "Expected to contain the same last name as user provided."),
                    () -> assertEquals(registerRequest.getHomeCity(), capturedUser.getHomeCity().getName(), "Expected to contain the same city as user provided."),
                    () -> assertEquals(registerRequest.getTimeZone(), capturedUser.getTimeZone(), "Expected to contain the same time zone as user provided."),
                    () -> assertTrue(passwordEncoder.matches(registerRequest.getPassword(), capturedUser.getPassword()), "Expected to contain password hash to which user password is correct.")
            );
        }

        @Test
        @DisplayName("When registering should convert email to lowercase")
        public void whenRegisteringShouldConvertEmailToLowercase() {
            String USER_EMAIL_CAPITAL = "TEST@EXAMPLE.COM";
            registerRequest.setEmail(USER_EMAIL_CAPITAL);
            registerRequest.setEmailConfirmation(USER_EMAIL_CAPITAL);

            when(userRepository.findByIgnoreCaseEmail(USER_EMAIL_CAPITAL)).thenReturn(Optional.empty());
            when(cityService.getCityByNameOrCreate(CITY_RZESZOW_NAME)).thenReturn(cityRzeszow);
            when(roleRepository.findByName(ROLE_USER_NAME)).thenReturn(roleUserOptional);
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(activationTokenRepository.save(any(ActivationToken.class))).thenReturn(activationToken);

            ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);

            authenticationService.register(registerRequest);

            verify(userRepository).save(userArgumentCaptor.capture());
            assertEquals("test@example.com", userArgumentCaptor.getValue().getEmail());
        }

        @Test
        @DisplayName("When registering should throw UserRoleNotFoundException if ROLE_USER does not exist")
        public void whenRegisteringShouldThrowUserRoleNotFoundExceptionIfRoleUserDoesNotExist() {
            when(userRepository.findByIgnoreCaseEmail(USER_EMAIL)).thenReturn(Optional.empty());
            when(roleRepository.findByName(ROLE_USER_NAME)).thenReturn(Optional.empty());

            assertThrows(UserRoleNotFoundException.class,
                    () -> authenticationService.register(registerRequest));

            verify(userRepository, never()).save(any(User.class));
            verify(activationTokenRepository, never()).save(any(ActivationToken.class));
            verify(emailService, never()).sendActivationEmail(anyString(), any(UUID.class));
        }

        @Test
        @DisplayName("When registering should set \"ROLE_USER\" to new user account")
        public void whenRegisteringShouldSetUserRoleToNewUserAccount() {
            setupSuccessfulRegistrationMocks();

            ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);

            authenticationService.register(registerRequest);

            verify(userRepository, times(1)).save(userArgumentCaptor.capture());

            User capturedUser = userArgumentCaptor.getValue();

            assertAll("User role assertions: ",
                    () -> assertTrue(capturedUser.getRoles().contains(roleUser), "Expected to contain \"ROLE_USER\"."),
                    () -> assertEquals(1, capturedUser.getRoles().size(), "Expected to contain exactly one user role.")
            );
        }

        @Test
        @DisplayName("When registering should set correct time stamps")
        public void whenRegisteringShouldSetCorrectTimeStamps() {
            setupSuccessfulRegistrationMocks();

            ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);

            authenticationService.register(registerRequest);

            verify(userRepository, times(1)).save(userArgumentCaptor.capture());

            User capturedUser = userArgumentCaptor.getValue();

            Instant userCreateDate = capturedUser.getCreatedAt();
            Instant userLastCredentialChange = capturedUser.getLastCredentialsChangeTime();
            assertEquals(userCreateDate, userLastCredentialChange, "Expected to user createdAt and lastCredentialsChangeTime to be exactly the same.");

        }

        @Test
        @DisplayName("When registering should generate activation token with correct expiration date")
        public void whenRegisteringShouldGenerateActivationTokenWithCorrectExpirationDate() {
            setupSuccessfulRegistrationMocks();

            ArgumentCaptor<ActivationToken> activationTokenArgumentCaptor =
                    ArgumentCaptor.forClass(ActivationToken.class);

            authenticationService.register(registerRequest);

            verify(activationTokenRepository, times(1)).save(activationTokenArgumentCaptor.capture());

            ActivationToken capturedToken = activationTokenArgumentCaptor.getValue();

            assertAll("Activation token assertions:",
                    () -> assertEquals(user, capturedToken.getUser(),
                            "Expected token to be associated with the user"),
                    () -> assertNotNull(capturedToken.getExpirationDate(),
                            "Expected expiration date to be set"),
                    () -> assertTrue(capturedToken.getExpirationDate().isAfter(Instant.now()),
                            "Expected expiration date to be in the future")
            );
        }


        @Test
        @DisplayName("When registering should send email to user with account activation link")
        public void whenRegisteringShouldSendEmailToUserWithAccountActivationLink() {
            setupSuccessfulRegistrationMocks();

            authenticationService.register(registerRequest);

            verify(emailService, times(1)).sendActivationEmail(USER_EMAIL, ACTIVATION_TOKEN_UUID_TOKEN);
        }

    }

    @Nested
    @DisplayName("Activate account tests:")
    class ActivateAccountTests {

        @Test
        @DisplayName("When activating account should throw ActivationTokenNotFoundException if token with given id does not exist")
        public void whenActivatingAccountShouldThrowActivationTokenNotFoundExceptionIfTokenWithGivenIdDoesNotExist() {
            when(activationTokenRepository.findByToken(ACTIVATION_TOKEN_UUID_TOKEN)).thenReturn(Optional.empty());

            assertThrows(ActivationTokenNotFoundException.class,
                    () -> authenticationService.activateAccount(ACTIVATION_TOKEN_UUID_TOKEN), "Expected to throw ActivationTokenNotFoundException if token with given id is not present in database.");
            verify(userRepository, never()).save(any(User.class));
            verify(activationTokenRepository, never()).delete(any(ActivationToken.class));
        }

        @Test
        @DisplayName("When activating account should regenerate activation token and send new activation email if old token expired")
        public void whenActivatingAccountShouldRegenerateActivationTokenAndSendNewActivationEmailIfOldTokenExpired() {
            activationToken.setExpirationDate(Instant.now().minusSeconds(100));
            when(activationTokenRepository.findByToken(ACTIVATION_TOKEN_UUID_TOKEN)).thenReturn(activationTokenOptional);

            when(activationTokenRepository.save(any(ActivationToken.class))).thenReturn(activationToken);

            authenticationService.activateAccount(ACTIVATION_TOKEN_UUID_TOKEN);

            verify(activationTokenRepository, times(1).description("Expected to save regenerated token in database.")).save(any(ActivationToken.class));
            verify(emailService, times(1).description("Expected to send new activation email.")).sendActivationEmail(USER_EMAIL, activationToken.getToken());
            verify(userRepository, never().description("Expected to not save any user.")).save(any(User.class));
        }
        @Test
        @DisplayName("When activating account should return ActivationResult TOKEN_EXPIRED_NEW_SENT if token is expired")
        public void whenActivatingAccountShouldThrowActivationTokenExpiredExceptionIfTokenIsExpired() {
            activationToken.setExpirationDate(Instant.now().minusSeconds(100));
            when(activationTokenRepository.findByToken(ACTIVATION_TOKEN_UUID_TOKEN)).thenReturn(activationTokenOptional);

            final Long newActivationTokenId = 2L;
            final UUID newUuidToken = UUID.randomUUID();
            ActivationToken newActivationToken = new ActivationToken(newActivationTokenId, newUuidToken, user, Instant.now().plusSeconds(EXPIRATION_TIME));
            when(activationTokenRepository.save(any(ActivationToken.class))).thenReturn(newActivationToken);

            ActivationResult activationResult = authenticationService.activateAccount(ACTIVATION_TOKEN_UUID_TOKEN);
            assertFalse(user.isActivated(), "Expected to not set user account as activated.");
            verify(userRepository, never().description("Expected to not save any user.")).save(any(User.class));
            assertEquals(ActivationResult.TOKEN_EXPIRED_NEW_SENT, activationResult);
        }

        @Test
        @DisplayName("When activating account should set field activated in user to true")
        public void whenActivatingAccountShouldSetFieldActivatedInUserToTrue() {
            when(activationTokenRepository.findByToken(ACTIVATION_TOKEN_UUID_TOKEN)).thenReturn(activationTokenOptional);

            authenticationService.activateAccount(ACTIVATION_TOKEN_UUID_TOKEN);
            assertTrue(user.isActivated(), "User account is expected to be activated now");
            verify(userRepository,times(1)).save(user);

        }

        @Test
        @DisplayName("When activating account should save activated user in database")
        public void whenActivatingAccountShouldSaveActivatedUserInDatabase() {
            when(activationTokenRepository.findByToken(ACTIVATION_TOKEN_UUID_TOKEN)).thenReturn(activationTokenOptional);

            authenticationService.activateAccount(ACTIVATION_TOKEN_UUID_TOKEN);

            ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository, times(1)).save(userArgumentCaptor.capture());

            User capturedUser = userArgumentCaptor.getValue();

            assertAll("User save assertions:",
                    () -> assertSame(user, capturedUser,
                            "Expected to save the same user instance retrieved from token"),
                    () -> assertTrue(capturedUser.isActivated(),
                            "aExpected user to be activated before saving")
            );
        }

        @Test
        @DisplayName("When activating account should delete activation token after successful activation")
        public void whenActivatingAccountShouldDeleteActivationTokenAfterSuccessfulActivation() {
            when(activationTokenRepository.findByToken(ACTIVATION_TOKEN_UUID_TOKEN)).thenReturn(activationTokenOptional);

            authenticationService.activateAccount(ACTIVATION_TOKEN_UUID_TOKEN);

            verify(activationTokenRepository, times(1).description("Expected to delete used activation token after successful activation")).delete(activationToken);
        }

        @Test
        @DisplayName("When activating account should return ActivationResult ACTIVATED on successful activation")
        public void whenActivatingAccountShouldReturnActivationResultActivatedOnSuccessfulActivation(){
            when(activationTokenRepository.findByToken(ACTIVATION_TOKEN_UUID_TOKEN)).thenReturn(activationTokenOptional);

            ActivationResult activationResult = authenticationService.activateAccount(ACTIVATION_TOKEN_UUID_TOKEN);

            assertEquals(ActivationResult.ACTIVATED, activationResult);
        }

    }

    @Nested
    @DisplayName("Regenerate activation token by email tests:")
    class RegenerateActivationTokenByEmailTests {

        private final Long NEW_ACTIVATION_TOKEN_ID = 2L;
        private final UUID NEW_ACTIVATION_TOKEN_UUID_TOKEN = UUID.randomUUID();
        private ActivationToken newActivationToken;

        @BeforeEach
        void setUp() {
            newActivationToken = new ActivationToken(NEW_ACTIVATION_TOKEN_ID, NEW_ACTIVATION_TOKEN_UUID_TOKEN, user, Instant.now().plusMillis(EXPIRATION_TIME));
        }

        private void setupSuccessfulRegenerationMocks() {
            when(userRepository.findByIgnoreCaseEmail(USER_EMAIL)).thenReturn(userOptional);
            when(activationTokenRepository.findByIgnoreCaseUserEmail(USER_EMAIL)).thenReturn(activationTokenOptional);
            when(activationTokenRepository.save(any(ActivationToken.class))).thenReturn(activationToken);
        }

        @Test
        @DisplayName("When regenerating activation token should throw UserNotFoundException if user with given email does not exist.")
        public void whenRegeneratingActivationTokenShouldThrowUserNotFoundExceptionIfUserWithGivenEmailDoesNotExist() {
            when(userRepository.findByIgnoreCaseEmail(USER_EMAIL)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> authenticationService.regenerateActivationTokenByUserEmail(USER_EMAIL));

        }

        @Test
        @DisplayName("When regenerating activation token should load user by email from database")
        public void whenRegeneratingActivationTokenShouldLoadUserByEmailFromDatabase() {
            setupSuccessfulRegenerationMocks();

            authenticationService.regenerateActivationTokenByUserEmail(USER_EMAIL);

            verify(userRepository, times(1).description("Expected to load user by provided email")).findByIgnoreCaseEmail(USER_EMAIL);
        }

        @Test
        @DisplayName("When regenerating activation token should throw AccountAlreadyActivatedException if user have already activated his account")
        public void whenGRegeneratingActivationTokenShouldThrowAccountAlreadyActivatedExceptionIfUserHaveAlreadyActivatedHisAccount() {
            user.setActivated(true);
            when(userRepository.findByIgnoreCaseEmail(USER_EMAIL)).thenReturn(userOptional);

            assertThrows(AccountAlreadyActivatedException.class, () -> authenticationService.regenerateActivationTokenByUserEmail(USER_EMAIL));
        }


        @Test
        @DisplayName("When regenerating activation token should work correctly when no old token exists")
        public void whenRegeneratingActivationTokenShouldWorkCorrectlyWhenNoOldTokenExists() {
            when(userRepository.findByIgnoreCaseEmail(USER_EMAIL)).thenReturn(userOptional);
            when(activationTokenRepository.findByIgnoreCaseUserEmail(USER_EMAIL)).thenReturn(Optional.empty()); // No old token
            when(activationTokenRepository.save(any(ActivationToken.class))).thenReturn(newActivationToken);

            ArgumentCaptor<ActivationToken> activationTokenArgumentCaptor = ArgumentCaptor.forClass(ActivationToken.class);

            authenticationService.regenerateActivationTokenByUserEmail(USER_EMAIL);

            verify(activationTokenRepository, never()).delete(any(ActivationToken.class));
            verify(activationTokenRepository, times(1)).save(activationTokenArgumentCaptor.capture());
            ActivationToken capturedActivationToken = activationTokenArgumentCaptor.getValue();
            verify(emailService, times(1)).sendActivationEmail(USER_EMAIL, capturedActivationToken.getToken());
        }


        @Test
        @DisplayName("When regenerating activation token should generate new activation token for user and save it in database")
        public void whenRegeneratingActivationTokenShouldGenerateNewActivationTokenForUserAndSaveItInDatabase() {
            setupSuccessfulRegenerationMocks();

            ArgumentCaptor<ActivationToken> activationTokenArgumentCaptor = ArgumentCaptor.forClass(ActivationToken.class);

            authenticationService.regenerateActivationTokenByUserEmail(USER_EMAIL);

            verify(activationTokenRepository, times(1).description("Expected to save new activation token in database")).save(activationTokenArgumentCaptor.capture());

            ActivationToken capturedToken = activationTokenArgumentCaptor.getValue();
            assertEquals(user, capturedToken.getUser(), "Expected to issue token for correct user.");
            assertTrue(Instant.now().isBefore(capturedToken.getExpirationDate()), "Expected to new token expiration time be in the future");
        }

        @Test
        @DisplayName("When regenerating activation token should send new activation email to user")
        public void whenRegeneratingActivationTokenShouldSendNewActivationEmailToUser() {
            setupSuccessfulRegenerationMocks();
            authenticationService.regenerateActivationTokenByUserEmail(USER_EMAIL);

            verify(emailService, times(1).description("Expected to send user new activation email with correct token id.")).sendActivationEmail(USER_EMAIL, activationToken.getToken());
        }



    }


    @Nested
    @DisplayName("Authenticate user tests:")
    class AuthenticateUserTests{

        private AuthenticationRequest authenticationRequest;
        private DeviceType deviceType;

        private final Long REFRESH_TOKEN_ID = 1L;
        private final String ACCESS_TOKEN = "example-access-token-generated-by-jwt-utils";
        private final Long JWT_EXPIRATION_TIME = 1800000L;

        private RefreshToken refreshToken;

        @BeforeEach
        void setUp() {
            user.setActivated(true);

            deviceType = DeviceType.WEB;

            authenticationRequest = AuthenticationRequest.builder()
                    .email(USER_EMAIL)
                    .password(USER_PASSWORD)
                    .build();

            Instant tokenCreateDate = Instant.now();

            refreshToken = new RefreshToken();
            refreshToken.setId(REFRESH_TOKEN_ID);
            refreshToken.setToken(UUID.randomUUID().toString());
            refreshToken.setUser(user);
            refreshToken.setDeviceType(deviceType);
            refreshToken.setCreatedAt(tokenCreateDate);
            refreshToken.setExpiryDate(tokenCreateDate.plusSeconds(JWT_EXPIRATION_TIME));
            refreshToken.setLastUsedAt(tokenCreateDate);

        }

        private void setupSuccessfulAuthenticationMocks(){
            when(userRepository.findByIgnoreCaseEmail(USER_EMAIL)).thenReturn(userOptional);
            when(jwtUtils.generateAccessToken(user)).thenReturn(ACCESS_TOKEN);
            when(refreshTokenService.createRefreshToken(any(User.class), any(DeviceType.class))).thenReturn(refreshToken);
            when(jwtUtils.getAccessTokenExpiration()).thenReturn(JWT_EXPIRATION_TIME);

        }

        @Test
        @DisplayName("When authenticating should not generate any token or load user if authentication manager throws exception")
        public void whenAuthenticatingShouldNotGenerateAnyTokenOrLoadIfAuthenticationManagerThrowsException() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(new BadCredentialsException("Bad credentials."));
            authenticationRequest.setPassword(USER_PASSWORD_WRONG);

            assertThrows(AuthenticationException.class,
                    () -> authenticationService.authenticate(authenticationRequest, deviceType));

            verify(userRepository, never().description("Expected to not load user if any AuthenticationException will be thrown by AuthenticationManager.")).findByIgnoreCaseEmail(any(String.class));
            verify(jwtUtils, never().description("Expected to not generate any access token if any AuthenticationException will be thrown by AuthenticationManager.")).generateAccessToken(any(User.class));
            verify(refreshTokenService, never().description("Expected to not create any new RefreshToken if any AuthenticationException will be thrown by AuthenticationManager.")).createRefreshToken(any(User.class), any(DeviceType.class));

        }


        @Test
        @DisplayName("When authenticating should load user from database by method ignoring case in email")
        public void whenAuthenticatingShouldLoadUserFromDatabaseByMethodIgnoringCaseInEmail(){
            setupSuccessfulAuthenticationMocks();

            authenticationService.authenticate(authenticationRequest, deviceType);

            verify(userRepository, times(1)).findByIgnoreCaseEmail(USER_EMAIL);
        }


        @Test
        @DisplayName("When authenticating should generate jwt access token")
        public void whenAuthenticatingShouldGenerateJwtAccessToken(){
            setupSuccessfulAuthenticationMocks();

            authenticationService.authenticate(authenticationRequest, deviceType);

            verify((jwtUtils),times(1)).generateAccessToken(any(User.class));
        }

        @Test
        @DisplayName("When authenticating should create new refresh token using refresh token service")
        public void whenAuthenticatingShouldCreateNewRefreshToken(){
            setupSuccessfulAuthenticationMocks();

            authenticationService.authenticate(authenticationRequest, deviceType);

            verify(refreshTokenService,times(1)).createRefreshToken(user, deviceType);
        }


        @Test
        @DisplayName("When authenticating should return AuthenticationResponse containing correct tokens and expiration time")
        public void whenAuthenticatingShouldReturnAuthenticationResponseContainingCorrectTokensAndExpirationTime(){
            setupSuccessfulAuthenticationMocks();

            AuthenticationResponse output = authenticationService.authenticate(authenticationRequest, deviceType);

            assertAll("Returned authentication response assertions: ",
                    () -> assertEquals(ACCESS_TOKEN, output.getAccessToken()),
                    () -> assertEquals(refreshToken.getToken(), output.getRefreshToken()),
                    () -> assertEquals(JWT_EXPIRATION_TIME, output.getAccessTokenExpiration())
                    );
        }
    }

    @Nested
    @DisplayName("Refresh token tests:")
    class RefreshTokenTests{

        RefreshTokenRequest refreshTokenRequest;

        private final Long OLD_REFRESH_TOKEN_ID = 1L;
        private final Long NEW_REFRESH_TOKEN_ID = 2L;
        private final String ACCESS_TOKEN = "example-access-token-generated-by-jwt-utils";
        private final Long JWT_EXPIRATION_TIME = 1800000L;

        private DeviceType deviceType;
        private RefreshToken oldRefreshToken;
        private String oldRefreshTokenString;
        private RefreshToken newRefreshToken;
        private String newRefreshTokenString;

        static Stream<DeviceType> deviceTypes() {
            return Stream.of(DeviceType.values());
        }

        @BeforeEach
        void setUp() {

            user.setActivated(true);

            deviceType = DeviceType.WEB;

            Instant oldTokenCreateDate = Instant.now().minusSeconds(EXPIRATION_TIME /2);
            oldRefreshTokenString = UUID.randomUUID().toString();

            oldRefreshToken = new RefreshToken();
            oldRefreshToken.setId(OLD_REFRESH_TOKEN_ID);
            oldRefreshToken.setToken(oldRefreshTokenString);
            oldRefreshToken.setUser(user);
            oldRefreshToken.setDeviceType(deviceType);
            oldRefreshToken.setCreatedAt(oldTokenCreateDate);
            oldRefreshToken.setExpiryDate(oldTokenCreateDate.plusSeconds(JWT_EXPIRATION_TIME));
            oldRefreshToken.setLastUsedAt(oldTokenCreateDate);

            Instant newTokenCreateDate = Instant.now();
            newRefreshTokenString = UUID.randomUUID().toString();

            newRefreshToken = new RefreshToken();
            newRefreshToken.setId(NEW_REFRESH_TOKEN_ID);
            newRefreshToken.setToken(newRefreshTokenString);
            newRefreshToken.setUser(user);
            newRefreshToken.setDeviceType(deviceType);
            newRefreshToken.setCreatedAt(newTokenCreateDate);
            newRefreshToken.setExpiryDate(newTokenCreateDate.plusSeconds(JWT_EXPIRATION_TIME));
            newRefreshToken.setLastUsedAt(newTokenCreateDate);

            refreshTokenRequest = new RefreshTokenRequest(oldRefreshTokenString);
        }

        @Test
        @DisplayName("When refreshing access token should not generate any token if old refresh token was not found in database")
        public void whenRefreshingAccessTokenShouldNotGenerateAnyTokenIfOldRefreshTokenWasNotFoundInDatabase(){
            when(refreshTokenService.verifyAndGetRefreshToken(oldRefreshTokenString)).thenThrow(new RefreshTokenNotFoundException());

            assertThrows(RefreshTokenNotFoundException.class , () -> authenticationService.refreshToken(refreshTokenRequest));

            verify(jwtUtils, never().description("Expected to not generate and access token")).generateAccessToken(any(User.class));
            verify(refreshTokenService, never().description("Expected to not create new refresh token")).createRefreshToken(any(User.class), any(DeviceType.class));
        }
        @Test
        @DisplayName("When refreshing access token should not generate any token if old refresh token is revoked")
        public void whenRefreshingAccessTokenShouldNotGenerateAnyTokenIfOldRefreshTokenIsRevoked(){
            when(refreshTokenService.verifyAndGetRefreshToken(oldRefreshTokenString)).thenThrow(new RefreshTokenRevokedException());

            assertThrows(RefreshTokenRevokedException.class , () -> authenticationService.refreshToken(refreshTokenRequest));

            verify(jwtUtils, never().description("Expected to not generate and access token")).generateAccessToken(any(User.class));
            verify(refreshTokenService, never().description("Expected to not create new refresh token")).createRefreshToken(any(User.class), any(DeviceType.class));
        }

        @Test
        @DisplayName("When refreshing access token should not generate any token if old refresh token is expired")
        public void whenRefreshingAccessTokenShouldNotGenerateAnyTokenIfOldRefreshTokenIsExpired(){
            when(refreshTokenService.verifyAndGetRefreshToken(oldRefreshTokenString)).thenThrow(new RefreshTokenExpiredException());

            assertThrows(RefreshTokenExpiredException.class , () -> authenticationService.refreshToken(refreshTokenRequest));

            verify(jwtUtils, never().description("Expected to not generate and access token")).generateAccessToken(any(User.class));
            verify(refreshTokenService, never().description("Expected to not create new refresh token")).createRefreshToken(any(User.class), any(DeviceType.class));
        }

        @Test
        @DisplayName("When refreshing access token should not generate any token if user is banned")
        public void whenRefreshingAccessTokenShouldNotGenerateAnyTokenIfUserIsBanned(){
            user.setBanned(true);
            when(refreshTokenService.verifyAndGetRefreshToken(oldRefreshTokenString)).thenReturn(oldRefreshToken);

            assertThrows(UserBannedException.class , () -> authenticationService.refreshToken(refreshTokenRequest));

            verify(jwtUtils, never().description("Expected to not generate and access token")).generateAccessToken(any(User.class));
            verify(refreshTokenService, never().description("Expected to not create new refresh token")).createRefreshToken(any(User.class), any(DeviceType.class));
        }

        @Test
        @DisplayName("When refreshing access token should generate new access token for user")
        public void whenRefreshingAccessTokenShouldGenerateNewAccessTokenForUser(){
            when(refreshTokenService.verifyAndGetRefreshToken(oldRefreshTokenString)).thenReturn(oldRefreshToken);
            when(refreshTokenService.createRefreshToken(user, oldRefreshToken.getDeviceType())).thenReturn(newRefreshToken);
            when(jwtUtils.generateAccessToken(user)).thenReturn(ACCESS_TOKEN);
            when(jwtUtils.getAccessTokenExpiration()).thenReturn(JWT_EXPIRATION_TIME);


            authenticationService.refreshToken(refreshTokenRequest);

            verify(jwtUtils, times(1).description("Expected to generate new access token for user")).generateAccessToken(user);
        }


        @ParameterizedTest(name = "refreshToken behavior for deviceType={0}")
        @MethodSource("deviceTypes")
        @DisplayName("When refreshing access token should revoke old refresh token if device type is marked as rotational")
        public void whenRefreshingAccessTokenShouldRevokeOldRefreshTokenIfDeviceTypeIsMarkedAsRotational(DeviceType deviceTypeParam) {
            oldRefreshToken.setDeviceType(deviceTypeParam);
            newRefreshToken.setDeviceType(deviceTypeParam);

            when(jwtUtils.generateAccessToken(user)).thenReturn(ACCESS_TOKEN);
            when(jwtUtils.getAccessTokenExpiration()).thenReturn(JWT_EXPIRATION_TIME);
            when(refreshTokenService.verifyAndGetRefreshToken(oldRefreshTokenString)).thenReturn(oldRefreshToken);

            if (deviceTypeParam.shouldRotateRefreshToken())
                when(refreshTokenService.createRefreshToken(user, deviceTypeParam)).thenReturn(newRefreshToken);


            authenticationService.refreshToken(refreshTokenRequest);

            if (deviceTypeParam.shouldRotateRefreshToken()) {
                verify(refreshTokenService).revokeRefreshToken(oldRefreshTokenString);
                verify(refreshTokenService).createRefreshToken(user, deviceTypeParam);
            } else {
                verify(refreshTokenService, never()).revokeRefreshToken(any());
                verify(refreshTokenService, never()).createRefreshToken(any(User.class), any(DeviceType.class));
            }

        }

        @ParameterizedTest(name = "refreshToken behavior for deviceType={0}")
        @MethodSource("deviceTypes")
        @DisplayName("When refreshing access token should return new access token and new refresh token on rotational devices")
        public void whenRefreshingAccessTokenShouldReturnNewAccessTokenAndNewRefreshTokenOnRotationalDevices(DeviceType deviceTypeParam){
            oldRefreshToken.setDeviceType(deviceTypeParam);
            newRefreshToken.setDeviceType(deviceTypeParam);


            when(jwtUtils.generateAccessToken(user)).thenReturn(ACCESS_TOKEN);
            when(jwtUtils.getAccessTokenExpiration()).thenReturn(JWT_EXPIRATION_TIME);
            when(refreshTokenService.verifyAndGetRefreshToken(oldRefreshTokenString)).thenReturn(oldRefreshToken);

            if (deviceTypeParam.shouldRotateRefreshToken())
                when(refreshTokenService.createRefreshToken(user, deviceTypeParam)).thenReturn(newRefreshToken);


            AuthenticationResponse authenticationResponse = authenticationService.refreshToken(refreshTokenRequest);

            if (deviceTypeParam.shouldRotateRefreshToken()) {
                assertEquals(newRefreshTokenString, authenticationResponse.getRefreshToken());
                assertEquals(ACCESS_TOKEN, authenticationResponse.getAccessToken());
            } else {
                assertEquals(ACCESS_TOKEN, authenticationResponse.getAccessToken());
                assertEquals(oldRefreshTokenString, authenticationResponse.getRefreshToken());
            }

        }
    }

    @Nested
    @DisplayName("Logout tests:")
    class LogoutTests {

        RefreshTokenRequest refreshTokenRequest;

        private final Long REFRESH_TOKEN_ID = 1L;
        private final Long JWT_EXPIRATION_TIME = 1800000L;

        private RefreshToken refreshToken;
        private String refreshTokenString;
        private DeviceType deviceType;

        @BeforeEach
        void setUp() {
            Instant tokenCreateDate = Instant.now();
            refreshTokenString = UUID.randomUUID().toString();
            deviceType = DeviceType.WEB;

            refreshToken = new RefreshToken();
            refreshToken.setId(REFRESH_TOKEN_ID);
            refreshToken.setToken(refreshTokenString);
            refreshToken.setUser(user);
            refreshToken.setDeviceType(deviceType);
            refreshToken.setCreatedAt(tokenCreateDate);
            refreshToken.setExpiryDate(tokenCreateDate.plusSeconds(JWT_EXPIRATION_TIME));
            refreshToken.setLastUsedAt(tokenCreateDate);

            refreshTokenRequest = new RefreshTokenRequest(refreshTokenString);
        }


        @Test
        @DisplayName("When logging out should revoke token using RefreshTokenService")
        public void whenLoggingOutShouldRevokeTokenUsingRefreshTokenService() {

            authenticationService.logout(refreshTokenRequest);

            verify(refreshTokenService, times(1).description("Expected to delegate revoking logic to refresh token service")).revokeRefreshToken(refreshTokenString);
        }

        @Test
        @DisplayName("When logging out should throw RefreshTokenNotFoundException if given token does not exist")
        public void whenLoggingOutShouldThrowRefreshTokenNotFoundExceptionIfGivenTokenDoesNotExist() {
            doThrow(new RefreshTokenNotFoundException()).when(refreshTokenService).revokeRefreshToken(refreshTokenString);

            assertThrows(RefreshTokenNotFoundException.class, () -> authenticationService.logout(refreshTokenRequest));
        }

    }

    @Nested
    @DisplayName("Get current user tests")
    class GetCurrentUserTests {
        private Authentication authentication;
        private JwtUserDetails userDetails;
        SecurityContext securityContext;

        @BeforeEach
        void setUp() {
            SecurityContextHolder.clearContext();
            securityContext = SecurityContextHolder.createEmptyContext();
            SecurityContextHolder.setContext(securityContext);
            userDetails = new JwtUserDetails(USER_ID, USER_EMAIL, Collections.emptyList());
        }

        @AfterEach
        void tearDown() {
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("When getting current user should throw UserNotAuthenticatedException when authentication is null")
        void WhenGettingCurrentUserShouldThrowExceptionWhenAuthenticationIsNull() {
            SecurityContextHolder.getContext().setAuthentication(null);
            assertThrows(UserNotAuthenticatedException.class,
                    () -> authenticationService.getCurrentUser(), "Expected to throw UserNotAuthenticated if \'Authentication\' is null");

            verify(userRepository, never().description("Expected to not load any user from database")).findById(any());
        }

        @Test
        @DisplayName("When getting current user should throw UserNotAuthenticatedException when user is not authenticated")
        void WhenGettingCurrentUserShouldThrowExceptionWhenUserIsNotAuthenticated() {
            authentication = mock(Authentication.class);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            assertThrows(UserNotAuthenticatedException.class,
                    () -> authenticationService.getCurrentUser(), "Expected to throw UserNotAuthenticatedException if user was not authenticated in jwt request filter");

            verify(userRepository, never().description("Expected to not load any user from database")).findById(any());
        }
        @Test
        @DisplayName("When getting current user should throw UserNotAuthenticatedException when principal is not JwtUserDetails")
        void whenGettingCurrentUserShouldThrowExceptionWhenPrincipalIsNotJwtUserDetails() {
            authentication = mock(Authentication.class);

            when(authentication.getPrincipal()).thenReturn("anonymousUser");
            SecurityContextHolder.getContext().setAuthentication(authentication);

            assertThrows(UserNotAuthenticatedException.class,
                    () -> authenticationService.getCurrentUser());

            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("When getting current user should throw UserNotFoundException when user not found in database")
        void WhenGettingCurrentUserShouldThrowExceptionWhenUserNotFoundInDatabase() {
            authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> authenticationService.getCurrentUser(), "Expected to throw UserNotFoundException if user is not present in database");

            verify(userRepository).findById(USER_ID);
        }

        @Test
        @DisplayName("When getting current user should throw UserBannedException when user is banned")
        void WhenGettingCurrentUserShouldThrowExceptionWhenUserIsBanned() {
            user.setBanned(true);

            authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            when(userRepository.findById(USER_ID)).thenReturn(userOptional);


            assertThrows(UserBannedException.class,
                    () -> authenticationService.getCurrentUser(), "Expected to throw UserBannedException if user is marked as banned.");

            verify(userRepository, times(1).description("Expected to load user and check if is banned before returning")).findById(USER_ID);
        }

        @Test
        @DisplayName("When getting current user should return current user when authenticated")
        void WhenGettingCurrentUserShouldReturnCurrentUserWhenAuthenticated() {
            authentication = mock(Authentication.class);

            when(authentication.getPrincipal()).thenReturn(userDetails);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            when(userRepository.findById(USER_ID)).thenReturn(userOptional);

            User result = authenticationService.getCurrentUser();

            assertNotNull(result);
            assertEquals(USER_ID, result.getId());
            assertEquals(USER_EMAIL, result.getEmail());
            verify(userRepository).findById(USER_ID);
        }

    }

    @Nested
    @DisplayName("Get current user id tests")
    class GetCurrentUserIdTests{
        private Authentication authentication;
        private JwtUserDetails userDetails;


        @BeforeEach
        void setUp() {

            SecurityContextHolder.clearContext();
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            SecurityContextHolder.setContext(securityContext);
            userDetails = new JwtUserDetails(USER_ID, USER_EMAIL, Collections.emptyList());
        }

        @AfterEach
        void tearDown() {
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("When getting current user id should throw UserNotAuthenticatedException if authentication is set to null")
        public void whenGettingCurrentUserIdShouldThrowUserNotAuthenticatedExceptionIfAuthenticationIsSetToNull(){
            SecurityContextHolder.getContext().setAuthentication(null);

            assertThrows(UserNotAuthenticatedException.class, () ->authenticationService.getCurrentUserId(), "Expected to throw UserNotAuthenticatedException if authentication is set to null");

        }
        @Test
        @DisplayName("When getting current user id should throw UserNotAuthenticatedException if user was not authenticated")
        public void whenGettingCurrentUserIdShouldThrowUserNotAuthenticatedExceptionIfUserWasNotAuthenticated(){
            authentication = mock(Authentication.class);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            assertThrows(UserNotAuthenticatedException.class, () ->authenticationService.getCurrentUserId(), "Expected to throw UserNotAuthenticatedException if user was not authenticated");

        }

        @Test
        @DisplayName("When getting current user id should retrieve user details from user authentication")
        public void whenGettingCurrentUserIdShouldRetrieveUserDetailsFromAuthentication(){
            authentication = mock(Authentication.class);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);

            authenticationService.getCurrentUserId();

            verify(authentication, times(1).description("Expected to retrieve user details from authentication")).getPrincipal();
        }

        @Test
        @DisplayName("When getting current user id should return correct user id")
        public void whenGettingCurrentUserIdShouldReturnCorrectUserId(){
            authentication = mock(Authentication.class);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);

            UUID returnedUserId = authenticationService.getCurrentUserId();

            assertEquals(USER_ID, returnedUserId, "Expected to return correct user id.");
        }
    }
}