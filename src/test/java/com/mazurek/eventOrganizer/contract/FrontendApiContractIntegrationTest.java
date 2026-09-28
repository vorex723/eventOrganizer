package com.mazurek.eventOrganizer.contract;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationRequest;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationResponse;
import com.mazurek.eventOrganizer.auth.dto.RefreshTokenRequest;
import com.mazurek.eventOrganizer.conversation.dto.ConversationDetailsDto;
import com.mazurek.eventOrganizer.conversation.dto.DirectMessageResponseDto;
import com.mazurek.eventOrganizer.conversation.dto.MarkConversationReadDto;
import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.dto.EventDto;
import com.mazurek.eventOrganizer.exception.ApiErrorCode;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.notification.dto.UpdateNotificationPreferenceDto;
import com.mazurek.eventOrganizer.notification.dto.UpdateNotificationPreferencesDto;
import com.mazurek.eventOrganizer.notification.repository.NotificationRepository;
import com.mazurek.eventOrganizer.notification.service.EmailServiceTestImpl;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestDataInitializer;
import com.mazurek.eventOrganizer.testData.builders.AuthenticationRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.NotificationTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.EventCreateDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.MultipartFileTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.RegisterRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.SendDirectMessageDtoTestBuilder;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.ApiConstants.*;
import static com.mazurek.eventOrganizer.testData.TestConstants.AuthConstants.ACTIVATION_RESULT_BASE_URL;
import static com.mazurek.eventOrganizer.testData.TestConstants.AuthConstants.JWT_PREFIX;
import static com.mazurek.eventOrganizer.testData.TestConstants.FileConstants.USER_FILE_NAME;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A compact regression suite for the API contract consumed by the frontend.
 *
 * <p>Feature-specific integration tests retain exhaustive behavioural coverage. This class protects
 * the stable status codes and JSON fields that the frontend depends on across those workflows.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Frontend API contract integration tests")
class FrontendApiContractIntegrationTest {

    private final AuthenticationRequest firstUserCredentials =
            AuthenticationRequestTestBuilder.authenticationRequestForFirstUser().build();
    private final AuthenticationRequest secondUserCredentials =
            AuthenticationRequestTestBuilder.authenticationRequestForSecondUser().build();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private TestDataInitializer testDataInitializer;
    @Autowired
    private DeletionService deletionService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private EmailServiceTestImpl emailService;

    private User firstUser;
    private User secondUser;
    private String firstUserJwt;
    private String secondUserJwt;

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();

