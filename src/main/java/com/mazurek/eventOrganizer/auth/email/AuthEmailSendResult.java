package com.mazurek.eventOrganizer.auth.email;

public record AuthEmailSendResult(
        AuthEmailSendOutcome outcome,
        String providerMessageId,
        String errorMessage
) {
    public static AuthEmailSendResult sent(String providerMessageId) {
        return new AuthEmailSendResult(AuthEmailSendOutcome.SENT, providerMessageId, null);
    }

    public static AuthEmailSendResult retryableFailure(String errorMessage) {
        return new AuthEmailSendResult(AuthEmailSendOutcome.RETRYABLE_FAILURE, null, errorMessage);
    }

    public static AuthEmailSendResult permanentFailure(String errorMessage) {
        return new AuthEmailSendResult(AuthEmailSendOutcome.PERMANENT_FAILURE, null, errorMessage);
    }
}
