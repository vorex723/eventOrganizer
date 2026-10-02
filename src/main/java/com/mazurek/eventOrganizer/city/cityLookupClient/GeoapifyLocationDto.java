package com.mazurek.eventOrganizer.city.cityLookupClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeoapifyLocationDto(

        @JsonProperty("place_id")
        String placeId,

        String name,
        String city,
        String state,
        String country,

        @JsonProperty("country_code")
        String countryCode,

        String formatted,

        @JsonProperty("result_type")
        String resultType,

        @JsonProperty("feature_type")
        String featureType,

        Double lat,
        Double lon,
        GeoapifyTimeZoneDto timezone
) {
}
