package com.mazurek.eventOrganizer.notification.firebaseCloudMessaging;

import com.google.firebase.messaging.*;
import com.mazurek.eventOrganizer.notification.requests.*;
import com.mazurek.eventOrganizer.notification.requests.EventAttendersNotificationRequest;
import com.mazurek.eventOrganizer.notification.requests.topic.RegisterAttenderInEventTopicRequest;
import com.mazurek.eventOrganizer.notification.requests.topic.RegisterEventTopicRequest;
import com.mazurek.eventOrganizer.notification.requests.topic.TopicNotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FcmApiClient {

    public void registerEventTopic(RegisterEventTopicRequest registerEventTopicRequest){

        try{
             FirebaseMessaging.getInstance().subscribeToTopic(List.of(registerEventTopicRequest.getEventOwnerFcmToken()), registerEventTopicRequest.getEventFcmTopicId());
        }   catch (FirebaseMessagingException exception){
            throw new RuntimeException("not sent");
        }
    }

    public void registerAttenderInEventTopic(RegisterAttenderInEventTopicRequest registerAttenderInEventTopicRequest){
        try{

           FirebaseMessaging.getInstance().subscribeToTopic(List.of(registerAttenderInEventTopicRequest.getUserFcmToken()), registerAttenderInEventTopicRequest.getEventFcmTopicId());

        }   catch (FirebaseMessagingException exception){
            throw new RuntimeException("not sent");
        }
    }

    public void sendNotificationToTopic(TopicNotificationRequest topicNotificationRequest){

        Notification notification = Notification.builder()
                .setTitle(topicNotificationRequest.getTitle())
                .setBody(topicNotificationRequest.getBody())
                .build();

        Message message = Message.builder()
                .setNotification(notification)
                .setTopic(topicNotificationRequest.getFcmTopicId())
                .build();

        try{
            FirebaseMessaging.getInstance().send(message);
        } catch (FirebaseMessagingException exception) {
            throw new RuntimeException("not sent");
        }
    }


    public void sendNotificationToSingleUser(SingleUserNotificationRequest notificationRequest){
        Notification notification = Notification.builder()
                .setTitle(notificationRequest.getTitle())
                .setBody(notificationRequest.getBody())
                .build();

        Message message = Message.builder()
                .setToken(notificationRequest.getReceiverFcmToken())
                .setNotification(notification)
                .putData("notificationType", notificationRequest.getNotificationType().toString())
                .putData("resourceId", notificationRequest.getResourceId().toString())
                .build();
        try{
           FirebaseMessaging.getInstance().send(message);
        } catch (Exception exception) {
            throw new RuntimeException("not sent");
        }

    }

    public void sendNotificationToEventAttenders(EventAttendersNotificationRequest notificationRequest){
        Notification notification = Notification.builder()
                .setTitle(notificationRequest.getTitle())
                .setBody(notificationRequest.getBody())
                .build();

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(notificationRequest.getEventAttendersFcmTokenList())
                .setNotification(notification)
                .putData("notificationType", notificationRequest.getNotificationType().toString())
                .putData("resourceId", notificationRequest.getResourceId().toString())
                .build();
        try{
          FirebaseMessaging.getInstance().sendEachForMulticastAsync(message);
        } catch (Exception exception) {
            throw new RuntimeException("not sent");
        }

    }

}
