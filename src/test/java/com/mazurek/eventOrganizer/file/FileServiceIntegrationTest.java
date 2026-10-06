package com.mazurek.eventOrganizer.file;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.testData.TestFileContentFactory;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotEventAttendeeException;
import com.mazurek.eventOrganizer.exception.file.EmptyUploadedFileException;
import com.mazurek.eventOrganizer.exception.file.FileNotFoundInEventException;
import com.mazurek.eventOrganizer.exception.file.FileTypeNotAllowedException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestDataInitializer;
import com.mazurek.eventOrganizer.testData.TestPersistenceQueries;
import com.mazurek.eventOrganizer.testData.builders.dto.FileUploadDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.MultipartFileTestBuilder;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("FileService integration tests:")
public class FileServiceIntegrationTest {

    @Autowired
    private FileService fileService;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private TestDataInitializer testDataInitializer;
    @Autowired
    private DeletionService deletionService;
    @Autowired
    private TestPersistenceQueries persistenceQueries;

    private UUID savedEventId;

    private static Stream<MockMultipartFile> allowedFileProvider() {
        return Stream.of(
                new MultipartFileTestBuilder().originalFileName(FileConstants.JPG_FILE_ORIGINAL_NAME)
                        .contentType(FileConstants.JPG_FILE_CONTENT_TYPE).content(TestFileContentFactory.jpg()).buildMultipartFile(),
                new MultipartFileTestBuilder().originalFileName(FileConstants.JPEG_FILE_ORIGINAL_NAME)
                        .contentType(FileConstants.JPEG_FILE_CONTENT_TYPE).content(TestFileContentFactory.jpeg()).buildMultipartFile(),
                new MultipartFileTestBuilder().originalFileName(FileConstants.PNG_FILE_ORIGINAL_NAME)
                        .contentType(FileConstants.PNG_FILE_CONTENT_TYPE).content(TestFileContentFactory.png()).buildMultipartFile(),
                new MultipartFileTestBuilder().originalFileName(FileConstants.PDF_FILE_ORIGINAL_NAME)
                        .contentType(FileConstants.PDF_FILE_CONTENT_TYPE).content(TestFileContentFactory.pdf()).buildMultipartFile(),
                new MultipartFileTestBuilder().originalFileName(FileConstants.DOC_FILE_ORIGINAL_NAME)
                        .contentType(FileConstants.DOC_FILE_CONTENT_TYPE).content(TestFileContentFactory.doc()).buildMultipartFile(),
                new MultipartFileTestBuilder().originalFileName(FileConstants.DOCX_FILE_ORIGINAL_NAME)
                        .contentType(FileConstants.DOCX_FILE_CONTENT_TYPE).content(TestFileContentFactory.docx()).buildMultipartFile(),
                new MultipartFileTestBuilder().originalFileName(FileConstants.PPT_FILE_ORIGINAL_NAME)
                        .contentType(FileConstants.PPT_FILE_CONTENT_TYPE).content(TestFileContentFactory.ppt()).buildMultipartFile(),
                new MultipartFileTestBuilder().originalFileName(FileConstants.PPTX_FILE_ORIGINAL_NAME)
                        .contentType(FileConstants.PPTX_FILE_CONTENT_TYPE).content(TestFileContentFactory.pptx()).buildMultipartFile(),
                new MultipartFileTestBuilder().originalFileName(FileConstants.ODT_FILE_ORIGINAL_NAME)
                        .contentType(FileConstants.ODT_FILE_CONTENT_TYPE).content(TestFileContentFactory.odt()).buildMultipartFile(),
                new MultipartFileTestBuilder().originalFileName(FileConstants.XLS_FILE_ORIGINAL_NAME)
                        .contentType(FileConstants.XLS_FILE_CONTENT_TYPE).content(TestFileContentFactory.xls()).buildMultipartFile(),
                new MultipartFileTestBuilder().originalFileName(FileConstants.XLSX_FILE_ORIGINAL_NAME)
                        .contentType(FileConstants.XLSX_FILE_CONTENT_TYPE).content(TestFileContentFactory.xlsx()).buildMultipartFile(),
                new MultipartFileTestBuilder().originalFileName(FileConstants.MP4_FILE_ORIGINAL_NAME)
                        .contentType(FileConstants.MP4_FILE_CONTENT_TYPE).content(TestFileContentFactory.mp4()).buildMultipartFile(),
                new MultipartFileTestBuilder().originalFileName(FileConstants.AVI_FILE_ORIGINAL_NAME)
                        .contentType(FileConstants.AVI_FILE_CONTENT_TYPE).content(TestFileContentFactory.avi()).buildMultipartFile()
        );
    }

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();
        savedEventId = testDataInitializer.setupFirstEvent();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
    }

    @Nested
    @DisplayName("Upload file tests:")
    class UploadFileTests {

        private FileUploadDto fileUploadDto;

        @BeforeEach
        void setUp() {
            fileUploadDto = FileUploadDtoTestBuilder.jpgFile().build();
        }

        @Test
        @DisplayName("When uploading file should throw EventNotFoundException if event with given id does not exist")
        public void whenUploadingFileShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
            authHelper.setupSecurityContextForFirstUser();

            var beforeOperation = persistenceQueries.fileState();

            assertThatThrownBy(() -> fileService.uploadFileToEvent(fileUploadDto, EventConstants.NOT_EXISTING_EVENT_ID))
                    .isInstanceOf(EventNotFoundException.class);
            assertThat(persistenceQueries.fileState())
                    .as("Rejected operation must preserve file bytes, relationships and outbox")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When uploading file should throw NotEventAttendeeException if performing user is not attending event")
        public void whenUploadingFileShouldThrowNotEventAttendeeExceptionIfPerformingUserIsNotAttendingEvent() {
            authHelper.setupSecurityContextForSecondUser();

            var beforeOperation = persistenceQueries.fileState();

            assertThatThrownBy(() -> fileService.uploadFileToEvent(fileUploadDto, savedEventId))
                    .isInstanceOf(NotEventAttendeeException.class);
            assertThat(persistenceQueries.fileState())
                    .as("Rejected operation must preserve file bytes, relationships and outbox")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When uploading file should throw EmptyUploadedFileException if file has no content")
        public void whenUploadingFileShouldThrowEmptyUploadedFileExceptionIfFileHasNoContent() {
            authHelper.setupSecurityContextForFirstUser();
            fileUploadDto = FileUploadDtoTestBuilder.emptyJpgFile().build();

            var beforeOperation = persistenceQueries.fileState();

            assertThatThrownBy(() -> fileService.uploadFileToEvent(fileUploadDto, savedEventId))
                    .isInstanceOf(EmptyUploadedFileException.class);
            assertThat(persistenceQueries.fileState())
                    .as("Rejected operation must preserve file bytes, relationships and outbox")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When uploading file should throw FileTypeNotAllowedException if file is not on whitelist")
        public void whenUploadingFileShouldThrowFileTypeNotAllowedExceptionIfFileIsNotOnWhitelist() {
            authHelper.setupSecurityContextForFirstUser();
            fileUploadDto = FileUploadDtoTestBuilder.malwareFile().build();

            var beforeOperation = persistenceQueries.fileState();

            assertThatThrownBy(() -> fileService.uploadFileToEvent(fileUploadDto, savedEventId))
                    .isInstanceOf(FileTypeNotAllowedException.class);
            assertThat(persistenceQueries.fileState())
                    .as("Rejected operation must preserve file bytes, relationships and outbox")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When uploading file should save file with correct data and relationships in database")
        public void whenUploadingFileShouldSaveFileWithCorrectDataAndRelationshipsInDatabase() throws IOException {
            authHelper.setupSecurityContextForFirstUser();
            MockMultipartFile multipartFile = (MockMultipartFile) fileUploadDto.getFile();

            UUID savedFileId = fileService.uploadFileToEvent(fileUploadDto, savedEventId).getId();

            File savedFile = requirePresent(
                    fileRepository.findById(savedFileId),
                    "Expected uploaded file to exist before persistence assertions");
            User uploader = userRepository.findByEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);
            byte[] expectedBytes = multipartFile.getBytes();


            assertThat(savedFile.getOwner()).as("Expected related record before dereference").isNotNull();
            assertThat(savedFile.getEvent()).as("Expected related record before dereference").isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(savedFile.getOwner().getId())
                        .as("File owner should be set to the performing user")
                        .isEqualTo(uploader.getId());
                softly.assertThat(savedFile.getEvent().getId())
                        .as("File event should be set to the event with given id")
                        .isEqualTo(savedEventId);
                softly.assertThat(savedFile.getUserFileName())
                        .as("User file name should match dto")
                        .isEqualTo(fileUploadDto.getUserFilename());
                softly.assertThat(savedFile.getOriginalFileName())
                        .as("Original file name should match uploaded file")
                        .isEqualTo(multipartFile.getOriginalFilename());
                softly.assertThat(savedFile.getContentType())
                        .as("Content type should use validated MIME type")
                        .isEqualTo(FileConstants.JPG_FILE_CONTENT_TYPE);
                softly.assertThat(savedFile.getContent())
                        .as("File content should be persisted unchanged")
                        .isEqualTo(expectedBytes);
                softly.assertThat(savedFile.getUploadDateTime())
                        .as("Upload date time should use application clock and be truncated to minutes")
                        .isEqualTo(TimeConstants.NOW.truncatedTo(ChronoUnit.MINUTES))
                        .isEqualTo(savedFile.getUploadDateTime().truncatedTo(ChronoUnit.MINUTES));
            });
        }

        @Test
        @DisplayName("When uploading file should persist validated content type instead of client provided one")
        public void whenUploadingFileShouldPersistValidatedContentTypeInsteadOfClientProvidedOne() throws IOException {
            authHelper.setupSecurityContextForFirstUser();

            MockMultipartFile mismatchedMimeJpgFile = MultipartFileTestBuilder.jpgFile()
                    .contentType(FileConstants.PDF_FILE_CONTENT_TYPE)
                    .buildMultipartFile();
            fileUploadDto = FileUploadDtoTestBuilder.jpgFile()
                    .file(mismatchedMimeJpgFile)
                    .build();

            UUID savedFileId = fileService.uploadFileToEvent(fileUploadDto, savedEventId).getId();
            File savedFile = requirePresent(
                    fileRepository.findById(savedFileId),
                    "Expected uploaded file to exist before content type assertions");

            assertThat(savedFile.getContentType()).isEqualTo(FileConstants.JPG_FILE_CONTENT_TYPE);
        }
    }

    @Nested
    @DisplayName("Get file overview by id tests:")
    class GetFileOverviewByIdTests {

        private UUID savedFileId;

        @BeforeEach
        void setUp() throws IOException {
            savedFileId = testDataInitializer.setupFileInEvent(savedEventId);
        }

        @Test
        @DisplayName("When getting file overview by id should throw EventNotFoundException if event with given id does not exist")
        public void whenGettingFileOverviewByIdShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
            authHelper.setupSecurityContextForFirstUser();

            var beforeOperation = persistenceQueries.fileState();

            assertThatThrownBy(() -> fileService.getFileOverviewById(savedFileId, EventConstants.NOT_EXISTING_EVENT_ID))
                    .isInstanceOf(EventNotFoundException.class);
            assertThat(persistenceQueries.fileState())
                    .as("Rejected operation must preserve file bytes, relationships and outbox")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When getting file overview by id should throw NotEventAttendeeException if user is not attending event")
        public void whenGettingFileOverviewByIdShouldThrowNotEventAttendeeExceptionIfUserIsNotAttendingEvent() {
            authHelper.setupSecurityContextForSecondUser();

            var beforeOperation = persistenceQueries.fileState();

            assertThatThrownBy(() -> fileService.getFileOverviewById(savedFileId, savedEventId))
                    .isInstanceOf(NotEventAttendeeException.class);
            assertThat(persistenceQueries.fileState())
                    .as("Rejected operation must preserve file bytes, relationships and outbox")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When getting file overview by id should throw FileNotFoundInEventException if file with given id does not exist")
        public void whenGettingFileOverviewByIdShouldThrowFileNotFoundInEventExceptionIfFileWithGivenIdDoesNotExist() {
            authHelper.setupSecurityContextForFirstUser();

            var beforeOperation = persistenceQueries.fileState();

            assertThatThrownBy(() -> fileService.getFileOverviewById(FileConstants.NOT_EXISTING_FILE_ID, savedEventId))
                    .isInstanceOf(FileNotFoundInEventException.class);
            assertThat(persistenceQueries.fileState())
                    .as("Rejected operation must preserve file bytes, relationships and outbox")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When getting file overview by id should throw FileNotFoundInEventException if file and event exist but are not related")
        public void whenGettingFileOverviewByIdShouldThrowFileNotFoundInEventExceptionIfFileAndEventExistButAreNotRelated() {
            UUID secondEventId = testDataInitializer.setupEventByFirstUser();
            authHelper.setupSecurityContextForFirstUser();

            var beforeOperation = persistenceQueries.fileState();

            assertThatThrownBy(() -> fileService.getFileOverviewById(savedFileId, secondEventId))
                    .isInstanceOf(FileNotFoundInEventException.class);
            assertThat(persistenceQueries.fileState())
                    .as("Rejected operation must preserve file bytes, relationships and outbox")
                    .isEqualTo(beforeOperation);
        }
    }

    @Nested
    @DisplayName("Get file data by id tests:")
    class GetFileDataByIdTests {

        private UUID savedFileId;

        @BeforeEach
        void setUp() throws IOException {
            savedFileId = testDataInitializer.setupFileInEvent(savedEventId);
        }

        @Test
        @DisplayName("When getting file data by id should throw EventNotFoundException if event with given id does not exist")
        public void whenGettingFileDataByIdShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
            authHelper.setupSecurityContextForFirstUser();

            var beforeOperation = persistenceQueries.fileState();

            assertThatThrownBy(() -> fileService.getFileDataById(savedFileId, EventConstants.NOT_EXISTING_EVENT_ID))
                    .isInstanceOf(EventNotFoundException.class);
            assertThat(persistenceQueries.fileState())
                    .as("Rejected operation must preserve file bytes, relationships and outbox")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When getting file data by id should throw NotEventAttendeeException if user is not attending event")
        public void whenGettingFileDataByIdShouldThrowNotEventAttendeeExceptionIfUserIsNotAttendingEvent() {
            authHelper.setupSecurityContextForSecondUser();

            var beforeOperation = persistenceQueries.fileState();

            assertThatThrownBy(() -> fileService.getFileDataById(savedFileId, savedEventId))
                    .isInstanceOf(NotEventAttendeeException.class);
            assertThat(persistenceQueries.fileState())
                    .as("Rejected operation must preserve file bytes, relationships and outbox")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When getting file data by id should throw FileNotFoundInEventException if file with given id does not exist")
        public void whenGettingFileDataByIdShouldThrowFileNotFoundInEventExceptionIfFileWithGivenIdDoesNotExist() {
            authHelper.setupSecurityContextForFirstUser();

            var beforeOperation = persistenceQueries.fileState();

            assertThatThrownBy(() -> fileService.getFileDataById(FileConstants.NOT_EXISTING_FILE_ID, savedEventId))
                    .isInstanceOf(FileNotFoundInEventException.class);
            assertThat(persistenceQueries.fileState())
                    .as("Rejected operation must preserve file bytes, relationships and outbox")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When getting file data by id should throw FileNotFoundInEventException if file and event exist but are not related")
        public void whenGettingFileDataByIdShouldThrowFileNotFoundInEventExceptionIfFileAndEventExistButAreNotRelated() {
            UUID secondEventId = testDataInitializer.setupEventByFirstUser();
            authHelper.setupSecurityContextForFirstUser();

            var beforeOperation = persistenceQueries.fileState();

            assertThatThrownBy(() -> fileService.getFileDataById(savedFileId, secondEventId))
                    .isInstanceOf(FileNotFoundInEventException.class);
            assertThat(persistenceQueries.fileState())
                    .as("Rejected operation must preserve file bytes, relationships and outbox")
                    .isEqualTo(beforeOperation);
        }
    }

    @Nested
    @DisplayName("Get file overview page by event id tests:")
    class GetFileOverviewPageByEventIdTests {

        @Test
        @DisplayName("When getting file overview page should throw InvalidPageNumberException if page number is negative")
        public void whenGettingFileOverviewPageShouldThrowInvalidPageNumberExceptionIfPageNumberIsNegative() {
            authHelper.setupSecurityContextForFirstUser();

            var beforeOperation = persistenceQueries.fileState();

            assertThatThrownBy(() -> fileService.getFileOverviewPageByEventId(savedEventId, PaginationConstants.PAGE_MINUS_ONE))
                    .isInstanceOf(InvalidPageNumberException.class);
            assertThat(persistenceQueries.fileState())
                    .as("Rejected operation must preserve file bytes, relationships and outbox")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When getting file overview page should throw EventNotFoundException if event with given id does not exist")
        public void whenGettingFileOverviewPageShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
            authHelper.setupSecurityContextForFirstUser();

            var beforeOperation = persistenceQueries.fileState();

            assertThatThrownBy(() -> fileService.getFileOverviewPageByEventId(EventConstants.NOT_EXISTING_EVENT_ID, PaginationConstants.PAGE_ZERO))
                    .isInstanceOf(EventNotFoundException.class);
            assertThat(persistenceQueries.fileState())
                    .as("Rejected operation must preserve file bytes, relationships and outbox")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When getting file overview page should throw NotEventAttendeeException if user is not attending event")
        public void whenGettingFileOverviewPageShouldThrowNotEventAttendeeExceptionIfUserIsNotAttendingEvent() {
            authHelper.setupSecurityContextForSecondUser();

            var beforeOperation = persistenceQueries.fileState();

            assertThatThrownBy(() -> fileService.getFileOverviewPageByEventId(savedEventId, PaginationConstants.PAGE_ZERO))
                    .isInstanceOf(NotEventAttendeeException.class);
            assertThat(persistenceQueries.fileState())
                    .as("Rejected operation must preserve file bytes, relationships and outbox")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When getting file overview page should return empty page if there are no files in event")
        public void whenGettingFileOverviewPageShouldReturnEmptyPageIfThereAreNoFilesInEvent() {
            authHelper.setupSecurityContextForFirstUser();

            var beforeRead = persistenceQueries.fileState();

            FileOverviewPageDto returnedPage = fileService.getFileOverviewPageByEventId(savedEventId, PaginationConstants.PAGE_ZERO);

            assertThat(returnedPage).as("Expected returnedPage before field assertions").isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(returnedPage.totalPages()).isEqualTo(0);
                softly.assertThat(returnedPage.fileOverviews()).isEmpty();
                softly.assertThat(returnedPage.pageNumber()).isEqualTo(PaginationConstants.PAGE_ZERO);
                softly.assertThat(returnedPage.lastPage()).isTrue();
                softly.assertThat(returnedPage.totalElements()).isEqualTo(PaginationConstants.PAGE_ZERO);
                softly.assertThat(returnedPage.pageSize()).isEqualTo(PaginationConstants.FILE_PAGE_SIZE);
            });
            assertThat(persistenceQueries.fileState())
                    .as("File reads must preserve stored bytes, relationships and outbox")
                    .isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting file overview page should return correct page when file count is below page size")
        public void whenGettingFileOverviewPageShouldReturnCorrectPageWhenFileCountIsBelowPageSize() {
            authHelper.setupSecurityContextForFirstUser();
            Set<UUID> savedFilesIds = prepareAndUploadFiles(PaginationConstants.FIVE_ELEMENTS);

            var beforeRead = persistenceQueries.fileState();

            FileOverviewPageDto returnedPage = fileService.getFileOverviewPageByEventId(savedEventId, PaginationConstants.PAGE_ZERO);

            assertThat(returnedPage).as("Expected returnedPage before field assertions").isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(returnedPage.pageNumber()).isEqualTo(PaginationConstants.PAGE_ZERO);
                softly.assertThat(returnedPage.fileOverviews()).hasSize(PaginationConstants.FIVE_ELEMENTS);
                softly.assertThat(returnedPage.lastPage()).isTrue();
                softly.assertThat(returnedPage.totalElements()).isEqualTo(PaginationConstants.FIVE_ELEMENTS);
                softly.assertThat(returnedPage.pageSize()).isEqualTo(PaginationConstants.FILE_PAGE_SIZE);
                softly.assertThat(returnedPage.fileOverviews().stream()
                                .map(FileOverviewDto::getId)
                                .collect(Collectors.toUnmodifiableSet()))
                        .as("Returned file overviews should match uploaded file ids")
                        .isEqualTo(savedFilesIds);
            });
            assertThat(persistenceQueries.fileState())
                    .as("File reads must preserve stored bytes, relationships and outbox")
                    .isEqualTo(beforeRead);
            assertThat(returnedPage.fileOverviews()).hasSize(PaginationConstants.FIVE_ELEMENTS);
            File expectedFile = requirePresent(fileRepository.findById(returnedPage.fileOverviews().getFirst().getId()),
                    "Expected overview's source file to remain persisted");
            FileOverviewDto firstOverview = returnedPage.fileOverviews().getFirst();
            assertThat(firstOverview).isNotNull();
            assertThat(firstOverview.getOwner()).isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(firstOverview.getOriginalFilename()).isEqualTo(expectedFile.getOriginalFileName());
                softly.assertThat(firstOverview.getUserFilename()).isEqualTo(expectedFile.getUserFileName());
                softly.assertThat(firstOverview.getFileContentType()).isEqualTo(expectedFile.getContentType());
                softly.assertThat(firstOverview.getUploadDateTime()).isEqualTo(expectedFile.getUploadDateTime());
                softly.assertThat(firstOverview.getOwner().getId()).isEqualTo(expectedFile.getOwner().getId());
                softly.assertThat(returnedPage.totalPages()).isEqualTo(1);
            });
        }

        @Test
        @DisplayName("When getting file overview page should return files sorted from oldest to newest by upload date time")
        public void whenGettingFileOverviewPageShouldReturnFilesSortedFromOldestToNewestByUploadDateTime() {
            authHelper.setupSecurityContextForFirstUser();
            prepareAndUploadFiles(PaginationConstants.FIVE_ELEMENTS);

            List<File> uploadedFiles = fileRepository.findAll();
            assertThat(uploadedFiles).hasSize(PaginationConstants.FIVE_ELEMENTS);
            for (int index = 0; index < uploadedFiles.size(); index++) {
                uploadedFiles.get(index).setUploadDateTime(TimeConstants.TWO_HOURS_AGO.plusSeconds(index / 2));
            }
            fileRepository.saveAllAndFlush(uploadedFiles);
            List<UUID> expectedIds = uploadedFiles.stream()
                    .sorted(Comparator.comparing(File::getUploadDateTime)
                            .thenComparing(file -> file.getId().toString()))
                    .map(File::getId).toList();

            var beforeRead = persistenceQueries.fileState();

            FileOverviewPageDto returnedPage = fileService.getFileOverviewPageByEventId(savedEventId, PaginationConstants.PAGE_ZERO);

            List<Instant> uploadTimes = returnedPage.fileOverviews().stream()
                    .map(FileOverviewDto::getUploadDateTime)
                    .toList();

            assertThat(returnedPage.fileOverviews()).extracting(FileOverviewDto::getId)
                    .containsExactlyElementsOf(expectedIds);
            assertThat(uploadTimes)
                    .as("Files should be sorted from oldest to newest by uploadDateTime")
                    .isSortedAccordingTo(Comparator.naturalOrder());
            assertThat(persistenceQueries.fileState())
                    .as("File reads must preserve stored bytes, relationships and outbox")
                    .isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting file overview page should return exactly twenty files as first and last page when file count equals page size")
        public void whenGettingFileOverviewPageShouldReturnExactlyTwentyFilesWhenFileCountEqualsPageSize() {
            authHelper.setupSecurityContextForFirstUser();
            Set<UUID> savedFilesIds = prepareAndUploadFiles(PaginationConstants.TWENTY_ELEMENTS);

            var beforeRead = persistenceQueries.fileState();

            FileOverviewPageDto returnedPage = fileService.getFileOverviewPageByEventId(savedEventId, PaginationConstants.PAGE_ZERO);

            assertThat(returnedPage).as("Expected returnedPage before field assertions").isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(returnedPage.pageNumber()).isEqualTo(PaginationConstants.PAGE_ZERO);
                softly.assertThat(returnedPage.fileOverviews()).hasSize(PaginationConstants.TWENTY_ELEMENTS);
                softly.assertThat(returnedPage.lastPage()).isTrue();
                softly.assertThat(returnedPage.totalElements()).isEqualTo(PaginationConstants.TWENTY_ELEMENTS);
                softly.assertThat(returnedPage.pageSize()).isEqualTo(PaginationConstants.FILE_PAGE_SIZE);
                softly.assertThat(returnedPage.fileOverviews().stream()
                                .map(FileOverviewDto::getId)
                                .collect(Collectors.toUnmodifiableSet()))
                        .isEqualTo(savedFilesIds);
            });
            assertThat(persistenceQueries.fileState())
                    .as("File reads must preserve stored bytes, relationships and outbox")
                    .isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting file overview page should split files across two pages when file count exceeds page size")
        public void whenGettingFileOverviewPageShouldSplitFilesAcrossTwoPagesWhenFileCountExceedsPageSize() {
            authHelper.setupSecurityContextForFirstUser();
            Set<UUID> savedFilesIds = prepareAndUploadFiles(PaginationConstants.THIRTY_ELEMENTS);

            List<File> uploadedFiles = fileRepository.findAll();
            assertThat(uploadedFiles).hasSize(PaginationConstants.THIRTY_ELEMENTS);
            for (int index = 0; index < uploadedFiles.size(); index++) {
                uploadedFiles.get(index).setUploadDateTime(TimeConstants.TWO_HOURS_AGO.plusSeconds(index / 2));
            }
            fileRepository.saveAllAndFlush(uploadedFiles);
            List<UUID> expectedIds = uploadedFiles.stream()
                    .sorted(Comparator.comparing(File::getUploadDateTime)
                            .thenComparing(file -> file.getId().toString()))
                    .map(File::getId).toList();

            var beforeRead = persistenceQueries.fileState();

            FileOverviewPageDto pageZero = fileService.getFileOverviewPageByEventId(savedEventId, PaginationConstants.PAGE_ZERO);
            FileOverviewPageDto pageOne = fileService.getFileOverviewPageByEventId(savedEventId, PaginationConstants.PAGE_ONE);

            Set<UUID> pageZeroIds = pageZero.fileOverviews().stream().map(FileOverviewDto::getId).collect(Collectors.toSet());
            Set<UUID> pageOneIds = pageOne.fileOverviews().stream().map(FileOverviewDto::getId).collect(Collectors.toSet());
            Set<UUID> allReturnedIds = new HashSet<>(pageZeroIds);
            allReturnedIds.addAll(pageOneIds);

            assertThat(pageZero).as("Expected pageZero before field assertions").isNotNull();
            assertThat(pageOne).as("Expected pageOne before field assertions").isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(pageZero.pageNumber()).isEqualTo(PaginationConstants.PAGE_ZERO);
                softly.assertThat(pageZero.fileOverviews()).hasSize(PaginationConstants.FILE_PAGE_SIZE);
                softly.assertThat(pageZero.totalPages()).isEqualTo(2);
                softly.assertThat(pageZero.lastPage()).isFalse();
                softly.assertThat(pageZero.totalElements()).isEqualTo(PaginationConstants.THIRTY_ELEMENTS);
                softly.assertThat(pageZero.pageSize()).isEqualTo(PaginationConstants.FILE_PAGE_SIZE);

                softly.assertThat(pageOne.pageNumber()).isEqualTo(PaginationConstants.PAGE_ONE);
                softly.assertThat(pageOne.fileOverviews()).hasSize(PaginationConstants.TEN_ELEMENTS);
                softly.assertThat(pageOne.totalPages()).isEqualTo(2);
                softly.assertThat(pageOne.lastPage()).isTrue();
                softly.assertThat(pageOne.totalElements()).isEqualTo(PaginationConstants.THIRTY_ELEMENTS);
                softly.assertThat(pageOne.pageSize()).isEqualTo(PaginationConstants.FILE_PAGE_SIZE);

                softly.assertThat(pageZeroIds)
                        .as("Pages should not contain overlapping file overviews")
                        .doesNotContainAnyElementsOf(pageOneIds);
                softly.assertThat(allReturnedIds)
                        .as("All returned ids across both pages should match the saved file ids")
                        .isEqualTo(savedFilesIds);
            });
            assertThat(persistenceQueries.fileState())
                    .as("File reads must preserve stored bytes, relationships and outbox")
                    .isEqualTo(beforeRead);
            assertThat(pageZero.fileOverviews()).extracting(FileOverviewDto::getId)
                    .containsExactlyElementsOf(expectedIds.subList(0, PaginationConstants.FILE_PAGE_SIZE));
            assertThat(pageOne.fileOverviews()).extracting(FileOverviewDto::getId)
                    .containsExactlyElementsOf(expectedIds.subList(PaginationConstants.FILE_PAGE_SIZE, expectedIds.size()));
        }

        @Test
        @DisplayName("When getting file overview page should return empty page if requested page number exceeds available pages")
        public void whenGettingFileOverviewPageShouldReturnEmptyPageIfRequestedPageNumberExceedsAvailablePages() {
            authHelper.setupSecurityContextForFirstUser();
            Set<UUID> savedFilesIds = prepareAndUploadFiles(PaginationConstants.TWENTY_ELEMENTS);

            var beforeRead = persistenceQueries.fileState();

            FileOverviewPageDto pageZero = fileService.getFileOverviewPageByEventId(savedEventId, PaginationConstants.PAGE_ZERO);
            FileOverviewPageDto pageOne = fileService.getFileOverviewPageByEventId(savedEventId, PaginationConstants.PAGE_ONE);

            assertThat(pageZero).as("Expected pageZero before field assertions").isNotNull();
            assertThat(pageOne).as("Expected pageOne before field assertions").isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(pageOne.totalPages()).isEqualTo(1);
                softly.assertThat(pageZero.fileOverviews()).hasSize(PaginationConstants.TWENTY_ELEMENTS);
                softly.assertThat(pageZero.lastPage()).isTrue();
                softly.assertThat(pageZero.fileOverviews().stream()
                                .map(FileOverviewDto::getId)
                                .collect(Collectors.toUnmodifiableSet()))
                        .isEqualTo(savedFilesIds);

                softly.assertThat(pageOne.pageNumber()).isEqualTo(PaginationConstants.PAGE_ONE);
                softly.assertThat(pageOne.fileOverviews()).isEmpty();
                softly.assertThat(pageOne.lastPage()).isTrue();
                softly.assertThat(pageOne.totalElements()).isEqualTo(PaginationConstants.TWENTY_ELEMENTS);
                softly.assertThat(pageOne.pageSize()).isEqualTo(PaginationConstants.FILE_PAGE_SIZE);
            });
            assertThat(persistenceQueries.fileState())
                    .as("File reads must preserve stored bytes, relationships and outbox")
                    .isEqualTo(beforeRead);
        }

        private Set<UUID> prepareAndUploadFiles(int fileAmount) {
            List<MockMultipartFile> fileDataCycle = allowedFileProvider().toList();

            return IntStream.range(0, fileAmount)
                    .mapToObj(i -> {
                        MockMultipartFile fileData = fileDataCycle.get(i % fileDataCycle.size());
                        try {
                            FileUploadDto dto = FileUploadDtoTestBuilder.jpgFile()
                                .file(MultipartFileTestBuilder.jpgFile()
                                        .originalFileName("allowed_" + i + "_" + fileData.getOriginalFilename())
                                        .contentType(fileData.getContentType())
                                        .content(fileData.getBytes())
                                        .buildMultipartFile())
                                .userFilename("user_filename_" + i)
                                .build();
                            return fileService.uploadFileToEvent(dto, savedEventId).getId();
                        } catch (IOException e) {
                            return fail("File upload failed while preparing page fixture", e);
                        }
                    })
                    .collect(Collectors.toUnmodifiableSet());
        }
    }
}
