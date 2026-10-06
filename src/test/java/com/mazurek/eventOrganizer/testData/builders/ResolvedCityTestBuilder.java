package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.city.cityLookupClient.ResolvedCity;

import static com.mazurek.eventOrganizer.testData.TestConstants.CityLookupConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class ResolvedCityTestBuilder {
    private String externalId = CityLookupConstants.EXTERNAL_ID;
    private String name = CityLookupConstants.NAME;
    private String countryCode = CityLookupConstants.COUNTRY_CODE;
    private String countryName = CityLookupConstants.COUNTRY_NAME;
    private String adminArea = CityLookupConstants.ADMIN_AREA;
    private double latitude = CityLookupConstants.LATITUDE;
    private double longitude = CityLookupConstants.LONGITUDE;
    private String timeZoneId = CityLookupConstants.TIME_ZONE_ID;

    public ResolvedCityTestBuilder externalId(String externalId) {
        this.externalId = externalId;
        return this;
    }

    public ResolvedCityTestBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ResolvedCityTestBuilder countryCode(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    public ResolvedCityTestBuilder countryName(String countryName) {
        this.countryName = countryName;
        return this;
    }

    public ResolvedCityTestBuilder adminArea(String adminArea) {
        this.adminArea = adminArea;
        return this;
    }

    public ResolvedCityTestBuilder latitude(double latitude) {
        this.latitude = latitude;
        return this;
    }

    public ResolvedCityTestBuilder longitude(double longitude) {
        this.longitude = longitude;
        return this;
    }

    public ResolvedCityTestBuilder timeZoneId(String timeZoneId) {
        this.timeZoneId = timeZoneId;
        return this;
    }


    public ResolvedCity build() {
        return new ResolvedCity(
                externalId,
                name,
                countryCode,
                countryName,
                adminArea,
                latitude,
                longitude,
                timeZoneId
        );
    }
}
