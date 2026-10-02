package com.mazurek.eventOrganizer.city.cityLookupClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeoapifyTimeZoneDto(String name) {
}
