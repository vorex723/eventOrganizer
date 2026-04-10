package com.mazurek.eventOrganizer.auth;

import tools.jackson.databind.ObjectMapper;
import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.dto.*;
import com.mazurek.eventOrganizer.exception.auth.AccountAlreadyActivatedException;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenExpiredException;
import com.mazurek.eventOrganizer.exception.user.UserAlreadyExistException;
import com.mazurek.eventOrganizer.exception.user.UserBannedException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.jwt.RefreshTokenRepository;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.builders.AuthenticationRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.EmailBasedRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.RefreshTokenRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.RegisterRequestTestBuilder;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AuthenticationController integration tests:")
public class AuthenticationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ActivationTokenRepository activationTokenRepository;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private DeletionService deletionService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Value("${app.auth.activation-result-base-url}")
    private String activationResultBaseUrl;

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
    }

    private AuthenticationResponse loginAs(String email, String password) throws Exception {
        AuthenticationRequest request = new AuthenticationRequestTestBuilder()
                .email(email)
                .password(password)
                .build();
        MvcResult result = mockMvc.perform(post(ApiConstants.AUTH_LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthenticationResponse.class);
    }

    private AuthenticationResponse loginAs(String email, String password, String userAgent, String deviceTypeHeader) throws Exception {
        AuthenticationRequest request = new AuthenticationRequestTestBuilder()
                .email(email)
                .password(password)
                .build();
        var requestBuilder = post(ApiConstants.AUTH_LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request));

        if (userAgent != null)
            requestBuilder.header(DeviceConstants.USER_AGENT_HEADER, userAgent);
        if (deviceTypeHeader != null)
            requestBuilder.header(DeviceConstants.DEVICE_TYPE_HEADER, deviceTypeHeader);

        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthenticationResponse.class);
    }

    private RefreshTokenRequest refreshTokenRequest(String refreshToken) {
        return RefreshTokenRequestTestBuilder.firstToken()
                .refreshToken(refreshToken)
                .build();
    }

    private EmailBasedRequest emailBasedRequest(String email) {
        return EmailBasedRequestTestBuilder.firstUser()
                .email(email)
                .build();
    }

    private void banUser(String email) {
        var user = requirePresent(
                userRepository.findByIgnoreCaseEmail(email),
                "Expected user to exist before banning it for controller test");
        user.setBanned(true);
        userRepository.save(user);
    }

    private void expireRefreshToken(String token) {
        var refreshToken = requirePresent(
                refreshTokenRepository.findByToken(token),
                "Expected refresh token to exist before expiring it for controller test");
        refreshToken.setExpiryDate(TimeConstants.ONE_HOUR_AGO);
        refreshTokenRepository.save(refreshToken);
    }

    private String activationResultRedirect(String status) {
        return activationResultBaseUrl + "?status=" + status;
    }

    // ===========================================================================================
    // POST /api/v1/auth/register
    // ===========================================================================================

    @Nested
    @DisplayName("Register tests: POST /api/v1/auth/register")
    class RegisterTests {

        private RegisterRequest validRegisterRequest;

        @BeforeEach
        void setUp() {
            validRegisterRequest = RegisterRequestTestBuilder.thirdUserRegisterRequest().build();
        }

        @Test
        @DisplayName("When registering should return HTTP 201 Created on success")
        public void whenRegisteringShouldReturnCreatedOnSuccess() throws Exception {
            mockMvc.perform(post(ApiConstants.AUTH_REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(AuthConstants.REGISTRATION_VERIFICATION_REQUIRED_MESSAGE));
        }

        @Test
        @DisplayName("When registering should return HTTP 409 Conflict if email is already registered")
        public void whenRegisteringShouldReturnConflictIfEmailIsAlreadyRegistered() throws Exception {
            validRegisterRequest.setEmail(UserConstants.FIRST_USER_EMAIL);
            validRegisterRequest.setEmailConfirmation(UserConstants.FIRST_USER_EMAIL);

            mockMvc.perform(post(ApiConstants.AUTH_REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                    .andExpect(jsonPath("$.message").value(UserAlreadyExistException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When registering should return HTTP 400 Bad Request if passwords do not match")
        public void whenRegisteringShouldReturnBadRequestIfPasswordsDoNotMatch() throws Exception {
            validRegisterRequest.setPasswordConfirmation(InvalidInputConstants.DIFFERENT_PASSWORD);

            mockMvc.perform(post(ApiConstants.AUTH_REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("When registering should return HTTP 400 Bad Request if email addresses do not match")
        public void whenRegisteringShouldReturnBadRequestIfEmailsDoNotMatch() throws Exception {
            validRegisterRequest.setEmailConfirmation(InvalidInputConstants.DIFFERENT_EMAIL);

            mockMvc.perform(post(ApiConstants.AUTH_REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("When registering should return HTTP 400 Bad Request if request body is invalid")
        public void whenRegisteringShouldReturnBadRequestIfRequestBodyIsInvalid() throws Exception {
            validRegisterRequest.setFirstName(UserConstants.INVALID_FIRST_NAME);
            validRegisterRequest.setLastName(UserConstants.INVALID_LAST_NAME);
            validRegisterRequest.setEmail(InvalidInputConstants.INVALID_EMAIL);
            validRegisterRequest.setEmailConfirmation(InvalidInputConstants.BLANK_VALUE);
            validRegisterRequest.setTimeZone(InvalidInputConstants.BLANK_VALUE);
            validRegisterRequest.setPassword(InvalidInputConstants.WEAK_PASSWORD);
            validRegisterRequest.setPasswordConfirmation(InvalidInputConstants.WEAK_PASSWORD);

            mockMvc.perform(post(ApiConstants.AUTH_REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors").isMap())
                    .andExpect(jsonPath("$.errors.firstName").hasJsonPath())
                    .andExpect(jsonPath("$.errors.lastName").hasJsonPath())
                    .andExpect(jsonPath("$.errors.email").hasJsonPath())
                    .andExpect(jsonPath("$.errors.emailConfirmation").hasJsonPath())
                    .andExpect(jsonPath("$.errors.timeZone").hasJsonPath())
                    .andExpect(jsonPath("$.errors.password").hasJsonPath());
        }

        @Test
        @DisplayName("When registering should return HTTP 400 Bad Request if time zone is invalid")
        public void whenRegisteringShouldReturnBadRequestIfTimeZoneIsInvalid() throws Exception {
            validRegisterRequest.setTimeZone(InvalidInputConstants.INVALID_TIME_ZONE);

            mockMvc.perform(post(ApiConstants.AUTH_REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors").isMap())
                    .andExpect(jsonPath("$.errors.timeZone").hasJsonPath());
        }
    }

    // ===========================================================================================
    // POST /api/v1/auth/login
    // ===========================================================================================

    @Nested
    @DisplayName("Login tests: POST /api/v1/auth/login")
    class LoginTests {

        @Test
        @DisplayName("When logging in should return HTTP 200 OK with tokens on success")
        public void whenLoggingInShouldReturnOkWithTokensOnSuccess() throws Exception {
            AuthenticationRequest request = AuthenticationRequestTestBuilder.authenticationRequestForFirstUser().build();

            MvcResult result = mockMvc.perform(post(ApiConstants.AUTH_LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            AuthenticationResponse response = objectMapper.readValue(
                    result.getResponse().getContentAsString(), AuthenticationResponse.class);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getAccessToken())
                        .as("Access token should be present")
                        .isNotBlank();
                softly.assertThat(response.getRefreshToken())
                        .as("Refresh token should be present")
                        .isNotBlank();
                softly.assertThat(response.getAccessTokenExpiration())
                        .as("Access token expiration should be present")
                        .isNotNull();
            });
        }

        @Test
        @DisplayName("When logging in should return HTTP 401 Unauthorized if credentials are invalid")
        public void whenLoggingInShouldReturnUnauthorizedIfCredentialsAreInvalid() throws Exception {
            AuthenticationRequest request = new AuthenticationRequestTestBuilder()
                    .email(UserConstants.FIRST_USER_EMAIL)
                    .password(InvalidInputConstants.WRONG_LOGIN_PASSWORD)
                    .build();

            mockMvc.perform(post(ApiConstants.AUTH_LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("When logging in should return HTTP 400 Bad Request if request body is invalid")
        public void whenLoggingInShouldReturnBadRequestIfRequestBodyIsInvalid() throws Exception {
            AuthenticationRequest request = new AuthenticationRequestTestBuilder()
                    .email(InvalidInputConstants.INVALID_EMAIL)
                    .password(InvalidInputConstants.INVALID_SHORT_PASSWORD)
                    .build();

            mockMvc.perform(post(ApiConstants.AUTH_LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.email").hasJsonPath())
                    .andExpect(jsonPath("$.errors.password").hasJsonPath());
        }

        @Test
        @DisplayName("When logging in should return HTTP 400 Bad Request if email is blank")
        public void whenLoggingInShouldReturnBadRequestIfEmailIsBlank() throws Exception {
            AuthenticationRequest request = new AuthenticationRequestTestBuilder()
                    .email(InvalidInputConstants.BLANK_VALUE)
                    .password(UserConstants.USER_PASSWORD)
                    .build();

            mockMvc.perform(post(ApiConstants.AUTH_LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.email").hasJsonPath());
        }

        @Test
        @DisplayName("When logging in should return HTTP 403 Forbidden if account is not activated")
        public void whenLoggingInShouldReturnForbiddenIfAccountIsNotActivated() throws Exception {
            RegisterRequest request = RegisterRequestTestBuilder.thirdUserRegisterRequest().build();
            mockMvc.perform(post(ApiConstants.AUTH_REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            AuthenticationRequest loginRequest = new AuthenticationRequestTestBuilder()
                    .email(UserConstants.THIRD_USER_EMAIL)
                    .password(UserConstants.USER_PASSWORD)
                    .build();

            mockMvc.perform(post(ApiConstants.AUTH_LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When logging in should return HTTP 403 Forbidden if account is banned")
        public void whenLoggingInShouldReturnForbiddenIfAccountIsBanned() throws Exception {
            banUser(UserConstants.FIRST_USER_EMAIL);

            AuthenticationRequest request = AuthenticationRequestTestBuilder.authenticationRequestForFirstUser().build();

            mockMvc.perform(post(ApiConstants.AUTH_LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()));
        }

        @Test
        @DisplayName("When logging in with WEB device type header should rotate refresh token on refresh")
        public void whenLoggingInWithWebDeviceTypeHeaderShouldRotateRefreshTokenOnRefresh() throws Exception {
            AuthenticationResponse login = loginAs(
                    UserConstants.FIRST_USER_EMAIL,
                    UserConstants.USER_PASSWORD,
                    null,
                    DeviceType.WEB.name());

            MvcResult result = mockMvc.perform(post(ApiConstants.AUTH_REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshTokenRequest(login.getRefreshToken()))))
                    .andExpect(status().isOk())
                    .andReturn();

            AuthenticationResponse refreshed = objectMapper.readValue(
                    result.getResponse().getContentAsString(), AuthenticationResponse.class);

            assertThat(refreshed.getRefreshToken()).isNotEqualTo(login.getRefreshToken());
        }

        @Test
        @DisplayName("When logging in with MOBILE device type header should not rotate refresh token on refresh")
        public void whenLoggingInWithMobileDeviceTypeHeaderShouldNotRotateRefreshTokenOnRefresh() throws Exception {
            AuthenticationResponse login = loginAs(
                    UserConstants.FIRST_USER_EMAIL,
                    UserConstants.USER_PASSWORD,
                    null,
                    DeviceType.MOBILE_ANDROID.name());

            MvcResult result = mockMvc.perform(post(ApiConstants.AUTH_REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshTokenRequest(login.getRefreshToken()))))
                    .andExpect(status().isOk())
                    .andReturn();

            AuthenticationResponse refreshed = objectMapper.readValue(
                    result.getResponse().getContentAsString(), AuthenticationResponse.class);

            assertThat(refreshed.getRefreshToken()).isEqualTo(login.getRefreshToken());
        }

        @Test
        @DisplayName("When logging in with invalid device header should fallback to user-agent detection")
        public void whenLoggingInWithInvalidDeviceTypeHeaderShouldFallbackToUserAgentDetection() throws Exception {
            AuthenticationResponse login = loginAs(
                    UserConstants.FIRST_USER_EMAIL,
                    UserConstants.USER_PASSWORD,
                    DeviceConstants.USER_AGENT_IPHONE,
                    DeviceConstants.INVALID_DEVICE_TYPE_HEADER);

            MvcResult result = mockMvc.perform(post(ApiConstants.AUTH_REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshTokenRequest(login.getRefreshToken()))))
                    .andExpect(status().isOk())
                    .andReturn();

            AuthenticationResponse refreshed = objectMapper.readValue(
                    result.getResponse().getContentAsString(), AuthenticationResponse.class);

            assertThat(refreshed.getRefreshToken()).isEqualTo(login.getRefreshToken());
        }
    }


    // ===========================================================================================
    // POST /api/v1/auth/logout
    // ===========================================================================================

    @Nested
    @DisplayName("Logout tests: POST /api/v1/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("When logging out should return HTTP 204 No Content on success")
        public void whenLoggingOutShouldReturnNoContentOnSuccess() throws Exception {
            AuthenticationResponse tokens = loginAs(UserConstants.FIRST_USER_EMAIL, UserConstants.USER_PASSWORD);

            mockMvc.perform(post(ApiConstants.AUTH_LOGOUT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshTokenRequest(tokens.getRefreshToken()))))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));
        }

        @Test
        @DisplayName("When logging out should return HTTP 401 Unauthorized if refresh token is invalid")
        public void whenLoggingOutShouldReturnUnauthorizedIfRefreshTokenIsInvalid() throws Exception {
            mockMvc.perform(post(ApiConstants.AUTH_LOGOUT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    RefreshTokenRequestTestBuilder.firstToken()
                                            .refreshToken(RefreshTokenConstants.NOT_EXISTING_REFRESH_TOKEN)
                                            .build())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("When logging out should return HTTP 400 Bad Request if refresh token is blank")
        public void whenLoggingOutShouldReturnBadRequestIfRefreshTokenIsBlank() throws Exception {
            mockMvc.perform(post(ApiConstants.AUTH_LOGOUT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    RefreshTokenRequestTestBuilder.firstToken()
                                            .refreshToken(InvalidInputConstants.BLANK_VALUE)
                                            .build())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.refreshToken").hasJsonPath());
        }

        @Test
        @DisplayName("When logging out twice with same token should return HTTP 204 No Content both times")
        public void whenLoggingOutTwiceWithSameTokenShouldReturnNoContentBothTimes() throws Exception {
            AuthenticationResponse tokens = loginAs(UserConstants.FIRST_USER_EMAIL, UserConstants.USER_PASSWORD);

            mockMvc.perform(post(ApiConstants.AUTH_LOGOUT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshTokenRequest(tokens.getRefreshToken()))))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            mockMvc.perform(post(ApiConstants.AUTH_LOGOUT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshTokenRequest(tokens.getRefreshToken()))))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));
        }
    }

    // ===========================================================================================
    // POST /api/v1/auth/refresh
    // ===========================================================================================

    @Nested
    @DisplayName("Refresh token tests: POST /api/v1/auth/refresh")
    class RefreshTokenTests {

        @Test
        @DisplayName("When refreshing token should return HTTP 200 OK with new tokens on success")
        public void whenRefreshingTokenShouldReturnOkWithNewTokensOnSuccess() throws Exception {
            AuthenticationResponse tokens = loginAs(UserConstants.FIRST_USER_EMAIL, UserConstants.USER_PASSWORD);

            MvcResult result = mockMvc.perform(post(ApiConstants.AUTH_REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshTokenRequest(tokens.getRefreshToken()))))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            AuthenticationResponse refreshed = objectMapper.readValue(
                    result.getResponse().getContentAsString(), AuthenticationResponse.class);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(refreshed.getAccessToken())
                        .as("New access token should be present")
                        .isNotBlank();
                softly.assertThat(refreshed.getRefreshToken())
                        .as("Refresh token should be present")
                        .isNotBlank();
                softly.assertThat(refreshed.getAccessTokenExpiration())
                        .as("Access token expiration should be present")
                        .isNotNull();
            });
        }

        @Test
        @DisplayName("When refreshing token should return HTTP 401 Unauthorized if refresh token is invalid")
        public void whenRefreshingTokenShouldReturnUnauthorizedIfRefreshTokenIsInvalid() throws Exception {
            mockMvc.perform(post(ApiConstants.AUTH_REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    RefreshTokenRequestTestBuilder.firstToken()
                                            .refreshToken(RefreshTokenConstants.NOT_EXISTING_REFRESH_TOKEN)
                                            .build())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("When refreshing token should return HTTP 400 Bad Request if refresh token is blank")
        public void whenRefreshingTokenShouldReturnBadRequestIfRefreshTokenIsBlank() throws Exception {
            mockMvc.perform(post(ApiConstants.AUTH_REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    RefreshTokenRequestTestBuilder.firstToken()
                                            .refreshToken(InvalidInputConstants.BLANK_VALUE)
                                            .build())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.refreshToken").hasJsonPath());
        }

        @Test
        @DisplayName("When refreshing token should return HTTP 401 Unauthorized if refresh token has been revoked")
        public void whenRefreshingTokenShouldReturnUnauthorizedIfRefreshTokenHasBeenRevoked() throws Exception {
            AuthenticationResponse tokens = loginAs(UserConstants.FIRST_USER_EMAIL, UserConstants.USER_PASSWORD);

            mockMvc.perform(post(ApiConstants.AUTH_LOGOUT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshTokenRequest(tokens.getRefreshToken()))))
                    .andExpect(status().isNoContent());

            mockMvc.perform(post(ApiConstants.AUTH_REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshTokenRequest(tokens.getRefreshToken()))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("When refreshing token should return HTTP 401 Unauthorized if refresh token has expired")
        public void whenRefreshingTokenShouldReturnUnauthorizedIfRefreshTokenHasExpired() throws Exception {
            AuthenticationResponse tokens = loginAs(UserConstants.FIRST_USER_EMAIL, UserConstants.USER_PASSWORD);
            expireRefreshToken(tokens.getRefreshToken());

            mockMvc.perform(post(ApiConstants.AUTH_REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshTokenRequest(tokens.getRefreshToken()))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(HttpStatus.UNAUTHORIZED.value()))
                    .andExpect(jsonPath("$.message").value(RefreshTokenExpiredException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When refreshing token should return HTTP 403 Forbidden if user is banned")
        public void whenRefreshingTokenShouldReturnForbiddenIfUserIsBanned() throws Exception {
            AuthenticationResponse tokens = loginAs(UserConstants.FIRST_USER_EMAIL, UserConstants.USER_PASSWORD);
            banUser(UserConstants.FIRST_USER_EMAIL);

            mockMvc.perform(post(ApiConstants.AUTH_REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshTokenRequest(tokens.getRefreshToken()))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.message").value(UserBannedException.DEFAULT_MESSAGE));
        }
    }

    // ===========================================================================================
    // POST /api/v1/auth/activate  (resend activation email)
    // ===========================================================================================

    @Nested
    @DisplayName("Resend activation email tests: POST /api/v1/auth/activate")
    class ResendActivationEmailTests {

        @Test
        @DisplayName("When resending activation email should return HTTP 204 No Content on success")
        public void whenResendingActivationEmailShouldReturnNoContentOnSuccess() throws Exception {
            // Register a new user without activating them
            RegisterRequest request = RegisterRequestTestBuilder.thirdUserRegisterRequest().build();
            mockMvc.perform(post(ApiConstants.AUTH_REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post(ApiConstants.AUTH_ACTIVATE_RESEND_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(EmailBasedRequestTestBuilder.thirdUser().build())))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));
        }

        @Test
        @DisplayName("When resending activation email should return HTTP 404 Not Found if user does not exist")
        public void whenResendingActivationEmailShouldReturnNotFoundIfUserDoesNotExist() throws Exception {
            mockMvc.perform(post(ApiConstants.AUTH_ACTIVATE_RESEND_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(EmailBasedRequestTestBuilder.nonExistingUser().build())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(UserNotFoundException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When resending activation email should return HTTP 409 Conflict if account is already activated")
        public void whenResendingActivationEmailShouldReturnConflictIfAccountIsAlreadyActivated() throws Exception {
            mockMvc.perform(post(ApiConstants.AUTH_ACTIVATE_RESEND_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emailBasedRequest(UserConstants.FIRST_USER_EMAIL))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                    .andExpect(jsonPath("$.message").value(AccountAlreadyActivatedException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When resending activation email should return HTTP 400 Bad Request if email format is invalid")
        public void whenResendingActivationEmailShouldReturnBadRequestIfEmailFormatIsInvalid() throws Exception {
            mockMvc.perform(post(ApiConstants.AUTH_ACTIVATE_RESEND_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(EmailBasedRequestTestBuilder.invalidEmail().build())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("When resending activation email should return HTTP 400 Bad Request if email is blank")
        public void whenResendingActivationEmailShouldReturnBadRequestIfEmailIsBlank() throws Exception {
            mockMvc.perform(post(ApiConstants.AUTH_ACTIVATE_RESEND_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emailBasedRequest(InvalidInputConstants.BLANK_VALUE))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.email").hasJsonPath());
        }
    }

    // ===========================================================================================
    // GET /api/v1/auth/activate/{tokenId}
    // ===========================================================================================

    @Nested
    @DisplayName("Activate account tests: GET /api/v1/auth/activate/{tokenId}")
    class ActivateAccountTests {

        @Test
        @DisplayName("When activating account should redirect to activated result page on success")
        public void whenActivatingAccountShouldRedirectToActivatedResultPageOnSuccess() throws Exception {
            RegisterRequest request = RegisterRequestTestBuilder.thirdUserRegisterRequest().build();
            mockMvc.perform(post(ApiConstants.AUTH_REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            ActivationToken token = requirePresent(
                    activationTokenRepository.findByIgnoreCaseUserEmail(UserConstants.THIRD_USER_EMAIL),
                    "Expected activation token for third user after registration");

            mockMvc.perform(get(ApiConstants.AUTH_ACTIVATE_URL, token.getToken()))
                    .andExpect(status().isSeeOther())
                    .andExpect(redirectedUrl(activationResultRedirect("activated")));
        }

        @Test
        @DisplayName("When activating account should redirect to invalid token result page if token does not exist")
        public void whenActivatingAccountShouldRedirectToInvalidTokenResultPageIfTokenDoesNotExist() throws Exception {
            mockMvc.perform(get(ApiConstants.AUTH_ACTIVATE_URL, UUID.randomUUID()))
                    .andExpect(status().isSeeOther())
                    .andExpect(redirectedUrl(activationResultRedirect("invalid_token")));
        }

        @Test
        @DisplayName("When activating account with expired token should redirect to expired result page and send new activation email")
        public void whenActivatingAccountWithExpiredTokenShouldRedirectToExpiredResultPageAndSendNewActivationEmail() throws Exception {
            RegisterRequest request = RegisterRequestTestBuilder.thirdUserRegisterRequest().build();
            mockMvc.perform(post(ApiConstants.AUTH_REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            ActivationToken token = requirePresent(
                    activationTokenRepository.findByIgnoreCaseUserEmail(UserConstants.THIRD_USER_EMAIL),
                    "Expected activation token for third user after registration");

            // Force token expiration
            token.setExpirationDate(TimeConstants.ONE_HOUR_AGO);
            activationTokenRepository.save(token);

            // Expired token — service regenerates and redirects to the expired result page.
            mockMvc.perform(get(ApiConstants.AUTH_ACTIVATE_URL, token.getToken()))
                    .andExpect(status().isSeeOther())
                    .andExpect(redirectedUrl(activationResultRedirect("expired_resent")));

            // A new token should now exist for this user
            assertThat(activationTokenRepository.findByIgnoreCaseUserEmail(UserConstants.THIRD_USER_EMAIL))
                    .as("A new activation token should have been generated after expiry")
                    .isPresent();
        }
    }
}
