package com.mazurek.eventOrganizer.contract;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationRequest;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationResponse;
import com.mazurek.eventOrganizer.conversation.dto.ConversationDetailsDto;
import com.mazurek.eventOrganizer.conversation.dto.DirectMessageResponseDto;
import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.dto.EventDto;
import com.mazurek.eventOrganizer.exception.ApiErrorCode;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.notification.dto.UpdateNotificationPreferenceDto;
import com.mazurek.eventOrganizer.notification.repository.NotificationRepository;
import com.mazurek.eventOrganizer.notification.service.RecordingEmailService;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestDataInitializer;
import com.mazurek.eventOrganizer.testData.builders.AuthenticationRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.MarkConversationReadDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.NotificationTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UpdateNotificationPreferenceDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UpdateNotificationPreferencesDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.EventCreateDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.MultipartFileTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.RefreshTokenRequestTestBuilder;
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
import org.springframework.test.context.ActiveProfiles;
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

import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static com.mazurek.eventOrganizer.testData.TestConstants.ApiConstants.*;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A compact regression suite for the API contract consumed by the frontend.
 *
 * <p>Feature-specific integration tests retain exhaustive behavioural coverage. This class protects
 * the stable status codes and JSON fields that the frontend depends on across those workflows.</p>
 */
