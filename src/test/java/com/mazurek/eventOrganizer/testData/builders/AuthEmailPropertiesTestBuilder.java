package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.config.properties.AuthProperties.Email;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.mazurek.eventOrganizer.testData.TestConstants.PropertyFixtureConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class AuthEmailPropertiesTestBuilder {
    private boolean workerEnabled = PropertyFixtureConstants.ENABLED;
    private Duration pollDelay = PropertyFixtureConstants.POLL_DELAY;
    private int batchSize = PropertyFixtureConstants.BATCH_SIZE;
    private int maxAttempts = PropertyFixtureConstants.MAX_ATTEMPTS;
    private List<Duration> retryDelays = PropertyFixtureConstants.RETRY_DELAYS;
    private Duration processingTimeout = PropertyFixtureConstants.PROCESSING_TIMEOUT;
    private Duration resendCooldown = PropertyFixtureConstants.RESEND_COOLDOWN;
    private boolean cleanupEnabled = PropertyFixtureConstants.ENABLED;
    private Duration retention = PropertyFixtureConstants.RETENTION;
    private String cleanupCron = PropertyFixtureConstants.AUTH_CLEANUP_CRON;

    public AuthEmailPropertiesTestBuilder workerEnabled(boolean workerEnabled) {
        this.workerEnabled = workerEnabled;
        return this;
    }

    public AuthEmailPropertiesTestBuilder pollDelay(Duration pollDelay) {
        this.pollDelay = pollDelay;
        return this;
    }

    public AuthEmailPropertiesTestBuilder batchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    public AuthEmailPropertiesTestBuilder maxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
        return this;
    }

    public AuthEmailPropertiesTestBuilder retryDelays(List<Duration> retryDelays) {
        this.retryDelays = retryDelays == null ? null : new ArrayList<>(retryDelays);
        return this;
    }

    public AuthEmailPropertiesTestBuilder processingTimeout(Duration processingTimeout) {
        this.processingTimeout = processingTimeout;
        return this;
    }

    public AuthEmailPropertiesTestBuilder resendCooldown(Duration resendCooldown) {
        this.resendCooldown = resendCooldown;
        return this;
    }

    public AuthEmailPropertiesTestBuilder cleanupEnabled(boolean cleanupEnabled) {
        this.cleanupEnabled = cleanupEnabled;
        return this;
    }

    public AuthEmailPropertiesTestBuilder retention(Duration retention) {
        this.retention = retention;
        return this;
    }

    public AuthEmailPropertiesTestBuilder cleanupCron(String cleanupCron) {
        this.cleanupCron = cleanupCron;
        return this;
    }

    public static Email copyOf(Email source) {
        if (source == null) return null;
        return new AuthEmailPropertiesTestBuilder()
                .workerEnabled(source.isWorkerEnabled())
                .pollDelay(source.getPollDelay())
                .batchSize(source.getBatchSize())
                .maxAttempts(source.getMaxAttempts())
                .retryDelays(source.getRetryDelays())
                .processingTimeout(source.getProcessingTimeout())
                .resendCooldown(source.getResendCooldown())
                .cleanupEnabled(source.isCleanupEnabled())
                .retention(source.getRetention())
                .cleanupCron(source.getCleanupCron())
                .build();
    }


    public Email build() {
        Email value = new Email();
        value.setWorkerEnabled(workerEnabled);
        value.setPollDelay(pollDelay);
        value.setBatchSize(batchSize);
        value.setMaxAttempts(maxAttempts);
        value.setRetryDelays(retryDelays == null ? null : new ArrayList<>(retryDelays));
        value.setProcessingTimeout(processingTimeout);
        value.setResendCooldown(resendCooldown);
        value.setCleanupEnabled(cleanupEnabled);
        value.setRetention(retention);
        value.setCleanupCron(cleanupCron);
        return value;
    }
}
