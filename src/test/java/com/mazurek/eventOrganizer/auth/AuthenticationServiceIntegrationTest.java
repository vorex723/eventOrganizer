package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.exception.auth.AccountAlreadyActivatedException;
import com.mazurek.eventOrganizer.exception.auth.ActivationTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.auth.UserNotAuthenticatedException;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.user.*;
import com.mazurek.eventOrganizer.jwt.*;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.RoleRepository;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import javax.management.relation.RoleNotFoundException;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthenticationServiceIntegrationTest {

    @Autowired
    AuthenticationService authenticationService;

    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private ActivationTokenRepository activationTokenRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtils jwtUtils;


    @Value("${activationToken.expiration:345600000}")
    private Long activationTokenExpiration;

    private final String USER_EMAIL = "example@dot.com";
    private final String USER_EMAIL_MIXED = "ExAmPlE@Dot.coM";
    private final String USER_FIRST_NAME = "Andrew";
    private final String USER_LAST_NAME = "Golota";
    private final String USER_PASSWORD = "Password123!";
    private final String USER_CITY = "Rzeszow".toLowerCase(Locale.ROOT);
    private final String USER_PASSWORD_WRONG = "WrongPassword123!";
    private final String USER_TIME_ZONE = "Europe/Warsaw";

    private final String ROLE_USER_NAME = "ROLE_USER";
    private final String ROLE_ADMIN_NAME = "ROLE_ADMIN";

    private final String CITY_RZESZOW_NAME = "rzeszow".toLowerCase(Locale.ROOT);

    @BeforeEach
    void setUp() {
        if (roleRepository.findByName(ROLE_USER_NAME).isEmpty()){
            Role roleUser = new Role(ROLE_USER_NAME);
            roleRepository.save(roleUser);
        }
        if(roleRepository.findByName(ROLE_ADMIN_NAME).isEmpty()){
            Role roleAdmin = new Role(ROLE_ADMIN_NAME);
            roleRepository.save(roleAdmin);
        }

    }

    @Nested
    @DisplayName("Register user tests:")
    class RegisterUserTests{

        private RegisterRequest registerRequest;

        @BeforeEach
        void setUp() {

            registerRequest = RegisterRequest.builder()
                    .email(USER_EMAIL)
                    .emailConfirmation(USER_EMAIL)
                    .password(USER_PASSWORD)
                    .passwordConfirmation(USER_PASSWORD)
                    .firstName(USER_FIRST_NAME)
                    .lastName(USER_LAST_NAME)
                    .homeCity(USER_CITY)
                    .timeZone(USER_TIME_ZONE)
                    .build();

        }

        @Test
        @DisplayName("When registering user should throw UserAlreadyExistException if account with provided email already exist")
        public void whenRegisteringUserShouldThrowUserAlreadyExistExceptionIfAccountWithProvidedEmailsAlreadyExist() throws RoleNotFoundException {
            City cityRzeszow= cityRepository.save(new City(CITY_RZESZOW_NAME));
            Instant userCreateDate = Instant.now();
            Role roleUser = roleRepository.findByName(ROLE_USER_NAME).orElseThrow(RoleNotFoundException::new);

            userRepository.save(User.builder()
                    .firstName(USER_FIRST_NAME)
                    .lastName(USER_LAST_NAME)
                    .email(USER_EMAIL)
                    .homeCity(cityRzeszow)
                    .password(passwordEncoder.encode(USER_PASSWORD))
                    .createdAt(userCreateDate)
                    .roles(Set.of(roleUser))
                    .timeZone(USER_TIME_ZONE)
                    .lastCredentialsChangeTime(userCreateDate)
                    .build());

            assertThrows(UserAlreadyExistException.class, () -> authenticationService.register(registerRequest), "Expected to throw UserAlreadyExistException if there is account using provided by user email");
        }
        @AfterEach
        void clean() {
            activationTokenRepository.deleteAll();
            userRepository.deleteAll();
            cityRepository.deleteAll();
        }

        @Test
        @DisplayName("When registering should detect duplicate email regardless of case")
        public void whenRegisteringShouldDetectDuplicateEmailRegardlessOfCase()
                throws RoleNotFoundException {

            authenticationService.register(registerRequest);

            RegisterRequest duplicateRequest = RegisterRequest.builder()
                    .email(USER_EMAIL.toUpperCase())
                    .emailConfirmation(USER_EMAIL.toUpperCase())
                    .password(USER_PASSWORD)
                    .passwordConfirmation(USER_PASSWORD)
                    .firstName("Different")
                    .lastName("Person")
                    .homeCity(USER_CITY)
                    .timeZone(USER_TIME_ZONE)
                    .build();

            assertThrows(UserAlreadyExistException.class,
                    () -> authenticationService.register(duplicateRequest),
                    "Should detect duplicate email regardless of case");
        }

        @Test
        @DisplayName("When registering user should throw NotMatchingPasswordsException and not persist any data if provided passwords are not the same")
        public void whenRegisteringUserShouldThrowNotMatchingPasswordsExceptionAndNotPersistAnyDataIfProvidedPasswordsAreNotTheSame() {
            registerRequest.setPasswordConfirmation(USER_PASSWORD_WRONG);

            long userCountBefore = userRepository.count();
            long tokenCountBefore = activationTokenRepository.count();

            assertThrows(NotMatchingPasswordsException.class,
                    () -> authenticationService.register(registerRequest));

            assertAll("Verify no data was persisted",
                    () -> assertEquals(userCountBefore, userRepository.count(),
                            "No new users should be created"),
                    () -> assertEquals(tokenCountBefore, activationTokenRepository.count(),
                            "No activation tokens should be created"),
                    () -> assertFalse(userRepository.findByIgnoreCaseEmail(USER_EMAIL).isPresent(),
                            "User with provided email should not exist")
            );
        }

        @Test
        @DisplayName("When registering user should throw NotMatchingEmailsException and not persist any data if provided emails are not the same")
        public void whenRegisteringUserShouldThrowNotMatchingEmailsExceptionAndNotPersistAnyDataIfProvidedEmailsAreNotTheSame() {
            registerRequest.setEmailConfirmation("different@email.com");

            long userCountBefore = userRepository.count();
            long tokenCountBefore = activationTokenRepository.count();

            assertThrows(NotMatchingEmailsException.class,
                    () -> authenticationService.register(registerRequest));

            assertAll("Verify no data was persisted",
                    () -> assertEquals(userCountBefore, userRepository.count(),
                            "No new users should be created"),
                    () -> assertEquals(tokenCountBefore, activationTokenRepository.count(),
                            "No activation tokens should be created"),
                    () -> assertFalse(userRepository.findByIgnoreCaseEmail(USER_EMAIL).isPresent(),
                            "User with provided email should not exist")
            );
        }

        @Test
        @DisplayName("When registering user should save user in database with correct data")
        public void whenRegisteringUserShouldSaveUserInDatabaseWithCorrectData(){
            authenticationService.register(registerRequest);

            User savedUser = userRepository.findByIgnoreCaseEmail(USER_EMAIL).orElseThrow(UserNotFoundException::new);

            assertAll("User provided data assertion: ",
                    () -> assertEquals(USER_EMAIL.toLowerCase(), savedUser.getEmail()),
                    () -> assertEquals(USER_FIRST_NAME, savedUser.getFirstName()),
                    () -> assertEquals(USER_LAST_NAME, savedUser.getLastName()),
                    () -> assertEquals(USER_TIME_ZONE, savedUser.getTimeZone()),
                    () -> assertTrue(passwordEncoder.matches(USER_PASSWORD, savedUser.getPassword())),
                    () -> assertEquals(USER_CITY.toLowerCase(), savedUser.getHomeCity().getName())
            );

            assertAll("Server side provided data assertions: ",
                    () -> assertEquals(1,savedUser.getRoles().size()),
                    () -> assertFalse(savedUser.isActivated()),
                    () -> assertFalse(savedUser.isBanned()),
                    () -> assertTrue(savedUser.getRoles().stream()
                            .anyMatch(role -> role.getName().equals(ROLE_USER_NAME))),
                    () -> assertTrue(Instant.now().isAfter(savedUser.getCreatedAt())),
                    () -> assertEquals(savedUser.getCreatedAt(), savedUser.getLastCredentialsChangeTime())
            );
        }

        @Test
        @DisplayName("When registering user should normalize user email to lower case")
        public void whenRegisteringUserShouldNormalizeUserEmailToLowerCase(){
            registerRequest.setEmail(USER_EMAIL_MIXED);
            registerRequest.setEmailConfirmation(USER_EMAIL_MIXED);

            authenticationService.register(registerRequest);

            User savedUser = userRepository.findByIgnoreCaseEmail(USER_EMAIL).orElseThrow(UserNotFoundException::new);

            assertEquals(USER_EMAIL_MIXED.toLowerCase(), savedUser.getEmail());

        }

        @Test
        @DisplayName("When registering user should generate and save account activation token in database")
        public void whenRegisteringUserShouldGenerateAndSaveAccountActivationTokenInDatabase(){
            authenticationService.register(registerRequest);

            User user = userRepository.findByIgnoreCaseEmail(USER_EMAIL).orElseThrow(UserNotFoundException::new);
            ActivationToken savedToken = activationTokenRepository.findByIgnoreCaseUserEmail(USER_EMAIL).orElseThrow(ActivationTokenNotFoundException::new);

            assertAll("Activation token assertions: ",
                    () -> assertEquals(user, savedToken.getUser()),
                    () -> assertTrue(Instant.now().isBefore(savedToken.getExpirationDate()))
            );

        }

    }

    @Nested
    @DisplayName("Activate account tests:")
    class ActivateAccountTests{
        private UUID token;

        @BeforeEach
        void setUp() throws RoleNotFoundException {
            City cityRzeszow = cityRepository.save(new City(CITY_RZESZOW_NAME));
            Role roleUser = roleRepository.findByName(ROLE_USER_NAME)
                    .orElseThrow(RoleNotFoundException::new);

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
                    .activated(false)
                    .banned(false)
                    .build());

            cityRzeszow.addResident(user);
            cityRepository.save(cityRzeszow);

            token = UUID.randomUUID();

            activationTokenRepository.save(
                    ActivationToken.builder()
                            .user(user)
                            .token(token)
                            .expirationDate(Instant.now().plusMillis(activationTokenExpiration))
                            .build());
        }

        @AfterEach
        void clean() {
            activationTokenRepository.deleteAll();
            userRepository.deleteAll();
            cityRepository.deleteAll();
        }

        @Test
        @DisplayName("When activating account should activate user account and return ACTIVATED when token is valid")
        void whenActivatingAccountShouldActivateUserAccountAndReturnActivatedWhenTokenIsValid(){
            ActivationResult result = authenticationService.activateAccount(token);

            User activatedUser = userRepository.findByEmail(USER_EMAIL)
                    .orElseThrow(UserNotFoundException::new);

            assertEquals(ActivationResult.ACTIVATED, result, "Expected returned value ACTIVATED.");
            assertTrue(activatedUser.isActivated(), "Expected user account to be set as activated.");
        }

        @Test
        @DisplayName("When activating account should delete activation token after successful activation")
        void whenActivatingAccountShouldDeleteActivationTokenAfterSuccessfulActivation(){
            authenticationService.activateAccount(token);

            assertTrue(activationTokenRepository.findByToken(token).isEmpty());
        }

        @Test
        @DisplayName("When activating account should regenerate token with future expiration if old token is expired")
        void whenActivatingAccountShouldRegenerateTokenWithFutureExpirationWhenOldTokenIsExpired(){
            expireToken(token);

            authenticationService.activateAccount(token);

            ActivationToken regeneratedToken = activationTokenRepository.findByIgnoreCaseUserEmail(USER_EMAIL)
                    .orElseThrow(ActivationTokenNotFoundException::new);

            assertFalse(regeneratedToken.isExpired(), "Expected new token to not be already expired.");
        }

        @Test
        @DisplayName("When activating account should not activate user when token is expired")
        void whenActivatingAccountShouldNotActivateUserWhenTokenIsExpired(){
            expireToken(token);

            authenticationService.activateAccount(token);

            User user = userRepository.findByIgnoreCaseEmail(USER_EMAIL)
                    .orElseThrow(UserNotFoundException::new);

            assertFalse(user.isActivated(), "Expected user account to not be activated.");
        }

        @Test
        @DisplayName("When activating account should return TOKEN_EXPIRED_NEW_SENT when token is expired")
        void whenActivatingAccountShouldReturnTokenExpiredNewSentWhenTokenIsExpired(){
            expireToken(token);

            ActivationResult result = authenticationService.activateAccount(token);

            assertEquals(ActivationResult.TOKEN_EXPIRED_NEW_SENT, result, "Expected returned value TOKEN_EXPIRED_NEW_SENT if old token was expired.");
        }

        @Test
        @DisplayName("When activating account should have exactly one token after replacing expired token")
        void whenActivatingAccountShouldHaveExactlyOneTokenAfterReplacingExpiredToken(){

            expireToken(token);

            authenticationService.activateAccount(token);

            long tokenCount = activationTokenRepository.findAll().stream()
                    .filter(token -> token.getUser().getEmail().equalsIgnoreCase(USER_EMAIL))
                    .count();

            assertEquals(1, tokenCount);
        }

        @Test
        @DisplayName("When activating account should throw ActivationTokenNotFoundException when token does not exist")
        void whenActivatingAccountShouldThrowActivationTokenNotFoundExceptionWhenTokenDoesNotExist(){
            UUID nonExistentTokenId = UUID.randomUUID();

            assertThrows(ActivationTokenNotFoundException.class,
                    () -> authenticationService.activateAccount(nonExistentTokenId));
        }
        @Test
        @DisplayName("When activating account should not activate user when token is not found")
        void whenActivatingAccountShouldNotActivateUserWhenTokenIsNotFound(){
            assertThrows(ActivationTokenNotFoundException.class,() -> authenticationService.activateAccount(UUID.randomUUID()));

            User unchangedUser = userRepository.findByIgnoreCaseEmail(USER_EMAIL)
                    .orElseThrow(UserNotFoundException::new);

            assertFalse(unchangedUser.isActivated(), "Expected user to not be activated.");
        }

        private void expireToken(UUID tokenId) {
            ActivationToken token = activationTokenRepository.findByToken(tokenId)
                    .orElseThrow(ActivationTokenNotFoundException::new);
            token.setExpirationDate(Instant.now().minusSeconds(1000L));
            activationTokenRepository.save(token);
        }

    }

    @Nested
    @DisplayName("Regenerate activation token by user email tests:")
    class RegenerateActivationTokenByUserEmailTests{
        private Long activationTokenId;
        private UUID token;

        @BeforeEach
        void setUp() throws RoleNotFoundException {

            City cityRzeszow = cityRepository.save(new City(CITY_RZESZOW_NAME));
            Role roleUser = roleRepository.findByName(ROLE_USER_NAME)
                    .orElseThrow(RoleNotFoundException::new);

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
                    .activated(false)
                    .banned(false)
                    .build());

            cityRzeszow.addResident(user);
            cityRepository.save(cityRzeszow);

            token = UUID.randomUUID();

            activationTokenId = activationTokenRepository.save(
                            ActivationToken.builder()
                                    .user(user)
                                    .token(token)
                                    .expirationDate(Instant.now().plusMillis(activationTokenExpiration))
                                    .build())
                    .getId();
        }

        @AfterEach
        void clean() {
            activationTokenRepository.deleteAll();
            userRepository.deleteAll();
            cityRepository.deleteAll();
        }

        @Test
        @DisplayName("When regenerating activation token should not proceed if account is already active")
        public void whenRegeneratingActivationTokenShouldNotProceedIfAccountIsAlreadyActive(){
            User user = userRepository.findByIgnoreCaseEmail(USER_EMAIL).orElseThrow(UserNotFoundException::new);
            user.setActivated(true);
            userRepository.save(user);
            activationTokenRepository.deleteById(activationTokenId);

            assertThrows(AccountAlreadyActivatedException.class, () ->
                    authenticationService.regenerateActivationTokenByUserEmail(USER_EMAIL),
                    "Expected to throw AccountAlreadyActivatedException if account is already active.");
            assertTrue(activationTokenRepository.findByIgnoreCaseUserEmail(USER_EMAIL).isEmpty(), "Expected to not create any token for active account.");

        }

        @Test
        @DisplayName("When regenerating activation token should replace old activation token in database if old token is present")
        public void whenRegeneratingActivationTokenShouldRemoveOldTokenInDatabaseIfOldTokenIsPresent(){

            UUID oldToken = activationTokenRepository.findByToken(token).orElseThrow(ActivationTokenNotFoundException::new).getToken();

            authenticationService.regenerateActivationTokenByUserEmail(USER_EMAIL);

            ActivationToken regeneratedToken = activationTokenRepository.findByIgnoreCaseUserEmail(USER_EMAIL).orElseThrow(ActivationTokenNotFoundException::new);

            assertAll("Regenerated token assertions: ",
                    () -> assertTrue(activationTokenRepository.findByToken(token).isEmpty(), "Expected old token to replaced in database."),
                    () -> assertEquals(activationTokenId, regeneratedToken.getId(), "Expected to not change Activation Token id."),
                    () -> assertNotEquals(oldToken, regeneratedToken.getToken(), "Expected token value to be different after regeneration.")
            );

        }

        @Test
        @DisplayName("When regenerating activation token should generate new token and expiration date and save it in database")
        public void whenRegeneratingActivationTokenShouldGenerateNewTokenAndExpirationDateAndSaveItInDatabase(){
            ActivationToken oldToken = activationTokenRepository.findByToken(token).orElseThrow(ActivationTokenNotFoundException::new);
            Instant oldExpirationDate = oldToken.getExpirationDate();

            authenticationService.regenerateActivationTokenByUserEmail(USER_EMAIL);

            ActivationToken regeneratedToken = activationTokenRepository.findByIgnoreCaseUserEmail(USER_EMAIL).orElseThrow(ActivationTokenNotFoundException::new);

            assertAll("Regenerated token assertions: ",
                    () -> assertEquals(activationTokenId, regeneratedToken.getId(), "Expected new token to have the same id as old one."),
                    () -> assertTrue(Instant.now().isBefore(regeneratedToken.getExpirationDate()), "Expected new token expiration date to be in the future."),
                    () -> assertNotEquals(oldExpirationDate, regeneratedToken.getExpirationDate(), "Expected expiration date to be updated.")
            );
        }

        @Test
        @DisplayName("When regenerating activation token should create new token when old token does not exist")
        void whenRegeneratingActivationTokenShouldCreateNewTokenWhenOldTokenDoesNotExist(){
            activationTokenRepository.deleteById(activationTokenId);

            authenticationService.regenerateActivationTokenByUserEmail(USER_EMAIL);

            ActivationToken newToken = activationTokenRepository.findByIgnoreCaseUserEmail(USER_EMAIL)
                    .orElseThrow(ActivationTokenNotFoundException::new);

            assertAll("New token assertions: ",
                    () -> assertNotEquals(activationTokenId, newToken.getId(), "Expected new token to not have the same id as old one."),
                    () -> assertNotEquals(token, newToken.getToken(), "Expected token to be different than last one."),
                    () -> assertTrue(Instant.now().isBefore(newToken.getExpirationDate()), "Expected new token expiration date to be in the future.")
            );
        }
    }

    @Nested
    @DisplayName("Authenticate user tests:")
    class AuthenticateUserTests{
        private AuthenticationRequest authenticationRequest;
        private DeviceType deviceType;

        @BeforeEach
        void setUp() throws RoleNotFoundException {

            City cityRzeszow = cityRepository.save(new City(CITY_RZESZOW_NAME));
            Role roleUser = roleRepository.findByName(ROLE_USER_NAME)
                    .orElseThrow(RoleNotFoundException::new);

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

            authenticationRequest = new AuthenticationRequest(USER_EMAIL, USER_PASSWORD);
            deviceType = DeviceType.WEB;
        }

        @AfterEach
        void clean() {
            activationTokenRepository.deleteAll();
            refreshTokenRepository.deleteAll();
            userRepository.deleteAll();
            cityRepository.deleteAll();
        }

        @Test
        @DisplayName("When authenticating user should throw spring BadCredentialsException if provided password is wrong.")
        public void whenAuthenticatingUserShouldThrowSpringBadCredentialsExceptionIfProvidedPasswordIsWrong(){
            authenticationRequest.setPassword(USER_PASSWORD_WRONG);
            assertThrows(BadCredentialsException.class , () -> authenticationService.authenticate(authenticationRequest, deviceType), "Expected to throw BadCredentialsException if user provide wrong password.");

        }

        @Test
        @DisplayName("When authenticating user should throw spring BadCredentialsException if provided email is not in database.")
        public void whenAuthenticatingUserShouldThrowSpringBadCredentialsExceptionIfProvidedEmailIsNotInDatabase(){
            authenticationRequest.setEmail("wrongEmail@example.com");
            assertThrows(BadCredentialsException.class , () -> authenticationService.authenticate(authenticationRequest, deviceType), "Expected to throw BadCredentialsException if user with given email does not exist.");

        }

        @Test
        @DisplayName("When authenticating user should throw spring DisabledException if user account is not activated.")
        public void whenAuthenticatingUserShouldThrowSpringDisabledExceptionIfUserAccountIsNotActivated(){
            User user = userRepository.findByIgnoreCaseEmail(USER_EMAIL).orElseThrow(UserNotFoundException::new);
            user.setActivated(false);
            userRepository.saveAndFlush(user);

            assertThrows(DisabledException.class , () -> authenticationService.authenticate(authenticationRequest, deviceType), "Expected to throw Spring DisabledException if user account is not activated.");
        }

        @Test
        @DisplayName("When authenticating user should throw spring LockedException if user is marked as banned")
        public void whenAuthenticatingUserShouldThrowSpringLockedExceptionIfUserIsMarkedAsBanned(){
            User user = userRepository.findByIgnoreCaseEmail(USER_EMAIL).orElseThrow(UserNotFoundException::new);
            user.setBanned(true);
            userRepository.saveAndFlush(user);

            assertThrows(LockedException.class , () -> authenticationService.authenticate(authenticationRequest, deviceType), "Expected to throw Spring LockedException if user is banned.");

        }

        @Test
        @DisplayName("When authenticating user should not throw spring BadCredentialsException if email is correct but is not in lower case.")
        public void whenAuthenticatingUserShouldNotThrowSpringBadCredentialsExceptionIfEmailIsCorrectButIsNotInLowerCase(){
            authenticationRequest.setEmail(USER_EMAIL.toUpperCase());
            assertDoesNotThrow(() -> authenticationService.authenticate(authenticationRequest, deviceType), "Expected to not throw BadCredentialsException if user do not provide email in lower case.");
        }


        @Test
        @DisplayName("When authenticating user should return AuthenticationResponse with access and refresh tokens present in it")
        public void whenAuthenticatingUserShouldReturnAuthenticationResponseWithAccessAndRefreshTokensPresentInIt(){
            AuthenticationResponse response = authenticationService.authenticate(authenticationRequest,deviceType);

            assertTrue(refreshTokenRepository.findByToken(response.getRefreshToken()).isPresent(), "Expected to create new refresh token in database");

            assertAll("Authentication response assertions: ",
                    () -> assertNotNull(response.getAccessToken(), "Expected returned access token to not be null."),
                    () -> assertFalse(response.getAccessToken().isBlank(), "Expected returned access token to not be blank."),
                    () -> assertNotNull(response.getRefreshToken(), "Expected returned refresh token to not be null."),
                    () -> assertFalse(response.getRefreshToken().isBlank(), "Expected refresh token to not be blank."),
                    () -> assertEquals(jwtUtils.getAccessTokenExpiration(), response.getAccessTokenExpiration(), "Expected to set correct token expiration time")
                    );

        }

    }

    @Nested
    @DisplayName("Refresh token tests:")
    class RefreshTokenTests{
        private RefreshTokenRequest refreshTokenRequest;
        private DeviceType deviceType;

        private final Long REFRESH_TOKEN_EXPIRATION_TIME = 2592000000L;

        @BeforeEach
        void setUp() throws RoleNotFoundException {
            City cityRzeszow = cityRepository.save(new City(CITY_RZESZOW_NAME));
            Role roleUser = roleRepository.findByName(ROLE_USER_NAME)
                    .orElseThrow(RoleNotFoundException::new);

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

            Instant refreshTokenCreateDateTime = Instant.now();
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setToken(UUID.randomUUID().toString());
            refreshToken.setUser(user);
            refreshToken.setDeviceType(deviceType);
            refreshToken.setCreatedAt(refreshTokenCreateDateTime);
            refreshToken.setExpiryDate(refreshTokenCreateDateTime.plusMillis(REFRESH_TOKEN_EXPIRATION_TIME));
            refreshToken.setLastUsedAt(refreshTokenCreateDateTime);

            refreshTokenRepository.save(refreshToken);

            refreshTokenRequest = new RefreshTokenRequest(refreshToken.getToken());
        }

        @AfterEach
        void clean() {
            activationTokenRepository.deleteAll();
            refreshTokenRepository.deleteAll();
            userRepository.deleteAll();
            cityRepository.deleteAll();
        }

        @Test
        @DisplayName("When refreshing token should throw UserBannedException if banned user tries to refresh token")
        public void whenRefreshingTokenShouldThrowUserBannedExceptionIfBannedUserTriesToRefreshToken(){
            User user = userRepository.findByIgnoreCaseEmail(USER_EMAIL).orElseThrow(UserNotFoundException::new);
            user.setBanned(true);
            userRepository.saveAndFlush(user);

            assertThrows(UserBannedException.class ,() -> authenticationService.refreshToken(refreshTokenRequest));
        }

        @Test
        @DisplayName("When refreshing token should return valid access token")
        public void whenRefreshingTokenShouldReturn(){
            AuthenticationResponse authenticationResponse = authenticationService.refreshToken(refreshTokenRequest);

            assertTrue(jwtUtils.isTokenValid(authenticationResponse.getAccessToken()), "Expected to return valid access token.");
            assertEquals(jwtUtils.getAccessTokenExpiration(), authenticationResponse.getAccessTokenExpiration(), "Expected to return correct value of access token expiration.");

        }

        @Test
        @DisplayName("When refreshing token should rotate refresh token with correct data")
        public void whenRefreshingTokenShouldRotateRefreshTokenIfWithCorrectData(){
            AuthenticationResponse authenticationResponse = authenticationService.refreshToken(refreshTokenRequest);
            User user = userRepository.findByIgnoreCaseEmail(USER_EMAIL).orElseThrow(UserNotFoundException::new);
            RefreshToken oldRefreshToken = refreshTokenRepository.findByToken(refreshTokenRequest.refreshToken())
                    .orElseThrow(RefreshTokenNotFoundException::new);

            assertTrue(oldRefreshToken.isRevoked(), "Expected old refresh token to be revoked");
            assertNotEquals(authenticationResponse.getRefreshToken(), oldRefreshToken.getToken(), "Expected to return new refresh token.");

            Optional<RefreshToken> refreshTokenOptional = refreshTokenRepository.findByToken(authenticationResponse.getRefreshToken());
            assertTrue(refreshTokenOptional.isPresent(), "Expected to create new refresh token");
            RefreshToken refreshToken = refreshTokenOptional.get();
            assertAll("New refresh token assertions: ",
                    () -> assertEquals(user, refreshToken.getUser(), "Expected to set correct user."),
                    () -> assertFalse(refreshToken.isRevoked(), "Expected new token to not be instantly revoked."),
                    () -> assertFalse(refreshToken.isExpired(), "Expected new token to not be instantly expired."),
                    () -> assertEquals(oldRefreshToken.getDeviceType(), refreshToken.getDeviceType(), "Expected to set the same device type as before token refreshing.")
            );
        }

        @Test
        @DisplayName("When refreshing token should not rotate refresh token if device type is not marked as rotational")
        public void whenRefreshingTokenShouldNotRotateRefreshTokenIfDeviceTypeIsNotMarkedAsRotational(){
            refreshTokenRepository.deleteAll();
            User user = userRepository.findByIgnoreCaseEmail(USER_EMAIL).orElseThrow(UserNotFoundException::new);

            Instant refreshTokenCreateDateTime = Instant.now();
            RefreshToken mobileRefreshToken = new RefreshToken();
            mobileRefreshToken.setToken(UUID.randomUUID().toString());
            mobileRefreshToken.setUser(user);
            mobileRefreshToken.setDeviceType(DeviceType.MOBILE_ANDROID);
            mobileRefreshToken.setCreatedAt(refreshTokenCreateDateTime);
            mobileRefreshToken.setExpiryDate(refreshTokenCreateDateTime.plusMillis(REFRESH_TOKEN_EXPIRATION_TIME));
            mobileRefreshToken.setLastUsedAt(refreshTokenCreateDateTime);

            refreshTokenRepository.saveAndFlush(mobileRefreshToken);

            RefreshTokenRequest mobileRefreshTokenRequest = new RefreshTokenRequest(mobileRefreshToken.getToken());

            AuthenticationResponse authenticationResponse = authenticationService.refreshToken(mobileRefreshTokenRequest);

            assertEquals(mobileRefreshToken.getToken(), authenticationResponse.getRefreshToken(), "Expected to not rotate refresh token.");
            assertEquals(1, refreshTokenRepository.findAll().stream().filter(refreshToken -> refreshToken.getUser().equals(user)).count());
        }

    }

    @Nested
    @DisplayName("Logout tests:")
    class LogoutTests{
        private RefreshTokenRequest refreshTokenRequest;
        private DeviceType deviceType;

        private final Long REFRESH_TOKEN_EXPIRATION_TIME = 2592000000L;

        @BeforeEach
        void setUp() throws RoleNotFoundException {
            City cityRzeszow = cityRepository.save(new City(CITY_RZESZOW_NAME));
            Role roleUser = roleRepository.findByName(ROLE_USER_NAME)
                    .orElseThrow(RoleNotFoundException::new);

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

            Instant refreshTokenCreateDateTime = Instant.now();
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setToken(UUID.randomUUID().toString());
            refreshToken.setUser(user);
            refreshToken.setDeviceType(deviceType);
            refreshToken.setCreatedAt(refreshTokenCreateDateTime);
            refreshToken.setExpiryDate(refreshTokenCreateDateTime.plusMillis(REFRESH_TOKEN_EXPIRATION_TIME));
            refreshToken.setLastUsedAt(refreshTokenCreateDateTime);

            refreshTokenRepository.save(refreshToken);

            refreshTokenRequest = new RefreshTokenRequest(refreshToken.getToken());
        }

        @AfterEach
        void clean() {
            activationTokenRepository.deleteAll();
            refreshTokenRepository.deleteAll();
            userRepository.deleteAll();
            cityRepository.deleteAll();
        }

        @Test
        @DisplayName("When logging out user should revoke provided token")
        public void whenLoggingOutUserShouldRevokeProvidedToken(){
            authenticationService.logout(refreshTokenRequest);

            RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenRequest.refreshToken()).orElseThrow(RefreshTokenNotFoundException::new);

            assertTrue(refreshToken.isRevoked(), "Expected to revoke provided token.");
        }
    }

    @Nested
    @DisplayName("Get current user tests:")
    class GetCurrentUserTests{
        @BeforeEach
        void setUp() throws RoleNotFoundException {

            City cityRzeszow = cityRepository.save(new City(CITY_RZESZOW_NAME));
            Role roleUser = roleRepository.findByName(ROLE_USER_NAME)
                    .orElseThrow(RoleNotFoundException::new);

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
        }

        @AfterEach
        void clean() {
            activationTokenRepository.deleteAll();
            refreshTokenRepository.deleteAll();
            userRepository.deleteAll();
            cityRepository.deleteAll();
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("When getting current user should throw UserNotAuthenticatedException if authentication is not present in SecurityContext")
        public void whenGettingCurrentUserShouldThrowUserNotAuthenticatedExceptionIfAuthenticationIsNotPresentInSecurityContext(){
            SecurityContextHolder.clearContext();
            assertThrows(UserNotAuthenticatedException.class, () -> authenticationService.getCurrentUser());

        }

        @Test
        @DisplayName("When getting current user should return correct user")
        public void whenGettingCurrentUserShouldReturnCorrectUser(){
            User user = userRepository.findByIgnoreCaseEmail(USER_EMAIL).orElseThrow(UserNotFoundException::new);

            JwtUserDetails userDetails = new JwtUserDetails(user.getId(),
                    user.getEmail(),
                    user.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getName())).toList());

            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    user.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getName())).toList()
            );

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);


            User returnedUser = authenticationService.getCurrentUser();

            assertEquals(user, returnedUser, "Expected to return correct user.");

        }
        @Test
        @DisplayName("When getting current user should throw UserBannedException if user have valid token but is banned")
        public void whenGettingCurrentUserShouldThrowUserBannedExceptionIfUserHaveValidTokenButIsBanned(){
            User user = userRepository.findByIgnoreCaseEmail(USER_EMAIL).orElseThrow(UserNotFoundException::new);

            JwtUserDetails userDetails = new JwtUserDetails(user.getId(),
                    user.getEmail(),
                    user.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getName())).toList());

            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    user.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getName())).toList()
            );

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            user.setBanned(true);
            userRepository.saveAndFlush(user);

            assertThrows(UserBannedException.class, () -> authenticationService.getCurrentUser(), "Expected to throw UserBannedException if user is banned.");

        }

    }


    @Nested
    @DisplayName("Get current user id tests: ")
    class GetCurrentUserIdTests{

        @BeforeEach
        void setUp() throws RoleNotFoundException {

            City cityRzeszow = cityRepository.save(new City(CITY_RZESZOW_NAME));
            Role roleUser = roleRepository.findByName(ROLE_USER_NAME)
                    .orElseThrow(RoleNotFoundException::new);

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

            SecurityContextHolder.clearContext();
        }

        @AfterEach
        void clean() {
            activationTokenRepository.deleteAll();
            refreshTokenRepository.deleteAll();
            userRepository.deleteAll();
            cityRepository.deleteAll();
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("When getting current user id should throw UserNotAuthenticatedException if authentication is not present in SecurityContext")
        public void whenGettingCurrentUserIdShouldThrowUserNotAuthenticatedExceptionIfAuthenticationIsNotPresentInSecurityContext(){
            assertThrows(UserNotAuthenticatedException.class, () -> authenticationService.getCurrentUserId());
        }

        @Test
        @DisplayName("When getting current user should return correct user id")
        public void whenGettingCurrentUserShouldReturnCorrectUser(){

            User user = userRepository.findByIgnoreCaseEmail(USER_EMAIL).orElseThrow(UserNotFoundException::new);

            JwtUserDetails userDetails = new JwtUserDetails(user.getId(),
                    user.getEmail(),
                    user.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getName())).toList());

            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    user.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getName())).toList()
            );

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            UUID returnedUserId = authenticationService.getCurrentUserId();

            assertEquals(user.getId(), returnedUserId, "Expected to return correct user id.");
        }
    }

}
