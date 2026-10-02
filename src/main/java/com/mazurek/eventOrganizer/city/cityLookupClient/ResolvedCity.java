package com.mazurek.eventOrganizer.city.cityLookupClient;

public record ResolvedCity(
        String externalId,
        String name,
        String countryCode,
        String countryName,
        String adminArea,
        double latitude,
        double longitude,
        String timeZoneId
) {
}
