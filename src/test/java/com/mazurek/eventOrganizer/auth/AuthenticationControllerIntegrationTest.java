package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.testData.builders.ResetPasswordRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.NotificationDeviceTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ChangeUserEmailDtoTestBuilder;

import tools.jackson.databind.ObjectMapper;
import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.dto.*;
import com.mazurek.eventOrganizer.auth.email.AuthEmailDelivery;
import com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryRepository;
import com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryStatus;
import com.mazurek.eventOrganizer.auth.email.AuthEmailType;
import com.mazurek.eventOrganizer.exception.ApiErrorCode;
import com.mazurek.eventOrganizer.exception.auth.PasswordResetTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenExpiredException;
import com.mazurek.eventOrganizer.exception.user.UserAlreadyExistException;
import com.mazurek.eventOrganizer.exception.user.UserBannedException;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.jwt.RefreshTokenRepository;
import com.mazurek.eventOrganizer.notification.domain.DevicePlatform;
import com.mazurek.eventOrganizer.notification.domain.NotificationDevice;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeviceRepository;
import com.mazurek.eventOrganizer.notification.service.RecordingEmailService;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestPersistenceQueries;
import com.mazurek.eventOrganizer.testData.TestConstants.AuthConstants;
import com.mazurek.eventOrganizer.testData.builders.AuthenticationRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.RefreshTokenTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.EmailBasedRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.RefreshTokenRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.RegisterRequestTestBuilder;
import com.mazurek.eventOrganizer.user.dto.ChangeUserEmailDto;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import com.mazurek.eventOrganizer.testSupport.concurrency.TestWorkers;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest(properties = "app.auth.email.worker-enabled=false")
@AutoConfigureMockMvc
@DisplayName("AuthenticationController integration tests:")
public class AuthenticationControllerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private TestPersistenceQueries testPersistenceQueries;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ActivationTokenRepository activationTokenRepository;
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired
    private EmailChangeTokenRepository emailChangeTokenRepository;
    @Autowired
    private AuthEmailDeliveryRepository authEmailDeliveryRepository;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private DeletionService deletionService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private NotificationDeviceRepository notificationDeviceRepository;
    @Autowired
    private RecordingEmailService emailService;
    @Autowired
    private TransactionTemplate transactionTemplate;

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
                testPersistenceQueries.findRefreshTokenByHash(RefreshTokenTestBuilder.hashOf(token)),
                "Expected refresh token to exist before expiring it for controller test");
        refreshToken.setExpiryDate(TimeConstants.ONE_HOUR_AGO);
        refreshTokenRepository.save(refreshToken);
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
        @DisplayName("When registering with a long TLD should return HTTP 201 Created")
        public void whenRegisteringWithLongTldShouldReturnCreated() throws Exception {
            validRegisterRequest.setEmail("third.user@example.technology");
            validRegisterRequest.setEmailConfirmation("third.user@example.technology");

            mockMvc.perform(post(ApiConstants.AUTH_REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("When registering should return HTTP 409 Conflict if email is already registered")
        public void whenRegisteringShouldReturnConflictIfEmailIsAlreadyRegistered() throws Exception {
            validRegisterRequest.setEmail(UserConstants.FIRST_USER_EMAIL);
            validRegisterRequest.setEmailConfirmation(UserConstants.FIRST_USER_EMAIL);

            var beforeWrite = registrationWriteState();

            mockMvc.perform(post(ApiConstants.AUTH_REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.EMAIL_ALREADY_EXISTS))
                    .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                    .andExpect(jsonPath("$.message").value(UserAlreadyExistException.DEFAULT_MESSAGE));

            assertThat(registrationWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        void concurrentRegistrationsForSameEmailReturnCreatedAndConflict() throws Exception {
            String requestBody = objectMapper.writeValueAsString(validRegisterRequest);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            ExecutorService executor = TestWorkers.newFixedThreadPool(2);
            try {
                var registration = (java.util.concurrent.Callable<MvcResult>) () -> {
                    ready.countDown();
                    assertThat(start.await(10, TimeUnit.SECONDS))
                            .as("Registration workers received the start signal").isTrue();
                    return mockMvc.perform(post(ApiConstants.AUTH_REGISTER_URL)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody))
                            .andReturn();
                };
                Future<MvcResult> first = executor.submit(registration);
                Future<MvcResult> second = executor.submit(registration);
                assertThat(ready.await(10, TimeUnit.SECONDS))
                        .as("Registration workers reached the start barrier").isTrue();
                start.countDown();

                List<MvcResult> responses = List.of(first.get(10, TimeUnit.SECONDS),
                        second.get(10, TimeUnit.SECONDS));
                assertThat(responses.stream().map(result -> result.getResponse().getStatus()).toList())
                        .containsExactlyInAnyOrder(HttpStatus.CREATED.value(), HttpStatus.CONFLICT.value());
                MvcResult conflict = requirePresent(responses.stream()
                        .filter(result -> result.getResponse().getStatus() == HttpStatus.CONFLICT.value())
                        .findFirst(), "Expected matching persisted record in concurrentRegistrationsForSameEmailReturnCreatedAndConflict");
                assertThat(objectMapper.readTree(conflict.getResponse().getContentAsByteArray())
                        .get("code").asString()).isEqualTo(ApiErrorCode.EMAIL_ALREADY_EXISTS);
                assertThat(userRepository.count()).isEqualTo(3);
                assertThat(activationTokenRepository.count()).isEqualTo(1);
                assertThat(testPersistenceQueries.findActivationTokenByUserEmail(validRegisterRequest.getEmail()))
                        .isPresent();
            } finally {
                start.countDown();
                TestWorkers.stop(executor);
            }
        }

        @Test
        @DisplayName("When registering should return HTTP 400 Bad Request if passwords do not match")
        public void whenRegisteringShouldReturnBadRequestIfPasswordsDoNotMatch() throws Exception {
            validRegisterRequest.setPasswordConfirmation(InvalidInputConstants.DIFFERENT_PASSWORD);

            var beforeWrite = registrationWriteState();

            mockMvc.perform(post(ApiConstants.AUTH_REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.PASSWORD_CONFIRMATION_MISMATCH));

            assertThat(registrationWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When registering should return HTTP 400 Bad Request if email addresses do not match")
        public void whenRegisteringShouldReturnBadRequestIfEmailsDoNotMatch() throws Exception {
            validRegisterRequest.setEmailConfirmation(InvalidInputConstants.DIFFERENT_EMAIL);

            var beforeWrite = registrationWriteState();

            mockMvc.perform(post(ApiConstants.AUTH_REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.EMAIL_CONFIRMATION_MISMATCH));

            assertThat(registrationWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When registering should return HTTP 400 Bad Request if request body is invalid")
        public void whenRegisteringShouldReturnBadRequestIfRequestBodyIsInvalid() throws Exception {
            validRegisterRequest.setFirstName(UserConstants.INVALID_FIRST_NAME);
            validRegisterRequest.setLastName(UserConstants.INVALID_LAST_NAME);
            validRegisterRequest.setEmail(InvalidInputConstants.INVALID_EMAIL);
            validRegisterRequest.setEmailConfirmation(InvalidInputConstants.BLANK_VALUE);
            validRegisterRequest.setPassword(InvalidInputConstants.WEAK_PASSWORD);
            validRegisterRequest.setPasswordConfirmation(InvalidInputConstants.WEAK_PASSWORD);

            var beforeWrite = registrationWriteState();

            mockMvc.perform(post(ApiConstants.AUTH_REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_FAILED))
                    .andExpect(jsonPath("$.errors").isMap())
                    .andExpect(jsonPath("$.errors.firstName").hasJsonPath())
                    .andExpect(jsonPath("$.errors.lastName").hasJsonPath())
                    .andExpect(jsonPath("$.errors.email").hasJsonPath())
                    .andExpect(jsonPath("$.errors.emailConfirmation").hasJsonPath())
                    .andExpect(jsonPath("$.errors.password").hasJsonPath());

            assertThat(registrationWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
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
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.INVALID_CREDENTIALS));
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
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_FAILED))
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
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_FAILED))
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
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.ACCOUNT_DISABLED));
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
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.ACCOUNT_LOCKED))
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

        private NotificationDevice persistDevice(UUID userId, String installationId) {
            return notificationDeviceRepository.saveAndFlush(new NotificationDeviceTestBuilder().id(null)
                    .userId(userId)
                    .platform(DevicePlatform.ANDROID)
                    .firebaseInstallationId(installationId)
                    .createdAt(TimeConstants.NOW)
                    .lastSeenAt(TimeConstants.NOW)
                    .build());
        }

        private void logout(String rawToken, String installationId) throws Exception {
            mockMvc.perform(post(ApiConstants.AUTH_LOGOUT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RefreshTokenRequestTestBuilder()
                                    .refreshToken(rawToken)
                                    .firebaseInstallationId(installationId)
                                    .build())))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));
        }

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
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.REFRESH_TOKEN_NOT_FOUND));
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
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_FAILED))
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

        @Test
        @DisplayName("When logging out with an installation id should delete only that user's matching device")
        public void whenLoggingOutWithInstallationIdShouldDeleteOnlyMatchingOwnedDevice() throws Exception {
            User firstUser = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL), "Expected persisted user in whenLoggingOutWithInstallationIdShouldDeleteOnlyMatchingOwnedDevice");
            User secondUser = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL), "Expected persisted user in whenLoggingOutWithInstallationIdShouldDeleteOnlyMatchingOwnedDevice");
            NotificationDevice target = persistDevice(firstUser.getId(), "logout-target");
            NotificationDevice otherOwned = persistDevice(firstUser.getId(), "logout-other-owned");
            NotificationDevice otherUsers = persistDevice(secondUser.getId(), "logout-other-user");
            String rawToken = loginAs(UserConstants.FIRST_USER_EMAIL, UserConstants.USER_PASSWORD).getRefreshToken();

            logout(rawToken, target.getFirebaseInstallationId());

            assertThat(notificationDeviceRepository.findById(target.getId())).isEmpty();
            assertThat(notificationDeviceRepository.findById(otherOwned.getId())).isPresent();
            assertThat(notificationDeviceRepository.findById(otherUsers.getId())).isPresent();
            assertThat(requirePresent(testPersistenceQueries.findRefreshTokenByHash(RefreshTokenTestBuilder.hashOf(rawToken)), "Expected refresh token in whenLoggingOutWithInstallationIdShouldDeleteOnlyMatchingOwnedDevice").isRevoked()).isTrue();

            logout(rawToken, target.getFirebaseInstallationId());
        }

        @Test
        @DisplayName("When logging out with another user's installation id should leave that device intact")
        public void whenLoggingOutWithAnotherUsersInstallationIdShouldNotDeleteIt() throws Exception {
            User firstUser = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL), "Expected persisted user in whenLoggingOutWithAnotherUsersInstallationIdShouldNotDeleteIt");
            User secondUser = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL), "Expected persisted user in whenLoggingOutWithAnotherUsersInstallationIdShouldNotDeleteIt");
            NotificationDevice ownDevice = persistDevice(firstUser.getId(), "logout-own-device");
            NotificationDevice otherUsers = persistDevice(secondUser.getId(), "logout-other-users-device");
            String rawToken = loginAs(UserConstants.FIRST_USER_EMAIL, UserConstants.USER_PASSWORD).getRefreshToken();

            logout(rawToken, otherUsers.getFirebaseInstallationId());

            assertThat(notificationDeviceRepository.findById(ownDevice.getId())).isPresent();
            assertThat(notificationDeviceRepository.findById(otherUsers.getId())).isPresent();
        }

        @Test
        @DisplayName("When logging out without an installation id should leave devices intact")
        public void whenLoggingOutWithoutInstallationIdShouldNotDeleteDevices() throws Exception {
            User firstUser = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL), "Expected persisted user in whenLoggingOutWithoutInstallationIdShouldNotDeleteDevices");
            NotificationDevice device = persistDevice(firstUser.getId(), "logout-unchanged-device");
            String rawToken = loginAs(UserConstants.FIRST_USER_EMAIL, UserConstants.USER_PASSWORD).getRefreshToken();

            logout(rawToken, null);

            assertThat(notificationDeviceRepository.findById(device.getId())).isPresent();
        }

        @Test
        @DisplayName("When logging out with an unknown token should not delete the supplied installation")
        public void whenLoggingOutWithUnknownTokenShouldNotDeleteDevice() throws Exception {
            User firstUser = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL), "Expected persisted user in whenLoggingOutWithUnknownTokenShouldNotDeleteDevice");
            NotificationDevice device = persistDevice(firstUser.getId(), "logout-protected-device");

            mockMvc.perform(post(ApiConstants.AUTH_LOGOUT_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RefreshTokenRequestTestBuilder()
                                    .refreshToken(RefreshTokenConstants.NOT_EXISTING_REFRESH_TOKEN)
                                    .firebaseInstallationId(device.getFirebaseInstallationId())
                                    .build())))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.REFRESH_TOKEN_NOT_FOUND));

            assertThat(notificationDeviceRepository.findById(device.getId())).isPresent();
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
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.REFRESH_TOKEN_NOT_FOUND));
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
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_FAILED))
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
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.REFRESH_TOKEN_REVOKED));
        }

        @Test
        @DisplayName("When a rotated web token is reused should reject it and persist revocation of its family")
        public void whenRotatedWebTokenIsReusedShouldPersistFamilyRevocation() throws Exception {
            AuthenticationResponse original = loginAs(
                    UserConstants.FIRST_USER_EMAIL, UserConstants.USER_PASSWORD, null, DeviceType.WEB.name());
            AuthenticationResponse unrelated = loginAs(
                    UserConstants.FIRST_USER_EMAIL, UserConstants.USER_PASSWORD, null, DeviceType.WEB.name());

            MvcResult rotation = mockMvc.perform(post(ApiConstants.AUTH_REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshTokenRequest(original.getRefreshToken()))))
                    .andExpect(status().isOk())
                    .andReturn();
            AuthenticationResponse successor = objectMapper.readValue(
                    rotation.getResponse().getContentAsString(), AuthenticationResponse.class);

            var originalRow = requirePresent(testPersistenceQueries.findRefreshTokenByHash(
                    RefreshTokenTestBuilder.hashOf(original.getRefreshToken())), "Expected refresh token in whenRotatedWebTokenIsReusedShouldPersistFamilyRevocation");
            var successorRow = requirePresent(testPersistenceQueries.findRefreshTokenByHash(
                    RefreshTokenTestBuilder.hashOf(successor.getRefreshToken())), "Expected refresh token in whenRotatedWebTokenIsReusedShouldPersistFamilyRevocation");
            var unrelatedRow = requirePresent(testPersistenceQueries.findRefreshTokenByHash(
                    RefreshTokenTestBuilder.hashOf(unrelated.getRefreshToken())), "Expected refresh token in whenRotatedWebTokenIsReusedShouldPersistFamilyRevocation");
            assertThat(originalRow.isRevoked()).isTrue();
            assertThat(successorRow.isRevoked()).isFalse();
            assertThat(successorRow.getFamilyId()).isEqualTo(originalRow.getFamilyId());
            assertThat(unrelatedRow.getFamilyId()).isNotEqualTo(originalRow.getFamilyId());

            mockMvc.perform(post(ApiConstants.AUTH_REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshTokenRequest(original.getRefreshToken()))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.REFRESH_TOKEN_REVOKED));

            assertThat(requirePresent(testPersistenceQueries.findRefreshTokenByHash(
                    RefreshTokenTestBuilder.hashOf(successor.getRefreshToken())), "Expected refresh token in whenRotatedWebTokenIsReusedShouldPersistFamilyRevocation").isRevoked()).isTrue();
            assertThat(requirePresent(testPersistenceQueries.findRefreshTokenByHash(
                    RefreshTokenTestBuilder.hashOf(unrelated.getRefreshToken())), "Expected refresh token in whenRotatedWebTokenIsReusedShouldPersistFamilyRevocation").isRevoked()).isFalse();

            mockMvc.perform(post(ApiConstants.AUTH_REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshTokenRequest(successor.getRefreshToken()))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.REFRESH_TOKEN_REVOKED));
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
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.REFRESH_TOKEN_EXPIRED))
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
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.USER_BANNED))
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
        @DisplayName("When resending activation email for unknown user should return HTTP 204")
        public void whenResendingActivationEmailForUnknownUserShouldReturnNoContent() throws Exception {
            mockMvc.perform(post(ApiConstants.AUTH_ACTIVATE_RESEND_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(EmailBasedRequestTestBuilder.nonExistingUser().build())))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("When resending activation email for active account should return HTTP 204")
        public void whenResendingActivationEmailForActiveAccountShouldReturnNoContent() throws Exception {
            mockMvc.perform(post(ApiConstants.AUTH_ACTIVATE_RESEND_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emailBasedRequest(UserConstants.FIRST_USER_EMAIL))))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("When resending activation email should return HTTP 400 Bad Request if email format is invalid")
    public void whenResendingActivationEmailShouldReturnBadRequestIfEmailFormatIsInvalid() throws Exception {
            mockMvc.perform(post(ApiConstants.AUTH_ACTIVATE_RESEND_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(EmailBasedRequestTestBuilder.invalidEmail().build())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_FAILED))
                    .andExpect(jsonPath("$.errors.email").exists());
        }

        @Test
        @DisplayName("When resending activation email should return HTTP 400 Bad Request if email is blank")
        public void whenResendingActivationEmailShouldReturnBadRequestIfEmailIsBlank() throws Exception {
            mockMvc.perform(post(ApiConstants.AUTH_ACTIVATE_RESEND_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emailBasedRequest(InvalidInputConstants.BLANK_VALUE))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_FAILED))
                    .andExpect(jsonPath("$.errors.email").hasJsonPath());
        }
    }

    // ===========================================================================================
    // POST /api/v1/auth/password-reset
    // ===========================================================================================

    @Nested
    @DisplayName("Password reset tests")
    class PasswordResetTests {

        @Test
        @DisplayName("When requesting reset for an unknown email should return HTTP 204")
        void requestForUnknownEmailDoesNotDiscloseAccountState() throws Exception {
            mockMvc.perform(post("/api/v1/auth/password-reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    EmailBasedRequestTestBuilder.thirdUser().build()
                            )))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));
        }

        @Test
        @DisplayName("When resetting password should revoke existing refresh tokens")
        void resetPasswordChangesCredentialsAndRevokesExistingRefreshTokens() throws Exception {
            AuthenticationResponse tokens = loginAs(
                    UserConstants.FIRST_USER_EMAIL,
                    UserConstants.USER_PASSWORD
            );
            EmailBasedRequest request = EmailBasedRequestTestBuilder.firstUser().build();
            mockMvc.perform(post("/api/v1/auth/password-reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            UUID resetToken = emailService.lastPasswordResetToken(UserConstants.FIRST_USER_EMAIL);
            assertThat(resetToken).isNotNull();

            mockMvc.perform(post("/api/v1/auth/password-reset/{tokenId}", resetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ResetPasswordRequestTestBuilder()
                                    .password(UserConstants.NEW_PASSWORD)
                                    .passwordConfirmation(UserConstants.NEW_PASSWORD)
                                    .build())))
                    .andExpect(status().isNoContent());

            UUID userId = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL), "Expected persisted user in resetPasswordChangesCredentialsAndRevokesExistingRefreshTokens").getId();
            assertThat(testPersistenceQueries.findPasswordResetTokenByUserId(userId)).isEmpty();
            mockMvc.perform(post(ApiConstants.AUTH_REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshTokenRequest(tokens.getRefreshToken()))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.REFRESH_TOKEN_REVOKED));
            loginAs(UserConstants.FIRST_USER_EMAIL, UserConstants.NEW_PASSWORD);
        }

        @Test
        @DisplayName("When resetting password with an unknown token should return HTTP 400")
        void resetPasswordWithUnknownTokenReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/auth/password-reset/{tokenId}", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ResetPasswordRequestTestBuilder()
                                    .password(UserConstants.NEW_PASSWORD)
                                    .passwordConfirmation(UserConstants.NEW_PASSWORD)
                                    .build())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.PASSWORD_RESET_TOKEN_INVALID))
                    .andExpect(jsonPath("$.message")
                            .value(PasswordResetTokenNotFoundException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When resetting password with an expired token should return HTTP 400")
        void resetPasswordWithExpiredTokenReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/auth/password-reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    EmailBasedRequestTestBuilder.firstUser().build()
                            )))
                    .andExpect(status().isNoContent());

            UUID resetToken = emailService.lastPasswordResetToken(UserConstants.FIRST_USER_EMAIL);
            UUID userId = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL), "Expected persisted user in resetPasswordWithExpiredTokenReturnsBadRequest").getId();
            PasswordResetToken persistedToken = requirePresent(testPersistenceQueries.findPasswordResetTokenByUserId(userId), "Expected password reset token in resetPasswordWithExpiredTokenReturnsBadRequest");
            persistedToken.setExpirationDate(TimeConstants.ONE_HOUR_AGO);
            passwordResetTokenRepository.saveAndFlush(persistedToken);

            mockMvc.perform(post("/api/v1/auth/password-reset/{tokenId}", resetToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ResetPasswordRequestTestBuilder()
                                    .password(UserConstants.NEW_PASSWORD)
                                    .passwordConfirmation(UserConstants.NEW_PASSWORD)
                                    .build())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.PASSWORD_RESET_TOKEN_INVALID))
                    .andExpect(jsonPath("$.message")
                            .value(PasswordResetTokenNotFoundException.DEFAULT_MESSAGE));
        }
    }

    // ===========================================================================================
    // Explicit token confirmation endpoints
    // ===========================================================================================

    @Nested
    @DisplayName("Email change confirmation tests: PUT /api/v1/users/change-email and POST /api/v1/auth/change-email/{tokenId}")
    class EmailChangeConfirmationTests {

        @ParameterizedTest
        @CsvSource({"GET, false", "HEAD, false", "GET, true", "HEAD, true"})
        void safeMethodsCannotConfirmOrCleanUpEmailChange(String method, boolean expired) throws Exception {
            AuthenticationResponse session = loginAs(UserConstants.FIRST_USER_EMAIL, UserConstants.USER_PASSWORD);
            ChangeUserEmailDto request = new ChangeUserEmailDtoTestBuilder()
                    .newEmail(UserConstants.FIRST_USER_NEW_EMAIL)
                    .newEmailConfirmation(UserConstants.FIRST_USER_NEW_EMAIL)
                    .password(UserConstants.USER_PASSWORD)
                    .build();
            mockMvc.perform(put(ApiConstants.USER_CHANGE_EMAIL_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, AuthConstants.JWT_PREFIX + session.getAccessToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted());
            User before = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL), "Expected persisted user in safeMethodsCannotConfirmOrCleanUpEmailChange");
            UUID rawToken = emailService.lastEmailChangeToken(UserConstants.FIRST_USER_NEW_EMAIL);
            EmailChangeToken token = requirePresent(testPersistenceQueries.findEmailChangeTokenByUserId(before.getId()), "Expected email change token in safeMethodsCannotConfirmOrCleanUpEmailChange");
            if (expired) {
                token.setExpirationDate(TimeConstants.ONE_HOUR_AGO);
                emailChangeTokenRepository.saveAndFlush(token);
            }
            List<AuthEmailDelivery> deliveriesBefore = authEmailDeliveryRepository.findAll();
            long refreshCount = refreshTokenRepository.count();

            mockMvc.perform("GET".equals(method)
                            ? get("/api/v1/auth/change-email/{tokenId}", rawToken)
                            : head("/api/v1/auth/change-email/{tokenId}", rawToken))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(header().string("Allow", "POST"))
                    .andExpect(header().doesNotExist("Location"));

            User after = requirePresent(userRepository.findById(before.getId()), "Expected persisted user in safeMethodsCannotConfirmOrCleanUpEmailChange");
            assertThat(after.getEmail()).isEqualTo(before.getEmail());
            assertThat(after.getSecurityVersion()).isEqualTo(before.getSecurityVersion());
            assertThat(after.getLastCredentialsChangeTime()).isEqualTo(before.getLastCredentialsChangeTime());
            EmailChangeToken unchanged = requirePresent(testPersistenceQueries.findEmailChangeTokenByUserId(before.getId()), "Expected email change token in safeMethodsCannotConfirmOrCleanUpEmailChange");
            assertThat(unchanged.getTokenHash()).isEqualTo(token.getTokenHash());
            assertThat(unchanged.getExpirationDate()).isEqualTo(token.getExpirationDate());
            assertThat(refreshTokenRepository.count()).isEqualTo(refreshCount);
            assertThat(requirePresent(testPersistenceQueries.findRefreshTokenByHash(RefreshTokenTestBuilder.hashOf(session.getRefreshToken())), "Expected refresh token in safeMethodsCannotConfirmOrCleanUpEmailChange").isRevoked()).isFalse();
            assertThat(authEmailDeliveryRepository.findAll()).usingRecursiveComparison()
                    .ignoringCollectionOrder().isEqualTo(deliveriesBefore);
        }

        @Test
        void expiredConfirmationReturnsGoneAfterCommittingTokenCleanupWithoutRevokingSessions() throws Exception {
            AuthenticationResponse session = loginAs(UserConstants.FIRST_USER_EMAIL, UserConstants.USER_PASSWORD);
            ChangeUserEmailDto request = new ChangeUserEmailDtoTestBuilder()
                    .newEmail(UserConstants.FIRST_USER_NEW_EMAIL)
                    .newEmailConfirmation(UserConstants.FIRST_USER_NEW_EMAIL)
                    .password(UserConstants.USER_PASSWORD)
                    .build();
            mockMvc.perform(put(ApiConstants.USER_CHANGE_EMAIL_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, AuthConstants.JWT_PREFIX + session.getAccessToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted());
            User before = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL), "Expected persisted user in expiredConfirmationReturnsGoneAfterCommittingTokenCleanupWithoutRevokingSessions");
            UUID rawToken = emailService.lastEmailChangeToken(UserConstants.FIRST_USER_NEW_EMAIL);
            EmailChangeToken token = requirePresent(testPersistenceQueries.findEmailChangeTokenByUserId(before.getId()), "Expected email change token in expiredConfirmationReturnsGoneAfterCommittingTokenCleanupWithoutRevokingSessions");
            token.setExpirationDate(TimeConstants.ONE_HOUR_AGO);
            emailChangeTokenRepository.saveAndFlush(token);

            mockMvc.perform(post("/api/v1/auth/change-email/{tokenId}", rawToken))
                    .andExpect(status().isGone())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(410))
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.EMAIL_CHANGE_TOKEN_EXPIRED))
                    .andExpect(header().doesNotExist("Location"));

            assertThat(testPersistenceQueries.findEmailChangeTokenByUserId(before.getId())).isEmpty();
            User after = requirePresent(userRepository.findById(before.getId()), "Expected persisted user in expiredConfirmationReturnsGoneAfterCommittingTokenCleanupWithoutRevokingSessions");
            assertThat(after.getEmail()).isEqualTo(before.getEmail());
            assertThat(after.getSecurityVersion()).isEqualTo(before.getSecurityVersion());
            assertThat(after.getLastCredentialsChangeTime()).isEqualTo(before.getLastCredentialsChangeTime());
            assertThat(requirePresent(testPersistenceQueries.findRefreshTokenByHash(RefreshTokenTestBuilder.hashOf(session.getRefreshToken())), "Expected refresh token in expiredConfirmationReturnsGoneAfterCommittingTokenCleanupWithoutRevokingSessions").isRevoked()).isFalse();
            assertThat(authEmailDeliveryRepository.findAll())
                    .filteredOn(delivery -> delivery.getType() == AuthEmailType.EMAIL_CHANGE_CONFIRMATION)
                    .extracting(AuthEmailDelivery::getStatus)
                    .containsExactly(AuthEmailDeliveryStatus.CANCELLED);
            mockMvc.perform(post("/api/v1/auth/change-email/{tokenId}", rawToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.EMAIL_CHANGE_TOKEN_INVALID));
        }

        @Test
        void unknownConfirmationTokenReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/auth/change-email/{tokenId}", UUID.randomUUID()))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.EMAIL_CHANGE_TOKEN_INVALID))
                    .andExpect(header().doesNotExist("Location"));
        }

        @Test
        void confirmationChangesTheEmailRevokesSessionsAndCannotBeReplayed() throws Exception {
            AuthenticationResponse tokens = loginAs(UserConstants.FIRST_USER_EMAIL, UserConstants.USER_PASSWORD);
            ChangeUserEmailDto request = new ChangeUserEmailDtoTestBuilder()
                    .newEmail(UserConstants.FIRST_USER_NEW_EMAIL)
                    .newEmailConfirmation(UserConstants.FIRST_USER_NEW_EMAIL)
                    .password(UserConstants.USER_PASSWORD)
                    .build();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_EMAIL_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, AuthConstants.JWT_PREFIX + tokens.getAccessToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted());
            assertThat(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL)).isPresent();

            UUID confirmationToken = emailService.lastEmailChangeToken(UserConstants.FIRST_USER_NEW_EMAIL);
            assertThat(confirmationToken).isNotNull();

            mockMvc.perform(post("/api/v1/auth/change-email/{tokenId}", confirmationToken))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value("changed"))
                    .andExpect(header().doesNotExist("Location"));

            assertThat(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_NEW_EMAIL)).isPresent();
            assertThat(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL)).isEmpty();
            mockMvc.perform(post(ApiConstants.AUTH_REFRESH_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshTokenRequest(tokens.getRefreshToken()))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.REFRESH_TOKEN_REVOKED));

            mockMvc.perform(post("/api/v1/auth/change-email/{tokenId}", confirmationToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.EMAIL_CHANGE_TOKEN_INVALID))
                    .andExpect(header().doesNotExist("Location"));
        }

        @Test
        void confirmationReturnsConflictWhenAddressWasTakenAfterRequest() throws Exception {
            AuthenticationResponse tokens = loginAs(UserConstants.FIRST_USER_EMAIL, UserConstants.USER_PASSWORD);
            ChangeUserEmailDto changeRequest = new ChangeUserEmailDtoTestBuilder()
                    .newEmail(UserConstants.FIRST_USER_NEW_EMAIL)
                    .newEmailConfirmation(UserConstants.FIRST_USER_NEW_EMAIL)
                    .password(UserConstants.USER_PASSWORD)
                    .build();
            mockMvc.perform(put(ApiConstants.USER_CHANGE_EMAIL_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, AuthConstants.JWT_PREFIX + tokens.getAccessToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(changeRequest)))
                    .andExpect(status().isAccepted());
            UUID confirmationToken = emailService.lastEmailChangeToken(UserConstants.FIRST_USER_NEW_EMAIL);

            RegisterRequest registration = RegisterRequestTestBuilder.thirdUserRegisterRequest()
                    .email(UserConstants.FIRST_USER_NEW_EMAIL)
                    .emailConfirmation(UserConstants.FIRST_USER_NEW_EMAIL)
                    .build();
            mockMvc.perform(post(ApiConstants.AUTH_REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(registration)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/v1/auth/change-email/{tokenId}", confirmationToken))
                    .andExpect(status().isConflict())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.EMAIL_CHANGE_ADDRESS_UNAVAILABLE))
                    .andExpect(header().doesNotExist("Location"));
            assertThat(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL)).isPresent();
            assertThat(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_NEW_EMAIL)).isPresent();
        }

        @Test
        void confirmationReturnsConflictWhenConcurrentInsertWinsUniqueConstraint() throws Exception {
            AuthenticationResponse tokens = loginAs(UserConstants.FIRST_USER_EMAIL, UserConstants.USER_PASSWORD);
            ChangeUserEmailDto changeRequest = new ChangeUserEmailDtoTestBuilder()
                    .newEmail(UserConstants.FIRST_USER_NEW_EMAIL)
                    .newEmailConfirmation(UserConstants.FIRST_USER_NEW_EMAIL)
                    .password(UserConstants.USER_PASSWORD)
                    .build();
            mockMvc.perform(put(ApiConstants.USER_CHANGE_EMAIL_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, AuthConstants.JWT_PREFIX + tokens.getAccessToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(changeRequest)))
                    .andExpect(status().isAccepted());
            UUID confirmationToken = emailService.lastEmailChangeToken(UserConstants.FIRST_USER_NEW_EMAIL);
            User firstUser = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL), "Expected persisted user in confirmationReturnsConflictWhenConcurrentInsertWinsUniqueConstraint");
            long securityVersion = firstUser.getSecurityVersion();

            CountDownLatch ready = new CountDownLatch(1);
            CountDownLatch start = new CountDownLatch(1);
            ExecutorService executor = TestWorkers.newSingleThreadExecutor();
            try {
                Future<MvcResult> confirmation = executor.submit(() -> {
                    ready.countDown();
                    assertThat(start.await(10, TimeUnit.SECONDS))
                            .as("Email confirmation worker received the start signal").isTrue();
                    return mockMvc.perform(post("/api/v1/auth/change-email/{tokenId}", confirmationToken))
                            .andReturn();
                });
                assertThat(ready.await(10, TimeUnit.SECONDS))
                        .as("Email confirmation worker reached the start barrier").isTrue();

                transactionTemplate.executeWithoutResult(ignored -> {
                    userRepository.saveAndFlush(UserTestBuilder.firstUser().id(null)
                            .firstName("Concurrent")
                            .lastName("Registrant")
                            .email(UserConstants.FIRST_USER_NEW_EMAIL)
                            .password("unused")
                            .homeCity(firstUser.getHomeCity())
                            .timeZone(firstUser.getTimeZone())
                            .createdAt(TimeConstants.NOW)
                            .lastCredentialsChangeTime(TimeConstants.NOW)
                            .roles(Set.of())
                            .activated(false)
                            .banned(false)
                            .build());
                    start.countDown();
                    assertThatThrownBy(() -> confirmation.get(1, TimeUnit.SECONDS))
                            .isInstanceOf(TimeoutException.class);
                });

                MvcResult result = confirmation.get(10, TimeUnit.SECONDS);
                assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
                assertThat(objectMapper.readTree(result.getResponse().getContentAsString()).path("code").asString())
                        .isEqualTo(ApiErrorCode.EMAIL_CHANGE_ADDRESS_UNAVAILABLE);
                assertThat(result.getResponse().getHeader("Location")).isNull();
            } finally {
                start.countDown();
                TestWorkers.stop(executor);
            }

            User unchangedUser = requirePresent(userRepository.findById(firstUser.getId()), "Expected persisted user in confirmationReturnsConflictWhenConcurrentInsertWinsUniqueConstraint");
            assertThat(unchangedUser.getEmail()).isEqualTo(UserConstants.FIRST_USER_EMAIL);
            assertThat(unchangedUser.getSecurityVersion()).isEqualTo(securityVersion);
            assertThat(testPersistenceQueries.findEmailChangeTokenByUserId(firstUser.getId())).isPresent();
        }
    }

    @Nested
    @DisplayName("Activate account tests: POST /api/v1/auth/activate/{tokenId}")
    class ActivateAccountTests {

        @ParameterizedTest
        @CsvSource({"GET, false", "HEAD, false", "GET, true", "HEAD, true"})
        void safeMethodsCannotActivateOrRegenerateToken(String method, boolean expired) throws Exception {
            mockMvc.perform(post(ApiConstants.AUTH_REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(RegisterRequestTestBuilder.thirdUserRegisterRequest().build())))
                    .andExpect(status().isCreated());
            UUID rawToken = emailService.lastActivationToken(UserConstants.THIRD_USER_EMAIL);
            ActivationToken token = requirePresent(testPersistenceQueries.findActivationTokenByUserEmail(UserConstants.THIRD_USER_EMAIL), "Expected activation token in safeMethodsCannotActivateOrRegenerateToken");
            if (expired) {
                token.setExpirationDate(TimeConstants.ONE_HOUR_AGO);
                activationTokenRepository.saveAndFlush(token);
            }
            List<AuthEmailDelivery> deliveriesBefore = authEmailDeliveryRepository.findAll();
            long refreshCount = refreshTokenRepository.count();

            mockMvc.perform("GET".equals(method)
                            ? get(ApiConstants.AUTH_ACTIVATE_URL, rawToken)
                            : head(ApiConstants.AUTH_ACTIVATE_URL, rawToken))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(header().string("Allow", "POST"))
                    .andExpect(header().doesNotExist("Location"));

            assertThat(requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.THIRD_USER_EMAIL), "Expected persisted user in safeMethodsCannotActivateOrRegenerateToken").isActivated()).isFalse();
            ActivationToken unchanged = requirePresent(testPersistenceQueries.findActivationTokenByUserEmail(UserConstants.THIRD_USER_EMAIL), "Expected activation token in safeMethodsCannotActivateOrRegenerateToken");
            assertThat(unchanged.getTokenHash()).isEqualTo(token.getTokenHash());
            assertThat(unchanged.getExpirationDate()).isEqualTo(token.getExpirationDate());
            assertThat(emailService.lastActivationToken(UserConstants.THIRD_USER_EMAIL)).isEqualTo(rawToken);
            assertThat(refreshTokenRepository.count()).isEqualTo(refreshCount);
            assertThat(authEmailDeliveryRepository.findAll()).usingRecursiveComparison()
                    .ignoringCollectionOrder().isEqualTo(deliveriesBefore);
        }

        @Test
        @DisplayName("When activating account should return JSON on success")
        public void whenActivatingAccountShouldReturnJsonOnSuccess() throws Exception {
            RegisterRequest request = RegisterRequestTestBuilder.thirdUserRegisterRequest().build();
            mockMvc.perform(post(ApiConstants.AUTH_REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            UUID rawToken = emailService.lastActivationToken(UserConstants.THIRD_USER_EMAIL);
            mockMvc.perform(post(ApiConstants.AUTH_ACTIVATE_URL, rawToken))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value("activated"))
                    .andExpect(header().doesNotExist("Location"));

            assertThat(requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.THIRD_USER_EMAIL), "Expected persisted user in whenActivatingAccountShouldReturnJsonOnSuccess").isActivated()).isTrue();
            assertThat(testPersistenceQueries.findActivationTokenByUserEmail(UserConstants.THIRD_USER_EMAIL)).isEmpty();
            mockMvc.perform(post(ApiConstants.AUTH_ACTIVATE_URL, rawToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.ACTIVATION_TOKEN_INVALID));
        }

        @Test
        @DisplayName("When activating account should return bad request if token does not exist")
        public void whenActivatingAccountShouldReturnBadRequestIfTokenDoesNotExist() throws Exception {
            mockMvc.perform(post(ApiConstants.AUTH_ACTIVATE_URL, UUID.randomUUID()))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.ACTIVATION_TOKEN_INVALID))
                    .andExpect(header().doesNotExist("Location"));
        }

        @Test
        @DisplayName("When activating account with expired token should return expired_resent and queue new activation email")
        public void whenActivatingAccountWithExpiredTokenShouldReturnExpiredResentAndQueueNewEmail() throws Exception {
            RegisterRequest request = RegisterRequestTestBuilder.thirdUserRegisterRequest().build();
            mockMvc.perform(post(ApiConstants.AUTH_REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            ActivationToken token = requirePresent(
                    testPersistenceQueries.findActivationTokenByUserEmail(UserConstants.THIRD_USER_EMAIL),
                    "Expected activation token for third user after registration");

            // Force token expiration
            token.setExpirationDate(TimeConstants.ONE_HOUR_AGO);
            activationTokenRepository.save(token);

            // An explicit POST regenerates the expired token and queues a replacement email.
            mockMvc.perform(post(
                    ApiConstants.AUTH_ACTIVATE_URL,
                    emailService.lastActivationToken(UserConstants.THIRD_USER_EMAIL)
            ))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value("expired_resent"))
                    .andExpect(header().doesNotExist("Location"));

            // A new token should now exist for this user
            ActivationToken replacement = requirePresent(testPersistenceQueries.findActivationTokenByUserEmail(UserConstants.THIRD_USER_EMAIL), "Expected activation token in whenActivatingAccountWithExpiredTokenShouldReturnExpiredResentAndQueueNewEmail");
            assertThat(replacement.getTokenHash()).isNotEqualTo(token.getTokenHash());
            assertThat(replacement.getExpirationDate()).isAfter(TimeConstants.NOW);
            assertThat(requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.THIRD_USER_EMAIL), "Expected persisted user in whenActivatingAccountWithExpiredTokenShouldReturnExpiredResentAndQueueNewEmail").isActivated()).isFalse();
            assertThat(authEmailDeliveryRepository.findAll())
                    .filteredOn(delivery -> delivery.getType() == AuthEmailType.ACCOUNT_ACTIVATION
                            && delivery.getRecipientEmail().equals(UserConstants.THIRD_USER_EMAIL))
                    .extracting(AuthEmailDelivery::getStatus)
                    .containsExactlyInAnyOrder(AuthEmailDeliveryStatus.CANCELLED, AuthEmailDeliveryStatus.PENDING);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/auth/activate/not-a-uuid", "/api/v1/auth/change-email/not-a-uuid"})
    void confirmationRejectsMalformedTokenWithoutRedirecting(String path) throws Exception {
        mockMvc.perform(post(path))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(ApiErrorCode.MALFORMED_REQUEST))
                .andExpect(header().doesNotExist("Location"));
    }

    // Independent committed reads: no managed entity snapshot or test-level transaction.
    private Map<String, List<Map<String, Object>>> registrationWriteState() {
        return Map.of(
                "users", jdbcTemplate.queryForList("SELECT * FROM users ORDER BY id"),
                "activationTokens", jdbcTemplate.queryForList("SELECT * FROM activation_tokens ORDER BY id"),
                "emailOutbox", jdbcTemplate.queryForList("SELECT * FROM auth_email_deliveries ORDER BY id")
        );
    }
}
