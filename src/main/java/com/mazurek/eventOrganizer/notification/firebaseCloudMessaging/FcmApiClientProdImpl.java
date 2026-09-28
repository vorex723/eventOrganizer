package com.mazurek.eventOrganizer.notification.firebaseCloudMessaging;

import com.google.firebase.messaging.*;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.DevicePlatform;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeviceRepository;
import com.mazurek.eventOrganizer.utils.NotificationResourceLinkResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("production")
@ConditionalOnProperty(prefix = "app.firebase", name = "enabled", havingValue = "true")
public class FcmApiClientProdImpl implements FcmApiClient {

    private static final int MAX_MESSAGES_PER_REQUEST = 500;

    private final FirebaseMessaging firebaseMessaging;
    private final NotificationDeviceRepository notificationDeviceRepository;
    private final NotificationResourceLinkResolver notificationResourceLinkResolver;

    @Override
    public FcmSendResult sendNotificationToSingleUserMobile(Notification inAppNotification) {
        List<String> firebaseInstallationIds = notificationDeviceRepository
                .findAllFirebaseInstallationIdsByUserIdAndPlatformIn(
                        inAppNotification.getRecipientId(),
                        EnumSet.of(DevicePlatform.ANDROID, DevicePlatform.IOS)
                );

        return sendMobile(inAppNotification, firebaseInstallationIds);
    }

    @Override
    public FcmSendResult sendNotificationToInstallationMobile(Notification notification, String firebaseInstallationId) {
        return sendMobile(notification, List.of(firebaseInstallationId), false);
    }

    private FcmSendResult sendMobile(Notification inAppNotification, List<String> firebaseInstallationIds) {
        return sendMobile(inAppNotification, firebaseInstallationIds, true);
    }

    private FcmSendResult sendMobile(
            Notification inAppNotification,
            List<String> firebaseInstallationIds,
            boolean cleanupInvalidInstallations
    ) {
        if (firebaseInstallationIds.isEmpty()) {
            return FcmSendResult.noTargets();
        }

        com.google.firebase.messaging.Notification fcmNotification = com.google.firebase.messaging.Notification.builder()
                .setTitle(inAppNotification.getTitle())
                .setBody(inAppNotification.getBody())
                .build();

        Map<String, String> data = new HashMap<>();

        data.put("notificationId", inAppNotification.getId().toString());
        data.put("resourceType", inAppNotification.getResourceType().toString());
        data.put("resourceId", inAppNotification.getResourceId().toString());

        if (inAppNotification.getParentResourceType() != null && inAppNotification.getParentResourceId() != null) {
            data.put(
                    "parentResourceType",
                    inAppNotification.getParentResourceType().toString()
            );
            data.put(
                    "parentResourceId",
                    inAppNotification.getParentResourceId().toString()
            );
        }

        List<Message> messages = firebaseInstallationIds.stream().map(fid ->
                    Message.builder()
                            .setFid(fid)
                            .setNotification(fcmNotification)
                            .putAllData(data)
                            .build()
        ).toList();

        return batchSendNotifications(firebaseInstallationIds, messages, cleanupInvalidInstallations);
    }

    @Override
    public FcmSendResult sendNotificationToSingleUserWeb(Notification inAppNotification) {
        List<String> firebaseInstallationIds = notificationDeviceRepository
                .findAllFirebaseInstallationIdsByUserIdAndPlatformIn(
                        inAppNotification.getRecipientId(),
                        EnumSet.of(DevicePlatform.WEB)
                );

        return sendWeb(inAppNotification, firebaseInstallationIds);
    }

    @Override
    public FcmSendResult sendNotificationToInstallationWeb(Notification notification, String firebaseInstallationId) {
        return sendWeb(notification, List.of(firebaseInstallationId), false);
    }

    private FcmSendResult sendWeb(Notification inAppNotification, List<String> firebaseInstallationIds) {
        return sendWeb(inAppNotification, firebaseInstallationIds, true);
    }

    private FcmSendResult sendWeb(
            Notification inAppNotification,
            List<String> firebaseInstallationIds,
            boolean cleanupInvalidInstallations
    ) {
        if (firebaseInstallationIds.isEmpty()) {
            return FcmSendResult.noTargets();
        }

        String link = notificationResourceLinkResolver.resolve(inAppNotification);
        com.google.firebase.messaging.Notification fcmNotification = com.google.firebase.messaging.Notification.builder()
                .setTitle(inAppNotification.getTitle())
                .setBody(inAppNotification.getBody())
                .build();

        List<Message> messages = firebaseInstallationIds.stream().map(fid ->
                Message.builder()
                        .setFid(fid)
                        .setNotification(fcmNotification)
                        .putData("notificationId", inAppNotification.getId().toString())
                        .setWebpushConfig(WebpushConfig.builder()
                                .setFcmOptions(WebpushFcmOptions.withLink(link))
                                .build())
                        .build()
        ).toList();

        return batchSendNotifications(firebaseInstallationIds, messages, cleanupInvalidInstallations);
    }

