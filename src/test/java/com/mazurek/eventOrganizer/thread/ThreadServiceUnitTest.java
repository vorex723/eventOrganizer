package com.mazurek.eventOrganizer.thread;

import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.event.EventRepository;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotEventAttenderException;
import com.mazurek.eventOrganizer.exception.thread.NotThreadOwnerException;
import com.mazurek.eventOrganizer.exception.thread.ThreadNotFoundInEventException;
import com.mazurek.eventOrganizer.testData.builders.*;
import com.mazurek.eventOrganizer.testData.builders.dto.ThreadCreateDtoTestBuilder;
import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadDto;
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

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Profile("test")
@DisplayName("ThreadService unit tests:")
public class ThreadServiceUnitTest {

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

    @InjectMocks
    private ThreadService threadService;

    private User firstUser;
    private User secondUser;
    private Event event;
    private Optional<Event> eventOptional;
    private City cityWarsaw;
    private Thread thread;
    private Optional<Thread> threadOptional;

    @BeforeEach
    void setUp() {
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
                softly.assertThat(capturedThread.getEvent()).isEqualTo(event);
                softly.assertThat(capturedThread.getReplies()).isNotNull().isEmpty();
                softly.assertThat(capturedThread.getEditCounter()).isZero();
                softly.assertThat(capturedThread.getCreateDate()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(capturedThread.getLastUpdate()).isEqualTo(TimeConstants.NOW);
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

            int oldEditCounter = thread.getEditCounter();

            threadService.updateThreadInEvent(threadUpdateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(thread.getName()).isEqualTo(threadUpdateDto.getName());
                softly.assertThat(thread.getContent()).isEqualTo(threadUpdateDto.getContent());
                softly.assertThat(thread.getEditCounter()).isEqualTo(oldEditCounter + 1);
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
}
