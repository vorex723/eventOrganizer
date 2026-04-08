package com.mazurek.eventOrganizer.notification.firebaseCloudMessaging;

import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.requests.EventAttendersNotificationRequest;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.requests.SingleUserNotificationRequest;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.requests.topic.RegisterAttenderInEventTopicRequest;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.requests.topic.RegisterEventTopicRequest;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.requests.topic.TopicNotificationRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "test"})
public class FcmApiClientTestImpl implements FcmApiClient {
    @Override
    public void registerEventTopic(RegisterEventTopicRequest registerEventTopicRequest) {

    }

    @Override
    public void registerAttenderInEventTopic(RegisterAttenderInEventTopicRequest registerAttenderInEventTopicRequest) {

    }

    @Override
    public void sendNotificationToTopic(TopicNotificationRequest topicNotificationRequest) {

    }

    @Override
    public void sendNotificationToSingleUser(SingleUserNotificationRequest notificationRequest) {

    }

    @Override
    public void sendNotificationToEventAttenders(EventAttendersNotificationRequest notificationRequest) {

    }
}
