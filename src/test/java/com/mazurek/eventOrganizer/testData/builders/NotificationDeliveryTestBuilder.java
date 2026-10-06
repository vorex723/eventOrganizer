package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationDelivery;
import com.mazurek.eventOrganizer.notification.domain.NotificationDeliveryStatus;
import java.time.Instant;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.DeliveryFixtureConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class NotificationDeliveryTestBuilder {
    private UUID id = DeliveryFixtureConstants.DELIVERY_ID;
    private Notification notification;
    private boolean notificationSet;
    private NotificationChannel channel = DeliveryFixtureConstants.CHANNEL;
    private String targetKey = UserConstants.FIRST_USER_EMAIL;
    private String targetEmail = UserConstants.FIRST_USER_EMAIL;
    private UUID targetDeviceId = null;
    private String targetInstallationId = null;
    private NotificationDeliveryStatus status = DeliveryFixtureConstants.STATUS;
    private int attemptCount = DeliveryFixtureConstants.ATTEMPT_COUNT;
    private Instant nextAttemptAt = null;
    private Instant processingStartedAt = null;
    private UUID claimToken = null;
    private Instant sentAt = null;
    private String providerMessageId = null;
    private String lastError = null;
    private Instant createdAt = TimeConstants.NOW;

    public NotificationDeliveryTestBuilder id(UUID id) {
        this.id = id;
        return this;
    }

    public NotificationDeliveryTestBuilder notification(Notification notification) {
        this.notification = notification;
        this.notificationSet = true;
        return this;
    }

    public NotificationDeliveryTestBuilder channel(NotificationChannel channel) {
        this.channel = channel;
        return this;
    }

    public NotificationDeliveryTestBuilder targetKey(String targetKey) {
        this.targetKey = targetKey;
        return this;
    }

    public NotificationDeliveryTestBuilder targetEmail(String targetEmail) {
        this.targetEmail = targetEmail;
        return this;
    }

    public NotificationDeliveryTestBuilder targetDeviceId(UUID targetDeviceId) {
        this.targetDeviceId = targetDeviceId;
        return this;
    }

    public NotificationDeliveryTestBuilder targetInstallationId(String targetInstallationId) {
        this.targetInstallationId = targetInstallationId;
        return this;
    }

    public NotificationDeliveryTestBuilder status(NotificationDeliveryStatus status) {
        this.status = status;
        return this;
    }

    public NotificationDeliveryTestBuilder attemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
        return this;
    }

    public NotificationDeliveryTestBuilder nextAttemptAt(Instant nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
        return this;
    }

    public NotificationDeliveryTestBuilder processingStartedAt(Instant processingStartedAt) {
        this.processingStartedAt = processingStartedAt;
        return this;
    }

    public NotificationDeliveryTestBuilder claimToken(UUID claimToken) {
        this.claimToken = claimToken;
        return this;
    }

    public NotificationDeliveryTestBuilder sentAt(Instant sentAt) {
        this.sentAt = sentAt;
        return this;
    }

    public NotificationDeliveryTestBuilder providerMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
        return this;
    }

    public NotificationDeliveryTestBuilder lastError(String lastError) {
        this.lastError = lastError;
        return this;
    }

    public NotificationDeliveryTestBuilder createdAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }


    public NotificationDelivery build() {
        NotificationDelivery value = new NotificationDelivery();
        value.setId(id);
        value.setNotification(notificationSet ? notification : NotificationTestBuilder.eventUpdateNotification().build());
        value.setChannel(channel);
        value.setTargetKey(targetKey);
        value.setTargetEmail(targetEmail);
        value.setTargetDeviceId(targetDeviceId);
        value.setTargetInstallationId(targetInstallationId);
        value.setStatus(status);
        value.setAttemptCount(attemptCount);
        value.setNextAttemptAt(nextAttemptAt);
        value.setProcessingStartedAt(processingStartedAt);
        value.setClaimToken(claimToken);
        value.setSentAt(sentAt);
        value.setProviderMessageId(providerMessageId);
        value.setLastError(lastError);
        value.setCreatedAt(createdAt);
        return value;
    }
}
