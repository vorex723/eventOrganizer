package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.event.dto.EventWithoutUsersDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CityDto {
    private UUID id;
    private String name;
    private Set<EventWithoutUsersDto> events;
}
