package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.city.cityLookupClient.CitySearchResult;
import java.util.List;
import java.util.UUID;

public interface CityService {
    CityDto getCityById(UUID id);
    City getCityByIdOrThrow(UUID id);
    City resolve(String externalId);
    List<CitySearchResult> search(String query, String countryBias);
}
