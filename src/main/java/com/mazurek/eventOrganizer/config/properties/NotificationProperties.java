package com.mazurek.eventOrganizer.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.notifications")
public class NotificationProperties {

    @Valid
    private Delivery delivery = new Delivery();

    @Valid
    private Devices devices = new Devices();

    @Valid
    private Email email = new Email();

    @Getter
    @Setter
    public static class Delivery {

        private boolean workerEnabled = true;

        @NotNull
        private Duration pollDelay = Duration.ofSeconds(5);

        @Min(1)
        private int batchSize = 100;

        @Min(1)
        private int maxAttempts = 6;

        @NotEmpty
        private List<@NotNull Duration> retryDelays = new ArrayList<>(List.of(
                Duration.ofMinutes(1),
                Duration.ofMinutes(5),
                Duration.ofMinutes(15),
                Duration.ofMinutes(30),
                Duration.ofMinutes(60)
        ));

        @NotNull
        private Duration processingTimeout = Duration.ofMinutes(10);

        @AssertTrue(message = "notification delivery durations must be positive and retry-delays must cover every retry")
        public boolean isValidTimingConfiguration() {
            return pollDelay != null
                    && !pollDelay.isZero()
                    && !pollDelay.isNegative()
                    && processingTimeout != null
                    && !processingTimeout.isZero()
                    && !processingTimeout.isNegative()
                    && retryDelays != null
                    && retryDelays.size() >= Math.max(0, maxAttempts - 1)
                    && retryDelays.stream().allMatch(delay ->
                            delay != null && !delay.isZero() && !delay.isNegative()
                    );
        }
    }

    @Getter
    @Setter
    public static class Devices {

        private boolean cleanupEnabled = true;

        @NotNull
        private Duration staleAfter = Duration.ofDays(90);

        @NotBlank
        private String cleanupCron = "0 0 3 * * *";

        @AssertTrue(message = "notification device stale-after must be positive")
        public boolean isValidStaleAfter() {
            return staleAfter != null && !staleAfter.isZero() && !staleAfter.isNegative();
        }
    }

    @Getter
    @Setter
    public static class Email {
        private boolean enabled;
    }
}
