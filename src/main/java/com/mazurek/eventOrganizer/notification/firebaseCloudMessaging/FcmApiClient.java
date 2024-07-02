package com.mazurek.eventOrganizer.notification.firebaseCloudMessaging;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import com.google.firebase.projectmanagement.FirebaseProjectManagement;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.notification.requests.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FcmApiClient {

    public String registerEventTopic(RegisterEventTopicRequest registerEventTopicRequest){

        try{
            String notficiationRespone  = FirebaseMessaging.getInstance().subscribeToTopic(List.of(registerEventTopicRequest.getEventOwnerFcmToken()), registerEventTopicRequest.getEventFcmTopicId()).toString();
            return notficiationRespone;

        }   catch (FirebaseMessagingException exception){
            throw new RuntimeException("not sent");
        }
    }

    public String registerAttenderInEventTopic(RegisterAttenderInEventTopicRequest registerAttenderInEventTopicRequest){
        try{

            String notificationResponse = FirebaseMessaging.getInstance().subscribeToTopic(List.of(registerAttenderInEventTopicRequest.getUserFcmToken()), registerAttenderInEventTopicRequest.getEventFcmTopicId()).toString();
            return notificationResponse;

        }   catch (FirebaseMessagingException exception){
            throw new RuntimeException("not sent");
        }
    }

    public String sendNotificationToTopic(TopicNotificationRequest topicNotificationRequest){

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
            return notificationResponse;
        } catch (FirebaseMessagingException exception) {
            throw new RuntimeException("not sent");
        }
    }


    public String sendNotificationToSingleUser(SingleUserNotificationRequest singleUserNotificationRequest){
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
            return notificationResponse;
        } catch (Exception exception) {
            throw new RuntimeException("not sent");
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
           BatchResponse notificationResponse =  FirebaseMessaging.getInstance().sendEachForMulticast(message);
            notificationResponse.getResponses().forEach(sendResponse -> System.out.println(sendResponse.getMessageId() + ": " + sendResponse.getException()));
        } catch (Exception exception) {
            throw new RuntimeException("not sent");
        }

    }

}
