package com.mazurek.eventOrganizer.file;

import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.config.properties.CommunityProperties;
import com.mazurek.eventOrganizer.config.properties.PaginationProperties;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.event.EventRepository;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotEventAttendeeException;
import com.mazurek.eventOrganizer.exception.file.EmptyUploadedFileException;
import com.mazurek.eventOrganizer.exception.file.EventFileQuotaExceededException;
import com.mazurek.eventOrganizer.exception.file.FileNotFoundInEventException;
import com.mazurek.eventOrganizer.exception.file.FileTypeNotAllowedException;
import com.mazurek.eventOrganizer.notification.service.NotificationCommandService;
import com.mazurek.eventOrganizer.testData.TestFileContentFactory;
import com.mazurek.eventOrganizer.testData.builders.CityTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.CommunityPropertiesTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.EventTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.FileContentProjectionTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.FileOverviewProjectionTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.FileTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.FileUploadDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.MultipartFileTestBuilder;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.utils.FileUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileService unit tests:")
public class FileServiceUnitTest {

    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private NotificationCommandService notificationCommandService;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private FileUtils fileUtils;
    @Mock
    private PaginationProperties paginationProperties;
    private CommunityProperties communityProperties;
    private final Clock clock = TimeConstants.FIXED_CLOCK;

    private FileService fileService;

    private City cityWarsaw;
    private User firstUser;
    private User secondUser;
    private Event event;
    private Optional<Event> eventOptional;

    @BeforeEach
    void setUp() {
        communityProperties = new CommunityPropertiesTestBuilder().build();
        fileService = new FileService(
                authenticationService,
                notificationCommandService,
                eventRepository,
                fileRepository,
                fileUtils,
                paginationProperties,
                communityProperties,
                clock
        );
        cityWarsaw = CityTestBuilder.warsaw().build();

        firstUser = UserTestBuilder.firstUser().homeCity(cityWarsaw).build();
        secondUser = UserTestBuilder.secondUser().homeCity(cityWarsaw).build();

        event = EventTestBuilder
                .firstEvent()
                .owner(firstUser)
                .city(cityWarsaw)
                .build();

        eventOptional = Optional.of(event);

        event.addAttendee(secondUser);
    }

    @Nested
    @DisplayName("Upload file tests:")
    class UploadFileTests {

        private File saveFileReturn;
        private FileUploadDto fileUploadDto;
        private MockMultipartFile jpgMultipartFile;

        @BeforeEach
        void setUp() {
            jpgMultipartFile = MultipartFileTestBuilder.jpgFile().buildMultipartFile();

            fileUploadDto = FileUploadDtoTestBuilder.jpgFile()
                    .file(jpgMultipartFile)
                    .build();

            saveFileReturn = FileTestBuilder.jpgFile().build();
        }

        private void setupSuccessfulFileUploadMocks() throws IOException {
            setupSuccessfulFileUploadMocks(firstUser);
        }

