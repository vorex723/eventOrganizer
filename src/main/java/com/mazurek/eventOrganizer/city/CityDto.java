package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.event.dto.EventOverviewDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CityDto {
    private UUID id;
    private String name;
    private List<EventOverviewDto> events;

    public CityDto(City city) {
        this.id = city.getId();
        this.name = city.getName();
        this.events = city.getEvents().stream().map(EventOverviewDto::new).toList();
    }
}
