package com.mazurek.eventOrganizer.notification;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationRequest;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.notification.domain.DevicePlatform;
import com.mazurek.eventOrganizer.notification.domain.NotificationDevice;
import com.mazurek.eventOrganizer.notification.dto.NotificationDeviceDto;
import com.mazurek.eventOrganizer.notification.dto.RegisterNotificationDeviceDto;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeviceRepository;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.builders.AuthenticationRequestTestBuilder;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.ApiConstants.AUTHORIZATION_HEADER;
import static com.mazurek.eventOrganizer.testData.TestConstants.ApiConstants.NOTIFICATION_DEVICES_URL;
import static com.mazurek.eventOrganizer.testData.TestConstants.AuthConstants.JWT_PREFIX;
import static com.mazurek.eventOrganizer.testData.TestConstants.NotificationDeviceConstants.FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID;
import static com.mazurek.eventOrganizer.testData.TestConstants.NotificationDeviceConstants.SECOND_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.FIRST_USER_EMAIL;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.SECOND_USER_EMAIL;
import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("NotificationDeviceController integration tests:")
class NotificationDeviceControllerIntegrationTest {

    private final AuthenticationRequest firstUserAuthRequest =
            AuthenticationRequestTestBuilder.authenticationRequestForFirstUser().build();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private NotificationDeviceRepository notificationDeviceRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private DeletionService deletionService;

    private UUID firstUserId;
    private UUID secondUserId;
    private String firstUserJwt;

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();

