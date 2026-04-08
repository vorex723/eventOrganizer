package com.mazurek.eventOrganizer.file;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.TestFileContentFactory;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotEventAttenderException;
import com.mazurek.eventOrganizer.exception.file.EmptyUploadedFileException;
import com.mazurek.eventOrganizer.exception.file.FileNotFoundException;
import com.mazurek.eventOrganizer.exception.file.FileNotFoundInEventException;
import com.mazurek.eventOrganizer.exception.file.FileTypeNotAllowedException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestDataInitializer;
import com.mazurek.eventOrganizer.testData.builders.dto.FileUploadDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.MultipartFileTestBuilder;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest
@Profile("test")
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

    private UUID savedEventId;

    private record TestFileData(String extension, String contentType, byte[] bytes) {}

    static Stream<TestFileData> allowedFileProvider() {
        return Stream.of(
                new TestFileData(".jpg", "image/jpeg", TestFileContentFactory.jpg()),
                new TestFileData(".jpeg", "image/jpeg", TestFileContentFactory.jpeg()),
                new TestFileData(".png", "image/png", TestFileContentFactory.png()),
                new TestFileData(".pdf", "application/pdf", TestFileContentFactory.pdf()),
                new TestFileData(".doc", "application/msword", TestFileContentFactory.doc()),
                new TestFileData(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", TestFileContentFactory.docx()),
                new TestFileData(".ppt", "application/vnd.ms-powerpoint", TestFileContentFactory.ppt()),
                new TestFileData(".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation", TestFileContentFactory.pptx()),
                new TestFileData(".odt", "application/vnd.oasis.opendocument.text", TestFileContentFactory.odt()),
                new TestFileData(".xls", "application/vnd.ms-excel", TestFileContentFactory.xls()),
                new TestFileData(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", TestFileContentFactory.xlsx()),
                new TestFileData(".mp4", "video/mp4", TestFileContentFactory.mp4()),
                new TestFileData(".avi", "video/x-msvideo", TestFileContentFactory.avi())
        );
    }

    static Stream<TestFileData> disallowedFileProvider() {
        return Stream.of(
                new TestFileData(".exe", "application/octet-stream", new byte[]{0x4D, 0x5A, 0x50, 0x00}),
                new TestFileData(".bat", "text/plain", "@echo off".getBytes()),
                new TestFileData(".zip", "application/zip", new byte[]{0x50, 0x4B, 0x03, 0x04}),
                new TestFileData(".js", "application/javascript", "alert('hack');".getBytes()),
                new TestFileData(".sh", "application/x-sh", "echo test".getBytes())
        );
    }

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        SecurityContextHolder.clearContext();
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

            assertThatThrownBy(() -> fileService.uploadFileToEvent(fileUploadDto, EventConstants.NOT_EXISTING_EVENT_ID))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("When uploading file should throw NotEventAttenderException if performing user is not attending event")
        public void whenUploadingFileShouldThrowNotEventAttenderExceptionIfPerformingUserIsNotAttendingEvent() {
            authHelper.setupSecurityContextForSecondUser();

            assertThatThrownBy(() -> fileService.uploadFileToEvent(fileUploadDto, savedEventId))
                    .isInstanceOf(NotEventAttenderException.class);
        }

        @Test
        @DisplayName("When uploading file should throw EmptyUploadedFileException if file has no content")
        public void whenUploadingFileShouldThrowEmptyUploadedFileExceptionIfFileHasNoContent() {
            authHelper.setupSecurityContextForFirstUser();
            fileUploadDto = FileUploadDtoTestBuilder.emptyJpgFile().build();

            assertThatThrownBy(() -> fileService.uploadFileToEvent(fileUploadDto, savedEventId))
                    .isInstanceOf(EmptyUploadedFileException.class);
        }

        @Test
        @DisplayName("When uploading file should throw FileTypeNotAllowedException if file is not on whitelist")
        public void whenUploadingFileShouldThrowFileTypeNotAllowedExceptionIfFileIsNotOnWhitelist() {
            authHelper.setupSecurityContextForFirstUser();
            fileUploadDto = FileUploadDtoTestBuilder.malwareFile().build();

            assertThatThrownBy(() -> fileService.uploadFileToEvent(fileUploadDto, savedEventId))
                    .isInstanceOf(FileTypeNotAllowedException.class);
        }

        @Test
        @DisplayName("When uploading file should save file with correct data and relationships in database")
        public void whenUploadingFileShouldSaveFileWithCorrectDataAndRelationshipsInDatabase() throws IOException {
            authHelper.setupSecurityContextForFirstUser();
            Instant beforeUpload = Instant.now().truncatedTo(ChronoUnit.MINUTES);
            MockMultipartFile multipartFile = (MockMultipartFile) fileUploadDto.getFile();

            UUID savedFileId = fileService.uploadFileToEvent(fileUploadDto, savedEventId).getId();

            File savedFile = fileRepository.findById(savedFileId).orElseThrow(FileNotFoundException::new);
            User uploader = userRepository.findByEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);
            Set<File> userFiles = fileRepository.findByOwnerId(uploader.getId());
            Set<File> eventFiles = fileRepository.findByEventId(savedEventId);
            byte[] expectedBytes = multipartFile.getBytes();


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
                        .as("Content type should match uploaded file")
                        .isEqualTo(multipartFile.getContentType());
                softly.assertThat(savedFile.getContent())
                        .as("File content should be persisted unchanged")
                        .isEqualTo(expectedBytes);
                softly.assertThat(savedFile.getUploadDateTime())
                        .as("Upload date time should be set, truncated to minutes, and not before upload started")
                        .isAfterOrEqualTo(beforeUpload)
                        .isEqualTo(savedFile.getUploadDateTime().truncatedTo(ChronoUnit.MINUTES));
                softly.assertThat(userFiles)
                        .as("Uploaded file should be present in user's files")
                        .contains(savedFile);
                softly.assertThat(eventFiles)
                        .as("Uploaded file should be present in event's files")
                        .contains(savedFile);
            });
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

            assertThatThrownBy(() -> fileService.getFileOverviewById(savedFileId, EventConstants.NOT_EXISTING_EVENT_ID))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("When getting file overview by id should throw NotEventAttenderException if user is not attending event")
        public void whenGettingFileOverviewByIdShouldThrowNotEventAttenderExceptionIfUserIsNotAttendingEvent() {
            authHelper.setupSecurityContextForSecondUser();

            assertThatThrownBy(() -> fileService.getFileOverviewById(savedFileId, savedEventId))
                    .isInstanceOf(NotEventAttenderException.class);
        }

        @Test
        @DisplayName("When getting file overview by id should throw FileNotFoundInEventException if file with given id does not exist")
        public void whenGettingFileOverviewByIdShouldThrowFileNotFoundInEventExceptionIfFileWithGivenIdDoesNotExist() {
            authHelper.setupSecurityContextForFirstUser();

            assertThatThrownBy(() -> fileService.getFileOverviewById(FileConstants.NOT_EXISTING_FILE_ID, savedEventId))
                    .isInstanceOf(FileNotFoundInEventException.class);
        }

        @Test
        @DisplayName("When getting file overview by id should throw FileNotFoundInEventException if file and event exist but are not related")
        public void whenGettingFileOverviewByIdShouldThrowFileNotFoundInEventExceptionIfFileAndEventExistButAreNotRelated() {
            UUID secondEventId = testDataInitializer.setupEventByFirstUser();
            authHelper.setupSecurityContextForFirstUser();

            assertThatThrownBy(() -> fileService.getFileOverviewById(savedFileId, secondEventId))
                    .isInstanceOf(FileNotFoundInEventException.class);
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

            assertThatThrownBy(() -> fileService.getFileDataById(savedFileId, EventConstants.NOT_EXISTING_EVENT_ID))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("When getting file data by id should throw NotEventAttenderException if user is not attending event")
        public void whenGettingFileDataByIdShouldThrowNotEventAttenderExceptionIfUserIsNotAttendingEvent() {
            authHelper.setupSecurityContextForSecondUser();

            assertThatThrownBy(() -> fileService.getFileDataById(savedFileId, savedEventId))
                    .isInstanceOf(NotEventAttenderException.class);
        }

        @Test
        @DisplayName("When getting file data by id should throw FileNotFoundInEventException if file with given id does not exist")
        public void whenGettingFileDataByIdShouldThrowFileNotFoundInEventExceptionIfFileWithGivenIdDoesNotExist() {
            authHelper.setupSecurityContextForFirstUser();

            assertThatThrownBy(() -> fileService.getFileDataById(FileConstants.NOT_EXISTING_FILE_ID, savedEventId))
                    .isInstanceOf(FileNotFoundInEventException.class);
        }

        @Test
        @DisplayName("When getting file data by id should throw FileNotFoundInEventException if file and event exist but are not related")
        public void whenGettingFileDataByIdShouldThrowFileNotFoundInEventExceptionIfFileAndEventExistButAreNotRelated() {
            UUID secondEventId = testDataInitializer.setupEventByFirstUser();
            authHelper.setupSecurityContextForFirstUser();

            assertThatThrownBy(() -> fileService.getFileDataById(savedFileId, secondEventId))
                    .isInstanceOf(FileNotFoundInEventException.class);
        }
    }

    @Nested
    @DisplayName("Get file overview page by event id tests:")
    class GetFileOverviewPageByEventIdTests {

        @Test
        @DisplayName("When getting file overview page should throw InvalidPageNumberException if page number is negative")
        public void whenGettingFileOverviewPageShouldThrowInvalidPageNumberExceptionIfPageNumberIsNegative() {
            authHelper.setupSecurityContextForFirstUser();

            assertThatThrownBy(() -> fileService.getFileOverviewPageByEventId(savedEventId, PaginationConstants.PAGE_MINUS_ONE))
                    .isInstanceOf(InvalidPageNumberException.class);
        }

        @Test
        @DisplayName("When getting file overview page should throw EventNotFoundException if event with given id does not exist")
        public void whenGettingFileOverviewPageShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
            authHelper.setupSecurityContextForFirstUser();

            assertThatThrownBy(() -> fileService.getFileOverviewPageByEventId(EventConstants.NOT_EXISTING_EVENT_ID, PaginationConstants.PAGE_ZERO))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("When getting file overview page should throw NotEventAttenderException if user is not attending event")
        public void whenGettingFileOverviewPageShouldThrowNotEventAttenderExceptionIfUserIsNotAttendingEvent() {
            authHelper.setupSecurityContextForSecondUser();

            assertThatThrownBy(() -> fileService.getFileOverviewPageByEventId(savedEventId, PaginationConstants.PAGE_ZERO))
                    .isInstanceOf(NotEventAttenderException.class);
        }

        @Test
        @DisplayName("When getting file overview page should return empty page if there are no files in event")
        public void whenGettingFileOverviewPageShouldReturnEmptyPageIfThereAreNoFilesInEvent() {
            authHelper.setupSecurityContextForFirstUser();

            FileOverviewPageDto returnedPage = fileService.getFileOverviewPageByEventId(savedEventId, PaginationConstants.PAGE_ZERO);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(returnedPage.fileOverviews()).isEmpty();
                softly.assertThat(returnedPage.lastPage()).isTrue();
                softly.assertThat(returnedPage.totalElements()).isEqualTo(PaginationConstants.PAGE_ZERO);
                softly.assertThat(returnedPage.pageSize()).isEqualTo(PaginationConstants.FILE_PAGE_SIZE);
            });
        }

        @Test
        @DisplayName("When getting file overview page should return correct page when file count is below page size")
        public void whenGettingFileOverviewPageShouldReturnCorrectPageWhenFileCountIsBelowPageSize() {
            authHelper.setupSecurityContextForFirstUser();
            Set<UUID> savedFilesIds = prepareAndUploadFiles(PaginationConstants.FIVE_ELEMENTS);

            FileOverviewPageDto returnedPage = fileService.getFileOverviewPageByEventId(savedEventId, PaginationConstants.PAGE_ZERO);

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
        }

        @Test
        @DisplayName("When getting file overview page should return files sorted from oldest to newest by upload date time")
        public void whenGettingFileOverviewPageShouldReturnFilesSortedFromOldestToNewestByUploadDateTime() {
            authHelper.setupSecurityContextForFirstUser();
            prepareAndUploadFiles(PaginationConstants.FIVE_ELEMENTS);

            List<File> uploadedFiles = fileRepository.findAll();
            AtomicInteger minutesAmount = new AtomicInteger(1);
            uploadedFiles.forEach(file -> file.setUploadDateTime(
                    file.getUploadDateTime().plusSeconds(60L * minutesAmount.getAndIncrement())));
            fileRepository.saveAll(uploadedFiles);

            FileOverviewPageDto returnedPage = fileService.getFileOverviewPageByEventId(savedEventId, PaginationConstants.PAGE_ZERO);

            List<Instant> uploadTimes = returnedPage.fileOverviews().stream()
                    .map(FileOverviewDto::getUploadDateTime)
                    .toList();

            assertThat(uploadTimes)
                    .as("Files should be sorted from oldest to newest by uploadDateTime")
                    .isSortedAccordingTo(Comparator.naturalOrder());
        }

        @Test
        @DisplayName("When getting file overview page should return exactly twenty files as first and last page when file count equals page size")
        public void whenGettingFileOverviewPageShouldReturnExactlyTwentyFilesWhenFileCountEqualsPageSize() {
            authHelper.setupSecurityContextForFirstUser();
            Set<UUID> savedFilesIds = prepareAndUploadFiles(PaginationConstants.TWENTY_ELEMENTS);

            FileOverviewPageDto returnedPage = fileService.getFileOverviewPageByEventId(savedEventId, PaginationConstants.PAGE_ZERO);

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
        }

        @Test
        @DisplayName("When getting file overview page should split files across two pages when file count exceeds page size")
        public void whenGettingFileOverviewPageShouldSplitFilesAcrossTwoPagesWhenFileCountExceedsPageSize() {
            authHelper.setupSecurityContextForFirstUser();
            Set<UUID> savedFilesIds = prepareAndUploadFiles(PaginationConstants.THIRTY_ELEMENTS);

            FileOverviewPageDto pageZero = fileService.getFileOverviewPageByEventId(savedEventId, PaginationConstants.PAGE_ZERO);
            FileOverviewPageDto pageOne = fileService.getFileOverviewPageByEventId(savedEventId, PaginationConstants.PAGE_ONE);

            Set<UUID> pageZeroIds = pageZero.fileOverviews().stream().map(FileOverviewDto::getId).collect(Collectors.toSet());
            Set<UUID> pageOneIds = pageOne.fileOverviews().stream().map(FileOverviewDto::getId).collect(Collectors.toSet());
            Set<UUID> allReturnedIds = new HashSet<>(pageZeroIds);
            allReturnedIds.addAll(pageOneIds);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(pageZero.pageNumber()).isEqualTo(PaginationConstants.PAGE_ZERO);
                softly.assertThat(pageZero.fileOverviews()).hasSize(PaginationConstants.FILE_PAGE_SIZE);
                softly.assertThat(pageZero.lastPage()).isFalse();
                softly.assertThat(pageZero.totalElements()).isEqualTo(PaginationConstants.THIRTY_ELEMENTS);
                softly.assertThat(pageZero.pageSize()).isEqualTo(PaginationConstants.FILE_PAGE_SIZE);

                softly.assertThat(pageOne.pageNumber()).isEqualTo(PaginationConstants.PAGE_ONE);
                softly.assertThat(pageOne.fileOverviews()).hasSize(PaginationConstants.TEN_ELEMENTS);
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
        }

        @Test
        @DisplayName("When getting file overview page should return empty page if requested page number exceeds available pages")
        public void whenGettingFileOverviewPageShouldReturnEmptyPageIfRequestedPageNumberExceedsAvailablePages() {
            authHelper.setupSecurityContextForFirstUser();
            Set<UUID> savedFilesIds = prepareAndUploadFiles(PaginationConstants.TWENTY_ELEMENTS);

            FileOverviewPageDto pageZero = fileService.getFileOverviewPageByEventId(savedEventId, PaginationConstants.PAGE_ZERO);
            FileOverviewPageDto pageOne = fileService.getFileOverviewPageByEventId(savedEventId, PaginationConstants.PAGE_ONE);

            SoftAssertions.assertSoftly(softly -> {
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
        }

        private Set<UUID> prepareAndUploadFiles(int fileAmount) {
            List<TestFileData> fileDataCycle = allowedFileProvider().toList();

            return IntStream.range(0, fileAmount)
                    .mapToObj(i -> {
                        TestFileData fileData = fileDataCycle.get(i % fileDataCycle.size());
                        FileUploadDto dto = FileUploadDtoTestBuilder.jpgFile()
                                .file(MultipartFileTestBuilder.jpgFile()
                                        .originalFileName("allowed_" + i + fileData.extension())
                                        .contentType(fileData.contentType())
                                        .content(fileData.bytes())
                                        .buildMultipartFile())
                                .userFilename("user_filename_" + i)
                                .build();
                        try {
                            return fileService.uploadFileToEvent(dto, savedEventId).getId();
                        } catch (IOException e) {
                            return fail("File upload failed: " + e.getMessage());
                        }
                    })
                    .collect(Collectors.toUnmodifiableSet());
        }
    }
}
