package com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.requests;

import com.mazurek.eventOrganizer.notification.NotificationType;
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
    private NotificationType notificationType;

    public BasicNotificationRequest(NotificationType notificationType) {
        this.notificationType = notificationType;
    }
}
