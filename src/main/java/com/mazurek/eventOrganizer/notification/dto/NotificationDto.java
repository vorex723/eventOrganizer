package com.mazurek.eventOrganizer.notification.dto;

import com.mazurek.eventOrganizer.notification.Notification;
import com.mazurek.eventOrganizer.notification.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class NotificationDto {

    UUID id;
    String title;
    String body;
    boolean opened;
    NotificationType type;
    UUID resourceId;
    LocalDateTime createDate;

    public NotificationDto(Notification notification) {
        this.id = notification.getId();
        this.title = notification.getTitle();
        this.body = notification.getBody();
        this.opened = notification.getOpened();
        this.type = notification.getType();
        this.resourceId = notification.getResourceId();
        this.createDate = notification.getCreateDate();
    }
}