        firstUser = userByEmail(FIRST_USER_EMAIL);
        secondUser = userByEmail(SECOND_USER_EMAIL);
        firstUserJwt = jwtFor(firstUserCredentials);
        secondUserJwt = jwtFor(secondUserCredentials);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
    }

    @Test
    void loginAndRefreshExposeTheFrontendSessionContract() throws Exception {
        MvcResult loginResult = mockMvc.perform(post(AUTH_LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstUserCredentials)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.accessTokenExpiration").isNumber())
                .andReturn();

        AuthenticationResponse login = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(),
                AuthenticationResponse.class
        );

        mockMvc.perform(post(AUTH_REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(login.getRefreshToken()))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.accessTokenExpiration").isNumber());
    }

    @Test
    void registrationActivationCallbackUsesTheFrontendRedirectContract() throws Exception {
        mockMvc.perform(post(AUTH_REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                RegisterRequestTestBuilder.thirdUserRegisterRequest().build()
                        )))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").isNotEmpty());

        mockMvc.perform(get(AUTH_ACTIVATE_URL, emailService.lastActivationToken(THIRD_USER_EMAIL)))
                .andExpect(status().isSeeOther())
                .andExpect(redirectedUrl(ACTIVATION_RESULT_BASE_URL + "?status=activated"));
    }

    @Test
    void currentUserProfileExposesPrivateAccountFieldsWithoutAuthenticationData() throws Exception {
        mockMvc.perform(get(CURRENT_USER_URL).header(AUTHORIZATION_HEADER, firstUserJwt))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(firstUser.getId().toString()))
                .andExpect(jsonPath("$.firstName").value(FIRST_USER_FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(FIRST_USER_LAST_NAME))
                .andExpect(jsonPath("$.email").value(FIRST_USER_EMAIL))
                .andExpect(jsonPath("$.homeCity").value("warsaw"))
                .andExpect(jsonPath("$.timeZone").value(FIRST_USER_TIMEZONE))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.roles").doesNotExist());
    }

    @Test
    void eventsSupportCreateUpdateAndPaginatedListContracts() throws Exception {
        EventCreateDto createRequest = EventCreateDtoTestBuilder.firstEvent().build();
        MvcResult createResult = mockMvc.perform(post(EVENTS_URL)
                        .header(AUTHORIZATION_HEADER, firstUserJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value(createRequest.getName()))
                .andExpect(jsonPath("$.timeZone").value(createRequest.getTimeZone()))
                .andExpect(jsonPath("$.maxAttendees").value(createRequest.getMaxAttendees()))
                .andReturn();

        EventDto createdEvent = objectMapper.readValue(createResult.getResponse().getContentAsString(), EventDto.class);
        EventCreateDto updateRequest = EventCreateDtoTestBuilder.updatedEvent().build();

        mockMvc.perform(put(EVENT_BY_ID_URL, createdEvent.getId())
                        .header(AUTHORIZATION_HEADER, firstUserJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(createdEvent.getId().toString()))
                .andExpect(jsonPath("$.name").value(updateRequest.getName()))
                .andExpect(jsonPath("$.timeZone").value(updateRequest.getTimeZone()));

        mockMvc.perform(get(EVENTS_URL)
                        .param("page", "0")
                        .header(AUTHORIZATION_HEADER, firstUserJwt))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.events[0].id").value(createdEvent.getId().toString()))
                .andExpect(jsonPath("$.events[0].name").value(updateRequest.getName()))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").isNumber())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.lastPage").value(true));
    }

    @Test
    void conversationsExposeMessagesAndRequireAnExplicitReadAcknowledgement() throws Exception {
        MvcResult sendResult = mockMvc.perform(post(DIRECT_CONVERSATIONS_URL)
                        .header(AUTHORIZATION_HEADER, firstUserJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                SendDirectMessageDtoTestBuilder.firstDirectMessage()
                                        .recipientId(secondUser.getId())
                                        .build()
                        )))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.conversationId").isNotEmpty())
                .andExpect(jsonPath("$.conversationCreated").value(true))
                .andExpect(jsonPath("$.message.id").isNumber())
                .andExpect(jsonPath("$.message.senderId").value(firstUser.getId().toString()))
                .andExpect(jsonPath("$.message.content").isNotEmpty())
                .andReturn();

        DirectMessageResponseDto directMessage = objectMapper.readValue(
                sendResult.getResponse().getContentAsString(),
                DirectMessageResponseDto.class
        );

        mockMvc.perform(get(CONVERSATION_MESSAGES_URL, directMessage.conversationId())
                        .header(AUTHORIZATION_HEADER, secondUserJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].id").value(directMessage.message().getId()))
                .andExpect(jsonPath("$.messages[0].content").value(directMessage.message().getContent()))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(post("/api/v1/conversations/{conversationId}/read", directMessage.conversationId())
                        .header(AUTHORIZATION_HEADER, secondUserJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MarkConversationReadDto(directMessage.message().getId())
                        )))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        MvcResult detailsResult = mockMvc.perform(get(CONVERSATION_BY_ID_URL, directMessage.conversationId())
                        .header(AUTHORIZATION_HEADER, secondUserJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants").isArray())
                .andReturn();
        ConversationDetailsDto details = objectMapper.readValue(
                detailsResult.getResponse().getContentAsString(),
                ConversationDetailsDto.class
        );

        var secondParticipant = details.participants().stream()
                .filter(participant -> secondUser.getId().equals(participant.userId()))
                .findFirst()
                .orElseThrow();
        assertThat(secondParticipant.lastReadAt()).isNotNull();
        assertThat(secondParticipant.lastReadMessageId()).isEqualTo(directMessage.message().getId());
    }

    @Test
    void notificationPreferencesAndReadActionsExposeCurrentUserState() throws Exception {
        mockMvc.perform(get(NOTIFICATION_PREFERENCES_URL).header(AUTHORIZATION_HEADER, firstUserJwt))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.preferences").isArray());

        List<UpdateNotificationPreferenceDto> preferences = mutableDefaultPreferenceMatrix();
        replacePreference(preferences, NotificationResourceType.EVENT, NotificationChannel.PUSH_WEB, false);
        mockMvc.perform(put(NOTIFICATION_PREFERENCES_URL)
                        .header(AUTHORIZATION_HEADER, firstUserJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateNotificationPreferencesDto(0L, preferences))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.preferences.length()").value(preferences.size()));

        UUID notificationId = notificationRepository.saveAndFlush(
                NotificationTestBuilder.privateMessageNotification()
                        .id(null)
                        .recipientId(firstUser.getId())
                        .build()
        ).getId();

        mockMvc.perform(patch(NOTIFICATION_READ_URL, notificationId).header(AUTHORIZATION_HEADER, firstUserJwt))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(notificationRepository.findById(notificationId).orElseThrow().getReadAt()).isNotNull();
    }

    @Test
    void fileUploadUsesTheMultipartCreatedContract() throws Exception {
        UUID eventId = testDataInitializer.setupFirstEvent();
        MockMultipartFile file = MultipartFileTestBuilder.jpgFile().buildMultipartFile();

        mockMvc.perform(multipart(EVENT_FILES_URL, eventId)
                        .file(file)
                        .param("userFilename", USER_FILE_NAME)
                        .header(AUTHORIZATION_HEADER, firstUserJwt))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.userFilename").value(USER_FILE_NAME))
                .andExpect(jsonPath("$.originalFilename").value(file.getOriginalFilename()))
                .andExpect(jsonPath("$.fileContentType").value(file.getContentType()))
                .andExpect(jsonPath("$.owner.id").value(firstUser.getId().toString()));
    }

    @Test
    void representativeClientFailuresShareTheApiErrorEnvelope() throws Exception {
        expectErrorEnvelope(
                mockMvc.perform(get(EVENTS_URL)
                        .param("page", "-1")
                        .header(AUTHORIZATION_HEADER, firstUserJwt)),
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.INVALID_PAGE_NUMBER
        );

        expectErrorEnvelope(
                mockMvc.perform(get(CURRENT_USER_URL)),
                HttpStatus.UNAUTHORIZED,
                ApiErrorCode.AUTHENTICATION_REQUIRED
        );

        UUID eventId = testDataInitializer.setupFirstEvent();
        expectErrorEnvelope(
                mockMvc.perform(put(EVENT_BY_ID_URL, eventId)
                        .header(AUTHORIZATION_HEADER, secondUserJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(EventCreateDtoTestBuilder.updatedEvent().build()))),
                HttpStatus.FORBIDDEN,
                ApiErrorCode.NOT_EVENT_OWNER
        );

        expectErrorEnvelope(
                mockMvc.perform(put(NOTIFICATION_PREFERENCES_URL)
                        .header(AUTHORIZATION_HEADER, firstUserJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateNotificationPreferencesDto(1L, mutableDefaultPreferenceMatrix())
                        ))),
                HttpStatus.CONFLICT,
                ApiErrorCode.STALE_NOTIFICATION_PREFERENCES
        );
    }

    private String jwtFor(AuthenticationRequest credentials) {
        return JWT_PREFIX + authenticationService.authenticate(credentials, DeviceType.WEB).getAccessToken();
    }

    private User userByEmail(String email) {
        return userRepository.findByIgnoreCaseEmail(email).orElseThrow();
    }

    private void expectErrorEnvelope(ResultActions result, HttpStatus expectedStatus, String expectedCode) throws Exception {
        result.andExpect(status().is(expectedStatus.value()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(expectedStatus.value()))
                .andExpect(jsonPath("$.code").value(expectedCode))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    private List<UpdateNotificationPreferenceDto> mutableDefaultPreferenceMatrix() {
        return new ArrayList<>(Arrays.stream(NotificationResourceType.values())
                .flatMap(resourceType -> Arrays.stream(NotificationChannel.values())
                        .map(channel -> new UpdateNotificationPreferenceDto(
                                resourceType,
                                channel,
                                channel != NotificationChannel.EMAIL
                        )))
                .toList());
    }

    private void replacePreference(
            List<UpdateNotificationPreferenceDto> preferences,
            NotificationResourceType resourceType,
            NotificationChannel channel,
            boolean enabled
    ) {
        for (int index = 0; index < preferences.size(); index++) {
            UpdateNotificationPreferenceDto preference = preferences.get(index);
            if (preference.resourceType() == resourceType && preference.channel() == channel) {
                preferences.set(index, new UpdateNotificationPreferenceDto(resourceType, channel, enabled));
                return;
            }
        }

        throw new IllegalArgumentException("Preference is not present in the complete matrix.");
    }
}
