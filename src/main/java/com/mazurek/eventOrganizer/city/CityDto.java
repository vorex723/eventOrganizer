package com.mazurek.eventOrganizer.city;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CityDto {
    private UUID id;
    private String name;
    private long eventCount;

    public CityDto(City city) {
        this.id = city.getId();
        this.name = city.getName();
        this.eventCount = 0;
    }
}
