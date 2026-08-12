package com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.requests.topic;

import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterEventTopicRequest {
    private String eventOwnerFcmToken;
    private String eventFcmTopicId;
    private String title;
    private String body;
    private NotificationResourceType resourceType;
    private String id;
}
