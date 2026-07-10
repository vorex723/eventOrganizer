package com.mazurek.eventOrganizer.notification.dto;

import com.mazurek.eventOrganizer.notification.domain.Notification;
import org.springframework.data.domain.Page;

import java.util.List;


public record NotificationPageDto(
        List<NotificationDto> notifications,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean lastPage
) {
    public NotificationPageDto(Page<Notification> page) {
        this(
                page.getContent().stream().map(NotificationDto::new).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
