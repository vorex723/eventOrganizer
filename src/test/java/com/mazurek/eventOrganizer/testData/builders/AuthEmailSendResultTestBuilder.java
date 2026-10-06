package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.auth.email.AuthEmailSendOutcome;
import com.mazurek.eventOrganizer.auth.email.AuthEmailSendResult;

import static com.mazurek.eventOrganizer.testData.TestConstants.SendResultConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class AuthEmailSendResultTestBuilder {
    private AuthEmailSendOutcome outcome = SendResultConstants.AUTHEMAILSENDRESULT_OUTCOME;
    private String providerMessageId = SendResultConstants.PROVIDER_MESSAGE_ID;
    private String errorMessage = null;

    public AuthEmailSendResultTestBuilder outcome(AuthEmailSendOutcome outcome) {
        this.outcome = outcome;
        return this;
    }

    public AuthEmailSendResultTestBuilder providerMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
        return this;
    }

    public AuthEmailSendResultTestBuilder errorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }


    public AuthEmailSendResult build() {
        return new AuthEmailSendResult(
                outcome,
                providerMessageId,
                errorMessage
        );
    }
}
