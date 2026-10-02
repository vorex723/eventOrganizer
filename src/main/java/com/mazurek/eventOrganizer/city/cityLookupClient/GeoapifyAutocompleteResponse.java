package com.mazurek.eventOrganizer.city.cityLookupClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeoapifyAutocompleteResponse(
        List<GeoapifyLocationDto> results
) {
}
