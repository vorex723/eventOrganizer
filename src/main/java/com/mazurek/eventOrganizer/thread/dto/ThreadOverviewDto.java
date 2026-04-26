package com.mazurek.eventOrganizer.thread.dto;

import com.mazurek.eventOrganizer.user.dto.UserProfileDto;
import com.mazurek.eventOrganizer.thread.Thread;

import java.time.Instant;
import java.util.UUID;

public record ThreadOverviewDto(
         UUID id,
         UUID eventId,
         UserProfileDto owner,
         String name,
         int replyCount,
         Instant lastActivity,
         Instant createDate
) {
    public ThreadOverviewDto(Thread thread) {
        this(
                thread.getId(),
                thread.getEvent().getId(),
                new UserProfileDto(thread.getOwner()),
                thread.getName(),
                thread.getReplyCount(),
                thread.getLastActivity(),
                thread.getCreateDate()
        );
    }
}