        firstUserId = userRepository.findByIgnoreCaseEmail(FIRST_USER_EMAIL).orElseThrow().getId();
        secondUserId = userRepository.findByIgnoreCaseEmail(SECOND_USER_EMAIL).orElseThrow().getId();
        firstUserJwt = JWT_PREFIX + authenticationService
                .authenticate(firstUserAuthRequest, DeviceType.WEB)
                .getAccessToken();
    }
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
    }

    @Nested
    @DisplayName("Delete current user device tests: DELETE /api/v1/notification-devices/{deviceId}")
    class DeleteCurrentUserDeviceTests {

        @Test
        @DisplayName("When unauthenticated should return HTTP 403 Forbidden")
        void whenUnauthenticatedShouldReturnHttpForbidden() throws Exception {
            deleteDeviceWithoutAuthentication(UUID.randomUUID())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When current user deletes their device should return HTTP 204 and remove it")
        void whenCurrentUserDeletesTheirDeviceShouldReturnHttpNoContentAndRemoveIt() throws Exception {
            NotificationDevice existingDevice = persistDevice(
                    firstUserId,
                    FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID
            );

            deleteDevice(firstUserJwt, existingDevice.getId())
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            assertThat(notificationDeviceRepository.findById(existingDevice.getId())).isEmpty();
        }

        @Test
        @DisplayName("When current user deletes another user's device should return HTTP 204 and leave it unchanged")
        void whenCurrentUserDeletesAnotherUsersDeviceShouldReturnHttpNoContentAndLeaveItUnchanged() throws Exception {
            NotificationDevice existingDevice = persistDevice(
                    secondUserId,
                    SECOND_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID
            );

            deleteDevice(firstUserJwt, existingDevice.getId())
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            assertThat(notificationDeviceRepository.findById(existingDevice.getId()))
                    .hasValueSatisfying(device -> assertThat(device.getUserId()).isEqualTo(secondUserId));
        }

        @Test
        @DisplayName("When current user deletes an unknown device should return HTTP 204")
        void whenCurrentUserDeletesUnknownDeviceShouldReturnHttpNoContent() throws Exception {
            deleteDevice(firstUserJwt, UUID.randomUUID())
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            assertThat(notificationDeviceRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("When device ID is malformed should return HTTP 400 Bad Request")
        void whenDeviceIdIsMalformedShouldReturnHttpBadRequest() throws Exception {
            mockMvc.perform(
                    delete(NOTIFICATION_DEVICES_URL + "/not-a-uuid")
                            .header(AUTHORIZATION_HEADER, firstUserJwt)
            )
                    .andExpect(status().isBadRequest());
        }
    }



    @Nested
    @DisplayName("Register current user device tests: POST /api/v1/notification-devices")
    class RegisterCurrentUserDeviceTests {

        @Test
        @DisplayName("When unauthenticated should return HTTP 403 Forbidden")
        void whenUnauthenticatedShouldReturnHttpForbidden() throws Exception {
            registerDeviceWithoutAuthentication(request(
                    DevicePlatform.ANDROID,
                    FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID
            ))
                    .andExpect(status().isForbidden());
        }

        @ParameterizedTest
        @EnumSource(DevicePlatform.class)
        @DisplayName("When registration uses a supported platform should return HTTP 200 and persist the current user's device")
        void whenRegistrationUsesSupportedPlatformShouldReturnHttpOkAndPersistCurrentUsersDevice(
                DevicePlatform platform
        ) throws Exception {
            MvcResult result = registerDevice(
                    firstUserJwt,
                    request(platform, FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID)
            )
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.platform").value(platform.name()))
                    .andExpect(jsonPath("$.firebaseInstallationId").doesNotExist())
                    .andReturn();

            NotificationDeviceDto response = readDevice(result);
            NotificationDevice storedDevice = notificationDeviceRepository
                    .findByFirebaseInstallationId(FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID)
                    .orElseThrow();

            assertThat(response).isEqualTo(new NotificationDeviceDto(storedDevice));
            assertThat(storedDevice.getUserId()).isEqualTo(firstUserId);
            assertThat(storedDevice.getPlatform()).isEqualTo(platform);
        }

        @Test
        @DisplayName("When a device registers again should return HTTP 200 and retain one device row")
        void whenDeviceRegistersAgainShouldReturnHttpOkAndRetainOneDeviceRow() throws Exception {
            NotificationDeviceDto firstResponse = readDevice(registerDevice(
                    firstUserJwt,
                    request(DevicePlatform.ANDROID, SECOND_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID)
            )
                    .andExpect(status().isOk())
                    .andReturn());

            NotificationDeviceDto secondResponse = readDevice(registerDevice(
                    firstUserJwt,
                    request(DevicePlatform.WEB, SECOND_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID)
            )
                    .andExpect(status().isOk())
                    .andReturn());

            assertThat(secondResponse.id()).isEqualTo(firstResponse.id());
            assertThat(secondResponse.platform()).isEqualTo(DevicePlatform.ANDROID);
            assertThat(notificationDeviceRepository.count()).isOne();
        }

        @Test
        @DisplayName("When installation ID is blank should return HTTP 400 Bad Request")
        void whenInstallationIdIsBlankShouldReturnHttpBadRequest() throws Exception {
            registerDevice(firstUserJwt, request(DevicePlatform.ANDROID, " "))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("When installation ID exceeds 255 characters should return HTTP 400 Bad Request")
        void whenInstallationIdExceeds255CharactersShouldReturnHttpBadRequest() throws Exception {
            registerDevice(firstUserJwt, request(DevicePlatform.ANDROID, "a".repeat(256)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("When installation ID has 255 characters should return HTTP 200 OK")
        void whenInstallationIdHas255CharactersShouldReturnHttpOk() throws Exception {
            registerDevice(firstUserJwt, request(DevicePlatform.ANDROID, "a".repeat(255)))
                    .andExpect(status().isOk());
        }

        @ParameterizedTest
        @ValueSource(strings = {" firebase-installation-id", "firebase-installation-id "})
        @DisplayName("When installation ID has surrounding whitespace should return HTTP 400 Bad Request")
        void whenInstallationIdHasSurroundingWhitespaceShouldReturnHttpBadRequest(
                String firebaseInstallationId
        ) throws Exception {
            registerDevice(firstUserJwt, request(DevicePlatform.ANDROID, firebaseInstallationId))
                    .andExpect(status().isBadRequest());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "{}",
                "{\"platform\":\"ANDROID\"}",
                "{\"firebaseInstallationId\":\"firebase-installation-id\"}",
                "{\"platform\":null,\"firebaseInstallationId\":\"firebase-installation-id\"}",
                "{\"platform\":\"ANDROID\",\"firebaseInstallationId\":null}"
        })
        @DisplayName("When a required registration field is missing or null should return HTTP 400 Bad Request")
        void whenRequiredRegistrationFieldIsMissingOrNullShouldReturnHttpBadRequest(String request) throws Exception {
            registerDeviceRaw(firstUserJwt, request)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("When platform is unsupported should return HTTP 400 Bad Request")
        void whenPlatformIsUnsupportedShouldReturnHttpBadRequest() throws Exception {
            registerDeviceRaw(firstUserJwt, """
                    {"platform":"DESKTOP","firebaseInstallationId":"%s"}
                    """.formatted(FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID))
                    .andExpect(status().isBadRequest());
        }
    }

    private RegisterNotificationDeviceDto request(DevicePlatform platform, String firebaseInstallationId) {
        return new RegisterNotificationDeviceDto(platform, firebaseInstallationId);
    }

    private ResultActions registerDevice(String jwt, RegisterNotificationDeviceDto request) throws Exception {
        return mockMvc.perform(
                post(NOTIFICATION_DEVICES_URL)
                        .header(AUTHORIZATION_HEADER, jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );
    }

    private ResultActions registerDeviceWithoutAuthentication(RegisterNotificationDeviceDto request) throws Exception {
        return mockMvc.perform(
                post(NOTIFICATION_DEVICES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );
    }

    private ResultActions registerDeviceRaw(String jwt, String request) throws Exception {
        return mockMvc.perform(
                post(NOTIFICATION_DEVICES_URL)
                        .header(AUTHORIZATION_HEADER, jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        );
    }

    private ResultActions deleteDevice(String jwt, UUID deviceId) throws Exception {
        return mockMvc.perform(
                delete(NOTIFICATION_DEVICES_URL + "/{deviceId}", deviceId)
                        .header(AUTHORIZATION_HEADER, jwt)
        );
    }

    private ResultActions deleteDeviceWithoutAuthentication(UUID deviceId) throws Exception {
        return mockMvc.perform(delete(NOTIFICATION_DEVICES_URL + "/{deviceId}", deviceId));
    }

    private NotificationDevice persistDevice(UUID userId, String firebaseInstallationId) {
        return notificationDeviceRepository.saveAndFlush(NotificationDevice.builder()
                .userId(userId)
                .platform(DevicePlatform.ANDROID)
                .firebaseInstallationId(firebaseInstallationId)
                .createdAt(NOW)
                .lastSeenAt(NOW)
                .build());
    }

    private NotificationDeviceDto readDevice(MvcResult mvcResult) throws Exception {
        return objectMapper.readValue(
                mvcResult.getResponse().getContentAsString(),
                NotificationDeviceDto.class
        );
    }
}
