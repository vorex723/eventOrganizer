package com.mazurek.eventOrganizer.notification.dto;

import com.mazurek.eventOrganizer.notification.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
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
    Date createDate;

}
