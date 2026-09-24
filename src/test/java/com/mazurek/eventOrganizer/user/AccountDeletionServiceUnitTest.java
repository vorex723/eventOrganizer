package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipantRepository;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.event.EventRepository;
import com.mazurek.eventOrganizer.exception.user.InvalidPasswordException;
import com.mazurek.eventOrganizer.file.FileRepository;
import com.mazurek.eventOrganizer.notification.repository.NotificationRepository;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.thread.ThreadRepository;
import com.mazurek.eventOrganizer.threadReply.ThreadReplyRepository;
import com.mazurek.eventOrganizer.user.dto.DeleteCurrentUserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.util.List;
import java.util.Set;

import static com.mazurek.eventOrganizer.testData.TestConstants.EventConstants.FIRST_EVENT_ID;
import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants.NOW;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.USER_PASSWORD;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.WRONG_USER_PASSWORD;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountDeletionService unit tests:")
class AccountDeletionServiceUnitTest {

    @Mock private AuthenticationService authenticationService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EventRepository eventRepository;
    @Mock private ConversationParticipantRepository conversationParticipantRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private FileRepository fileRepository;
    @Mock private ThreadRepository threadRepository;
    @Mock private ThreadReplyRepository threadReplyRepository;
    @Mock private UserRepository userRepository;
    @Mock private Clock clock;
    @Mock private Event attendedEvent;
    @Mock private Event pastOwnedEvent;
    @Mock private Event upcomingOwnedEvent;

    private AccountDeletionService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new AccountDeletionService(
                authenticationService,
                passwordEncoder,
                eventRepository,
                conversationParticipantRepository,
                notificationRepository,
                fileRepository,
                threadRepository,
                threadReplyRepository,
                userRepository,
                clock
        );
        user = UserTestBuilder.firstUser().build();
    }

    @Test
    @DisplayName("Wrong password should reject deletion without changing account data")
    void wrongPasswordShouldRejectDeletionWithoutChangingAccountData() {
        when(authenticationService.getCurrentUser()).thenReturn(user);
        when(passwordEncoder.matches(WRONG_USER_PASSWORD, user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> service.deleteCurrentUser(new DeleteCurrentUserDto(WRONG_USER_PASSWORD)))
                .isInstanceOf(InvalidPasswordException.class);

        verifyNoInteractions(
                eventRepository,
                conversationParticipantRepository,
                notificationRepository,
                fileRepository,
                threadRepository,
                threadReplyRepository,
                userRepository,
                clock
        );
    }

    @Test
    @DisplayName("Deletion should preserve past events and remove upcoming account relationships")
    void deletionShouldPreservePastEventsAndRemoveUpcomingAccountRelationships() {
        when(authenticationService.getCurrentUser()).thenReturn(user);
        when(passwordEncoder.matches(USER_PASSWORD, user.getPassword())).thenReturn(true);
        when(clock.instant()).thenReturn(NOW);
        when(eventRepository.findAllAttendedByUserIdForUpdate(user.getId())).thenReturn(List.of(attendedEvent));
        when(eventRepository.findAllOwnedByUserIdForUpdate(user.getId()))
                .thenReturn(List.of(pastOwnedEvent, upcomingOwnedEvent));
        when(pastOwnedEvent.hadPlace(NOW)).thenReturn(true);
        when(upcomingOwnedEvent.hadPlace(NOW)).thenReturn(false);
        when(upcomingOwnedEvent.getId()).thenReturn(FIRST_EVENT_ID);

        service.deleteCurrentUser(new DeleteCurrentUserDto(USER_PASSWORD));

        verify(attendedEvent).removeAttendingUser(user);
        verify(pastOwnedEvent).setOwner(null);
        verify(upcomingOwnedEvent, never()).setOwner(null);
        verify(notificationRepository).deleteAllReferencingEvents(Set.of(FIRST_EVENT_ID));
        verify(eventRepository).deleteAll(List.of(upcomingOwnedEvent));
        verify(fileRepository).anonymizeOwnerSnapshotsByUserId(user.getId());
        verify(threadRepository).anonymizeOwnerSnapshotsByUserId(user.getId());
        verify(threadReplyRepository).anonymizeReplierSnapshotsByUserId(user.getId());
        verify(conversationParticipantRepository).markGroupParticipantsLeftByUserId(user.getId(), NOW);

        InOrder persistenceOrder = inOrder(eventRepository, conversationParticipantRepository, userRepository);
        persistenceOrder.verify(eventRepository).flush();
        persistenceOrder.verify(conversationParticipantRepository).flush();
        persistenceOrder.verify(userRepository).delete(user);
        persistenceOrder.verify(userRepository).flush();
    }

    @Test
    @DisplayName("Deletion without upcoming events should not execute an empty notification cleanup query")
    void deletionWithoutUpcomingEventsShouldSkipNotificationCleanup() {
        when(authenticationService.getCurrentUser()).thenReturn(user);
        when(passwordEncoder.matches(USER_PASSWORD, user.getPassword())).thenReturn(true);
        when(clock.instant()).thenReturn(NOW);
        when(eventRepository.findAllAttendedByUserIdForUpdate(user.getId())).thenReturn(List.of());
        when(eventRepository.findAllOwnedByUserIdForUpdate(user.getId())).thenReturn(List.of(pastOwnedEvent));
        when(pastOwnedEvent.hadPlace(NOW)).thenReturn(true);

        service.deleteCurrentUser(new DeleteCurrentUserDto(USER_PASSWORD));

        verifyNoInteractions(notificationRepository);
        verify(eventRepository).deleteAll(List.of());
        verify(userRepository).delete(user);
    }
}
