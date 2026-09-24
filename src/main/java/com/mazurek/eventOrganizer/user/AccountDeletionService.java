package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipantRepository;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.event.EventRepository;
import com.mazurek.eventOrganizer.exception.user.InvalidPasswordException;
import com.mazurek.eventOrganizer.file.FileRepository;
import com.mazurek.eventOrganizer.notification.repository.NotificationRepository;
import com.mazurek.eventOrganizer.thread.ThreadRepository;
import com.mazurek.eventOrganizer.threadReply.ThreadReplyRepository;
import com.mazurek.eventOrganizer.user.dto.DeleteCurrentUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private final AuthenticationService authenticationService;
    private final PasswordEncoder passwordEncoder;
    private final EventRepository eventRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final NotificationRepository notificationRepository;
    private final FileRepository fileRepository;
    private final ThreadRepository threadRepository;
    private final ThreadReplyRepository threadReplyRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional
    public void deleteCurrentUser(DeleteCurrentUserDto request) {
        User user = authenticationService.getCurrentUser();
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        UUID userId = user.getId();
        Instant deletedAt = clock.instant();

        removeEventAttendance(user);
        removeOwnedUpcomingEventsAndDetachPastEvents(userId, deletedAt);
        anonymizeNonChatAuthorSnapshots(userId);
        conversationParticipantRepository.markGroupParticipantsLeftByUserId(userId, deletedAt);

        eventRepository.flush();
        conversationParticipantRepository.flush();
        userRepository.delete(user);
        userRepository.flush();
    }

    private void removeEventAttendance(User user) {
        eventRepository.findAllAttendedByUserIdForUpdate(user.getId())
                .forEach(event -> event.removeAttendingUser(user));
    }

    private void removeOwnedUpcomingEventsAndDetachPastEvents(UUID userId, Instant deletedAt) {
        List<Event> ownedEvents = eventRepository.findAllOwnedByUserIdForUpdate(userId);
        List<Event> upcomingEvents = ownedEvents.stream()
                .filter(event -> !event.hadPlace(deletedAt))
                .toList();

        Set<UUID> upcomingEventIds = upcomingEvents.stream()
                .map(Event::getId)
                .collect(Collectors.toSet());
        if (!upcomingEventIds.isEmpty()) {
            notificationRepository.deleteAllReferencingEvents(upcomingEventIds);
        }

        ownedEvents.stream()
                .filter(event -> event.hadPlace(deletedAt))
                .forEach(event -> event.setOwner(null));
        eventRepository.deleteAll(upcomingEvents);
    }

    private void anonymizeNonChatAuthorSnapshots(UUID userId) {
        fileRepository.anonymizeOwnerSnapshotsByUserId(userId);
        threadRepository.anonymizeOwnerSnapshotsByUserId(userId);
        threadReplyRepository.anonymizeReplierSnapshotsByUserId(userId);
    }
}
