package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.notification.domain.DevicePlatform;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationDevice;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmApiClient;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FcmNotificationSender implements NotificationSender {

    private final FcmApiClient fcmApiClient;
    private final NotificationDeviceRepository notificationDeviceRepository;


    @Override
    public NotificationChannel supportedChannel() {
        return NotificationChannel.PUSH_ANDROID;
    }

    @Override
    public NotificationSendResult send(NotificationSendRequest request) {
        List<NotificationDevice> devices = notificationDeviceRepository
                .findByUserIdAndPlatformAndActiveTrue(request.recipientId(), DevicePlatform.ANDROID);
        if (devices.isEmpty())
            return NotificationSendResult.failed("Recipient has no active Android device.");

        return NotificationSendResult.failed("FCM NOT DONE YET");
    }
}
