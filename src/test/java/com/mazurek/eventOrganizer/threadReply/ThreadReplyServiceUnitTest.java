package com.mazurek.eventOrganizer.threadReply;

import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.event.EventRepository;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotEventAttenderException;
import com.mazurek.eventOrganizer.exception.thread.NotThreadReplyOwnerException;
import com.mazurek.eventOrganizer.exception.thread.ReplyNotFoundInThreadException;
import com.mazurek.eventOrganizer.exception.thread.ThreadNotFoundInEventException;
import com.mazurek.eventOrganizer.notification.NotificationService;
import com.mazurek.eventOrganizer.testData.builders.*;
import com.mazurek.eventOrganizer.testData.builders.dto.ThreadReplyCreateDtoTestBuilder;
import com.mazurek.eventOrganizer.thread.*;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.thread.dto.ThreadReplyCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadReplyDto;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Profile("test")
@DisplayName("ThreadReplyService unit tests:")
public class ThreadReplyServiceUnitTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ThreadRepository threadRepository;
    @Mock
    private ThreadReplyRepository threadReplyRepository;
    @Mock
    private Clock clock;

    @InjectMocks
    private ThreadReplyService threadReplyService;

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

        thread = ThreadTestBuilder.firstThread().owner(firstUser).event(event).build();
        threadOptional = Optional.of(thread);
    }

    @Nested
    @DisplayName("Create reply tests:")
    class CreateReplyTests {
        private ThreadReply threadReply;
        private ThreadReplyCreateDto threadReplyCreateDto;

        @BeforeEach
        void setUp() {
            threadReplyCreateDto = ThreadReplyCreateDtoTestBuilder.firstReply().build();
            threadReply = ThreadReplyTestBuilder.firstReply().thread(thread).replier(secondUser).build();
        }

        private void setupSuccessfulThreadReplyCreateMocks() {
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(secondUser);
            when(threadRepository.findByIdAndEventId(ThreadConstants.FIRST_THREAD_ID, EventConstants.FIRST_EVENT_ID)).thenReturn(threadOptional);
            when(threadReplyRepository.save(any(ThreadReply.class))).thenReturn(threadReply);
        }

        @Test
        @DisplayName("When creating reply in thread should load event with given id from database")
        public void whenCreatingReplyInThreadShouldLoadEventWithGivenIdFromDatabase() {
            setupSuccessfulThreadReplyCreateMocks();

            threadReplyService.createReplyInThread(threadReplyCreateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID);

            verify(eventRepository, times(1)).findById(EventConstants.FIRST_EVENT_ID);
        }

        @Test
        @DisplayName("When creating reply in thread should throw EventNotFoundException if there is no event with given id")
        public void whenCreatingReplyInThreadShouldThrowEventNotFoundExceptionIfThereIsNoEventWithGivenId() {
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> threadReplyService.createReplyInThread(threadReplyCreateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID))
                    .isInstanceOf(EventNotFoundException.class);

            verify(threadReplyRepository, never()).save(any(ThreadReply.class));
            verify(threadRepository, never()).save(any(Thread.class));
        }

        @Test
        @DisplayName("When creating reply in thread should retrieve performing user from AuthenticationService")
        public void whenCreatingReplyInThreadShouldRetrievePerformingUserFromAuthenticationService() {
            setupSuccessfulThreadReplyCreateMocks();

            threadReplyService.createReplyInThread(threadReplyCreateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID);

            verify(authenticationService, times(1)).getCurrentUser();
        }

        @Test
        @DisplayName("When creating reply in thread should throw NotAttenderException if user is not attending event")
        public void whenCreatingReplyInThreadShouldThrowNotAttenderExceptionIfUserIsNotAttendingEvent() {
            event.removeAttendingUser(secondUser);
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(secondUser);

            assertThatThrownBy(() -> threadReplyService.createReplyInThread(threadReplyCreateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID))
                    .isInstanceOf(NotEventAttenderException.class);

            verify(threadReplyRepository, never()).save(any(ThreadReply.class));
            verify(threadRepository, never()).save(any(Thread.class));
        }

        @Test
        @DisplayName("When creating reply in thread should load thread with given id")
        public void whenCreatingReplyInThreadShouldLoadThreadWithGivenId() {
            setupSuccessfulThreadReplyCreateMocks();

            threadReplyService.createReplyInThread(threadReplyCreateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID);

            verify(threadRepository, times(1)).findByIdAndEventId(ThreadConstants.FIRST_THREAD_ID, EventConstants.FIRST_EVENT_ID);
        }

        @Test
        @DisplayName("When creating reply in thread should throw ThreadNotFoundException if there is no thread with given id in event with given id")
        public void whenCreatingReplyInThreadShouldThrowThreadNotFoundExceptionIfThereIsNoThreadWithGivenIdInEventWithGivenId() {
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(secondUser);
            when(threadRepository.findByIdAndEventId(ThreadConstants.FIRST_THREAD_ID, EventConstants.FIRST_EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> threadReplyService.createReplyInThread(threadReplyCreateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID))
                    .isInstanceOf(ThreadNotFoundInEventException.class);

            verify(threadReplyRepository, never()).save(any(ThreadReply.class));
            verify(threadRepository, never()).save(any(Thread.class));
        }

        @Test
        @DisplayName("When creating reply in thread should set up new ThreadReply object and save it with all necessary fields")
        public void whenCreatingReplyInThreadShouldSetUpNewThreadReplyObjectAndSaveItWithAllNecessaryFields() {
            setupSuccessfulThreadReplyCreateMocks();
            ArgumentCaptor<ThreadReply> threadReplyArgumentCaptor = ArgumentCaptor.forClass(ThreadReply.class);

            threadReplyService.createReplyInThread(threadReplyCreateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID);

            verify(threadReplyRepository, times(1)).save(threadReplyArgumentCaptor.capture());
            ThreadReply capturedReply = threadReplyArgumentCaptor.getValue();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(capturedReply.getContent()).isEqualTo(threadReplyCreateDto.getReplyContent());
                softly.assertThat(capturedReply.getThread().getId()).isEqualTo(ThreadConstants.FIRST_THREAD_ID);
                softly.assertThat(capturedReply.getReplier()).isEqualTo(secondUser);
                softly.assertThat(capturedReply.getEditCounter()).isZero();
                softly.assertThat(capturedReply.getReplyDate()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(capturedReply.getLastUpdate()).isEqualTo(TimeConstants.NOW);
                softly.assertThat(capturedReply.getReplyDate()).isEqualTo(capturedReply.getLastUpdate());
            });
        }

        @Test
        @DisplayName("When creating reply in thread should save updated User with new thread reply")
        public void whenCreatingReplyInThreadShouldSaveUpdatedUserWithNewThreadReply() {
            setupSuccessfulThreadReplyCreateMocks();
            ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);

            threadReplyService.createReplyInThread(threadReplyCreateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID);

            verify(userRepository, times(1)).save(userArgumentCaptor.capture());
            User capturedUser = userArgumentCaptor.getValue();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(capturedUser.getThreadReplies()).hasSize(1);
                softly.assertThat(capturedUser.getThreadReplies()).containsExactly(threadReply);
            });
        }

        @Test
        @DisplayName("When creating reply in thread should save updated thread with new thread reply")
        public void whenCreatingReplyInThreadShouldSaveUpdatedThreadWithNewThreadReply() {
            setupSuccessfulThreadReplyCreateMocks();
            ArgumentCaptor<Thread> threadArgumentCaptor = ArgumentCaptor.forClass(Thread.class);

            threadReplyService.createReplyInThread(threadReplyCreateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID);

            verify(threadRepository, times(1)).save(threadArgumentCaptor.capture());
            Thread capturedThread = threadArgumentCaptor.getValue();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(capturedThread.getReplies()).hasSize(1);
                softly.assertThat(capturedThread.getReplies()).containsExactly(threadReply);
            });
        }

        @Test
        @DisplayName("When creating reply in thread should notify thread owner about a new reply in the thread")
        public void whenCreatingReplyInThreadShouldNotifyThreadOwnerAboutNewReplyInHisThread() {
            setupSuccessfulThreadReplyCreateMocks();

            threadReplyService.createReplyInThread(threadReplyCreateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID);

            verify(notificationService, times(1)).notifyThreadOwner(thread, secondUser.getFullName());
        }

        @Test
        @DisplayName("When creating reply in thread should not send notification if thread owner is replying")
        public void whenCreatingReplyInThreadShouldNotSendNotificationIfThreadOwnerIsReplying() {
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(threadRepository.findByIdAndEventId(ThreadConstants.FIRST_THREAD_ID, EventConstants.FIRST_EVENT_ID)).thenReturn(threadOptional);
            when(threadReplyRepository.save(any(ThreadReply.class))).thenReturn(threadReply);

            threadReplyService.createReplyInThread(threadReplyCreateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID);

            verify(notificationService, never()).notifyThreadOwner(any(Thread.class), any(String.class));
        }

        @Test
        @DisplayName("When creating reply in thread should return reply dto with correct data")
        public void whenCreatingReplyInThreadShouldReturnDtoWithCorrectData() {
            setupSuccessfulThreadReplyCreateMocks();

            ThreadReplyDto output = threadReplyService.createReplyInThread(threadReplyCreateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(output.getId()).isEqualTo(ThreadReplyConstants.FIRST_REPLY_ID);
                softly.assertThat(output.getContent()).isEqualTo(ThreadReplyConstants.FIRST_REPLY_CONTENT);
                softly.assertThat(output.getEditCounter()).isZero();
                softly.assertThat(output.getReplyDate()).isEqualTo(output.getLastUpdate());
                softly.assertThat(output.getThreadId()).isEqualTo(ThreadConstants.FIRST_THREAD_ID);
                softly.assertThat(output.getReplier().getId()).isEqualTo(UserConstants.SECOND_USER_ID);
            });
        }
    }


    @Nested
    @DisplayName("Update reply tests:")
    class UpdateReplyTests {
        private ThreadReplyCreateDto threadReplyUpdateDto;
        private ThreadReply threadReply;
        private Optional<ThreadReply> threadReplyOptional;

        @BeforeEach
        void setUp() {
            threadReply = ThreadReplyTestBuilder.firstReply().thread(thread).replier(secondUser).build();
            threadReplyOptional = Optional.of(threadReply);
            threadReplyUpdateDto = ThreadReplyCreateDtoTestBuilder.firstReplyUpdate().build();
        }

        private void setupSuccessfulThreadReplyUpdateMocks() {
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(secondUser);
            when(threadRepository.existsByIdAndEventId(ThreadConstants.FIRST_THREAD_ID, EventConstants.FIRST_EVENT_ID)).thenReturn(ThreadConstants.THREAD_EXISTS_IN_EVENT);
            when(threadReplyRepository.findByIdAndThreadId(ThreadReplyConstants.FIRST_REPLY_ID, ThreadConstants.FIRST_THREAD_ID)).thenReturn(threadReplyOptional);
            when(threadReplyRepository.save(threadReply)).thenReturn(threadReply);
        }

        @Test
        @DisplayName("When updating reply in thread should load event with given id from database")
        public void whenUpdatingReplyInThreadShouldLoadEventWithGivenIdFromDatabase() {
            setupSuccessfulThreadReplyUpdateMocks();

            threadReplyService.updateThreadReplyInEventThread(threadReplyUpdateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID, ThreadReplyConstants.FIRST_REPLY_ID);

            verify(eventRepository, times(1).description("Expected to look for event in database only once.")).findById(EventConstants.FIRST_EVENT_ID);
        }

        @Test
        @DisplayName("When updating reply in thread should throw EventNotFoundException if there is no event with given id")
        public void whenUpdatingReplyInThreadShouldThrowEventNotFoundExceptionIfThereIsNoEventWithGivenId() {
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> threadReplyService.updateThreadReplyInEventThread(threadReplyUpdateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID, ThreadReplyConstants.FIRST_REPLY_ID))
                    .isInstanceOf(EventNotFoundException.class);

            verify(threadReplyRepository, never()).save(any(ThreadReply.class));
        }

        @Test
        @DisplayName("When updating reply in thread should throw NotAttenderException if user is not attending event")
        public void whenUpdatingReplyInThreadShouldThrowNotAttenderExceptionIfUserIsNotAttendingEvent() {
            event.removeAttendingUser(secondUser);
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(secondUser);

            assertThatThrownBy(() -> threadReplyService.updateThreadReplyInEventThread(threadReplyUpdateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID, ThreadReplyConstants.FIRST_REPLY_ID))
                    .isInstanceOf(NotEventAttenderException.class);

            verify(threadReplyRepository, never()).save(any(ThreadReply.class));
        }

        @Test
        @DisplayName("When updating reply in thread should check if thread and event by given ids are related")
        public void whenUpdatingReplyInThreadShouldCheckIfThreadAndEventByGivenIdsAreRelated() {
            setupSuccessfulThreadReplyUpdateMocks();

            threadReplyService.updateThreadReplyInEventThread(threadReplyUpdateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID, ThreadReplyConstants.FIRST_REPLY_ID);

            verify(threadRepository, times(1)).existsByIdAndEventId(ThreadConstants.FIRST_THREAD_ID, EventConstants.FIRST_EVENT_ID);
        }

        @Test
        @DisplayName("When updating reply in thread should throw ThreadNotFoundInEventException if there is no thread with that id or it's not related with event with given id")
        public void whenUpdatingReplyInThreadShouldThrowThreadNotFoundInEventExceptionIfThereIsNoThreadWithThatIdOrItsNotRelatedWithEventWithGivenId() {
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(secondUser);
            when(threadRepository.existsByIdAndEventId(ThreadConstants.FIRST_THREAD_ID, EventConstants.FIRST_EVENT_ID)).thenReturn(ThreadConstants.THREAD_DOES_NOT_EXIST_IN_EVENT);

            assertThatThrownBy(() -> threadReplyService.updateThreadReplyInEventThread(threadReplyUpdateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID, ThreadReplyConstants.FIRST_REPLY_ID))
                    .isInstanceOf(ThreadNotFoundInEventException.class);

            verify(threadReplyRepository, never()).save(any(ThreadReply.class));
        }

        @Test
        @DisplayName("When updating reply in thread should look up for thread reply with given id")
        public void whenUpdatingReplyInThreadShouldLookUpForThreadReplyWithGivenId() {
            setupSuccessfulThreadReplyUpdateMocks();

            threadReplyService.updateThreadReplyInEventThread(threadReplyUpdateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID, ThreadReplyConstants.FIRST_REPLY_ID);

            verify(threadReplyRepository, times(1)).findByIdAndThreadId(ThreadReplyConstants.FIRST_REPLY_ID, ThreadConstants.FIRST_THREAD_ID);
        }

        @Test
        @DisplayName("When updating reply in thread should throw ReplyNotFoundInThreadException if reply with given id does not exist in the thread")
        public void whenUpdatingReplyInThreadShouldThrowWrongThreadExceptionIfThreadReplyWithGivenIdAndThreadWithGivenIdAreNotRelatedOrItNotExists() {
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(secondUser);
            when(threadRepository.existsByIdAndEventId(ThreadConstants.FIRST_THREAD_ID, EventConstants.FIRST_EVENT_ID)).thenReturn(ThreadConstants.THREAD_EXISTS_IN_EVENT);
            when(threadReplyRepository.findByIdAndThreadId(ThreadReplyConstants.FIRST_REPLY_ID, ThreadConstants.FIRST_THREAD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> threadReplyService.updateThreadReplyInEventThread(threadReplyUpdateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID, ThreadReplyConstants.FIRST_REPLY_ID))
                    .isInstanceOf(ReplyNotFoundInThreadException.class);

            verify(threadReplyRepository, never()).save(any(ThreadReply.class));
        }

        @Test
        @DisplayName("When updating reply in thread should throw NotThreadReplyOwnerException if user tries to update a reply they do not own")
        public void whenUpdatingReplyInThreadShouldThrowNotThreadReplyOwnerExceptionIfUserTryToUpdateNotHisReply() {
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(threadRepository.existsByIdAndEventId(ThreadConstants.FIRST_THREAD_ID, EventConstants.FIRST_EVENT_ID)).thenReturn(ThreadConstants.THREAD_EXISTS_IN_EVENT);
            when(threadReplyRepository.findByIdAndThreadId(ThreadReplyConstants.FIRST_REPLY_ID, ThreadConstants.FIRST_THREAD_ID)).thenReturn(threadReplyOptional);

            assertThatThrownBy(() -> threadReplyService.updateThreadReplyInEventThread(threadReplyUpdateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID, ThreadReplyConstants.FIRST_REPLY_ID))
                    .isInstanceOf(NotThreadReplyOwnerException.class);

            verify(threadReplyRepository, never()).save(any(ThreadReply.class));
        }

        @Test
        @DisplayName("When updating reply in thread should update fields in stored thread reply and save it")
        public void whenUpdatingReplyInThreadShouldUpdateFieldsInStoredThreadReply() {
            setupSuccessfulThreadReplyUpdateMocks();

            int oldEditCounter = threadReply.getEditCounter();
            ArgumentCaptor<ThreadReply> threadReplyArgumentCaptor = ArgumentCaptor.forClass(ThreadReply.class);

            threadReplyService.updateThreadReplyInEventThread(threadReplyUpdateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID, ThreadReplyConstants.FIRST_REPLY_ID);

            verify(threadReplyRepository, times(1)).save(threadReplyArgumentCaptor.capture());

            ThreadReply capturedThreadReply = threadReplyArgumentCaptor.getValue();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(capturedThreadReply.getEditCounter()).isGreaterThan(oldEditCounter);
                softly.assertThat(capturedThreadReply.getEditCounter()).isEqualTo(oldEditCounter+1);
                softly.assertThat(capturedThreadReply.getContent()).isEqualTo(threadReplyUpdateDto.getReplyContent());
                softly.assertThat(capturedThreadReply.getLastUpdate()).isEqualTo(TimeConstants.NOW);
            });
        }


        @Test
        @DisplayName("When updating reply in thread should return reply dto with correct data")
        public void whenUpdatingReplyInThreadShouldReturnDtoWithCorrectData() {
            setupSuccessfulThreadReplyUpdateMocks();

            ThreadReplyDto output = threadReplyService.updateThreadReplyInEventThread(threadReplyUpdateDto, EventConstants.FIRST_EVENT_ID, ThreadConstants.FIRST_THREAD_ID, ThreadReplyConstants.FIRST_REPLY_ID);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(output.getId()).isEqualTo(ThreadReplyConstants.FIRST_REPLY_ID);
                softly.assertThat(output.getContent()).isEqualTo(ThreadReplyConstants.FIRST_REPLY_UPDATE_CONTENT);
                softly.assertThat(output.getEditCounter()).isEqualTo(1);
                softly.assertThat(output.getReplyDate()).isBefore(output.getLastUpdate());
                softly.assertThat(output.getThreadId()).isEqualTo(ThreadConstants.FIRST_THREAD_ID);
                softly.assertThat(output.getReplier().getId()).isEqualTo(UserConstants.SECOND_USER_ID);
            });
        }
    }
}
