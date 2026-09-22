package com.mazurek.eventOrganizer.notification.repository;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.notification.delivery.NotificationDeliveryClaim;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationDelivery;
import com.mazurek.eventOrganizer.notification.domain.NotificationDeliveryStatus;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.builders.NotificationTestBuilder;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants.NOW;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.FIRST_USER_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class NotificationDeliveryClaimRepositoryIntegrationTest {

    @Autowired
    private NotificationDeliveryClaimRepository claimRepository;
    @Autowired
    private NotificationDeliveryRepository deliveryRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private DeletionService deletionService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();
        userId = userRepository.findByIgnoreCaseEmail(FIRST_USER_EMAIL)
                .orElseThrow(UserNotFoundException::new)
                .getId();
    }

    @AfterEach
    void tearDown() {
        deletionService.deleteAllSafe();
    }

    @Test
    void concurrentWorkersCannotClaimTheSameDelivery() throws Exception {
        NotificationDelivery delivery = persistPendingDelivery();
        Callable<List<NotificationDeliveryClaim>> claim = () -> claimRepository.claimBatch(
                NOW,
                NOW.minus(Duration.ofMinutes(10)),
                10,
                6
        );

        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<List<NotificationDeliveryClaim>> first = executor.submit(claim);
            Future<List<NotificationDeliveryClaim>> second = executor.submit(claim);

            List<NotificationDeliveryClaim> allClaims = java.util.stream.Stream
                    .concat(first.get().stream(), second.get().stream())
                    .toList();

            assertThat(allClaims)
                    .extracting(NotificationDeliveryClaim::deliveryId)
                    .containsExactly(delivery.getId());
        }
    }

    @Test
    void abandonedClaimCanBeReclaimedAndOldTokenCannotCompleteIt() {
        NotificationDelivery delivery = persistPendingDelivery();
        NotificationDeliveryClaim firstClaim = claimRepository.claimOne(
                delivery.getId(),
                NOW,
                NOW.minus(Duration.ofMinutes(10)),
                6
        ).orElseThrow();

        Instant later = NOW.plus(Duration.ofMinutes(11));
        NotificationDeliveryClaim secondClaim = claimRepository.claimOne(
                delivery.getId(),
                later,
                later.minus(Duration.ofMinutes(10)),
                6
        ).orElseThrow();

        assertThat(secondClaim.claimToken()).isNotEqualTo(firstClaim.claimToken());
        assertThat(deliveryRepository.findClaimedForDispatch(
                delivery.getId(),
                firstClaim.claimToken()
        )).isEmpty();
        assertThat(deliveryRepository.findClaimedForDispatch(
                delivery.getId(),
                secondClaim.claimToken()
        )).isPresent();
    }

    private NotificationDelivery persistPendingDelivery() {
        Notification notification = notificationRepository.saveAndFlush(
                NotificationTestBuilder.privateMessageNotification()
                        .id(null)
                        .recipientId(userId)
                        .build()
        );

        return deliveryRepository.saveAndFlush(NotificationDelivery.builder()
                .notification(notification)
                .channel(NotificationChannel.PUSH_MOBILE)
                .status(NotificationDeliveryStatus.PENDING)
                .attemptCount(0)
                .createdAt(NOW)
                .build());
    }
}
