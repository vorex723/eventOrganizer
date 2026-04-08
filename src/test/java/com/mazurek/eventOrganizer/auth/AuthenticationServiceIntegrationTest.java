package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationRequest;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationResponse;
import com.mazurek.eventOrganizer.auth.dto.RefreshTokenRequest;
import com.mazurek.eventOrganizer.auth.dto.RegisterRequest;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.exception.auth.AccountAlreadyActivatedException;
import com.mazurek.eventOrganizer.exception.auth.ActivationTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.auth.UserNotAuthenticatedException;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.user.*;
import com.mazurek.eventOrganizer.jwt.*;
import com.mazurek.eventOrganizer.testData.builders.ActivationTokenTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.AuthenticationRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.CityTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.RefreshTokenTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.RoleTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.RefreshTokenRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.RegisterRequestTestBuilder;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.RoleRepository;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Locale;
import java.util.*;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthenticationService integration tests:")
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
    @Autowired
    private DeletionService deletionService;


    @Value("${app.auth.activation-token-expiration:345600000}")
    private Long activationTokenExpiration;

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        SecurityContextHolder.clearContext();
        if (roleRepository.findByName(RoleConstants.ROLE_USER_NAME).isEmpty()){
            Role roleUser = RoleTestBuilder.userRole().id(null).build();
            roleRepository.save(roleUser);
        }
        if(roleRepository.findByName(RoleConstants.ROLE_ADMIN_NAME).isEmpty()){
            Role roleAdmin = RoleTestBuilder.adminRole().id(null).build();
            roleRepository.save(roleAdmin);
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

    private User persistUser(UserTestBuilder userBuilder, City city, boolean activated, boolean banned) {
        Instant userCreateDateTime = Instant.now();

        return userRepository.save(userBuilder
                .id(null)
                .homeCity(city)
                .password(passwordEncoder.encode(UserConstants.USER_PASSWORD))
                .createdAt(userCreateDateTime)
                .lastCredentialsChangeTime(userCreateDateTime)
                .roles(Set.of(getUserRole()))
                .activated(activated)
                .banned(banned)
                .build());
    }

    private User persistFirstUser(boolean activated, boolean banned) {
        return persistUser(UserTestBuilder.firstUser(), persistCity(CitiesConstants.WARSAW_NAME), activated, banned);
    }

    private ActivationToken persistActivationToken(User user, UUID token, Instant expirationDate) {
        return activationTokenRepository.save(ActivationTokenTestBuilder.firstToken()
                .id(null)
                .user(user)
                .token(token)
                .expirationDate(expirationDate)
                .build());
    }

    private RefreshToken persistRefreshToken(User user, DeviceType deviceType, Instant createdAt, Instant expiryDate) {
        return refreshTokenRepository.save(RefreshTokenTestBuilder.firstRefreshTokenForUser(user)
                .id(null)
                .token(UUID.randomUUID().toString())
                .deviceType(deviceType)
                .createdAt(createdAt)
                .lastUsedAt(createdAt)
                .expiryDate(expiryDate)
                .build());
    }

    @Nested
    @DisplayName("Register user tests:")
    class RegisterUserTests {

        private RegisterRequest registerRequest;

        @BeforeEach
        void setUp() {

            registerRequest = RegisterRequestTestBuilder.firstUserRegisterRequest().build();

        }

        @Test
        @DisplayName("When registering user should throw UserAlreadyExistException if an account with the provided email already exists")
        public void whenRegisteringUserShouldThrowUserAlreadyExistExceptionIfAccountWithProvidedEmailsAlreadyExist() {
            persistFirstUser(false, false);

            assertThatThrownBy(() -> authenticationService.register(registerRequest))
                    .as("Expected to throw UserAlreadyExistException if there is account using provided by user email")
                    .isInstanceOf(UserAlreadyExistException.class);
        }
        @Test
        @DisplayName("When registering should detect duplicate email regardless of case")
        public void whenRegisteringShouldDetectDuplicateEmailRegardlessOfCase() {

            authenticationService.register(registerRequest);

            RegisterRequest duplicateRequest = RegisterRequestTestBuilder.firstUserRegisterRequest()
                    .email(UserConstants.FIRST_USER_EMAIL.toUpperCase())
                    .emailConfirmation(UserConstants.FIRST_USER_EMAIL.toUpperCase())
                    .firstName("Different")
                    .lastName("Person")
                    .homeCity(CitiesConstants.WARSAW_NAME)
                    .timeZone(UserConstants.FIRST_USER_TIMEZONE)
                    .build();

            assertThatThrownBy(() -> authenticationService.register(duplicateRequest))
                    .as("Should detect duplicate email regardless of case")
                    .isInstanceOf(UserAlreadyExistException.class);
        }

        @Test
        @DisplayName("When registering user should throw NotMatchingPasswordsException and not persist any data if provided passwords are not the same")
        public void whenRegisteringUserShouldThrowNotMatchingPasswordsExceptionAndNotPersistAnyDataIfProvidedPasswordsAreNotTheSame() {
            registerRequest.setPasswordConfirmation(UserConstants.WRONG_USER_PASSWORD);

            long userCountBefore = userRepository.count();
            long tokenCountBefore = activationTokenRepository.count();

            assertThatThrownBy(() -> authenticationService.register(registerRequest))
                    .isInstanceOf(NotMatchingPasswordsException.class);

            assertThat(userRepository.count()).as("No new users should be created").isEqualTo(userCountBefore);
            assertThat(activationTokenRepository.count()).as("No activation tokens should be created").isEqualTo(tokenCountBefore);
            assertThat(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL)).as("User with provided email should not exist").isEmpty();
        }

        @Test
        @DisplayName("When registering user should throw NotMatchingEmailsException and not persist any data if provided email addresses do not match")
        public void whenRegisteringUserShouldThrowNotMatchingEmailsExceptionAndNotPersistAnyDataIfProvidedEmailsAreNotTheSame() {
            registerRequest.setEmailConfirmation("different@email.com");

            long userCountBefore = userRepository.count();
            long tokenCountBefore = activationTokenRepository.count();

            assertThatThrownBy(() -> authenticationService.register(registerRequest))
                    .isInstanceOf(NotMatchingEmailsException.class);

            assertThat(userRepository.count()).as("No new users should be created").isEqualTo(userCountBefore);
            assertThat(activationTokenRepository.count()).as("No activation tokens should be created").isEqualTo(tokenCountBefore);
            assertThat(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL)).as("User with provided email should not exist").isEmpty();
        }

        @Test
        @DisplayName("When registering user should save user in database with correct data")
        public void whenRegisteringUserShouldSaveUserInDatabaseWithCorrectData(){
            authenticationService.register(registerRequest);

            User savedUser = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);

            assertThat(savedUser.getEmail()).isEqualTo(UserConstants.FIRST_USER_EMAIL.toLowerCase());
            assertThat(savedUser.getFirstName()).isEqualTo(UserConstants.FIRST_USER_FIRST_NAME);
            assertThat(savedUser.getLastName()).isEqualTo(UserConstants.FIRST_USER_LAST_NAME);
            assertThat(savedUser.getTimeZone()).isEqualTo(UserConstants.FIRST_USER_TIMEZONE);
            assertThat(passwordEncoder.matches(UserConstants.USER_PASSWORD, savedUser.getPassword())).isTrue();
            assertThat(savedUser.getHomeCity().getName()).isEqualTo(CitiesConstants.WARSAW_NAME.toLowerCase());

            assertThat(savedUser.getRoles()).hasSize(1);
            assertThat(savedUser.isActivated()).isFalse();
            assertThat(savedUser.isBanned()).isFalse();
            assertThat(savedUser.getRoles())
                    .extracting(Role::getName)
                    .contains(RoleConstants.ROLE_USER_NAME);
            assertThat(savedUser.getCreatedAt()).isBeforeOrEqualTo(Instant.now());
            assertThat(savedUser.getLastCredentialsChangeTime()).isEqualTo(savedUser.getCreatedAt());
        }

        @Test
        @DisplayName("When registering user should normalize user email to lower case")
        public void whenRegisteringUserShouldNormalizeUserEmailToLowerCase(){
            String providedEmailUpperCase = UserConstants.FIRST_USER_EMAIL.toUpperCase(Locale.ROOT);

            registerRequest.setEmail(providedEmailUpperCase);
            registerRequest.setEmailConfirmation(providedEmailUpperCase);

            authenticationService.register(registerRequest);

            User savedUser = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);

            assertThat(savedUser.getEmail()).isEqualTo(UserConstants.FIRST_USER_EMAIL);

        }

        @Test
        @DisplayName("When registering user should generate and save account activation token in database")
        public void whenRegisteringUserShouldGenerateAndSaveAccountActivationTokenInDatabase(){
            authenticationService.register(registerRequest);

            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);
            ActivationToken savedToken = activationTokenRepository.findByIgnoreCaseUserEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(ActivationTokenNotFoundException::new);

            assertThat(savedToken.getUser()).isEqualTo(user);
            assertThat(savedToken.getExpirationDate()).isAfter(Instant.now());

        }

    }

    @Nested
    @DisplayName("Activate account tests:")
    class ActivateAccountTests {
        private UUID token;

        @BeforeEach
        void setUp() {
            User user = persistFirstUser(false, false);
            token = UUID.randomUUID();
            persistActivationToken(user, token, Instant.now().plusMillis(activationTokenExpiration));
        }

        @Test
        @DisplayName("When activating account should activate user account and return ACTIVATED when token is valid")
        void whenActivatingAccountShouldActivateUserAccountAndReturnActivatedWhenTokenIsValid(){
            ActivationResult result = authenticationService.activateAccount(token);

            User activatedUser = userRepository.findByEmail(UserConstants.FIRST_USER_EMAIL)
                    .orElseThrow(UserNotFoundException::new);

            assertThat(result).as("Expected returned value ACTIVATED.").isEqualTo(ActivationResult.ACTIVATED);
            assertThat(activatedUser.isActivated()).as("Expected user account to be set as activated.").isTrue();
        }

        @Test
        @DisplayName("When activating account should delete activation token after successful activation")
        void whenActivatingAccountShouldDeleteActivationTokenAfterSuccessfulActivation(){
            authenticationService.activateAccount(token);

            assertThat(activationTokenRepository.findByToken(token)).isEmpty();
        }

        @Test
        @DisplayName("When activating account should regenerate token with future expiration if old token is expired")
        void whenActivatingAccountShouldRegenerateTokenWithFutureExpirationWhenOldTokenIsExpired(){
            expireToken(token);

            authenticationService.activateAccount(token);

            ActivationToken regeneratedToken = activationTokenRepository.findByIgnoreCaseUserEmail(UserConstants.FIRST_USER_EMAIL)
                    .orElseThrow(ActivationTokenNotFoundException::new);

            assertThat(regeneratedToken.isExpired()).as("Expected new token to not be already expired.").isFalse();
        }

        @Test
        @DisplayName("When activating account should not activate user when token is expired")
        void whenActivatingAccountShouldNotActivateUserWhenTokenIsExpired(){
            expireToken(token);

            authenticationService.activateAccount(token);

            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL)
                    .orElseThrow(UserNotFoundException::new);

            assertThat(user.isActivated()).as("Expected user account to not be activated.").isFalse();
        }

        @Test
        @DisplayName("When activating account should return TOKEN_EXPIRED_NEW_SENT when token is expired")
        void whenActivatingAccountShouldReturnTokenExpiredNewSentWhenTokenIsExpired(){
            expireToken(token);

            ActivationResult result = authenticationService.activateAccount(token);

            assertThat(result)
                    .as("Expected returned value TOKEN_EXPIRED_NEW_SENT if old token was expired.")
                    .isEqualTo(ActivationResult.TOKEN_EXPIRED_NEW_SENT);
        }

        @Test
        @DisplayName("When activating account should have exactly one token after replacing expired token")
        void whenActivatingAccountShouldHaveExactlyOneTokenAfterReplacingExpiredToken(){

            expireToken(token);

            authenticationService.activateAccount(token);

            long tokenCount = activationTokenRepository.findAll().stream()
                    .filter(token -> token.getUser().getEmail().equalsIgnoreCase(UserConstants.FIRST_USER_EMAIL))
                    .count();

            assertThat(tokenCount).isEqualTo(1);
        }

        @Test
        @DisplayName("When activating account should throw ActivationTokenNotFoundException when token does not exist")
        void whenActivatingAccountShouldThrowActivationTokenNotFoundExceptionWhenTokenDoesNotExist(){
            UUID nonExistentTokenId = UUID.randomUUID();

            assertThatThrownBy(() -> authenticationService.activateAccount(nonExistentTokenId))
                    .isInstanceOf(ActivationTokenNotFoundException.class);
        }
        @Test
        @DisplayName("When activating account should not activate user when token is not found")
        void whenActivatingAccountShouldNotActivateUserWhenTokenIsNotFound(){
            assertThatThrownBy(() -> authenticationService.activateAccount(UUID.randomUUID()))
                    .isInstanceOf(ActivationTokenNotFoundException.class);

            User unchangedUser = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL)
                    .orElseThrow(UserNotFoundException::new);

            assertThat(unchangedUser.isActivated()).as("Expected user to not be activated.").isFalse();
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
    class RegenerateActivationTokenByUserEmailTests {
        private Long activationTokenId;
        private UUID token;

        @BeforeEach
        void setUp() {
            User user = persistFirstUser(false, false);
            token = UUID.randomUUID();
            activationTokenId = persistActivationToken(user, token, Instant.now().plusMillis(activationTokenExpiration))
                    .getId();
        }

        @Test
        @DisplayName("When regenerating activation token should not proceed if account is already active")
        public void whenRegeneratingActivationTokenShouldNotProceedIfAccountIsAlreadyActive(){
            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);
            user.setActivated(true);
            userRepository.save(user);
            activationTokenRepository.deleteById(activationTokenId);

            assertThatThrownBy(() -> authenticationService.regenerateActivationTokenByUserEmail(UserConstants.FIRST_USER_EMAIL))
                    .as("Expected to throw AccountAlreadyActivatedException if account is already active.")
                    .isInstanceOf(AccountAlreadyActivatedException.class);
            assertThat(activationTokenRepository.findByIgnoreCaseUserEmail(UserConstants.FIRST_USER_EMAIL))
                    .as("Expected to not create any token for active account.")
                    .isEmpty();

        }

        @Test
        @DisplayName("When regenerating activation token should replace old activation token in database if old token is present")
        public void whenRegeneratingActivationTokenShouldRemoveOldTokenInDatabaseIfOldTokenIsPresent(){

            UUID oldToken = activationTokenRepository.findByToken(token).orElseThrow(ActivationTokenNotFoundException::new).getToken();

            authenticationService.regenerateActivationTokenByUserEmail(UserConstants.FIRST_USER_EMAIL);

            ActivationToken regeneratedToken = activationTokenRepository.findByIgnoreCaseUserEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(ActivationTokenNotFoundException::new);

            assertThat(activationTokenRepository.findByToken(token))
                    .as("Expected old token to replaced in database.")
                    .isEmpty();
            assertThat(regeneratedToken.getId())
                    .as("Expected to not change Activation Token id.")
                    .isEqualTo(activationTokenId);
            assertThat(regeneratedToken.getToken())
                    .as("Expected token value to be different after regeneration.")
                    .isNotEqualTo(oldToken);

        }

        @Test
        @DisplayName("When regenerating activation token should generate new token and expiration date and save it in database")
        public void whenRegeneratingActivationTokenShouldGenerateNewTokenAndExpirationDateAndSaveItInDatabase(){
            ActivationToken oldToken = activationTokenRepository.findByToken(token).orElseThrow(ActivationTokenNotFoundException::new);
            Instant oldExpirationDate = oldToken.getExpirationDate();

            authenticationService.regenerateActivationTokenByUserEmail(UserConstants.FIRST_USER_EMAIL);

            ActivationToken regeneratedToken = activationTokenRepository.findByIgnoreCaseUserEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(ActivationTokenNotFoundException::new);

            assertThat(regeneratedToken.getId())
                    .as("Expected new token to have the same id as old one.")
                    .isEqualTo(activationTokenId);
            assertThat(regeneratedToken.getExpirationDate())
                    .as("Expected new token expiration date to be in the future.")
                    .isAfter(Instant.now());
            assertThat(regeneratedToken.getExpirationDate())
                    .as("Expected expiration date to be updated.")
                    .isNotEqualTo(oldExpirationDate);
        }

        @Test
        @DisplayName("When regenerating activation token should create new token when old token does not exist")
        void whenRegeneratingActivationTokenShouldCreateNewTokenWhenOldTokenDoesNotExist(){
            activationTokenRepository.deleteById(activationTokenId);

            authenticationService.regenerateActivationTokenByUserEmail(UserConstants.FIRST_USER_EMAIL);

            ActivationToken newToken = activationTokenRepository.findByIgnoreCaseUserEmail(UserConstants.FIRST_USER_EMAIL)
                    .orElseThrow(ActivationTokenNotFoundException::new);

            assertThat(newToken.getId())
                    .as("Expected new token to not have the same id as old one.")
                    .isNotEqualTo(activationTokenId);
            assertThat(newToken.getToken())
                    .as("Expected token to be different than last one.")
                    .isNotEqualTo(token);
            assertThat(newToken.getExpirationDate())
                    .as("Expected new token expiration date to be in the future.")
                    .isAfter(Instant.now());
        }
    }

    @Nested
    @DisplayName("Authenticate user tests:")
    class AuthenticateUserTests {
        private AuthenticationRequest authenticationRequest;
        private DeviceType deviceType;

        @BeforeEach
        void setUp() {
            persistFirstUser(true, false);
            authenticationRequest = AuthenticationRequestTestBuilder.authenticationRequestForFirstUser().build();
            deviceType = DeviceType.WEB;
        }

        @Test
        @DisplayName("When authenticating user should throw BadCredentialsException if provided password is wrong")
        public void whenAuthenticatingUserShouldThrowSpringBadCredentialsExceptionIfProvidedPasswordIsWrong(){
            authenticationRequest.setPassword(UserConstants.WRONG_USER_PASSWORD);
            assertThatThrownBy(() -> authenticationService.authenticate(authenticationRequest, deviceType))
                    .as("Expected to throw BadCredentialsException if user provide wrong password.")
                    .isInstanceOf(BadCredentialsException.class);

        }

        @Test
        @DisplayName("When authenticating user should throw BadCredentialsException if provided email is not in the database")
        public void whenAuthenticatingUserShouldThrowSpringBadCredentialsExceptionIfProvidedEmailIsNotInDatabase(){
            authenticationRequest.setEmail("wrongEmail@example.com");
            assertThatThrownBy(() -> authenticationService.authenticate(authenticationRequest, deviceType))
                    .as("Expected to throw BadCredentialsException if user with given email does not exist.")
                    .isInstanceOf(BadCredentialsException.class);

        }

        @Test
        @DisplayName("When authenticating user should throw DisabledException if user account is not activated")
        public void whenAuthenticatingUserShouldThrowSpringDisabledExceptionIfUserAccountIsNotActivated(){
            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);
            user.setActivated(false);
            userRepository.saveAndFlush(user);

            assertThatThrownBy(() -> authenticationService.authenticate(authenticationRequest, deviceType))
                    .as("Expected to throw Spring DisabledException if user account is not activated.")
                    .isInstanceOf(DisabledException.class);
        }

        @Test
        @DisplayName("When authenticating user should throw LockedException if user is marked as banned")
        public void whenAuthenticatingUserShouldThrowSpringLockedExceptionIfUserIsMarkedAsBanned(){
            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);
            user.setBanned(true);
            userRepository.saveAndFlush(user);

            assertThatThrownBy(() -> authenticationService.authenticate(authenticationRequest, deviceType))
                    .as("Expected to throw Spring LockedException if user is banned.")
                    .isInstanceOf(LockedException.class);

        }

        @Test
        @DisplayName("When authenticating user should authenticate successfully if email is correct but not in lower case")
        public void whenAuthenticatingUserShouldNotThrowSpringBadCredentialsExceptionIfEmailIsCorrectButIsNotInLowerCase(){
            authenticationRequest.setEmail(UserConstants.FIRST_USER_EMAIL.toUpperCase());
            assertThatCode(() -> authenticationService.authenticate(authenticationRequest, deviceType))
                    .as("Expected to not throw BadCredentialsException if user do not provide email in lower case.")
                    .doesNotThrowAnyException();
        }


        @Test
        @DisplayName("When authenticating user should return AuthenticationResponse with access and refresh tokens present in it")
        public void whenAuthenticatingUserShouldReturnAuthenticationResponseWithAccessAndRefreshTokensPresentInIt(){
            AuthenticationResponse response = authenticationService.authenticate(authenticationRequest,deviceType);

            assertThat(refreshTokenRepository.findByToken(response.getRefreshToken()))
                    .as("Expected to create new refresh token in database")
                    .isPresent();

            assertThat(response.getAccessToken()).as("Expected returned access token to not be null.").isNotBlank();
            assertThat(response.getRefreshToken()).as("Expected returned refresh token to not be null.").isNotBlank();
            assertThat(response.getAccessTokenExpiration())
                    .as("Expected to set correct token expiration time")
                    .isEqualTo(jwtUtils.getAccessTokenExpiration());

        }

    }

    @Nested
    @DisplayName("Refresh token tests:")
    class RefreshTokenTests {
        private RefreshTokenRequest refreshTokenRequest;
        private DeviceType deviceType;

        @BeforeEach
        void setUp() {
            User user = persistFirstUser(true, false);
            deviceType = DeviceType.WEB;

            Instant refreshTokenCreateDateTime = Instant.now();
            RefreshToken refreshToken = persistRefreshToken(
                    user,
                    deviceType,
                    refreshTokenCreateDateTime,
                    refreshTokenCreateDateTime.plusMillis(JwtConstants.REFRESH_TOKEN_EXPIRATION_LONG)
            );

            refreshTokenRequest = RefreshTokenRequestTestBuilder.firstToken()
                    .refreshToken(refreshToken.getToken())
                    .build();
        }

        @Test
        @DisplayName("When refreshing token should throw UserBannedException if banned user tries to refresh token")
        public void whenRefreshingTokenShouldThrowUserBannedExceptionIfBannedUserTriesToRefreshToken(){
            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);
            user.setBanned(true);
            userRepository.saveAndFlush(user);

            assertThatThrownBy(() -> authenticationService.refreshAccessToken(refreshTokenRequest))
                    .isInstanceOf(UserBannedException.class);
        }

        @Test
        @DisplayName("When refreshing token should return valid access token")
        public void whenRefreshingTokenShouldReturn(){
            AuthenticationResponse authenticationResponse = authenticationService.refreshAccessToken(refreshTokenRequest);

            assertThat(jwtUtils.isTokenValid(authenticationResponse.getAccessToken()))
                    .as("Expected to return valid access token.")
                    .isTrue();
            assertThat(authenticationResponse.getAccessTokenExpiration())
                    .as("Expected to return correct value of access token expiration.")
                    .isEqualTo(jwtUtils.getAccessTokenExpiration());

        }

        @Test
        @DisplayName("When refreshing token should rotate refresh token with correct data")
        public void whenRefreshingTokenShouldRotateRefreshTokenIfWithCorrectData(){
            AuthenticationResponse authenticationResponse = authenticationService.refreshAccessToken(refreshTokenRequest);
            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);
            RefreshToken oldRefreshToken = refreshTokenRepository.findByToken(refreshTokenRequest.refreshToken())
                    .orElseThrow(RefreshTokenNotFoundException::new);

            assertThat(oldRefreshToken.isRevoked()).as("Expected old refresh token to be revoked").isTrue();
            assertThat(authenticationResponse.getRefreshToken())
                    .as("Expected to return new refresh token.")
                    .isNotEqualTo(oldRefreshToken.getToken());

            Optional<RefreshToken> refreshTokenOptional = refreshTokenRepository.findByToken(authenticationResponse.getRefreshToken());
            assertThat(refreshTokenOptional).as("Expected to create new refresh token").isPresent();
            RefreshToken refreshToken = refreshTokenOptional.get();
            assertThat(refreshToken.getUser()).as("Expected to set correct user.").isEqualTo(user);
            assertThat(refreshToken.isRevoked()).as("Expected new token to not be instantly revoked.").isFalse();
            assertThat(refreshToken.isExpired()).as("Expected new token to not be instantly expired.").isFalse();
            assertThat(refreshToken.getDeviceType())
                    .as("Expected to set the same device type as before token refreshing.")
                    .isEqualTo(oldRefreshToken.getDeviceType());
        }

        @Test
        @DisplayName("When refreshing token should not rotate refresh token if device type is not marked as rotational")
        public void whenRefreshingTokenShouldNotRotateRefreshTokenIfDeviceTypeIsNotMarkedAsRotational(){
            refreshTokenRepository.deleteAll();
            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);

            Instant refreshTokenCreateDateTime = Instant.now();
            RefreshToken mobileRefreshToken = persistRefreshToken(
                    user,
                    DeviceType.MOBILE_ANDROID,
                    refreshTokenCreateDateTime,
                    refreshTokenCreateDateTime.plusMillis(JwtConstants.REFRESH_TOKEN_EXPIRATION_LONG)
            );

            RefreshTokenRequest mobileRefreshTokenRequest = RefreshTokenRequestTestBuilder.firstToken()
                    .refreshToken(mobileRefreshToken.getToken())
                    .build();

            AuthenticationResponse authenticationResponse = authenticationService.refreshAccessToken(mobileRefreshTokenRequest);

            assertThat(authenticationResponse.getRefreshToken())
                    .as("Expected to not rotate refresh token.")
                    .isEqualTo(mobileRefreshToken.getToken());
            assertThat(refreshTokenRepository.findAll().stream().filter(refreshToken -> refreshToken.getUser().equals(user)).count())
                    .isEqualTo(1);
        }

    }

    @Nested
    @DisplayName("Logout tests:")
    class LogoutTests {
        private RefreshTokenRequest refreshTokenRequest;
        private DeviceType deviceType;

        @BeforeEach
        void setUp() {
            User user = persistFirstUser(true, false);
            deviceType = DeviceType.WEB;

            Instant refreshTokenCreateDateTime = Instant.now();
            RefreshToken refreshToken = persistRefreshToken(
                    user,
                    deviceType,
                    refreshTokenCreateDateTime,
                    refreshTokenCreateDateTime.plusMillis(JwtConstants.REFRESH_TOKEN_EXPIRATION_LONG)
            );

            refreshTokenRequest = RefreshTokenRequestTestBuilder.firstToken()
                    .refreshToken(refreshToken.getToken())
                    .build();
        }

        @Test
        @DisplayName("When logging out user should revoke provided token")
        public void whenLoggingOutUserShouldRevokeProvidedToken(){
            authenticationService.logout(refreshTokenRequest);

            RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenRequest.refreshToken()).orElseThrow(RefreshTokenNotFoundException::new);

            assertThat(refreshToken.isRevoked()).as("Expected to revoke provided token.").isTrue();
        }
    }

    @Nested
    @DisplayName("Get current user tests:")
    class GetCurrentUserTests {
        @BeforeEach
        void setUp() {
            persistFirstUser(true, false);
        }

        @Test
        @DisplayName("When getting current user should throw UserNotAuthenticatedException if authentication is not present in SecurityContext")
        public void whenGettingCurrentUserShouldThrowUserNotAuthenticatedExceptionIfAuthenticationIsNotPresentInSecurityContext(){
            SecurityContextHolder.clearContext();
            assertThatThrownBy(() -> authenticationService.getCurrentUser())
                    .isInstanceOf(UserNotAuthenticatedException.class);

        }

        @Test
        @DisplayName("When getting current user should return correct user")
        public void whenGettingCurrentUserShouldReturnCorrectUser(){
            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);

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

            assertThat(returnedUser).as("Expected to return correct user.").isEqualTo(user);

        }
        @Test
        @DisplayName("When getting current user should throw UserBannedException if user has a valid token but is banned")
        public void whenGettingCurrentUserShouldThrowUserBannedExceptionIfUserHaveValidTokenButIsBanned(){
            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);

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

            assertThatThrownBy(() -> authenticationService.getCurrentUser())
                    .as("Expected to throw UserBannedException if user is banned.")
                    .isInstanceOf(UserBannedException.class);

        }

    }


    @Nested
    @DisplayName("Get current user id tests:")
    class GetCurrentUserIdTests {

        @BeforeEach
        void setUp() {
            persistFirstUser(true, false);

            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("When getting current user id should throw UserNotAuthenticatedException if authentication is not present in SecurityContext")
        public void whenGettingCurrentUserIdShouldThrowUserNotAuthenticatedExceptionIfAuthenticationIsNotPresentInSecurityContext(){
            assertThatThrownBy(() -> authenticationService.getCurrentUserId())
                    .isInstanceOf(UserNotAuthenticatedException.class);
        }

        @Test
        @DisplayName("When getting current user id should return correct user id")
        public void whenGettingCurrentUserShouldReturnCorrectUser(){

            User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);

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

            assertThat(returnedUserId).as("Expected to return correct user id.").isEqualTo(user.getId());
        }
    }

}
