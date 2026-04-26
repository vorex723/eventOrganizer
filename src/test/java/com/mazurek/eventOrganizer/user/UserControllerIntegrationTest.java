package com.mazurek.eventOrganizer.user;

import tools.jackson.databind.ObjectMapper;
import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationRequest;
import com.mazurek.eventOrganizer.event.EventRepository;
import com.mazurek.eventOrganizer.event.EventService;
import com.mazurek.eventOrganizer.exception.user.InvalidPasswordException;
import com.mazurek.eventOrganizer.exception.user.NotMatchingEmailsException;
import com.mazurek.eventOrganizer.exception.user.NotMatchingPasswordsException;
import com.mazurek.eventOrganizer.exception.user.UserAlreadyExistException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestDataInitializer;
import com.mazurek.eventOrganizer.testData.builders.AuthenticationRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ChangeUserDetailsDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ChangeUserEmailDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ChangeUserPasswordDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.RegisterFcmTokenRequestTestBuilder;
import com.mazurek.eventOrganizer.user.dto.ChangeUserDetailsDto;
import com.mazurek.eventOrganizer.user.dto.ChangeUserEmailDto;
import com.mazurek.eventOrganizer.user.dto.ChangeUserPasswordDto;
import com.mazurek.eventOrganizer.user.dto.RegisterFcmTokenRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("UserController integration tests:")
public class UserControllerIntegrationTest {

    private final AuthenticationRequest firstUserAuthRequest =
            AuthenticationRequestTestBuilder.authenticationRequestForFirstUser().build();
    private final AuthenticationRequest secondUserAuthRequest =
            AuthenticationRequestTestBuilder.authenticationRequestForSecondUser().build();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventService eventService;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private TestDataInitializer testDataInitializer;
    @Autowired
    private DeletionService deletionService;

    private UUID firstUserId;
    private UUID secondUserId;
    private String firstUserJwt;
    private String secondUserJwt;

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();

