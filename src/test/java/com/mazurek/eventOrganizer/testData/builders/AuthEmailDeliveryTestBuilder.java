package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.auth.email.AuthEmailDelivery;
import com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryStatus;
import com.mazurek.eventOrganizer.auth.email.AuthEmailType;
import java.time.Instant;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.DeliveryFixtureConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class AuthEmailDeliveryTestBuilder {
    private UUID id = DeliveryFixtureConstants.DELIVERY_ID;
    private UUID userId = UserConstants.FIRST_USER_ID;
    private String recipientEmail = UserConstants.FIRST_USER_EMAIL;
    private AuthEmailType type = DeliveryFixtureConstants.AUTH_TYPE;
    private String encryptedToken = DeliveryFixtureConstants.ENCRYPTED_TOKEN;
    private AuthEmailDeliveryStatus status = DeliveryFixtureConstants.AUTH_STATUS;
    private int attemptCount = DeliveryFixtureConstants.ATTEMPT_COUNT;
    private Instant nextAttemptAt = null;
    private Instant processingStartedAt = null;
    private UUID claimToken = null;
    private Instant sentAt = null;
    private String lastError = null;
    private Instant createdAt = TimeConstants.NOW;

    public AuthEmailDeliveryTestBuilder id(UUID id) {
        this.id = id;
        return this;
    }

    public AuthEmailDeliveryTestBuilder userId(UUID userId) {
        this.userId = userId;
        return this;
    }

    public AuthEmailDeliveryTestBuilder recipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
        return this;
    }

    public AuthEmailDeliveryTestBuilder type(AuthEmailType type) {
        this.type = type;
        return this;
    }

    public AuthEmailDeliveryTestBuilder encryptedToken(String encryptedToken) {
        this.encryptedToken = encryptedToken;
        return this;
    }

    public AuthEmailDeliveryTestBuilder status(AuthEmailDeliveryStatus status) {
        this.status = status;
        return this;
    }

    public AuthEmailDeliveryTestBuilder attemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
        return this;
    }

    public AuthEmailDeliveryTestBuilder nextAttemptAt(Instant nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
        return this;
    }

    public AuthEmailDeliveryTestBuilder processingStartedAt(Instant processingStartedAt) {
        this.processingStartedAt = processingStartedAt;
        return this;
    }

    public AuthEmailDeliveryTestBuilder claimToken(UUID claimToken) {
        this.claimToken = claimToken;
        return this;
    }

    public AuthEmailDeliveryTestBuilder sentAt(Instant sentAt) {
        this.sentAt = sentAt;
        return this;
    }

    public AuthEmailDeliveryTestBuilder lastError(String lastError) {
        this.lastError = lastError;
        return this;
    }

    public AuthEmailDeliveryTestBuilder createdAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }


    public AuthEmailDelivery build() {
        AuthEmailDelivery value = new AuthEmailDelivery();
        value.setId(id);
        value.setUserId(userId);
        value.setRecipientEmail(recipientEmail);
        value.setType(type);
        value.setEncryptedToken(encryptedToken);
        value.setStatus(status);
        value.setAttemptCount(attemptCount);
        value.setNextAttemptAt(nextAttemptAt);
        value.setProcessingStartedAt(processingStartedAt);
        value.setClaimToken(claimToken);
        value.setSentAt(sentAt);
        value.setLastError(lastError);
        value.setCreatedAt(createdAt);
        return value;
    }
}
