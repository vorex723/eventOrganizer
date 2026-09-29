package com.mazurek.eventOrganizer.notification.firebaseCloudMessaging;

import com.mazurek.eventOrganizer.notification.domain.Notification;


public interface FcmApiClient {
    FcmSendResult sendNotificationToInstallationMobile(
            Notification notification,
            String firebaseInstallationId
    );

    FcmSendResult sendNotificationToInstallationWeb(
            Notification notification,
            String firebaseInstallationId
    );
}