    private FcmSendResult batchSendNotifications(
            List<String> firebaseInstallationIds,
            List<Message> messages,
            boolean cleanupInvalidInstallations
    ) {
        BatchSummary total = new BatchSummary();

        for (int start = 0; start < messages.size(); start += MAX_MESSAGES_PER_REQUEST) {
            int end = Math.min(start + MAX_MESSAGES_PER_REQUEST, messages.size());
            BatchSummary chunk = sendChunk(
                    firebaseInstallationIds.subList(start, end),
                    messages.subList(start, end)
            );
            total.merge(chunk);
        }

        if (cleanupInvalidInstallations) {
            removeInvalidInstallationsBestEffort(total.invalidFirebaseInstallationIds);
        }

        String errorMessage = total.errorCounts.isEmpty()
                ? null
                : "FCM delivery failures: " + total.errorCounts;

        if (!total.errorCounts.isEmpty()) {
            log.warn(
                    "FCM delivery contained failed responses: targets={}, successes={}, invalid={}, retryable={}, permanent={}, errorCodes={}",
                    firebaseInstallationIds.size(),
                    total.successCount,
                    total.invalidTargetCount,
                    total.retryableFailureCount,
                    total.permanentFailureCount,
                    total.errorCounts
            );
        }

        return FcmSendResult.fromCounts(
                firebaseInstallationIds.size(),
                total.successCount,
                total.invalidTargetCount,
                total.retryableFailureCount,
                total.permanentFailureCount,
                errorMessage
        );
    }

    private BatchSummary sendChunk(
            List<String> firebaseInstallationIds,
            List<Message> messages
    ) {
        try {
            BatchResponse batchResponse = firebaseMessaging.sendEach(messages);
            return summarizeBatch(firebaseInstallationIds, batchResponse);
        } catch (FirebaseMessagingException exception) {
            MessagingErrorCode errorCode = exception.getMessagingErrorCode();

            log.error(
                    "FCM batch request failed: targets={}, errorCode={}, message={}",
                    firebaseInstallationIds.size(),
                    errorCode,
                    exception.getMessage(),
                    exception
            );

            if (isRetryable(errorCode)) {
                return BatchSummary.retryableFailure(
                        firebaseInstallationIds.size(),
                        errorCodeName(errorCode)
                );
            }

            return BatchSummary.permanentFailure(
                    firebaseInstallationIds.size(),
                    errorCodeName(errorCode)
            );
        }
    }

    private BatchSummary summarizeBatch(
            List<String> firebaseInstallationIds,
            BatchResponse batchResponse
    ) {
        List<SendResponse> responses = batchResponse.getResponses();
        if (responses.size() != firebaseInstallationIds.size()) {
            throw new IllegalStateException(
                    "FCM returned a different number of responses than submitted messages."
            );
        }

        BatchSummary summary = new BatchSummary();

        for (int index = 0; index < responses.size(); index++) {
            SendResponse response = responses.get(index);
            if (response.isSuccessful()) {
                summary.successCount++;
                continue;
            }

            FirebaseMessagingException exception = response.getException();
            MessagingErrorCode errorCode = exception == null ? null : exception.getMessagingErrorCode();
            summary.errorCounts.merge(errorCodeName(errorCode), 1, Integer::sum);

            if (errorCode == MessagingErrorCode.UNREGISTERED) {
                summary.invalidTargetCount++;
                summary.invalidFirebaseInstallationIds.add(firebaseInstallationIds.get(index));
            } else if (isRetryable(errorCode)) {
                summary.retryableFailureCount++;
            } else {
                summary.permanentFailureCount++;
            }
        }

        return summary;
    }

    private void removeInvalidInstallationsBestEffort(
            List<String> invalidFirebaseInstallationIds
    ) {
        if (invalidFirebaseInstallationIds.isEmpty()) {
            return;
        }

        try {
            notificationDeviceRepository.deleteAllByFirebaseInstallationIdIn(
                    invalidFirebaseInstallationIds
            );
        } catch (RuntimeException exception) {
            log.error(
                    "Failed to remove {} invalid FCM installations.",
                    invalidFirebaseInstallationIds.size(),
                    exception
            );
        }
    }

    private static final class BatchSummary {
        private int successCount;
        private int invalidTargetCount;
        private int retryableFailureCount;
        private int permanentFailureCount;
        private final List<String> invalidFirebaseInstallationIds = new ArrayList<>();
        private final Map<String, Integer> errorCounts = new LinkedHashMap<>();

        private static BatchSummary retryableFailure(int targetCount, String errorCode) {
            BatchSummary summary = new BatchSummary();
            summary.retryableFailureCount = targetCount;
            summary.errorCounts.put(errorCode, targetCount);
            return summary;
        }

        private static BatchSummary permanentFailure(int targetCount, String errorCode) {
            BatchSummary summary = new BatchSummary();
            summary.permanentFailureCount = targetCount;
            summary.errorCounts.put(errorCode, targetCount);
            return summary;
        }

        private void merge(BatchSummary other) {
            successCount += other.successCount;
            invalidTargetCount += other.invalidTargetCount;
            retryableFailureCount += other.retryableFailureCount;
            permanentFailureCount += other.permanentFailureCount;
            invalidFirebaseInstallationIds.addAll(other.invalidFirebaseInstallationIds);
            other.errorCounts.forEach((errorCode, count) ->
                    errorCounts.merge(errorCode, count, Integer::sum)
            );
        }
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
