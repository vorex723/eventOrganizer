package com.mazurek.eventOrganizer.event.dto;

import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.user.dto.UserProfileDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventDto {

    private UUID id;
    private String name;
    private String shortDescription;
    private String longDescription;
    private String city;
    private String exactAddress;
    @Builder.Default
    private Set<String> tags = new HashSet<>();
    private UserProfileDto owner;
    @Builder.Default
    private Set<UserProfileDto> attendingUsers = new HashSet<>();
    private String timeZone;
    private Instant eventStartDate;
    private Instant createDate;
    private Instant lastUpdate;

    public EventDto(Event event) {
        this.id = event.getId();
        this.name = event.getName();
        this.shortDescription = event.getShortDescription();
        this.longDescription = event.getLongDescription();
        this.city = event.getCity().getName().substring(0,1).toUpperCase() + event.getCity().getName().substring(1);
        this.exactAddress = event.getExactAddress();
        this.tags = event.getTags().stream().map(Tag::getName).collect(Collectors.toSet());
        this.owner = new UserProfileDto(event.getOwner());
        this.attendingUsers = event.getAttendingUsers().stream().map(UserProfileDto::new).collect(Collectors.toSet());
        this.eventStartDate = event.getEventStartDate();
        this.createDate = event.getCreateDate();
        this.lastUpdate = event.getLastUpdate();
        this.timeZone = event.getTimeZoneId();
        //TIME ZONE ID
    }
}
