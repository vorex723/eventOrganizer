package com.mazurek.eventOrganizer.config;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.mazurek.eventOrganizer.config.properties.FirebaseProperties;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.LocalFcmApiClient;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.TestFcmApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Firebase config profile integration tests:")
class FirebaseConfigProfileIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(FirebaseTestConfiguration.class);

    @Test
    void whenLocalProfileRunsShouldUseSimulatedFcmWithoutCredentials() {
        contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("local"))
                .withPropertyValues("app.firebase.enabled=true")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(LocalFcmApiClient.class)
                        .doesNotHaveBean(TestFcmApiClient.class)
                        .doesNotHaveBean(FirebaseApp.class)
                        .doesNotHaveBean(FirebaseMessaging.class));
    }

    @Test
    void whenTestProfileRunsShouldUseSimulatedFcmWithoutCredentials() {
        contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("test"))
                .withPropertyValues("app.firebase.enabled=true")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(TestFcmApiClient.class)
                        .doesNotHaveBean(LocalFcmApiClient.class)
                        .doesNotHaveBean(FirebaseApp.class)
                        .doesNotHaveBean(FirebaseMessaging.class));
    }

    @Test
    void whenProductionFirebaseIsDisabledShouldNotRequireCredentials() {
        contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("production"))
                .withPropertyValues("app.firebase.enabled=false")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(FirebaseApp.class)
                        .doesNotHaveBean(FirebaseMessaging.class));
    }

    @Test
    void whenProductionFirebaseIsEnabledWithoutCredentialsShouldRejectContext() {
        contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("production"))
                .withPropertyValues("app.firebase.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("app.firebase.service-account-location");
                });
    }

    @Test
    void whenFirebaseCredentialsAreOnClasspathShouldRejectContext() {
        contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("production"))
                .withPropertyValues(
                        "app.firebase.enabled=true",
                        "app.firebase.service-account-location=classpath:EventOrganizerFirebaseKey.json"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("app.firebase.service-account-location");
                });
    }

    @Test
    void whenFirebaseCredentialsFileIsMissingShouldRejectContext(@TempDir Path tempDir) {
        contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("production"))
                .withPropertyValues(
                        "app.firebase.enabled=true",
                        "app.firebase.service-account-location="
                                + tempDir.resolve("missing-firebase-key.json").toUri()
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("does not exist or is not readable");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(FirebaseProperties.class)
    @Import({FirebaseConfig.class, LocalFcmApiClient.class, TestFcmApiClient.class})
    static class FirebaseTestConfiguration {
    }
}
