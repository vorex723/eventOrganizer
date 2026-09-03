package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.notification.domain.DevicePlatform;
import com.mazurek.eventOrganizer.notification.domain.NotificationDevice;
import com.mazurek.eventOrganizer.notification.dto.NotificationDeviceDto;
import com.mazurek.eventOrganizer.notification.dto.RegisterNotificationDeviceDto;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeviceRepository;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeviceUpsertRepository;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.mazurek.eventOrganizer.testData.TestConstants.NotificationDeviceConstants.FIRST_NOTIFICATION_DEVICE_CREATED_AT;
import static com.mazurek.eventOrganizer.testData.TestConstants.NotificationDeviceConstants.FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID;
import static com.mazurek.eventOrganizer.testData.TestConstants.NotificationDeviceConstants.FIRST_NOTIFICATION_DEVICE_LAST_SEEN_AT;
import static com.mazurek.eventOrganizer.testData.TestConstants.NotificationDeviceConstants.SECOND_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID;
import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants.NOW;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.FIRST_USER_EMAIL;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.SECOND_USER_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NotificationDeviceServiceImpl integration tests:")
class NotificationDeviceServiceImplIntegrationTest {

    @Autowired
    private NotificationDeviceService notificationDeviceService;
    @Autowired
    private NotificationDeviceRepository notificationDeviceRepository;
    @Autowired
    private NotificationDeviceUpsertRepository notificationDeviceUpsertRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private DeletionService deletionService;

    private UUID firstUserId;
    private UUID secondUserId;

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();
        authHelper.setupSecurityContextForFirstUser();

