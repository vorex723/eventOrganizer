package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmSendOutcome;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmSendResult;

import static com.mazurek.eventOrganizer.testData.TestConstants.SendResultConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class FcmSendResultTestBuilder {
    private FcmSendOutcome outcome = SendResultConstants.FCM_OUTCOME;
    private int targetCount = SendResultConstants.TARGET_COUNT;
    private int successCount = SendResultConstants.SUCCESS_COUNT;
    private int invalidTargetCount = SendResultConstants.FAILURE_COUNT;
    private int retryableFailureCount = SendResultConstants.FAILURE_COUNT;
    private int permanentFailureCount = SendResultConstants.FAILURE_COUNT;
    private String errorMessage = null;

    public FcmSendResultTestBuilder outcome(FcmSendOutcome outcome) {
        this.outcome = outcome;
        return this;
    }

    public FcmSendResultTestBuilder targetCount(int targetCount) {
        this.targetCount = targetCount;
        return this;
    }

    public FcmSendResultTestBuilder successCount(int successCount) {
        this.successCount = successCount;
        return this;
    }

    public FcmSendResultTestBuilder invalidTargetCount(int invalidTargetCount) {
        this.invalidTargetCount = invalidTargetCount;
        return this;
    }

    public FcmSendResultTestBuilder retryableFailureCount(int retryableFailureCount) {
        this.retryableFailureCount = retryableFailureCount;
        return this;
    }

    public FcmSendResultTestBuilder permanentFailureCount(int permanentFailureCount) {
        this.permanentFailureCount = permanentFailureCount;
        return this;
    }

    public FcmSendResultTestBuilder errorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }


    public FcmSendResult build() {
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
