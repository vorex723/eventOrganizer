package com.mazurek.eventOrganizer.city.cityLookupClient;

public record CitySearchResult(
        String externalId,
        String displayName,
        String countryCode,
        String countryName,
        String adminArea,
        String timeZoneId
) {
}
