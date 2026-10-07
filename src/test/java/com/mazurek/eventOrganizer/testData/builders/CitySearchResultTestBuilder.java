package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.city.cityLookupClient.CitySearchResult;

import static com.mazurek.eventOrganizer.testData.TestConstants.CityLookupConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class CitySearchResultTestBuilder {
    private String externalId = CityLookupConstants.EXTERNAL_ID;
    private String displayName = CityLookupConstants.NAME;
    private String countryCode = CityLookupConstants.COUNTRY_CODE;
    private String countryName = CityLookupConstants.COUNTRY_NAME;
    private String adminArea = CityLookupConstants.ADMIN_AREA;
    private String timeZoneId = CityLookupConstants.TIME_ZONE_ID;

    public CitySearchResultTestBuilder externalId(String externalId) {
        this.externalId = externalId;
        return this;
    }

    public CitySearchResultTestBuilder displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public CitySearchResultTestBuilder countryCode(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    public CitySearchResultTestBuilder countryName(String countryName) {
        this.countryName = countryName;
        return this;
    }

    public CitySearchResultTestBuilder adminArea(String adminArea) {
        this.adminArea = adminArea;
        return this;
    }

    public CitySearchResultTestBuilder timeZoneId(String timeZoneId) {
        this.timeZoneId = timeZoneId;
        return this;
    }

    public CitySearchResult build() {
        return new CitySearchResult(
                externalId,
                displayName,
                countryCode,
                countryName,
                adminArea,
                timeZoneId
        );
    }
}
