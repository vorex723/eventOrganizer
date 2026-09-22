package com.mazurek.eventOrganizer.notification.delivery;

public record NotificationSendResult(
        NotificationSendOutcome outcome,
        String providerMessageId,
        String errorMessage
) {
    public static NotificationSendResult sent(String providerMessageId){
        return new NotificationSendResult(NotificationSendOutcome.SENT, providerMessageId, null);
    }

    public static NotificationSendResult skipped(String reason) {
        return new NotificationSendResult(NotificationSendOutcome.SKIPPED, null, reason);
    }

    public static NotificationSendResult retryableFailure(String errorMessage) {
        return new NotificationSendResult(NotificationSendOutcome.RETRYABLE_FAILURE, null, errorMessage);
    }

    public static NotificationSendResult permanentFailure(String errorMessage) {
        return new NotificationSendResult(NotificationSendOutcome.PERMANENT_FAILURE, null, errorMessage);
    }

    public static NotificationSendResult failed(String errorMessage){
        return retryableFailure(errorMessage);
    }

    public boolean success() {
        return outcome == NotificationSendOutcome.SENT;
    }
}
