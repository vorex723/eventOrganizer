package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.config.properties.SeedProperties;

import static com.mazurek.eventOrganizer.testData.TestConstants.CityLookupConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.PropertyFixtureConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class SeedPropertiesTestBuilder {
    private boolean localDataEnabled = PropertyFixtureConstants.DISABLED;
    private String cityExternalId = CityLookupConstants.EXTERNAL_ID;
    private String apiBaseUrl = PropertyFixtureConstants.SEED_API_BASE_URL;

    public SeedPropertiesTestBuilder localDataEnabled(boolean localDataEnabled) {
        this.localDataEnabled = localDataEnabled;
        return this;
    }

    public SeedPropertiesTestBuilder cityExternalId(String cityExternalId) {
        this.cityExternalId = cityExternalId;
        return this;
    }

    public SeedPropertiesTestBuilder apiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
        return this;
    }

    public static SeedProperties copyOf(SeedProperties source) {
        if (source == null) return null;
        return new SeedPropertiesTestBuilder()
                .localDataEnabled(source.isLocalDataEnabled())
                .cityExternalId(source.getCityExternalId())
                .apiBaseUrl(source.getApiBaseUrl())
                .build();
    }


    public SeedProperties build() {
        SeedProperties value = new SeedProperties();
        value.setLocalDataEnabled(localDataEnabled);
        value.setCityExternalId(cityExternalId);
        value.setApiBaseUrl(apiBaseUrl);
        return value;
    }
}
