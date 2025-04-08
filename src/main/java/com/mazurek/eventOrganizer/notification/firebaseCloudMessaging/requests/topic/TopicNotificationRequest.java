package com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.requests.topic;

import com.mazurek.eventOrganizer.notification.NotificationType;
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
    public TopicNotificationRequest(String title, String body, UUID notificationId, UUID resourceId, NotificationType notificationType, String fcmTopicId) {
        super(title, body, notificationId, resourceId, notificationType);
        this.fcmTopicId = fcmTopicId;
    }
}
