package com.mazurek.eventOrganizer.notification;

import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.exception.user.InvalidUserException;
import com.mazurek.eventOrganizer.notification.dto.NotificationsPageDto;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.user.User;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Profile({"local", "test"})
public class NotificationServiceTestImpl implements NotificationService{
    @Override
    public NotificationsPageDto getUserNotifications(UUID userId, String jwtToken, int page) throws InvalidUserException {
        return null;
    }

    @Override
    public void registerEventTopicInFcm(Event event, String eventOwnerFcmToken) {

    }

    @Override
    public void registerNewAttenderInEventTopic(Event event, String newAttenderFcmToken) {

    }

    @Override
    public void notifyEventAttenders(Event event, NotificationType notificationType, UUID resourceId, String actionPerformerFullName) {

    }

    @Override
    public void notifyThreadOwner(Thread thread, String replierFullName) {

    }

    @Override
    public void notifyMessageRecipient(User recipient, UUID conversationId, String senderFullName) {

    }

    @Override
    public void sendEventHasBeenUpdatedNotificationByTopic(Event event) {

    }

    @Override
    public void sendNewFileUploadedToEventNotificationByFcmTopic(Event event, String fileOwnerFullName) {

    }

    @Override
    public void sendNewThreadInEventNotificationByEventTopic(Event event, String threadCreatorFullName) {

    }

    @Override
    public void setNotificationOpened(UUID userID, UUID notificationId, String jwtToken) {

    }
}
