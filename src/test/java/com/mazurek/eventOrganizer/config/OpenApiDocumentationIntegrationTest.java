package com.mazurek.eventOrganizer.config;

import com.mazurek.eventOrganizer.auth.AuthenticationController;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.city.CityController;
import com.mazurek.eventOrganizer.city.CityService;
import com.mazurek.eventOrganizer.config.properties.AuthProperties;
import com.mazurek.eventOrganizer.conversation.ConversationController;
import com.mazurek.eventOrganizer.conversation.ConversationService;
import com.mazurek.eventOrganizer.event.EventController;
import com.mazurek.eventOrganizer.event.EventService;
import com.mazurek.eventOrganizer.file.FileController;
import com.mazurek.eventOrganizer.file.FileService;
import com.mazurek.eventOrganizer.jwt.JwtRequestFilter;
import com.mazurek.eventOrganizer.notification.NotificationController;
import com.mazurek.eventOrganizer.notification.NotificationDeviceController;
import com.mazurek.eventOrganizer.notification.service.NotificationDeviceService;
import com.mazurek.eventOrganizer.notification.service.NotificationPreferenceService;
import com.mazurek.eventOrganizer.notification.service.NotificationQueryService;
import com.mazurek.eventOrganizer.tag.TagController;
import com.mazurek.eventOrganizer.tag.TagService;
import com.mazurek.eventOrganizer.thread.ThreadController;
import com.mazurek.eventOrganizer.thread.ThreadService;
import com.mazurek.eventOrganizer.threadReply.ThreadReplyController;
import com.mazurek.eventOrganizer.threadReply.ThreadReplyService;
import com.mazurek.eventOrganizer.user.AccountDeletionService;
import com.mazurek.eventOrganizer.user.UserController;
import com.mazurek.eventOrganizer.user.UserService;
import com.mazurek.eventOrganizer.utils.DeviceTypeResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AuthenticationController.class,
        CityController.class,
        ConversationController.class,
        EventController.class,
        FileController.class,
        NotificationController.class,
        NotificationDeviceController.class,
        TagController.class,
        ThreadController.class,
        ThreadReplyController.class,
        UserController.class
})
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({SpringDocConfiguration.class, SpringDocWebMvcConfiguration.class})
@EnableConfigurationProperties(SpringDocConfigProperties.class)
@Import(OpenApiConfig.class)
@TestPropertySource(properties = "springdoc.api-docs.enabled=true")
class OpenApiDocumentationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private AuthProperties authProperties;

    @MockitoBean
    private DeviceTypeResolver deviceTypeResolver;

    @MockitoBean
    private CityService cityService;

    @MockitoBean
    private ConversationService conversationService;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private FileService fileService;

    @MockitoBean
    private NotificationDeviceService notificationDeviceService;

    @MockitoBean
    private NotificationPreferenceService notificationPreferenceService;

    @MockitoBean
    private NotificationQueryService notificationQueryService;

    @MockitoBean
    private TagService tagService;

    @MockitoBean
    private ThreadService threadService;

    @MockitoBean
    private ThreadReplyService threadReplyService;

    @MockitoBean
    private AccountDeletionService accountDeletionService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtRequestFilter jwtRequestFilter;

    @Test
    void generatedDocumentMatchesPublicSecurityAndNonDefaultSuccessContracts() throws Exception {
        JsonNode openApi = getOpenApiDocument();

        assertThat(openApi.path("security").get(0).path("bearerAuth").isArray()).isTrue();
        assertThat(operation(openApi, "/api/v1/auth/login", "post").path("security").isEmpty()).isTrue();
        for (String path : new String[]{
                "/api/v1/events",
                "/api/v1/events/{eventId}",
                "/api/v1/cities/{cityName}",
                "/api/v1/cities/{cityName}/events",
                "/api/v1/tags/{tagName}",
                "/api/v1/tags/{tagName}/events"
        }) {
            JsonNode publicOperation = operation(openApi, path, "get");
            assertThat(publicOperation.path("security").isEmpty()).as(path).isTrue();
            assertThat(publicOperation.path("responses").has("401")).as(path).isFalse();
            assertThat(publicOperation.path("responses").has("403")).as(path).isFalse();
        }
        assertThat(operation(openApi, "/api/v1/events", "post").path("security").isMissingNode()).isTrue();
        assertThat(operation(openApi, "/api/v1/events/{eventId}/attendees", "get")
                .path("security").isMissingNode()).isTrue();
        assertThat(operation(openApi, "/api/v1/events/{eventId}/attendees", "get")
                .path("responses").has("401")).isTrue();

        assertSuccessResponse(openApi, "/api/v1/auth/register", "post", "201");
        assertSuccessResponse(openApi, "/api/v1/auth/logout", "post", "204");
        assertSuccessResponse(openApi, "/api/v1/auth/activate", "post", "204");
        assertSuccessResponse(openApi, "/api/v1/auth/password-reset", "post", "204");
        assertSuccessResponse(openApi, "/api/v1/auth/password-reset/{tokenId}", "post", "204");
        assertRedirectResponse(openApi, "/api/v1/auth/activate/{tokenId}");
        assertRedirectResponse(openApi, "/api/v1/auth/change-email/{tokenId}");

        assertSuccessResponse(openApi, "/api/v1/events", "post", "201");
        assertSuccessResponse(openApi, "/api/v1/events/{eventId}/attendees", "get", "200");
        assertSuccessResponse(openApi, "/api/v1/events/{eventId}/attend", "post", "204");
        assertSuccessResponse(openApi, "/api/v1/events/{eventId}/attend", "delete", "204");
        assertSuccessResponse(openApi, "/api/v1/events/{eventId}/threads", "post", "201");
        assertSuccessResponse(openApi, "/api/v1/events/{eventId}/threads/{threadId}/replies", "post", "201");

        assertSuccessResponse(openApi, "/api/v1/conversations/direct", "post", "201");
        assertSuccessResponse(openApi, "/api/v1/conversations/{conversationId}/messages", "post", "201");
        assertSuccessResponse(openApi, "/api/v1/conversations/{conversationId}/read", "post", "204");
        assertSuccessResponse(openApi, "/api/v1/notifications/read-all", "patch", "204");
        assertSuccessResponse(openApi, "/api/v1/notifications/{notificationId}/read", "patch", "204");
        assertSuccessResponse(openApi, "/api/v1/notification-devices/{deviceId}", "delete", "204");
        assertSuccessResponse(openApi, "/api/v1/users/me", "delete", "204");
        assertSuccessResponse(openApi, "/api/v1/users/change-email", "put", "202");
    }

    @Test
    void generatedDocumentDescribesMultipartUploadBinaryDownloadAndSharedErrors() throws Exception {
        JsonNode openApi = getOpenApiDocument();
        JsonNode upload = operation(openApi, "/api/v1/events/{eventId}/files", "post");

        assertThat(upload.path("requestBody").path("content").has("multipart/form-data")).isTrue();
        assertSuccessResponse(openApi, "/api/v1/events/{eventId}/files", "post", "201");
        assertThat(upload.path("responses").has("409")).isTrue();
        assertThat(upload.path("responses").has("413")).isTrue();
        assertThat(upload.path("responses").has("415")).isTrue();
        assertThat(upload.path("responses").path("413").path("content")
                .has("application/json")).isTrue();

        JsonNode downloadResponse = operation(openApi, "/api/v1/events/{eventId}/files/{fileId}/data", "get")
                .path("responses").path("200");
        assertThat(downloadResponse.path("content").path("*/*")
                .path("schema").path("format").asString()).isEqualTo("binary");

        JsonNode apiError = openApi.path("components").path("schemas").path("ApiError").path("properties");
        assertThat(apiError.has("status")).isTrue();
        assertThat(apiError.has("code")).isTrue();
        assertThat(apiError.has("message")).isTrue();
        assertThat(apiError.has("errors")).isTrue();
    }

    @Test
    void generatedDocumentKeepsAttendeeIdentitiesOutOfEventsAndDocumentsTheirPage() throws Exception {
        JsonNode schemas = getOpenApiDocument().path("components").path("schemas");
        JsonNode eventProperties = schemas.path("EventDto").path("properties");
        JsonNode overviewProperties = schemas.path("EventOverviewDto").path("properties");
        JsonNode attendeePageProperties = schemas.path("EventAttendeePageDto").path("properties");

        assertThat(eventProperties.has("amountOfAttenders")).isTrue();
        assertThat(eventProperties.has("attendingUsers")).isFalse();
        assertThat(overviewProperties.has("timeZone")).isTrue();
        assertThat(attendeePageProperties.has("attendees")).isTrue();
        assertThat(attendeePageProperties.has("totalElements")).isTrue();
    }

    @Test
    void generatedDocumentDescribesCurrentUserAndAccountDeletion() throws Exception {
        JsonNode openApi = getOpenApiDocument();
        JsonNode schemas = openApi.path("components").path("schemas");

        operation(openApi, "/api/v1/users/me", "get");
        operation(openApi, "/api/v1/users/me", "delete");
        assertThat(schemas.path("CurrentUserDto").path("properties").has("email")).isTrue();
        assertThat(schemas.path("CurrentUserDto").path("properties").has("timeZone")).isTrue();
        assertThat(schemas.path("DeleteCurrentUserDto").path("properties").has("password")).isTrue();
    }

    private JsonNode getOpenApiDocument() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private JsonNode operation(JsonNode openApi, String path, String method) {
        JsonNode operation = openApi.path("paths").path(path).path(method);
        assertThat(operation.isMissingNode()).isFalse();
        return operation;
    }

    private void assertSuccessResponse(JsonNode openApi, String path, String method, String expectedStatus) {
        JsonNode responses = operation(openApi, path, method).path("responses");
        assertThat(responses.has(expectedStatus)).isTrue();
        if (!expectedStatus.equals("200")) {
            assertThat(responses.has("200")).isFalse();
        }
    }

    private void assertRedirectResponse(JsonNode openApi, String path) {
        JsonNode responses = operation(openApi, path, "get").path("responses");

        assertThat(responses.has("303")).isTrue();
        assertThat(responses.has("200")).isFalse();
        assertThat(responses.path("303").path("headers").path("Location").path("schema")
                .path("format").asString()).isEqualTo("uri");
    }
}
