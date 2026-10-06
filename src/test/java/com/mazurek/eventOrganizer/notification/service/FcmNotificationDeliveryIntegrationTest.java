package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.testData.builders.FcmSendResultTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.NotificationDeliveryTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.NotificationDeviceTestBuilder;
import com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.FcmSendOutcome;
import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static com.mazurek.eventOrganizer.testData.TestConstants.SendResultConstants.FCM_NO_TARGETS_ERROR_MESSAGE;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.core.context.SecurityContextHolder;
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
@DisplayName("FcmNotificationDeliveryIntegrationTest contracts:")
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
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();
        fcmApiClient.reset();
        recipientId = userRepository.findByIgnoreCaseEmail(FIRST_USER_EMAIL)
                .orElseThrow(UserNotFoundException::new)
                .getId();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        fcmApiClient.reset();
        deletionService.deleteAllSafe();
    }

    @ParameterizedTest
    @MethodSource("mobileFcmOutcomes")
    void whenMobileFcmResultIsReturnedShouldPersistMappedDeliveryState(
            FcmSendResult fcmResult,
            NotificationDeliveryStatus expectedStatus,
            Instant expectedNextAttemptAt,
            String expectedError
    ) {
        NotificationDelivery delivery = persistMobileDelivery();
        fcmApiClient.configureMobileResult(fcmResult);

        notificationDeliveryService.processDelivery(delivery.getId());

        NotificationDelivery persistedDelivery = requirePresent(notificationDeliveryRepository.findById(delivery.getId()), "Expected persisted prerequisite in whenMobileFcmResultIsReturnedShouldPersistMappedDeliveryState");
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
    void whenWebDeliveryIsDispatchedShouldUseTestWebFcmClient() {
        NotificationDelivery delivery = persistDelivery(NotificationChannel.PUSH_WEB);
        fcmApiClient.configureWebResult(new FcmSendResultTestBuilder()
                .outcome(FcmSendOutcome.PERMANENT_FAILURE)
                .targetCount(1)
                .successCount(0)
                .permanentFailureCount(1)
                .errorMessage("Web push configuration is invalid.")
                .build());

        notificationDeliveryService.processDelivery(delivery.getId());

        NotificationDelivery persistedDelivery = requirePresent(notificationDeliveryRepository.findById(delivery.getId()), "Expected persisted prerequisite in whenWebDeliveryIsDispatchedShouldUseTestWebFcmClient");
        assertThat(persistedDelivery)
                .extracting(NotificationDelivery::getStatus, NotificationDelivery::getLastError)
                .containsExactly(DEAD, "Web push configuration is invalid.");
    }

    @ParameterizedTest
    @EnumSource(value = NotificationChannel.class, names = {"PUSH_MOBILE", "PUSH_WEB"})
    void whenInstallationIsInvalidShouldDeleteOnlyTargetAndCompleteDelivery(NotificationChannel channel) {
        DevicePlatform platform = channel == PUSH_MOBILE ? DevicePlatform.ANDROID : DevicePlatform.WEB;
        NotificationDevice target = persistDevice(platform, "invalid-installation");
        NotificationDevice otherDevice = persistDevice(platform, "other-installation");
        NotificationDelivery delivery = persistDelivery(channel, target.getId(), target.getFirebaseInstallationId());
        FcmSendResult invalidTarget = new FcmSendResultTestBuilder()
                .outcome(FcmSendOutcome.NO_TARGETS)
                .targetCount(1)
                .successCount(0)
                .invalidTargetCount(1)
                .errorMessage("Invalid installation.")
                .build();
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
                Arguments.of(new FcmSendResultTestBuilder()
                        .outcome(FcmSendOutcome.SENT)
                        .targetCount(1)
                        .successCount(1)
                        .build(), SENT, null, null),
                Arguments.of(
                        new FcmSendResultTestBuilder()
                                .outcome(FcmSendOutcome.RETRYABLE_FAILURE)
                                .targetCount(1)
                                .successCount(0)
                                .retryableFailureCount(1)
                                .errorMessage("FCM is temporarily unavailable.")
                                .build(),
                        FAILED,
                        NOW.plus(1, ChronoUnit.MINUTES),
                        "FCM is temporarily unavailable."
                ),
                Arguments.of(
                        new FcmSendResultTestBuilder()
                                .outcome(FcmSendOutcome.PERMANENT_FAILURE)
                                .targetCount(1)
                                .successCount(0)
                                .permanentFailureCount(1)
                                .errorMessage("FCM request is invalid.")
                                .build(),
                        DEAD,
                        null,
                        "FCM request is invalid."
                ),
                Arguments.of(
                        new FcmSendResultTestBuilder()
                                .outcome(FcmSendOutcome.NO_TARGETS)
                                .targetCount(0)
                                .successCount(0)
                                .errorMessage(FCM_NO_TARGETS_ERROR_MESSAGE)
                                .build(),
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

        return notificationDeliveryRepository.saveAndFlush(new NotificationDeliveryTestBuilder().id(null)
                .notification(notification)
                .channel(channel)
                .targetKey("test:" + UUID.randomUUID())
                .targetDeviceId(deviceId)
                .targetInstallationId(installationId)
                .status(NotificationDeliveryStatus.PENDING)
                .attemptCount(0)
                .createdAt(NOW)
                .targetEmail(null)
                .build());
    }

    private NotificationDevice persistDevice(DevicePlatform platform, String installationId) {
        return notificationDeviceRepository.saveAndFlush(new NotificationDeviceTestBuilder().id(null)
                .userId(recipientId)
                .platform(platform)
                .firebaseInstallationId(installationId)
                .createdAt(NOW)
                .lastSeenAt(null)
                .build());
    }
}
