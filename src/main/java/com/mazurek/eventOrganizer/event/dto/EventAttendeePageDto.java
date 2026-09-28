package com.mazurek.eventOrganizer.event.dto;

import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.dto.UserProfileDto;
import org.springframework.data.domain.Page;

import java.util.List;

public record EventAttendeePageDto(
        List<UserProfileDto> attendees,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean lastPage
) {
    public EventAttendeePageDto(Page<User> attendeePage) {
        this(
                attendeePage.getContent().stream().map(UserProfileDto::new).toList(),
                attendeePage.getNumber(),
                attendeePage.getSize(),
                attendeePage.getTotalElements(),
                attendeePage.getTotalPages(),
                attendeePage.isLast()
        );
    }
}
