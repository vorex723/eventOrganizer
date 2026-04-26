package com.mazurek.eventOrganizer.thread;

import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.common.SortDirection;
import com.mazurek.eventOrganizer.config.properties.PaginationProperties;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.event.EventRepository;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotEventAttenderException;
import com.mazurek.eventOrganizer.exception.thread.NotThreadOwnerException;
import com.mazurek.eventOrganizer.exception.thread.ThreadNotFoundInEventException;
import com.mazurek.eventOrganizer.testData.builders.*;
import com.mazurek.eventOrganizer.testData.builders.dto.ThreadCreateDtoTestBuilder;
import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadOverviewDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadOverviewPageDto;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
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
import org.springframework.data.domain.*;

import java.time.Clock;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Profile("test")
@DisplayName("ThreadService unit tests:")
public class ThreadServiceImplUnitTest {

    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private ThreadRepository threadRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private Clock clock;
    @Mock
    private PaginationProperties paginationProperties;

    @InjectMocks
    private ThreadServiceImpl threadService;

    private User firstUser;
    private User secondUser;
    private Event event;
    private Optional<Event> eventOptional;
    private City cityWarsaw;
    private Thread thread;
    private Optional<Thread> threadOptional;

    @BeforeEach
    void setUp() {
        lenient().when(paginationProperties.getDefaultPageSize()).thenReturn(PaginationConstants.DEFAULT_PAGE_SIZE);
        lenient().when(clock.instant()).thenReturn(TimeConstants.NOW);
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

        thread = ThreadTestBuilder.firstThread().owner(firstUser).event(event).lastUpdate(TimeConstants.ONE_HOUR_AGO).build();
        threadOptional = Optional.of(thread);
    }

    @Nested
    @DisplayName("Create thread tests:")
    class ThreadCreateTests {

        private ThreadCreateDto threadCreateDto;

        @BeforeEach
        void setUp() {
            threadCreateDto = ThreadCreateDtoTestBuilder.firstThread().build();
        }

        private void setupSuccessfulThreadCreateMocks() {
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(threadRepository.save(any(Thread.class))).thenReturn(thread);
        }

        @Test
        @DisplayName("When creating thread should load event from database")
        public void whenCreatingThreadShouldLoadEventFromDatabase() {
            setupSuccessfulThreadCreateMocks();

            threadService.createThreadInEvent(threadCreateDto, EventConstants.FIRST_EVENT_ID);

            verify(eventRepository, times(1)).findById(EventConstants.FIRST_EVENT_ID);
        }

