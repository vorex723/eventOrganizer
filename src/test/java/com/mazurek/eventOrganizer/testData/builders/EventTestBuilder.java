package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.user.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;

public class EventTestBuilder {

    private UUID id = EventConstants.FIRST_EVENT_ID;
    private String name = EventConstants.FIRST_EVENT_NAME;
    private String shortDescription = EventConstants.FIRST_EVENT_SHORT_DESC;
    private String longDescription = EventConstants.FIRST_EVENT_LONG_DESC;
    private String exactAddress = EventConstants.FIRST_EVENT_ADDRESS;
    private String timeZoneId = UserConstants.FIRST_USER_TIMEZONE;
    private Instant createDate = TimeConstants.NOW;
    private Instant lastUpdate = TimeConstants.NOW;
    private Instant eventStartDate = TimeConstants.ONE_WEEK_FROM_NOW.truncatedTo(ChronoUnit.MINUTES);
    private City city = CityTestBuilder.warsaw().build();
    private User owner = UserTestBuilder.firstUser().build();

    public static EventTestBuilder firstEvent() {
        return new EventTestBuilder()
                .id(EventConstants.FIRST_EVENT_ID)
                .name(EventConstants.FIRST_EVENT_NAME)
                .shortDescription(EventConstants.FIRST_EVENT_SHORT_DESC);
    }

    public static EventTestBuilder secondEvent() {
        return new EventTestBuilder()
                .id(EventConstants.SECOND_EVENT_ID)
                .name(EventConstants.SECOND_EVENT_NAME)
                .shortDescription(EventConstants.SECOND_EVENT_SHORT_DESC)
                .owner(UserTestBuilder.secondUser().build());
    }

    public static EventTestBuilder pastEvent() {
        return new EventTestBuilder()
                .id(EventConstants.PAST_EVENT_ID)
                .name(EventConstants.PAST_EVENT_NAME)
                .eventStartDate(TimeConstants.ONE_WEEK_AGO); // Already happened
    }

    public EventTestBuilder id(UUID id) {
        this.id = id;
        return this;
    }

    public EventTestBuilder name(String name) {
        this.name = name;
        return this;
    }

    public EventTestBuilder shortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
        return this;
    }

    public EventTestBuilder longDescription(String longDescription) {
        this.longDescription = longDescription;
        return this;
    }

    public EventTestBuilder eventStartDate(Instant eventStartDate) {
        this.eventStartDate = eventStartDate;
        return this;
    }

    public EventTestBuilder owner(User owner) {
        this.owner = owner;
        return this;
    }

    public EventTestBuilder city(City city) {
        this.city = city;
        return this;
    }

    public EventTestBuilder createDate(Instant createDate) {
        this.createDate = createDate;
        return this;
    }

    public Event build() {
        Event event = Event.builder()
                .id(id)
                .name(name)
                .shortDescription(shortDescription)
                .longDescription(longDescription)
                .exactAddress(exactAddress)
                .timeZoneId(timeZoneId)
                .createDate(createDate)
                .lastUpdate(lastUpdate)
                .eventStartDate(eventStartDate)
                .city(city)
                .owner(owner)
                .build();

        this.owner.addUserEvent(event);
        this.city.addEvent(event);

        return event;
    }
}
