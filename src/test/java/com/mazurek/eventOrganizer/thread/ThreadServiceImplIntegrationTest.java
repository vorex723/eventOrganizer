package com.mazurek.eventOrganizer.thread;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.common.SortDirection;
import com.mazurek.eventOrganizer.event.EventService;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotEventAttendeeException;
import com.mazurek.eventOrganizer.exception.thread.NotThreadOwnerException;
import com.mazurek.eventOrganizer.exception.thread.ThreadNotFoundInEventException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestDataInitializer;
import com.mazurek.eventOrganizer.testData.TestPersistenceQueries;
import com.mazurek.eventOrganizer.testData.builders.dto.ThreadCreateDtoTestBuilder;
import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadOverviewDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadOverviewPageDto;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ThreadService integration tests:")
public class ThreadServiceImplIntegrationTest {

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
    @Autowired
    private TestPersistenceQueries persistenceQueries;

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

            var beforeOperation = persistenceQueries.threadState();

            assertThatThrownBy(() -> threadService.createThreadInEvent(threadCreateDto, EventConstants.NOT_EXISTING_EVENT_ID))
                    .isInstanceOf(EventNotFoundException.class);
            assertThat(persistenceQueries.threadState())
                    .as("Rejected service operation must preserve committed rows and outbox")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When creating thread in event should throw NotEventAttendeeException if user is not attending event")
        public void whenCreatingThreadInEventShouldThrowNotEventAttendeeExceptionIfUserIsNotAttendingEvent() {
            authHelper.setupSecurityContextForSecondUser();

            var beforeOperation = persistenceQueries.threadState();

            assertThatThrownBy(() -> threadService.createThreadInEvent(threadCreateDto, savedEventId))
                    .isInstanceOf(NotEventAttendeeException.class);
            assertThat(persistenceQueries.threadState())
                    .as("Rejected service operation must preserve committed rows and outbox")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When creating thread in event should save it with correct data")
        public void whenCreatingThreadInEventShouldSaveItWithCorrectData() {
            authHelper.setupSecurityContextForFirstUser();
            savedThreadId = threadService.createThreadInEvent(threadCreateDto, savedEventId).getId();

            Thread savedThread = requirePresent(
                    threadRepository.findById(savedThreadId),
                    "Expected created thread to exist before persistence assertions");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(savedThread.getCreateDate()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(savedThread.getLastUpdate()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(savedThread.getLastActivity()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(savedThread.getReplyCount()).isZero();
                softly.assertThat(savedThread.getName())
                        .as("Saved thread name should match thread create dto")
                        .isEqualTo(threadCreateDto.getName());
                softly.assertThat(savedThread.getContent())
                        .as("Saved thread content should match thread create dto")
                        .isEqualTo(threadCreateDto.getContent());
                softly.assertThat(savedThread.getCreateDate())
                        .as("Create date and last update should be equal on creation")
                        .isEqualTo(savedThread.getLastUpdate());
                softly.assertThat(savedThread.getEditCount())
                        .as("Edit counter should be zero on creation")
                        .isZero();
            });
        }

        @Test
        @DisplayName("When creating thread with maximum content should persist it")
        void whenCreatingThreadWithMaximumContentShouldPersistIt() {
            String maximumContent = "a".repeat(1000);
            threadCreateDto.setContent(maximumContent);
            authHelper.setupSecurityContextForFirstUser();

            UUID threadId = threadService.createThreadInEvent(threadCreateDto, savedEventId).getId();

            Thread savedThread = requirePresent(
                    threadRepository.findById(threadId),
                    "Expected created thread to exist before maximum content assertions");
            assertThat(savedThread.getContent())
                    .isEqualTo(maximumContent);
        }

        @Test
        @DisplayName("When creating thread in event should save all relationships in database")
        public void whenCreatingThreadInEventShouldSaveAllRelationshipsInDatabase() {
            authHelper.setupSecurityContextForFirstUser();

            savedThreadId = threadService.createThreadInEvent(threadCreateDto, savedEventId).getId();

            User performingUser = userRepository.findByEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow(UserNotFoundException::new);
            Thread savedThread = requirePresent(
                    threadRepository.findById(savedThreadId),
                    "Expected created thread to exist before relationship assertions");

            assertThat(savedThread.getOwner()).as("Expected mapped relationship before dereference").isNotNull();
            assertThat(savedThread.getEvent()).as("Expected mapped relationship before dereference").isNotNull();
            SoftAssertions.assertSoftly(softly -> {
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

            var beforeOperation = persistenceQueries.threadState();

            assertThatThrownBy(() -> threadService.updateThreadInEvent(threadUpdateDto, EventConstants.NOT_EXISTING_EVENT_ID, savedThreadId))
                    .isInstanceOf(EventNotFoundException.class);
            assertThat(persistenceQueries.threadState())
                    .as("Rejected service operation must preserve committed rows and outbox")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When updating thread in event should throw ThreadNotFoundInEventException if thread does not exist")
        public void whenUpdatingThreadInEventShouldThrowThreadNotFoundInEventExceptionIfThreadDoesNotExist() {
            authHelper.setupSecurityContextForFirstUser();

            var beforeOperation = persistenceQueries.threadState();

            assertThatThrownBy(() -> threadService.updateThreadInEvent(threadUpdateDto, savedEventId, EventConstants.NOT_EXISTING_EVENT_ID))
                    .isInstanceOf(ThreadNotFoundInEventException.class);
            assertThat(persistenceQueries.threadState())
                    .as("Rejected service operation must preserve committed rows and outbox")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When updating thread in event should throw ThreadNotFoundInEventException if thread exists but is not related with event with given id")
        public void whenUpdatingThreadInEventShouldThrowThreadNotFoundInEventExceptionIfThreadExistButIsNotRelatedWithEventWithGivenId() {
            UUID secondEventId = testDataInitializer.setupEventByFirstUser();
            authHelper.setupSecurityContextForFirstUser();

            var beforeOperation = persistenceQueries.threadState();

            assertThatThrownBy(() -> threadService.updateThreadInEvent(threadUpdateDto, secondEventId, savedThreadId))
                    .isInstanceOf(ThreadNotFoundInEventException.class);
            assertThat(persistenceQueries.threadState())
                    .as("Rejected service operation must preserve committed rows and outbox")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When updating thread in event should throw NotEventAttendeeException if user is not attending event anymore")
        public void whenUpdatingThreadInEventShouldThrowNotEventAttendeeExceptionIfUserIsNotAttendingEventAnymore() {
            authHelper.setupSecurityContextForSecondUser();
            eventService.addAttendeeToEvent(savedEventId);
            eventService.removeAttendeeFromEvent(savedEventId);

            var beforeOperation = persistenceQueries.threadState();

            assertThatThrownBy(() -> threadService.updateThreadInEvent(threadUpdateDto, savedEventId, savedThreadId))
                    .isInstanceOf(NotEventAttendeeException.class);
            assertThat(persistenceQueries.threadState())
                    .as("Rejected service operation must preserve committed rows and outbox")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When updating thread in event should throw NotThreadOwnerException if performing user does not own thread with given id")
        public void whenUpdatingThreadInEventShouldThrowNotThreadOwnerExceptionIfPerformingUserDoesNotOwnThreadWithGivenId() {
            authHelper.setupSecurityContextForSecondUser();
            eventService.addAttendeeToEvent(savedEventId);

            var beforeOperation = persistenceQueries.threadState();

            assertThatThrownBy(() -> threadService.updateThreadInEvent(threadUpdateDto, savedEventId, savedThreadId))
                    .isInstanceOf(NotThreadOwnerException.class);
            assertThat(persistenceQueries.threadState())
                    .as("Rejected service operation must preserve committed rows and outbox")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When updating thread in event should persist all updated fields in database")
        public void whenUpdatingThreadInEventShouldPersistAllUpdatedFieldsInDatabase() {
            Thread beforeUpdate = requirePresent(
                    threadRepository.findById(savedThreadId),
                    "Expected thread to exist before update");
            beforeUpdate.setLastActivity(TimeConstants.TWO_HOURS_AGO);
            threadRepository.saveAndFlush(beforeUpdate);
            Instant beforeLastActivity = beforeUpdate.getLastActivity();
            int beforeReplyCount = beforeUpdate.getReplyCount();
            UUID beforeOwnerId = beforeUpdate.getOwner().getId();
            UUID beforeEventId = beforeUpdate.getEvent().getId();
            Instant beforeUpdateCreateDate = beforeUpdate.getCreateDate();

            authHelper.setupSecurityContextForFirstUser();
            var beforeSideEffects = persistenceQueries.threadState();

            threadService.updateThreadInEvent(threadUpdateDto, savedEventId, savedThreadId);

            Thread updatedThread = requirePresent(
                    threadRepository.findById(savedThreadId),
                    "Expected updated thread to exist before persistence assertions");

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(updatedThread.getLastActivity()).isEqualTo(beforeLastActivity);
                softly.assertThat(updatedThread.getReplyCount()).isEqualTo(beforeReplyCount);
                softly.assertThat(updatedThread.getOwner().getId()).isEqualTo(beforeOwnerId);
                softly.assertThat(updatedThread.getEvent().getId()).isEqualTo(beforeEventId);
                softly.assertThat(updatedThread.getName())
                        .as("Name should be updated to value from update dto")
                        .isEqualTo(threadUpdateDto.getName());
                softly.assertThat(updatedThread.getContent())
                        .as("Content should be updated to value from update dto")
                        .isEqualTo(threadUpdateDto.getContent());
                softly.assertThat(updatedThread.getEditCount())
                        .as("Edit counter should be incremented by exactly 1")
                        .isEqualTo(1);
                softly.assertThat(updatedThread.getCreateDate())
                        .as("Create date should not change on update")
                        .isEqualTo(beforeUpdateCreateDate);
                softly.assertThat(updatedThread.getLastUpdate())
                        .as("LastUpdate should use the application clock")
                        .isEqualTo(TimeConstants.NOW);
            });
            var afterSideEffects = persistenceQueries.threadState();
            for (String table : List.of("replies", "events", "attendees", "notifications", "deliveries")) {
                assertThat(afterSideEffects.get(table)).as("Editing must preserve unrelated rows in %s", table)
                        .isEqualTo(beforeSideEffects.get(table));
            }
        }

        @Test
        @DisplayName("When updating thread with maximum content should persist it")
        void whenUpdatingThreadWithMaximumContentShouldPersistIt() {
            String maximumContent = "a".repeat(1000);
            threadUpdateDto.setContent(maximumContent);
            authHelper.setupSecurityContextForFirstUser();

            threadService.updateThreadInEvent(threadUpdateDto, savedEventId, savedThreadId);

            Thread updatedThread = requirePresent(
                    threadRepository.findById(savedThreadId),
                    "Expected updated thread to exist before maximum content assertions");
            assertThat(updatedThread.getContent())
                    .isEqualTo(maximumContent);
        }

    }

    @Nested
    @DisplayName("Get threads by event id tests: ")
    class GetThreadsByEventIdTests {

        private ThreadSortField sortField;
        private SortDirection sortDirection;


        @BeforeEach
        void setUp() {

            sortField = ThreadSortField.LAST_ACTIVITY;
            sortDirection = SortDirection.DESC;
            testDataInitializer.addSecondUserToAttendees(savedEventId);
        }

        private Set<UUID> prepareThreadsForEvent(int threadAmount, UUID eventId) {
            return IntStream.range(0, threadAmount).mapToObj(threadNumber ->
                    testDataInitializer.setupThreadInEventByFirstUser(eventId))
                    .collect(Collectors.toSet());

        }

        private void setLastActivityInThread(UUID threadId, Instant lastActivityTime){
            Thread thread = requirePresent(
                    threadRepository.findById(threadId),
                    "Expected thread to exist before setting last activity: " + threadId);
            thread.setLastActivity(lastActivityTime);
            threadRepository.saveAndFlush(thread);
        }
        private void setCreateDate(UUID threadId, Instant createDate) {
            Thread thread = requirePresent(
                    threadRepository.findById(threadId),
                    "Expected thread to exist before setting creation date: " + threadId);
            thread.setCreateDate(createDate);
            threadRepository.saveAndFlush(thread);
        }

        private void setReplyCount(UUID threadId, int replyCount) {
            Thread thread = requirePresent(
                    threadRepository.findById(threadId),
                    "Expected thread to exist before setting reply count: " + threadId);
            thread.setReplyCount(replyCount);
            threadRepository.saveAndFlush(thread);
        }

        @Test
        @DisplayName("When getting threads by event id should correctly map page metadata")
        public void whenGettingThreadsByEventIdShouldCorrectlyMapPageMetadata(){
            prepareThreadsForEvent(PaginationConstants.TEN_ELEMENTS, savedEventId);
            authHelper.setupSecurityContextForSecondUser();

            var beforeRead = persistenceQueries.threadState();

            ThreadOverviewPageDto output = threadService.getThreadsByEventId(savedEventId, PaginationConstants.PAGE_ZERO, sortField, sortDirection);

            assertThat(output).as("Expected output before field assertions").isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(output.threads().size()).isEqualTo(PaginationConstants.TEN_ELEMENTS);
                softly.assertThat(output.pageNumber()).isEqualTo(PaginationConstants.PAGE_ZERO);
                softly.assertThat(output.totalPages()).isEqualTo(1);
                softly.assertThat(output.lastPage()).isTrue();
                softly.assertThat(output.totalElements()).isEqualTo(PaginationConstants.TEN_ELEMENTS);

            });
            assertThat(persistenceQueries.threadState())
                    .as("Reading threads or replies must not mutate committed rows or outbox")
                    .isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting threads by event id should return empty first page if event has no threads")
        public void whenGettingThreadsByEventIdShouldReturnEmptyFirstPageIfEventHasNoThreads() {
            authHelper.setupSecurityContextForSecondUser();

            var beforeRead = persistenceQueries.threadState();

            ThreadOverviewPageDto output = threadService.getThreadsByEventId(
                    savedEventId,
                    PaginationConstants.PAGE_ZERO,
                    sortField,
                    sortDirection
            );

            assertThat(output).as("Expected output before field assertions").isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(output.threads()).isEmpty();
                softly.assertThat(output.pageNumber()).isEqualTo(PaginationConstants.PAGE_ZERO);
                softly.assertThat(output.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(output.totalElements()).isZero();
                softly.assertThat(output.totalPages()).isZero();
                softly.assertThat(output.lastPage()).isTrue();
            });
            assertThat(persistenceQueries.threadState())
                    .as("Reading threads or replies must not mutate committed rows or outbox")
                    .isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting threads by event id should return threads from correct event only")
        public void whenGettingThreadsByEventIdShouldReturnThreadsFromCorrectEventOnly(){
            Set<UUID> savedThreadsIds = prepareThreadsForEvent(PaginationConstants.TEN_ELEMENTS, savedEventId);
            UUID secondEventId = testDataInitializer.setupEventByFirstUser();
            prepareThreadsForEvent(PaginationConstants.TEN_ELEMENTS, secondEventId);

            authHelper.setupSecurityContextForSecondUser();

            var beforeRead = persistenceQueries.threadState();

            ThreadOverviewPageDto output = threadService.getThreadsByEventId(savedEventId, PaginationConstants.PAGE_ZERO, sortField, sortDirection);

            SoftAssertions.assertSoftly(softly -> {
                    output.threads().forEach(thread -> softly.assertThat(thread.eventId()).isEqualTo(savedEventId));
                    softly.assertThat(output.threads()).hasSize(PaginationConstants.TEN_ELEMENTS);
                    softly.assertThat(output.threads()).extracting(ThreadOverviewDto::id)
                            .containsExactlyInAnyOrderElementsOf(savedThreadsIds);
            });
            assertThat(persistenceQueries.threadState())
                    .as("Reading threads or replies must not mutate committed rows or outbox")
                    .isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting threads by event id should throw NotEventAttendeeException if performing user is not attending event")
        public void whenGettingThreadsByEventIdShouldThrowNotEventAttendeeExceptionIfPerformingUserIsNotAttendingEvent() {
            UUID notAttendedEventId = testDataInitializer.setupEventByFirstUser();
            authHelper.setupSecurityContextForSecondUser();

            var beforeOperation = persistenceQueries.threadState();

            assertThatThrownBy(() -> threadService.getThreadsByEventId(
                    notAttendedEventId,
                    PaginationConstants.PAGE_ZERO,
                    sortField,
                    sortDirection
            )).isInstanceOf(NotEventAttendeeException.class);
            assertThat(persistenceQueries.threadState())
                    .as("Rejected service operation must preserve committed rows and outbox")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When getting threads by event id should return threads sorted by last activity descending")
        public void whenGettingThreadsByEventIdShouldReturnThreadsSortedByLastActivityDescending(){
            UUID newestThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);
            UUID middleThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);
            UUID oldestThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);

            setLastActivityInThread(middleThreadId, TimeConstants.ONE_HOUR_AGO);
            setLastActivityInThread(oldestThreadId, TimeConstants.TWO_HOURS_AGO);

            authHelper.setupSecurityContextForSecondUser();

            var beforeRead = persistenceQueries.threadState();

            ThreadOverviewPageDto output = threadService.getThreadsByEventId(
                    savedEventId,
                    PaginationConstants.PAGE_ZERO,
                    ThreadSortField.LAST_ACTIVITY,
                    SortDirection.DESC);

            assertThat(output.threads().stream().map(ThreadOverviewDto::id).toList())
                    .containsExactly(newestThreadId, middleThreadId, oldestThreadId);

            assertThat(persistenceQueries.threadState())
                    .as("Reading threads or replies must not mutate committed rows or outbox")
                    .isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting threads by event id should use id as stable secondary sort when last activity values are equal")
        public void whenGettingThreadsByEventIdShouldUseIdAsStableSecondarySortWhenLastActivityValuesAreEqual() {
            UUID firstThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);
            UUID secondThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);
            UUID thirdThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);
            Instant sameLastActivity = TimeConstants.ONE_HOUR_AGO;

            setLastActivityInThread(firstThreadId, sameLastActivity);
            setLastActivityInThread(secondThreadId, sameLastActivity);
            setLastActivityInThread(thirdThreadId, sameLastActivity);

            authHelper.setupSecurityContextForSecondUser();

            var beforeRead = persistenceQueries.threadState();

            ThreadOverviewPageDto output = threadService.getThreadsByEventId(
                    savedEventId,
                    PaginationConstants.PAGE_ZERO,
                    ThreadSortField.LAST_ACTIVITY,
                    SortDirection.ASC);
            ThreadOverviewPageDto reverseOutput = threadService.getThreadsByEventId(
                    savedEventId,
                    PaginationConstants.PAGE_ZERO,
                    ThreadSortField.LAST_ACTIVITY,
                    SortDirection.DESC);

            List<UUID> ascendingIds = output.threads().stream().map(ThreadOverviewDto::id).toList();
            List<UUID> descendingIds = reverseOutput.threads().stream().map(ThreadOverviewDto::id).toList();

            List<UUID> expectedAscendingIds = List.of(firstThreadId, secondThreadId, thirdThreadId).stream()
                    .sorted(java.util.Comparator.comparing(UUID::toString)).toList();
            assertThat(ascendingIds).containsExactlyElementsOf(expectedAscendingIds);
            assertThat(descendingIds).containsExactlyElementsOf(ascendingIds.reversed());
            assertThat(persistenceQueries.threadState())
                    .as("Reading threads or replies must not mutate committed rows or outbox")
                    .isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting threads by event id should return threads sorted by create date ascending")
        public void whenGettingThreadsByEventIdShouldReturnThreadsSortedByCreateDateAscending() {
            UUID newestThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);
            UUID middleThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);
            UUID oldestThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);

            setCreateDate(newestThreadId, TimeConstants.NOW);
            setCreateDate(middleThreadId, TimeConstants.ONE_HOUR_AGO);
            setCreateDate(oldestThreadId, TimeConstants.TWO_HOURS_AGO);

            authHelper.setupSecurityContextForSecondUser();

            var beforeRead = persistenceQueries.threadState();

            ThreadOverviewPageDto output = threadService.getThreadsByEventId(
                    savedEventId,
                    PaginationConstants.PAGE_ZERO,
                    ThreadSortField.CREATE_DATE,
                    SortDirection.ASC);

            assertThat(output.threads().stream().map(ThreadOverviewDto::id).toList())
                    .containsExactly(oldestThreadId, middleThreadId, newestThreadId);
            assertThat(persistenceQueries.threadState())
                    .as("Reading threads or replies must not mutate committed rows or outbox")
                    .isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting threads by event id should return threads sorted by reply count descending")
        public void whenGettingThreadsByEventIdShouldReturnThreadsSortedByReplyCountDescending() {
            UUID mostRepliesThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);
            UUID middleRepliesThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);
            UUID leastRepliesThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);

