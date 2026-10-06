package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.notification.delivery.NotificationSendOutcome;
import com.mazurek.eventOrganizer.notification.delivery.NotificationSendResult;

import static com.mazurek.eventOrganizer.testData.TestConstants.SendResultConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class NotificationSendResultTestBuilder {
    private NotificationSendOutcome outcome = SendResultConstants.NOTIFICATIONSENDRESULT_OUTCOME;
    private String providerMessageId = SendResultConstants.PROVIDER_MESSAGE_ID;
    private String errorMessage = null;

    public NotificationSendResultTestBuilder outcome(NotificationSendOutcome outcome) {
        this.outcome = outcome;
        return this;
    }

    public NotificationSendResultTestBuilder providerMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
        return this;
    }

    public NotificationSendResultTestBuilder errorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }


    public NotificationSendResult build() {
        return new NotificationSendResult(
                outcome,
                providerMessageId,
                errorMessage
        );
    }
}