        @Test
        @DisplayName("When creating thread should throw EventNotFoundException if event with given id is not present")
        public void whenCreatingThreadShouldThrowEventNotFoundExceptionIfEventWithGivenIdIsNotPresent() {
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> threadService.createThreadInEvent(threadCreateDto, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(EventNotFoundException.class);

            verify(threadRepository, never()).save(any(Thread.class));
            verify(eventRepository, never()).save(any(Event.class));
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("When creating thread should get performing user from authentication service")
        public void whenCreatingThreadShouldGetPerformingUserFromAuthenticationService() {
            setupSuccessfulThreadCreateMocks();

            threadService.createThreadInEvent(threadCreateDto, EventConstants.FIRST_EVENT_ID);

            verify(authenticationService, times(1)).getCurrentUser();
        }

        @Test
        @DisplayName("When creating thread should throw NotAttenderException if user is not attending event")
        public void whenCreatingThreadShouldThrowNotAttenderExceptionIfUserIsNotAttendingEvent() {
            event.removeAttendingUser(secondUser);
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(secondUser);

            assertThatThrownBy(() -> threadService.createThreadInEvent(threadCreateDto, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(NotEventAttenderException.class);

            verify(threadRepository, never()).save(any(Thread.class));
            verify(eventRepository, never()).save(any(Event.class));
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("When creating thread should save new thread with correct data")
        public void whenCreatingThreadShouldSaveNewThreadWithCorrectData() {
            setupSuccessfulThreadCreateMocks();
            ArgumentCaptor<Thread> threadArgumentCaptor = ArgumentCaptor.forClass(Thread.class);

            threadService.createThreadInEvent(threadCreateDto, EventConstants.FIRST_EVENT_ID);

            verify(threadRepository, times(1)).save(threadArgumentCaptor.capture());
            Thread capturedThread = threadArgumentCaptor.getValue();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(capturedThread.getName()).isEqualTo(threadCreateDto.getName());
                softly.assertThat(capturedThread.getContent()).isEqualTo(threadCreateDto.getContent());
                softly.assertThat(capturedThread.getOwner()).isEqualTo(firstUser);
                softly.assertThat(capturedThread.getOwnerNameAtCreation()).isEqualTo(firstUser.getFullName());
                softly.assertThat(capturedThread.getEvent()).isEqualTo(event);
                softly.assertThat(capturedThread.getReplies()).isNotNull().isEmpty();
                softly.assertThat(capturedThread.getEditCount()).isZero();
                softly.assertThat(capturedThread.getCreateDate()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(capturedThread.getLastUpdate()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(capturedThread.getLastActivity()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(capturedThread.getCreateDate()).isEqualTo(capturedThread.getLastUpdate());
            });
        }

        @Test
        @DisplayName("When creating thread should add saved thread to event and save event")
        public void whenCreatingThreadShouldAddSavedThreadToEventAndSaveEvent() {
            setupSuccessfulThreadCreateMocks();

            threadService.createThreadInEvent(threadCreateDto, EventConstants.FIRST_EVENT_ID);

            assertThat(event.containsThread(thread)).isTrue();
            verify(eventRepository, times(1)).save(event);
        }

        @Test
        @DisplayName("When creating thread should add saved thread to user and save user")
        public void whenCreatingThreadShouldAddSavedThreadToUserAndSaveUser() {
            setupSuccessfulThreadCreateMocks();

            threadService.createThreadInEvent(threadCreateDto, EventConstants.FIRST_EVENT_ID);

            assertThat(firstUser.getThreads()).contains(thread);
            verify(userRepository, times(1)).save(firstUser);
        }

        @Test
        @DisplayName("When creating thread should return thread dto with correct data")
        public void whenCreatingThreadShouldReturnDtoWithCorrectData() {
            setupSuccessfulThreadCreateMocks();

            ThreadDto output = threadService.createThreadInEvent(threadCreateDto, EventConstants.FIRST_EVENT_ID);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(output.getId()).isEqualTo(ThreadConstants.FIRST_THREAD_ID);
                softly.assertThat(output.getName()).isEqualTo(ThreadConstants.FIRST_THREAD_NAME);
                softly.assertThat(output.getContent()).isEqualTo(ThreadConstants.FIRST_THREAD_CONTENT);
                softly.assertThat(output.getEditCounter()).isZero();
                softly.assertThat(output.getOwner().getId()).isEqualTo(UserConstants.FIRST_USER_ID);
            });
        }
    }


    @Nested
    @DisplayName("Update thread tests:")
    class ThreadUpdateTests {

        private ThreadCreateDto threadUpdateDto;

        @BeforeEach
        void setUp() {
            threadUpdateDto = ThreadCreateDtoTestBuilder.firstThreadUpdate().build();
        }

        private void setupSuccessfulThreadUpdateMocks() {
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(threadRepository.findByIdAndEventId(ThreadConstants.FIRST_THREAD_ID, EventConstants.FIRST_EVENT_ID)).thenReturn(threadOptional);
            when(threadRepository.save(thread)).thenReturn(thread);
        }

        @Test
        @DisplayName("When updating thread should find event by given id")
        public void whenUpdatingThreadShouldFindEventByGivenId() {
            setupSuccessfulThreadUpdateMocks();

            threadService.updateThreadInEvent(threadUpdateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID);

            verify(eventRepository, times(1)).findById(EventConstants.FIRST_EVENT_ID);
        }

        @Test
        @DisplayName("When updating thread should throw EventNotFoundException if there is no event with given id")
        public void whenUpdatingThreadShouldThrowEventNotFoundExceptionIfThereIsNoEventWithGivenId() {
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> threadService.updateThreadInEvent(threadUpdateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID))
                    .isInstanceOf(EventNotFoundException.class);

            verify(threadRepository, never()).save(any(Thread.class));
        }

        @Test
        @DisplayName("When updating thread should retrieve performing user from AuthenticationService")
        public void whenUpdatingThreadShouldRetrievePerformingUserFromAuthenticationService() {
            setupSuccessfulThreadUpdateMocks();

            threadService.updateThreadInEvent(threadUpdateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID);

            verify(authenticationService, times(1)).getCurrentUser();
        }

        @Test
        @DisplayName("When updating thread should load thread by given id")
        public void whenUpdatingThreadShouldLoadThreadByGivenId() {
            setupSuccessfulThreadUpdateMocks();

            threadService.updateThreadInEvent(threadUpdateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID);

            verify(threadRepository, times(1)).findByIdAndEventId(ThreadConstants.FIRST_THREAD_ID, EventConstants.FIRST_EVENT_ID);
        }

        @Test
        @DisplayName("When updating thread should throw ThreadNotFoundInEventException if there is no thread with given id")
        public void whenUpdatingThreadShouldThrowThreadNotFoundExceptionIfThereIsNoThreadWithGivenId() {
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(threadRepository.findByIdAndEventId(ThreadConstants.FIRST_THREAD_ID, EventConstants.FIRST_EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> threadService.updateThreadInEvent(threadUpdateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID))
                    .isInstanceOf(ThreadNotFoundInEventException.class);

            verify(threadRepository, never()).save(any(Thread.class));
        }

        @Test
        @DisplayName("When updating thread should throw NotAttenderException if performing user is not attending event")
        public void whenUpdatingThreadShouldThrowNotAttenderExceptionIfPerformingUserIsNotAttendingEvent() {
            event.removeAttendingUser(secondUser);
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(secondUser);
            when(threadRepository.findByIdAndEventId(ThreadConstants.FIRST_THREAD_ID, EventConstants.FIRST_EVENT_ID)).thenReturn(threadOptional);

            assertThatThrownBy(() -> threadService.updateThreadInEvent(threadUpdateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID))
                    .isInstanceOf(NotEventAttenderException.class);

            verify(threadRepository, never()).save(any(Thread.class));
        }

        @Test
        @DisplayName("When updating thread should throw NotThreadOwnerException if performing user does not own this thread")
        public void whenUpdatingThreadShouldThrowNotThreadOwnerExceptionIfPerformingUserDoesNotOwnThisThread() {
            thread.setOwner(secondUser);
            secondUser.addThread(thread);
            firstUser.removeThread(thread);

            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(threadRepository.findByIdAndEventId(ThreadConstants.FIRST_THREAD_ID, EventConstants.FIRST_EVENT_ID)).thenReturn(threadOptional);

            assertThatThrownBy(() -> threadService.updateThreadInEvent(threadUpdateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID))
                    .isInstanceOf(NotThreadOwnerException.class);

            verify(threadRepository, never()).save(any(Thread.class));
        }

        @Test
        @DisplayName("When updating thread should update name, content and editCounter of thread and save it")
        public void whenUpdatingThreadShouldUpdateNameContentAndEditCounterOfThread() {
            setupSuccessfulThreadUpdateMocks();

            int oldEditCounter = thread.getEditCount();

            threadService.updateThreadInEvent(threadUpdateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(thread.getName()).isEqualTo(threadUpdateDto.getName());
                softly.assertThat(thread.getContent()).isEqualTo(threadUpdateDto.getContent());
                softly.assertThat(thread.getEditCount()).isEqualTo(oldEditCounter + 1);
            });

            verify(threadRepository, times(1)).save(thread);
        }

        @Test
        @DisplayName("When updating thread should update lastUpdate field")
        public void whenUpdatingThreadShouldUpdateLastUpdateField() {
            setupSuccessfulThreadUpdateMocks();

            threadService.updateThreadInEvent(threadUpdateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID);

            assertThat(thread.getLastUpdate()).isEqualTo(TimeConstants.NOW);
        }

        @Test
        @DisplayName("When updating thread should return thread dto with correct data")
        public void whenUpdatingThreadShouldReturnDtoWithCorrectData() {
            setupSuccessfulThreadUpdateMocks();

            ThreadDto output = threadService.updateThreadInEvent(threadUpdateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(output.getId()).isEqualTo(ThreadConstants.FIRST_THREAD_ID);
                softly.assertThat(output.getName()).isEqualTo(threadUpdateDto.getName());
                softly.assertThat(output.getContent()).isEqualTo(threadUpdateDto.getContent());
                softly.assertThat(output.getEditCounter()).isEqualTo(1);
                softly.assertThat(output.getOwner().getId()).isEqualTo(UserConstants.FIRST_USER_ID);
            });
        }
    }

    @Nested
    @DisplayName("Get threads by event id")
    class GetThreadsByEventIdTests {

        private int pageNumber;
        private Page<Thread> pageZero;
        private Page<Thread> pageOne;
        private ThreadSortField threadSortField;
        private SortDirection sortDirection;

        @BeforeEach
        void setUp() {
            pageNumber = 0;
            threadSortField = ThreadSortField.LAST_ACTIVITY;
            sortDirection = SortDirection.DESC;
            PageRequest pageZeroRequest = preparePageRequest(PaginationConstants.PAGE_ZERO, threadSortField);
            PageRequest pageOneRequest = preparePageRequest(PaginationConstants.PAGE_ONE, threadSortField);

            pageZero = new PageImpl<>(
                    prepareThreadsForPage(PaginationConstants.TEN_ELEMENTS, firstUser),
                    pageZeroRequest,
                    PaginationConstants.TEN_ELEMENTS
            );
            pageOne = new PageImpl<>(
                    new ArrayList<>(),
                    pageOneRequest,
                    PaginationConstants.TEN_ELEMENTS
            );

        }

        private List<Thread> prepareThreadsForPage(int threadCount, User threadOwner) {
            List<Thread> threadList = IntStream.range(0, threadCount)
                    .mapToObj(threadNumber ->
                            ThreadTestBuilder.randomThread()
                                    .name(ThreadConstants.THREAD_NAME_FOR_COUNTER + threadNumber)
                                    .owner(threadOwner)
                                    .build()
                    )
                    .toList();

            threadOwner.getThreads().addAll(threadList);
            return threadList;
        }

        private PageRequest preparePageRequest(int pageNumber, ThreadSortField sortField) {
            return PageRequest.of(
                    pageNumber,
                    PaginationConstants.DEFAULT_PAGE_SIZE,
                    Sort.by(sortField.getSortField()).descending().and(Sort.by("id")).descending()
            );
        }

        private void setupSuccessfulMocks() {
            when(authenticationService.getCurrentUser()).thenReturn(secondUser);
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(threadRepository.findByEventId(any(UUID.class), any(Pageable.class))).thenReturn(pageZero);
        }

        @Test
        @DisplayName("When getting threads by event id should throw InvalidPageNumberException if page number is below zero")
        public void whenGettingThreadsByEventIdShouldThrowInvalidPageNumberExceptionIfPageNumberIsBelowZero() {
            pageNumber = -1;
            assertThatThrownBy(() -> threadService.getThreadsByEventId(EventConstants.FIRST_EVENT_ID, pageNumber, threadSortField, sortDirection))
                    .isInstanceOf(InvalidPageNumberException.class);

            verify(authenticationService, never()).getCurrentUser();
            verify(eventRepository, never()).findById(any(UUID.class));
            verify(threadRepository, never()).findByEventId(any(UUID.class), any(Pageable.class));
        }

        @Test
        @DisplayName("When getting threads by event id should load performing user via authentication service")
        public void whenGettingThreadsByEventIdShouldLoadPerformingUserViaAuthenticationService() {
            setupSuccessfulMocks();

            threadService.getThreadsByEventId(EventConstants.FIRST_EVENT_ID, pageNumber, threadSortField, sortDirection);

            verify(authenticationService, times(1)).getCurrentUser();
        }

        @Test
        @DisplayName("When getting threads by event id should throw EventNotFoundException if event with given id does not exist")
        public void whenGettingThreadsByEventIdShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> threadService.getThreadsByEventId(EventConstants.FIRST_EVENT_ID, pageNumber, threadSortField, sortDirection))
                    .isInstanceOf(EventNotFoundException.class);

            verify(threadRepository, never()).findByEventId(any(UUID.class), any(Pageable.class));
        }