            setReplyCount(mostRepliesThreadId, 5);
            setReplyCount(middleRepliesThreadId, 3);
            setReplyCount(leastRepliesThreadId, 1);

            authHelper.setupSecurityContextForSecondUser();

            var beforeRead = persistenceQueries.threadState();

            ThreadOverviewPageDto output = threadService.getThreadsByEventId(
                    savedEventId,
                    PaginationConstants.PAGE_ZERO,
                    ThreadSortField.REPLY_COUNT,
                    SortDirection.DESC);

            assertThat(output.threads().stream().map(ThreadOverviewDto::id).toList())
                    .containsExactly(mostRepliesThreadId, middleRepliesThreadId, leastRepliesThreadId);
            assertThat(persistenceQueries.threadState())
                    .as("Reading threads or replies must not mutate committed rows or outbox")
                    .isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting threads by event id should correctly paginate across multiple pages")
        public void whenGettingThreadsByEventIdShouldCorrectlyPaginateAcrossMultiplePages() {
            List<UUID> createdIds = prepareThreadsForEvent(PaginationConstants.DEFAULT_PAGE_SIZE + 1, savedEventId).stream().toList();
            for (int index = 0; index < createdIds.size(); index++) {
                setLastActivityInThread(createdIds.get(index), TimeConstants.TWO_HOURS_AGO.plusSeconds(index));
            }
            List<UUID> expectedIds = createdIds.reversed();
            authHelper.setupSecurityContextForSecondUser();

            var beforeRead = persistenceQueries.threadState();

            ThreadOverviewPageDto firstPage = threadService.getThreadsByEventId(
                    savedEventId,
                    PaginationConstants.PAGE_ZERO,
                    sortField,
                    sortDirection
            );
            ThreadOverviewPageDto secondPage = threadService.getThreadsByEventId(
                    savedEventId,
                    PaginationConstants.PAGE_ONE,
                    sortField,
                    sortDirection
            );

            List<UUID> firstPageThreadIds = firstPage.threads().stream().map(ThreadOverviewDto::id).toList();
            List<UUID> secondPageThreadIds = secondPage.threads().stream().map(ThreadOverviewDto::id).toList();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(firstPage.threads()).hasSize(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(firstPage.pageNumber()).isEqualTo(PaginationConstants.PAGE_ZERO);
                softly.assertThat(firstPage.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(firstPage.totalElements()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE + 1L);
                softly.assertThat(firstPage.totalPages()).isEqualTo(2);
                softly.assertThat(firstPage.lastPage()).isFalse();

                softly.assertThat(secondPage.threads()).hasSize(1);
                softly.assertThat(secondPage.pageNumber()).isEqualTo(PaginationConstants.PAGE_ONE);
                softly.assertThat(secondPage.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(secondPage.totalElements()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE + 1L);
                softly.assertThat(secondPage.totalPages()).isEqualTo(2);
                softly.assertThat(secondPage.lastPage()).isTrue();

                softly.assertThat(firstPageThreadIds).doesNotContainAnyElementsOf(secondPageThreadIds);
            });
            assertThat(persistenceQueries.threadState())
                    .as("Reading threads or replies must not mutate committed rows or outbox")
                    .isEqualTo(beforeRead);
            assertThat(firstPageThreadIds).containsExactlyElementsOf(expectedIds.subList(0, PaginationConstants.DEFAULT_PAGE_SIZE));
            assertThat(secondPageThreadIds).containsExactlyElementsOf(expectedIds.subList(PaginationConstants.DEFAULT_PAGE_SIZE, expectedIds.size()));
        }

    }

