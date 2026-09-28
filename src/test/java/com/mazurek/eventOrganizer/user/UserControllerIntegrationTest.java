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
import com.mazurek.eventOrganizer.user.dto.ChangeUserDetailsDto;
import com.mazurek.eventOrganizer.user.dto.ChangeUserEmailDto;
import com.mazurek.eventOrganizer.user.dto.ChangeUserPasswordDto;
import com.mazurek.eventOrganizer.user.dto.DeleteCurrentUserDto;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
                    .andExpect(status().isUnauthorized());
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
    @DisplayName("Get current user tests: GET /api/v1/users/me")
    class GetCurrentUserTests {

        @Test
        @DisplayName("When getting current user should return HTTP 401 if authorization header is missing")
        void whenGettingCurrentUserShouldReturnUnauthorizedWithoutAuthorizationHeader() throws Exception {
            mockMvc.perform(get(ApiConstants.CURRENT_USER_URL))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("When getting current user should return complete private account data")
        void whenGettingCurrentUserShouldReturnCompletePrivateAccountData() throws Exception {
            mockMvc.perform(get(ApiConstants.CURRENT_USER_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(firstUserId.toString()))
                    .andExpect(jsonPath("$.firstName").value(UserConstants.FIRST_USER_FIRST_NAME))
                    .andExpect(jsonPath("$.lastName").value(UserConstants.FIRST_USER_LAST_NAME))
                    .andExpect(jsonPath("$.email").value(UserConstants.FIRST_USER_EMAIL))
                    .andExpect(jsonPath("$.homeCity").value(CitiesConstants.WARSAW_NAME))
                    .andExpect(jsonPath("$.timeZone").value(UserConstants.FIRST_USER_TIMEZONE))
                    .andExpect(jsonPath("$.password").doesNotExist())
                    .andExpect(jsonPath("$.roles").doesNotExist())
                    .andExpect(jsonPath("$.securityVersion").doesNotExist());
        }

        @Test
        @DisplayName("OpenAPI should expose the current-user operation and schema")
        void openApiShouldExposeCurrentUserOperationAndSchema() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$['paths']['/api/v1/users/me']['get']").exists())
                    .andExpect(jsonPath("$['components']['schemas']['CurrentUserDto']['properties']['email']").exists())
                    .andExpect(jsonPath("$['components']['schemas']['CurrentUserDto']['properties']['timeZone']").exists());
        }
    }

    @Nested
    @DisplayName("Delete current user tests: DELETE /api/v1/users/me")
    class DeleteCurrentUserTests {

        @Test
        @DisplayName("Deleting current user should require authentication")
        void deletingCurrentUserShouldRequireAuthentication() throws Exception {
            mockMvc.perform(delete(ApiConstants.CURRENT_USER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new DeleteCurrentUserDto(UserConstants.USER_PASSWORD))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Deleting current user should validate password presence")
        void deletingCurrentUserShouldValidatePasswordPresence() throws Exception {
            mockMvc.perform(delete(ApiConstants.CURRENT_USER_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new DeleteCurrentUserDto(" "))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors.password").value("Password is required."));
        }

        @Test
        @DisplayName("Deleting current user should reject an invalid current password")
        void deletingCurrentUserShouldRejectInvalidCurrentPassword() throws Exception {
            mockMvc.perform(delete(ApiConstants.CURRENT_USER_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new DeleteCurrentUserDto(UserConstants.WRONG_USER_PASSWORD))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));

            assertThat(userRepository.existsById(firstUserId)).isTrue();
        }

        @Test
        @DisplayName("Deleting current user should return HTTP 204 and invalidate account access")
        void deletingCurrentUserShouldReturnNoContentAndInvalidateAccountAccess() throws Exception {
            mockMvc.perform(delete(ApiConstants.CURRENT_USER_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new DeleteCurrentUserDto(UserConstants.USER_PASSWORD))))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            assertThat(userRepository.existsById(firstUserId)).isFalse();
            assertThat(userRepository.existsById(secondUserId)).isTrue();

            mockMvc.perform(get(ApiConstants.CURRENT_USER_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("OpenAPI should expose the account-deletion operation and request schema")
        void openApiShouldExposeAccountDeletionOperationAndRequestSchema() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$['paths']['/api/v1/users/me']['delete']").exists())
                    .andExpect(jsonPath("$['components']['schemas']['DeleteCurrentUserDto']['properties']['password']").exists());
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
                    .andExpect(status().isUnauthorized());
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
    @DisplayName("Update user details tests: PUT /api/v1/users/update")
    class UpdateUserDetailsTests {

        @Test
        @DisplayName("When updating user details should return HTTP 403 Forbidden if authorization header is missing")
        public void whenUpdatingUserDetailsShouldReturnForbiddenIfAuthorizationHeaderIsMissing() throws Exception {
            ChangeUserDetailsDto request = ChangeUserDetailsDtoTestBuilder.validUpdate().build();

            mockMvc.perform(put(ApiConstants.USER_UPDATE_DETAILS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("When updating user details should return HTTP 400 Bad Request on validation errors")
        public void whenUpdatingUserDetailsShouldReturnBadRequestOnValidationErrors() throws Exception {
            ChangeUserDetailsDto request = ChangeUserDetailsDtoTestBuilder.validUpdate()
                    .firstName(UserConstants.INVALID_FIRST_NAME)
                    .lastName(UserConstants.INVALID_LAST_NAME)
                    .homeCity(UserConstants.INVALID_CITY_NAME)
                    .timeZone(InvalidInputConstants.INVALID_TIME_ZONE)
                    .build();

            mockMvc.perform(put(ApiConstants.USER_UPDATE_DETAILS_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.firstName").exists())
                    .andExpect(jsonPath("$.errors.lastName").exists())
                    .andExpect(jsonPath("$.errors.homeCity").exists())
                    .andExpect(jsonPath("$.errors.timeZone").exists());
        }

        @Test
        @DisplayName("When updating user details should require a time zone")
        void whenUpdatingUserDetailsShouldRequireTimeZone() throws Exception {
            ChangeUserDetailsDto request = ChangeUserDetailsDtoTestBuilder.validUpdate()
                    .timeZone(null)
                    .build();

            mockMvc.perform(put(ApiConstants.USER_UPDATE_DETAILS_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors.timeZone").exists());
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
                    .andExpect(jsonPath("$.email").value(UserConstants.FIRST_USER_EMAIL))
                    .andExpect(jsonPath("$.homeCity").value(CitiesConstants.KRAKOW_NAME))
                    .andExpect(jsonPath("$.timeZone").value(UserConstants.SECOND_USER_TIMEZONE));

            User updatedUser = userRepository.findById(firstUserId).orElseThrow();
            assertThat(updatedUser.getTimeZone()).isEqualTo(UserConstants.SECOND_USER_TIMEZONE);
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
                    .andExpect(status().isUnauthorized());
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
                    .andExpect(status().isUnauthorized());
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
        @DisplayName("When requesting an email change should return HTTP 202 and keep the verified address")
        public void whenChangingEmailShouldAcceptConfirmationRequestWithoutChangingCurrentEmail() throws Exception {
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
                    .andExpect(status().isAccepted())
                    .andExpect(content().string(""));

        User updatedUser = userRepository.findById(firstUserId).orElseThrow();
            assertThat(updatedUser.getEmail()).isEqualTo(UserConstants.FIRST_USER_EMAIL);
        }
    }

}
