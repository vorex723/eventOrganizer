package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.notification.domain.DevicePlatform;
import com.mazurek.eventOrganizer.notification.domain.NotificationDevice;
import com.mazurek.eventOrganizer.notification.dto.NotificationDeviceDto;
import com.mazurek.eventOrganizer.notification.dto.RegisterNotificationDeviceDto;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeviceRepository;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeviceUpsertRepository;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.NotificationDeviceConstants.FIRST_NOTIFICATION_DEVICE_CREATED_AT;
import static com.mazurek.eventOrganizer.testData.TestConstants.NotificationDeviceConstants.FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID;
import static com.mazurek.eventOrganizer.testData.TestConstants.NotificationDeviceConstants.FIRST_NOTIFICATION_DEVICE_ID;
import static com.mazurek.eventOrganizer.testData.TestConstants.NotificationDeviceConstants.FIRST_NOTIFICATION_DEVICE_LAST_SEEN_AT;
import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants.NOW;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.FIRST_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationDeviceServiceImpl unit tests:")
class NotificationDeviceServiceImplUnitTest {

    @Mock
    private Clock clock;
    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private NotificationDeviceRepository notificationDeviceRepository;
    @Mock
    private NotificationDeviceUpsertRepository notificationDeviceUpsertRepository;
    @InjectMocks
    private NotificationDeviceServiceImpl notificationDeviceService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = UserTestBuilder.firstUser().build();
    }

    @Nested
    @DisplayName("Register current user device tests:")
    class RegisterCurrentUserDeviceTests {

        @Test
        @DisplayName("When registration succeeds should upsert the current user's device and return it")
        void whenRegistrationSucceedsShouldUpsertCurrentUsersDeviceAndReturnIt() {
            NotificationDevice storedDevice = device();
            when(authenticationService.getCurrentUser()).thenReturn(currentUser);
            when(clock.instant()).thenReturn(NOW);
            when(notificationDeviceUpsertRepository.upsert(
                    FIRST_USER_ID,
                    DevicePlatform.ANDROID,
                    FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID,
                    NOW
            )).thenReturn(storedDevice);

            NotificationDeviceDto result = notificationDeviceService.registerCurrentUserDevice(request());

            assertThat(result).isEqualTo(new NotificationDeviceDto(storedDevice));
            verify(authenticationService).getCurrentUser();
            verify(clock).instant();
            verify(notificationDeviceUpsertRepository).upsert(
                    FIRST_USER_ID,
                    DevicePlatform.ANDROID,
                    FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID,
                    NOW
            );
            verifyNoInteractions(notificationDeviceRepository);
        }

        @Test
        @DisplayName("When current user cannot be resolved should not access clock or persistence")
        void whenCurrentUserCannotBeResolvedShouldNotAccessClockOrPersistence() {
            IllegalStateException exception = new IllegalStateException("Current user is unavailable.");
            when(authenticationService.getCurrentUser()).thenThrow(exception);

            assertThatThrownBy(() -> notificationDeviceService.registerCurrentUserDevice(request()))
                    .isSameAs(exception);

            verifyNoInteractions(clock, notificationDeviceRepository, notificationDeviceUpsertRepository);
        }

        @Test
        @DisplayName("When upsert fails should propagate the failure")
        void whenUpsertFailsShouldPropagateFailure() {
            IllegalStateException exception = new IllegalStateException("Database is unavailable.");
            when(authenticationService.getCurrentUser()).thenReturn(currentUser);
            when(clock.instant()).thenReturn(NOW);
            when(notificationDeviceUpsertRepository.upsert(
                    FIRST_USER_ID,
                    DevicePlatform.ANDROID,
                    FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID,
                    NOW
            )).thenThrow(exception);

            assertThatThrownBy(() -> notificationDeviceService.registerCurrentUserDevice(request()))
                    .isSameAs(exception);

            verifyNoInteractions(notificationDeviceRepository);
        }
    }

    @Test
    @DisplayName("When system cleanup deletes a device should delegate without checking ownership or existence")
    void whenSystemCleanupDeletesDeviceShouldDelegateWithoutCheckingOwnershipOrExistence() {
        UUID deviceId = UUID.randomUUID();

        notificationDeviceService.deleteNotificationDeviceForSystem(deviceId);

        verify(notificationDeviceRepository).deleteIfExistsById(deviceId);
        verifyNoInteractions(clock, authenticationService, notificationDeviceUpsertRepository);
    }

    @Test
    @DisplayName("When system cleanup persistence fails should propagate the failure")
    void whenSystemCleanupPersistenceFailsShouldPropagateFailure() {
        UUID deviceId = UUID.randomUUID();
        IllegalStateException exception = new IllegalStateException("Database is unavailable.");
        doThrow(exception).when(notificationDeviceRepository).deleteIfExistsById(deviceId);

        assertThatThrownBy(() -> notificationDeviceService.deleteNotificationDeviceForSystem(deviceId))
                .isSameAs(exception);

        verifyNoInteractions(clock, authenticationService, notificationDeviceUpsertRepository);
    }

    @Test
    @DisplayName("When current user deletes a device should delegate an owner-scoped delete")
    void whenCurrentUserDeletesDeviceShouldDelegateOwnerScopedDelete() {
        UUID deviceId = UUID.randomUUID();
        when(authenticationService.getCurrentUser()).thenReturn(currentUser);
        when(notificationDeviceRepository.deleteIfOwnedByIdAndUserId(deviceId, FIRST_USER_ID)).thenReturn(1);

        notificationDeviceService.deleteCurrentUserNotificationDevice(deviceId);

        verify(authenticationService).getCurrentUser();
        verify(notificationDeviceRepository).deleteIfOwnedByIdAndUserId(deviceId, FIRST_USER_ID);
        verifyNoInteractions(clock, notificationDeviceUpsertRepository);
    }

    @Test
    @DisplayName("When current user deletes an unknown or non-owned device should complete without error")
    void whenCurrentUserDeletesDeviceWithNoOwnerScopedMatchShouldCompleteWithoutError() {
        UUID deviceId = UUID.randomUUID();
        when(authenticationService.getCurrentUser()).thenReturn(currentUser);
        when(notificationDeviceRepository.deleteIfOwnedByIdAndUserId(deviceId, FIRST_USER_ID)).thenReturn(0);

        assertThatCode(() -> notificationDeviceService.deleteCurrentUserNotificationDevice(deviceId))
                .doesNotThrowAnyException();

        verify(authenticationService).getCurrentUser();
        verify(notificationDeviceRepository).deleteIfOwnedByIdAndUserId(deviceId, FIRST_USER_ID);
        verifyNoInteractions(clock, notificationDeviceUpsertRepository);
    }

    @Test
    @DisplayName("When current user deletion persistence fails should propagate the failure")
    void whenCurrentUserDeletionPersistenceFailsShouldPropagateFailure() {
        UUID deviceId = UUID.randomUUID();
        IllegalStateException exception = new IllegalStateException("Database is unavailable.");
        when(authenticationService.getCurrentUser()).thenReturn(currentUser);
        when(notificationDeviceRepository.deleteIfOwnedByIdAndUserId(deviceId, FIRST_USER_ID))
                .thenThrow(exception);

        assertThatThrownBy(() -> notificationDeviceService.deleteCurrentUserNotificationDevice(deviceId))
                .isSameAs(exception);

        verifyNoInteractions(clock, notificationDeviceUpsertRepository);
    }

    @Test
    @DisplayName("When current user cannot be resolved should not attempt an owner-scoped delete")
    void whenCurrentUserCannotBeResolvedShouldNotAttemptOwnerScopedDelete() {
        UUID deviceId = UUID.randomUUID();
        IllegalStateException exception = new IllegalStateException("Current user is unavailable.");
        when(authenticationService.getCurrentUser()).thenThrow(exception);

        assertThatThrownBy(() -> notificationDeviceService.deleteCurrentUserNotificationDevice(deviceId))
                .isSameAs(exception);

        verifyNoInteractions(clock, notificationDeviceRepository, notificationDeviceUpsertRepository);
    }

    private RegisterNotificationDeviceDto request() {
        return new RegisterNotificationDeviceDto(
                DevicePlatform.ANDROID,
                FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID
        );
    }

    private NotificationDevice device() {
        return NotificationDevice.builder()
                .id(FIRST_NOTIFICATION_DEVICE_ID)
                .userId(FIRST_USER_ID)
                .platform(DevicePlatform.ANDROID)
                .firebaseInstallationId(FIRST_NOTIFICATION_DEVICE_FIREBASE_INSTALLATION_ID)
                .createdAt(FIRST_NOTIFICATION_DEVICE_CREATED_AT)
                .lastSeenAt(FIRST_NOTIFICATION_DEVICE_LAST_SEEN_AT)
                .build();
    }
}
