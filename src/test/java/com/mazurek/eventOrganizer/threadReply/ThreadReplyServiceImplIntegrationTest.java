package com.mazurek.eventOrganizer.threadReply;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.event.EventService;
import com.mazurek.eventOrganizer.exception.auth.UserNotAuthenticatedException;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotEventAttenderException;
import com.mazurek.eventOrganizer.exception.thread.*;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestDataInitializer;
import com.mazurek.eventOrganizer.testData.builders.dto.ThreadReplyCreateDtoTestBuilder;
import com.mazurek.eventOrganizer.thread.*;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.threadReply.dto.ThreadReplyCreateDto;
import com.mazurek.eventOrganizer.threadReply.dto.ThreadReplyDto;
import com.mazurek.eventOrganizer.threadReply.dto.ThreadReplyPageDto;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Comparator;
import java.util.List;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@SpringBootTest
@Profile("test")
@DisplayName("ThreadReplyService integration tests:")
public class ThreadReplyServiceImplIntegrationTest {

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
        @DisplayName("When creating thread reply in event thread should allow event attender who does not own the thread to create reply")
        public void whenCreatingThreadReplyInEventThreadShouldAllowEventAttenderWhoDoesNotOwnTheThreadToCreateReply() {
            testDataInitializer.addSecondUserToAttenders(savedEventId);
            authHelper.setupSecurityContextForSecondUser();

            UUID savedThreadReplyId = threadReplyService.createReplyInThread(threadReplyCreateDto, savedEventId, savedThreadId).getId();

            ThreadReply savedThreadReply = threadReplyRepository.findById(savedThreadReplyId).orElseThrow(ThreadReplyNotFoundException::new);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(savedThreadReply.getThread().getId()).isEqualTo(savedThreadId);
                softly.assertThat(savedThreadReply.getReplier().getEmail()).isEqualTo(UserConstants.SECOND_USER_EMAIL);
                softly.assertThat(savedThreadReply.getContent()).isEqualTo(threadReplyCreateDto.getReplyContent());
            });
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
        @Test
        @DisplayName("When creating thread reply in event thread should update thread fields")
        public void whenCreatingThreadReplyInEventThreadShouldUpdateThreadFields() {
            authHelper.setupSecurityContextForFirstUser();

            Thread threadBefore = threadRepository.findById(savedThreadId).orElseThrow(ThreadNotFoundException::new);
            threadBefore.setLastActivity(TimeConstants.ONE_HOUR_AGO);
            int replyCountBefore = threadBefore.getReplyCount();
            threadRepository.saveAndFlush(threadBefore);

            threadReplyService.createReplyInThread(threadReplyCreateDto, savedEventId, savedThreadId);

            Thread threadAfter = threadRepository.findById(savedThreadId).orElseThrow(ThreadNotFoundException::new);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(threadAfter.getReplyCount()).isGreaterThan(replyCountBefore);
                softly.assertThat(threadAfter.getReplyCount()).isEqualTo(replyCountBefore+1);
                softly.assertThat(threadAfter.getLastActivity()).isEqualTo(TimeConstants.NOW);

            });

        }

    }

    @Nested
    @DisplayName("Update reply tests:")
    class UpdateReplyTests {

        private UUID savedThreadReplyId;

        private ThreadReplyCreateDto threadReplyUpdateDto;

        @BeforeEach
        void setUp() {
            savedThreadReplyId = testDataInitializer.setupThreadReplyInThreadByFirstUser(savedEventId, savedThreadId);

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
            UUID secondThreadReplyId = testDataInitializer.setupThreadReplyInThreadBySecondUser(savedEventId, savedThreadId);
            authHelper.setupSecurityContextForSecondUser();
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
            Instant oldReplyDate = beforeUpdate.getReplyDate();
            UUID oldThreadId = beforeUpdate.getThread().getId();
            UUID oldReplierId = beforeUpdate.getReplier().getId();

            threadReplyService.updateThreadReplyInEventThread(threadReplyUpdateDto, savedEventId, savedThreadId, savedThreadReplyId);

            ThreadReply updatedThreadReply = threadReplyRepository.findById(savedThreadReplyId).orElseThrow(ThreadReplyNotFoundException::new);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(updatedThreadReply.getContent())
                        .as("Content must be updated to the value from update dto")
                        .isEqualTo(threadReplyUpdateDto.getReplyContent());
                softly.assertThat(updatedThreadReply.getEditCounter())
                        .as("Edit counter must be incremented by exactly 1")
                        .isEqualTo(oldEditCounter + 1);
                softly.assertThat(updatedThreadReply.getReplyDate())
                        .as("Reply creation date must stay unchanged on update")
                        .isEqualTo(oldReplyDate);
                softly.assertThat(updatedThreadReply.getThread().getId())
                        .as("Reply must remain related to the same thread after update")
                        .isEqualTo(oldThreadId);
                softly.assertThat(updatedThreadReply.getReplier().getId())
                        .as("Reply owner must remain unchanged on update")
                        .isEqualTo(oldReplierId);
                softly.assertThat(updatedThreadReply.getLastUpdate())
                        .as("LastUpdate must use the application clock")
                        .isEqualTo(TimeConstants.NOW);
            });
        }

        @Test
        @DisplayName("When updating thread reply should not change parent thread last activity and reply count")
        public void whenUpdatingThreadReplyShouldNotChangeParentThreadLastActivityAndReplyCount() {
            authHelper.setupSecurityContextForFirstUser();

            Thread threadBeforeUpdate = threadRepository.findById(savedThreadId).orElseThrow(ThreadNotFoundException::new);
            Instant lastActivityBeforeUpdate = threadBeforeUpdate.getLastActivity();
            int replyCountBeforeUpdate = threadBeforeUpdate.getReplyCount();

            threadReplyService.updateThreadReplyInEventThread(threadReplyUpdateDto, savedEventId, savedThreadId, savedThreadReplyId);

            Thread threadAfterUpdate = threadRepository.findById(savedThreadId).orElseThrow(ThreadNotFoundException::new);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(threadAfterUpdate.getLastActivity())
                        .as("Updating a reply should not affect the parent thread last activity")
                        .isEqualTo(lastActivityBeforeUpdate);
                softly.assertThat(threadAfterUpdate.getReplyCount())
                        .as("Updating a reply should not affect the parent thread reply count")
                        .isEqualTo(replyCountBeforeUpdate);
            });
        }
    }


    @Nested
    @DisplayName("Get thread replies in event thread tests: ")
    class GetThreadRepliesInEventThreadTest {

        private int pageNumber;

        @BeforeEach
        void setUp() {
            pageNumber = PaginationConstants.PAGE_ZERO;
            testDataInitializer.addSecondUserToAttenders(savedEventId);
        }

        private ThreadReply getStoredReply(UUID replyId) {
            return threadReplyRepository.findById(replyId).orElseThrow(ThreadReplyNotFoundException::new);
        }

        private void setReplyDate(UUID replyId, Instant replyDate) {
            ThreadReply threadReply = getStoredReply(replyId);
            threadReply.setReplyDate(replyDate);
            threadReplyRepository.saveAndFlush(threadReply);
        }

        private void setLastUpdate(UUID replyId, Instant lastUpdate) {
            ThreadReply threadReply = getStoredReply(replyId);
            threadReply.setLastUpdate(lastUpdate);
            threadReplyRepository.saveAndFlush(threadReply);
        }

        private List<UUID> getReplyIds(ThreadReplyPageDto threadReplyPageDto) {
            return threadReplyPageDto.replies().stream()
                    .map(ThreadReplyDto::getId)
                    .toList();
        }

        @Test
        @DisplayName("When getting thread replies in event thread should throw UserNotAuthenticatedException if user is not authenticated")
        public void whenGettingThreadRepliesInEventThreadShouldThrowUserNotAuthenticatedExceptionIfUserIsNotAuthenticated() {
            SecurityContextHolder.clearContext();

            assertThatThrownBy(() -> threadReplyService.getRepliesInEventThread(
                    savedEventId,
                    savedThreadId,
                    pageNumber
            )).isInstanceOf(UserNotAuthenticatedException.class);
        }

        @Test
        @DisplayName("When getting thread replies in event thread should throw InvalidPageNumberException if page number is below zero")
        public void whenGettingThreadRepliesInEventThreadShouldThrowInvalidPageNumberExceptionIfPageNumberIsBelowZero() {
            authHelper.setupSecurityContextForFirstUser();

            assertThatThrownBy(() -> threadReplyService.getRepliesInEventThread(
                    savedEventId,
                    savedThreadId,
                    PaginationConstants.PAGE_MINUS_ONE
            )).isInstanceOf(InvalidPageNumberException.class);
        }

        @Test
        @DisplayName("When getting thread replies in event thread should throw EventNotFoundException if event with given id does not exist")
        public void whenGettingThreadRepliesInEventThreadShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
            authHelper.setupSecurityContextForFirstUser();

            assertThatThrownBy(() -> threadReplyService.getRepliesInEventThread(
                    EventConstants.NOT_EXISTING_EVENT_ID,
                    savedThreadId,
                    pageNumber
            )).isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("When getting thread replies in event thread should throw NotEventAttenderException if user is not attending event")
        public void whenGettingThreadRepliesInEventThreadShouldThrowNotEventAttenderExceptionIfUserIsNotAttendingEvent() {
            UUID secondEventId = testDataInitializer.setupEventByFirstUser();
            UUID secondThreadId = testDataInitializer.setupThreadInEventByFirstUser(secondEventId);
            authHelper.setupSecurityContextForSecondUser();

            assertThatThrownBy(() -> threadReplyService.getRepliesInEventThread(
                    secondEventId,
                    secondThreadId,
                    pageNumber
            )).isInstanceOf(NotEventAttenderException.class);
        }

        @Test
        @DisplayName("When getting thread replies in event thread should throw ThreadNotFoundInEventException if thread with given id does not exist")
        public void whenGettingThreadRepliesInEventThreadShouldThrowThreadNotFoundInEventExceptionIfThreadWithGivenIdDoesNotExist() {
            authHelper.setupSecurityContextForFirstUser();

            assertThatThrownBy(() -> threadReplyService.getRepliesInEventThread(
                    savedEventId,
                    ThreadConstants.NOT_EXISTING_THREAD_ID,
                    pageNumber
            )).isInstanceOf(ThreadNotFoundInEventException.class);
        }

        @Test
        @DisplayName("When getting thread replies in event thread should throw ThreadNotFoundInEventException if thread belongs to different event")
        public void whenGettingThreadRepliesInEventThreadShouldThrowThreadNotFoundInEventExceptionIfThreadBelongsToDifferentEvent() {
            UUID secondEventId = testDataInitializer.setupEventByFirstUser();
            UUID secondThreadId = testDataInitializer.setupThreadInEventByFirstUser(secondEventId);
            authHelper.setupSecurityContextForFirstUser();

            assertThatThrownBy(() -> threadReplyService.getRepliesInEventThread(
                    savedEventId,
                    secondThreadId,
                    pageNumber
            )).isInstanceOf(ThreadNotFoundInEventException.class);

            assertThatThrownBy(() -> threadReplyService.getRepliesInEventThread(
                    secondEventId,
                    savedThreadId,
                    pageNumber
            )).isInstanceOf(ThreadNotFoundInEventException.class);
        }

        @Test
        @DisplayName("When getting thread replies in event thread should return empty first page if thread has no replies")
        public void whenGettingThreadRepliesInEventThreadShouldReturnEmptyFirstPageIfThreadHasNoReplies() {
            authHelper.setupSecurityContextForSecondUser();

            ThreadReplyPageDto output = threadReplyService.getRepliesInEventThread(savedEventId, savedThreadId, pageNumber);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(output.replies()).isEmpty();
                softly.assertThat(output.pageNumber()).isEqualTo(PaginationConstants.PAGE_ZERO);
                softly.assertThat(output.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(output.totalElements()).isZero();
                softly.assertThat(output.totalPages()).isZero();
                softly.assertThat(output.lastPage()).isTrue();
            });
        }

        @Test
        @DisplayName("When getting thread replies in event thread should return reply dtos with correct data for event attender")
        public void whenGettingThreadRepliesInEventThreadShouldReturnReplyDtosWithCorrectDataForEventAttender() {
            UUID olderReplyId = testDataInitializer.setupThreadReplyInThreadByFirstUser(
                    savedEventId,
                    savedThreadId,
                    ThreadReplyConstants.FIRST_REPLY_CONTENT
            );
            UUID newerReplyId = testDataInitializer.setupThreadReplyInThreadBySecondUser(
                    savedEventId,
                    savedThreadId,
                    ThreadReplyConstants.SECOND_REPLY_CONTENT
            );

            setReplyDate(olderReplyId, TimeConstants.TWO_HOURS_AGO);
            setReplyDate(newerReplyId, TimeConstants.ONE_HOUR_AGO);
            setLastUpdate(olderReplyId, TimeConstants.TWO_HOURS_AGO);
            setLastUpdate(newerReplyId, TimeConstants.NOW);

            ThreadReply expectedOldestReply = getStoredReply(olderReplyId);
            authHelper.setupSecurityContextForSecondUser();

            ThreadReplyPageDto output = threadReplyService.getRepliesInEventThread(savedEventId, savedThreadId, pageNumber);
            ThreadReplyDto firstReplyDto = output.replies().getFirst();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(getReplyIds(output)).containsExactly(olderReplyId, newerReplyId);
                softly.assertThat(output.pageNumber()).isEqualTo(PaginationConstants.PAGE_ZERO);
                softly.assertThat(output.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(output.totalElements()).isEqualTo(2);
                softly.assertThat(output.totalPages()).isEqualTo(1);
                softly.assertThat(output.lastPage()).isTrue();
                softly.assertThat(firstReplyDto.getId()).isEqualTo(expectedOldestReply.getId());
                softly.assertThat(firstReplyDto.getThreadId()).isEqualTo(savedThreadId);
                softly.assertThat(firstReplyDto.getContent()).isEqualTo(expectedOldestReply.getContent());
                softly.assertThat(firstReplyDto.getReplyDate()).isEqualTo(expectedOldestReply.getReplyDate());
                softly.assertThat(firstReplyDto.getLastUpdate()).isEqualTo(expectedOldestReply.getLastUpdate());
                softly.assertThat(firstReplyDto.getEditCounter()).isEqualTo(expectedOldestReply.getEditCounter());
                softly.assertThat(firstReplyDto.getReplier().getId()).isEqualTo(expectedOldestReply.getReplier().getId());
            });
        }

        @Test
        @DisplayName("When getting thread replies in event thread should return replies from correct thread only")
        public void whenGettingThreadRepliesInEventThreadShouldReturnRepliesFromCorrectThreadOnly() {
            UUID targetOlderReplyId = testDataInitializer.setupThreadReplyInThreadByFirstUser(
                    savedEventId,
                    savedThreadId,
                    ThreadReplyConstants.FIRST_REPLY_CONTENT
            );
            UUID targetNewerReplyId = testDataInitializer.setupThreadReplyInThreadBySecondUser(
                    savedEventId,
                    savedThreadId,
                    ThreadReplyConstants.SECOND_REPLY_CONTENT
            );
            UUID secondThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);
            UUID secondEventId = testDataInitializer.setupEventByFirstUser();
            UUID threadFromSecondEventId = testDataInitializer.setupThreadInEventByFirstUser(secondEventId);

            testDataInitializer.setupThreadReplyInThreadByFirstUser(savedEventId, secondThreadId, ThreadReplyConstants.THIRD_REPLY_CONTENT);
            testDataInitializer.setupThreadReplyInThreadByFirstUser(secondEventId, threadFromSecondEventId, ThreadReplyConstants.OLD_REPLY_CONTENT);

            setReplyDate(targetOlderReplyId, TimeConstants.TWO_HOURS_AGO);
            setReplyDate(targetNewerReplyId, TimeConstants.ONE_HOUR_AGO);

            authHelper.setupSecurityContextForSecondUser();

            ThreadReplyPageDto output = threadReplyService.getRepliesInEventThread(savedEventId, savedThreadId, pageNumber);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(getReplyIds(output)).containsExactly(targetOlderReplyId, targetNewerReplyId);
                softly.assertThat(output.replies()).allSatisfy(reply -> softly.assertThat(reply.getThreadId()).isEqualTo(savedThreadId));
            });
        }

        @Test
        @DisplayName("When getting thread replies in event thread should return replies sorted by reply date ascending")
        public void whenGettingThreadRepliesInEventThreadShouldReturnRepliesSortedByReplyDateAscending() {
            UUID newestReplyId = testDataInitializer.setupThreadReplyInThreadByFirstUser(
                    savedEventId,
                    savedThreadId,
                    ThreadReplyConstants.FIRST_REPLY_CONTENT
            );
            UUID middleReplyId = testDataInitializer.setupThreadReplyInThreadByFirstUser(
                    savedEventId,
                    savedThreadId,
                    ThreadReplyConstants.SECOND_REPLY_CONTENT
            );
            UUID oldestReplyId = testDataInitializer.setupThreadReplyInThreadBySecondUser(
                    savedEventId,
                    savedThreadId,
                    ThreadReplyConstants.THIRD_REPLY_CONTENT
            );

            setReplyDate(newestReplyId, TimeConstants.NOW);
            setReplyDate(middleReplyId, TimeConstants.ONE_HOUR_AGO);
            setReplyDate(oldestReplyId, TimeConstants.TWO_HOURS_AGO);

            authHelper.setupSecurityContextForSecondUser();

            ThreadReplyPageDto output = threadReplyService.getRepliesInEventThread(savedEventId, savedThreadId, pageNumber);

            assertThat(getReplyIds(output))
                    .containsExactly(oldestReplyId, middleReplyId, newestReplyId);
        }

        @Test
        @DisplayName("When getting thread replies in event thread should use id as stable secondary sort when reply dates are equal")
        public void whenGettingThreadRepliesInEventThreadShouldUseIdAsStableSecondarySortWhenReplyDatesAreEqual() {
            List<UUID> savedReplyIds = testDataInitializer.setupThreadRepliesInThreadByFirstUser(
                    savedEventId,
                    savedThreadId,
                    PaginationConstants.FIVE_ELEMENTS
            );
            Instant sameReplyDate = TimeConstants.ONE_HOUR_AGO;
            savedReplyIds.forEach(replyId -> setReplyDate(replyId, sameReplyDate));

            authHelper.setupSecurityContextForSecondUser();

            ThreadReplyPageDto firstOutput = threadReplyService.getRepliesInEventThread(savedEventId, savedThreadId, pageNumber);
            ThreadReplyPageDto secondOutput = threadReplyService.getRepliesInEventThread(savedEventId, savedThreadId, pageNumber);

            List<UUID> expectedDatabaseOrder = savedReplyIds.stream()
                    .sorted(Comparator.comparing(UUID::toString).reversed())
                    .toList();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(getReplyIds(firstOutput)).containsExactlyElementsOf(expectedDatabaseOrder);
                softly.assertThat(getReplyIds(secondOutput)).containsExactlyElementsOf(getReplyIds(firstOutput));
            });
        }

        @Test
        @DisplayName("When getting thread replies in event thread should correctly paginate across multiple pages")
        public void whenGettingThreadRepliesInEventThreadShouldCorrectlyPaginateAcrossMultiplePages() {
            testDataInitializer.setupThreadRepliesInThreadByFirstUser(
                    savedEventId,
                    savedThreadId,
                    PaginationConstants.DEFAULT_PAGE_SIZE + 1
            );
            authHelper.setupSecurityContextForSecondUser();

            ThreadReplyPageDto firstPage = threadReplyService.getRepliesInEventThread(
                    savedEventId,
                    savedThreadId,
                    PaginationConstants.PAGE_ZERO
            );
            ThreadReplyPageDto secondPage = threadReplyService.getRepliesInEventThread(
                    savedEventId,
                    savedThreadId,
                    PaginationConstants.PAGE_ONE
            );

            List<UUID> firstPageReplyIds = getReplyIds(firstPage);
            List<UUID> secondPageReplyIds = getReplyIds(secondPage);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(firstPage.replies()).hasSize(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(firstPage.pageNumber()).isEqualTo(PaginationConstants.PAGE_ZERO);
                softly.assertThat(firstPage.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(firstPage.totalElements()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE + 1L);
                softly.assertThat(firstPage.totalPages()).isEqualTo(2);
                softly.assertThat(firstPage.lastPage()).isFalse();

                softly.assertThat(secondPage.replies()).hasSize(1);
                softly.assertThat(secondPage.pageNumber()).isEqualTo(PaginationConstants.PAGE_ONE);
                softly.assertThat(secondPage.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(secondPage.totalElements()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE + 1L);
                softly.assertThat(secondPage.totalPages()).isEqualTo(2);
                softly.assertThat(secondPage.lastPage()).isTrue();

                softly.assertThat(firstPageReplyIds).doesNotContainAnyElementsOf(secondPageReplyIds);
            });
        }

        @Test
        @DisplayName("When getting thread replies in event thread should return empty page beyond last available page with preserved metadata")
        public void whenGettingThreadRepliesInEventThreadShouldReturnEmptyPageBeyondLastAvailablePageWithPreservedMetadata() {
            testDataInitializer.setupThreadReplyInThreadByFirstUser(savedEventId, savedThreadId, ThreadReplyConstants.FIRST_REPLY_CONTENT);
            testDataInitializer.setupThreadReplyInThreadByFirstUser(savedEventId, savedThreadId, ThreadReplyConstants.SECOND_REPLY_CONTENT);
            authHelper.setupSecurityContextForSecondUser();

            ThreadReplyPageDto output = threadReplyService.getRepliesInEventThread(
                    savedEventId,
                    savedThreadId,
                    PaginationConstants.PAGE_ONE
            );

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(output.replies()).isEmpty();
                softly.assertThat(output.pageNumber()).isEqualTo(PaginationConstants.PAGE_ONE);
                softly.assertThat(output.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(output.totalElements()).isEqualTo(2);
                softly.assertThat(output.totalPages()).isEqualTo(1);
                softly.assertThat(output.lastPage()).isTrue();
            });
        }
    }
}