        firstUserId = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL)
                .orElseThrow(UserNotFoundException::new)
                .getId();
        secondUserId = userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL)
                .orElseThrow(UserNotFoundException::new)
                .getId();

        firstUserJwt = AuthConstants.JWT_PREFIX +
                authenticationService.authenticate(firstUserAuthRequest, DeviceType.WEB).getAccessToken();
        secondUserJwt = AuthConstants.JWT_PREFIX +
                authenticationService.authenticate(secondUserAuthRequest, DeviceType.WEB).getAccessToken();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
    }

    @Nested
    @DisplayName("Get user by id tests: GET /api/v1/users/{id}")
    class GetUserByIdTests {

        @Test
        @DisplayName("When getting user by id should return HTTP 403 Forbidden if authorization header is missing")
        public void whenGettingUserByIdShouldReturnForbiddenIfAuthorizationHeaderIsMissing() throws Exception {
            mockMvc.perform(get(ApiConstants.USER_BY_ID_URL, firstUserId))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When getting user by id should return HTTP 404 Not Found if user does not exist")
        public void whenGettingUserByIdShouldReturnNotFoundIfUserDoesNotExist() throws Exception {
            mockMvc.perform(get(ApiConstants.USER_BY_ID_URL, UserConstants.NOT_EXISTING_USER_ID)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()));
        }

        @Test
        @DisplayName("When getting user by id should return HTTP 200 OK with profile dto and correct fields")
        public void whenGettingUserByIdShouldReturnProfileDtoWithCorrectFields() throws Exception {
            mockMvc.perform(get(ApiConstants.USER_BY_ID_URL, firstUserId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(firstUserId.toString()))
                    .andExpect(jsonPath("$.firstName").value(UserConstants.FIRST_USER_FIRST_NAME))
                    .andExpect(jsonPath("$.lastName").value(UserConstants.FIRST_USER_LAST_NAME))
                    .andExpect(jsonPath("$.homeCity").value(CitiesConstants.WARSAW_NAME));
        }
    }

    @Nested
    @DisplayName("Get user events tests: GET /api/v1/users/{id}/events")
    class GetUserEventsTests {

        @Test
        @DisplayName("When getting user events should return HTTP 404 Not Found if user does not exist")
        public void whenGettingUserEventsShouldReturnNotFoundIfUserDoesNotExist() throws Exception {
            mockMvc.perform(get(ApiConstants.USER_EVENTS_URL, UserConstants.NOT_EXISTING_USER_ID)
                            .param("page", String.valueOf(PaginationConstants.PAGE_ZERO))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(UserNotFoundException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When getting user events should return HTTP 400 Bad Request if page is negative")
        public void whenGettingUserEventsShouldReturnBadRequestIfPageIsNegative() throws Exception {
            mockMvc.perform(get(ApiConstants.USER_EVENTS_URL, firstUserId)
                            .param("page", String.valueOf(PaginationConstants.PAGE_MINUS_ONE))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()));
        }

        @Test
        @DisplayName("When getting user events should return HTTP 200 OK and respect upcoming query parameter")
        public void whenGettingUserEventsShouldRespectUpcomingQueryParameter() throws Exception {
            UUID upcomingEventId = testDataInitializer.setupFirstEvent();
            UUID pastEventId = testDataInitializer.setupEventByFirstUser();

            var pastEvent = eventRepository.findById(pastEventId).orElseThrow();
            pastEvent.setEventStartDate(TimeConstants.ONE_WEEK_AGO);
            eventRepository.save(pastEvent);

            mockMvc.perform(get(ApiConstants.USER_EVENTS_URL, firstUserId)
                            .param("page", String.valueOf(PaginationConstants.PAGE_ZERO))
                            .param("upcoming", String.valueOf(true))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events.length()").value(1))
                    .andExpect(jsonPath("$.events[0].id").value(upcomingEventId.toString()));

            mockMvc.perform(get(ApiConstants.USER_EVENTS_URL, firstUserId)
                            .param("page", String.valueOf(PaginationConstants.PAGE_ZERO))
                            .param("upcoming", String.valueOf(false))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events.length()").value(2))
                    .andExpect(jsonPath("$.events[*].id").value(org.hamcrest.Matchers.hasItems(
                            upcomingEventId.toString(),
                            pastEventId.toString())));
        }
    }

    @Nested
    @DisplayName("Get current user attending events tests: GET /api/v1/users/me/attending-events")
    class GetUserAttendingEventsTests {

        @Test
        @DisplayName("When getting current user attending events should return HTTP 403 Forbidden if authorization header is missing")
        public void whenGettingCurrentUserAttendingEventsShouldReturnForbiddenIfAuthorizationHeaderIsMissing() throws Exception {
            mockMvc.perform(get(ApiConstants.USER_ATTENDING_EVENTS_URL)
                            .param("page", String.valueOf(PaginationConstants.PAGE_ZERO)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When getting current user attending events should return HTTP 400 Bad Request if page is negative")
        public void whenGettingCurrentUserAttendingEventsShouldReturnBadRequestIfPageIsNegative() throws Exception {
            mockMvc.perform(get(ApiConstants.USER_ATTENDING_EVENTS_URL)
                            .param("page", String.valueOf(PaginationConstants.PAGE_MINUS_ONE))
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()));
        }

        @Test
        @DisplayName("When getting current user attending events should return HTTP 200 OK and respect upcoming query parameter")
        public void whenGettingCurrentUserAttendingEventsShouldRespectUpcomingQueryParameter() throws Exception {
            UUID upcomingEventId = testDataInitializer.setupFirstEvent();
            UUID pastEventId = testDataInitializer.setupEventByFirstUser();

            authHelper.setupSecurityContextForSecondUser();
            eventService.addAttenderToEvent(upcomingEventId);
            eventService.addAttenderToEvent(pastEventId);
            SecurityContextHolder.clearContext();

            var pastEvent = eventRepository.findById(pastEventId).orElseThrow();
            pastEvent.setEventStartDate(TimeConstants.ONE_WEEK_AGO);
            eventRepository.save(pastEvent);

            mockMvc.perform(get(ApiConstants.USER_ATTENDING_EVENTS_URL)
                            .param("page", String.valueOf(PaginationConstants.PAGE_ZERO))
                            .param("upcoming", String.valueOf(true))
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events.length()").value(1))
                    .andExpect(jsonPath("$.events[0].id").value(upcomingEventId.toString()));

            mockMvc.perform(get(ApiConstants.USER_ATTENDING_EVENTS_URL)
                            .param("page", String.valueOf(PaginationConstants.PAGE_ZERO))
                            .param("upcoming", String.valueOf(false))
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events.length()").value(2))
                    .andExpect(jsonPath("$.events[*].id").value(org.hamcrest.Matchers.hasItems(
                            upcomingEventId.toString(),
                            pastEventId.toString())));
        }
    }

    @Nested
    @DisplayName("Register token tests: POST /api/v1/users/register-token")
    class RegisterTokenTests {

        @Test
        @DisplayName("When registering fcm token should return HTTP 403 Forbidden if authorization header is missing")
        public void whenRegisteringFcmTokenShouldReturnForbiddenIfAuthorizationHeaderIsMissing() throws Exception {
            RegisterFcmTokenRequest request = RegisterFcmTokenRequestTestBuilder.updatedFirstUserToken().build();

            mockMvc.perform(post(ApiConstants.USER_REGISTER_FCM_TOKEN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When registering fcm token should return HTTP 400 Bad Request if token is blank")
        public void whenRegisteringFcmTokenShouldReturnBadRequestIfTokenIsBlank() throws Exception {
            RegisterFcmTokenRequest request = RegisterFcmTokenRequestTestBuilder.updatedFirstUserToken()
                    .token(InvalidInputConstants.BLANK_VALUE)
                    .build();

            mockMvc.perform(post(ApiConstants.USER_REGISTER_FCM_TOKEN_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.token").exists());
        }

        @Test
        @DisplayName("When registering fcm token should return HTTP 200 OK and persist token")
        public void whenRegisteringFcmTokenShouldPersistTokenAndReturnResultTrue() throws Exception {
            RegisterFcmTokenRequest request = RegisterFcmTokenRequestTestBuilder.updatedFirstUserToken().build();

            mockMvc.perform(post(ApiConstants.USER_REGISTER_FCM_TOKEN_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").value("true"));

            User updatedUser = userRepository.findById(firstUserId).orElseThrow();
            assertThat(updatedUser.getFcmAndroidToken()).isEqualTo(UserConstants.FIRST_USER_NEW_FCM_TOKEN);
        }
    }

    @Nested
    @DisplayName("Update user details tests: PUT /api/v1/users/update")
    class UpdateUserDetailsTests {

        @Test
        @DisplayName("When updating user details should return HTTP 403 Forbidden if authorization header is missing")
        public void whenUpdatingUserDetailsShouldReturnForbiddenIfAuthorizationHeaderIsMissing() throws Exception {
            ChangeUserDetailsDto request = ChangeUserDetailsDtoTestBuilder.validUpdate().build();

            mockMvc.perform(put(ApiConstants.USER_UPDATE_DETAILS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When updating user details should return HTTP 400 Bad Request on validation errors")
        public void whenUpdatingUserDetailsShouldReturnBadRequestOnValidationErrors() throws Exception {
            ChangeUserDetailsDto request = ChangeUserDetailsDtoTestBuilder.validUpdate()
                    .firstName(UserConstants.INVALID_FIRST_NAME)
                    .lastName(UserConstants.INVALID_LAST_NAME)
                    .homeCity(UserConstants.INVALID_CITY_NAME)
                    .build();

            mockMvc.perform(put(ApiConstants.USER_UPDATE_DETAILS_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.firstName").exists())
                    .andExpect(jsonPath("$.errors.lastName").exists())
                    .andExpect(jsonPath("$.errors.homeCity").exists());
        }

        @Test
        @DisplayName("When updating user details should return HTTP 200 OK and updated profile")
        public void whenUpdatingUserDetailsShouldPersistChangesAndReturnUpdatedProfile() throws Exception {
            ChangeUserDetailsDto request = ChangeUserDetailsDtoTestBuilder.validUpdate().build();

            mockMvc.perform(put(ApiConstants.USER_UPDATE_DETAILS_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value(UserConstants.SECOND_USER_FIRST_NAME))
                    .andExpect(jsonPath("$.lastName").value(UserConstants.SECOND_USER_LAST_NAME))
                    .andExpect(jsonPath("$.homeCity").value(CitiesConstants.KRAKOW_NAME));
        }
    }

    @Nested
    @DisplayName("Change password tests: PUT /api/v1/users/change-password")
    class ChangePasswordTests {

        @Test
        @DisplayName("When changing password should return HTTP 403 Forbidden if authorization header is missing")
        public void whenChangingPasswordShouldReturnForbiddenIfAuthorizationHeaderIsMissing() throws Exception {
            ChangeUserPasswordDto request = ChangeUserPasswordDtoTestBuilder.validChange().build();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_PASSWORD_URL)
                            .header(DeviceConstants.DEVICE_TYPE_HEADER, DeviceType.WEB.name())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When changing password should return HTTP 400 Bad Request if request body is invalid")
        public void whenChangingPasswordShouldReturnBadRequestIfRequestBodyIsInvalid() throws Exception {
            ChangeUserPasswordDto request = ChangeUserPasswordDtoTestBuilder.validChange()
                    .newPassword(InvalidInputConstants.WEAK_PASSWORD)
                    .newPasswordConfirmation(InvalidInputConstants.WEAK_PASSWORD)
                    .build();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_PASSWORD_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .header(DeviceConstants.DEVICE_TYPE_HEADER, DeviceType.WEB.name())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.newPassword").exists())
                    .andExpect(jsonPath("$.errors.newPasswordConfirmation").exists());
        }

        @Test
        @DisplayName("When changing password should return HTTP 400 Bad Request if old password is invalid")
        public void whenChangingPasswordShouldReturnBadRequestIfOldPasswordIsInvalid() throws Exception {
            ChangeUserPasswordDto request = ChangeUserPasswordDtoTestBuilder.validChange()
                    .password(UserConstants.WRONG_USER_PASSWORD)
                    .build();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_PASSWORD_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .header(DeviceConstants.DEVICE_TYPE_HEADER, DeviceType.WEB.name())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("When changing password should return HTTP 400 Bad Request if new password confirmation does not match")
        public void whenChangingPasswordShouldReturnBadRequestIfNewPasswordConfirmationDoesNotMatch() throws Exception {
            ChangeUserPasswordDto request = ChangeUserPasswordDtoTestBuilder.validChange()
                    .newPasswordConfirmation(InvalidInputConstants.DIFFERENT_PASSWORD)
                    .build();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_PASSWORD_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .header(DeviceConstants.DEVICE_TYPE_HEADER, DeviceType.WEB.name())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.message").value(NotMatchingPasswordsException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When changing password should return HTTP 200 OK with new authentication tokens")
        public void whenChangingPasswordShouldReturnNewAuthenticationTokens() throws Exception {
            ChangeUserPasswordDto request = ChangeUserPasswordDtoTestBuilder.validChange().build();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_PASSWORD_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .header(DeviceConstants.DEVICE_TYPE_HEADER, DeviceType.WEB.name())
                            .header(DeviceConstants.USER_AGENT_HEADER, DeviceConstants.USER_AGENT_DESKTOP_WINDOWS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.accessTokenExpiration").isNumber());
        }
    }

    @Nested
    @DisplayName("Change email tests: PUT /api/v1/users/change-email")
    class ChangeEmailTests {

        @Test
        @DisplayName("When changing email should return HTTP 403 Forbidden if authorization header is missing")
        public void whenChangingEmailShouldReturnForbiddenIfAuthorizationHeaderIsMissing() throws Exception {
            ChangeUserEmailDto request = ChangeUserEmailDtoTestBuilder.validChange().build();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_EMAIL_URL)
                            .header(DeviceConstants.DEVICE_TYPE_HEADER, DeviceType.WEB.name())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When changing email should return HTTP 400 Bad Request if request body is invalid")
        public void whenChangingEmailShouldReturnBadRequestIfRequestBodyIsInvalid() throws Exception {
            ChangeUserEmailDto request = ChangeUserEmailDtoTestBuilder.validChange()
                    .newEmail(InvalidInputConstants.INVALID_EMAIL)
                    .newEmailConfirmation(InvalidInputConstants.INVALID_EMAIL)
                    .password(InvalidInputConstants.WEAK_PASSWORD)
                    .build();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_EMAIL_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .header(DeviceConstants.DEVICE_TYPE_HEADER, DeviceType.WEB.name())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.newEmail").exists())
                    .andExpect(jsonPath("$.errors.newEmailConfirmation").exists())
                    .andExpect(jsonPath("$.errors.password").exists());
        }

        @Test
        @DisplayName("When changing email should return HTTP 400 Bad Request if new email is blank")
        public void whenChangingEmailShouldReturnBadRequestIfNewEmailIsBlank() throws Exception {
            ChangeUserEmailDto request = ChangeUserEmailDtoTestBuilder.validChange()
                    .newEmail(InvalidInputConstants.BLANK_VALUE)
                    .newEmailConfirmation(InvalidInputConstants.BLANK_VALUE)
                    .build();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_EMAIL_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .header(DeviceConstants.DEVICE_TYPE_HEADER, DeviceType.WEB.name())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.newEmail").exists())
                    .andExpect(jsonPath("$.errors.newEmailConfirmation").exists());
        }

        @Test
        @DisplayName("When changing email should return HTTP 400 Bad Request if password is invalid")
        public void whenChangingEmailShouldReturnBadRequestIfPasswordIsInvalid() throws Exception {
            ChangeUserEmailDto request = ChangeUserEmailDtoTestBuilder.validChange()
                    .password(UserConstants.WRONG_USER_PASSWORD)
                    .build();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_EMAIL_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .header(DeviceConstants.DEVICE_TYPE_HEADER, DeviceType.WEB.name())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.message").value(InvalidPasswordException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When changing email should return HTTP 400 Bad Request if email confirmation does not match")
        public void whenChangingEmailShouldReturnBadRequestIfEmailConfirmationDoesNotMatch() throws Exception {
            ChangeUserEmailDto request = ChangeUserEmailDtoTestBuilder.validChange()
                    .newEmailConfirmation(InvalidInputConstants.DIFFERENT_EMAIL)
                    .build();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_EMAIL_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .header(DeviceConstants.DEVICE_TYPE_HEADER, DeviceType.WEB.name())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.message").value(NotMatchingEmailsException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When changing email should return HTTP 409 Conflict if new email is already used by another account")
        public void whenChangingEmailShouldReturnConflictIfNewEmailIsAlreadyUsedByAnotherAccount() throws Exception {
            ChangeUserEmailDto request = ChangeUserEmailDtoTestBuilder.validChange()
                    .newEmail(UserConstants.SECOND_USER_EMAIL)
                    .newEmailConfirmation(UserConstants.SECOND_USER_EMAIL)
                    .build();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_EMAIL_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .header(DeviceConstants.DEVICE_TYPE_HEADER, DeviceType.WEB.name())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                    .andExpect(jsonPath("$.message").value(UserAlreadyExistException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When changing email should return HTTP 409 Conflict if new email matches current email")
        public void whenChangingEmailShouldReturnConflictForSameEmail() throws Exception {
            ChangeUserEmailDto request = ChangeUserEmailDtoTestBuilder.validChange()
                    .newEmail(UserConstants.FIRST_USER_EMAIL)
                    .newEmailConfirmation(UserConstants.FIRST_USER_EMAIL)
                    .build();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_EMAIL_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .header(DeviceConstants.DEVICE_TYPE_HEADER, DeviceType.WEB.name())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("When changing email should return HTTP 200 OK with normalized email and new tokens")
        public void whenChangingEmailShouldPersistNormalizedEmailAndReturnNewTokens() throws Exception {
            ChangeUserEmailDto request = ChangeUserEmailDtoTestBuilder.validChange()
                    .newEmail(UserConstants.FIRST_USER_NEW_EMAIL.toUpperCase())
                    .newEmailConfirmation(UserConstants.FIRST_USER_NEW_EMAIL.toUpperCase())
                    .build();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_EMAIL_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .header(DeviceConstants.DEVICE_TYPE_HEADER, DeviceType.WEB.name())
                            .header(DeviceConstants.USER_AGENT_HEADER, DeviceConstants.USER_AGENT_DESKTOP_WINDOWS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.accessTokenExpiration").isNumber());

            User updatedUser = userRepository.findById(firstUserId).orElseThrow();
            assertThat(updatedUser.getEmail()).isEqualTo(UserConstants.FIRST_USER_NEW_EMAIL);
        }
    }

}
