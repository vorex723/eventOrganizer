package com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.requests.topic;

import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.requests.BasicNotificationRequest;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class TopicNotificationRequest extends BasicNotificationRequest {
    private String fcmTopicId;


    @Builder
    public TopicNotificationRequest(String title, String body, UUID notificationId, UUID resourceId, NotificationResourceType resourceType, String fcmTopicId) {
        super(title, body, notificationId, resourceId, resourceType);
        this.fcmTopicId = fcmTopicId;
    }
}
