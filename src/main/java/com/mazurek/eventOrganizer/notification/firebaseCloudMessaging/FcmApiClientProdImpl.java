package com.mazurek.eventOrganizer.notification.firebaseCloudMessaging;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushFcmOptions;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.utils.NotificationResourceLinkResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("production")
@ConditionalOnProperty(prefix = "app.firebase", name = "enabled", havingValue = "true")
public class FcmApiClientProdImpl implements FcmApiClient {

    private final FirebaseMessaging firebaseMessaging;
    private final NotificationResourceLinkResolver notificationResourceLinkResolver;

    @Override
    public FcmSendResult sendNotificationToInstallationMobile(Notification notification, String firebaseInstallationId) {
        com.google.firebase.messaging.Notification fcmNotification = com.google.firebase.messaging.Notification.builder()
                .setTitle(notification.getTitle())
                .setBody(notification.getBody())
                .build();

        Map<String, String> data = new HashMap<>();

        data.put("notificationId", notification.getId().toString());
        data.put("resourceType", notification.getResourceType().toString());
        data.put("resourceId", notification.getResourceId().toString());

        if (notification.getParentResourceType() != null && notification.getParentResourceId() != null) {
            data.put(
                    "parentResourceType",
                    notification.getParentResourceType().toString()
            );
            data.put(
                    "parentResourceId",
                    notification.getParentResourceId().toString()
            );
        }

        Message message = Message.builder()
                .setFid(firebaseInstallationId)
                .setNotification(fcmNotification)
                .putAllData(data)
                .build();

        return sendToInstallation(message);
    }

    @Override
    public FcmSendResult sendNotificationToInstallationWeb(Notification notification, String firebaseInstallationId) {
        String link = notificationResourceLinkResolver.resolve(notification);
        com.google.firebase.messaging.Notification fcmNotification = com.google.firebase.messaging.Notification.builder()
                .setTitle(notification.getTitle())
                .setBody(notification.getBody())
                .build();

        Message message = Message.builder()
                .setFid(firebaseInstallationId)
                .setNotification(fcmNotification)
                .putData("notificationId", notification.getId().toString())
                .setWebpushConfig(WebpushConfig.builder()
                        .setFcmOptions(WebpushFcmOptions.withLink(link))
                        .build())
                .build();

        return sendToInstallation(message);
    }

    private FcmSendResult sendToInstallation(Message message) {
        try {
            BatchResponse batchResponse = firebaseMessaging.sendEach(List.of(message));
            List<SendResponse> responses = batchResponse.getResponses();
            if (responses.size() != 1) {
                throw new IllegalStateException(
                        "FCM returned a different number of responses than submitted messages."
                );
            }

            SendResponse response = responses.getFirst();
            if (response.isSuccessful()) {
                return FcmSendResult.successful(1);
            }

            FirebaseMessagingException failure = response.getException();
            MessagingErrorCode errorCode = failure == null ? null : failure.getMessagingErrorCode();
            return failedResult(errorCode, true);
        } catch (FirebaseMessagingException exception) {
            MessagingErrorCode errorCode = exception.getMessagingErrorCode();

            log.error(
                    "FCM request failed: targets=1, errorCode={}, message={}",
                    errorCode,
                    exception.getMessage(),
                    exception
            );

            return failedResult(errorCode, false);
        }
    }

    private FcmSendResult failedResult(MessagingErrorCode errorCode, boolean perTargetResponse) {
        String errorMessage = "FCM delivery failures: {" + errorCodeName(errorCode) + "=1}";
        FcmSendResult result;

        if (perTargetResponse && errorCode == MessagingErrorCode.UNREGISTERED) {
            result = FcmSendResult.fromCounts(1, 0, 1, 0, 0, errorMessage);
        } else if (isRetryable(errorCode)) {
            result = FcmSendResult.retryableFailure(1, errorMessage);
        } else {
            result = FcmSendResult.permanentFailure(1, errorMessage);
        }

        log.warn(
                "FCM delivery contained failed response: targets=1, successes=0, invalid={}, retryable={}, permanent={}, errorCode={}",
                result.invalidTargetCount(),
                result.retryableFailureCount(),
                result.permanentFailureCount(),
                errorCodeName(errorCode)
        );
        return result;
    }

    private boolean isRetryable(MessagingErrorCode errorCode) {
        if (errorCode == null) {
            return true;
        }

        return switch (errorCode) {
            case INTERNAL, QUOTA_EXCEEDED, UNAVAILABLE -> true;
            case INVALID_ARGUMENT, SENDER_ID_MISMATCH, THIRD_PARTY_AUTH_ERROR, UNREGISTERED -> false;
        };
    }

    private String errorCodeName(MessagingErrorCode errorCode) {
        return errorCode == null ? "UNKNOWN" : errorCode.name();
    }
}
