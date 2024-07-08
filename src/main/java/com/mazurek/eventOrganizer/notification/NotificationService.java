package com.mazurek.eventOrganizer.notification;

import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.exception.notification.NotificationNotFoundException;
import com.mazurek.eventOrganizer.jwt.JwtUtil;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmApiClient;
import com.mazurek.eventOrganizer.notification.requests.*;
import com.mazurek.eventOrganizer.notification.requests.EventAttendersNotificationRequest;
import com.mazurek.eventOrganizer.notification.requests.topic.RegisterAttenderInEventTopicRequest;
import com.mazurek.eventOrganizer.notification.requests.topic.RegisterEventTopicRequest;
import com.mazurek.eventOrganizer.notification.requests.topic.TopicNotificationRequest;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class NotificationService {

    private final FcmApiClient fcmApiClient;
    private final NotificationRepository notificationRepository;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;


    public void registerEventTopicInFcm(Event event, String eventOwnerFcmToken){
        RegisterEventTopicRequest registerEventTopicRequest = RegisterEventTopicRequest.builder()
                .title("Your event has been created.")
                .body(MessageFormat.format("Your event named: \"{0}\", has been created.", event.getName()))
                .eventFcmTopicId(event.getIdAsString())
                .eventOwnerFcmToken(event.getOwner().getFcmAndroidToken())
                .build();

       fcmApiClient.registerEventTopic(registerEventTopicRequest);
    }

    public void registerNewAttenderInEventTopic(Event event, String newAttenderFcmToken){
        RegisterAttenderInEventTopicRequest registerAttenderInEventTopicRequest = RegisterAttenderInEventTopicRequest.builder()
                .eventFcmTopicId(event.getIdAsString())
                .userFcmToken(newAttenderFcmToken)
                .build();

        fcmApiClient.registerAttenderInEventTopic(registerAttenderInEventTopicRequest);
    }


    //------------------------------------------- NOTIFICATIONS BY TOKEN LIST ------------------------------------------


    public void notifyEventAttenders(Event event, NotificationType notificationType, UUID resourceId,String actionPerformerFullName){

        EventAttendersNotificationRequest notificationRequest = new EventAttendersNotificationRequest(notificationType);
        notificationRequest.setResourceId(resourceId);

        switch (notificationType){
            case EVENT_UPDATE:
                notificationRequest.setEventAttendersFcmTokenList(event.getAttendersWithoutOwnerFcmTokenList());
                notificationRequest.setTitle("Event has been updated.");
                notificationRequest.setBody(MessageFormat.format("\"{0}\" have been recently updated. Check it's page for latest news.",event.getName()));
                createAndSaveNotificationsForEventAttenders(event, notificationRequest,resourceId,false);
                break;
            case EVENT_NEW_FILE:
                notificationRequest.setEventAttendersFcmTokenList(event.getAttendersWithOwnerFcmTokenList());
                notificationRequest.setTitle("New file uploaded to event.");
                notificationRequest.setBody(MessageFormat.format("{1} have add new file to event \"{0}\".",event.getName(), actionPerformerFullName));
                createAndSaveNotificationsForEventAttenders(event, notificationRequest,resourceId,true);
                break;
            case EVENT_NEW_THREAD:
                notificationRequest.setEventAttendersFcmTokenList(event.getAttendersWithOwnerFcmTokenList());
                notificationRequest.setTitle("New thread was created in event.");
                notificationRequest.setBody(MessageFormat.format("{0} has created new thread in \"{1}\".",actionPerformerFullName, event.getName()));
                createAndSaveNotificationsForEventAttenders(event, notificationRequest,resourceId,true);
                break;
        }

        fcmApiClient.sendNotificationToEventAttenders(notificationRequest);
    }

    //------------------------------------------ SINGLE USER NOTIFICATIONS ---------------------------------------------


    public void notifyThreadOwner(Thread thread, String replierFullName){
        SingleUserNotificationRequest notificationRequest = SingleUserNotificationRequest.builder()
                .receiverFcmToken(thread.getOwner().getFcmAndroidToken())
                .title("New reply in thread.")
                .body(MessageFormat.format("{0} has just reply in your thread: \"{1}\"", replierFullName, thread.getName()))
                .notificationType(NotificationType.THREAD_REPLY)
                .resourceId(thread.getId())
                .build();

        createAndSaveNotificationForSingleUser(thread.getOwner(), notificationRequest);
        fcmApiClient.sendNotificationToSingleUser(notificationRequest);
    }

    public void notifyMessageRecipient(User recipient, UUID conversationId,String senderFullName){
        SingleUserNotificationRequest notificationRequest = SingleUserNotificationRequest.builder()
                .receiverFcmToken(recipient.getFcmAndroidToken())
                .title("New message.")
                .body(MessageFormat.format("You have received new message from {0}.", senderFullName))
                .notificationType(NotificationType.PRIVATE_MESSAGE)
                .resourceId(conversationId)
                .build();
        createAndSaveNotificationForSingleUser(recipient, notificationRequest);
        fcmApiClient.sendNotificationToSingleUser(notificationRequest);
    }

//--------------------------------------------NOTIFICATIONS BY TOPIC -----------------------------------------------
/*
* -add resource id
* */
    public void sendEventHasBeenUpdatedNotificationByTopic(Event event){
        TopicNotificationRequest notificationRequest = TopicNotificationRequest.builder()
                .fcmTopicId(event.getIdAsString())
                .title("Event has been updated.")
                .body(MessageFormat.format("\"{0}\" have been recently updated. Check it's page for latest news.",event.getName()))
                .build();

        fcmApiClient.sendNotificationToTopic(notificationRequest);
    }

    public void sendNewFileUploadedToEventNotificationByFcmTopic(Event event, String fileOwnerFullName){
        TopicNotificationRequest notificationRequest = TopicNotificationRequest.builder()
                .fcmTopicId(event.getIdAsString())
                .title("New file was uploaded.")
                .body(MessageFormat.format("{1} have add new file to event \"{0}\".",event.getName(), fileOwnerFullName))
                .build();


        fcmApiClient.sendNotificationToTopic(notificationRequest);
    }

    public void sendNewThreadInEventNotificationByEventTopic(Event event, String threadCreatorFullName){
        TopicNotificationRequest notificationRequest = TopicNotificationRequest.builder()
                .fcmTopicId(event.getIdAsString())
                .title("New thread in event.")
                .body(MessageFormat.format("{0} has created new thread in \"{1}\".",threadCreatorFullName, event.getName() ))
                .build();

        fcmApiClient.sendNotificationToTopic(notificationRequest);
    }

    //---------------------------------------NOTIFICATION USER INTERACTIONS---------------------------------------------

    public void setNotificationOpened(UUID userID, UUID notificationId, String jwtToken){
        User user = userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).get();
        Optional<Notification> notificationOptional = notificationRepository.findById(notificationId);

        if (notificationOptional.isEmpty())
            throw new NotificationNotFoundException();

        Notification notification = notificationOptional.get();

        if (!user.getId().equals(userID) || !notification.getReceiver().equals(user))
            throw new RuntimeException("You little bastard.");
            //maybe bad url exception for unsuccessful route id validation

        notification.setOpened(true);
        notificationRepository.save(notification);
    }

    public List<NotificationDto> getUserNotifications(UUID userId){
        return null;
    }

    private void createAndSaveNotificationsForEventAttenders(Event event, EventAttendersNotificationRequest notificationRequest,UUID resourceId, boolean notifyEventOwner){
        List<Notification> notifications = new ArrayList<>();

        event.getAttendingUsers().forEach(user ->
                notifications.add(
                        Notification.builder()
                                .receiver(user)
                                .title(notificationRequest.getTitle())
                                .body(notificationRequest.getBody())
                                .type(notificationRequest.getNotificationType())
                                .resourceId(notificationRequest.getResourceId())
                                .build()
                )
        );

        if (notifyEventOwner) {
            notifications.add(Notification.builder()
                    .receiver(event.getOwner())
                    .title(notificationRequest.getTitle())
                    .body(notificationRequest.getBody())
                    .type(notificationRequest.getNotificationType())
                    .resourceId(notificationRequest.getResourceId())
                    .build());
        }

        notifications.forEach(notification -> notification.getReceiver().addNotification(notification));
        notificationRepository.saveAll(notifications);
    }
    private void createAndSaveNotificationForSingleUser(User user, SingleUserNotificationRequest notificationRequest){
        Notification notification = Notification.builder()
                .receiver(user)
                .title(notificationRequest.getTitle())
                .body(notificationRequest.getBody())
                .type(notificationRequest.getNotificationType())
                .resourceId(notificationRequest.getResourceId())
                .build();

        notificationRepository.save(notification);
    }
}


