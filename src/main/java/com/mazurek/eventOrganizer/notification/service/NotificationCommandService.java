package com.mazurek.eventOrganizer.notification.service;

import java.util.Collection;
import java.util.UUID;

public interface NotificationCommandService {
    void notifyPrivateMessage(UUID recipientId, UUID conversationId, String senderFullName);
    void notifyThreadReply(UUID recipientId, UUID threadId, String replierFullName);
    void notifyEventUpdated(UUID recipientId, Collection<UUID> recipientIds);
    void notifyNewEventFile(UUID recipientId, Collection<UUID> recipientIds, String uploaderFullName);
    void notifyNewEventThread(UUID recipientId, Collection<UUID> recipientIds, String creatorFullName);
}