        private void setupSuccessfulFileUploadMocks(User uploadingUser) throws IOException {
            when(eventRepository.findByIdForUpdate(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(uploadingUser);
            when(eventRepository.isUserAttendeeOrOwner(uploadingUser.getId(), EventConstants.FIRST_EVENT_ID)).thenReturn(true);
            when(eventRepository.findAttendeeIdsByEventId(EventConstants.FIRST_EVENT_ID)).thenReturn(List.of(secondUser.getId()));
            when(fileUtils.detectValidatedContentType(eq(jpgMultipartFile.getOriginalFilename()), any(byte[].class)))
                    .thenReturn(Optional.of(FileConstants.JPG_FILE_CONTENT_TYPE));
            when(fileRepository.save(any(File.class))).thenReturn(saveFileReturn);
        }

        @Test
        @DisplayName("When uploading file should load and lock event with given id from database")
        public void whenUploadingFileShouldLoadAndLockEventWithGivenIdFromDatabase() throws IOException {
            setupSuccessfulFileUploadMocks();

            fileService.uploadFileToEvent(fileUploadDto, EventConstants.FIRST_EVENT_ID);

            verify(eventRepository, times(1)).findByIdForUpdate(EventConstants.FIRST_EVENT_ID);
            verify(eventRepository, never()).findById(any(UUID.class));
        }

        @Test
        @DisplayName("When uploading file should throw EventNotFoundException if event with given id does not exist")
        public void whenUploadingFileShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
            when(eventRepository.findByIdForUpdate(EventConstants.FIRST_EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fileService.uploadFileToEvent(fileUploadDto, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(EventNotFoundException.class);

            verify(eventRepository).findByIdForUpdate(EventConstants.FIRST_EVENT_ID);
            verify(eventRepository, never()).findById(any(UUID.class));

            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
        }

        @Test
        @DisplayName("When uploading file should retrieve performing user from AuthenticationService")
        public void whenUploadingFileShouldRetrievePerformingUserFromAuthenticationService() throws IOException {
            setupSuccessfulFileUploadMocks();

            fileService.uploadFileToEvent(fileUploadDto, EventConstants.FIRST_EVENT_ID);

            verify(authenticationService, times(1)).getCurrentUser();
        }

        @Test
        @DisplayName("When uploading file should throw NotEventAttendeeException if performing user does not attend event with given id")
        public void whenUploadingFileShouldThrowNotEventAttendeeExceptionIfPerformingUserDoesNotAttendEventWithGivenId() {
            when(eventRepository.findByIdForUpdate(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(secondUser);
            when(eventRepository.isUserAttendeeOrOwner(secondUser.getId(), EventConstants.FIRST_EVENT_ID)).thenReturn(false);

            assertThatThrownBy(() -> fileService.uploadFileToEvent(fileUploadDto, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(NotEventAttendeeException.class);

            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
        }

        @Test
        @DisplayName("When uploading file should throw EmptyUploadedFileException if file is empty")
        public void whenUploadingFileShouldThrowEmptyUploadedFileExceptionIfFileIsEmpty() {
            when(eventRepository.findByIdForUpdate(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(eventRepository.isUserAttendeeOrOwner(firstUser.getId(), EventConstants.FIRST_EVENT_ID)).thenReturn(true);

            MockMultipartFile emptyFile = MultipartFileTestBuilder
                    .jpgFile()
                    .content(new byte[0])
                    .buildMultipartFile();

            fileUploadDto.setFile(emptyFile);

            assertThatThrownBy(() -> fileService.uploadFileToEvent(fileUploadDto, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(EmptyUploadedFileException.class);

            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
        }

        @Test
        @DisplayName("When an event already has its maximum number of files should reject upload")
        void whenEventFileCountIsAtLimitShouldRejectUpload() {
            when(eventRepository.findByIdForUpdate(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(eventRepository.isUserAttendeeOrOwner(firstUser.getId(), EventConstants.FIRST_EVENT_ID)).thenReturn(true);
            when(fileRepository.countByEventId(EventConstants.FIRST_EVENT_ID)).thenReturn((long) PropertyFixtureConstants.MAX_FILES_PER_EVENT);

            assertThatThrownBy(() -> fileService.uploadFileToEvent(fileUploadDto, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(EventFileQuotaExceededException.class);

            verify(fileRepository, never()).save(any(File.class));
            verifyNoInteractions(fileUtils);
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
        }

        @Test
        @DisplayName("When an event would exceed its total file storage quota should reject upload")
        void whenEventStorageQuotaWouldBeExceededShouldRejectUpload() {
            when(eventRepository.findByIdForUpdate(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(eventRepository.isUserAttendeeOrOwner(firstUser.getId(), EventConstants.FIRST_EVENT_ID)).thenReturn(true);
            when(fileRepository.totalContentBytesByEventId(EventConstants.FIRST_EVENT_ID))
                    .thenReturn(PropertyFixtureConstants.MAX_EVENT_FILE_STORAGE.toBytes() - 1);

            assertThatThrownBy(() -> fileService.uploadFileToEvent(fileUploadDto, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(EventFileQuotaExceededException.class);

            verify(fileRepository, never()).save(any(File.class));
            verifyNoInteractions(fileUtils);
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
        }

        @Test
        @DisplayName("When uploading file should throw FileTypeNotAllowedException if detected file type is not on whitelist")
        public void whenUploadingFileShouldThrowFileTypeNotAllowedExceptionIfDetectedFileTypeIsNotOnWhitelist() throws IOException {
            when(eventRepository.findByIdForUpdate(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(eventRepository.isUserAttendeeOrOwner(firstUser.getId(), EventConstants.FIRST_EVENT_ID)).thenReturn(true);
            when(fileUtils.detectValidatedContentType(eq(jpgMultipartFile.getOriginalFilename()), any(byte[].class)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> fileService.uploadFileToEvent(fileUploadDto, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(FileTypeNotAllowedException.class);

            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
        }

        @Test
        @DisplayName("When uploading file should save it with correct data and relationships")
        public void whenUploadingFileShouldSaveItWithCorrectDataAndRelationships() throws IOException {
            setupSuccessfulFileUploadMocks();
            byte[] expectedContent = fileUploadDto.getFile().getBytes();

            ArgumentCaptor<File> fileArgumentCaptor = ArgumentCaptor.forClass(File.class);

            fileService.uploadFileToEvent(fileUploadDto, EventConstants.FIRST_EVENT_ID);

            verify(fileRepository, times(1)).save(fileArgumentCaptor.capture());
            File capturedFile = fileArgumentCaptor.getValue();

            assertThat(capturedFile).as("Expected capturedFile before field assertions").isNotNull();
            assertThat(capturedFile.getOwner()).as("Expected related record before dereference").isNotNull();
            assertThat(capturedFile.getEvent()).as("Expected related record before dereference").isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(capturedFile.getOwner())
                        .as("File owner should be set to performing user")
                        .isEqualTo(firstUser);
                softly.assertThat(capturedFile.getEvent())
                        .as("File event should be set to the event with given id")
                        .isEqualTo(event);
                softly.assertThat(capturedFile.getUserFileName())
                        .as("User file name should match dto")
                        .isEqualTo(fileUploadDto.getUserFilename());
                softly.assertThat(capturedFile.getOriginalFileName())
                        .as("Original file name should match multipart file")
                        .isEqualTo(jpgMultipartFile.getOriginalFilename());
                softly.assertThat(capturedFile.getContentType())
                        .as("Content type should use validated MIME type")
                        .isEqualTo(FileConstants.JPG_FILE_CONTENT_TYPE);
                softly.assertThat(capturedFile.getContent())
                        .as("File content should be unchanged")
                        .isEqualTo(expectedContent);
                softly.assertThat(capturedFile.getUploadDateTime())
                        .as("Upload date time should be set and truncated to minutes")
                        .isEqualTo(TimeConstants.NOW.truncatedTo(ChronoUnit.MINUTES))
                        .isEqualTo(capturedFile.getUploadDateTime().truncatedTo(ChronoUnit.MINUTES));
            });
        }

        @Test
        @DisplayName("When uploading file should persist validated content type instead of client provided one")
        public void whenUploadingFileShouldPersistValidatedContentTypeInsteadOfClientProvidedOne() throws IOException {
            MockMultipartFile mismatchedMimeJpgFile = MultipartFileTestBuilder.jpgFile()
                    .contentType(FileConstants.PDF_FILE_CONTENT_TYPE)
                    .buildMultipartFile();
            fileUploadDto = FileUploadDtoTestBuilder.jpgFile()
                    .file(mismatchedMimeJpgFile)
                    .build();

            when(eventRepository.findByIdForUpdate(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(eventRepository.isUserAttendeeOrOwner(firstUser.getId(), EventConstants.FIRST_EVENT_ID)).thenReturn(true);
            when(fileUtils.detectValidatedContentType(eq(mismatchedMimeJpgFile.getOriginalFilename()), any(byte[].class)))
                    .thenReturn(Optional.of(FileConstants.JPG_FILE_CONTENT_TYPE));
            when(fileRepository.save(any(File.class))).thenReturn(saveFileReturn);

            fileService.uploadFileToEvent(fileUploadDto, EventConstants.FIRST_EVENT_ID);

            ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
            verify(fileRepository).save(fileCaptor.capture());
            assertThat(fileCaptor.getValue().getContentType()).isEqualTo(FileConstants.JPG_FILE_CONTENT_TYPE);
        }

        @Test
        @DisplayName("When uploading file should not initialize or save inverse event and user collections")
        public void whenUploadingFileShouldNotSaveInverseCollections() throws IOException {
            setupSuccessfulFileUploadMocks();

            fileService.uploadFileToEvent(fileUploadDto, EventConstants.FIRST_EVENT_ID);

            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        @DisplayName("When event owner uploads file should notify event attendees")
        void whenEventOwnerUploadsFileShouldNotifyEventAttendees() throws IOException {
            setupSuccessfulFileUploadMocks();

            fileService.uploadFileToEvent(fileUploadDto, EventConstants.FIRST_EVENT_ID);

            verify(notificationCommandService).notifyNewEventFile(
                    EventConstants.FIRST_EVENT_ID,
                    saveFileReturn.getId(),
                    List.of(secondUser.getId()),
                    firstUser.getFullName()
            );
        }

        @Test
        @DisplayName("When owner uploads a file without attendees should have no notification recipients")
        void whenOwnerUploadsFileWithoutAttendeesShouldHaveNoRecipients() throws IOException {
            setupSuccessfulFileUploadMocks();
            when(eventRepository.findAttendeeIdsByEventId(EventConstants.FIRST_EVENT_ID)).thenReturn(List.of());

            fileService.uploadFileToEvent(fileUploadDto, EventConstants.FIRST_EVENT_ID);

            verify(notificationCommandService).notifyNewEventFile(EventConstants.FIRST_EVENT_ID,
                    saveFileReturn.getId(), List.of(), firstUser.getFullName());
        }

        @Test
        @DisplayName("When uploading file should exclude uploader and duplicate recipients")
        void whenUploadingFileShouldExcludeUploaderAndDuplicateRecipients() throws IOException {
            setupSuccessfulFileUploadMocks();
            when(eventRepository.findAttendeeIdsByEventId(EventConstants.FIRST_EVENT_ID))
                    .thenReturn(List.of(firstUser.getId(), secondUser.getId(), secondUser.getId()));

            fileService.uploadFileToEvent(fileUploadDto, EventConstants.FIRST_EVENT_ID);

            verify(notificationCommandService).notifyNewEventFile(EventConstants.FIRST_EVENT_ID,
                    saveFileReturn.getId(), List.of(secondUser.getId()), firstUser.getFullName());
        }

        @Test
        @DisplayName("When event attendee uploads file should notify event owner and exclude uploader")
        void whenEventAttendeeUploadsFileShouldNotifyEventOwnerAndExcludeUploader() throws IOException {
            setupSuccessfulFileUploadMocks(secondUser);

            fileService.uploadFileToEvent(fileUploadDto, EventConstants.FIRST_EVENT_ID);

            verify(notificationCommandService).notifyNewEventFile(
                    EventConstants.FIRST_EVENT_ID,
                    saveFileReturn.getId(),
                    List.of(firstUser.getId()),
                    secondUser.getFullName()
            );
        }

    }

    @Nested
    @DisplayName("Get file overview by id tests:")
    class GetFileOverviewByIdTests {

        private File fileToServe;
        private Optional<FileOverviewProjection> fileToServeOptional;

        @BeforeEach
        void setUp() {
            fileToServe = FileTestBuilder.pdfFile().event(event).owner(firstUser).build();
            fileToServeOptional = Optional.of(overviewProjection(fileToServe));
        }

        private void setupSuccessfulGetFileOverviewMocks() {
            when(authenticationService.getCurrentUserId()).thenReturn(firstUser.getId());
            when(eventRepository.isUserAttendeeOrOwner(firstUser.getId(), EventConstants.FIRST_EVENT_ID)).thenReturn(true);
            when(eventRepository.existsById(EventConstants.FIRST_EVENT_ID)).thenReturn(true);
            when(fileRepository.findOverviewByIdAndEventId(FileConstants.PDF_FILE_ID, EventConstants.FIRST_EVENT_ID)).thenReturn(fileToServeOptional);
        }

        @Test
        @DisplayName("When getting file overview by id should retrieve performing user ID from AuthenticationService")
        public void whenGettingFileOverviewByIdShouldRetrievePerformingUserId() {
            setupSuccessfulGetFileOverviewMocks();

            fileService.getFileOverviewById(FileConstants.PDF_FILE_ID, EventConstants.FIRST_EVENT_ID);

            verify(authenticationService, times(1)).getCurrentUserId();
            verify(authenticationService, never()).getCurrentUser();
            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
        }

        @Test
        @DisplayName("When getting file overview by id should check event existence without loading it")
        public void whenGettingFileOverviewByIdShouldCheckEventExistenceWithoutLoadingIt() {
            setupSuccessfulGetFileOverviewMocks();

            fileService.getFileOverviewById(FileConstants.PDF_FILE_ID, EventConstants.FIRST_EVENT_ID);

            verify(eventRepository, times(1)).existsById(EventConstants.FIRST_EVENT_ID);
            verify(eventRepository, never()).findById(any(UUID.class));
            verify(eventRepository).isUserAttendeeOrOwner(firstUser.getId(), EventConstants.FIRST_EVENT_ID);
            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
        }

        @Test
        @DisplayName("When getting file overview by id should throw EventNotFoundException if there is no event with given id")
        public void whenGettingFileOverviewByIdShouldThrowEventNotFoundExceptionIfThereIsNoEventWithGivenId() {
            when(authenticationService.getCurrentUserId()).thenReturn(firstUser.getId());
            when(eventRepository.existsById(EventConstants.FIRST_EVENT_ID)).thenReturn(false);

            assertThatThrownBy(() -> fileService.getFileOverviewById(FileConstants.PDF_FILE_ID, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(EventNotFoundException.class);
            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
        }

        @Test
        @DisplayName("When getting file overview by id should throw NotEventAttendeeException if performing user is not attending event")
        public void whenGettingFileOverviewByIdShouldThrowNotEventAttendeeExceptionIfPerformingUserIsNotAttendingEvent() {
            when(authenticationService.getCurrentUserId()).thenReturn(secondUser.getId());
            when(eventRepository.isUserAttendeeOrOwner(secondUser.getId(), EventConstants.FIRST_EVENT_ID)).thenReturn(false);
            when(eventRepository.existsById(EventConstants.FIRST_EVENT_ID)).thenReturn(true);

            assertThatThrownBy(() -> fileService.getFileOverviewById(FileConstants.PDF_FILE_ID, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(NotEventAttendeeException.class);
            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
        }

        @Test
        @DisplayName("When getting file overview by id should load file by file id and event id")
        public void whenGettingFileOverviewByIdShouldLoadFileByFileIdAndEventId() {
            setupSuccessfulGetFileOverviewMocks();

            fileService.getFileOverviewById(FileConstants.PDF_FILE_ID, EventConstants.FIRST_EVENT_ID);

            verify(fileRepository, times(1)).findOverviewByIdAndEventId(FileConstants.PDF_FILE_ID, EventConstants.FIRST_EVENT_ID);
            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
        }

        @Test
        @DisplayName("When getting file overview by id should throw FileNotFoundInEventException if there is no file with given id in event with given id")
        public void whenGettingFileOverviewByIdShouldThrowFileNotFoundInEventExceptionIfThereIsNoFileWithGivenIdInEventWithGivenId() {
            when(authenticationService.getCurrentUserId()).thenReturn(firstUser.getId());
            when(eventRepository.isUserAttendeeOrOwner(firstUser.getId(), EventConstants.FIRST_EVENT_ID)).thenReturn(true);
            when(eventRepository.existsById(EventConstants.FIRST_EVENT_ID)).thenReturn(true);
            when(fileRepository.findOverviewByIdAndEventId(FileConstants.PDF_FILE_ID, EventConstants.FIRST_EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fileService.getFileOverviewById(FileConstants.PDF_FILE_ID, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(FileNotFoundInEventException.class);
            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
        }

        @Test
        @DisplayName("When getting file overview by id should return file overview dto with correct data")
        public void whenGettingFileOverviewByIdShouldReturnDtoWithCorrectData() {
            setupSuccessfulGetFileOverviewMocks();

            FileOverviewDto returnedFileOverview = fileService.getFileOverviewById(FileConstants.PDF_FILE_ID, EventConstants.FIRST_EVENT_ID);

            assertThat(returnedFileOverview).as("Expected returnedFileOverview before field assertions").isNotNull();
            assertThat(returnedFileOverview.getOwner()).as("Expected related record before dereference").isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(returnedFileOverview.getId())
                        .as("Returned file overview id should match source file")
                        .isEqualTo(fileToServe.getId());
                softly.assertThat(returnedFileOverview.getOriginalFilename())
                        .as("Returned file overview original filename should match source file")
                        .isEqualTo(fileToServe.getOriginalFileName());
                softly.assertThat(returnedFileOverview.getUserFilename())
                        .as("Returned file overview user filename should match source file")
                        .isEqualTo(fileToServe.getUserFileName());
                softly.assertThat(returnedFileOverview.getFileContentType())
                        .as("Returned file overview content type should match source file")
                        .isEqualTo(fileToServe.getContentType());
                softly.assertThat(returnedFileOverview.getOwner().getId())
                        .as("Returned file overview owner id should match source file owner")
                        .isEqualTo(fileToServe.getOwner().getId());
                softly.assertThat(returnedFileOverview.getUploadDateTime())
                        .as("Returned file overview upload date time should match source file")
                        .isEqualTo(fileToServe.getUploadDateTime());
            });
            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
        }
    }

    @Nested
    @DisplayName("Get file data by id tests:")
    class GetFileDataByIdTests {

        private File fileToServe;
        private Optional<FileContentProjection> fileToServeOptional;

        @BeforeEach
        void setUp() {
            fileToServe = FileTestBuilder.pdfFile().owner(firstUser).event(event).build();
            fileToServeOptional = Optional.of(contentProjection(fileToServe));
        }

        private void setupSuccessfulGetFileDataMocks() {
            when(authenticationService.getCurrentUserId()).thenReturn(firstUser.getId());
            when(eventRepository.isUserAttendeeOrOwner(firstUser.getId(), EventConstants.FIRST_EVENT_ID)).thenReturn(true);
            when(eventRepository.existsById(EventConstants.FIRST_EVENT_ID)).thenReturn(true);
            when(fileRepository.findContentByIdAndEventId(FileConstants.PDF_FILE_ID, EventConstants.FIRST_EVENT_ID)).thenReturn(fileToServeOptional);
        }

        @Test
        @DisplayName("When getting file data by id should retrieve performing user ID from AuthenticationService")
        public void whenGettingFileDataByIdShouldRetrievePerformingUserId() {
            setupSuccessfulGetFileDataMocks();

            fileService.getFileDataById(FileConstants.PDF_FILE_ID, EventConstants.FIRST_EVENT_ID);

            verify(authenticationService, times(1)).getCurrentUserId();
            verify(authenticationService, never()).getCurrentUser();
            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
        }

        @Test
        @DisplayName("When getting file data by id should check event existence without loading it")
        public void whenGettingFileDataByIdShouldCheckEventExistenceWithoutLoadingIt() {
            setupSuccessfulGetFileDataMocks();

            fileService.getFileDataById(FileConstants.PDF_FILE_ID, EventConstants.FIRST_EVENT_ID);

            verify(eventRepository, times(1)).existsById(EventConstants.FIRST_EVENT_ID);
            verify(eventRepository, never()).findById(any(UUID.class));
            verify(eventRepository).isUserAttendeeOrOwner(firstUser.getId(), EventConstants.FIRST_EVENT_ID);
            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
        }

        @Test
        @DisplayName("When getting file data by id should throw EventNotFoundException if there is no event with given id")
        public void whenGettingFileDataByIdShouldThrowEventNotFoundExceptionIfThereIsNoEventWithGivenId() {
            when(authenticationService.getCurrentUserId()).thenReturn(firstUser.getId());
            when(eventRepository.existsById(EventConstants.FIRST_EVENT_ID)).thenReturn(false);

            assertThatThrownBy(() -> fileService.getFileDataById(FileConstants.PDF_FILE_ID, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(EventNotFoundException.class);
            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
        }

        @Test
        @DisplayName("When getting file data by id should throw NotEventAttendeeException if performing user is not attending event")
        public void whenGettingFileDataByIdShouldThrowNotEventAttendeeExceptionIfPerformingUserIsNotAttendingEvent() {
            when(authenticationService.getCurrentUserId()).thenReturn(secondUser.getId());
            when(eventRepository.isUserAttendeeOrOwner(secondUser.getId(), EventConstants.FIRST_EVENT_ID)).thenReturn(false);
            when(eventRepository.existsById(EventConstants.FIRST_EVENT_ID)).thenReturn(true);

            assertThatThrownBy(() -> fileService.getFileDataById(FileConstants.PDF_FILE_ID, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(NotEventAttendeeException.class);
            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
        }

        @Test
        @DisplayName("When getting file data by id should load file by file id and event id")
        public void whenGettingFileDataByIdShouldLoadFileByFileIdAndEventId() {
            setupSuccessfulGetFileDataMocks();

            fileService.getFileDataById(FileConstants.PDF_FILE_ID, EventConstants.FIRST_EVENT_ID);

            verify(fileRepository, times(1)).findContentByIdAndEventId(FileConstants.PDF_FILE_ID, EventConstants.FIRST_EVENT_ID);
            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
        }

        @Test
        @DisplayName("When getting file data by id should throw FileNotFoundInEventException if there is no file with given id in event with given id")
        public void whenGettingFileDataByIdShouldThrowFileNotFoundInEventExceptionIfThereIsNoFileWithGivenIdInEventWithGivenId() {
            when(authenticationService.getCurrentUserId()).thenReturn(firstUser.getId());
            when(eventRepository.isUserAttendeeOrOwner(firstUser.getId(), EventConstants.FIRST_EVENT_ID)).thenReturn(true);
            when(eventRepository.existsById(EventConstants.FIRST_EVENT_ID)).thenReturn(true);
            when(fileRepository.findContentByIdAndEventId(FileConstants.PDF_FILE_ID, EventConstants.FIRST_EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fileService.getFileDataById(FileConstants.PDF_FILE_ID, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(FileNotFoundInEventException.class);
            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
        }

        @Test
        @DisplayName("When getting file data by id should return content without loading file metadata")
        public void whenGettingFileDataByIdShouldReturnContentProjection() {
            setupSuccessfulGetFileDataMocks();

            FileContentDto returnedFile = fileService.getFileDataById(FileConstants.PDF_FILE_ID, EventConstants.FIRST_EVENT_ID);

            assertThat(returnedFile).as("Expected returnedFile before field assertions").isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(returnedFile.content())
                        .as("Returned file content should be unchanged")
                        .isEqualTo(fileToServe.getContent());
                softly.assertThat(returnedFile.contentType())
                        .isEqualTo(fileToServe.getContentType());
            });
            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
        }
    }

    @Nested
    @DisplayName("Get file overview page by event id tests:")
    class GetFileOverviewPageByEventIdTests {

        final int SECOND_PAGE_SIZE = PaginationConstants.TEN_ELEMENTS;
        final int FILE_COUNT_MAX = PaginationConstants.THIRTY_ELEMENTS;
        final int PAGE_NUMBER_ZERO = PaginationConstants.PAGE_ZERO;
        final int PAGE_NUMBER_ONE = PaginationConstants.PAGE_ONE;
        final int PAGE_COUNT_TWO = 2;

        Page<FileOverviewProjection> filePageOne;
        Page<FileOverviewProjection> filePageTwo;

        @BeforeEach
        void setUp() {
            List<FileOverviewProjection> testFiles = prepareFiles(FILE_COUNT_MAX);

            Pageable firstPageRequest = PageRequest.of(PAGE_NUMBER_ZERO, PaginationConstants.DEFAULT_PAGE_SIZE);
            Pageable secondPageRequest = PageRequest.of(PAGE_NUMBER_ONE, PaginationConstants.DEFAULT_PAGE_SIZE);
            filePageOne = new PageImpl<>(testFiles.subList(PAGE_NUMBER_ZERO, PaginationConstants.DEFAULT_PAGE_SIZE), firstPageRequest, testFiles.size());
            filePageTwo = new PageImpl<>(testFiles.subList(PaginationConstants.DEFAULT_PAGE_SIZE, FILE_COUNT_MAX), secondPageRequest, testFiles.size());
        }

        private void setupSuccessfulGetPageMocks(Page<FileOverviewProjection> page) {
            when(paginationProperties.getDefaultPageSize()).thenReturn(PaginationConstants.DEFAULT_PAGE_SIZE);
            when(authenticationService.getCurrentUserId()).thenReturn(firstUser.getId());
            when(eventRepository.isUserAttendeeOrOwner(firstUser.getId(), EventConstants.FIRST_EVENT_ID)).thenReturn(true);
            when(eventRepository.existsById(EventConstants.FIRST_EVENT_ID)).thenReturn(true);
            when(fileRepository.findOverviewsByEventId(eq(EventConstants.FIRST_EVENT_ID), any(Pageable.class))).thenReturn(page);
        }

        @Test
        @DisplayName("When getting file overview page should retrieve performing user ID from AuthenticationService")
        public void whenGettingFileOverviewPageShouldRetrievePerformingUserId() {
            setupSuccessfulGetPageMocks(filePageOne);

            fileService.getFileOverviewPageByEventId(EventConstants.FIRST_EVENT_ID, PAGE_NUMBER_ZERO);

            verify(authenticationService, times(1)).getCurrentUserId();
            verify(authenticationService, never()).getCurrentUser();
            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
        }

        @Test
        @DisplayName("When getting file overview page should throw InvalidPageNumberException if page number is below zero")
        public void whenGettingFileOverviewPageShouldThrowInvalidPageNumberExceptionIfPageNumberIsBelowZero() {
            assertThatThrownBy(() -> fileService.getFileOverviewPageByEventId(EventConstants.FIRST_EVENT_ID, PaginationConstants.PAGE_MINUS_ONE))
                    .isInstanceOf(InvalidPageNumberException.class);
            verifyNoInteractions(authenticationService, paginationProperties, fileUtils);
            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
        }

        @Test
        @DisplayName("When getting file overview page should check event existence without loading it")
        public void whenGettingFileOverviewPageShouldCheckEventExistenceWithoutLoadingIt() {
            setupSuccessfulGetPageMocks(filePageOne);

            fileService.getFileOverviewPageByEventId(EventConstants.FIRST_EVENT_ID, PAGE_NUMBER_ZERO);

            verify(eventRepository, times(1)).existsById(EventConstants.FIRST_EVENT_ID);
            verify(eventRepository, never()).findById(any(UUID.class));
            verify(eventRepository).isUserAttendeeOrOwner(firstUser.getId(), EventConstants.FIRST_EVENT_ID);
            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
        }

        @Test
        @DisplayName("When getting file overview page should throw EventNotFoundException if event with given id does not exist")
        public void whenGettingFileOverviewPageShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
            when(authenticationService.getCurrentUserId()).thenReturn(firstUser.getId());
            when(eventRepository.existsById(EventConstants.FIRST_EVENT_ID)).thenReturn(false);

            assertThatThrownBy(() -> fileService.getFileOverviewPageByEventId(EventConstants.FIRST_EVENT_ID, PAGE_NUMBER_ZERO))
                    .isInstanceOf(EventNotFoundException.class);
            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
        }

        @Test
        @DisplayName("When getting file overview page should throw NotEventAttendeeException if user is not attending event")
        public void whenGettingFileOverviewPageShouldThrowNotEventAttendeeExceptionIfUserIsNotAttendingEvent() {
            when(authenticationService.getCurrentUserId()).thenReturn(secondUser.getId());
            when(eventRepository.isUserAttendeeOrOwner(secondUser.getId(), EventConstants.FIRST_EVENT_ID)).thenReturn(false);
            when(eventRepository.existsById(EventConstants.FIRST_EVENT_ID)).thenReturn(true);

            assertThatThrownBy(() -> fileService.getFileOverviewPageByEventId(EventConstants.FIRST_EVENT_ID, PAGE_NUMBER_ZERO))
                    .isInstanceOf(NotEventAttendeeException.class);
            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
        }

        @Test
        @DisplayName("When getting file overview page should load page with correct page number, size and sort order")
        public void whenGettingFileOverviewPageShouldLoadPageWithCorrectPageNumberSizeAndSortOrder() {
            setupSuccessfulGetPageMocks(filePageOne);
            ArgumentCaptor<Pageable> pageRequestCaptor = ArgumentCaptor.forClass(Pageable.class);

            fileService.getFileOverviewPageByEventId(EventConstants.FIRST_EVENT_ID, PAGE_NUMBER_ZERO);

            verify(fileRepository, times(1)).findOverviewsByEventId(eq(EventConstants.FIRST_EVENT_ID), pageRequestCaptor.capture());
            Pageable capturedPageRequest = pageRequestCaptor.getValue();

            assertThat(capturedPageRequest).as("Expected capturedPageRequest before field assertions").isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(capturedPageRequest.getPageNumber())
                        .as("Page number should match requested page number")
                        .isEqualTo(PAGE_NUMBER_ZERO);
                softly.assertThat(capturedPageRequest.getPageSize())
                        .as("Page size should be the default page size")
                        .isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(capturedPageRequest.getSort())
                        .as("Results should be sorted by uploadDateTime and id ascending")
                        .isEqualTo(Sort.by("uploadDateTime").ascending().and(Sort.by("id").ascending()));
            });
            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
        }

        @Test
        @DisplayName("When getting file overview page should correctly map full page to FileOverviewPageDto")
        public void whenGettingFileOverviewPageShouldCorrectlyMapFullPageToFileOverviewPageDto() {
            setupSuccessfulGetPageMocks(filePageOne);

            FileOverviewPageDto output = fileService.getFileOverviewPageByEventId(EventConstants.FIRST_EVENT_ID, PAGE_NUMBER_ZERO);

            assertThat(output).as("Expected output before field assertions").isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(output.pageNumber()).isEqualTo(PAGE_NUMBER_ZERO);
                softly.assertThat(output.totalPages()).isEqualTo(PAGE_COUNT_TWO);
                softly.assertThat(output.totalElements()).isEqualTo(FILE_COUNT_MAX);
                softly.assertThat(output.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(output.fileOverviews()).hasSize(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(output.lastPage()).isFalse();
            });
            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
            assertThat(output.fileOverviews()).extracting(FileOverviewDto::getId)
                    .containsExactlyElementsOf(filePageOne.getContent().stream().map(FileOverviewProjection::getId).toList());
            assertThat(output.fileOverviews()).hasSize(PaginationConstants.DEFAULT_PAGE_SIZE);
            FileOverviewDto firstOverview = output.fileOverviews().getFirst();
            FileOverviewProjection expectedOverview = filePageOne.getContent().getFirst();
            assertThat(firstOverview).isNotNull();
            assertThat(firstOverview.getOwner()).isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(firstOverview.getOriginalFilename()).isEqualTo(expectedOverview.getOriginalFileName());
                softly.assertThat(firstOverview.getUserFilename()).isEqualTo(expectedOverview.getUserFileName());
                softly.assertThat(firstOverview.getFileContentType()).isEqualTo(expectedOverview.getContentType());
                softly.assertThat(firstOverview.getUploadDateTime()).isEqualTo(expectedOverview.getUploadDateTime());
                softly.assertThat(firstOverview.getOwner().getId()).isEqualTo(expectedOverview.getOwnerId());
                softly.assertThat(firstOverview.getOwner().getFirstName()).isEqualTo(expectedOverview.getOwnerFirstName());
                softly.assertThat(firstOverview.getOwner().getLastName()).isEqualTo(expectedOverview.getOwnerLastName());
            });
        }

        @Test
        @DisplayName("When getting file overview page should correctly map last page to FileOverviewPageDto")
        public void whenGettingFileOverviewPageShouldCorrectlyMapLastPageToFileOverviewPageDto() {
            setupSuccessfulGetPageMocks(filePageTwo);

            FileOverviewPageDto output = fileService.getFileOverviewPageByEventId(EventConstants.FIRST_EVENT_ID, PAGE_NUMBER_ONE);

            assertThat(output).as("Expected output before field assertions").isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(output.fileOverviews()).hasSize(SECOND_PAGE_SIZE);
                softly.assertThat(output.pageNumber()).isEqualTo(PAGE_NUMBER_ONE);
                softly.assertThat(output.totalPages()).isEqualTo(PAGE_COUNT_TWO);
                softly.assertThat(output.totalElements()).isEqualTo(FILE_COUNT_MAX);
                softly.assertThat(output.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(output.lastPage()).isTrue();
            });
            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
            assertThat(output.fileOverviews()).extracting(FileOverviewDto::getId)
                    .containsExactlyElementsOf(filePageTwo.getContent().stream().map(FileOverviewProjection::getId).toList());
        }

        @Test
        @DisplayName("When getting file overview page should correctly map first and last page to FileOverviewPageDto")
        public void whenGettingFileOverviewPageShouldCorrectlyMapFirstAndLastPageToFileOverviewPageDto() {
            final int PAGE_COUNT_ONE = 1;
            final int FILE_COUNT = 19;

            List<FileOverviewProjection> testFiles = prepareFiles(FILE_COUNT);
            Pageable firstLastPageRequest = PageRequest.of(PAGE_NUMBER_ZERO, PaginationConstants.DEFAULT_PAGE_SIZE);
            Page<FileOverviewProjection> firstLastFileOverviewPage = new PageImpl<>(testFiles, firstLastPageRequest, FILE_COUNT);

            setupSuccessfulGetPageMocks(firstLastFileOverviewPage);

            FileOverviewPageDto output = fileService.getFileOverviewPageByEventId(EventConstants.FIRST_EVENT_ID, PAGE_NUMBER_ZERO);

            assertThat(output).as("Expected output before field assertions").isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(output.fileOverviews()).hasSize(FILE_COUNT);
                softly.assertThat(output.pageNumber()).isEqualTo(PAGE_NUMBER_ZERO);
                softly.assertThat(output.totalPages()).isEqualTo(PAGE_COUNT_ONE);
                softly.assertThat(output.totalElements()).isEqualTo(FILE_COUNT);
                softly.assertThat(output.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(output.lastPage()).isTrue();
            });
            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
            assertThat(output.fileOverviews()).extracting(FileOverviewDto::getId)
                    .containsExactlyElementsOf(firstLastFileOverviewPage.getContent().stream().map(FileOverviewProjection::getId).toList());
        }

        @Test
        @DisplayName("When getting file overview page should correctly map empty page to FileOverviewPageDto")
        public void whenGettingFileOverviewPageShouldCorrectlyMapEmptyPageToFileOverviewPageDto() {
            final int PAGE_COUNT_ZERO = 0;
            final int FILE_COUNT_ZERO = 0;

            Pageable emptyPageRequest = PageRequest.of(PAGE_NUMBER_ZERO, PaginationConstants.DEFAULT_PAGE_SIZE);
            Page<FileOverviewProjection> emptyPage = new PageImpl<>(new ArrayList<>(), emptyPageRequest, FILE_COUNT_ZERO);

            setupSuccessfulGetPageMocks(emptyPage);

            FileOverviewPageDto output = fileService.getFileOverviewPageByEventId(EventConstants.FIRST_EVENT_ID, PAGE_NUMBER_ZERO);

            assertThat(output).as("Expected output before field assertions").isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(output.fileOverviews()).isEmpty();
                softly.assertThat(output.pageNumber()).isEqualTo(PAGE_NUMBER_ZERO);
                softly.assertThat(output.totalPages()).isEqualTo(PAGE_COUNT_ZERO);
                softly.assertThat(output.totalElements()).isEqualTo(FILE_COUNT_ZERO);
                softly.assertThat(output.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(output.lastPage()).isTrue();
            });
            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verifyNoInteractions(notificationCommandService);
        }

        private List<FileOverviewProjection> prepareFiles(int fileCount) {
            Instant fileUploadDateTime = TimeConstants.TWO_DAYS_FROM_NOW;

            List<File> testFiles = IntStream.range(0, fileCount).mapToObj(i ->
                    new FileTestBuilder()
                            .id(UUID.randomUUID())
                            .userFileName("number_" + i)
                            .originalFileName("file_" + i + ".png")
                            .contentType(FileConstants.PNG_FILE_CONTENT_TYPE)
                            .content(TestFileContentFactory.png())
                            .uploadDateTime(fileUploadDateTime.plus(i, ChronoUnit.HOURS))
                            .owner(firstUser)
                            .event(event)
                            .build()
            ).toList();

            event.getFiles().addAll(testFiles);

            return testFiles.stream().map(FileServiceUnitTest.this::overviewProjection).toList();
        }
    }

    private FileOverviewProjection overviewProjection(File file) {
        return new FileOverviewProjectionTestBuilder()
                .id(file.getId()).userFileName(file.getUserFileName())
                .originalFileName(file.getOriginalFileName()).contentType(file.getContentType())
                .uploadDateTime(file.getUploadDateTime()).ownerId(file.getOwner().getId())
                .ownerFirstName(file.getOwner().getFirstName()).ownerLastName(file.getOwner().getLastName())
                .build();
    }

    private FileContentProjection contentProjection(File file) {
        return new FileContentProjectionTestBuilder()
                .contentType(file.getContentType()).content(file.getContent()).build();
    }
}
