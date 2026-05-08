package com.mazurek.eventOrganizer.notification;

import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.exception.user.InvalidUserException;
import com.mazurek.eventOrganizer.notification.dto.NotificationsPageDto;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.user.User;

import java.util.UUID;

public interface NotificationService {
    NotificationsPageDto getUserNotifications(UUID userId, String jwtToken, int page) throws InvalidUserException;
    void registerEventTopicInFcm(Event event, String eventOwnerFcmToken);
    void registerNewAttenderInEventTopic(Event event, String newAttenderFcmToken);
    void notifyEventAttenders(Event event, NotificationType notificationType, UUID resourceId, String actionPerformerFullName);
    void notifyThreadOwner(Thread thread, String replierFullName);
    void notifyMessageRecipient(User recipient, UUID conversationId, String senderFullName);
    void notifyMessageRecipientById(UUID recipientId, UUID conversationId, String senderFullName);
    void sendEventHasBeenUpdatedNotificationByTopic(Event event);
    void sendNewFileUploadedToEventNotificationByFcmTopic(Event event, String fileOwnerFullName);
    void sendNewThreadInEventNotificationByEventTopic(Event event, String threadCreatorFullName);
    void setNotificationOpened(UUID userID, UUID notificationId, String jwtToken);

}
