package com.mazurek.eventOrganizer.notification.firebaseCloudMessaging;

import com.google.firebase.messaging.*;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.notification.requests.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FcmApiClient {

    public void registerEventTopic(RegisterEventTopicRequest registerEventTopicRequest){

        try{
            FirebaseMessaging.getInstance().subscribeToTopic(List.of(registerEventTopicRequest.getEventOwnerFcmToken()), registerEventTopicRequest.getEventFcmTopicId());

        }   catch (FirebaseMessagingException exception){
            System.out.println(exception.getMessage());
        }
    }

    public void registerAttenderInEventTopic(RegisterAttenderInEventTopicRequest registerAttenderInEventTopicRequest){
        try{
            FirebaseMessaging.getInstance().subscribeToTopic(List.of(registerAttenderInEventTopicRequest.getUserFcmToken()), registerAttenderInEventTopicRequest.getEventFcmTopicId());
        }   catch (FirebaseMessagingException exception){
            System.out.println(exception.getMessage());
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
            String notificationResponse = FirebaseMessaging.getInstance().send(message);
        } catch (FirebaseMessagingException exception) {
            System.out.println(exception.getMessage());
        }
    }


    public void sendNotificationToSingleUser(SingleUserNotificationRequest singleUserNotificationRequest){
        Notification notification = Notification.builder()
                .setTitle(singleUserNotificationRequest.getTitle())
                .setBody(singleUserNotificationRequest.getBody())
                .build();

        Message message = Message.builder()
                .setToken(singleUserNotificationRequest.getReceiverFcmToken())
                .setNotification(notification)
                .build();
        try{
            String  notificationResponse = FirebaseMessaging.getInstance().send(message);
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }

    }

    public void sendNotificationToAllEventAttenders(AllEventAttendersNotificationRequest allEventAttendersNotificationRequest){
        Notification notification = Notification.builder()
                .setTitle(allEventAttendersNotificationRequest.getTitle())
                .setBody(allEventAttendersNotificationRequest.getBody())
                .build();

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(allEventAttendersNotificationRequest.getEventAttendersFcmTokenList())
                .setNotification(notification)
                .build();
        try{
            FirebaseMessaging.getInstance().sendEachForMulticast(message);
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }

    }

}
