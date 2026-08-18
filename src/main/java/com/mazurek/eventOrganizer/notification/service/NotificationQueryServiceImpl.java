package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.config.properties.PaginationProperties;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import com.mazurek.eventOrganizer.exception.notification.NotificationNotFoundException;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.dto.NotificationPageDto;
import com.mazurek.eventOrganizer.notification.dto.NotificationUnreadCountDto;
import com.mazurek.eventOrganizer.notification.repository.NotificationRepository;
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
@Transactional(readOnly = true)
public class NotificationQueryServiceImpl implements NotificationQueryService{

    private final Clock clock;
    private final NotificationRepository notificationRepository;
    private final AuthenticationService authenticationService;
    private final PaginationProperties paginationProperties;

    @Override
    public NotificationPageDto getCurrentUserNotifications(int pageNumber) {
        if (pageNumber < 0)
            throw new InvalidPageNumberException();

        Page<Notification> notificationPage = notificationRepository
                .findByRecipientId(
                        authenticationService.getCurrentUserId(),
                        PageRequest.of(
                                pageNumber,
                                paginationProperties.getDefaultPageSize(),
                                Sort.by(Sort.Direction.DESC, "createdAt", "id")
                        )
                );
        return new NotificationPageDto(notificationPage);
    }

    @Override
    public NotificationUnreadCountDto getCurrentUserUnreadCount() {
        UUID currentUserId = authenticationService.getCurrentUserId();
        long unreadCount = notificationRepository.countByRecipientIdAndReadAtIsNull(currentUserId);

        return new NotificationUnreadCountDto(unreadCount);
    }

    @Transactional
    @Override
    public void markAsRead(UUID notificationId) {
        UUID currentUserId = authenticationService.getCurrentUserId();
        Notification notification =  notificationRepository.findByIdAndRecipientId(notificationId, currentUserId)
                .orElseThrow(NotificationNotFoundException::new);
        notification.markAsRead(clock.instant());
    }

    @Transactional
    @Override
    public void markAllAsRead() {
        UUID currentUserId = authenticationService.getCurrentUserId();
        Instant readAt = clock.instant();
        notificationRepository.markAllAsRead(currentUserId, readAt);
    }
}
