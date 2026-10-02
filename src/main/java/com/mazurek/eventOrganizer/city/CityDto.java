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
    private String externalId;
    private String name;
    private String countryCode;
    private String adminArea;
    private double latitude;
    private double longitude;
    private long eventCount;

    public CityDto(City city) {
        this.id = city.getId();
        this.externalId = city.getExternalId();
        this.name = city.getName();
        this.countryCode = city.getCountryCode();
        this.adminArea = city.getAdminArea();
        this.latitude = city.getLatitude();
        this.longitude = city.getLongitude();
    }
}
