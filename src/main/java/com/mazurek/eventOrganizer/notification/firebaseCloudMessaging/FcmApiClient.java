package com.mazurek.eventOrganizer.notification.firebaseCloudMessaging;

import com.mazurek.eventOrganizer.notification.domain.Notification;


public interface FcmApiClient {
    FcmSendResult sendNotificationToSingleUserMobile(Notification inAppNotification);
    FcmSendResult sendNotificationToSingleUserWeb(Notification inAppNotification);
}
