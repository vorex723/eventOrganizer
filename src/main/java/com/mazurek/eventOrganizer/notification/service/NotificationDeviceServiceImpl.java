package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.notification.domain.NotificationDevice;
import com.mazurek.eventOrganizer.notification.dto.NotificationDeviceDto;
import com.mazurek.eventOrganizer.notification.dto.RegisterNotificationDeviceDto;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeviceRepository;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeviceUpsertRepository;
import com.mazurek.eventOrganizer.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationDeviceServiceImpl implements NotificationDeviceService {

    private final Clock clock;
    private final AuthenticationService authenticationService;
    private final NotificationDeviceRepository notificationDeviceRepository;
    private final NotificationDeviceUpsertRepository notificationDeviceUpsertRepository;

    @Override
    public NotificationDeviceDto registerCurrentUserDevice(RegisterNotificationDeviceDto registerNotificationDeviceDto) {
        User user = authenticationService.getCurrentUser();

        Instant now = clock.instant();

        NotificationDevice notificationDevice = notificationDeviceUpsertRepository.upsert(
                user.getId(),
                registerNotificationDeviceDto.platform(),
                registerNotificationDeviceDto.firebaseInstallationId(),
                now
        );

        return new NotificationDeviceDto(notificationDevice);
    }

    @Override
    public void deleteNotificationDeviceForSystem(UUID deviceId) {
        notificationDeviceRepository.deleteIfExistsById(deviceId);
    }

    @Override
    public void deleteCurrentUserNotificationDevice(UUID deviceId) {
        User user = authenticationService.getCurrentUser();

        notificationDeviceRepository.deleteIfOwnedByIdAndUserId(deviceId, user.getId());
    }

}
