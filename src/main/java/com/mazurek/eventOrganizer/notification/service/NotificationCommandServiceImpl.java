package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.notification.domain.*;
import com.mazurek.eventOrganizer.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationCommandServiceImpl implements NotificationCommandService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateService notificationTemplateService;
    private final Clock clock;

    @Override
    public void notifyPrivateMessage(UUID conversationId, UUID recipientId, String senderFullName) {
        NotificationTemplate notificationTemplate = notificationTemplateService.buildPrivateMessage(senderFullName);

        NotificationResourceReference resourceReference = NotificationResourceReference
                .direct(
                        NotificationResourceType.CONVERSATION,
                        conversationId);

        createNotifications(
                List.of(recipientId),
                notificationTemplate,
                resourceReference
        );

    }

    @Override
    public void notifyThreadReply(UUID eventId, UUID threadId, UUID recipientId, String replierFullName) {
        NotificationTemplate notificationTemplate = notificationTemplateService.buildThreadReply(replierFullName);

        NotificationResourceReference resourceReference = NotificationResourceReference
                .nested(
                        NotificationResourceType.THREAD,
                        threadId,
                        NotificationResourceType.EVENT,
                        eventId
                );

        createNotifications(
                List.of(recipientId),
                notificationTemplate,
                resourceReference
        );
    }

    @Override
    public void notifyEventUpdated(UUID eventId, Collection<UUID> recipientIds, String eventName) {
        NotificationTemplate notificationTemplate = notificationTemplateService.buildEventUpdate(eventName);

        NotificationResourceReference resourceReference = NotificationResourceReference
                .direct(
                        NotificationResourceType.EVENT,
                        eventId);

        createNotifications(
                recipientIds,
                notificationTemplate,
                resourceReference
        );
    }

    @Override
    public void notifyNewEventFile(UUID eventId, UUID fileId, Collection<UUID> recipientIds, String uploaderFullName) {
        NotificationTemplate notificationTemplate = notificationTemplateService.buildNewEventFile(uploaderFullName);

        NotificationResourceReference resourceReference = NotificationResourceReference
                .nested(
                        NotificationResourceType.FILE,
                        fileId,
                        NotificationResourceType.EVENT,
                        eventId
                        );

        createNotifications(
                recipientIds,
                notificationTemplate,
                resourceReference
        );
    }

    @Override
    public void notifyNewEventThread(UUID eventId, UUID threadId, Collection<UUID> recipientIds, String creatorFullName) {
        NotificationTemplate notificationTemplate = notificationTemplateService.buildNewEventThread(creatorFullName);

        NotificationResourceReference resourceReference = NotificationResourceReference
                .nested(
                        NotificationResourceType.THREAD,
                        threadId,
                        NotificationResourceType.EVENT,
                        eventId
                        );

        createNotifications(
                recipientIds,
                notificationTemplate,
                resourceReference
        );
    }

    private void createNotifications(
            Collection<UUID> recipientIds,
            NotificationTemplate template,
            NotificationResourceReference resourceReference
    ) {
        Instant notificationCreateTime = clock.instant();

        List<Notification> notifications = recipientIds.stream().distinct().map(recipientId ->
                Notification.builder()
                        .recipientId(recipientId)
                        .title(template.title())
                        .body(template.body())
                        .resourceType(resourceReference.resourceType())
                        .resourceId(resourceReference.resourceId())
                        .parentResourceType(resourceReference.parentResourceType())
                        .parentResourceId(resourceReference.parentResourceId())
                        .createdAt(notificationCreateTime)
                        .readAt(null)
                        .build())
                .toList();
        if (notifications.isEmpty())
            return;

        notificationRepository.saveAll(notifications);
    }
}
