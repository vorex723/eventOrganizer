package com.mazurek.eventOrganizer.event.dto;

import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.user.dto.UserProfileDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    private List<String> tags = new ArrayList<>();
    private UserProfileDto owner;
    @Builder.Default
    private List<UserProfileDto> attendingUsers = new ArrayList<>();
    private ZonedDateTime eventStartDate;
    private ZonedDateTime createDate;
    private ZonedDateTime lastUpdate;

    public EventDto(Event event) {
        this.id = event.getId();
        this.name = event.getName();
        this.shortDescription = event.getShortDescription();
        this.longDescription = event.getLongDescription();
        this.city = event.getCity().getName().substring(0,1).toUpperCase() + event.getCity().getName().substring(1);
        this.exactAddress = event.getExactAddress();
        this.tags = event.getTags().stream().map(Tag::getName).toList();
        this.owner = new UserProfileDto(event.getOwner());
        this.attendingUsers = event.getAttendingUsers().stream().map(UserProfileDto::new).toList();
        this.eventStartDate = event.getEventStartDate().withZoneSameInstant(ZoneId.of(event.getTimeZoneId()));
        this.createDate = event.getCreateDate().withZoneSameInstant(ZoneId.of(event.getTimeZoneId()));
        this.lastUpdate = event.getLastUpdate().withZoneSameInstant(ZoneId.of(event.getTimeZoneId()));
    }
}
