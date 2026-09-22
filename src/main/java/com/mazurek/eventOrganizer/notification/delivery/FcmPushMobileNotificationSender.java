package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmApiClient;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmSendResult;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.firebase", name = "enabled", havingValue = "true")
public class FcmPushMobileNotificationSender implements NotificationSender {

    private final FcmApiClient fcmApiClient;

    @Override
    public NotificationChannel supportedChannel() {
        return NotificationChannel.PUSH_MOBILE;
    }

    @Override
    public NotificationSendResult send(Notification notification) {
        FcmSendResult result = fcmApiClient.sendNotificationToSingleUserMobile(notification);
        return FcmNotificationSendResultMapper.map(result);
    }
}
