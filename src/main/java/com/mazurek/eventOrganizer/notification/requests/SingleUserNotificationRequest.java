package com.mazurek.eventOrganizer.notification.requests;

import com.mazurek.eventOrganizer.notification.NotificationType;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class SingleUserNotificationRequest extends BasicNotificationRequest {

    private String receiverFcmToken;

    public SingleUserNotificationRequest(NotificationType notificationType) {
        super(notificationType);
    }

    @Builder
    public SingleUserNotificationRequest(String title, String body, UUID notificationId, UUID resourceId, NotificationType notificationType, String receiverFcmToken) {
        super(title, body, notificationId, resourceId, notificationType);
        this.receiverFcmToken = receiverFcmToken;
    }
}
