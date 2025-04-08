package com.mazurek.eventOrganizer.event.dto;

import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.user.dto.UserProfileDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventOverviewDto {
    private UUID id;
    private String name;
    private String city;
    private UserProfileDto owner;
    private String shortDescription;
    private Date eventStartDate;
    private int amountOfAttenders;
    private List<String> tags;

    public EventOverviewDto(Event event) {
        this.id = event.getId();
        this.name = event.getName();
        this.city = event.getCity().getName();
        this.owner = new UserProfileDto(event.getOwner());
        this.shortDescription = event.getShortDescription();
        this.eventStartDate = event.getEventStartDate();
        this.amountOfAttenders = event.getAttendingUsers().size();
        this.tags = event.getTags().stream().map(Tag::getName).toList();
    }
}
