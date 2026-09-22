package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.notification.domain.DevicePlatform;
import com.mazurek.eventOrganizer.notification.domain.NotificationDevice;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeviceRepository;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants.NOW;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.FIRST_USER_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class NotificationDeviceMaintenanceServiceIntegrationTest {

    @Autowired
    private NotificationDeviceMaintenanceService maintenanceService;
    @Autowired
    private NotificationDeviceRepository deviceRepository;
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
    void removesOnlyNullOrExpiredDeviceRegistrations() {
        NotificationDevice expired = persist("expired-fid", NOW.minus(Duration.ofDays(91)));
        NotificationDevice missingLastSeen = persist("missing-last-seen-fid", null);
        NotificationDevice active = persist("active-fid", NOW.minus(Duration.ofDays(89)));

        int removed = maintenanceService.removeStaleDevices();

        assertThat(removed).isEqualTo(2);
        assertThat(deviceRepository.findAll())
                .extracting(NotificationDevice::getId)
                .containsExactly(active.getId())
                .doesNotContain(expired.getId(), missingLastSeen.getId());
    }

    private NotificationDevice persist(String fid, java.time.Instant lastSeenAt) {
        return deviceRepository.saveAndFlush(NotificationDevice.builder()
                .userId(userId)
                .platform(DevicePlatform.WEB)
                .firebaseInstallationId(fid)
                .createdAt(NOW.minus(Duration.ofDays(100)))
                .lastSeenAt(lastSeenAt)
                .build());
    }
}
