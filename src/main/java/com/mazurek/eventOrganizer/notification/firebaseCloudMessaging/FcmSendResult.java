package com.mazurek.eventOrganizer.notification.firebaseCloudMessaging;

import java.util.Objects;

public record FcmSendResult(
        FcmSendOutcome outcome,
        int targetCount,
        int successCount,
        int invalidTargetCount,
        int retryableFailureCount,
        int permanentFailureCount,
        String errorMessage
) {
    public FcmSendResult {
        Objects.requireNonNull(outcome, "FCM send outcome must not be null.");

        if (targetCount < 0
                || successCount < 0
                || invalidTargetCount < 0
                || retryableFailureCount < 0
                || permanentFailureCount < 0) {
            throw new IllegalArgumentException("FCM send result counts must not be negative.");
        }

        int classifiedTargetCount = successCount
                + invalidTargetCount
                + retryableFailureCount
                + permanentFailureCount;

        if (classifiedTargetCount != targetCount) {
            throw new IllegalArgumentException("Every FCM target must have exactly one result.");
        }
    }

    public static FcmSendResult noTargets() {
        return new FcmSendResult(
                FcmSendOutcome.NO_TARGETS,
                0,
                0,
                0,
                0,
                0,
                "No registered installations for this notification channel."
        );
    }

    public static FcmSendResult successful(int targetCount) {
        return fromCounts(targetCount, targetCount, 0, 0, 0, null);
    }

    public static FcmSendResult retryableFailure(int targetCount, String errorMessage) {
        return fromCounts(targetCount, 0, 0, targetCount, 0, errorMessage);
    }

    public static FcmSendResult permanentFailure(int targetCount, String errorMessage) {
        return fromCounts(targetCount, 0, 0, 0, targetCount, errorMessage);
    }

    public static FcmSendResult fromCounts(
            int targetCount,
            int successCount,
            int invalidTargetCount,
            int retryableFailureCount,
            int permanentFailureCount,
            String errorMessage
    ) {
        FcmSendOutcome outcome;

        if (targetCount == 0 || invalidTargetCount == targetCount) {
            outcome = FcmSendOutcome.NO_TARGETS;
        } else if (successCount > 0) {
            outcome = FcmSendOutcome.SENT;
        } else if (retryableFailureCount > 0) {
            outcome = FcmSendOutcome.RETRYABLE_FAILURE;
        } else {
            outcome = FcmSendOutcome.PERMANENT_FAILURE;
        }

        return new FcmSendResult(
                outcome,
                targetCount,
                successCount,
                invalidTargetCount,
                retryableFailureCount,
                permanentFailureCount,
                errorMessage
        );
    }
}
