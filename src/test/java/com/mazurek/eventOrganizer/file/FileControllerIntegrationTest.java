package com.mazurek.eventOrganizer.file;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationRequest;
import com.mazurek.eventOrganizer.event.EventService;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotEventAttenderException;
import com.mazurek.eventOrganizer.exception.file.EmptyUploadedFileException;
import com.mazurek.eventOrganizer.exception.file.FileNotFoundInEventException;
import com.mazurek.eventOrganizer.exception.file.FileTypeNotAllowedException;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestDataInitializer;
import com.mazurek.eventOrganizer.testData.builders.AuthenticationRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.MultipartFileTestBuilder;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    private AuthHelper authHelper;
    @Autowired
    private TestDataInitializer testDataInitializer;
    @Autowired
    private DeletionService deletionService;

    private UUID savedEventId;
    private String firstUserJwt;
    private String secondUserJwt;

    @BeforeEach
    void setUp() {
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

    private void addSecondUserAsEventAttender() {
        authHelper.setupSecurityContextForSecondUser();
        eventService.addAttenderToEvent(savedEventId);
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
        @DisplayName("When getting file overview page should return HTTP 403 Forbidden if there is no Authorization header")
        public void whenGettingFileOverviewPageShouldReturnForbiddenIfThereIsNoAuthorizationHeader() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_FILES_URL, savedEventId))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When getting file overview page should return HTTP 404 Not Found if event does not exist")
        public void whenGettingFileOverviewPageShouldReturnNotFoundIfEventDoesNotExist() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_FILES_URL, EventConstants.NOT_EXISTING_EVENT_ID)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(EventNotFoundException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When getting file overview page should return HTTP 403 Forbidden if performing user is not attending event")
        public void whenGettingFileOverviewPageShouldReturnForbiddenIfPerformingUserIsNotAttendingEvent() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_FILES_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.message").value(NotEventAttenderException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When getting file overview page should return HTTP 200 OK with empty page if no files exist in event")
        public void whenGettingFileOverviewPageShouldReturnOkWithEmptyPageIfNoFilesExistInEvent() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_FILES_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.fileOverviews").isArray())
                    .andExpect(jsonPath("$.fileOverviews").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.lastPage").value(true));
        }

        @Test
        @DisplayName("When getting file overview page should return HTTP 200 OK with correct page data when files exist")
        public void whenGettingFileOverviewPageShouldReturnOkWithCorrectPageDataWhenFilesExist() throws Exception {
            testDataInitializer.setupFileInEvent(savedEventId);

            mockMvc.perform(get(ApiConstants.EVENT_FILES_URL, savedEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.fileOverviews").isArray())
                    .andExpect(jsonPath("$.fileOverviews[0].id").isNotEmpty())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.lastPage").value(true))
                    .andExpect(jsonPath("$.pageSize").value(PaginationConstants.FILE_PAGE_SIZE));
        }

        @Test
        @DisplayName("When getting file overview page should return HTTP 200 OK with requested page number when page param is provided")
        public void whenGettingFileOverviewPageShouldReturnOkWithRequestedPageNumberWhenPageParamIsProvided() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_FILES_URL, savedEventId)
                            .param("page", "0")
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pageNumber").value(0));
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
        @DisplayName("When getting file overview by id should return HTTP 403 Forbidden if there is no Authorization header")
        public void whenGettingFileOverviewByIdShouldReturnForbiddenIfThereIsNoAuthorizationHeader() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_FILE_BY_ID_URL, savedEventId, savedFileId))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When getting file overview by id should return HTTP 404 Not Found if event does not exist")
        public void whenGettingFileOverviewByIdShouldReturnNotFoundIfEventDoesNotExist() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_FILE_BY_ID_URL, EventConstants.NOT_EXISTING_EVENT_ID, savedFileId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(EventNotFoundException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When getting file overview by id should return HTTP 403 Forbidden if performing user is not attending event")
        public void whenGettingFileOverviewByIdShouldReturnForbiddenIfPerformingUserIsNotAttendingEvent() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_FILE_BY_ID_URL, savedEventId, savedFileId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.message").value(NotEventAttenderException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When getting file overview by id should return HTTP 404 Not Found if file does not exist in event")
        public void whenGettingFileOverviewByIdShouldReturnNotFoundIfFileDoesNotExistInEvent() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_FILE_BY_ID_URL, savedEventId, FileConstants.FIRST_FILE_ID)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(FileNotFoundInEventException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When getting file overview by id should return HTTP 404 Not Found if file belongs to different event")
        public void whenGettingFileOverviewByIdShouldReturnNotFoundIfFileBelongsToDifferentEvent() throws Exception {
            UUID secondEventId = testDataInitializer.setupEventByFirstUser();
            UUID fileFromSecondEventId = testDataInitializer.setupFileInEvent(secondEventId);

            mockMvc.perform(get(ApiConstants.EVENT_FILE_BY_ID_URL, savedEventId, fileFromSecondEventId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(FileNotFoundInEventException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When getting file overview by id should return HTTP 200 OK with correct data")
        public void whenGettingFileOverviewByIdShouldReturnOkWithCorrectData() throws Exception {
            File file = requirePresent(
                    fileRepository.findById(savedFileId),
                    "Expected file to exist before overview lookup");

            mockMvc.perform(get(ApiConstants.EVENT_FILE_BY_ID_URL, savedEventId, savedFileId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(savedFileId.toString()))
                    .andExpect(jsonPath("$.userFilename").value(file.getUserFileName()))
                    .andExpect(jsonPath("$.originalFilename").value(file.getOriginalFileName()))
                    .andExpect(jsonPath("$.fileContentType").value(file.getContentType()))
                    .andExpect(jsonPath("$.owner.id").value(file.getOwner().getId().toString()));
        }
    }

    // ===========================================================================================
    // POST /api/v1/events/{eventId}/files
    // ===========================================================================================

    @Nested
    @DisplayName("Upload file tests: POST /api/v1/events/{eventId}/files")
    class UploadFileTests {

        @Test
        @DisplayName("When uploading file should return HTTP 403 Forbidden if there is no Authorization header")
        public void whenUploadingFileShouldReturnForbiddenIfThereIsNoAuthorizationHeader() throws Exception {
            mockMvc.perform(multipart(ApiConstants.EVENT_FILES_URL, savedEventId)
                            .file(validJpgMultipartFile())
                            .param("userFilename", FileConstants.USER_FILE_NAME))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When uploading file should return HTTP 404 Not Found if event does not exist")
        public void whenUploadingFileShouldReturnNotFoundIfEventDoesNotExist() throws Exception {
            mockMvc.perform(multipart(ApiConstants.EVENT_FILES_URL, EventConstants.NOT_EXISTING_EVENT_ID)
                            .file(validJpgMultipartFile())
                            .param("userFilename", FileConstants.USER_FILE_NAME)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(EventNotFoundException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When uploading file should return HTTP 403 Forbidden if performing user is not attending event")
        public void whenUploadingFileShouldReturnForbiddenIfPerformingUserIsNotAttendingEvent() throws Exception {
            mockMvc.perform(multipart(ApiConstants.EVENT_FILES_URL, savedEventId)
                            .file(validJpgMultipartFile())
                            .param("userFilename", FileConstants.USER_FILE_NAME)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.message").value(NotEventAttenderException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When uploading file should return HTTP 400 Bad Request if file is empty")
        public void whenUploadingFileShouldReturnBadRequestIfFileIsEmpty() throws Exception {
            MockMultipartFile emptyFile = MultipartFileTestBuilder.jpgFile()
                    .content(new byte[0])
                    .buildMultipartFile();

            mockMvc.perform(multipart(ApiConstants.EVENT_FILES_URL, savedEventId)
                            .file(emptyFile)
                            .param("userFilename", FileConstants.USER_FILE_NAME)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.message").value(EmptyUploadedFileException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When uploading file should return HTTP 415 Unsupported Media Type if file type is not on whitelist")
        public void whenUploadingFileShouldReturnUnsupportedMediaTypeIfFileTypeIsNotOnWhitelist() throws Exception {
            MockMultipartFile malwareFile = MultipartFileTestBuilder.malwareFile().buildMultipartFile();

            mockMvc.perform(multipart(ApiConstants.EVENT_FILES_URL, savedEventId)
                            .file(malwareFile)
                            .param("userFilename", FileConstants.MALWARE_FILE_USER_FILE_NAME)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.status").value(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()))
                    .andExpect(jsonPath("$.message").value(FileTypeNotAllowedException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When uploading file should return HTTP 400 Bad Request if user filename is blank")
        public void whenUploadingFileShouldReturnBadRequestIfUserFilenameIsBlank() throws Exception {
            mockMvc.perform(multipart(ApiConstants.EVENT_FILES_URL, savedEventId)
                            .file(validJpgMultipartFile())
                            .param("userFilename", InvalidInputConstants.BLANK_VALUE)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.errors.userFilename").hasJsonPath());
        }

        @Test
        @DisplayName("When uploading file should return HTTP 400 Bad Request if user filename is too long")
        public void whenUploadingFileShouldReturnBadRequestIfUserFilenameIsTooLong() throws Exception {
            mockMvc.perform(multipart(ApiConstants.EVENT_FILES_URL, savedEventId)
                            .file(validJpgMultipartFile())
                            .param("userFilename", FileConstants.WRONG_USER_FILE_NAME_TOO_LONG)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.errors.userFilename").hasJsonPath());
        }

        @Test
        @DisplayName("When uploading file should return HTTP 400 Bad Request if file is missing")
        public void whenUploadingFileShouldReturnBadRequestIfFileIsMissing() throws Exception {
            mockMvc.perform(multipart(ApiConstants.EVENT_FILES_URL, savedEventId)
                            .param("userFilename", FileConstants.USER_FILE_NAME)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.errors.file").hasJsonPath());
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
                    .andExpect(jsonPath("$.uploadDateTime").isNotEmpty());
        }

        @Test
        @DisplayName("When uploading file should persist file in database with correct relationships")
        public void whenUploadingFileShouldPersistFileInDatabaseWithCorrectRelationships() throws Exception {
            mockMvc.perform(multipart(ApiConstants.EVENT_FILES_URL, savedEventId)
                            .file(validJpgMultipartFile())
                            .param("userFilename", FileConstants.USER_FILE_NAME)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isCreated());

            Set<File> eventFiles = fileRepository.findByEventId(savedEventId);

            assertThat(eventFiles)
                    .as("File should be persisted and linked to event")
                    .hasSize(1);
            assertThat(eventFiles.iterator().next().getUserFileName())
                    .as("Persisted file should have correct user file name")
                    .isEqualTo(FileConstants.USER_FILE_NAME);
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
        @DisplayName("When getting file data should return HTTP 403 Forbidden if there is no Authorization header")
        public void whenGettingFileDataShouldReturnForbiddenIfThereIsNoAuthorizationHeader() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_FILE_DATA_URL, savedEventId, savedFileId))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("When getting file data should return HTTP 404 Not Found if event does not exist")
        public void whenGettingFileDataShouldReturnNotFoundIfEventDoesNotExist() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_FILE_DATA_URL, EventConstants.NOT_EXISTING_EVENT_ID, savedFileId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(EventNotFoundException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When getting file data should return HTTP 403 Forbidden if performing user is not attending event")
        public void whenGettingFileDataShouldReturnForbiddenIfPerformingUserIsNotAttendingEvent() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_FILE_DATA_URL, savedEventId, savedFileId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, secondUserJwt))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
                    .andExpect(jsonPath("$.message").value(NotEventAttenderException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When getting file data should return HTTP 404 Not Found if file does not exist in event")
        public void whenGettingFileDataShouldReturnNotFoundIfFileDoesNotExistInEvent() throws Exception {
            mockMvc.perform(get(ApiConstants.EVENT_FILE_DATA_URL, savedEventId, FileConstants.FIRST_FILE_ID)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(FileNotFoundInEventException.DEFAULT_MESSAGE));
        }

        @Test
        @DisplayName("When getting file data should return HTTP 200 OK with file bytes and correct content type")
        public void whenGettingFileDataShouldReturnOkWithFileBytesAndCorrectContentType() throws Exception {
            File file = requirePresent(
                    fileRepository.findById(savedFileId),
                    "Expected file to exist before data lookup");

            mockMvc.perform(get(ApiConstants.EVENT_FILE_DATA_URL, savedEventId, savedFileId)
                            .header(ApiConstants.AUTHORIZATION_HEADER, firstUserJwt))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.parseMediaType(file.getContentType())))
                    .andExpect(content().bytes(file.getContent()));
        }
    }
}
