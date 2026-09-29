package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.notification.domain.DevicePlatform;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationDelivery;
import com.mazurek.eventOrganizer.notification.domain.NotificationDeliveryStatus;
import com.mazurek.eventOrganizer.notification.domain.NotificationDevice;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.TestFcmApiClient;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmSendResult;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeliveryRepository;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeviceRepository;
import com.mazurek.eventOrganizer.notification.repository.NotificationRepository;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.builders.NotificationTestBuilder;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.stream.Stream;

import static com.mazurek.eventOrganizer.notification.domain.NotificationChannel.PUSH_MOBILE;
import static com.mazurek.eventOrganizer.notification.domain.NotificationDeliveryStatus.DEAD;
import static com.mazurek.eventOrganizer.notification.domain.NotificationDeliveryStatus.FAILED;
import static com.mazurek.eventOrganizer.notification.domain.NotificationDeliveryStatus.SENT;
import static com.mazurek.eventOrganizer.notification.domain.NotificationDeliveryStatus.SKIPPED;
import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants.NOW;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.FIRST_USER_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class FcmNotificationDeliveryIntegrationTest {

    @Autowired
    private NotificationDeliveryService notificationDeliveryService;
    @Autowired
    private NotificationDeliveryRepository notificationDeliveryRepository;
    @Autowired
    private NotificationDeviceRepository notificationDeviceRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private TestFcmApiClient fcmApiClient;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private DeletionService deletionService;

    private UUID recipientId;

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();
        fcmApiClient.reset();
        recipientId = userRepository.findByIgnoreCaseEmail(FIRST_USER_EMAIL)
                .orElseThrow(UserNotFoundException::new)
                .getId();
    }

    @AfterEach
    void tearDown() {
        fcmApiClient.reset();
        deletionService.deleteAllSafe();
    }

    @ParameterizedTest
    @MethodSource("mobileFcmOutcomes")
    void persistsTheDeliveryStateMappedFromTheFakeMobileFcmResult(
            FcmSendResult fcmResult,
            NotificationDeliveryStatus expectedStatus,
            Instant expectedNextAttemptAt,
            String expectedError
    ) {
        NotificationDelivery delivery = persistMobileDelivery();
        fcmApiClient.configureMobileResult(fcmResult);

        notificationDeliveryService.processDelivery(delivery.getId());

        NotificationDelivery persistedDelivery = notificationDeliveryRepository.findById(delivery.getId())
                .orElseThrow();
        assertThat(persistedDelivery)
                .extracting(
                        NotificationDelivery::getStatus,
                        NotificationDelivery::getAttemptCount,
                        NotificationDelivery::getNextAttemptAt,
                        NotificationDelivery::getLastError
                )
                .containsExactly(expectedStatus, 1, expectedNextAttemptAt, expectedError);
    }

    @Test
    void dispatchesWebDeliveriesThroughTheFakeWebFcmClient() {
        NotificationDelivery delivery = persistDelivery(NotificationChannel.PUSH_WEB);
        fcmApiClient.configureWebResult(FcmSendResult.permanentFailure(1, "Web push configuration is invalid."));

        notificationDeliveryService.processDelivery(delivery.getId());

        NotificationDelivery persistedDelivery = notificationDeliveryRepository.findById(delivery.getId())
                .orElseThrow();
        assertThat(persistedDelivery)
                .extracting(NotificationDelivery::getStatus, NotificationDelivery::getLastError)
                .containsExactly(DEAD, "Web push configuration is invalid.");
    }

    @ParameterizedTest
    @EnumSource(value = NotificationChannel.class, names = {"PUSH_MOBILE", "PUSH_WEB"})
    void invalidInstallationCleanupDeletesOnlyTheTargetAndCompletesDelivery(NotificationChannel channel) {
        DevicePlatform platform = channel == PUSH_MOBILE ? DevicePlatform.ANDROID : DevicePlatform.WEB;
        NotificationDevice target = persistDevice(platform, "invalid-installation");
        NotificationDevice otherDevice = persistDevice(platform, "other-installation");
        NotificationDelivery delivery = persistDelivery(channel, target.getId(), target.getFirebaseInstallationId());
        FcmSendResult invalidTarget = FcmSendResult.fromCounts(1, 0, 1, 0, 0, "Invalid installation.");
        if (channel == PUSH_MOBILE) {
            fcmApiClient.configureMobileResult(invalidTarget);
        } else {
            fcmApiClient.configureWebResult(invalidTarget);
        }

        notificationDeliveryService.processDelivery(delivery.getId());

        assertThat(notificationDeviceRepository.existsById(target.getId())).isFalse();
        assertThat(notificationDeviceRepository.existsById(otherDevice.getId())).isTrue();
        assertThat(notificationDeliveryRepository.findById(delivery.getId())).get()
                .extracting(
                        NotificationDelivery::getStatus,
                        NotificationDelivery::getAttemptCount,
                        NotificationDelivery::getLastError,
                        NotificationDelivery::getClaimToken,
                        NotificationDelivery::getProcessingStartedAt
                )
                .containsExactly(SKIPPED, 1, "Invalid installation.", null, null);
    }

    private static Stream<Arguments> mobileFcmOutcomes() {
        return Stream.of(
                Arguments.of(FcmSendResult.successful(1), SENT, null, null),
                Arguments.of(
                        FcmSendResult.retryableFailure(1, "FCM is temporarily unavailable."),
                        FAILED,
                        NOW.plus(1, ChronoUnit.MINUTES),
                        "FCM is temporarily unavailable."
                ),
                Arguments.of(
                        FcmSendResult.permanentFailure(1, "FCM request is invalid."),
                        DEAD,
                        null,
                        "FCM request is invalid."
                ),
                Arguments.of(
                        FcmSendResult.noTargets(),
                        SKIPPED,
                        null,
                        "No registered installations for this notification channel."
                )
        );
    }

    private NotificationDelivery persistMobileDelivery() {
        return persistDelivery(PUSH_MOBILE);
    }

    private NotificationDelivery persistDelivery(NotificationChannel channel) {
        return persistDelivery(channel, UUID.randomUUID(), "test-installation");
    }

    private NotificationDelivery persistDelivery(NotificationChannel channel, UUID deviceId, String installationId) {
        Notification notification = notificationRepository.saveAndFlush(
                NotificationTestBuilder.privateMessageNotification()
                        .id(null)
                        .recipientId(recipientId)
                        .build()
        );

        return notificationDeliveryRepository.saveAndFlush(NotificationDelivery.builder()
                .notification(notification)
                .channel(channel)
                .targetKey("test:" + UUID.randomUUID())
                .targetDeviceId(deviceId)
                .targetInstallationId(installationId)
                .status(NotificationDeliveryStatus.PENDING)
                .attemptCount(0)
                .createdAt(NOW)
                .build());
    }

    private NotificationDevice persistDevice(DevicePlatform platform, String installationId) {
        return notificationDeviceRepository.saveAndFlush(NotificationDevice.builder()
                .userId(recipientId)
                .platform(platform)
                .firebaseInstallationId(installationId)
                .createdAt(NOW)
                .build());
    }
}
