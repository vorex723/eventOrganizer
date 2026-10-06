package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.testData.builders.FcmSendResultTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.NotificationSendResultTestBuilder;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmSendOutcome;
import com.google.firebase.messaging.FirebaseMessaging;
import com.mazurek.eventOrganizer.config.ProductionProfileExclusivityConfig;
import com.mazurek.eventOrganizer.config.properties.MailProperties;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmApiClient;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmApiClientProdImpl;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmSendResult;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.LocalFcmApiClient;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.TestFcmApiClient;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeviceRepository;
import com.mazurek.eventOrganizer.utils.NotificationResourceLinkResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("Notification channel bean condition integration tests:")
class NotificationChannelBeanConditionIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(NotificationChannelTestConfiguration.class)
            .withBean(FirebaseMessaging.class, () -> mock(FirebaseMessaging.class))
            .withBean(NotificationDeviceRepository.class, () -> mock(NotificationDeviceRepository.class))
            .withBean(NotificationResourceLinkResolver.class, () -> mock(NotificationResourceLinkResolver.class))
            .withBean(JavaMailSender.class, () -> mock(JavaMailSender.class))
            .withBean(MailProperties.class, () -> mock(MailProperties.class));

    @ParameterizedTest(name = "profile={0}, firebase={1}, email={2}")
    @CsvSource({
            "production, false, false",
            "production, true, false",
            "production, false, true",
            "production, true, true",
            "local, false, false",
            "local, true, false",
            "local, false, true",
            "local, true, true",
            "test, false, false",
            "test, true, false",
            "test, false, true",
            "test, true, true"
    })
    void whenProfileAndFlagsAreConfiguredShouldWireOnlyEnabledSenders(
            String profile,
            boolean firebaseEnabled,
            boolean emailEnabled
    ) {
        contextRunner
                .withInitializer(context -> context.getEnvironment().setActiveProfiles(profile))
                .withPropertyValues(
                        "app.firebase.enabled=" + firebaseEnabled,
                        "app.notifications.email.enabled=" + emailEnabled
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeansOfType(NotificationSender.class))
                            .hasSize((firebaseEnabled ? 2 : 0) + (emailEnabled ? 1 : 0));

                    if (firebaseEnabled) {
                        assertThat(context)
                                .hasSingleBean(FcmPushMobileNotificationSender.class)
                                .hasSingleBean(FcmWebPushNotificationSender.class);
                    } else {
                        assertThat(context)
                                .doesNotHaveBean(FcmPushMobileNotificationSender.class)
                                .doesNotHaveBean(FcmWebPushNotificationSender.class);
                    }

                    if (emailEnabled) {
                        assertThat(context).hasSingleBean(EmailNotificationSender.class);
                    } else {
                        assertThat(context).doesNotHaveBean(EmailNotificationSender.class);
                    }

                    if (profile.equals("production")) {
                        assertThat(context)
                                .doesNotHaveBean(LocalFcmApiClient.class)
                                .doesNotHaveBean(TestFcmApiClient.class)
                                .doesNotHaveBean(LocalNotificationEmailClient.class)
                                .doesNotHaveBean(TestNotificationEmailClient.class);
                        if (firebaseEnabled) {
                            assertThat(context)
                                    .hasSingleBean(FcmApiClientProdImpl.class)
                                    .hasSingleBean(FcmApiClient.class);
                        } else {
                            assertThat(context)
                                    .doesNotHaveBean(FcmApiClientProdImpl.class)
                                    .doesNotHaveBean(FcmApiClient.class);
                        }
                        if (emailEnabled) {
                            assertThat(context)
                                    .hasSingleBean(SmtpNotificationEmailClient.class)
                                    .hasSingleBean(NotificationEmailClient.class);
                        } else {
                            assertThat(context)
                                    .doesNotHaveBean(SmtpNotificationEmailClient.class)
                                    .doesNotHaveBean(NotificationEmailClient.class);
                        }
                    } else if (profile.equals("local")) {
                        assertThat(context)
                                .hasSingleBean(LocalFcmApiClient.class)
                                .hasSingleBean(FcmApiClient.class)
                                .hasSingleBean(LocalNotificationEmailClient.class)
                                .hasSingleBean(NotificationEmailClient.class)
                                .doesNotHaveBean(TestFcmApiClient.class)
                                .doesNotHaveBean(TestNotificationEmailClient.class)
                                .doesNotHaveBean(FcmApiClientProdImpl.class)
                                .doesNotHaveBean(SmtpNotificationEmailClient.class);
                        LocalFcmApiClient fcmClient = context.getBean(LocalFcmApiClient.class);
                        Notification notification = mock(Notification.class);
                        assertThat(fcmClient.sendNotificationToInstallationMobile(notification, "installation"))
                                .isEqualTo(new FcmSendResultTestBuilder()
                                        .outcome(FcmSendOutcome.SENT)
                                        .targetCount(1)
                                        .successCount(1)
                                        .build());
                        assertThat(fcmClient.sendNotificationToInstallationWeb(notification, "installation"))
                                .isEqualTo(new FcmSendResultTestBuilder()
                                        .outcome(FcmSendOutcome.SENT)
                                        .targetCount(1)
                                        .successCount(1)
                                        .build());
                        assertThat(context.getBean(LocalNotificationEmailClient.class)
                                .send("user@example.com", "title", "body"))
                                .isEqualTo(new NotificationSendResultTestBuilder()
                                        .outcome(NotificationSendOutcome.SENT)
                                        .providerMessageId(null)
                                        .errorMessage(null)
                                        .build());
                    } else {
                        assertThat(context)
                                .hasSingleBean(TestFcmApiClient.class)
                                .hasSingleBean(FcmApiClient.class)
                                .hasSingleBean(TestNotificationEmailClient.class)
                                .hasSingleBean(NotificationEmailClient.class)
                                .doesNotHaveBean(LocalFcmApiClient.class)
                                .doesNotHaveBean(LocalNotificationEmailClient.class)
                                .doesNotHaveBean(FcmApiClientProdImpl.class)
                                .doesNotHaveBean(SmtpNotificationEmailClient.class);
                        TestFcmApiClient testFcm = context.getBean(TestFcmApiClient.class);
                        TestNotificationEmailClient testEmail = context.getBean(TestNotificationEmailClient.class);
                        var failedPush = new FcmSendResultTestBuilder().outcome(FcmSendOutcome.PERMANENT_FAILURE)
                                .targetCount(1).successCount(0).permanentFailureCount(1).errorMessage("Configured failure").build();
                        var failedEmail = new NotificationSendResultTestBuilder().outcome(NotificationSendOutcome.PERMANENT_FAILURE)
                                .providerMessageId(null).errorMessage("Configured failure").build();
                        testFcm.configureMobileResult(failedPush);
                        testFcm.configureWebResult(failedPush);
                        testEmail.configureResult(failedEmail);
                        Notification notification = mock(Notification.class);
                        assertThat(testFcm.sendNotificationToInstallationMobile(notification, "installation")).isEqualTo(failedPush);
                        assertThat(testFcm.sendNotificationToInstallationWeb(notification, "installation")).isEqualTo(failedPush);
                        assertThat(testEmail.send("user@example.com", "title", "body")).isEqualTo(failedEmail);
                        testFcm.reset();
                        testEmail.reset();
                        assertThat(testFcm.sendNotificationToInstallationMobile(notification, "installation"))
                                .isEqualTo(new FcmSendResultTestBuilder().build());
                        assertThat(testFcm.sendNotificationToInstallationWeb(notification, "installation"))
                                .isEqualTo(new FcmSendResultTestBuilder().build());
                        assertThat(testEmail.send("user@example.com", "title", "body"))
                                .isEqualTo(new NotificationSendResultTestBuilder().providerMessageId(null).errorMessage(null).build());
                    }
                });
    }

    @ParameterizedTest(name = "profiles=production,{0}, firebase={1}, email={2}")
    @CsvSource({
            "local, false, false",
            "local, true, true",
            "test, false, false",
            "test, true, true"
    })
    void whenProductionIsMixedWithOtherProfilesShouldRejectContext(
            String otherProfile,
            boolean firebaseEnabled,
            boolean emailEnabled
    ) {
        contextRunner
                .withInitializer(context -> context.getEnvironment()
                        .setActiveProfiles("production", otherProfile))
                .withPropertyValues(
                        "app.firebase.enabled=" + firebaseEnabled,
                        "app.notifications.email.enabled=" + emailEnabled
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("The production profile cannot be combined with local or test.");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({
            ProductionProfileExclusivityConfig.class,
            FcmApiClientProdImpl.class,
            LocalFcmApiClient.class,
            TestFcmApiClient.class,
            SmtpNotificationEmailClient.class,
            LocalNotificationEmailClient.class,
            TestNotificationEmailClient.class,
            FcmPushMobileNotificationSender.class,
            FcmWebPushNotificationSender.class,
            EmailNotificationSender.class
    })
    static class NotificationChannelTestConfiguration {
    }
}
