package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmApiClient;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmSendResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

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
    void createsBothPushSendersWhenFirebaseIsEnabled() {
        contextRunner
                .withPropertyValues("app.firebase.enabled=true")
                .run(context -> assertThat(context)
                        .hasSingleBean(FcmPushMobileNotificationSender.class)
                        .hasSingleBean(FcmWebPushNotificationSender.class));
    }

    @Configuration(proxyBeanMethods = false)
    @Import({FcmPushMobileNotificationSender.class, FcmWebPushNotificationSender.class})
    static class PushSenderTestConfiguration {

        @Bean
        FcmApiClient fcmApiClient() {
            return new FcmApiClient() {
                @Override
                public FcmSendResult sendNotificationToSingleUserMobile(
                        com.mazurek.eventOrganizer.notification.domain.Notification notification
                ) {
                    return FcmSendResult.successful(1);
                }

                @Override
                public FcmSendResult sendNotificationToSingleUserWeb(
                        com.mazurek.eventOrganizer.notification.domain.Notification notification
                ) {
                    return FcmSendResult.successful(1);
                }
            };
        }
    }
}