@ActiveProfiles("test")
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
    private RecordingEmailService emailService;

    @Autowired
    private com.mazurek.eventOrganizer.testData.TestPersistenceQueries persistenceQueries;

    private User firstUser;
    private User secondUser;
    private String firstUserJwt;
    private String secondUserJwt;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
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
    void whenLoggingInAndRefreshingShouldExposeFrontendSessionContract() throws Exception {
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
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequestTestBuilder()
                                .refreshToken(login.getRefreshToken())
                                .firebaseInstallationId(null)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.accessTokenExpiration").isNumber());
    }

    @Test
    void whenRegisteringAndActivatingShouldUseExplicitPostJsonContract() throws Exception {
        mockMvc.perform(post(AUTH_REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                RegisterRequestTestBuilder.thirdUserRegisterRequest().build()
                        )))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").isNotEmpty());

        mockMvc.perform(post(AUTH_ACTIVATE_URL, emailService.lastActivationToken(THIRD_USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("activated"))
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void whenReadingCurrentUserProfileShouldExposePrivateFieldsWithoutAuthenticationData() throws Exception {
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
    void whenReadingOtherUserProfileShouldExposeOnlyIdentityFields() throws Exception {
        mockMvc.perform(get(USER_BY_ID_URL, secondUser.getId())
                        .header(AUTHORIZATION_HEADER, firstUserJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(secondUser.getId().toString()))
                .andExpect(jsonPath("$.firstName").value(SECOND_USER_FIRST_NAME))
                .andExpect(jsonPath("$.lastName").value(SECOND_USER_LAST_NAME))
                .andExpect(jsonPath("$.homeCity").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.email").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.timeZone").doesNotHaveJsonPath());
    }

    @Test
    void whenReadingEventAnonymouslyShouldHideOwnersHomeCity() throws Exception {
        UUID eventId = testDataInitializer.setupFirstEvent();
        SecurityContextHolder.clearContext();

        mockMvc.perform(get(EVENT_BY_ID_URL, eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner.id").value(firstUser.getId().toString()))
                .andExpect(jsonPath("$.owner.firstName").value(FIRST_USER_FIRST_NAME))
                .andExpect(jsonPath("$.owner.lastName").value(FIRST_USER_LAST_NAME))
                .andExpect(jsonPath("$.owner.homeCity").doesNotHaveJsonPath());

        mockMvc.perform(get(EVENTS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].id").value(eventId.toString()))
                .andExpect(jsonPath("$.events[0].owner.id").value(firstUser.getId().toString()))
                .andExpect(jsonPath("$.events[0].owner.homeCity").doesNotHaveJsonPath());
    }

    @Test
    void whenCreatingUpdatingAndListingEventsShouldPreserveFrontendContract() throws Exception {
        EventCreateDto createRequest = EventCreateDtoTestBuilder.firstEvent().build();
        MvcResult createResult = mockMvc.perform(post(EVENTS_URL)
                        .header(AUTHORIZATION_HEADER, firstUserJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value(createRequest.getName()))
                .andExpect(jsonPath("$.timeZone").value(com.mazurek.eventOrganizer.testData.TestCityData.timeZoneId(createRequest.getCityExternalId())))
                .andExpect(jsonPath("$.maxAttendees").value(createRequest.getMaxAttendees()))
                .andExpect(jsonPath("$.attendeeCount").value(0))
                .andReturn();

        EventDto createdEvent = objectMapper.readValue(createResult.getResponse().getContentAsString(), EventDto.class);
        assertThat(createdEvent).isNotNull();
        assertThat(createdEvent.getId()).isNotNull();
        EventCreateDto updateRequest = EventCreateDtoTestBuilder.updatedEvent().build();

        mockMvc.perform(put(EVENT_BY_ID_URL, createdEvent.getId())
                        .header(AUTHORIZATION_HEADER, firstUserJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(createdEvent.getId().toString()))
                .andExpect(jsonPath("$.name").value(updateRequest.getName()))
                .andExpect(jsonPath("$.timeZone").value(com.mazurek.eventOrganizer.testData.TestCityData.timeZoneId(updateRequest.getCityExternalId())))
                .andExpect(jsonPath("$.attendeeCount").value(0));

        mockMvc.perform(get(EVENTS_URL)
                        .param("page", "0")
                        .header(AUTHORIZATION_HEADER, firstUserJwt))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.events[0].id").value(createdEvent.getId().toString()))
                .andExpect(jsonPath("$.events[0].name").value(updateRequest.getName()))
                .andExpect(jsonPath("$.events[0].attendeeCount").value(0))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").isNumber())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.lastPage").value(true));
    }

    @Test
    void whenUsingConversationShouldExposeMessagesAndRequireReadAcknowledgement() throws Exception {
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
                                new MarkConversationReadDtoTestBuilder()
                                        .lastReadMessageId(directMessage.message().getId())
                                        .build()
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

        var secondParticipant = requirePresent(details.participants().stream()
                .filter(participant -> secondUser.getId().equals(participant.userId()))
                .findFirst(), "Expected frontend prerequisite in whenUsingConversationShouldExposeMessagesAndRequireReadAcknowledgement");
        assertThat(secondParticipant.lastReadAt()).isEqualTo(com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants.NOW);
        assertThat(secondParticipant.lastReadMessageId()).isEqualTo(directMessage.message().getId());
    }

    @Test
    void whenUpdatingNotificationPreferencesAndReadStateShouldExposeCurrentUserState() throws Exception {
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
                        .content(objectMapper.writeValueAsString(new UpdateNotificationPreferencesDtoTestBuilder()
                                .version(0L)
                                .preferences(preferences)
                                .build())))
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

        assertThat(requirePresent(notificationRepository.findById(notificationId), "Expected frontend prerequisite in whenUpdatingNotificationPreferencesAndReadStateShouldExposeCurrentUserState").getReadAt()).isEqualTo(com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants.NOW);
    }

    @Test
    void whenUploadingMultipartFileShouldReturnCreatedContract() throws Exception {
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
    void whenClientRequestsFailShouldShareApiErrorEnvelope() throws Exception {
        var beforeReads = persistenceQueries.notificationState();
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

        assertThat(persistenceQueries.notificationState()).isEqualTo(beforeReads);
        UUID eventId = testDataInitializer.setupFirstEvent();
        var beforeRejectedWrites = java.util.Map.of("events", persistenceQueries.fileState(),
                "notifications", persistenceQueries.notificationState());
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
                                new UpdateNotificationPreferencesDtoTestBuilder()
                                        .version(1L)
                                        .preferences(mutableDefaultPreferenceMatrix())
                                        .build()
                        ))),
                HttpStatus.CONFLICT,
                ApiErrorCode.STALE_NOTIFICATION_PREFERENCES
        );
        assertThat(java.util.Map.of("events", persistenceQueries.fileState(),
                "notifications", persistenceQueries.notificationState())).isEqualTo(beforeRejectedWrites);
    }

    private String jwtFor(AuthenticationRequest credentials) {
        return JWT_PREFIX + authenticationService.authenticate(credentials, DeviceType.WEB).getAccessToken();
    }

    private User userByEmail(String email) {
        return requirePresent(userRepository.findByIgnoreCaseEmail(email), "Expected frontend prerequisite in userByEmail");
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
                        .map(channel -> new UpdateNotificationPreferenceDtoTestBuilder()
                                .resourceType(resourceType)
                                .channel(channel)
                                .enabled(channel != NotificationChannel.EMAIL)
                                .build()))
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
                preferences.set(index, new UpdateNotificationPreferenceDtoTestBuilder()
                        .resourceType(resourceType)
                        .channel(channel)
                        .enabled(enabled)
                        .build());
                return;
            }
        }

        throw new IllegalArgumentException("Preference is not present in the complete matrix.");
    }
}
