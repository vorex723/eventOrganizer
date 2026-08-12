package com.mazurek.eventOrganizer.notification.service;

import java.util.Collection;
import java.util.UUID;

public interface NotificationCommandService {
    void notifyPrivateMessage(
            UUID conversationId,
            UUID recipientId,
            String senderFullName
    );

    void notifyThreadReply(
            UUID eventId,
            UUID threadId,
            UUID recipientId,
            String replierFullName
    );

    void notifyEventUpdated(
            UUID eventId,
            Collection<UUID> recipientIds,
            String eventName
    );

    void notifyNewEventFile(
            UUID eventId,
            UUID fileId,
            Collection<UUID> recipientIds,
            String uploaderFullName
    );

    void notifyNewEventThread(
            UUID eventId,
            UUID threadId,
            Collection<UUID> recipientIds,
            String creatorFullName
    );
}
