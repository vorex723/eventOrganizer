package com.mazurek.eventOrganizer.testData.builders.dto;

import com.mazurek.eventOrganizer.event.dto.EventDto;
import com.mazurek.eventOrganizer.testData.builders.EventTestBuilder;

import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.EventConstants.FIRST_EVENT_ID;

/** Fresh service-return fixture; defaults reuse the corresponding domain builder. */
public class EventDtoTestBuilder {
    private UUID id = FIRST_EVENT_ID;

    public EventDtoTestBuilder id(UUID id) {
        this.id = id;
        return this;
    }

    public EventDto build() {
        return new EventDto(EventTestBuilder.firstEvent().id(id).build());
    }
}
