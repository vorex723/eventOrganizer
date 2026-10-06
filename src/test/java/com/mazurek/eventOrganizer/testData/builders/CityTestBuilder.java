package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.city.City;

import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;

public class CityTestBuilder {

    private UUID id = CitiesConstants.WARSAW_ID;
    private String name = CitiesConstants.WARSAW_NAME;
    private String externalId = CitiesConstants.WARSAW_EXTERNAL_ID;
    private String countryCode = CitiesConstants.DEFAULT_COUNTRY_CODE;
    private String adminArea = CitiesConstants.DEFAULT_ADMIN_AREA;
    private String timeZoneId = CitiesConstants.DEFAULT_TIME_ZONE_ID;
    private double latitude = CitiesConstants.DEFAULT_LATITUDE;
    private double longitude = CitiesConstants.DEFAULT_LONGITUDE;

    public static CityTestBuilder warsaw() {
        return new CityTestBuilder()
                .id(CitiesConstants.WARSAW_ID)
                .name(CitiesConstants.WARSAW_NAME);
    }

    public static CityTestBuilder krakow() {
        return new CityTestBuilder()
                .id(CitiesConstants.KRAKOW_ID)
                .name(CitiesConstants.KRAKOW_NAME)
                .externalId(CitiesConstants.KRAKOW_EXTERNAL_ID);
    }

    public static CityTestBuilder systemCity() {
        return new CityTestBuilder()
                .id(CitiesConstants.SYSTEM_CITY_ID)
                .name(CitiesConstants.SYSTEM_CITY_NAME)
                .externalId(CitiesConstants.SYSTEM_CITY_EXTERNAL_ID);
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
        City city = new City(externalId,
                name, countryCode, adminArea, latitude, longitude, timeZoneId);
        city.setId(id);
        return city;
    }

    public CityTestBuilder timeZoneId(String timeZoneId) {
        this.timeZoneId = timeZoneId;
        return this;
    }

    public CityTestBuilder latitude(double latitude) {
        this.latitude = latitude;
        return this;
    }

    public CityTestBuilder longitude(double longitude) {
        this.longitude = longitude;
        return this;
    }
}
