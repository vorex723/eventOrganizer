package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.notification.dto.NotificationDeviceDto;
import com.mazurek.eventOrganizer.notification.dto.RegisterNotificationDeviceDto;

import java.util.UUID;

public interface NotificationDeviceService {
    NotificationDeviceDto registerCurrentUserDevice(RegisterNotificationDeviceDto registerNotificationDeviceDto);
    void deactivateCurrentUserDevice(UUID deviceId);
}
