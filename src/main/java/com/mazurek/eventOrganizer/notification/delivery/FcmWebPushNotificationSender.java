package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationDelivery;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmApiClient;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmSendResult;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeviceRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.firebase", name = "enabled", havingValue = "true")
public class FcmWebPushNotificationSender implements NotificationSender {

    private final FcmApiClient fcmApiClient;
    private final NotificationDeviceRepository notificationDeviceRepository;

    @Autowired
    public FcmWebPushNotificationSender(
            FcmApiClient fcmApiClient,
            ObjectProvider<NotificationDeviceRepository> notificationDeviceRepository
    ) {
        this(fcmApiClient, notificationDeviceRepository.getIfAvailable());
    }

    private FcmWebPushNotificationSender(
            FcmApiClient fcmApiClient,
            NotificationDeviceRepository notificationDeviceRepository
    ) {
        this.fcmApiClient = fcmApiClient;
        this.notificationDeviceRepository = notificationDeviceRepository;
    }

    /** @deprecated Test/legacy constructor; production delivery requires the repository for safe cleanup. */
    @Deprecated
    public FcmWebPushNotificationSender(FcmApiClient fcmApiClient) {
        this(fcmApiClient, (NotificationDeviceRepository) null);
    }

    @Override
    public NotificationChannel supportedChannel() {
        return NotificationChannel.PUSH_WEB;
    }

    @Override
    public NotificationSendResult send(NotificationDelivery delivery) {
        if (delivery.getTargetInstallationId() == null) {
            return NotificationSendResult.permanentFailure("Web push delivery has no target snapshot.");
        }
        FcmSendResult result = fcmApiClient.sendNotificationToInstallationWeb(
                delivery.getNotification(), delivery.getTargetInstallationId());
        removeInvalidTarget(delivery, result);
        return FcmNotificationSendResultMapper.map(result);
    }

    /** @deprecated Delivery processing must use the immutable target on {@link NotificationDelivery}. */
    @Deprecated
    public NotificationSendResult send(Notification notification) {
        return FcmNotificationSendResultMapper.map(fcmApiClient.sendNotificationToSingleUserWeb(notification));
    }

    private void removeInvalidTarget(NotificationDelivery delivery, FcmSendResult result) {
        if (result.invalidTargetCount() > 0 && delivery.getTargetDeviceId() != null
                && notificationDeviceRepository != null) {
            notificationDeviceRepository.deleteIfOwnedByIdAndUserIdAndFirebaseInstallationId(
                    delivery.getTargetDeviceId(),
                    delivery.getNotification().getRecipientId(),
                    delivery.getTargetInstallationId()
            );
        }
    }
}
