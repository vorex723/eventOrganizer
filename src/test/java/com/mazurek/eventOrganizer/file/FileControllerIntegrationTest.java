package com.mazurek.eventOrganizer.file;

import com.mazurek.eventOrganizer.exception.ApiErrorCode;
import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationRequest;
import com.mazurek.eventOrganizer.event.EventService;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotEventAttendeeException;
import com.mazurek.eventOrganizer.exception.file.EmptyUploadedFileException;
import com.mazurek.eventOrganizer.exception.file.FileNotFoundInEventException;
import com.mazurek.eventOrganizer.exception.file.FileTypeNotAllowedException;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestDataInitializer;
import com.mazurek.eventOrganizer.testData.TestPersistenceQueries;
import com.mazurek.eventOrganizer.testData.builders.AuthenticationRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.MultipartFileTestBuilder;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("FileController integration tests:")
public class FileControllerIntegrationTest {

    private final AuthenticationRequest firstUserAuthRequest =
            AuthenticationRequestTestBuilder.authenticationRequestForFirstUser().build();
    private final AuthenticationRequest secondUserAuthRequest =
            AuthenticationRequestTestBuilder.authenticationRequestForSecondUser().build();

    @Autowired
    private EventService eventService;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private TestDataInitializer testDataInitializer;
    @Autowired
    private DeletionService deletionService;
    @Autowired
    private TestPersistenceQueries persistenceQueries;

    private UUID savedEventId;
    private String firstUserJwt;
    private String secondUserJwt;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();
        savedEventId = testDataInitializer.setupFirstEvent();

