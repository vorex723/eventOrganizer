package com.mazurek.eventOrganizer.file;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationRequest;
import com.mazurek.eventOrganizer.exception.ApiErrorCode;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.notification.repository.NotificationRepository;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestDataInitializer;
import com.mazurek.eventOrganizer.testData.builders.AuthenticationRequestTestBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.ApiConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.AuthConstants;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.servlet.multipart.max-file-size=1KB",
                "spring.servlet.multipart.max-request-size=1KB"
        }
)
@DisplayName("File upload size integration tests:")
class FileUploadSizeIntegrationTest {

    private static final String MULTIPART_BOUNDARY = "EventOrganizerFileUploadLimitTest";

    private final AuthenticationRequest firstUserAuthRequest =
            AuthenticationRequestTestBuilder.authenticationRequestForFirstUser().build();

    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private TestDataInitializer testDataInitializer;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private DeletionService deletionService;
    @LocalServerPort
    private int port;

    private UUID eventId;
    private String firstUserJwt;

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();
        eventId = testDataInitializer.setupFirstEvent();
        firstUserJwt = AuthConstants.JWT_PREFIX +
                authenticationService.authenticate(firstUserAuthRequest, DeviceType.WEB).getAccessToken();
    }

    @AfterEach
    void tearDown() {
        deletionService.deleteAllSafe();
    }

    @Test
    @DisplayName("When multipart request exceeds the configured limit should return HTTP 413 error envelope")
    void whenMultipartRequestExceedsConfiguredLimitShouldReturnHttp413ErrorEnvelope()
            throws IOException, InterruptedException {
        long notificationCountBefore = notificationRepository.count();

        HttpRequest request = HttpRequest.newBuilder(fileUploadUri())
                .header("Content-Type", "multipart/form-data; boundary=" + MULTIPART_BOUNDARY)
                .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartRequestBody()))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE.value());
        assertThat(response.body())
                .contains("\"status\":" + HttpStatus.CONTENT_TOO_LARGE.value())
                .contains("\"code\":\"" + ApiErrorCode.FILE_TOO_LARGE + "\"")
                .contains("\"message\":\"Uploaded file exceeds the maximum allowed size.\"")
                .contains("\"errors\":null");
        assertThat(fileRepository.countByEventId(eventId)).isZero();
        assertThat(notificationRepository.count()).isEqualTo(notificationCountBefore);
    }

    private URI fileUploadUri() {
        String path = ApiConstants.EVENT_FILES_URL.replace("{eventId}", eventId.toString());
        return URI.create("http://localhost:" + port + path);
    }

    private byte[] multipartRequestBody() throws IOException {
        try (ByteArrayOutputStream body = new ByteArrayOutputStream()) {
            writePart(body, "userFilename", null, null, "too-large.jpg".getBytes(StandardCharsets.UTF_8));
            writePart(body, "file", "too-large.jpg", "image/jpeg", new byte[2 * 1024]);
            body.write(("--" + MULTIPART_BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return body.toByteArray();
        }
    }

    private void writePart(
            ByteArrayOutputStream body,
            String name,
            String filename,
            String contentType,
            byte[] content
    ) throws IOException {
        body.write(("--" + MULTIPART_BOUNDARY + "\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(("Content-Disposition: form-data; name=\"" + name + "\"").getBytes(StandardCharsets.UTF_8));
        if (filename != null) {
            body.write(("; filename=\"" + filename + "\"").getBytes(StandardCharsets.UTF_8));
        }
        body.write("\r\n".getBytes(StandardCharsets.UTF_8));
        if (contentType != null) {
            body.write(("Content-Type: " + contentType + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
        body.write("\r\n".getBytes(StandardCharsets.UTF_8));
        body.write(content);
        body.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }
}
