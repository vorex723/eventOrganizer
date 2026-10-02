package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.city.City;

import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;

public class CityTestBuilder {

    private UUID id = CitiesConstants.WARSAW_ID;
    private String name = CitiesConstants.WARSAW_NAME;
    private String externalId;
    private String countryCode = "PL";
    private String adminArea = "Test region";
    private String timeZoneId = "Europe/Warsaw";

    public static CityTestBuilder warsaw() {
        return new CityTestBuilder()
                .id(CitiesConstants.WARSAW_ID)
                .name(CitiesConstants.WARSAW_NAME);
    }

    public static CityTestBuilder krakow() {
        return new CityTestBuilder()
                .id(CitiesConstants.KRAKOW_ID)
                .name(CitiesConstants.KRAKOW_NAME);
    }

    public static CityTestBuilder systemCity() {
        return new CityTestBuilder()
                .id(CitiesConstants.SYSTEM_CITY_ID)
                .name(CitiesConstants.SYSTEM_CITY_NAME);
    }

    public CityTestBuilder id(UUID id) {
        this.id = id;
        return this;
    }

    public CityTestBuilder name(String name) {
        this.name = name;
        return this;
    }

    public CityTestBuilder externalId(String externalId) {
        this.externalId = externalId;
        return this;
    }

    public CityTestBuilder countryCode(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    public CityTestBuilder adminArea(String adminArea) {
        this.adminArea = adminArea;
        return this;
    }

    public City build() {
        City city = new City(externalId == null ? com.mazurek.eventOrganizer.testData.TestCityData.externalId(name) : externalId,
                name, countryCode, adminArea, 52.2297, 21.0122, timeZoneId);
        city.setId(id);
        return city;
    }

    public CityTestBuilder timeZoneId(String timeZoneId) {
        this.timeZoneId = timeZoneId;
        return this;
    }
}
