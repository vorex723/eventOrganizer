package com.mazurek.eventOrganizer.user;

import tools.jackson.databind.ObjectMapper;
import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationRequest;
import com.mazurek.eventOrganizer.event.EventRepository;
import com.mazurek.eventOrganizer.event.EventService;
import com.mazurek.eventOrganizer.exception.ApiErrorCode;
import com.mazurek.eventOrganizer.exception.user.InvalidPasswordException;
import com.mazurek.eventOrganizer.exception.user.NotMatchingEmailsException;
import com.mazurek.eventOrganizer.exception.user.NotMatchingPasswordsException;
import com.mazurek.eventOrganizer.exception.user.UserAlreadyExistException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.jwt.JwtUtils;
import com.mazurek.eventOrganizer.testData.builders.RefreshTokenTestBuilder;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestDataInitializer;
import com.mazurek.eventOrganizer.testData.TestPersistenceQueries;
import com.mazurek.eventOrganizer.testData.builders.AuthenticationRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.DeleteCurrentUserDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ChangeUserDetailsDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ChangeUserEmailDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ChangeUserPasswordDtoTestBuilder;
import com.mazurek.eventOrganizer.user.dto.ChangeUserDetailsDto;
import com.mazurek.eventOrganizer.user.dto.ChangeUserEmailDto;
import com.mazurek.eventOrganizer.user.dto.ChangeUserPasswordDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("UserController integration tests:")
public class UserControllerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private TestPersistenceQueries testPersistenceQueries;

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
    private JwtUtils jwtUtils;
    @Autowired
    private PasswordEncoder passwordEncoder;
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
        @DisplayName("When getting user by id should return HTTP 401 Unauthorized if authorization header is missing")
        public void whenGettingUserByIdShouldReturnUnauthorizedIfAuthorizationHeaderIsMissing() throws Exception {
            mockMvc.perform(get(ApiConstants.USER_BY_ID_URL, firstUserId))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));
        }

        @Test
        @DisplayName("When getting user by id should return HTTP 404 Not Found if user does not exist")
        public void whenGettingUserByIdShouldReturnNotFoundIfUserDoesNotExist() throws Exception {
            mockMvc.perform(get(ApiConstants.USER_BY_ID_URL, UserConstants.NOT_EXISTING_USER_ID)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.USER_NOT_FOUND))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()));
        }

        @Test
        @DisplayName("When getting user by id should return HTTP 200 OK with profile dto and correct fields")
        public void whenGettingUserByIdShouldReturnProfileDtoWithCorrectFields() throws Exception {
            mockMvc.perform(get(ApiConstants.USER_BY_ID_URL, firstUserId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(firstUserId.toString()))
                    .andExpect(jsonPath("$.firstName").value(UserConstants.FIRST_USER_FIRST_NAME))
                    .andExpect(jsonPath("$.lastName").value(UserConstants.FIRST_USER_LAST_NAME))
                    .andExpect(jsonPath("$.homeCity").doesNotHaveJsonPath())
                    .andExpect(jsonPath("$.homeCityId").doesNotHaveJsonPath())
                    .andExpect(jsonPath("$.homeCityExternalId").doesNotHaveJsonPath())
                    .andExpect(jsonPath("$.email").doesNotHaveJsonPath())
                    .andExpect(jsonPath("$.timeZone").doesNotHaveJsonPath())
                    .andExpect(jsonPath("$.password").doesNotHaveJsonPath())
                    .andExpect(jsonPath("$.roles").doesNotHaveJsonPath())
                    .andExpect(jsonPath("$.securityVersion").doesNotHaveJsonPath());
        }
    }

    @Nested
    @DisplayName("Get current user tests: GET /api/v1/users/me")
    class GetCurrentUserTests {

        @Test
        @DisplayName("When getting current user should return HTTP 401 if authorization header is missing")
        void whenGettingCurrentUserShouldReturnUnauthorizedWithoutAuthorizationHeader() throws Exception {
            mockMvc.perform(get(ApiConstants.CURRENT_USER_URL))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));
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

    }

    @Nested
    @DisplayName("Delete current user tests: DELETE /api/v1/users/me")
    class DeleteCurrentUserTests {

        @Test
        @DisplayName("When deleting current user should require authentication")
        void whenDeletingCurrentUserShouldRequireAuthentication() throws Exception {
            var beforeWrite = credentialWriteState();

            mockMvc.perform(delete(ApiConstants.CURRENT_USER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new DeleteCurrentUserDtoTestBuilder()
                                    .password(UserConstants.USER_PASSWORD)
                                    .build())))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));

            assertThat(credentialWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When deleting current user should validate password presence")
        void whenDeletingCurrentUserShouldValidatePasswordPresence() throws Exception {
            var beforeWrite = credentialWriteState();

            mockMvc.perform(delete(ApiConstants.CURRENT_USER_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new DeleteCurrentUserDtoTestBuilder()
                                    .password(" ")
                                    .build())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_FAILED))
                    .andExpect(jsonPath("$.errors.password").value("Password is required."));

            assertThat(credentialWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When deleting current user should reject an invalid current password")
        void whenDeletingCurrentUserShouldRejectInvalidCurrentPassword() throws Exception {
            var beforeWrite = credentialWriteState();

            mockMvc.perform(delete(ApiConstants.CURRENT_USER_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new DeleteCurrentUserDtoTestBuilder()
                                            .password(UserConstants.WRONG_USER_PASSWORD)
                                            .build())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.INVALID_CURRENT_PASSWORD));

            assertThat(userRepository.existsById(firstUserId)).isTrue();

            assertThat(credentialWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When deleting current user should return HTTP 204 and invalidate account access")
        void whenDeletingCurrentUserShouldReturnNoContentAndInvalidateAccountAccess() throws Exception {
            mockMvc.perform(delete(ApiConstants.CURRENT_USER_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new DeleteCurrentUserDtoTestBuilder()
                                    .password(UserConstants.USER_PASSWORD)
                                    .build())))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            assertThat(userRepository.existsById(firstUserId)).isFalse();
            assertThat(userRepository.existsById(secondUserId)).isTrue();

            mockMvc.perform(get(ApiConstants.CURRENT_USER_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));
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
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.USER_NOT_FOUND))
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
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.INVALID_PAGE_NUMBER))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()));
        }

        @Test
        @DisplayName("When getting user events should return HTTP 200 OK and respect upcoming query parameter")
        public void whenGettingUserEventsShouldRespectUpcomingQueryParameter() throws Exception {
            UUID upcomingEventId = testDataInitializer.setupFirstEvent();
            UUID pastEventId = testDataInitializer.setupEventByFirstUser();

            var pastEvent = requirePresent(eventRepository.findById(pastEventId), "Expected event record in whenGettingUserEventsShouldRespectUpcomingQueryParameter");
            pastEvent.setEventStartDate(TimeConstants.ONE_WEEK_AGO);
            eventRepository.save(pastEvent);

            mockMvc.perform(get(ApiConstants.USER_EVENTS_URL, firstUserId)
                            .param("page", String.valueOf(PaginationConstants.PAGE_ZERO))
                            .param("upcoming", String.valueOf(true))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events.length()").value(1))
                    .andExpect(jsonPath("$.pageNumber").value(PaginationConstants.PAGE_ZERO))
                    .andExpect(jsonPath("$.pageSize").value(PaginationConstants.EVENT_PAGE_SIZE))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.lastPage").value(true))
                    .andExpect(jsonPath("$.events[0].id").value(upcomingEventId.toString()));

            mockMvc.perform(get(ApiConstants.USER_EVENTS_URL, firstUserId)
                            .param("page", String.valueOf(PaginationConstants.PAGE_ZERO))
                            .param("upcoming", String.valueOf(false))
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events.length()").value(2))
                    .andExpect(jsonPath("$.pageNumber").value(PaginationConstants.PAGE_ZERO))
                    .andExpect(jsonPath("$.pageSize").value(PaginationConstants.EVENT_PAGE_SIZE))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.lastPage").value(true))
                    .andExpect(jsonPath("$.events[0].id").value(upcomingEventId.toString()))
                    .andExpect(jsonPath("$.events[1].id").value(pastEventId.toString()))
                    .andExpect(jsonPath("$.events[*].id").value(org.hamcrest.Matchers.hasItems(
                            upcomingEventId.toString(),
                            pastEventId.toString())));
        }
    }

    @Nested
    @DisplayName("Get current user attending events tests: GET /api/v1/users/me/attending-events")
    class GetUserAttendingEventsTests {

        @Test
        @DisplayName("When getting current user attending events should return HTTP 401 Unauthorized if authorization header is missing")
        public void whenGettingCurrentUserAttendingEventsShouldReturnUnauthorizedIfAuthorizationHeaderIsMissing() throws Exception {
            mockMvc.perform(get(ApiConstants.USER_ATTENDING_EVENTS_URL)
                            .param("page", String.valueOf(PaginationConstants.PAGE_ZERO)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));
        }

        @Test
        @DisplayName("When getting current user attending events should return HTTP 400 Bad Request if page is negative")
        public void whenGettingCurrentUserAttendingEventsShouldReturnBadRequestIfPageIsNegative() throws Exception {
            mockMvc.perform(get(ApiConstants.USER_ATTENDING_EVENTS_URL)
                            .param("page", String.valueOf(PaginationConstants.PAGE_MINUS_ONE))
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.INVALID_PAGE_NUMBER))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()));
        }

        @Test
        @DisplayName("When getting current user attending events should return HTTP 200 OK and respect upcoming query parameter")
        public void whenGettingCurrentUserAttendingEventsShouldRespectUpcomingQueryParameter() throws Exception {
            UUID upcomingEventId = testDataInitializer.setupFirstEvent();
            UUID pastEventId = testDataInitializer.setupEventByFirstUser();

            authHelper.setupSecurityContextForSecondUser();
            eventService.addAttendeeToEvent(upcomingEventId);
            eventService.addAttendeeToEvent(pastEventId);
            SecurityContextHolder.clearContext();

            var pastEvent = requirePresent(eventRepository.findById(pastEventId), "Expected event record in whenGettingCurrentUserAttendingEventsShouldRespectUpcomingQueryParameter");
            pastEvent.setEventStartDate(TimeConstants.ONE_WEEK_AGO);
            eventRepository.save(pastEvent);

            mockMvc.perform(get(ApiConstants.USER_ATTENDING_EVENTS_URL)
                            .param("page", String.valueOf(PaginationConstants.PAGE_ZERO))
                            .param("upcoming", String.valueOf(true))
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events.length()").value(1))
                    .andExpect(jsonPath("$.pageNumber").value(PaginationConstants.PAGE_ZERO))
                    .andExpect(jsonPath("$.pageSize").value(PaginationConstants.EVENT_PAGE_SIZE))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.lastPage").value(true))
                    .andExpect(jsonPath("$.events[0].id").value(upcomingEventId.toString()));

            mockMvc.perform(get(ApiConstants.USER_ATTENDING_EVENTS_URL)
                            .param("page", String.valueOf(PaginationConstants.PAGE_ZERO))
                            .param("upcoming", String.valueOf(false))
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events.length()").value(2))
                    .andExpect(jsonPath("$.pageNumber").value(PaginationConstants.PAGE_ZERO))
                    .andExpect(jsonPath("$.pageSize").value(PaginationConstants.EVENT_PAGE_SIZE))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.lastPage").value(true))
                    .andExpect(jsonPath("$.events[0].id").value(upcomingEventId.toString()))
                    .andExpect(jsonPath("$.events[1].id").value(pastEventId.toString()))
                    .andExpect(jsonPath("$.events[*].id").value(org.hamcrest.Matchers.hasItems(
                            upcomingEventId.toString(),
                            pastEventId.toString())));
        }
    }

    @Nested
    @DisplayName("Update user details tests: PUT /api/v1/users/update")
    class UpdateUserDetailsTests {

        @Test
        @DisplayName("When updating user details should return HTTP 401 Unauthorized if authorization header is missing")
        public void whenUpdatingUserDetailsShouldReturnUnauthorizedIfAuthorizationHeaderIsMissing() throws Exception {
            ChangeUserDetailsDto request = ChangeUserDetailsDtoTestBuilder.validUpdate().build();

            var beforeWrite = credentialWriteState();

            mockMvc.perform(put(ApiConstants.USER_UPDATE_DETAILS_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));

            assertThat(credentialWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When updating user details should return HTTP 400 Bad Request on validation errors")
        public void whenUpdatingUserDetailsShouldReturnBadRequestOnValidationErrors() throws Exception {
            ChangeUserDetailsDto request = ChangeUserDetailsDtoTestBuilder.validUpdate()
                    .firstName(UserConstants.INVALID_FIRST_NAME)
                    .lastName(UserConstants.INVALID_LAST_NAME)
                    .homeCityExternalId(UserConstants.INVALID_CITY_NAME)
                    .timeZone(InvalidInputConstants.INVALID_TIME_ZONE)
                    .build();

            var beforeWrite = credentialWriteState();

            mockMvc.perform(put(ApiConstants.USER_UPDATE_DETAILS_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_FAILED))
                    .andExpect(jsonPath("$.errors.firstName").exists())
                    .andExpect(jsonPath("$.errors.lastName").exists())
                    .andExpect(jsonPath("$.errors.homeCityExternalId").exists())
                    .andExpect(jsonPath("$.errors.timeZone").exists());

            assertThat(credentialWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When updating user details should require a time zone")
        void whenUpdatingUserDetailsShouldRequireTimeZone() throws Exception {
            ChangeUserDetailsDto request = ChangeUserDetailsDtoTestBuilder.validUpdate()
                    .timeZone(null)
                    .build();

            var beforeWrite = credentialWriteState();

            mockMvc.perform(put(ApiConstants.USER_UPDATE_DETAILS_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_FAILED))
                    .andExpect(jsonPath("$.errors.timeZone").exists());

            assertThat(credentialWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
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

            User updatedUser = requirePresent(userRepository.findById(firstUserId), "Expected user record in whenUpdatingUserDetailsShouldPersistChangesAndReturnUpdatedProfile");
            assertThat(updatedUser)
                    .extracting(User::getFirstName, User::getLastName, User::getEmail, User::getTimeZone)
                    .containsExactly(request.getFirstName(), request.getLastName(), UserConstants.FIRST_USER_EMAIL, request.getTimeZone());
            assertThat(updatedUser.getHomeCity().getExternalId()).isEqualTo(request.getHomeCityExternalId());
        }
    }

    @Nested
    @DisplayName("Change password tests: PUT /api/v1/users/change-password")
    class ChangePasswordTests {

        @Test
        @DisplayName("When changing password should return HTTP 401 Unauthorized if authorization header is missing")
        public void whenChangingPasswordShouldReturnUnauthorizedIfAuthorizationHeaderIsMissing() throws Exception {
            ChangeUserPasswordDto request = ChangeUserPasswordDtoTestBuilder.validChange().build();

            var beforeWrite = credentialWriteState();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_PASSWORD_URL)
                            .header(DeviceConstants.DEVICE_TYPE_HEADER, DeviceType.WEB.name())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));

            assertThat(credentialWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When changing password should return HTTP 400 Bad Request if request body is invalid")
        public void whenChangingPasswordShouldReturnBadRequestIfRequestBodyIsInvalid() throws Exception {
            ChangeUserPasswordDto request = ChangeUserPasswordDtoTestBuilder.validChange()
                    .newPassword(InvalidInputConstants.WEAK_PASSWORD)
                    .newPasswordConfirmation(InvalidInputConstants.WEAK_PASSWORD)
                    .build();

            var beforeWrite = credentialWriteState();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_PASSWORD_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .header(DeviceConstants.DEVICE_TYPE_HEADER, DeviceType.WEB.name())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_FAILED))
                    .andExpect(jsonPath("$.errors.newPassword").exists())
                    .andExpect(jsonPath("$.errors.newPasswordConfirmation").exists());

            assertThat(credentialWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When changing password should return HTTP 400 Bad Request if old password is invalid")
        public void whenChangingPasswordShouldReturnBadRequestIfOldPasswordIsInvalid() throws Exception {
            ChangeUserPasswordDto request = ChangeUserPasswordDtoTestBuilder.validChange()
                    .password(UserConstants.WRONG_USER_PASSWORD)
                    .build();

            var beforeWrite = credentialWriteState();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_PASSWORD_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .header(DeviceConstants.DEVICE_TYPE_HEADER, DeviceType.WEB.name())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.INVALID_CURRENT_PASSWORD));

            assertThat(credentialWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When changing password should return HTTP 400 Bad Request if new password confirmation does not match")
        public void whenChangingPasswordShouldReturnBadRequestIfNewPasswordConfirmationDoesNotMatch() throws Exception {
            ChangeUserPasswordDto request = ChangeUserPasswordDtoTestBuilder.validChange()
                    .newPasswordConfirmation(InvalidInputConstants.DIFFERENT_PASSWORD)
                    .build();

            var beforeWrite = credentialWriteState();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_PASSWORD_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .header(DeviceConstants.DEVICE_TYPE_HEADER, DeviceType.WEB.name())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.PASSWORD_CONFIRMATION_MISMATCH))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.message").value(NotMatchingPasswordsException.DEFAULT_MESSAGE));

            assertThat(credentialWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When changing password should return HTTP 200 OK with new authentication tokens")
        public void whenChangingPasswordShouldReturnNewAuthenticationTokens() throws Exception {
            ChangeUserPasswordDto request = ChangeUserPasswordDtoTestBuilder.validChange().build();
            User beforeChange = requirePresent(userRepository.findById(firstUserId), "Expected account before password change");

            var result = mockMvc.perform(put(ApiConstants.USER_CHANGE_PASSWORD_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .header(DeviceConstants.DEVICE_TYPE_HEADER, DeviceType.WEB.name())
                            .header(DeviceConstants.USER_AGENT_HEADER, DeviceConstants.USER_AGENT_DESKTOP_WINDOWS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.accessTokenExpiration").isNumber())
                    .andReturn();

            var response = objectMapper.readTree(result.getResponse().getContentAsString());
            String accessToken = response.path("accessToken").asString();
            String refreshToken = response.path("refreshToken").asString();
            assertThat(jwtUtils.isTokenValid(accessToken)).isTrue();
            assertThat(jwtUtils.extractUserId(accessToken)).isEqualTo(firstUserId);
            assertThat(jwtUtils.extractSecurityVersion(accessToken)).isEqualTo(beforeChange.getSecurityVersion() + 1);

            User updatedUser = requirePresent(userRepository.findById(firstUserId), "Expected committed password change");
            assertThat(updatedUser).isNotSameAs(beforeChange);
            assertThat(updatedUser.getSecurityVersion()).isEqualTo(beforeChange.getSecurityVersion() + 1);
            assertThat(updatedUser.getLastCredentialsChangeTime()).isEqualTo(TimeConstants.NOW);
            assertThat(passwordEncoder.matches(request.getNewPassword(), updatedUser.getPassword())).isTrue();
            assertThat(passwordEncoder.matches(UserConstants.USER_PASSWORD, updatedUser.getPassword())).isFalse();
            var storedRefreshToken = requirePresent(testPersistenceQueries.findRefreshTokenByHash(
                    RefreshTokenTestBuilder.hashOf(refreshToken)), "Expected new persisted refresh credential");
            assertThat(storedRefreshToken.getUser().getId()).isEqualTo(firstUserId);
            assertThat(storedRefreshToken.isRevoked()).isFalse();

            mockMvc.perform(get(ApiConstants.CURRENT_USER_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));
            mockMvc.perform(get(ApiConstants.CURRENT_USER_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, AuthConstants.JWT_PREFIX + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(firstUserId.toString()));
            mockMvc.perform(get(ApiConstants.CURRENT_USER_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(secondUserId.toString()));
        }
    }

    @Nested
    @DisplayName("Change email tests: PUT /api/v1/users/change-email")
    class ChangeEmailTests {

        @Test
        @DisplayName("When changing email should return HTTP 401 Unauthorized if authorization header is missing")
        public void whenChangingEmailShouldReturnUnauthorizedIfAuthorizationHeaderIsMissing() throws Exception {
            ChangeUserEmailDto request = ChangeUserEmailDtoTestBuilder.validChange().build();

            var beforeWrite = credentialWriteState();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_EMAIL_URL)
                            .header(DeviceConstants.DEVICE_TYPE_HEADER, DeviceType.WEB.name())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));

            assertThat(credentialWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When changing email should return HTTP 400 Bad Request if request body is invalid")
        public void whenChangingEmailShouldReturnBadRequestIfRequestBodyIsInvalid() throws Exception {
            ChangeUserEmailDto request = ChangeUserEmailDtoTestBuilder.validChange()
                    .newEmail(InvalidInputConstants.INVALID_EMAIL)
                    .newEmailConfirmation(InvalidInputConstants.INVALID_EMAIL)
                    .password(InvalidInputConstants.WEAK_PASSWORD)
                    .build();

            var beforeWrite = credentialWriteState();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_EMAIL_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .header(DeviceConstants.DEVICE_TYPE_HEADER, DeviceType.WEB.name())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_FAILED))
                    .andExpect(jsonPath("$.errors.newEmail").exists())
                    .andExpect(jsonPath("$.errors.newEmailConfirmation").exists())
                    .andExpect(jsonPath("$.errors.password").exists());

            assertThat(credentialWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When changing email should return HTTP 400 Bad Request if new email is blank")
        public void whenChangingEmailShouldReturnBadRequestIfNewEmailIsBlank() throws Exception {
            ChangeUserEmailDto request = ChangeUserEmailDtoTestBuilder.validChange()
                    .newEmail(InvalidInputConstants.BLANK_VALUE)
                    .newEmailConfirmation(InvalidInputConstants.BLANK_VALUE)
                    .build();

            var beforeWrite = credentialWriteState();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_EMAIL_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .header(DeviceConstants.DEVICE_TYPE_HEADER, DeviceType.WEB.name())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_FAILED))
                    .andExpect(jsonPath("$.errors.newEmail").exists())
                    .andExpect(jsonPath("$.errors.newEmailConfirmation").exists());

            assertThat(credentialWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When changing email should return HTTP 400 Bad Request if password is invalid")
        public void whenChangingEmailShouldReturnBadRequestIfPasswordIsInvalid() throws Exception {
            ChangeUserEmailDto request = ChangeUserEmailDtoTestBuilder.validChange()
                    .password(UserConstants.WRONG_USER_PASSWORD)
                    .build();

            var beforeWrite = credentialWriteState();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_EMAIL_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .header(DeviceConstants.DEVICE_TYPE_HEADER, DeviceType.WEB.name())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.INVALID_CURRENT_PASSWORD))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.message").value(InvalidPasswordException.DEFAULT_MESSAGE));

            assertThat(credentialWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When changing email should return HTTP 400 Bad Request if email confirmation does not match")
        public void whenChangingEmailShouldReturnBadRequestIfEmailConfirmationDoesNotMatch() throws Exception {
            ChangeUserEmailDto request = ChangeUserEmailDtoTestBuilder.validChange()
                    .newEmailConfirmation(InvalidInputConstants.DIFFERENT_EMAIL)
                    .build();

            var beforeWrite = credentialWriteState();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_EMAIL_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .header(DeviceConstants.DEVICE_TYPE_HEADER, DeviceType.WEB.name())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.EMAIL_CONFIRMATION_MISMATCH))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.message").value(NotMatchingEmailsException.DEFAULT_MESSAGE));

            assertThat(credentialWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When changing email should return HTTP 409 Conflict if new email is already used by another account")
        public void whenChangingEmailShouldReturnConflictIfNewEmailIsAlreadyUsedByAnotherAccount() throws Exception {
            ChangeUserEmailDto request = ChangeUserEmailDtoTestBuilder.validChange()
                    .newEmail(UserConstants.SECOND_USER_EMAIL)
                    .newEmailConfirmation(UserConstants.SECOND_USER_EMAIL)
                    .build();

            var beforeWrite = credentialWriteState();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_EMAIL_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .header(DeviceConstants.DEVICE_TYPE_HEADER, DeviceType.WEB.name())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.EMAIL_ALREADY_EXISTS))
                    .andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
                    .andExpect(jsonPath("$.message").value(UserAlreadyExistException.DEFAULT_MESSAGE));

            assertThat(credentialWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When changing email should return HTTP 409 Conflict if new email matches current email")
        public void whenChangingEmailShouldReturnConflictForSameEmail() throws Exception {
            ChangeUserEmailDto request = ChangeUserEmailDtoTestBuilder.validChange()
                    .newEmail(UserConstants.FIRST_USER_EMAIL)
                    .newEmailConfirmation(UserConstants.FIRST_USER_EMAIL)
                    .build();

            var beforeWrite = credentialWriteState();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_EMAIL_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .header(DeviceConstants.DEVICE_TYPE_HEADER, DeviceType.WEB.name())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.EMAIL_UNCHANGED));

            assertThat(credentialWriteState())
                    .as("Rejected request must preserve persisted state")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When requesting an email change should return HTTP 202 and keep the verified address")
        public void whenChangingEmailShouldAcceptConfirmationRequestWithoutChangingCurrentEmail() throws Exception {
            ChangeUserEmailDto request = ChangeUserEmailDtoTestBuilder.validChange()
                    .newEmail(UserConstants.FIRST_USER_NEW_EMAIL.toUpperCase(Locale.ROOT))
                    .newEmailConfirmation(UserConstants.FIRST_USER_NEW_EMAIL.toUpperCase(Locale.ROOT))
                    .build();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_EMAIL_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .header(DeviceConstants.DEVICE_TYPE_HEADER, DeviceType.WEB.name())
                            .header(DeviceConstants.USER_AGENT_HEADER, DeviceConstants.USER_AGENT_DESKTOP_WINDOWS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted())
                    .andExpect(content().string(""));

            User updatedUser = requirePresent(userRepository.findById(firstUserId), "Expected user record in whenChangingEmailShouldAcceptConfirmationRequestWithoutChangingCurrentEmail");
            assertThat(updatedUser.getEmail()).isEqualTo(UserConstants.FIRST_USER_EMAIL);
        }

        @Test
        void whenSecondUserReservesSamePendingEmailShouldRejectRequest() throws Exception {
            ChangeUserEmailDto request = ChangeUserEmailDtoTestBuilder.validChange().build();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_EMAIL_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted());

            var beforeRejectedReservation = credentialWriteState();
            mockMvc.perform(put(ApiConstants.USER_CHANGE_EMAIL_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.EMAIL_CHANGE_ADDRESS_UNAVAILABLE));

            assertThat(credentialWriteState()).isEqualTo(beforeRejectedReservation);
            assertThat(testPersistenceQueries.findEmailChangeTokenByUserId(firstUserId)).isPresent();
            assertThat(testPersistenceQueries.findEmailChangeTokenByUserId(secondUserId)).isEmpty();
            assertThat(requirePresent(userRepository.findById(firstUserId), "Expected user record in whenSecondUserReservesSamePendingEmailShouldRejectRequest").getEmail())
                    .isEqualTo(UserConstants.FIRST_USER_EMAIL);
            assertThat(requirePresent(userRepository.findById(secondUserId), "Expected user record in whenSecondUserReservesSamePendingEmailShouldRejectRequest").getEmail())
                    .isEqualTo(UserConstants.SECOND_USER_EMAIL);
        }

        @Test
        void whenChangingPendingEmailToReservedAddressShouldReturnConflict() throws Exception {
            String previousPendingEmail = "previous.pending@example.com";
            ChangeUserEmailDto previousRequest = ChangeUserEmailDtoTestBuilder.validChange()
                    .newEmail(previousPendingEmail)
                    .newEmailConfirmation(previousPendingEmail)
                    .build();
            ChangeUserEmailDto reservedRequest = ChangeUserEmailDtoTestBuilder.validChange().build();

            mockMvc.perform(put(ApiConstants.USER_CHANGE_EMAIL_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(previousRequest)))
                    .andExpect(status().isAccepted());
            mockMvc.perform(put(ApiConstants.USER_CHANGE_EMAIL_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reservedRequest)))
                    .andExpect(status().isAccepted());

            var beforeRejectedReservation = credentialWriteState();
            mockMvc.perform(put(ApiConstants.USER_CHANGE_EMAIL_URL)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reservedRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.EMAIL_CHANGE_ADDRESS_UNAVAILABLE));

            assertThat(credentialWriteState()).isEqualTo(beforeRejectedReservation);
            assertThat(requirePresent(testPersistenceQueries.findEmailChangeTokenByUserId(secondUserId), "Expected email-change token record in whenChangingPendingEmailToReservedAddressShouldReturnConflict").getPendingEmail())
                    .isEqualTo(previousPendingEmail);
        }
    }

    // Independent committed reads: no managed entity snapshot or test-level transaction.
    private Map<String, List<Map<String, Object>>> credentialWriteState() {
        return Map.of(
                "users", jdbcTemplate.queryForList("SELECT * FROM users ORDER BY id"),
                "refreshTokens", jdbcTemplate.queryForList("SELECT * FROM refresh_tokens ORDER BY id"),
                "emailChangeTokens", jdbcTemplate.queryForList("SELECT * FROM email_change_tokens ORDER BY id"),
                "emailOutbox", jdbcTemplate.queryForList("SELECT * FROM auth_email_deliveries ORDER BY id")
        );
    }
}
