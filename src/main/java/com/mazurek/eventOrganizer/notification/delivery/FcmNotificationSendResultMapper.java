package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmSendResult;

final class FcmNotificationSendResultMapper {

    private FcmNotificationSendResultMapper() {
    }

    static NotificationSendResult map(FcmSendResult result) {
        return switch (result.outcome()) {
            case SENT -> NotificationSendResult.sent(null);
            case NO_TARGETS -> NotificationSendResult.skipped(result.errorMessage());
            case RETRYABLE_FAILURE -> NotificationSendResult.retryableFailure(result.errorMessage());
            case PERMANENT_FAILURE -> NotificationSendResult.permanentFailure(result.errorMessage());
        };
    }
}
