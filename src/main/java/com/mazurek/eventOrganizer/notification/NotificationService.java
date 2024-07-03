package com.mazurek.eventOrganizer.notification;

import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmApiClient;
import com.mazurek.eventOrganizer.notification.requests.*;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;


@RequiredArgsConstructor
@Service
public class NotificationService {
    private final FcmApiClient fcmApiClient;

    public void registerEventTopicInFcm(Event event, String eventOwnerFcmToken){
        RegisterEventTopicRequest registerEventTopicRequest = RegisterEventTopicRequest.builder()
                .title("Your event has been created.")
                .body(MessageFormat.format("Your event named: \"{0}\", has been created.", event.getName()))
                .eventFcmTopicId(event.getIdAsString())
                .eventOwnerFcmToken(event.getOwner().getFcmAndroidToken())
                .build();

        String response = fcmApiClient.registerEventTopic(registerEventTopicRequest);
    }

    public void registerNewAttenderInEventTopic(Event event, String newAttenderFcmToken){
        RegisterAttenderInEventTopicRequest registerAttenderInEventTopicRequest = RegisterAttenderInEventTopicRequest.builder()
                .eventFcmTopicId(event.getIdAsString())
                .userFcmToken(newAttenderFcmToken)
                .build();

        String response = fcmApiClient.registerAttenderInEventTopic(registerAttenderInEventTopicRequest);
    }

    //--------------------------------------------NOTIFICATIONS BY TOPIC -----------------------------------------------

    public void sendEventHasBeenUpdatedNotificationByTopic(Event event){
        TopicNotificationRequest topicNotificationRequest = TopicNotificationRequest.builder()
                .fcmTopicId(event.getIdAsString())
                .title("Event has been updated.")
                .body(MessageFormat.format("\"{0}\" have been recently updated. Check it's page for latest news.",event.getName()))
                .build();

        String response = fcmApiClient.sendNotificationToTopic(topicNotificationRequest);
    }

    public void sendNewFileUploadedToEventNotificationByFcmTopic(Event event, String fileOwnerFullName){
        TopicNotificationRequest topicNotificationRequest = TopicNotificationRequest.builder()
                .fcmTopicId(event.getIdAsString())
                .title("New file was uploaded.")
                .body(MessageFormat.format("{1} have add new file to event \"{0}\".",event.getName(), fileOwnerFullName))
                .build();

        String response = fcmApiClient.sendNotificationToTopic(topicNotificationRequest);
    }

    public void sendNewThreadInEventNotificationByEventTopic(Event event, String threadCreatorFullName){
        TopicNotificationRequest topicNotificationRequest = TopicNotificationRequest.builder()
                .fcmTopicId(event.getIdAsString())
                .title("New thread in event.")
                .body(MessageFormat.format("{0} has created new thread in \"{1}\".",threadCreatorFullName, event.getName() ))
                .build();

        String response = fcmApiClient.sendNotificationToTopic(topicNotificationRequest);
    }

    //------------------------------------------- NOTIFICATIONS BY TOKEN LIST ------------------------------------------


    public void sendEventHasBeenUpdatedNotificationByUsersFcmTokens(Event event){
        AllEventAttendersNotificationRequest allEventAttendersNotificationRequest = AllEventAttendersNotificationRequest.builder()
                .eventAttendersFcmTokenList(event.getAttendersFcmTokenList())
                .title("Event has been updated.")
                .body(MessageFormat.format("\"{0}\" have been recently updated. Check it's page for latest news.",event.getName()))
                .build();

        fcmApiClient.sendNotificationToAllEventAttenders(allEventAttendersNotificationRequest);
    }

    public void sendNewFileUploadedToEventNotificationByUsersFcmTokens(Event event, String fileOwnerFullName){
        AllEventAttendersNotificationRequest allEventAttendersNotificationRequest = AllEventAttendersNotificationRequest.builder()
                .eventAttendersFcmTokenList(event.getAttendersFcmTokenList())
                .title("New file was uploaded.")
                .body(MessageFormat.format("{1} have add new file to event \"{0}\".",event.getName(), fileOwnerFullName))
                .build();

         fcmApiClient.sendNotificationToAllEventAttenders(allEventAttendersNotificationRequest);
    }

    public void sendNewThreadInEventNotificationByUsersFcmTokens(Event event, String threadCreatorFullName){
        AllEventAttendersNotificationRequest allEventAttendersNotificationRequest = AllEventAttendersNotificationRequest.builder()
                .eventAttendersFcmTokenList(event.getAttendersFcmTokenList())
                .title("New thread in event.")
                .body(MessageFormat.format("{0} has created new thread in \"{1}\".",threadCreatorFullName, event.getName() ))
                .build();

        fcmApiClient.sendNotificationToAllEventAttenders(allEventAttendersNotificationRequest);
    }

    //------------------------------------------ SINGLE USER NOTIFICATIONS ---------------------------------------------

    public void sendNewReplyInThreadNotification(Thread thread, String replierFullName){
        SingleUserNotificationRequest singleUserNotificationRequest = SingleUserNotificationRequest.builder()
                .receiverFcmToken(thread.getOwner().getFcmAndroidToken())
                .title("New reply in thread.")
                .body(MessageFormat.format("{0} has just reply in your thread: \"{1}\"", replierFullName, thread.getName()))
                .build();

        String response = fcmApiClient.sendNotificationToSingleUser(singleUserNotificationRequest);
    }

    public void sendNewPrivateMessageNotification(User recipient, String senderFullName){
        SingleUserNotificationRequest singleUserNotificationRequest = SingleUserNotificationRequest.builder()
                .receiverFcmToken(recipient.getFcmAndroidToken())
                .title("New message.")
                .body(MessageFormat.format("You have received new message from {0}.", senderFullName))
                .build();

        String response = fcmApiClient.sendNotificationToSingleUser(singleUserNotificationRequest);
    }


}
