package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.city.City;

import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;

public class CityTestBuilder {

    private UUID id = CitiesConstants.WARSAW_ID;
    private String name = CitiesConstants.WARSAW_NAME;

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

    public City build() {
        City city = new City();
        city.setId(id);
        city.setName(name);
        return city;
    }
}