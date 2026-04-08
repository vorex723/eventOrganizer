package com.mazurek.eventOrganizer.thread;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.event.EventService;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotEventAttenderException;
import com.mazurek.eventOrganizer.exception.thread.NotThreadOwnerException;
import com.mazurek.eventOrganizer.exception.thread.ThreadNotFoundException;
import com.mazurek.eventOrganizer.exception.thread.ThreadNotFoundInEventException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestDataInitializer;
import com.mazurek.eventOrganizer.testData.builders.dto.ThreadCreateDtoTestBuilder;
import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadDto;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Profile("test")
@DisplayName("ThreadService integration tests:")
public class ThreadServiceIntegrationTest {

    private UUID savedEventId;
    private UUID savedThreadId;

    @Autowired
    private EventService eventService;
    @Autowired
    private ThreadService threadService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ThreadRepository threadRepository;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private TestDataInitializer testDataInitializer;
    @Autowired
    private DeletionService deletionService;

    @BeforeEach
    void setUp() {
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
    @DisplayName("Create thread tests:")
    class EventThreadCreateTests {

        private ThreadCreateDto threadCreateDto;

        @BeforeEach
        void setUp() {
            threadCreateDto = ThreadCreateDtoTestBuilder.firstThread().build();
        }

        @Test
        @DisplayName("When creating thread in event should throw EventNotFoundException if event with given id does not exist")
        public void whenCreatingThreadInEventShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
            authHelper.setupSecurityContextForFirstUser();

            assertThatThrownBy(() -> threadService.createThreadInEvent(threadCreateDto, EventConstants.NOT_EXISTING_EVENT_ID))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("When creating thread in event should throw NotEventAttenderException if user is not attending event")
        public void whenCreatingThreadInEventShouldThrowNotEventAttenderExceptionIfUserIsNotAttendingEvent() {
            authHelper.setupSecurityContextForSecondUser();

            assertThatThrownBy(() -> threadService.createThreadInEvent(threadCreateDto, savedEventId))
                    .isInstanceOf(NotEventAttenderException.class);
        }

        @Test
        @DisplayName("When creating thread in event should save it with correct data")
        public void whenCreatingThreadInEventShouldSaveItWithCorrectData() {
            authHelper.setupSecurityContextForFirstUser();
            savedThreadId = threadService.createThreadInEvent(threadCreateDto, savedEventId).getId();

            Thread savedThread = threadRepository.findById(savedThreadId).orElseThrow(ThreadNotFoundException::new);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(savedThread.getName())
                        .as("Saved thread name should match thread create dto")
                        .isEqualTo(threadCreateDto.getName());
                softly.assertThat(savedThread.getContent())
                        .as("Saved thread content should match thread create dto")
                        .isEqualTo(threadCreateDto.getContent());
                softly.assertThat(savedThread.getCreateDate())
                        .as("Create date and last update should be equal on creation")
                        .isEqualTo(savedThread.getLastUpdate());
                softly.assertThat(savedThread.getEditCounter())
                        .as("Edit counter should be zero on creation")
                        .isZero();
            });
        }

