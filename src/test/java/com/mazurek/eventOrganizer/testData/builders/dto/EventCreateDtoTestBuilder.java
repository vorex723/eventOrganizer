package com.mazurek.eventOrganizer.testData.builders.dto;

import com.mazurek.eventOrganizer.event.dto.EventCreateDto;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;

public class EventCreateDtoTestBuilder {

    private String name = EventConstants.FIRST_EVENT_NAME;
    private String shortDescription = EventConstants.FIRST_EVENT_SHORT_DESC;
    private String longDescription = EventConstants.FIRST_EVENT_LONG_DESC;
    private Instant eventStartDate = TimeConstants.ONE_WEEK_FROM_NOW;
    private String city = CitiesConstants.WARSAW_NAME;
    private String exactAddress = EventConstants.FIRST_EVENT_ADDRESS;
    private Set<String> tags = new HashSet<>(TagConstants.DEFAULT_EVENT_TAGS);
    private String timeZone = UserConstants.FIRST_USER_TIMEZONE;

    public static EventCreateDtoTestBuilder firstEvent() {
        return new EventCreateDtoTestBuilder();
    }

    public static EventCreateDtoTestBuilder secondEvent() {
        return new EventCreateDtoTestBuilder()
                .name(EventConstants.SECOND_EVENT_NAME)
                .shortDescription(EventConstants.SECOND_EVENT_SHORT_DESC)
                .longDescription(EventConstants.SECOND_EVENT_LONG_DESC)
                .eventStartDate(TimeConstants.ONE_WEEK_FROM_NOW)
                .city(CitiesConstants.WARSAW_NAME)
                .exactAddress(EventConstants.SECOND_EVENT_ADDRESS);
    }

    public static EventCreateDtoTestBuilder updatedEvent() {
        return new EventCreateDtoTestBuilder()
                .name(EventConstants.EVENT_UPDATE_NAME)
                .shortDescription(EventConstants.EVENT_UPDATE_SHORT_DESCRIPTION)
                .longDescription(EventConstants.EVENT_UPDATE_LONG_DESCRIPTION)
                .eventStartDate(TimeConstants.EVENT_UPDATE_START_DATE)
                .city(EventConstants.EVENT_UPDATE_CITY)
                .exactAddress(EventConstants.EVENT_UPDATE_EXACT_ADDRESS)
                .tags(new HashSet<>(TagConstants.EVENT_UPDATE_TAGS));
    }


    public EventCreateDtoTestBuilder name(String name) {
        this.name = name;
        return this;
    }

    public EventCreateDtoTestBuilder shortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
        return this;
    }

    public EventCreateDtoTestBuilder longDescription(String longDescription) {
        this.longDescription = longDescription;
        return this;
    }

    public EventCreateDtoTestBuilder eventStartDate(Instant eventStartDate) {
        this.eventStartDate = eventStartDate;
        return this;
    }

    public EventCreateDtoTestBuilder city(String city) {
        this.city = city;
        return this;
    }

    public EventCreateDtoTestBuilder exactAddress(String exactAddress) {
        this.exactAddress = exactAddress;
        return this;
    }

    public EventCreateDtoTestBuilder tags(Set<String> tags) {
        this.tags = tags == null ? null : new HashSet<>(tags);
        return this;
    }

    public EventCreateDtoTestBuilder timeZone(String timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    public EventCreateDto build() {
        return EventCreateDto.builder()
                .name(name)
                .shortDescription(shortDescription)
                .longDescription(longDescription)
                .eventStartDate(eventStartDate)
                .city(city)
                .exactAddress(exactAddress)
                .tags(tags == null ? null : new HashSet<>(tags))
                .timeZone(timeZone)
                .build();
    }
}