    @Nested
    @DisplayName("Get thread in event tests:")
    class GetThreadInEventTests {

        @BeforeEach
        void setUp() {
            savedThreadId = testDataInitializer.setupThreadInEventByFirstUser(savedEventId);
        }

        private Thread getStoredThread(UUID threadId) {
            return requirePresent(
                    threadRepository.findById(threadId),
                    "Expected stored thread to exist before lookup assertions: " + threadId);
        }

        @Test
        @DisplayName("When getting thread in event should throw EventNotFoundException if event with given id does not exist")
        public void whenGettingThreadInEventShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
            authHelper.setupSecurityContextForFirstUser();

            var beforeOperation = persistenceQueries.threadState();

            assertThatThrownBy(() -> threadService.getThreadInEvent(EventConstants.NOT_EXISTING_EVENT_ID, savedThreadId))
                    .isInstanceOf(EventNotFoundException.class);
            assertThat(persistenceQueries.threadState())
                    .as("Rejected service operation must preserve committed rows and outbox")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When getting thread in event should throw NotEventAttendeeException if user is not attending event")
        public void whenGettingThreadInEventShouldThrowNotEventAttendeeExceptionIfUserIsNotAttendingEvent() {
            authHelper.setupSecurityContextForSecondUser();

            var beforeOperation = persistenceQueries.threadState();

            assertThatThrownBy(() -> threadService.getThreadInEvent(savedEventId, savedThreadId))
                    .isInstanceOf(NotEventAttendeeException.class);
            assertThat(persistenceQueries.threadState())
                    .as("Rejected service operation must preserve committed rows and outbox")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When getting thread in event should throw ThreadNotFoundInEventException if thread with given id does not exist")
        public void whenGettingThreadInEventShouldThrowThreadNotFoundInEventExceptionIfThreadWithGivenIdDoesNotExist() {
            authHelper.setupSecurityContextForFirstUser();

            var beforeOperation = persistenceQueries.threadState();

            assertThatThrownBy(() -> threadService.getThreadInEvent(savedEventId, ThreadConstants.NOT_EXISTING_THREAD_ID))
                    .isInstanceOf(ThreadNotFoundInEventException.class);
            assertThat(persistenceQueries.threadState())
                    .as("Rejected service operation must preserve committed rows and outbox")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When getting thread in event should throw ThreadNotFoundInEventException if thread belongs to different event")
        public void whenGettingThreadInEventShouldThrowThreadNotFoundInEventExceptionIfThreadBelongsToDifferentEvent() {
            UUID secondEventId = testDataInitializer.setupEventByFirstUser();
            UUID secondThreadId = testDataInitializer.setupThreadInEventByFirstUser(secondEventId);
            authHelper.setupSecurityContextForFirstUser();

            var beforeOperation = persistenceQueries.threadState();

            assertThatThrownBy(() -> threadService.getThreadInEvent(savedEventId, secondThreadId))
                    .isInstanceOf(ThreadNotFoundInEventException.class);
            assertThat(persistenceQueries.threadState())
                    .as("Rejected service operation must preserve committed rows and outbox")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When getting thread in event should return thread dto with correct data for event owner")
        public void whenGettingThreadInEventShouldReturnThreadDtoWithCorrectDataForEventOwner() {
            testDataInitializer.setupThreadReplyInThreadByFirstUser(savedEventId, savedThreadId);
            testDataInitializer.setupThreadReplyInThreadByFirstUser(savedEventId, savedThreadId);
            Thread expectedThread = getStoredThread(savedThreadId);
            authHelper.setupSecurityContextForFirstUser();

            var beforeRead = persistenceQueries.threadState();

            ThreadDto output = threadService.getThreadInEvent(savedEventId, savedThreadId);

            assertThat(output).as("Expected output before field assertions").isNotNull();
            assertThat(output.getOwner()).as("Expected mapped relationship before dereference").isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(output.getId()).isEqualTo(expectedThread.getId());
                softly.assertThat(output.getEventId()).isEqualTo(savedEventId);
                softly.assertThat(output.getOwner().getId()).isEqualTo(expectedThread.getOwner().getId());
                softly.assertThat(output.getName()).isEqualTo(expectedThread.getName());
                softly.assertThat(output.getContent()).isEqualTo(expectedThread.getContent());
                softly.assertThat(output.getReplyCount()).isEqualTo(expectedThread.getReplyCount());
                softly.assertThat(output.getCreateDate()).isEqualTo(expectedThread.getCreateDate());
                softly.assertThat(output.getLastUpdate()).isEqualTo(expectedThread.getLastUpdate());
                softly.assertThat(output.getEditCounter()).isEqualTo(expectedThread.getEditCount());
            });
            assertThat(persistenceQueries.threadState())
                    .as("Reading threads or replies must not mutate committed rows or outbox")
                    .isEqualTo(beforeRead);
        }

        @Test
        @DisplayName("When getting thread in event should allow event attendee who does not own thread to load it")
        public void whenGettingThreadInEventShouldAllowEventAttendeeWhoDoesNotOwnThreadToLoadIt() {
            testDataInitializer.addSecondUserToAttendees(savedEventId);
            Thread expectedThread = getStoredThread(savedThreadId);
            authHelper.setupSecurityContextForSecondUser();

            var beforeRead = persistenceQueries.threadState();

            ThreadDto output = threadService.getThreadInEvent(savedEventId, savedThreadId);

            assertThat(output).as("Expected output before field assertions").isNotNull();
            assertThat(output.getOwner()).as("Expected mapped relationship before dereference").isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(output.getId()).isEqualTo(savedThreadId);
                softly.assertThat(output.getEventId()).isEqualTo(savedEventId);
                softly.assertThat(output.getOwner().getId()).isEqualTo(expectedThread.getOwner().getId());
                softly.assertThat(output.getName()).isEqualTo(expectedThread.getName());
                softly.assertThat(output.getContent()).isEqualTo(expectedThread.getContent());
            });
            assertThat(persistenceQueries.threadState())
                    .as("Reading threads or replies must not mutate committed rows or outbox")
                    .isEqualTo(beforeRead);
        }
    }
}
