package com.mazurek.eventOrganizer.notification.firebaseCloudMessaging;

import com.google.firebase.messaging.*;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.requests.EventAttendersNotificationRequest;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.requests.SingleUserNotificationRequest;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.requests.topic.RegisterAttenderInEventTopicRequest;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.requests.topic.RegisterEventTopicRequest;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.requests.topic.TopicNotificationRequest;

import java.util.List;

public interface FcmApiClient {
    void registerEventTopic(RegisterEventTopicRequest registerEventTopicRequest);
    void registerAttenderInEventTopic(RegisterAttenderInEventTopicRequest registerAttenderInEventTopicRequest);
    void sendNotificationToTopic(TopicNotificationRequest topicNotificationRequest);
    void sendNotificationToSingleUser(SingleUserNotificationRequest notificationRequest);
    void sendNotificationToEventAttenders(EventAttendersNotificationRequest notificationRequest);

}
