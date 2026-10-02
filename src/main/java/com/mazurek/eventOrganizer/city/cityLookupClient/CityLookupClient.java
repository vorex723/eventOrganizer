package com.mazurek.eventOrganizer.city.cityLookupClient;

import java.util.List;

public interface CityLookupClient {
    List<CitySearchResult> search(
            String query,
            String countryBias
    );

    ResolvedCity getById(String externalId);
}