        firstUserId = userRepository.findByIgnoreCaseEmail(FIRST_USER_EMAIL)
                .orElseThrow(UserNotFoundException::new)
                .getId();
        secondUserId = userRepository.findByIgnoreCaseEmail(SECOND_USER_EMAIL)
                .orElseThrow(UserNotFoundException::new)
                .getId();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
    }

    @Nested
    @DisplayName("Register current user device tests:")
    class RegisterCurrentUserDeviceTests {

        @Test
        @DisplayName("When installation ID is new should persist a device for the current user")
        void whenInstallationIdIsNewShouldPersistDeviceForCurrentUser() {
            NotificationDeviceDto result = notificationDeviceService.registerCurrentUserDevice(request(
                    FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID
            ));

            NotificationDevice storedDevice = getStoredDevice(result.id());

            assertThat(storedDevice)
                    .extracting(
                            NotificationDevice::getUserId,
                            NotificationDevice::getPlatform,
                            NotificationDevice::getFirebaseInstallationId,
                            NotificationDevice::getCreatedAt,
                            NotificationDevice::getLastSeenAt
                    )
                    .containsExactly(
                            firstUserId,
                            DevicePlatform.ANDROID,
                            FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID,
                            NOW,
                            NOW
                    );
            assertThat(result).isEqualTo(new NotificationDeviceDto(storedDevice));
            assertThat(notificationDeviceRepository.count()).isOne();
        }

        @Test
        @DisplayName("When installation ID belongs to current user should reuse its row and refresh last seen")
        void whenInstallationIdBelongsToCurrentUserShouldReuseRowAndRefreshLastSeen() {
            NotificationDevice existingDevice = persistDevice(
                    firstUserId,
                    FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID,
                    FIRST_NOTIFICATION_DEVICE_CREATED_AT,
                    FIRST_NOTIFICATION_DEVICE_LAST_SEEN_AT
            );

            NotificationDeviceDto result = notificationDeviceService.registerCurrentUserDevice(request(
                    FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID
            ));

            NotificationDevice storedDevice = getStoredDevice(existingDevice.getId());

            assertThat(storedDevice)
                    .extracting(
                            NotificationDevice::getId,
                            NotificationDevice::getUserId,
                            NotificationDevice::getPlatform,
                            NotificationDevice::getFirebaseInstallationId,
                            NotificationDevice::getCreatedAt,
                            NotificationDevice::getLastSeenAt
                    )
                    .containsExactly(
                            existingDevice.getId(),
                            firstUserId,
                            DevicePlatform.ANDROID,
                            FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID,
                            FIRST_NOTIFICATION_DEVICE_CREATED_AT,
                            NOW
                    );
            assertThat(result).isEqualTo(new NotificationDeviceDto(storedDevice));
            assertThat(notificationDeviceRepository.count()).isOne();
        }

        @Test
        @DisplayName("When installation ID belongs to another user should transfer its existing row")
        void whenInstallationIdBelongsToAnotherUserShouldTransferItsExistingRow() {
            NotificationDevice existingDevice = persistDevice(
                    secondUserId,
                    DevicePlatform.IOS,
                    FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID,
                    FIRST_NOTIFICATION_DEVICE_CREATED_AT,
                    FIRST_NOTIFICATION_DEVICE_LAST_SEEN_AT
            );

            NotificationDeviceDto result = notificationDeviceService.registerCurrentUserDevice(request(
                    DevicePlatform.WEB,
                    FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID
            ));

            NotificationDevice storedDevice = getStoredDevice(existingDevice.getId());

            assertThat(storedDevice)
                    .extracting(
                            NotificationDevice::getId,
                            NotificationDevice::getUserId,
                            NotificationDevice::getPlatform,
                            NotificationDevice::getFirebaseInstallationId,
                            NotificationDevice::getCreatedAt,
                            NotificationDevice::getLastSeenAt
                    )
                    .containsExactly(
                            existingDevice.getId(),
                            firstUserId,
                            DevicePlatform.IOS,
                            FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID,
                            FIRST_NOTIFICATION_DEVICE_CREATED_AT,
                            NOW
                    );
            assertThat(result).isEqualTo(new NotificationDeviceDto(storedDevice));
            assertThat(notificationDeviceRepository.count()).isOne();
        }

        @Test
        @DisplayName("When current user registers multiple installation IDs should retain every device")
        void whenCurrentUserRegistersMultipleInstallationIdsShouldRetainEveryDevice() {
            NotificationDeviceDto firstDevice = notificationDeviceService.registerCurrentUserDevice(request(
                    DevicePlatform.ANDROID,
                    FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID
            ));
            NotificationDeviceDto secondDevice = notificationDeviceService.registerCurrentUserDevice(request(
                    DevicePlatform.WEB,
                    SECOND_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID
            ));

            assertThat(notificationDeviceRepository.findAll())
                    .extracting(
                            NotificationDevice::getId,
                            NotificationDevice::getUserId,
                            NotificationDevice::getPlatform,
                            NotificationDevice::getFirebaseInstallationId
                    )
                    .containsExactlyInAnyOrder(
                            tuple(
                                    firstDevice.id(),
                                    firstUserId,
                                    DevicePlatform.ANDROID,
                                    FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID
                            ),
                            tuple(
                                    secondDevice.id(),
                                    firstUserId,
                                    DevicePlatform.WEB,
                                    SECOND_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID
                            )
                    );
        }

        @Test
        @DisplayName("When persisting duplicate installation ID should reject the second device")
        void whenPersistingDuplicateInstallationIdShouldRejectSecondDevice() {
            persistDevice(
                    firstUserId,
                    FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID,
                    NOW,
                    NOW
            );

            assertThatThrownBy(() -> persistDevice(
                    secondUserId,
                    FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID,
                    NOW,
                    NOW
            )).isInstanceOf(DataIntegrityViolationException.class);

            assertThat(notificationDeviceRepository.count()).isOne();
        }
    }

    @Nested
    @DisplayName("Concurrent registration tests:")
    class ConcurrentRegistrationTests {

        @Test
        @DisplayName("When the same installation ID is upserted concurrently should create only one device")
        void whenSameInstallationIdIsUpsertedConcurrentlyShouldCreateOnlyOneDevice() throws Exception {
            ExecutorService executorService = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);

            try {
                Future<UUID> firstResult = executorService.submit(() -> upsertConcurrently(
                        firstUserId,
                        DevicePlatform.ANDROID,
                        FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID,
                        ready,
                        start
                ));
                Future<UUID> secondResult = executorService.submit(() -> upsertConcurrently(
                        firstUserId,
                        DevicePlatform.ANDROID,
                        FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID,
                        ready,
                        start
                ));

                assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
                start.countDown();

                UUID firstDeviceId = firstResult.get(5, TimeUnit.SECONDS);
                UUID secondDeviceId = secondResult.get(5, TimeUnit.SECONDS);

                assertThat(firstDeviceId).isEqualTo(secondDeviceId);
                assertThat(notificationDeviceRepository.findByFirebaseInstallationId(
                        FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID
                )).isPresent();
                assertThat(notificationDeviceRepository.count()).isOne();
            }
            finally {
                executorService.shutdownNow();
            }
        }

        @Test
        @DisplayName("When two users upsert the same installation ID concurrently should retain one valid device")
        void whenTwoUsersUpsertSameInstallationIdConcurrentlyShouldRetainOneValidDevice() throws Exception {
            ExecutorService executorService = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);

            try {
                Future<UUID> firstResult = executorService.submit(() -> upsertConcurrently(
                        firstUserId,
                        DevicePlatform.ANDROID,
                        FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID,
                        ready,
                        start
                ));
                Future<UUID> secondResult = executorService.submit(() -> upsertConcurrently(
                        secondUserId,
                        DevicePlatform.WEB,
                        FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID,
                        ready,
                        start
                ));

                assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
                start.countDown();

                UUID firstDeviceId = firstResult.get(5, TimeUnit.SECONDS);
                UUID secondDeviceId = secondResult.get(5, TimeUnit.SECONDS);
                NotificationDevice storedDevice = notificationDeviceRepository.findByFirebaseInstallationId(
                        FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID
                ).orElseThrow();

                assertThat(firstDeviceId).isEqualTo(secondDeviceId);
                assertThat(storedDevice.getUserId()).isIn(firstUserId, secondUserId);
                assertThat(storedDevice.getPlatform()).isIn(DevicePlatform.ANDROID, DevicePlatform.WEB);
                assertThat(storedDevice.getFirebaseInstallationId())
                        .isEqualTo(FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID);
                assertThat(storedDevice.getCreatedAt()).isEqualTo(NOW);
                assertThat(storedDevice.getLastSeenAt()).isEqualTo(NOW);
                assertThat(notificationDeviceRepository.count()).isOne();
            }
            finally {
                executorService.shutdownNow();
            }
        }
    }

    @Nested
    @DisplayName("System deletion tests:")
    class SystemDeletionTests {

        @Test
        @DisplayName("When system cleanup deletes another user's device should remove it without authentication")
        void whenSystemCleanupDeletesAnotherUsersDeviceShouldRemoveItWithoutAuthentication() {
            NotificationDevice existingDevice = persistDevice(
                    secondUserId,
                    SECOND_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID,
                    NOW,
                    NOW
            );
            SecurityContextHolder.clearContext();

            notificationDeviceService.deleteNotificationDeviceForSystem(existingDevice.getId());

            assertThat(notificationDeviceRepository.findById(existingDevice.getId())).isEmpty();
        }

        @Test
        @DisplayName("When system cleanup deletes an unknown device should complete without error")
        void whenSystemCleanupDeletesUnknownDeviceShouldCompleteWithoutError() {
            SecurityContextHolder.clearContext();

            notificationDeviceService.deleteNotificationDeviceForSystem(UUID.randomUUID());

            assertThat(notificationDeviceRepository.findAll()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Current user deletion tests:")
    class CurrentUserDeletionTests {

        @Test
        @DisplayName("When current user deletes their device should remove it")
        void whenCurrentUserDeletesTheirDeviceShouldRemoveIt() {
            NotificationDevice existingDevice = persistDevice(
                    firstUserId,
                    FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID,
                    NOW,
                    NOW
            );

            notificationDeviceService.deleteCurrentUserNotificationDevice(existingDevice.getId());

            assertThat(notificationDeviceRepository.findById(existingDevice.getId())).isEmpty();
        }

        @Test
        @DisplayName("When current user deletes another user's device should leave it unchanged")
        void whenCurrentUserDeletesAnotherUsersDeviceShouldLeaveItUnchanged() {
            NotificationDevice existingDevice = persistDevice(
                    secondUserId,
                    SECOND_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID,
                    NOW,
                    NOW
            );

            notificationDeviceService.deleteCurrentUserNotificationDevice(existingDevice.getId());

            assertThat(notificationDeviceRepository.findById(existingDevice.getId()))
                    .hasValueSatisfying(device -> assertThat(device.getUserId()).isEqualTo(secondUserId));
        }

        @Test
        @DisplayName("When current user deletes an unknown device should complete without error")
        void whenCurrentUserDeletesUnknownDeviceShouldCompleteWithoutError() {
            notificationDeviceService.deleteCurrentUserNotificationDevice(UUID.randomUUID());

            assertThat(notificationDeviceRepository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("When current user deletes the same device twice should complete without error")
        void whenCurrentUserDeletesSameDeviceTwiceShouldCompleteWithoutError() {
            NotificationDevice existingDevice = persistDevice(
                    firstUserId,
                    FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID,
                    NOW,
                    NOW
            );

            notificationDeviceService.deleteCurrentUserNotificationDevice(existingDevice.getId());
            notificationDeviceService.deleteCurrentUserNotificationDevice(existingDevice.getId());

            assertThat(notificationDeviceRepository.findById(existingDevice.getId())).isEmpty();
        }

        @Test
        @DisplayName("When current user registers a deleted installation ID should create a replacement device")
        void whenCurrentUserRegistersDeletedInstallationIdShouldCreateReplacementDevice() {
            NotificationDeviceDto originalDevice = notificationDeviceService.registerCurrentUserDevice(request(
                    FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID
            ));
            notificationDeviceService.deleteCurrentUserNotificationDevice(originalDevice.id());

            NotificationDeviceDto replacementDevice = notificationDeviceService.registerCurrentUserDevice(request(
                    FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID
            ));
            NotificationDevice storedDevice = getStoredDevice(replacementDevice.id());

            assertThat(replacementDevice.id()).isNotEqualTo(originalDevice.id());
            assertThat(storedDevice)
                    .extracting(
                            NotificationDevice::getUserId,
                            NotificationDevice::getFirebaseInstallationId,
                            NotificationDevice::getCreatedAt,
                            NotificationDevice::getLastSeenAt
                    )
                    .containsExactly(
                            firstUserId,
                            FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID,
                            NOW,
                            NOW
                    );
            assertThat(notificationDeviceRepository.count()).isOne();
        }
    }

    private RegisterNotificationDeviceDto request(String firebaseInstallationId) {
        return request(DevicePlatform.ANDROID, firebaseInstallationId);
    }

    private RegisterNotificationDeviceDto request(DevicePlatform platform, String firebaseInstallationId) {
        return new RegisterNotificationDeviceDto(platform, firebaseInstallationId);
    }

    private NotificationDevice persistDevice(
            UUID userId,
            String firebaseInstallationId,
            Instant createdAt,
            Instant lastSeenAt
    ) {
        return persistDevice(
                userId,
                DevicePlatform.ANDROID,
                firebaseInstallationId,
                createdAt,
                lastSeenAt
        );
    }

    private NotificationDevice persistDevice(
            UUID userId,
            DevicePlatform platform,
            String firebaseInstallationId,
            Instant createdAt,
            Instant lastSeenAt
    ) {
        return notificationDeviceRepository.saveAndFlush(NotificationDevice.builder()
                .userId(userId)
                .platform(platform)
                .firebaseInstallationId(firebaseInstallationId)
                .createdAt(createdAt)
                .lastSeenAt(lastSeenAt)
                .build());
    }

    private NotificationDevice getStoredDevice(UUID deviceId) {
        return notificationDeviceRepository.findById(deviceId).orElseThrow();
    }

    private UUID upsertConcurrently(
            UUID userId,
            DevicePlatform platform,
            String firebaseInstallationId,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        start.await();
        return notificationDeviceUpsertRepository.upsert(
                userId,
                platform,
                firebaseInstallationId,
                NOW
        ).getId();
    }
}
