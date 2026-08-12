package com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.requests;

import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BasicNotificationRequest {
    private String title;
    private String body;
    private UUID notificationId;
    private UUID resourceId;
    private NotificationResourceType resourceType;

    public BasicNotificationRequest(NotificationResourceType resourceType) {
        this.resourceType = resourceType;
    }
}
