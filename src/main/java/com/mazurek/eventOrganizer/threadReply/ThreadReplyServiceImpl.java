package com.mazurek.eventOrganizer.threadReply;


import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.config.properties.PaginationProperties;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.event.EventRepository;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotEventAttenderException;
import com.mazurek.eventOrganizer.exception.thread.NotThreadReplyOwnerException;
import com.mazurek.eventOrganizer.exception.thread.ReplyNotFoundInThreadException;
import com.mazurek.eventOrganizer.exception.thread.ThreadNotFoundInEventException;
import com.mazurek.eventOrganizer.notification.service.NotificationCommandService;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.thread.ThreadRepository;
import com.mazurek.eventOrganizer.threadReply.dto.ThreadReplyCreateDto;
import com.mazurek.eventOrganizer.threadReply.dto.ThreadReplyDto;
import com.mazurek.eventOrganizer.threadReply.dto.ThreadReplyPageDto;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ThreadReplyServiceImpl implements ThreadReplyService {
    private final AuthenticationService authenticationService;
    private final NotificationCommandService notificationCommandService;
    private final ThreadRepository threadRepository;
    private final ThreadReplyRepository threadReplyRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final PaginationProperties paginationProperties;

    @Transactional
    public ThreadReplyDto createReplyInThread(ThreadReplyCreateDto threadReplyCreateDto, UUID eventId, UUID threadId) {
        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);

        User replyingUser = authenticationService.getCurrentUser();

        if(!event.isUserAttending(replyingUser))
            throw new NotEventAttenderException();

        Thread thread = threadRepository.findByIdAndEventId(threadId,eventId).orElseThrow(ThreadNotFoundInEventException::new);

        Instant createDateTime = clock.instant();

        ThreadReply savedThreadReply = threadReplyRepository.save(
                ThreadReply.builder()
                        .content(threadReplyCreateDto.getReplyContent())
                        .thread(thread)
                        .replier(replyingUser)
                        .replyDate(createDateTime)
                        .lastUpdate(createDateTime)
                        .build());

        replyingUser.addThreadReply(savedThreadReply);
        thread.addReplyToThread(savedThreadReply);
        thread.setLastActivity(createDateTime);
        thread.incrementReplyCounter();

        userRepository.save(replyingUser);
        threadRepository.save(thread);

        User threadOwner = thread.getOwner();

        if (threadOwner != null && !replyingUser.equals(threadOwner)) {
            notificationCommandService.notifyThreadReply(
                    eventId,
                    threadId,
                    threadOwner.getId(),
                    replyingUser.getFullName()
            );
        }

        return new ThreadReplyDto(savedThreadReply);
    }

    @Transactional
    public ThreadReplyDto updateThreadReplyInEventThread(ThreadReplyCreateDto threadReplyUpdateDto, UUID eventId, UUID threadId, UUID threadReplyId){
        Event event = eventRepository.findById(eventId).orElseThrow(EventNotFoundException::new);

        User replyingUser = authenticationService.getCurrentUser();

        if(!event.isUserAttending(replyingUser))
            throw new NotEventAttenderException();

        if (!threadRepository.existsByIdAndEventId(threadId,eventId))
            throw new ThreadNotFoundInEventException();

        ThreadReply threadReply = threadReplyRepository.findByIdAndThreadId(threadReplyId, threadId).orElseThrow(ReplyNotFoundInThreadException::new);

        if (!threadReply.isReplier(replyingUser))
            throw new NotThreadReplyOwnerException();

        threadReply.setContent(threadReplyUpdateDto.getReplyContent());
        threadReply.incrementEditCounter();
        threadReply.setLastUpdate(clock.instant());

        return new ThreadReplyDto(threadReplyRepository.save(threadReply));
    }

    @Transactional

    @Override
    public ThreadReplyPageDto getRepliesInEventThread(UUID eventId, UUID threadId, int pageNumber) {
        if (pageNumber < 0)
            throw new InvalidPageNumberException();

        UUID userId = authenticationService.getCurrentUserId();

        if (!eventRepository.existsById(eventId))
            throw new EventNotFoundException();
        if (!eventRepository.isUserAttenderOrOwner(userId, eventId))
            throw new NotEventAttenderException();
        if (!threadRepository.existsByIdAndEventId(threadId, eventId))
            throw new ThreadNotFoundInEventException();

        PageRequest pageRequest = PageRequest.of(
                pageNumber,
                paginationProperties.getDefaultPageSize(),
                Sort.by("replyDate").ascending()
                        .and(Sort.by("id").descending())
        );

        Page<ThreadReply> threadReplyPage = threadReplyRepository.findByThreadId(threadId, pageRequest);

        return new ThreadReplyPageDto(threadReplyPage);
    }
}
