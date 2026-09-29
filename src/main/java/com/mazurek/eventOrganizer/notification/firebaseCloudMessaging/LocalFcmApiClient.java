package com.mazurek.eventOrganizer.notification.firebaseCloudMessaging;

import com.mazurek.eventOrganizer.notification.domain.Notification;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalFcmApiClient implements FcmApiClient {

    @Override
    public FcmSendResult sendNotificationToInstallationMobile(Notification notification, String firebaseInstallationId) {
        return FcmSendResult.successful(1);
    }

    @Override
    public FcmSendResult sendNotificationToInstallationWeb(Notification notification, String firebaseInstallationId) {
        return FcmSendResult.successful(1);
    }
}
