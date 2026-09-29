package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmApiClient;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmSendResult;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeviceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FirebasePushSenderConditionUnitTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PushSenderTestConfiguration.class);

    @Test
    void doesNotCreatePushSendersWhenFirebaseIsDisabled() {
        contextRunner
                .withPropertyValues("app.firebase.enabled=false")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(FcmPushMobileNotificationSender.class)
                        .doesNotHaveBean(FcmWebPushNotificationSender.class));
    }

    @Test
    void disabledOrUnconfiguredPushSendersDoNotRequireDependencies() {
        ApplicationContextRunner sendersOnlyRunner = new ApplicationContextRunner()
                .withUserConfiguration(PushSendersOnlyTestConfiguration.class);

        sendersOnlyRunner
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(FcmPushMobileNotificationSender.class)
                        .doesNotHaveBean(FcmWebPushNotificationSender.class));

        sendersOnlyRunner
                .withPropertyValues("app.firebase.enabled=false")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(FcmPushMobileNotificationSender.class)
                        .doesNotHaveBean(FcmWebPushNotificationSender.class));
    }

    @Test
    void createsBothPushSendersWhenFirebaseIsEnabled() {
        contextRunner
                .withPropertyValues("app.firebase.enabled=true")
                .withBean(NotificationDeviceRepository.class, () -> mock(NotificationDeviceRepository.class))
                .run(context -> assertThat(context)
                        .hasSingleBean(FcmPushMobileNotificationSender.class)
                        .hasSingleBean(FcmWebPushNotificationSender.class));
    }

    @Test
    void doesNotCreateEnabledPushSendersWithoutDeviceRepository() {
        contextRunner
                .withPropertyValues("app.firebase.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class);
                });
    }

    @Test
    void enabledPushSendersRequireFcmClient() {
        new ApplicationContextRunner()
                .withUserConfiguration(PushSendersOnlyTestConfiguration.class)
                .withPropertyValues("app.firebase.enabled=true")
                .withBean(NotificationDeviceRepository.class, () -> mock(NotificationDeviceRepository.class))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({FcmPushMobileNotificationSender.class, FcmWebPushNotificationSender.class})
    static class PushSenderTestConfiguration {

        @Bean
        FcmApiClient fcmApiClient() {
            return new FcmApiClient() {
                @Override
                public FcmSendResult sendNotificationToInstallationMobile(
                        com.mazurek.eventOrganizer.notification.domain.Notification notification,
                        String firebaseInstallationId
                ) {
                    return FcmSendResult.successful(1);
                }

                @Override
                public FcmSendResult sendNotificationToInstallationWeb(
                        com.mazurek.eventOrganizer.notification.domain.Notification notification,
                        String firebaseInstallationId
                ) {
                    return FcmSendResult.successful(1);
                }
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import({FcmPushMobileNotificationSender.class, FcmWebPushNotificationSender.class})
    static class PushSendersOnlyTestConfiguration {
    }
}
