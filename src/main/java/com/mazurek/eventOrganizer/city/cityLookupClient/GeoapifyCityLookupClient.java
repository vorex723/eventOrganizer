package com.mazurek.eventOrganizer.city.cityLookupClient;

import com.mazurek.eventOrganizer.config.properties.GeoapifyProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class GeoapifyCityLookupClient implements CityLookupClient {

    private static final int MAX_RESULTS = 10;
    private static final int MIN_QUERY_LENGTH = 2;

    private static final String RESULT_LANGUAGE = "en";
    private static final String CITY_RESULT_TYPE = "city";
    private static final String DETAILS_FEATURE_TYPE = "details";

    private final RestClient restClient;
    private final GeoapifyProperties properties;

    public GeoapifyCityLookupClient(
            RestClient geoapifyRestClient,
            GeoapifyProperties properties
    ) {
        this.restClient = geoapifyRestClient;
        this.properties = properties;
    }

    @Override
    public List<CitySearchResult> search(
            String query,
            String countryBias
    ) {
        String normalizedQuery = normalizeQuery(query);

        if (normalizedQuery.length() < MIN_QUERY_LENGTH) {
            return List.of();
        }

        String normalizedCountryBias =
                normalizeCountryBias(countryBias);

        try {
            GeoapifyAutocompleteResponse response =
                    restClient.get()
                            .uri(uriBuilder -> {
                                uriBuilder
                                        .path("/v1/geocode/autocomplete")
                                        .queryParam("text", normalizedQuery)
                                        .queryParam("type", CITY_RESULT_TYPE)
                                        .queryParam("limit", MAX_RESULTS)
                                        .queryParam("lang", RESULT_LANGUAGE)
                                        .queryParam("format", "json")
                                        .queryParam("apiKey", properties.apiKey());

                                if (normalizedCountryBias != null) {
                                    uriBuilder.queryParam("bias", "countrycode:" + normalizedCountryBias);
                                } else {
                                    // Geoapify may use backend ip bias automatically so need zeroing it
                                    uriBuilder.queryParam("bias", "countrycode:none");
                                }

                                return uriBuilder.build();
                            })
                            .retrieve()
                            .body(GeoapifyAutocompleteResponse.class);

            if (response == null || response.results() == null) {
                return List.of();
            }

            return response.results()
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(this::isSupportedCity)
                    .filter(place -> place.placeId() != null && !place.placeId().isBlank())
                    .map(this::mapSearchResult)
                    .toList();

        } catch (CityLookupException exception) {
            throw exception;

        } catch (Exception exception) {
            throw new CityLookupException("Geoapify search failed for query: " + normalizedQuery, exception);
        }
    }

    @Override
    public ResolvedCity getById(String externalId) {
        String normalizedExternalId = normalizeExternalId(externalId);

        try {
            GeoapifyPlaceDetailsResponse response =
                    restClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/v2/place-details")
                                    .queryParam("id", normalizedExternalId)
                                    .queryParam("features", DETAILS_FEATURE_TYPE)
                                    .queryParam("lang", RESULT_LANGUAGE)
                                    .queryParam("apiKey", properties.apiKey())
                                    .build()
                            )
                            .retrieve()
                            .body(GeoapifyPlaceDetailsResponse.class);

            if (response == null || response.features() == null || response.features().isEmpty()) {

                throw new CityLookupException("Geoapify returned no place for id: " + normalizedExternalId);
            }

            GeoapifyLocationDto place =
                    response.features()
                            .stream()
                            .filter(Objects::nonNull)
                            .map(GeoapifyFeatureDto::properties)
                            .filter(Objects::nonNull)
                            .filter(properties ->
                                    DETAILS_FEATURE_TYPE.equals(
                                            properties.featureType()
                                    )
                            )
                            .findFirst()
                            .orElseThrow(() ->
                                    new CityLookupException("Geoapify returned no details for id: " + normalizedExternalId)
                            );

            return mapResolvedCity(
                    normalizedExternalId,
                    place
            );

        } catch (CityLookupException exception) {
            throw exception;

        } catch (Exception exception) {
            throw new CityLookupException("Geoapify lookup failed for id: " + normalizedExternalId, exception);
        }
    }

    private CitySearchResult mapSearchResult(
            GeoapifyLocationDto place
    ) {
        return new CitySearchResult(
                place.placeId(),
                resolveDisplayName(place),
                normalizeCountryCode(place.countryCode()),
                place.country(),
                place.state()
        );
    }

    private ResolvedCity mapResolvedCity(
            String externalId,
            GeoapifyLocationDto place
    ) {
        if (place.lat() == null || place.lon() == null) {
            throw new CityLookupException("Geoapify returned place without coordinates: " + externalId);
        }

        return new ResolvedCity(
                externalId,
                resolveDisplayName(place),
                normalizeCountryCode(place.countryCode()),
                place.country(),
                place.state(),
                place.lat(),
                place.lon()
        );
    }

    private String resolveDisplayName(
            GeoapifyLocationDto place
    ) {
        if (place.name() != null && !place.name().isBlank()) {
            return place.name();
        }

        if (place.city() != null && !place.city().isBlank()) {
            return place.city();
        }

        if (place.formatted() != null && !place.formatted().isBlank()) {
            return place.formatted();
        }

        throw new CityLookupException("Geoapify returned place without usable name");
    }

    private String normalizeQuery(
            String query
    ) {
        if (query == null) {
            return "";
        }

        return query.strip();
    }

    private String normalizeExternalId(
            String externalId
    ) {
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("externalId cannot be blank");
        }

        return externalId.strip();
    }

    private String normalizeCountryBias(
            String countryBias
    ) {
        if (countryBias == null || countryBias.isBlank()) {
            return null;
        }

        String normalized = countryBias.strip().toLowerCase(Locale.ROOT);

        if (!normalized.matches("[a-z]{2}")) {
            throw new IllegalArgumentException("countryBias must be a two-letter ISO country code");
        }

        return normalized;
    }

    private String normalizeCountryCode(
            String countryCode
    ) {
        if (countryCode == null) {
            return null;
        }

        return countryCode
                .toUpperCase(Locale.ROOT);
    }

    private boolean isSupportedCity(
            GeoapifyLocationDto place
    ) {
        return CITY_RESULT_TYPE.equals(place.resultType());
    }
}