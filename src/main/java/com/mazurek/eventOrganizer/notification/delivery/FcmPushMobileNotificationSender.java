package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationDelivery;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmApiClient;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmSendResult;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeviceRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@ConditionalOnProperty(prefix = "app.firebase", name = "enabled", havingValue = "true")
public class FcmPushMobileNotificationSender implements NotificationSender {

    private final FcmApiClient fcmApiClient;
    private final NotificationDeviceRepository notificationDeviceRepository;

    public FcmPushMobileNotificationSender(
            FcmApiClient fcmApiClient,
            NotificationDeviceRepository notificationDeviceRepository
    ) {
        this.fcmApiClient = Objects.requireNonNull(fcmApiClient);
        this.notificationDeviceRepository = Objects.requireNonNull(notificationDeviceRepository);
    }

    @Override
    public NotificationChannel supportedChannel() {
        return NotificationChannel.PUSH_MOBILE;
    }

    @Override
    public NotificationSendResult send(NotificationDelivery delivery) {
        if (delivery.getTargetInstallationId() == null) {
            return NotificationSendResult.permanentFailure("Mobile push delivery has no target snapshot.");
        }
        FcmSendResult result = fcmApiClient.sendNotificationToInstallationMobile(
                delivery.getNotification(), delivery.getTargetInstallationId());
        removeInvalidTarget(delivery, result);
        return FcmNotificationSendResultMapper.map(result);
    }

    private void removeInvalidTarget(NotificationDelivery delivery, FcmSendResult result) {
        if (result.invalidTargetCount() > 0 && delivery.getTargetDeviceId() != null) {
            notificationDeviceRepository.deleteIfOwnedByIdAndUserIdAndFirebaseInstallationId(
                    delivery.getTargetDeviceId(),
                    delivery.getNotification().getRecipientId(),
                    delivery.getTargetInstallationId()
            );
        }
    }
}
