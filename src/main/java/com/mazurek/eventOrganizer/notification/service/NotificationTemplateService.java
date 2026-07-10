package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.notification.domain.NotificationTemplate;

public interface NotificationTemplateService {
    NotificationTemplate buildPrivateMessage(String senderFullName);
    NotificationTemplate buildThreadReply(String replierFullName);
    NotificationTemplate buildEventUpdate(String eventName);
}
