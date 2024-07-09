package com.mazurek.eventOrganizer.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class NotificationsPageDto {

    List<NotificationDto> notifications;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;


}
