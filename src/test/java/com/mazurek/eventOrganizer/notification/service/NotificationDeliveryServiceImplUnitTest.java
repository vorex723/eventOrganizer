package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.config.properties.NotificationProperties;
import com.mazurek.eventOrganizer.testData.builders.NotificationPropertiesTestBuilder;
import com.mazurek.eventOrganizer.exception.notification.NotificationDeliveryNotFoundException;
import com.mazurek.eventOrganizer.notification.delivery.NotificationSenderDispatcher;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationDelivery;
import com.mazurek.eventOrganizer.notification.domain.NotificationDeliveryStatus;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.notification.domain.DevicePlatform;
import com.mazurek.eventOrganizer.notification.domain.NotificationDevice;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeliveryClaimRepository;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeliveryRepository;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeliveryUpsertRepository;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeviceRepository;
import com.mazurek.eventOrganizer.testData.builders.NotificationDeviceTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.NotificationTestBuilder;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants.NOW;
import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants.FIXED_CLOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationDeliveryServiceImpl unit tests:")
class NotificationDeliveryServiceImplUnitTest {


    @Mock
    private NotificationDeliveryRepository notificationDeliveryRepository;
    @Mock
    private NotificationDeliveryUpsertRepository notificationDeliveryUpsertRepository;
    @Mock
    private NotificationDeliveryClaimRepository notificationDeliveryClaimRepository;
    @Mock
    private NotificationDeviceRepository notificationDeviceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationPreferenceService notificationPreferenceService;
    @Mock
    private NotificationSenderDispatcher notificationSenderDispatcher;
    @Mock
    private NotificationChannelAvailability notificationChannelAvailability;
    @Mock
    private TransactionTemplate transactionTemplate;

    private NotificationProperties notificationProperties;
    private NotificationDeliveryServiceImpl service;

    @BeforeEach
    void setUp() {
        notificationProperties = new NotificationPropertiesTestBuilder().build();
        service = new NotificationDeliveryServiceImpl(
                FIXED_CLOCK,
                notificationDeliveryRepository,
                notificationDeliveryUpsertRepository,
                notificationDeliveryClaimRepository,
                notificationDeviceRepository,
                userRepository,
                notificationPreferenceService,
                notificationSenderDispatcher,
                notificationChannelAvailability,
                new NotificationRetryPolicy(notificationProperties),
                notificationProperties,
                transactionTemplate
        );
    }

    @Test
    @DisplayName("Creates one pending delivery for every enabled channel")
    void whenChannelsAreEnabledShouldCreatePendingDeliveries() {
        Notification notification = notification();
        when(notificationPreferenceService.getEnabledExternalChannels(
                notification.getRecipientId(),
                notification.getResourceType()
        )).thenReturn(Set.of(NotificationChannel.PUSH_MOBILE, NotificationChannel.PUSH_WEB));
        NotificationDevice mobileDevice = new NotificationDeviceTestBuilder()
                .id(UUID.randomUUID())
                .userId(notification.getRecipientId())
                .platform(DevicePlatform.ANDROID)
                .firebaseInstallationId("fid-mobile")
                .createdAt(NOW)
                .lastSeenAt(NOW)
                .build();
        NotificationDevice webDevice = new NotificationDeviceTestBuilder()
                .id(UUID.randomUUID())
                .userId(notification.getRecipientId())
                .platform(DevicePlatform.WEB)
                .firebaseInstallationId("fid-web")
                .createdAt(NOW)
                .lastSeenAt(NOW)
                .build();
        when(notificationDeviceRepository.findByUserIdAndPlatformIn(
                org.mockito.ArgumentMatchers.eq(notification.getRecipientId()),
                org.mockito.ArgumentMatchers.anySet()
        )).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Set<DevicePlatform> platforms = invocation.getArgument(1);
            return platforms.contains(DevicePlatform.WEB) ? List.of(webDevice) : List.of(mobileDevice);
        });

        service.createDeliveries(notification);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<NotificationDelivery> captor = ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(notificationDeliveryUpsertRepository, org.mockito.Mockito.times(2)).insertIfAbsent(captor.capture());
        assertThat(captor.getAllValues())
                .hasSize(2)
                .allSatisfy(delivery -> {
                    assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.PENDING);
                    assertThat(delivery.getAttemptCount()).isZero();
                    assertThat(delivery.getCreatedAt()).isEqualTo(NOW);
                    assertThat(delivery.getNotification()).isSameAs(notification);
                })
                .extracting(NotificationDelivery::getChannel)
                .containsExactlyInAnyOrder(NotificationChannel.PUSH_MOBILE, NotificationChannel.PUSH_WEB);
    }

    @Test
    @DisplayName("Creates no delivery when every channel is unavailable or disabled")
    void whenChannelsAreDisabledShouldNotCreateDeliveries() {
        Notification notification = notification();
        when(notificationPreferenceService.getEnabledExternalChannels(
                notification.getRecipientId(),
                notification.getResourceType()
        )).thenReturn(Set.of());

        service.createDeliveries(notification);

        verify(notificationDeliveryUpsertRepository, never()).insertIfAbsent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Does not start a claim when a requested delivery does not exist")
    void whenDeliveryIsUnknownShouldRejectWithoutClaim() {
        UUID deliveryId = UUID.randomUUID();
        when(notificationDeliveryRepository.existsById(deliveryId)).thenReturn(false);

        assertThatThrownBy(() -> service.processDelivery(deliveryId))
                .isInstanceOf(NotificationDeliveryNotFoundException.class);

        verify(notificationDeliveryClaimRepository, never())
                .claimOne(org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("Claims the configured batch of due deliveries")
    void whenProcessingPendingDeliveriesShouldClaimConfiguredBatch() {
        NotificationProperties.Delivery settings = notificationProperties.getDelivery();
        when(notificationDeliveryClaimRepository.claimBatch(
                NOW,
                NOW.minus(settings.getProcessingTimeout()),
                settings.getBatchSize(),
                settings.getMaxAttempts()
        )).thenReturn(List.of());

        service.processPendingDeliveries();

        verify(notificationDeliveryClaimRepository).claimBatch(
                NOW,
                NOW.minus(settings.getProcessingTimeout()),
                100,
                6
        );
    }

    private Notification notification() {
        return new NotificationTestBuilder()
                .id(UUID.randomUUID())
                .recipientId(UUID.randomUUID())
                .resourceType(NotificationResourceType.EVENT)
                .resourceId(UUID.randomUUID())
                .createdAt(NOW)
                .build();
    }
}