        @Test
        @DisplayName("When getting threads by event id should throw NotEventAttenderException if performing user is not attending")
        public void whenGettingThreadsByEventIdShouldThrowNotEventAttenderExceptionIfPerformingUserIsNotAttending() {
            event.removeAttendingUser(secondUser);
            when(authenticationService.getCurrentUser()).thenReturn(secondUser);
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);

            assertThatThrownBy(() -> threadService.getThreadsByEventId(EventConstants.FIRST_EVENT_ID, pageNumber, threadSortField, sortDirection))
                    .isInstanceOf(NotEventAttenderException.class);
            verify(threadRepository, never()).findByEventId(any(UUID.class), any(Pageable.class));
        }

        @Test
        @DisplayName("When getting threads by event id should load threads with correct event id")
        public void whenGettingThreadsByEventIdShouldLoadThreadsWithCorrectEventId() {
            setupSuccessfulMocks();
            ArgumentCaptor<UUID> uuidArgumentCaptor = ArgumentCaptor.forClass(UUID.class);

            threadService.getThreadsByEventId(EventConstants.FIRST_EVENT_ID, pageNumber, threadSortField, sortDirection);
            verify(threadRepository, times(1)).findByEventId(uuidArgumentCaptor.capture(), any(Pageable.class));

            UUID capturedEventId = uuidArgumentCaptor.getValue();

            assertThat(capturedEventId).isEqualTo(EventConstants.FIRST_EVENT_ID);
        }

