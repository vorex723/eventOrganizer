package com.mazurek.eventOrganizer.notification.firebaseCloudMessaging;

import com.mazurek.eventOrganizer.notification.domain.Notification;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Profile("test")
public class TestFcmApiClient implements FcmApiClient {

    private final AtomicReference<FcmSendResult> mobileResult =
            new AtomicReference<>(FcmSendResult.successful(1));
    private final AtomicReference<FcmSendResult> webResult =
            new AtomicReference<>(FcmSendResult.successful(1));

    @Override
    public FcmSendResult sendNotificationToInstallationMobile(Notification notification, String firebaseInstallationId) {
        return mobileResult.get();
    }

    @Override
    public FcmSendResult sendNotificationToInstallationWeb(Notification notification, String firebaseInstallationId) {
        return webResult.get();
    }

    public void configureMobileResult(FcmSendResult result) {
        mobileResult.set(Objects.requireNonNull(result));
    }

    public void configureWebResult(FcmSendResult result) {
        webResult.set(Objects.requireNonNull(result));
    }

    public void reset() {
        mobileResult.set(FcmSendResult.successful(1));
        webResult.set(FcmSendResult.successful(1));
    }
}
