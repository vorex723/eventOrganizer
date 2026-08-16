package com.mazurek.eventOrganizer.file;

import com.mazurek.eventOrganizer.testData.TestFileContentFactory;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.config.properties.PaginationProperties;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.event.EventRepository;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotEventAttenderException;
import com.mazurek.eventOrganizer.exception.file.EmptyUploadedFileException;
import com.mazurek.eventOrganizer.exception.file.FileNotFoundInEventException;
import com.mazurek.eventOrganizer.exception.file.FileTypeNotAllowedException;
import com.mazurek.eventOrganizer.notification.service.NotificationCommandService;
import com.mazurek.eventOrganizer.testData.builders.CityTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.EventTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.FileTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.FileUploadDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.MultipartFileTestBuilder;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import com.mazurek.eventOrganizer.utils.FileUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Profile;
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
@Profile("test")
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
    private UserRepository userRepository;
    @Mock
    private FileUtils fileUtils;
    @Mock
    private PaginationProperties paginationProperties;
    @Mock
    private Clock clock;

    @InjectMocks
    private FileService fileService;

    private City cityWarsaw;
    private User firstUser;
    private User secondUser;
    private Event event;
    private Optional<Event> eventOptional;

    @BeforeEach
    void setUp() {
        lenient().when(clock.instant()).thenReturn(TimeConstants.NOW);
        lenient().when(paginationProperties.getDefaultPageSize()).thenReturn(PaginationConstants.DEFAULT_PAGE_SIZE);
        cityWarsaw = CityTestBuilder.warsaw().build();

        firstUser = UserTestBuilder.firstUser().homeCity(cityWarsaw).build();
        secondUser = UserTestBuilder.secondUser().homeCity(cityWarsaw).build();

        event = EventTestBuilder
                .firstEvent()
                .owner(firstUser)
                .city(cityWarsaw)
                .build();

        eventOptional = Optional.of(event);

        event.addAttendingUser(secondUser);
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
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(uploadingUser);
            when(fileUtils.detectValidatedContentType(jpgMultipartFile)).thenReturn(Optional.of(FileConstants.JPG_FILE_CONTENT_TYPE));
            when(fileRepository.save(any(File.class))).thenReturn(saveFileReturn);
        }

        @Test
        @DisplayName("When uploading file should load event with given id from database")
        public void whenUploadingFileShouldLoadEventWithGivenIdFromDatabase() throws IOException {
            setupSuccessfulFileUploadMocks();

            fileService.uploadFileToEvent(fileUploadDto, EventConstants.FIRST_EVENT_ID);

            verify(eventRepository, times(1)).findById(EventConstants.FIRST_EVENT_ID);
        }

        @Test
        @DisplayName("When uploading file should throw EventNotFoundException if event with given id does not exist")
        public void whenUploadingFileShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fileService.uploadFileToEvent(fileUploadDto, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(EventNotFoundException.class);

            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("When uploading file should retrieve performing user from AuthenticationService")
        public void whenUploadingFileShouldRetrievePerformingUserFromAuthenticationService() throws IOException {
            setupSuccessfulFileUploadMocks();

            fileService.uploadFileToEvent(fileUploadDto, EventConstants.FIRST_EVENT_ID);

            verify(authenticationService, times(1)).getCurrentUser();
        }

        @Test
        @DisplayName("When uploading file should throw NotEventAttenderException if performing user does not attend event with given id")
        public void whenUploadingFileShouldThrowNotEventAttenderExceptionIfPerformingUserDoesNotAttendEventWithGivenId() {
            event.removeAttendingUser(secondUser);
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(secondUser);

            assertThatThrownBy(() -> fileService.uploadFileToEvent(fileUploadDto, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(NotEventAttenderException.class);

            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("When uploading file should throw EmptyUploadedFileException if file is empty")
        public void whenUploadingFileShouldThrowEmptyUploadedFileExceptionIfFileIsEmpty() {
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);

            MockMultipartFile emptyFile = MultipartFileTestBuilder
                    .jpgFile()
                    .content(new byte[0])
                    .buildMultipartFile();

            fileUploadDto.setFile(emptyFile);

            assertThatThrownBy(() -> fileService.uploadFileToEvent(fileUploadDto, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(EmptyUploadedFileException.class);

            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("When uploading file should throw FileTypeNotAllowedException if detected file type is not on whitelist")
        public void whenUploadingFileShouldThrowFileTypeNotAllowedExceptionIfDetectedFileTypeIsNotOnWhitelist() throws IOException {
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(fileUtils.detectValidatedContentType(jpgMultipartFile)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fileService.uploadFileToEvent(fileUploadDto, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(FileTypeNotAllowedException.class);

            verify(fileRepository, never()).save(any(File.class));
            verify(eventRepository, never()).save(any(Event.class));
            verify(userRepository, never()).save(any(User.class));
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
                softly.assertThat(firstUser.getFiles())
                        .as("File should be added to performing user's files")
                        .contains(capturedFile);
                softly.assertThat(event.getFiles())
                        .as("File should be added to event's files")
                        .contains(capturedFile);
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

            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(fileUtils.detectValidatedContentType(mismatchedMimeJpgFile)).thenReturn(Optional.of(FileConstants.JPG_FILE_CONTENT_TYPE));
            when(fileRepository.save(any(File.class))).thenReturn(saveFileReturn);

            fileService.uploadFileToEvent(fileUploadDto, EventConstants.FIRST_EVENT_ID);

            ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
            verify(fileRepository).save(fileCaptor.capture());
            assertThat(fileCaptor.getValue().getContentType()).isEqualTo(FileConstants.JPG_FILE_CONTENT_TYPE);
        }

        @Test
        @DisplayName("When uploading file should save updated event and user")
        public void whenUploadingFileShouldSaveUpdatedEventAndUser() throws IOException {
            setupSuccessfulFileUploadMocks();

            fileService.uploadFileToEvent(fileUploadDto, EventConstants.FIRST_EVENT_ID);

            verify(eventRepository, times(1)).save(event);
            verify(userRepository, times(1)).save(firstUser);
        }

        @Test
        @DisplayName("When event owner uploads file should notify event attenders")
        void whenEventOwnerUploadsFileShouldNotifyEventAttenders() throws IOException {
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
        @DisplayName("When event attender uploads file should notify event owner and exclude uploader")
        void whenEventAttenderUploadsFileShouldNotifyEventOwnerAndExcludeUploader() throws IOException {
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
        private Optional<File> fileToServeOptional;

        @BeforeEach
        void setUp() {
            fileToServe = FileTestBuilder.pdfFile().event(event).owner(firstUser).build();
            fileToServeOptional = Optional.of(fileToServe);
        }

        private void setupSuccessfulGetFileOverviewMocks() {
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(fileRepository.findByIdAndEventId(FileConstants.FIRST_FILE_ID, EventConstants.FIRST_EVENT_ID)).thenReturn(fileToServeOptional);
        }

        @Test
        @DisplayName("When getting file overview by id should retrieve performing user from AuthenticationService")
        public void whenGettingFileOverviewByIdShouldRetrievePerformingUserFromAuthenticationService() {
            setupSuccessfulGetFileOverviewMocks();

            fileService.getFileOverviewById(FileConstants.FIRST_FILE_ID, EventConstants.FIRST_EVENT_ID);

            verify(authenticationService, times(1)).getCurrentUser();
        }

        @Test
        @DisplayName("When getting file overview by id should load event with given id from database")
        public void whenGettingFileOverviewByIdShouldLoadEventWithGivenIdFromDatabase() {
            setupSuccessfulGetFileOverviewMocks();

            fileService.getFileOverviewById(FileConstants.FIRST_FILE_ID, EventConstants.FIRST_EVENT_ID);

            verify(eventRepository, times(1)).findById(EventConstants.FIRST_EVENT_ID);
        }

        @Test
        @DisplayName("When getting file overview by id should throw EventNotFoundException if there is no event with given id")
        public void whenGettingFileOverviewByIdShouldThrowEventNotFoundExceptionIfThereIsNoEventWithGivenId() {
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fileService.getFileOverviewById(FileConstants.FIRST_FILE_ID, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("When getting file overview by id should throw NotEventAttenderException if performing user is not attending event")
        public void whenGettingFileOverviewByIdShouldThrowNotEventAttenderExceptionIfPerformingUserIsNotAttendingEvent() {
            event.removeAttendingUser(secondUser);
            when(authenticationService.getCurrentUser()).thenReturn(secondUser);
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);

            assertThatThrownBy(() -> fileService.getFileOverviewById(FileConstants.FIRST_FILE_ID, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(NotEventAttenderException.class);
        }

        @Test
        @DisplayName("When getting file overview by id should load file by file id and event id")
        public void whenGettingFileOverviewByIdShouldLoadFileByFileIdAndEventId() {
            setupSuccessfulGetFileOverviewMocks();

            fileService.getFileOverviewById(FileConstants.FIRST_FILE_ID, EventConstants.FIRST_EVENT_ID);

            verify(fileRepository, times(1)).findByIdAndEventId(FileConstants.FIRST_FILE_ID, EventConstants.FIRST_EVENT_ID);
        }

        @Test
        @DisplayName("When getting file overview by id should throw FileNotFoundInEventException if there is no file with given id in event with given id")
        public void whenGettingFileOverviewByIdShouldThrowFileNotFoundInEventExceptionIfThereIsNoFileWithGivenIdInEventWithGivenId() {
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(fileRepository.findByIdAndEventId(FileConstants.FIRST_FILE_ID, EventConstants.FIRST_EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fileService.getFileOverviewById(FileConstants.FIRST_FILE_ID, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(FileNotFoundInEventException.class);
        }

        @Test
        @DisplayName("When getting file overview by id should return file overview dto with correct data")
        public void whenGettingFileOverviewByIdShouldReturnDtoWithCorrectData() {
            setupSuccessfulGetFileOverviewMocks();

            FileOverviewDto returnedFileOverview = fileService.getFileOverviewById(FileConstants.FIRST_FILE_ID, EventConstants.FIRST_EVENT_ID);

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
        }
    }

    @Nested
    @DisplayName("Get file data by id tests:")
    class GetFileDataByIdTests {

        private File fileToServe;
        private Optional<File> fileToServeOptional;

        @BeforeEach
        void setUp() {
            fileToServe = FileTestBuilder.pdfFile().owner(firstUser).event(event).build();
            fileToServeOptional = Optional.of(fileToServe);
        }

        private void setupSuccessfulGetFileDataMocks() {
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(fileRepository.findByIdAndEventId(FileConstants.FIRST_FILE_ID, EventConstants.FIRST_EVENT_ID)).thenReturn(fileToServeOptional);
        }

        @Test
        @DisplayName("When getting file data by id should retrieve performing user from AuthenticationService")
        public void whenGettingFileDataByIdShouldRetrievePerformingUserFromAuthenticationService() {
            setupSuccessfulGetFileDataMocks();

            fileService.getFileDataById(FileConstants.FIRST_FILE_ID, EventConstants.FIRST_EVENT_ID);

            verify(authenticationService, times(1)).getCurrentUser();
        }

        @Test
        @DisplayName("When getting file data by id should load event with given id from database")
        public void whenGettingFileDataByIdShouldLoadEventWithGivenIdFromDatabase() {
            setupSuccessfulGetFileDataMocks();

            fileService.getFileDataById(FileConstants.FIRST_FILE_ID, EventConstants.FIRST_EVENT_ID);

            verify(eventRepository, times(1)).findById(EventConstants.FIRST_EVENT_ID);
        }

        @Test
        @DisplayName("When getting file data by id should throw EventNotFoundException if there is no event with given id")
        public void whenGettingFileDataByIdShouldThrowEventNotFoundExceptionIfThereIsNoEventWithGivenId() {
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fileService.getFileDataById(FileConstants.FIRST_FILE_ID, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("When getting file data by id should throw NotEventAttenderException if performing user is not attending event")
        public void whenGettingFileDataByIdShouldThrowNotEventAttenderExceptionIfPerformingUserIsNotAttendingEvent() {
            event.removeAttendingUser(secondUser);
            when(authenticationService.getCurrentUser()).thenReturn(secondUser);
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);

            assertThatThrownBy(() -> fileService.getFileDataById(FileConstants.FIRST_FILE_ID, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(NotEventAttenderException.class);
        }

        @Test
        @DisplayName("When getting file data by id should load file by file id and event id")
        public void whenGettingFileDataByIdShouldLoadFileByFileIdAndEventId() {
            setupSuccessfulGetFileDataMocks();

            fileService.getFileDataById(FileConstants.FIRST_FILE_ID, EventConstants.FIRST_EVENT_ID);

            verify(fileRepository, times(1)).findByIdAndEventId(FileConstants.FIRST_FILE_ID, EventConstants.FIRST_EVENT_ID);
        }

        @Test
        @DisplayName("When getting file data by id should throw FileNotFoundInEventException if there is no file with given id in event with given id")
        public void whenGettingFileDataByIdShouldThrowFileNotFoundInEventExceptionIfThereIsNoFileWithGivenIdInEventWithGivenId() {
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(fileRepository.findByIdAndEventId(FileConstants.FIRST_FILE_ID, EventConstants.FIRST_EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fileService.getFileDataById(FileConstants.FIRST_FILE_ID, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(FileNotFoundInEventException.class);
        }

        @Test
        @DisplayName("When getting file data by id should return correct file entity with unchanged content")
        public void whenGettingFileDataByIdShouldReturnCorrectFileEntityWithUnchangedContent() {
            setupSuccessfulGetFileDataMocks();

            File returnedFile = fileService.getFileDataById(FileConstants.FIRST_FILE_ID, EventConstants.FIRST_EVENT_ID);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(returnedFile)
                        .as("Returned file should be the same entity as loaded from database")
                        .isEqualTo(fileToServe);
                softly.assertThat(returnedFile.getContent())
                        .as("Returned file content should be unchanged")
                        .isEqualTo(fileToServe.getContent());
            });
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

        Page<File> filePageOne;
        Page<File> filePageTwo;

        @BeforeEach
        void setUp() {
            List<File> testFiles = prepareFiles(FILE_COUNT_MAX);

            Pageable firstPageRequest = PageRequest.of(PAGE_NUMBER_ZERO, PaginationConstants.DEFAULT_PAGE_SIZE);
            Pageable secondPageRequest = PageRequest.of(PAGE_NUMBER_ONE, PaginationConstants.DEFAULT_PAGE_SIZE);
            filePageOne = new PageImpl<>(testFiles.subList(PAGE_NUMBER_ZERO, PaginationConstants.DEFAULT_PAGE_SIZE), firstPageRequest, testFiles.size());
            filePageTwo = new PageImpl<>(testFiles.subList(PaginationConstants.DEFAULT_PAGE_SIZE, FILE_COUNT_MAX), secondPageRequest, testFiles.size());
        }

        private void setupSuccessfulGetPageMocks(Page<File> page) {
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(fileRepository.findByEventId(eq(EventConstants.FIRST_EVENT_ID), any(Pageable.class))).thenReturn(page);
        }

        @Test
        @DisplayName("When getting file overview page should retrieve performing user from AuthenticationService")
        public void whenGettingFileOverviewPageShouldRetrievePerformingUserFromAuthenticationService() {
            setupSuccessfulGetPageMocks(filePageOne);

            fileService.getFileOverviewPageByEventId(EventConstants.FIRST_EVENT_ID, PAGE_NUMBER_ZERO);

            verify(authenticationService, times(1)).getCurrentUser();
        }

        @Test
        @DisplayName("When getting file overview page should throw InvalidPageNumberException if page number is below zero")
        public void whenGettingFileOverviewPageShouldThrowInvalidPageNumberExceptionIfPageNumberIsBelowZero() {
            assertThatThrownBy(() -> fileService.getFileOverviewPageByEventId(EventConstants.FIRST_EVENT_ID, PaginationConstants.PAGE_MINUS_ONE))
                    .isInstanceOf(InvalidPageNumberException.class);
        }

        @Test
        @DisplayName("When getting file overview page should load event with given id from database")
        public void whenGettingFileOverviewPageShouldLoadEventWithGivenIdFromDatabase() {
            setupSuccessfulGetPageMocks(filePageOne);

            fileService.getFileOverviewPageByEventId(EventConstants.FIRST_EVENT_ID, PAGE_NUMBER_ZERO);

            verify(eventRepository, times(1)).findById(EventConstants.FIRST_EVENT_ID);
        }

        @Test
        @DisplayName("When getting file overview page should throw EventNotFoundException if event with given id does not exist")
        public void whenGettingFileOverviewPageShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fileService.getFileOverviewPageByEventId(EventConstants.FIRST_EVENT_ID, PAGE_NUMBER_ZERO))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("When getting file overview page should throw NotEventAttenderException if user is not attending event")
        public void whenGettingFileOverviewPageShouldThrowNotEventAttenderExceptionIfUserIsNotAttendingEvent() {
            event.removeAttendingUser(secondUser);
            when(authenticationService.getCurrentUser()).thenReturn(secondUser);
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);

            assertThatThrownBy(() -> fileService.getFileOverviewPageByEventId(EventConstants.FIRST_EVENT_ID, PAGE_NUMBER_ZERO))
                    .isInstanceOf(NotEventAttenderException.class);
        }

        @Test
        @DisplayName("When getting file overview page should load page with correct page number, size and sort order")
        public void whenGettingFileOverviewPageShouldLoadPageWithCorrectPageNumberSizeAndSortOrder() {
            setupSuccessfulGetPageMocks(filePageOne);
            ArgumentCaptor<Pageable> pageRequestCaptor = ArgumentCaptor.forClass(Pageable.class);

            fileService.getFileOverviewPageByEventId(EventConstants.FIRST_EVENT_ID, PAGE_NUMBER_ZERO);

            verify(fileRepository, times(1)).findByEventId(eq(EventConstants.FIRST_EVENT_ID), pageRequestCaptor.capture());
            Pageable capturedPageRequest = pageRequestCaptor.getValue();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(capturedPageRequest.getPageNumber())
                        .as("Page number should match requested page number")
                        .isEqualTo(PAGE_NUMBER_ZERO);
                softly.assertThat(capturedPageRequest.getPageSize())
                        .as("Page size should be the default page size")
                        .isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(capturedPageRequest.getSort())
                        .as("Results should be sorted by uploadDateTime ascending")
                        .isEqualTo(Sort.by("uploadDateTime").ascending());
            });
        }

        @Test
        @DisplayName("When getting file overview page should correctly map full page to FileOverviewPageDto")
        public void whenGettingFileOverviewPageShouldCorrectlyMapFullPageToFileOverviewPageDto() {
            setupSuccessfulGetPageMocks(filePageOne);

            FileOverviewPageDto output = fileService.getFileOverviewPageByEventId(EventConstants.FIRST_EVENT_ID, PAGE_NUMBER_ZERO);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(output.pageNumber()).isEqualTo(PAGE_NUMBER_ZERO);
                softly.assertThat(output.totalPages()).isEqualTo(PAGE_COUNT_TWO);
                softly.assertThat(output.totalElements()).isEqualTo(FILE_COUNT_MAX);
                softly.assertThat(output.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(output.fileOverviews()).hasSize(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(output.lastPage()).isFalse();
            });
        }

        @Test
        @DisplayName("When getting file overview page should correctly map last page to FileOverviewPageDto")
        public void whenGettingFileOverviewPageShouldCorrectlyMapLastPageToFileOverviewPageDto() {
            setupSuccessfulGetPageMocks(filePageTwo);

            FileOverviewPageDto output = fileService.getFileOverviewPageByEventId(EventConstants.FIRST_EVENT_ID, PAGE_NUMBER_ONE);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(output.fileOverviews()).hasSize(SECOND_PAGE_SIZE);
                softly.assertThat(output.pageNumber()).isEqualTo(PAGE_NUMBER_ONE);
                softly.assertThat(output.totalPages()).isEqualTo(PAGE_COUNT_TWO);
                softly.assertThat(output.totalElements()).isEqualTo(FILE_COUNT_MAX);
                softly.assertThat(output.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(output.lastPage()).isTrue();
            });
        }

        @Test
        @DisplayName("When getting file overview page should correctly map first and last page to FileOverviewPageDto")
        public void whenGettingFileOverviewPageShouldCorrectlyMapFirstAndLastPageToFileOverviewPageDto() {
            final int PAGE_COUNT_ONE = 1;
            final int FILE_COUNT = 19;

            List<File> testFiles = prepareFiles(FILE_COUNT);
            Pageable firstLastPageRequest = PageRequest.of(PAGE_NUMBER_ZERO, PaginationConstants.DEFAULT_PAGE_SIZE);
            Page<File> firstLastFileOverviewPage = new PageImpl<>(testFiles, firstLastPageRequest, FILE_COUNT);

            setupSuccessfulGetPageMocks(firstLastFileOverviewPage);

            FileOverviewPageDto output = fileService.getFileOverviewPageByEventId(EventConstants.FIRST_EVENT_ID, PAGE_NUMBER_ZERO);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(output.fileOverviews()).hasSize(FILE_COUNT);
                softly.assertThat(output.pageNumber()).isEqualTo(PAGE_NUMBER_ZERO);
                softly.assertThat(output.totalPages()).isEqualTo(PAGE_COUNT_ONE);
                softly.assertThat(output.totalElements()).isEqualTo(FILE_COUNT);
                softly.assertThat(output.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(output.lastPage()).isTrue();
            });
        }

        @Test
        @DisplayName("When getting file overview page should correctly map empty page to FileOverviewPageDto")
        public void whenGettingFileOverviewPageShouldCorrectlyMapEmptyPageToFileOverviewPageDto() {
            final int PAGE_COUNT_ZERO = 0;
            final int FILE_COUNT_ZERO = 0;

            Pageable emptyPageRequest = PageRequest.of(PAGE_NUMBER_ZERO, PaginationConstants.DEFAULT_PAGE_SIZE);
            Page<File> emptyPage = new PageImpl<>(new ArrayList<>(), emptyPageRequest, FILE_COUNT_ZERO);

            setupSuccessfulGetPageMocks(emptyPage);

            FileOverviewPageDto output = fileService.getFileOverviewPageByEventId(EventConstants.FIRST_EVENT_ID, PAGE_NUMBER_ZERO);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(output.fileOverviews()).isEmpty();
                softly.assertThat(output.pageNumber()).isEqualTo(PAGE_NUMBER_ZERO);
                softly.assertThat(output.totalPages()).isEqualTo(PAGE_COUNT_ZERO);
                softly.assertThat(output.totalElements()).isEqualTo(FILE_COUNT_ZERO);
                softly.assertThat(output.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(output.lastPage()).isTrue();
            });
        }

        private List<File> prepareFiles(int fileCount) {
            Instant fileUploadDateTime = TimeConstants.TWO_DAYS_FROM_NOW;

            List<File> testFiles = IntStream.range(0, fileCount).mapToObj(i ->
                    File.builder()
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
            firstUser.getFiles().addAll(testFiles);

            return testFiles;
        }
    }
}