        @Test
        @DisplayName("When getting threads by event id should correctly map page request for descending direction")
        public void whenGettingThreadsByEventIdShouldCorrectlyMapPageRequestForDescendingDirection() {
            setupSuccessfulMocks();
            ArgumentCaptor<PageRequest> pageRequestArgumentCaptor = ArgumentCaptor.forClass(PageRequest.class);

            threadService.getThreadsByEventId(EventConstants.FIRST_EVENT_ID, pageNumber, threadSortField, sortDirection);

            verify(threadRepository, times(1)).findByEventId(any(UUID.class), pageRequestArgumentCaptor.capture());

            PageRequest capturedPageRequest = pageRequestArgumentCaptor.getValue();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(capturedPageRequest.getPageNumber()).isEqualTo(pageNumber);
                softly.assertThat(capturedPageRequest.getPageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(capturedPageRequest.getSort()).isEqualTo(Sort.by(threadSortField.getSortField()).descending().and(Sort.by("id").descending()));
            });
        }

        @Test
        @DisplayName("When getting threads by event id should correctly map page request for ascending direction")
        public void whenGettingThreadsByEventIdShouldCorrectlyMapPageRequestForAscendingDirection() {
            setupSuccessfulMocks();
            final String idSortParam = "id";
            sortDirection = SortDirection.ASC;
            ArgumentCaptor<PageRequest> pageRequestArgumentCaptor = ArgumentCaptor.forClass(PageRequest.class);

            threadService.getThreadsByEventId(EventConstants.FIRST_EVENT_ID, pageNumber, threadSortField, sortDirection);

            verify(threadRepository, times(1)).findByEventId(any(UUID.class), pageRequestArgumentCaptor.capture());

            PageRequest capturedPageRequest = pageRequestArgumentCaptor.getValue();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(capturedPageRequest.getPageNumber()).isEqualTo(pageNumber);
                softly.assertThat(capturedPageRequest.getPageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(capturedPageRequest.getSort()).isEqualTo(Sort.by(threadSortField.getSortField()).ascending().and(Sort.by(idSortParam).ascending()));
            });
        }

        @Test
        @DisplayName("When getting threads by event id should correctly map page data to dto")
        public void whenGettingThreadsByEventIdShouldCorrectlyMapPageDataToDto() {
            setupSuccessfulMocks();

            ThreadOverviewPageDto returnedDtoPage = threadService.getThreadsByEventId(EventConstants.FIRST_EVENT_ID, pageNumber, threadSortField, sortDirection);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(returnedDtoPage.pageNumber()).isEqualTo(pageZero.getNumber());
                softly.assertThat(returnedDtoPage.threads().size()).isEqualTo(pageZero.getContent().size());
                softly.assertThat(returnedDtoPage.totalElements()).isEqualTo(pageZero.getTotalElements());
                softly.assertThat(returnedDtoPage.totalPages()).isEqualTo(pageZero.getTotalPages());
                softly.assertThat(returnedDtoPage.lastPage()).isEqualTo(pageZero.isLast());
                softly.assertThat(returnedDtoPage.pageSize()).isEqualTo(pageZero.getSize());
            });
        }

        @Test
        @DisplayName("When getting threads by event id should return empty page if requested page is empty")
        public void whenGettingThreadsByEventIdShouldReturnEmptyPageIfRequestedPageIsEmpty() {
            pageNumber = 1;

            when(authenticationService.getCurrentUser()).thenReturn(secondUser);
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(threadRepository.findByEventId(any(UUID.class), any(Pageable.class))).thenReturn(pageOne);

            ThreadOverviewPageDto returnedDtoPage = threadService.getThreadsByEventId(EventConstants.FIRST_EVENT_ID, pageNumber, threadSortField, sortDirection);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(returnedDtoPage.pageNumber()).isEqualTo(pageOne.getNumber());
                softly.assertThat(returnedDtoPage.threads().size()).isEqualTo(pageOne.getContent().size());
                softly.assertThat(returnedDtoPage.totalElements()).isEqualTo(pageOne.getTotalElements());
                softly.assertThat(returnedDtoPage.totalPages()).isEqualTo(pageOne.getTotalPages());
                softly.assertThat(returnedDtoPage.lastPage()).isEqualTo(pageOne.isLast());
                softly.assertThat(returnedDtoPage.pageSize()).isEqualTo(pageOne.getSize());
            });
        }

        @Test
        @DisplayName("When getting threads by event id should correctly map thread to thread overview dto")
        public void whenGettingThreadsByEventIdShouldCorrectlyMapThreadToThreadOverviewDto() {
            int pageElementCount = 1;
            when(authenticationService.getCurrentUser()).thenReturn(secondUser);
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            Thread thread = ThreadTestBuilder.firstThread().event(event).owner(firstUser).build();
            PageRequest pageRequest = preparePageRequest(pageNumber, threadSortField);
            pageZero = new PageImpl<>(
                    List.of(thread),
                    pageRequest,
                    pageElementCount
            );
            when(threadRepository.findByEventId(EventConstants.FIRST_EVENT_ID, pageRequest)).thenReturn(pageZero);

            ThreadOverviewPageDto returnedDtoPage = threadService.getThreadsByEventId(EventConstants.FIRST_EVENT_ID, pageNumber, threadSortField, sortDirection);

            ThreadOverviewDto mappedThread = returnedDtoPage.threads().getFirst();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(returnedDtoPage.threads().size()).isEqualTo(pageElementCount);
                softly.assertThat(mappedThread.id()).isEqualTo(thread.getId());
                softly.assertThat(mappedThread.name()).isEqualTo(thread.getName());
                softly.assertThat(mappedThread.createDate()).isEqualTo(thread.getCreateDate());
                softly.assertThat(mappedThread.lastActivity()).isEqualTo(thread.getLastActivity());
                softly.assertThat(mappedThread.owner().getId()).isEqualTo(thread.getOwner().getId());
                softly.assertThat(mappedThread.replyCount()).isEqualTo(thread.getReplyCount());
                softly.assertThat(mappedThread.eventId()).isEqualTo(event.getId());
            });
        }

    }

    @Nested
    @DisplayName("Get thread in event tests:")
    class GetThreadInEventTests {

        private void setupSuccessfulOwnerAccessMocks() {
            when(eventRepository.existsById(EventConstants.FIRST_EVENT_ID)).thenReturn(true);
            when(authenticationService.getCurrentUserId()).thenReturn(UserConstants.FIRST_USER_ID);
            when(eventRepository.isUserAttenderOrOwner(UserConstants.FIRST_USER_ID, EventConstants.FIRST_EVENT_ID)).thenReturn(true);
            when(threadRepository.findByIdAndEventId(ThreadConstants.FIRST_THREAD_ID, EventConstants.FIRST_EVENT_ID)).thenReturn(threadOptional);
        }

        private void setupSuccessfulAttenderAccessMocks() {
            when(eventRepository.existsById(EventConstants.FIRST_EVENT_ID)).thenReturn(true);
            when(authenticationService.getCurrentUserId()).thenReturn(UserConstants.SECOND_USER_ID);
            when(eventRepository.isUserAttenderOrOwner(UserConstants.SECOND_USER_ID, EventConstants.FIRST_EVENT_ID)).thenReturn(true);
            when(threadRepository.findByIdAndEventId(ThreadConstants.FIRST_THREAD_ID, EventConstants.FIRST_EVENT_ID)).thenReturn(threadOptional);
        }

        @Test
        @DisplayName("When getting thread in event should throw EventNotFoundException if event with given id does not exist")
        public void whenGettingThreadInEventShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
            when(eventRepository.existsById(EventConstants.FIRST_EVENT_ID)).thenReturn(false);

            assertThatThrownBy(() -> threadService.getThreadInEvent(EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID))
                    .isInstanceOf(EventNotFoundException.class);

            verify(authenticationService, never()).getCurrentUserId();
            verify(eventRepository, never()).isUserAttenderOrOwner(any(UUID.class), any(UUID.class));
            verify(threadRepository, never()).findByIdAndEventId(any(UUID.class), any(UUID.class));
        }

        @Test
        @DisplayName("When getting thread in event should retrieve performing user id from authentication service")
        public void whenGettingThreadInEventShouldRetrievePerformingUserIdFromAuthenticationService() {
            setupSuccessfulOwnerAccessMocks();

            threadService.getThreadInEvent(EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID);

            verify(authenticationService, times(1)).getCurrentUserId();
        }

        @Test
        @DisplayName("When getting thread in event should check if user is event owner or attender")
        public void whenGettingThreadInEventShouldCheckIfUserIsEventOwnerOrAttender() {
            setupSuccessfulOwnerAccessMocks();

            threadService.getThreadInEvent(EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID);

            verify(eventRepository, times(1)).isUserAttenderOrOwner(UserConstants.FIRST_USER_ID, EventConstants.FIRST_EVENT_ID);
        }

        @Test
        @DisplayName("When getting thread in event should throw NotEventAttenderException if user is not attending event")
        public void whenGettingThreadInEventShouldThrowNotEventAttenderExceptionIfUserIsNotAttendingEvent() {
            when(eventRepository.existsById(EventConstants.FIRST_EVENT_ID)).thenReturn(true);
            when(authenticationService.getCurrentUserId()).thenReturn(UserConstants.SECOND_USER_ID);
            when(eventRepository.isUserAttenderOrOwner(UserConstants.SECOND_USER_ID, EventConstants.FIRST_EVENT_ID)).thenReturn(false);

            assertThatThrownBy(() -> threadService.getThreadInEvent(EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID))
                    .isInstanceOf(NotEventAttenderException.class);

            verify(threadRepository, never()).findByIdAndEventId(any(UUID.class), any(UUID.class));
        }

        @Test
        @DisplayName("When getting thread in event should load thread with correct ids")
        public void whenGettingThreadInEventShouldLoadThreadWithCorrectIds() {
            setupSuccessfulAttenderAccessMocks();

            threadService.getThreadInEvent(EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID);

            verify(threadRepository, times(1)).findByIdAndEventId(ThreadConstants.FIRST_THREAD_ID, EventConstants.FIRST_EVENT_ID);
        }

        @Test
        @DisplayName("When getting thread in event should throw ThreadNotFoundInEventException if thread does not exist")
        public void whenGettingThreadInEventShouldThrowThreadNotFoundInEventExceptionIfThreadDoesNotExist() {
            when(eventRepository.existsById(EventConstants.FIRST_EVENT_ID)).thenReturn(true);
            when(authenticationService.getCurrentUserId()).thenReturn(UserConstants.FIRST_USER_ID);
            when(eventRepository.isUserAttenderOrOwner(UserConstants.FIRST_USER_ID, EventConstants.FIRST_EVENT_ID)).thenReturn(true);
            when(threadRepository.findByIdAndEventId(ThreadConstants.FIRST_THREAD_ID, EventConstants.FIRST_EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> threadService.getThreadInEvent(EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID))
                    .isInstanceOf(ThreadNotFoundInEventException.class);
        }

        @Test
        @DisplayName("When getting thread in event should throw ThreadNotFoundInEventException if thread belongs to different event")
        public void whenGettingThreadInEventShouldThrowThreadNotFoundInEventExceptionIfThreadBelongsToDifferentEvent() {
            when(eventRepository.existsById(EventConstants.SECOND_EVENT_ID)).thenReturn(true);
            when(authenticationService.getCurrentUserId()).thenReturn(UserConstants.FIRST_USER_ID);
            when(eventRepository.isUserAttenderOrOwner(UserConstants.FIRST_USER_ID, EventConstants.SECOND_EVENT_ID)).thenReturn(true);
            when(threadRepository.findByIdAndEventId(ThreadConstants.FIRST_THREAD_ID, EventConstants.SECOND_EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> threadService.getThreadInEvent(EventConstants.SECOND_EVENT_ID, ThreadConstants.FIRST_THREAD_ID))
                    .isInstanceOf(ThreadNotFoundInEventException.class);
        }

        @Test
        @DisplayName("When getting thread in event should allow event attender who is not thread owner to load thread")
        public void whenGettingThreadInEventShouldAllowEventAttenderWhoIsNotThreadOwnerToLoadThread() {
            setupSuccessfulAttenderAccessMocks();

            ThreadDto output = threadService.getThreadInEvent(EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(output.getId()).isEqualTo(ThreadConstants.FIRST_THREAD_ID);
                softly.assertThat(output.getOwner().getId()).isEqualTo(UserConstants.FIRST_USER_ID);
            });
        }

        @Test
        @DisplayName("When getting thread in event should correctly map thread to dto and not save anything")
        public void whenGettingThreadInEventShouldCorrectlyMapThreadToDtoAndNotSaveAnything() {
            setupSuccessfulOwnerAccessMocks();

            ThreadDto output = threadService.getThreadInEvent(EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(output.getId()).isEqualTo(thread.getId());
                softly.assertThat(output.getEventId()).isEqualTo(event.getId());
                softly.assertThat(output.getOwner().getId()).isEqualTo(thread.getOwner().getId());
                softly.assertThat(output.getName()).isEqualTo(thread.getName());
                softly.assertThat(output.getContent()).isEqualTo(thread.getContent());
                softly.assertThat(output.getReplyCount()).isEqualTo(thread.getReplyCount());
                softly.assertThat(output.getCreateDate()).isEqualTo(thread.getCreateDate());
                softly.assertThat(output.getLastUpdate()).isEqualTo(thread.getLastUpdate());
                softly.assertThat(output.getEditCounter()).isEqualTo(thread.getEditCount());
            });

            verify(threadRepository, never()).save(any(Thread.class));
            verify(eventRepository, never()).save(any(Event.class));
            verify(userRepository, never()).save(any(User.class));
        }
    }
}
