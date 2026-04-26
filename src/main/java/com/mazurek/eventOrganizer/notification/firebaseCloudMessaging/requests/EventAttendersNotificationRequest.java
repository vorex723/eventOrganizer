package com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.requests;

import com.mazurek.eventOrganizer.notification.NotificationType;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventAttendersNotificationRequest extends BasicNotificationRequest {

    private List<String> eventAttendersFcmTokenList;

    public EventAttendersNotificationRequest(NotificationType notificationType) {
        super(notificationType);
    }

    @Builder
    public EventAttendersNotificationRequest(String title, String body, UUID notificationId, UUID resourceId, NotificationType notificationType, List<String> eventAttendersFcmTokenList) {
        super(title, body, notificationId, resourceId, notificationType);
        this.eventAttendersFcmTokenList = eventAttendersFcmTokenList;
    }
}