        @Test
        @DisplayName("When creating thread in event should save all relationships in database")
        public void whenCreatingThreadInEventShouldSaveAllRelationshipsInDatabase() {
            authHelper.setupSecurityContextForFirstUser();

            savedThreadId = threadService.createThreadInEvent(threadCreateDto, savedEventId).getId();

            User performingUser = userRepository.findByEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);
            Set<Thread> performingUserThreads = threadRepository.findByOwnerId(performingUser.getId());
            Set<Thread> eventThreads = threadRepository.findByEventId(savedEventId);
            Thread savedThread = threadRepository.findById(savedThreadId).orElseThrow(ThreadNotFoundException::new);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(performingUserThreads)
                        .as("Performing user should have the saved thread in their threads")
                        .contains(savedThread);
                softly.assertThat(eventThreads)
                        .as("Event threads should contain the new thread")
                        .contains(savedThread);
                softly.assertThat(savedThread.getOwner().getId())
                        .as("Thread owner should be set to the performing user")
                        .isEqualTo(performingUser.getId());
                softly.assertThat(savedThread.getEvent().getId())
                        .as("Thread event should be set to the event with given id")
                        .isEqualTo(savedEventId);
            });
        }

    }

    @Nested
    @DisplayName("Update thread tests:")
    class EventThreadUpdateTests {

        private ThreadCreateDto threadUpdateDto;

        @BeforeEach
        void setUp() {
            threadUpdateDto = ThreadCreateDtoTestBuilder.firstThreadUpdate().build();
            savedThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);
        }

        @Test
        @DisplayName("When updating thread in event should throw EventNotFoundException if event with given id does not exist")
        public void whenUpdatingThreadInEventShouldThrowEventNotFoundIfEventWithGivenIdDoesNotExist() {
            authHelper.setupSecurityContextForFirstUser();

            assertThatThrownBy(() -> threadService.updateThreadInEvent(threadUpdateDto, EventConstants.NOT_EXISTING_EVENT_ID, savedThreadId))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("When updating thread in event should throw ThreadNotFoundInEventException if thread does not exist")
        public void whenUpdatingThreadInEventShouldThrowThreadNotFoundInEventExceptionIfThreadDoesNotExist() {
            authHelper.setupSecurityContextForFirstUser();

            assertThatThrownBy(() -> threadService.updateThreadInEvent(threadUpdateDto, savedEventId, EventConstants.NOT_EXISTING_EVENT_ID))
                    .isInstanceOf(ThreadNotFoundInEventException.class);
        }

        @Test
        @DisplayName("When updating thread in event should throw ThreadNotFoundInEventException if thread exists but is not related with event with given id")
        public void whenUpdatingThreadInEventShouldThrowThreadNotFoundInEventExceptionIfThreadExistButIsNotRelatedWithEventWithGivenId() {
            UUID secondEventId = testDataInitializer.setupEventByFirstUser();
            authHelper.setupSecurityContextForFirstUser();

            assertThatThrownBy(() -> threadService.updateThreadInEvent(threadUpdateDto, secondEventId, savedThreadId))
                    .isInstanceOf(ThreadNotFoundInEventException.class);
        }

        @Test
        @DisplayName("When updating thread in event should throw NotEventAttenderException if user is not attending event anymore")
        public void whenUpdatingThreadInEventShouldThrowNotEventAttenderExceptionIfUserIsNotAttendingEventAnymore() {
            authHelper.setupSecurityContextForSecondUser();
            eventService.addAttenderToEvent(savedEventId);
            eventService.removeAttenderFromEvent(savedEventId);

            assertThatThrownBy(() -> threadService.updateThreadInEvent(threadUpdateDto, savedEventId, savedThreadId))
                    .isInstanceOf(NotEventAttenderException.class);
        }

        @Test
        @DisplayName("When updating thread in event should throw NotThreadOwnerException if performing user does not own thread with given id")
        public void whenUpdatingThreadInEventShouldThrowNotThreadOwnerExceptionIfPerformingUserDoesNotOwnThreadWithGivenId() {
            authHelper.setupSecurityContextForSecondUser();
            eventService.addAttenderToEvent(savedEventId);

            assertThatThrownBy(() -> threadService.updateThreadInEvent(threadUpdateDto, savedEventId, savedThreadId))
                    .isInstanceOf(NotThreadOwnerException.class);
        }

        @Test
        @DisplayName("When updating thread in event should persist all updated fields in database")
        public void whenUpdatingThreadInEventShouldPersistAllUpdatedFieldsInDatabase() {
            Thread beforeUpdate = threadRepository.findById(savedThreadId).orElseThrow(ThreadNotFoundException::new);
            Instant beforeUpdateLastUpdate = beforeUpdate.getLastUpdate();
            Instant beforeUpdateCreateDate = beforeUpdate.getCreateDate();

            authHelper.setupSecurityContextForFirstUser();
            threadService.updateThreadInEvent(threadUpdateDto, savedEventId, savedThreadId);

            Thread updatedThread = threadRepository.findById(savedThreadId).orElseThrow(ThreadNotFoundException::new);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(updatedThread.getName())
                        .as("Name should be updated to value from update dto")
                        .isEqualTo(threadUpdateDto.getName());
                softly.assertThat(updatedThread.getContent())
                        .as("Content should be updated to value from update dto")
                        .isEqualTo(threadUpdateDto.getContent());
                softly.assertThat(updatedThread.getEditCounter())
                        .as("Edit counter should be incremented by exactly 1")
                        .isEqualTo(1);
                softly.assertThat(updatedThread.getCreateDate())
                        .as("Create date should not change on update")
                        .isEqualTo(beforeUpdateCreateDate);
                softly.assertThat(updatedThread.getLastUpdate())
                        .as("LastUpdate should be after the value before update")
                        .isAfter(beforeUpdateLastUpdate);
            });
        }


    }
}
