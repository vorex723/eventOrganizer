package com.mazurek.eventOrganizer.config.properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkerEnvironmentConfigurationUnitTest {

    @Test
    void absentEnvironmentVariablesPreserveWorkerDefaults() throws IOException {
        StandardEnvironment environment = environment(Map.of());

        assertThat(notifications(environment)).usingRecursiveComparison()
                .isEqualTo(new NotificationProperties());
        assertThat(authEmail(environment)).usingRecursiveComparison()
                .isEqualTo(new AuthProperties.Email());
        assertThat(environment.getProperty("app.notifications.devices.cleanup-cron"))
                .isEqualTo("0 0 3 * * *");
        assertThat(environment.getProperty("app.notifications.retention.cleanup-cron"))
                .isEqualTo("0 15 3 * * *");
        assertThat(environment.getProperty("app.auth.email.cleanup-cron"))
                .isEqualTo("0 30 3 * * *");
    }

    @Test
    void explicitNotificationEnvironmentVariablesBindAllWorkerSettings() throws IOException {
        StandardEnvironment environment = environment(Map.ofEntries(
                Map.entry("APP_NOTIFICATIONS_DELIVERY_WORKER_ENABLED", "false"),
                Map.entry("APP_NOTIFICATIONS_DELIVERY_POLL_DELAY", "12s"),
                Map.entry("APP_NOTIFICATIONS_DELIVERY_BATCH_SIZE", "25"),
                Map.entry("APP_NOTIFICATIONS_DELIVERY_MAX_ATTEMPTS", "3"),
                Map.entry("APP_NOTIFICATIONS_DELIVERY_RETRY_DELAYS", "2m,7m"),
                Map.entry("APP_NOTIFICATIONS_DELIVERY_PROCESSING_TIMEOUT", "20m"),
                Map.entry("APP_NOTIFICATIONS_DEVICES_CLEANUP_ENABLED", "false"),
                Map.entry("APP_NOTIFICATIONS_DEVICES_STALE_AFTER", "45d"),
                Map.entry("APP_NOTIFICATIONS_DEVICES_CLEANUP_CRON", "0 10 2 * * *"),
                Map.entry("APP_NOTIFICATIONS_EMAIL_ENABLED", "true"),
                Map.entry("APP_NOTIFICATIONS_RETENTION_CLEANUP_ENABLED", "false"),
                Map.entry("APP_NOTIFICATIONS_RETENTION_COMPLETED_FOR", "30d"),
                Map.entry("APP_NOTIFICATIONS_RETENTION_DEAD_FOR", "60d"),
                Map.entry("APP_NOTIFICATIONS_RETENTION_CLEANUP_CRON", "0 20 2 * * *"),
                Map.entry("APP_NOTIFICATIONS_RETENTION_BACKLOG_ALERT_THRESHOLD", "200")
        ));

        NotificationProperties settings = notifications(environment);
        assertThat(settings.getDelivery())
                .extracting("workerEnabled", "pollDelay", "batchSize", "maxAttempts", "retryDelays", "processingTimeout")
                .containsExactly(false, Duration.ofSeconds(12), 25, 3,
                        List.of(Duration.ofMinutes(2), Duration.ofMinutes(7)), Duration.ofMinutes(20));
        assertThat(settings.getDevices()).extracting("cleanupEnabled", "staleAfter", "cleanupCron")
                .containsExactly(false, Duration.ofDays(45), "0 10 2 * * *");
        assertThat(settings.getEmail().isEnabled()).isTrue();
        assertThat(settings.getRetention())
                .extracting("cleanupEnabled", "completedFor", "deadFor", "cleanupCron", "backlogAlertThreshold")
                .containsExactly(false, Duration.ofDays(30), Duration.ofDays(60), "0 20 2 * * *", 200);
        assertThat(environment.getProperty("app.notifications.delivery.poll-delay")).isEqualTo("12s");
        assertThat(environment.getProperty("app.notifications.devices.cleanup-cron")).isEqualTo("0 10 2 * * *");
        assertThat(environment.getProperty("app.notifications.retention.cleanup-cron")).isEqualTo("0 20 2 * * *");
    }

    @Test
    void explicitAuthEmailEnvironmentVariablesBindAllWorkerSettings() throws IOException {
        StandardEnvironment environment = environment(Map.ofEntries(
                Map.entry("APP_AUTH_EMAIL_WORKER_ENABLED", "false"),
                Map.entry("APP_AUTH_EMAIL_POLL_DELAY", "8s"),
                Map.entry("APP_AUTH_EMAIL_BATCH_SIZE", "50"),
                Map.entry("APP_AUTH_EMAIL_MAX_ATTEMPTS", "3"),
                Map.entry("APP_AUTH_EMAIL_RETRY_DELAYS", "3m,9m"),
                Map.entry("APP_AUTH_EMAIL_PROCESSING_TIMEOUT", "15m"),
                Map.entry("APP_AUTH_EMAIL_RESEND_COOLDOWN", "2m"),
                Map.entry("APP_AUTH_EMAIL_CLEANUP_ENABLED", "false"),
                Map.entry("APP_AUTH_EMAIL_RETENTION", "30d"),
                Map.entry("APP_AUTH_EMAIL_CLEANUP_CRON", "0 30 2 * * *")
        ));

        assertThat(authEmail(environment))
                .extracting("workerEnabled", "pollDelay", "batchSize", "maxAttempts", "retryDelays",
                        "processingTimeout", "resendCooldown", "cleanupEnabled", "retention", "cleanupCron")
                .containsExactly(false, Duration.ofSeconds(8), 50, 3,
                        List.of(Duration.ofMinutes(3), Duration.ofMinutes(9)), Duration.ofMinutes(15),
                        Duration.ofMinutes(2), false, Duration.ofDays(30), "0 30 2 * * *");
        assertThat(environment.getProperty("app.auth.email.poll-delay")).isEqualTo("8s");
        assertThat(environment.getProperty("app.auth.email.cleanup-cron")).isEqualTo("0 30 2 * * *");
    }

    @Test
    void higherPriorityCanonicalPropertiesStillOverrideExplicitEnvironmentVariables() throws IOException {
        StandardEnvironment environment = environment(Map.of(
                "APP_NOTIFICATIONS_DELIVERY_BATCH_SIZE", "25",
                "APP_AUTH_EMAIL_BATCH_SIZE", "50"
        ));
        environment.getPropertySources().addFirst(new MapPropertySource("commandLineOverrides", Map.of(
                "app.notifications.delivery.batch-size", 17,
                "app.auth.email.batch-size", 18
        )));

        assertThat(notifications(environment).getDelivery().getBatchSize()).isEqualTo(17);
        assertThat(authEmail(environment).getBatchSize()).isEqualTo(18);
    }

    @Test
    void localProfilePreservesDisabledNotificationWorkersByDefault() throws IOException {
        StandardEnvironment environment = environment(Map.of());
        environment.getPropertySources().addBefore("applicationDefaults", new PropertiesPropertySource("localProfile",
                PropertiesLoaderUtils.loadProperties(new FileSystemResource("src/main/resources/application-local.properties"))));

        assertThat(notifications(environment).getDelivery().isWorkerEnabled()).isFalse();
        assertThat(notifications(environment).getDevices().isCleanupEnabled()).isFalse();
    }

    @Test
    void testProfilePreservesDisabledNotificationMaintenanceByDefault() throws IOException {
        StandardEnvironment environment = environment(Map.of());
        environment.getPropertySources().addBefore("applicationDefaults", new PropertiesPropertySource("testProfile",
                PropertiesLoaderUtils.loadProperties(new FileSystemResource("src/test/resources/application-test.properties"))));

        NotificationProperties settings = notifications(environment);
        assertThat(settings.getDelivery().isWorkerEnabled()).isFalse();
        assertThat(settings.getDevices().isCleanupEnabled()).isFalse();
        assertThat(settings.getEmail().isEnabled()).isFalse();
        assertThat(settings.getRetention().isCleanupEnabled()).isFalse();
    }

    private StandardEnvironment environment(Map<String, Object> variables) throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource("testEnvironment", variables));
        environment.getPropertySources().addLast(new PropertiesPropertySource("applicationDefaults",
                PropertiesLoaderUtils.loadProperties(new FileSystemResource("src/main/resources/application.properties"))));
        ConfigurationPropertySources.attach(environment);
        return environment;
    }

    private NotificationProperties notifications(StandardEnvironment environment) {
        return Binder.get(environment).bind("app.notifications", NotificationProperties.class).get();
    }

    private AuthProperties.Email authEmail(StandardEnvironment environment) {
        return Binder.get(environment).bind("app.auth.email", AuthProperties.Email.class).get();
    }
}
