package com.mazurek.eventOrganizer.notification.repository;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.notification.domain.DevicePlatform;
import com.mazurek.eventOrganizer.notification.domain.NotificationDevice;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.builders.NotificationDeviceTestBuilder;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants.NOW;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.FIRST_USER_EMAIL;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.SECOND_USER_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NotificationDeviceRepositoryIntegrationTest contracts:")
class NotificationDeviceRepositoryIntegrationTest {

    @Autowired
    private NotificationDeviceRepository notificationDeviceRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private DeletionService deletionService;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private UUID firstUserId;
    private UUID secondUserId;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();
        firstUserId = requirePresent(userRepository.findByIgnoreCaseEmail(FIRST_USER_EMAIL), "Expected persisted prerequisite in setUp").getId();
        secondUserId = requirePresent(userRepository.findByIgnoreCaseEmail(SECOND_USER_EMAIL), "Expected persisted prerequisite in setUp").getId();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
    }

    @Test
    void whenCleaningInvalidTargetShouldDeleteOnlyMatchingDevice() {
        NotificationDevice target = persistDevice(firstUserId, "invalid-installation");
        NotificationDevice otherDevice = persistDevice(firstUserId, "other-installation");
        NotificationDevice otherUserDevice = persistDevice(secondUserId, "other-user-installation");

        int deleted = deleteWithOwnershipSnapshot(
                target.getId(), firstUserId, "invalid-installation");

        assertThat(deleted).isOne();
        assertThat(notificationDeviceRepository.findAll())
                .extracting(NotificationDevice::getId)
                .containsExactlyInAnyOrder(otherDevice.getId(), otherUserDevice.getId());
    }

    @Test
    void whenInstallationHasChangedShouldPreserveDevice() {
        NotificationDevice target = persistDevice(firstUserId, "old-installation");
        target.setFirebaseInstallationId("replacement-installation");
        notificationDeviceRepository.saveAndFlush(target);

        int deleted = deleteWithOwnershipSnapshot(
                target.getId(), firstUserId, "old-installation");

        assertThat(deleted).isZero();
        assertThat(notificationDeviceRepository.findById(target.getId()))
                .get()
                .extracting(NotificationDevice::getFirebaseInstallationId)
                .isEqualTo("replacement-installation");
    }

    @Test
    void whenOwnerHasChangedShouldPreserveDevice() {
        NotificationDevice target = persistDevice(firstUserId, "transferred-installation");
        target.setUserId(secondUserId);
        notificationDeviceRepository.saveAndFlush(target);

        int deleted = deleteWithOwnershipSnapshot(
                target.getId(), firstUserId, "transferred-installation");

        assertThat(deleted).isZero();
        assertThat(notificationDeviceRepository.findById(target.getId()))
                .get()
                .extracting(NotificationDevice::getUserId)
                .isEqualTo(secondUserId);
    }

    private NotificationDevice persistDevice(UUID userId, String installationId) {
        return notificationDeviceRepository.saveAndFlush(new NotificationDeviceTestBuilder().id(null)
                .userId(userId)
                .platform(DevicePlatform.ANDROID)
                .firebaseInstallationId(installationId)
                .createdAt(NOW)
                .lastSeenAt(null)
                .build());
    }

    private int deleteWithOwnershipSnapshot(UUID deviceId, UUID userId, String installationId) {
        return transactionTemplate.execute(status ->
                notificationDeviceRepository.deleteIfOwnedByIdAndUserIdAndFirebaseInstallationId(
                        deviceId, userId, installationId));
    }
}
