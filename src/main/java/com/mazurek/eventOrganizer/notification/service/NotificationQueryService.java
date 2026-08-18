package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.notification.dto.NotificationPageDto;
import com.mazurek.eventOrganizer.notification.dto.NotificationUnreadCountDto;

import java.util.UUID;

public interface NotificationQueryService {
    NotificationPageDto getCurrentUserNotifications(int pageNumber);
    NotificationUnreadCountDto getCurrentUserUnreadCount();
    void markAsRead(UUID notificationId);
    void markAllAsRead();
}
