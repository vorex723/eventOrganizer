package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.notification.delivery.NotificationEmailClientTestImpl;
import com.mazurek.eventOrganizer.notification.delivery.NotificationSendResult;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationDelivery;
import com.mazurek.eventOrganizer.notification.domain.NotificationDeliveryStatus;
import com.mazurek.eventOrganizer.notification.domain.NotificationPreference;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeliveryRepository;
import com.mazurek.eventOrganizer.notification.repository.NotificationPreferenceRepository;
import com.mazurek.eventOrganizer.notification.repository.NotificationRepository;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.builders.NotificationTestBuilder;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.mazurek.eventOrganizer.notification.domain.NotificationChannel.EMAIL;
import static com.mazurek.eventOrganizer.notification.domain.NotificationChannel.PUSH_MOBILE;
import static com.mazurek.eventOrganizer.notification.domain.NotificationChannel.PUSH_WEB;
import static com.mazurek.eventOrganizer.notification.domain.NotificationDeliveryStatus.DEAD;
import static com.mazurek.eventOrganizer.notification.domain.NotificationDeliveryStatus.FAILED;
import static com.mazurek.eventOrganizer.notification.domain.NotificationDeliveryStatus.SENT;
import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants.NOW;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.FIRST_USER_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.notifications.email.enabled=true")
@ActiveProfiles("test")
class EmailNotificationDeliveryIntegrationTest {

    @Autowired
    private NotificationDeliveryService notificationDeliveryService;
    @Autowired
    private NotificationDeliveryRepository notificationDeliveryRepository;
    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private NotificationEmailClientTestImpl notificationEmailClient;
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
        notificationEmailClient.reset();
        recipientId = userRepository.findByIgnoreCaseEmail(FIRST_USER_EMAIL)
                .orElseThrow(UserNotFoundException::new)
                .getId();
    }

    @AfterEach
    void tearDown() {
        notificationEmailClient.reset();
        deletionService.deleteAllSafe();
    }

    @Test
    void createsEmailDeliveryWhenUserHasEnabledEmailAndDisabledPush() {
        notificationPreferenceRepository.saveAllAndFlush(List.of(
                preference(PUSH_MOBILE, false),
                preference(PUSH_WEB, false),
                preference(EMAIL, true)
        ));
        Notification notification = notificationRepository.saveAndFlush(
                NotificationTestBuilder.privateMessageNotification()
                        .id(null)
                        .recipientId(recipientId)
                        .build()
        );

        notificationDeliveryService.createDeliveries(notification);

        assertThat(notificationDeliveryRepository.findAll())
                .extracting(NotificationDelivery::getChannel)
                .containsExactly(EMAIL);
    }

    @ParameterizedTest
    @MethodSource("emailOutcomes")
    void persistsDeliveryStateMappedFromEmailResult(
            NotificationSendResult emailResult,
            NotificationDeliveryStatus expectedStatus,
            Instant expectedNextAttemptAt,
            String expectedError
    ) {
        NotificationDelivery delivery = persistEmailDelivery();
        notificationEmailClient.configureResult(emailResult);

        notificationDeliveryService.processDelivery(delivery.getId());

        NotificationDelivery persisted = notificationDeliveryRepository.findById(delivery.getId())
                .orElseThrow();
        assertThat(persisted)
                .extracting(
                        NotificationDelivery::getStatus,
                        NotificationDelivery::getAttemptCount,
                        NotificationDelivery::getNextAttemptAt,
                        NotificationDelivery::getLastError
                )
                .containsExactly(expectedStatus, 1, expectedNextAttemptAt, expectedError);
    }

    private static Stream<Arguments> emailOutcomes() {
        return Stream.of(
                Arguments.of(NotificationSendResult.sent("smtp-id"), SENT, null, null),
                Arguments.of(
                        NotificationSendResult.retryableFailure("SMTP provider is temporarily unavailable."),
                        FAILED,
                        NOW.plus(1, ChronoUnit.MINUTES),
                        "SMTP provider is temporarily unavailable."
                ),
                Arguments.of(
                        NotificationSendResult.permanentFailure("SMTP configuration is invalid."),
                        DEAD,
                        null,
                        "SMTP configuration is invalid."
                )
        );
    }

    private NotificationDelivery persistEmailDelivery() {
        Notification notification = notificationRepository.saveAndFlush(
                NotificationTestBuilder.privateMessageNotification()
                        .id(null)
                        .recipientId(recipientId)
                        .build()
        );
        return notificationDeliveryRepository.saveAndFlush(NotificationDelivery.builder()
                .notification(notification)
                .channel(EMAIL)
                .targetKey("email:" + FIRST_USER_EMAIL)
                .targetEmail(FIRST_USER_EMAIL)
                .status(NotificationDeliveryStatus.PENDING)
                .attemptCount(0)
                .createdAt(NOW)
                .build());
    }

    private NotificationPreference preference(NotificationChannel channel, boolean enabled) {
        return NotificationPreference.builder()
                .userId(recipientId)
                .resourceType(NotificationResourceType.CONVERSATION)
                .channel(channel)
                .enabled(enabled)
                .build();
    }
}
