package com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.requests;

import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class SingleUserNotificationRequest extends BasicNotificationRequest {

    private String receiverFcmToken;

    public SingleUserNotificationRequest(NotificationResourceType resourceType) {
        super(resourceType);
    }

    @Builder
    public SingleUserNotificationRequest(String title, String body, UUID notificationId, UUID resourceId, NotificationResourceType resourceType, String receiverFcmToken) {
        super(title, body, notificationId, resourceId, resourceType);
        this.receiverFcmToken = receiverFcmToken;
    }
}
