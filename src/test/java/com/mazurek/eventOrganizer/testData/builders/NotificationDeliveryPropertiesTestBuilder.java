package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.config.properties.NotificationProperties.Delivery;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.mazurek.eventOrganizer.testData.TestConstants.PropertyFixtureConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class NotificationDeliveryPropertiesTestBuilder {
    private boolean workerEnabled = PropertyFixtureConstants.ENABLED;
    private Duration pollDelay = PropertyFixtureConstants.POLL_DELAY;
    private int batchSize = PropertyFixtureConstants.BATCH_SIZE;
    private int maxAttempts = PropertyFixtureConstants.MAX_ATTEMPTS;
    private List<Duration> retryDelays = PropertyFixtureConstants.RETRY_DELAYS;
    private Duration processingTimeout = PropertyFixtureConstants.PROCESSING_TIMEOUT;

    public NotificationDeliveryPropertiesTestBuilder workerEnabled(boolean workerEnabled) {
        this.workerEnabled = workerEnabled;
        return this;
    }

    public NotificationDeliveryPropertiesTestBuilder pollDelay(Duration pollDelay) {
        this.pollDelay = pollDelay;
        return this;
    }

    public NotificationDeliveryPropertiesTestBuilder batchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    public NotificationDeliveryPropertiesTestBuilder maxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
        return this;
    }

    public NotificationDeliveryPropertiesTestBuilder retryDelays(List<Duration> retryDelays) {
        this.retryDelays = retryDelays == null ? null : new ArrayList<>(retryDelays);
        return this;
    }

    public NotificationDeliveryPropertiesTestBuilder processingTimeout(Duration processingTimeout) {
        this.processingTimeout = processingTimeout;
        return this;
    }

    public static Delivery copyOf(Delivery source) {
        if (source == null) return null;
        return new NotificationDeliveryPropertiesTestBuilder()
                .workerEnabled(source.isWorkerEnabled())
                .pollDelay(source.getPollDelay())
                .batchSize(source.getBatchSize())
                .maxAttempts(source.getMaxAttempts())
                .retryDelays(source.getRetryDelays())
                .processingTimeout(source.getProcessingTimeout())
                .build();
    }


    public Delivery build() {
        Delivery value = new Delivery();
        value.setWorkerEnabled(workerEnabled);
        value.setPollDelay(pollDelay);
        value.setBatchSize(batchSize);
        value.setMaxAttempts(maxAttempts);
        value.setRetryDelays(retryDelays == null ? null : new ArrayList<>(retryDelays));
        value.setProcessingTimeout(processingTimeout);
        return value;
    }
}
