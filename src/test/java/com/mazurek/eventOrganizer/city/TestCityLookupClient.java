package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.city.cityLookupClient.CityLookupClient;
import com.mazurek.eventOrganizer.city.cityLookupClient.CityLookupException;
import com.mazurek.eventOrganizer.city.cityLookupClient.CitySearchResult;
import com.mazurek.eventOrganizer.city.cityLookupClient.ResolvedCity;
import com.mazurek.eventOrganizer.testData.TestCityData;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/** Offline provider fixture. The external integration test explicitly selects the real adapter. */
@Component
@Primary
@Profile("test")
public class TestCityLookupClient implements CityLookupClient {
    @Override
    public List<CitySearchResult> search(String query, String countryBias) {
        if (query == null || query.strip().length() < 2) return List.of();
        if (countryBias != null && !countryBias.isBlank() && !countryBias.strip().matches("[a-zA-Z]{2}")) {
            throw new IllegalArgumentException("countryBias must be a two-letter ISO country code");
        }
        String id = TestCityData.externalId(query);
        return List.of(new CitySearchResult(id, TestCityData.name(id), "PL", "Poland", "Test region"));
    }

    @Override
    public ResolvedCity getById(String externalId) {
        if (externalId == null || !externalId.startsWith("test:") || externalId.length() <= 5) {
            throw new CityLookupException("Unknown test place");
        }
        return new ResolvedCity(externalId, TestCityData.name(externalId), "PL", "Poland", "Test region", 52.2297, 21.0122,
                TestCityData.timeZoneId(externalId));
    }
}