        firstUserJwt = generateJwt(firstUserAuthRequest);
        secondUserJwt = generateJwt(secondUserAuthRequest);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
    }

    private String generateJwt(AuthenticationRequest authRequest) {
        return AuthConstants.JWT_PREFIX +
                authenticationService.authenticate(authRequest, DeviceType.WEB).getAccessToken();
    }

    private void addSecondUserAsEventAttendee() {
        authHelper.setupSecurityContextForSecondUser();
        eventService.addAttendeeToEvent(savedEventId);
        SecurityContextHolder.clearContext();
    }

    private MockMultipartFile validJpgMultipartFile() {
        return MultipartFileTestBuilder.jpgFile().buildMultipartFile();
    }

    // ===========================================================================================
    // GET /api/v1/events/{eventId}/files
    // ===========================================================================================

    @Nested
    @DisplayName("Get file overview page tests: GET /api/v1/events/{eventId}/files")
    class GetFileOverviewPageTests {

        @Test
        @DisplayName("When getting file overview page should return HTTP 401 Unauthorized if there is no Authorization header")
        public void whenGettingFileOverviewPageShouldReturnUnauthorizedIfThereIsNoAuthorizationHeader() throws Exception {
            var beforeRead = persistenceQueries.fileState();

            mockMvc.perform(get(ApiConstants.EVENT_FILES_URL, savedEventId))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));
            assertThat(persistenceQueries.fileState())
                    .as("File reads must preserve stored bytes, relationships and outbox")
                    .isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting file overview page should return HTTP 404 Not Found if event does not exist")
        public void whenGettingFileOverviewPageShouldReturnNotFoundIfEventDoesNotExist() throws Exception {
            var beforeRead = persistenceQueries.fileState();

            mockMvc.perform(get(ApiConstants.EVENT_FILES_URL, EventConstants.NOT_EXISTING_EVENT_ID)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.EVENT_NOT_FOUND))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(EventNotFoundException.DEFAULT_MESSAGE));
            assertThat(persistenceQueries.fileState())
                    .as("File reads must preserve stored bytes, relationships and outbox")
                    .isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting file overview page should return HTTP 403 Forbidden if performing user is not attending event")
        public void whenGettingFileOverviewPageShouldReturnForbiddenIfPerformingUserIsNotAttendingEvent() throws Exception {
            var beforeRead = persistenceQueries.fileState();

            mockMvc.perform(get(ApiConstants.EVENT_FILES_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.NOT_EVENT_ATTENDEE))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.message").value(NotEventAttendeeException.DEFAULT_MESSAGE));
            assertThat(persistenceQueries.fileState())
                    .as("File reads must preserve stored bytes, relationships and outbox")
                    .isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting file overview page should return HTTP 200 OK with empty page if no files exist in event")
        public void whenGettingFileOverviewPageShouldReturnOkWithEmptyPageIfNoFilesExistInEvent() throws Exception {
            var beforeRead = persistenceQueries.fileState();

            mockMvc.perform(get(ApiConstants.EVENT_FILES_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.fileOverviews").isArray())
                    .andExpect(jsonPath("$.fileOverviews").isEmpty())
                    .andExpect(jsonPath("$.pageNumber").value(PaginationConstants.PAGE_ZERO))
                    .andExpect(jsonPath("$.pageSize").value(PaginationConstants.FILE_PAGE_SIZE))
                    .andExpect(jsonPath("$.totalPages").value(0))
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.lastPage").value(true));
            assertThat(persistenceQueries.fileState())
                    .as("File reads must preserve stored bytes, relationships and outbox")
                    .isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting file overview page should return HTTP 200 OK with correct page data when files exist")
        public void whenGettingFileOverviewPageShouldReturnOkWithCorrectPageDataWhenFilesExist() throws Exception {
            UUID savedFileId = testDataInitializer.setupFileInEvent(savedEventId);
            File expectedFile = requirePresent(fileRepository.findById(savedFileId), "Expected uploaded file for page assertions");
            assertThat(expectedFile.getOwner()).isNotNull();

            var beforeRead = persistenceQueries.fileState();

            mockMvc.perform(get(ApiConstants.EVENT_FILES_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.fileOverviews").isArray())
                    .andExpect(jsonPath("$.fileOverviews.length()").value(1))
                    .andExpect(jsonPath("$.fileOverviews[0].id").value(savedFileId.toString()))
                    .andExpect(jsonPath("$.fileOverviews[0].userFilename").value(expectedFile.getUserFileName()))
                    .andExpect(jsonPath("$.fileOverviews[0].originalFilename").value(expectedFile.getOriginalFileName()))
                    .andExpect(jsonPath("$.fileOverviews[0].fileContentType").value(expectedFile.getContentType()))
                    .andExpect(jsonPath("$.fileOverviews[0].uploadDateTime").value(expectedFile.getUploadDateTime().toString()))
                    .andExpect(jsonPath("$.fileOverviews[0].owner.id").value(expectedFile.getOwner().getId().toString()))
                    .andExpect(jsonPath("$.fileOverviews[0].owner.homeCity").doesNotHaveJsonPath())
                    .andExpect(jsonPath("$.pageNumber").value(PaginationConstants.PAGE_ZERO))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.lastPage").value(true))
                    .andExpect(jsonPath("$.pageSize").value(PaginationConstants.FILE_PAGE_SIZE));
            assertThat(persistenceQueries.fileState())
                    .as("File reads must preserve stored bytes, relationships and outbox")
                    .isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting file overview page should return HTTP 200 OK with requested page number when page param is provided")
        public void whenGettingFileOverviewPageShouldReturnOkWithRequestedPageNumberWhenPageParamIsProvided() throws Exception {
            var beforeRead = persistenceQueries.fileState();

            mockMvc.perform(get(ApiConstants.EVENT_FILES_URL, savedEventId)
                            .param("page", "0")
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pageNumber").value(0));
            assertThat(persistenceQueries.fileState())
                    .as("File reads must preserve stored bytes, relationships and outbox")
                    .isEqualTo(beforeRead);
        }
    }

    // ===========================================================================================
    // GET /api/v1/events/{eventId}/files/{fileId}
    // ===========================================================================================

    @Nested
    @DisplayName("Get file overview by id tests: GET /api/v1/events/{eventId}/files/{fileId}")
    class GetFileOverviewByIdTests {

        private UUID savedFileId;

        @BeforeEach
        void setUp() throws Exception {
            savedFileId = testDataInitializer.setupFileInEvent(savedEventId);
        }

        @Test
        @DisplayName("When getting file overview by id should return HTTP 401 Unauthorized if there is no Authorization header")
        public void whenGettingFileOverviewByIdShouldReturnUnauthorizedIfThereIsNoAuthorizationHeader() throws Exception {
            var beforeRead = persistenceQueries.fileState();

            mockMvc.perform(get(ApiConstants.EVENT_FILE_BY_ID_URL, savedEventId, savedFileId))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));
            assertThat(persistenceQueries.fileState())
                    .as("File reads must preserve stored bytes, relationships and outbox")
                    .isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting file overview by id should return HTTP 404 Not Found if event does not exist")
        public void whenGettingFileOverviewByIdShouldReturnNotFoundIfEventDoesNotExist() throws Exception {
            var beforeRead = persistenceQueries.fileState();

            mockMvc.perform(get(ApiConstants.EVENT_FILE_BY_ID_URL, EventConstants.NOT_EXISTING_EVENT_ID, savedFileId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.EVENT_NOT_FOUND))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(EventNotFoundException.DEFAULT_MESSAGE));
            assertThat(persistenceQueries.fileState())
                    .as("File reads must preserve stored bytes, relationships and outbox")
                    .isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting file overview by id should return HTTP 403 Forbidden if performing user is not attending event")
        public void whenGettingFileOverviewByIdShouldReturnForbiddenIfPerformingUserIsNotAttendingEvent() throws Exception {
            var beforeRead = persistenceQueries.fileState();

            mockMvc.perform(get(ApiConstants.EVENT_FILE_BY_ID_URL, savedEventId, savedFileId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.NOT_EVENT_ATTENDEE))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.message").value(NotEventAttendeeException.DEFAULT_MESSAGE));
            assertThat(persistenceQueries.fileState())
                    .as("File reads must preserve stored bytes, relationships and outbox")
                    .isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting file overview by id should return HTTP 404 Not Found if file does not exist in event")
        public void whenGettingFileOverviewByIdShouldReturnNotFoundIfFileDoesNotExistInEvent() throws Exception {
            var beforeRead = persistenceQueries.fileState();

            mockMvc.perform(get(ApiConstants.EVENT_FILE_BY_ID_URL, savedEventId, FileConstants.NOT_EXISTING_FILE_ID)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.FILE_NOT_FOUND_IN_EVENT))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(FileNotFoundInEventException.DEFAULT_MESSAGE));
            assertThat(persistenceQueries.fileState())
                    .as("File reads must preserve stored bytes, relationships and outbox")
                    .isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting file overview by id should return HTTP 404 Not Found if file belongs to different event")
        public void whenGettingFileOverviewByIdShouldReturnNotFoundIfFileBelongsToDifferentEvent() throws Exception {
            UUID secondEventId = testDataInitializer.setupEventByFirstUser();
            UUID fileFromSecondEventId = testDataInitializer.setupFileInEvent(secondEventId);

            var beforeRead = persistenceQueries.fileState();

            mockMvc.perform(get(ApiConstants.EVENT_FILE_BY_ID_URL, savedEventId, fileFromSecondEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.FILE_NOT_FOUND_IN_EVENT))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(FileNotFoundInEventException.DEFAULT_MESSAGE));
            assertThat(persistenceQueries.fileState())
                    .as("File reads must preserve stored bytes, relationships and outbox")
                    .isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting file overview by id should return HTTP 200 OK with correct data")
        public void whenGettingFileOverviewByIdShouldReturnOkWithCorrectData() throws Exception {
            File file = requirePresent(
                    fileRepository.findById(savedFileId),
                    "Expected file to exist before overview lookup");

            assertThat(file.getOwner()).isNotNull();
            var beforeRead = persistenceQueries.fileState();

            mockMvc.perform(get(ApiConstants.EVENT_FILE_BY_ID_URL, savedEventId, savedFileId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(savedFileId.toString()))
                    .andExpect(jsonPath("$.userFilename").value(file.getUserFileName()))
                    .andExpect(jsonPath("$.originalFilename").value(file.getOriginalFileName()))
                    .andExpect(jsonPath("$.fileContentType").value(file.getContentType()))
                    .andExpect(jsonPath("$.owner.id").value(file.getOwner().getId().toString()))
                    .andExpect(jsonPath("$.owner.homeCity").doesNotHaveJsonPath())
                    .andExpect(jsonPath("$.uploadDateTime").value(file.getUploadDateTime().toString()));
            assertThat(persistenceQueries.fileState())
                    .as("File reads must preserve stored bytes, relationships and outbox")
                    .isEqualTo(beforeRead);
        }
    }

    // ===========================================================================================
    // POST /api/v1/events/{eventId}/files
    // ===========================================================================================

    @Nested
    @DisplayName("Upload file tests: POST /api/v1/events/{eventId}/files")
    class UploadFileTests {

        @Test
        @DisplayName("When uploading file should return HTTP 401 Unauthorized if there is no Authorization header")
        public void whenUploadingFileShouldReturnUnauthorizedIfThereIsNoAuthorizationHeader() throws Exception {
            var beforeWrite = persistenceQueries.fileState();

            mockMvc.perform(multipart(ApiConstants.EVENT_FILES_URL, savedEventId)
                            .file(validJpgMultipartFile())
                            .param("userFilename", FileConstants.USER_FILE_NAME))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));

            assertThat(persistenceQueries.fileState())
                    .as("Rejected upload must preserve file bytes, relationships and outbox")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When uploading file should return HTTP 404 Not Found if event does not exist")
        public void whenUploadingFileShouldReturnNotFoundIfEventDoesNotExist() throws Exception {
            var beforeWrite = persistenceQueries.fileState();

            mockMvc.perform(multipart(ApiConstants.EVENT_FILES_URL, EventConstants.NOT_EXISTING_EVENT_ID)
                            .file(validJpgMultipartFile())
                            .param("userFilename", FileConstants.USER_FILE_NAME)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.EVENT_NOT_FOUND))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(EventNotFoundException.DEFAULT_MESSAGE));

            assertThat(persistenceQueries.fileState())
                    .as("Rejected upload must preserve file bytes, relationships and outbox")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When uploading file should return HTTP 403 Forbidden if performing user is not attending event")
        public void whenUploadingFileShouldReturnForbiddenIfPerformingUserIsNotAttendingEvent() throws Exception {
            var beforeWrite = persistenceQueries.fileState();

            mockMvc.perform(multipart(ApiConstants.EVENT_FILES_URL, savedEventId)
                            .file(validJpgMultipartFile())
                            .param("userFilename", FileConstants.USER_FILE_NAME)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.NOT_EVENT_ATTENDEE))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.message").value(NotEventAttendeeException.DEFAULT_MESSAGE));

            assertThat(persistenceQueries.fileState())
                    .as("Rejected upload must preserve file bytes, relationships and outbox")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When uploading file should return HTTP 400 Bad Request if file is empty")
        public void whenUploadingFileShouldReturnBadRequestIfFileIsEmpty() throws Exception {
            MockMultipartFile emptyFile = MultipartFileTestBuilder.jpgFile()
                    .content(new byte[0])
                    .buildMultipartFile();

            var beforeWrite = persistenceQueries.fileState();

            mockMvc.perform(multipart(ApiConstants.EVENT_FILES_URL, savedEventId)
                            .file(emptyFile)
                            .param("userFilename", FileConstants.USER_FILE_NAME)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.EMPTY_UPLOADED_FILE))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.message").value(EmptyUploadedFileException.DEFAULT_MESSAGE));

            assertThat(persistenceQueries.fileState())
                    .as("Rejected upload must preserve file bytes, relationships and outbox")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When uploading file should return HTTP 415 Unsupported Media Type if file type is not on whitelist")
        public void whenUploadingFileShouldReturnUnsupportedMediaTypeIfFileTypeIsNotOnWhitelist() throws Exception {
            MockMultipartFile malwareFile = MultipartFileTestBuilder.malwareFile().buildMultipartFile();

            var beforeWrite = persistenceQueries.fileState();

            mockMvc.perform(multipart(ApiConstants.EVENT_FILES_URL, savedEventId)
                            .file(malwareFile)
                            .param("userFilename", FileConstants.MALWARE_FILE_USER_FILE_NAME)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.FILE_TYPE_NOT_ALLOWED))
                    .andExpect(jsonPath("$.status").value(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()))
                    .andExpect(jsonPath("$.message").value(FileTypeNotAllowedException.DEFAULT_MESSAGE));

            assertThat(persistenceQueries.fileState())
                    .as("Rejected upload must preserve file bytes, relationships and outbox")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When uploading file should return HTTP 400 Bad Request if user filename is blank")
        public void whenUploadingFileShouldReturnBadRequestIfUserFilenameIsBlank() throws Exception {
            var beforeWrite = persistenceQueries.fileState();

            mockMvc.perform(multipart(ApiConstants.EVENT_FILES_URL, savedEventId)
                            .file(validJpgMultipartFile())
                            .param("userFilename", InvalidInputConstants.BLANK_VALUE)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_FAILED))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.errors.userFilename").hasJsonPath());

            assertThat(persistenceQueries.fileState())
                    .as("Rejected upload must preserve file bytes, relationships and outbox")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When uploading file should return HTTP 400 Bad Request if user filename is too long")
        public void whenUploadingFileShouldReturnBadRequestIfUserFilenameIsTooLong() throws Exception {
            var beforeWrite = persistenceQueries.fileState();

            mockMvc.perform(multipart(ApiConstants.EVENT_FILES_URL, savedEventId)
                            .file(validJpgMultipartFile())
                            .param("userFilename", FileConstants.WRONG_USER_FILE_NAME_TOO_LONG)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_FAILED))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.errors.userFilename").hasJsonPath());

            assertThat(persistenceQueries.fileState())
                    .as("Rejected upload must preserve file bytes, relationships and outbox")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When uploading file should return HTTP 400 Bad Request if file is missing")
        public void whenUploadingFileShouldReturnBadRequestIfFileIsMissing() throws Exception {
            var beforeWrite = persistenceQueries.fileState();

            mockMvc.perform(multipart(ApiConstants.EVENT_FILES_URL, savedEventId)
                            .param("userFilename", FileConstants.USER_FILE_NAME)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_FAILED))
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.errors.file").hasJsonPath());

            assertThat(persistenceQueries.fileState())
                    .as("Rejected upload must preserve file bytes, relationships and outbox")
                    .isEqualTo(beforeWrite);
        }

        @Test
        @DisplayName("When uploading file should return HTTP 201 Created with correct dto on success")
        public void whenUploadingFileShouldReturnCreatedWithCorrectDtoOnSuccess() throws Exception {
            MockMultipartFile jpgFile = validJpgMultipartFile();

            mockMvc.perform(multipart(ApiConstants.EVENT_FILES_URL, savedEventId)
                            .file(jpgFile)
                            .param("userFilename", FileConstants.USER_FILE_NAME)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.userFilename").value(FileConstants.USER_FILE_NAME))
                    .andExpect(jsonPath("$.originalFilename").value(jpgFile.getOriginalFilename()))
                    .andExpect(jsonPath("$.fileContentType").value(FileConstants.JPG_FILE_CONTENT_TYPE))
                    .andExpect(jsonPath("$.owner.id").isNotEmpty())
                    .andExpect(jsonPath("$.uploadDateTime").value(TimeConstants.NOW.truncatedTo(java.time.temporal.ChronoUnit.MINUTES).toString()));
        }

        @Test
        @DisplayName("When uploading file should persist file in database with correct relationships")
        public void whenUploadingFileShouldPersistFileInDatabaseWithCorrectRelationships() throws Exception {
            MockMultipartFile uploadedFile = validJpgMultipartFile();
            byte[] expectedBytes = uploadedFile.getBytes();
            MvcResult result = mockMvc.perform(multipart(ApiConstants.EVENT_FILES_URL, savedEventId)
                            .file(uploadedFile)
                            .param("userFilename", FileConstants.USER_FILE_NAME)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isCreated())
                    .andReturn();

            FileOverviewDto response = objectMapper.readValue(
                    result.getResponse().getContentAsString(), FileOverviewDto.class);
            File savedFile = requirePresent(
                    fileRepository.findById(response.getId()),
                    "Expected uploaded file to be persisted");

            assertThat(savedFile.getEvent()).as("Expected related record before dereference").isNotNull();
            assertThat(savedFile.getOwner()).as("Expected related record before dereference").isNotNull();
            assertThat(response).isNotNull();
            assertThat(response.getOwner()).isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(savedFile.getOriginalFileName()).isEqualTo(FileConstants.JPG_FILE_ORIGINAL_NAME);
                softly.assertThat(savedFile.getContentType()).isEqualTo(FileConstants.JPG_FILE_CONTENT_TYPE);
                softly.assertThat(savedFile.getContent()).containsExactly(expectedBytes);
                softly.assertThat(savedFile.getUploadDateTime()).isEqualTo(TimeConstants.NOW.truncatedTo(java.time.temporal.ChronoUnit.MINUTES));
                softly.assertThat(response.getId()).isEqualTo(savedFile.getId());
                softly.assertThat(response.getUserFilename()).isEqualTo(savedFile.getUserFileName());
                softly.assertThat(response.getOriginalFilename()).isEqualTo(savedFile.getOriginalFileName());
                softly.assertThat(response.getFileContentType()).isEqualTo(savedFile.getContentType());
                softly.assertThat(response.getUploadDateTime()).isEqualTo(savedFile.getUploadDateTime());
                softly.assertThat(response.getOwner().getId()).isEqualTo(savedFile.getOwner().getId());
                softly.assertThat(fileRepository.countByEventId(savedEventId))
                        .as("Exactly one file should be persisted for the event")
                        .isEqualTo(1L);
                softly.assertThat(savedFile.getEvent().getId()).isEqualTo(savedEventId);
                softly.assertThat(savedFile.getOwner().getEmail()).isEqualTo(UserConstants.FIRST_USER_EMAIL);
                softly.assertThat(savedFile.getUserFileName())
                        .as("Persisted file should have correct user file name")
                        .isEqualTo(FileConstants.USER_FILE_NAME);
            });
        }
    }

    // ===========================================================================================
    // GET /api/v1/events/{eventId}/files/{fileId}/data
    // ===========================================================================================

    @Nested
    @DisplayName("Get file data tests: GET /api/v1/events/{eventId}/files/{fileId}/data")
    class GetFileDataTests {

        private UUID savedFileId;

        @BeforeEach
        void setUp() throws Exception {
            savedFileId = testDataInitializer.setupFileInEvent(savedEventId);
        }

        @Test
        @DisplayName("When getting file data should return HTTP 401 Unauthorized if there is no Authorization header")
        public void whenGettingFileDataShouldReturnUnauthorizedIfThereIsNoAuthorizationHeader() throws Exception {
            var beforeRead = persistenceQueries.fileState();

            mockMvc.perform(get(ApiConstants.EVENT_FILE_DATA_URL, savedEventId, savedFileId))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.AUTHENTICATION_REQUIRED));
            assertThat(persistenceQueries.fileState())
                    .as("File reads must preserve stored bytes, relationships and outbox")
                    .isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting file data should return HTTP 404 Not Found if event does not exist")
        public void whenGettingFileDataShouldReturnNotFoundIfEventDoesNotExist() throws Exception {
            var beforeRead = persistenceQueries.fileState();

            mockMvc.perform(get(ApiConstants.EVENT_FILE_DATA_URL, EventConstants.NOT_EXISTING_EVENT_ID, savedFileId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.EVENT_NOT_FOUND))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(EventNotFoundException.DEFAULT_MESSAGE));
            assertThat(persistenceQueries.fileState())
                    .as("File reads must preserve stored bytes, relationships and outbox")
                    .isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting file data should return HTTP 403 Forbidden if performing user is not attending event")
        public void whenGettingFileDataShouldReturnForbiddenIfPerformingUserIsNotAttendingEvent() throws Exception {
            var beforeRead = persistenceQueries.fileState();

            mockMvc.perform(get(ApiConstants.EVENT_FILE_DATA_URL, savedEventId, savedFileId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.NOT_EVENT_ATTENDEE))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.message").value(NotEventAttendeeException.DEFAULT_MESSAGE));
            assertThat(persistenceQueries.fileState())
                    .as("File reads must preserve stored bytes, relationships and outbox")
                    .isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting file data should return HTTP 404 Not Found if file does not exist in event")
        public void whenGettingFileDataShouldReturnNotFoundIfFileDoesNotExistInEvent() throws Exception {
            var beforeRead = persistenceQueries.fileState();

            mockMvc.perform(get(ApiConstants.EVENT_FILE_DATA_URL, savedEventId, FileConstants.NOT_EXISTING_FILE_ID)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ApiErrorCode.FILE_NOT_FOUND_IN_EVENT))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(FileNotFoundInEventException.DEFAULT_MESSAGE));
            assertThat(persistenceQueries.fileState())
                    .as("File reads must preserve stored bytes, relationships and outbox")
                    .isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting file data should return HTTP 200 OK with file bytes and correct content type")
        public void whenGettingFileDataShouldReturnOkWithFileBytesAndCorrectContentType() throws Exception {
            File file = requirePresent(
                    fileRepository.findById(savedFileId),
                    "Expected file to exist before data lookup");

            var beforeRead = persistenceQueries.fileState();

            mockMvc.perform(get(ApiConstants.EVENT_FILE_DATA_URL, savedEventId, savedFileId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.parseMediaType(file.getContentType())))
                    .andExpect(content().bytes(file.getContent()));
            assertThat(persistenceQueries.fileState())
                    .as("File reads must preserve stored bytes, relationships and outbox")
                    .isEqualTo(beforeRead);
        }
    }
}
