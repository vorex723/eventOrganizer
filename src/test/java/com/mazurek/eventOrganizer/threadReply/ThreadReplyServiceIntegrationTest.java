package com.mazurek.eventOrganizer.threadReply;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.event.EventService;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotEventAttenderException;
import com.mazurek.eventOrganizer.exception.thread.*;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestDataInitializer;
import com.mazurek.eventOrganizer.testData.builders.dto.ThreadReplyCreateDtoTestBuilder;
import com.mazurek.eventOrganizer.thread.*;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.thread.dto.ThreadReplyCreateDto;
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
@DisplayName("ThreadReplyService integration tests:")
public class ThreadReplyServiceIntegrationTest {

    @Autowired
    private EventService eventService;
    @Autowired
    private ThreadReplyService threadReplyService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ThreadRepository threadRepository;
    @Autowired
    private ThreadReplyRepository threadReplyRepository;
    @Autowired
    private DeletionService deletionService;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private TestDataInitializer testDataInitializer;

    private UUID savedEventId;
    private UUID savedThreadId;

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();
        savedEventId = testDataInitializer.setupFirstEvent();
        savedThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
    }

    @Nested
    @DisplayName("Create reply tests:")
    class CreateReplyTests {

        private ThreadReplyCreateDto threadReplyCreateDto;

        @BeforeEach
        void setUp() {
            threadReplyCreateDto = ThreadReplyCreateDtoTestBuilder.firstReply().build();
        }

        @Test
        @DisplayName("When creating thread reply in event thread should throw EventNotFoundException if event with given id does not exist")
        public void whenCreatingThreadReplyInEventThreadShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
            authHelper.setupSecurityContextForFirstUser();

            assertThatThrownBy(() -> threadReplyService.createReplyInThread(threadReplyCreateDto, EventConstants.NOT_EXISTING_EVENT_ID, savedThreadId))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("When creating thread reply in event thread should throw NotEventAttenderException if user is not attending event with given id")
        public void whenCreatingThreadReplyInEventThreadShouldThrowNotEventAttenderExceptionIfUserIsNotAttendingEventWithGivenId() {
            authHelper.setupSecurityContextForSecondUser();

            assertThatThrownBy(() -> threadReplyService.createReplyInThread(threadReplyCreateDto, savedEventId, savedThreadId))
                    .isInstanceOf(NotEventAttenderException.class);
        }

        @Test
        @DisplayName("When creating thread reply in event thread should throw ThreadNotFoundInEventException if thread with given id does not exist")
        public void whenCreatingThreadReplyInEventThreadShouldThrowThreadNotFoundInEventExceptionIfThreadWithGivenIdDoesNotExist() {
            authHelper.setupSecurityContextForFirstUser();

            assertThatThrownBy(() -> threadReplyService.createReplyInThread(threadReplyCreateDto, savedEventId, ThreadConstants.FIRST_THREAD_ID))
                    .isInstanceOf(ThreadNotFoundInEventException.class);
        }

        @Test
        @DisplayName("When creating thread reply in event thread should throw ThreadNotFoundInEventException if thread with given id exists but is not related to event with given id")
        public void whenCreatingThreadReplyInEventThreadShouldThrowThreadNotFoundInEventExceptionIfThreadWithGivenIdExistsButIsNotRelatedToEventWithGivenId() {
            UUID secondSavedEventId = testDataInitializer.setupEventByFirstUser();
            UUID secondSavedThreadId = testDataInitializer.setupThreadInEventByFirstUser(secondSavedEventId);

            authHelper.setupSecurityContextForFirstUser();

            assertThatThrownBy(() -> threadReplyService.createReplyInThread(threadReplyCreateDto, savedEventId, secondSavedThreadId))
                    .as("Thread with given id and event with given id are not related, should not allow for creating thread reply")
                    .isInstanceOf(ThreadNotFoundInEventException.class);

            assertThatThrownBy(() -> threadReplyService.createReplyInThread(threadReplyCreateDto, secondSavedEventId, savedThreadId))
                    .as("Thread with given id and event with given id are not related, should not allow for creating thread reply")
                    .isInstanceOf(ThreadNotFoundInEventException.class);
        }

        @Test
        @DisplayName("When creating thread reply in event thread should save it with correct data and relationships in database")
        public void whenCreatingThreadReplyInEventThreadShouldSaveItWithCorrectDataAndRelationshipsInDatabase() {
            authHelper.setupSecurityContextForFirstUser();

            UUID savedThreadReplyId = threadReplyService.createReplyInThread(threadReplyCreateDto, savedEventId, savedThreadId).getId();

            ThreadReply savedThreadReply = threadReplyRepository.findById(savedThreadReplyId).orElseThrow(ThreadReplyNotFoundException::new);
            User threadReplyOwner = userRepository.findByEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);
            Thread thread = threadRepository.findById(savedThreadId).orElseThrow(ThreadNotFoundException::new);

            Set<ThreadReply> threadReplies = threadReplyRepository.findByThreadId(savedThreadId);
            Set<ThreadReply> userThreadReplies = threadReplyRepository.findByReplierId(threadReplyOwner.getId());

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(savedThreadReply.getContent())
                        .as("Content of the thread reply must match the dto")
                        .isEqualTo(threadReplyCreateDto.getReplyContent());
                softly.assertThat(savedThreadReply.getThread())
                        .as("Thread in thread reply must be set to the one with given id")
                        .isEqualTo(thread);
                softly.assertThat(threadReplies)
                        .as("Thread must contain the new thread reply")
                        .contains(savedThreadReply);
                softly.assertThat(savedThreadReply.getReplier())
                        .as("Creator of thread reply must be set to the user making the request")
                        .isEqualTo(threadReplyOwner);
                softly.assertThat(userThreadReplies)
                        .as("User must have the new reply in their replies")
                        .contains(savedThreadReply);
                softly.assertThat(savedThreadReply.getReplyDate())
                        .as("Thread reply creation date must equal lastUpdate on creation")
                        .isEqualTo(savedThreadReply.getLastUpdate());
            });
        }
    }

    @Nested
    @DisplayName("Update reply tests:")
    class UpdateReplyTests {

        private UUID savedThreadReplyId;

        private ThreadReplyCreateDto threadReplyCreateDto;
        private ThreadReplyCreateDto threadReplyUpdateDto;

        @BeforeEach
        void setUp() {
            savedThreadReplyId = testDataInitializer.setupThreadReplyInThreadByFirstUser(savedEventId, savedThreadId);

            threadReplyCreateDto = ThreadReplyCreateDtoTestBuilder.firstReply()
                    .replyContent(ThreadReplyConstants.SECOND_REPLY_CONTENT)
                    .build();
            threadReplyUpdateDto = ThreadReplyCreateDtoTestBuilder.firstReplyUpdate()
                    .replyContent(ThreadReplyConstants.THIRD_REPLY_CONTENT)
                    .build();
        }

        @Test
        @DisplayName("When updating thread reply should throw EventNotFoundException if event with given id does not exist")
        public void whenUpdatingThreadReplyShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
            authHelper.setupSecurityContextForFirstUser();

            assertThatThrownBy(() -> threadReplyService.updateThreadReplyInEventThread(threadReplyUpdateDto, EventConstants.NOT_EXISTING_EVENT_ID, savedThreadId, savedThreadReplyId))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("When updating thread reply should throw NotEventAttenderException if user is not attending event anymore.")
        public void whenUpdatingThreadReplyShouldThrowNotEventAttenderExceptionIfUserIsNotAttendingEventAnymore() {
            authHelper.setupSecurityContextForSecondUser();
            eventService.addAttenderToEvent(savedEventId);
            UUID secondThreadReplyId = threadReplyService.createReplyInThread(threadReplyCreateDto, savedEventId, savedThreadId).getId();
            eventService.removeAttenderFromEvent(savedEventId);

            assertThatThrownBy(() -> threadReplyService.updateThreadReplyInEventThread(threadReplyUpdateDto, savedEventId, savedThreadId, secondThreadReplyId))
                    .as("If user is not attending event anymore should throw NotEventAttenderException")
                    .isInstanceOf(NotEventAttenderException.class);
        }

        @Test
        @DisplayName("When updating thread reply should throw ThreadNotFoundInEventException if thread with given id does not exist")
        public void whenUpdatingThreadReplyShouldThrowThreadNotFoundInEventExceptionIfThreadWithGivenIdDoesNotExist() {
            authHelper.setupSecurityContextForFirstUser();

            assertThatThrownBy(() -> threadReplyService.updateThreadReplyInEventThread(threadReplyUpdateDto, savedEventId, ThreadConstants.NOT_EXISTING_THREAD_ID, savedThreadReplyId))
                    .isInstanceOf(ThreadNotFoundInEventException.class);
        }

        @Test
        @DisplayName("When updating thread reply should throw ThreadNotFoundInEventException if thread with given id is not related with event with given id.")
        public void whenUpdatingThreadReplyShouldThrowThreadNotFoundInEventExceptionIfThreadWithGivenIdIsNotRelatedWithEventWithGivenId() {
            UUID secondSavedEventId = testDataInitializer.setupEventByFirstUser();
            authHelper.setupSecurityContextForFirstUser();

            assertThatThrownBy(() -> threadReplyService.updateThreadReplyInEventThread(threadReplyUpdateDto, secondSavedEventId, savedThreadId, savedThreadReplyId))
                    .as("When updating event thread reply should throw ThreadNotFoundInEventException if event and thread are not related.")
                    .isInstanceOf(ThreadNotFoundInEventException.class);
        }

        @Test
        @DisplayName("When updating thread reply should throw ReplyNotFoundInThreadException if reply with given id does not exist")
        public void whenUpdatingThreadReplyShouldThrowReplyNotFoundInThreadException() {
            authHelper.setupSecurityContextForFirstUser();

            assertThatThrownBy(() -> threadReplyService.updateThreadReplyInEventThread(threadReplyUpdateDto, savedEventId, savedThreadId, ThreadReplyConstants.NOT_EXISTING_REPLY_ID))
                    .isInstanceOf(ReplyNotFoundInThreadException.class);
        }

        @Test
        @DisplayName("When updating thread reply should throw ReplyNotFoundInThreadException if thread reply with given id is not related with thread with given id.")
        public void whenUpdatingThreadReplyShouldThrowReplyNotFoundInThreadExceptionIfThreadReplyIsNotRelatedWithThread() {
            UUID secondSavedThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);
            UUID secondThreadReplyId = testDataInitializer.setupThreadReplyInThreadByFirstUser(savedEventId, secondSavedThreadId);

            authHelper.setupSecurityContextForFirstUser();

            assertThatThrownBy(() -> threadReplyService.updateThreadReplyInEventThread(threadReplyUpdateDto, savedEventId, secondSavedThreadId, savedThreadReplyId))
                    .as("Thread and thread reply are not related, should not allow to update thread reply")
                    .isInstanceOf(ReplyNotFoundInThreadException.class);

            assertThatThrownBy(() -> threadReplyService.updateThreadReplyInEventThread(threadReplyUpdateDto, savedEventId, savedThreadId, secondThreadReplyId))
                    .as("Thread and thread reply are not related, should not allow to update thread reply")
                    .isInstanceOf(ReplyNotFoundInThreadException.class);
        }

        @Test
        @DisplayName("When updating thread reply should throw NotThreadReplyOwnerException if user tries to modify a reply they do not own")
        public void whenUpdatingThreadReplyShouldThrowNotThreadReplyOwnerExceptionIfUserTriesToModifyNotHisReply() {
            authHelper.setupSecurityContextForSecondUser();
            eventService.addAttenderToEvent(savedEventId);

            assertThatThrownBy(() -> threadReplyService.updateThreadReplyInEventThread(threadReplyUpdateDto, savedEventId, savedThreadId, savedThreadReplyId))
                    .as("Expected NotThreadReplyOwnerException when non-owner tries to modify thread reply.")
                    .isInstanceOf(NotThreadReplyOwnerException.class);
        }

        @Test
        @DisplayName("When updating thread reply should persist all updated fields in the database")
        public void whenUpdatingThreadReplyShouldPersistAllUpdatedFieldsInDatabase() {
            authHelper.setupSecurityContextForFirstUser();

            ThreadReply beforeUpdate = threadReplyRepository.findById(savedThreadReplyId).orElseThrow(ThreadReplyNotFoundException::new);
            int oldEditCounter = beforeUpdate.getEditCounter();
            Instant oldLastUpdate = beforeUpdate.getLastUpdate();

            threadReplyService.updateThreadReplyInEventThread(threadReplyUpdateDto, savedEventId, savedThreadId, savedThreadReplyId);

            ThreadReply updatedThreadReply = threadReplyRepository.findById(savedThreadReplyId).orElseThrow(ThreadReplyNotFoundException::new);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(updatedThreadReply.getContent())
                        .as("Content must be updated to the value from update dto")
                        .isEqualTo(threadReplyUpdateDto.getReplyContent());
                softly.assertThat(updatedThreadReply.getEditCounter())
                        .as("Edit counter must be incremented by exactly 1")
                        .isEqualTo(oldEditCounter + 1);
                softly.assertThat(updatedThreadReply.getLastUpdate())
                        .as("LastUpdate must be after the value before update")
                        .isAfter(oldLastUpdate);
            });
        }
    }
}
