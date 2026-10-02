package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.city.cityLookupClient.CityLookupClient;
import com.mazurek.eventOrganizer.city.cityLookupClient.CityLookupException;
import com.mazurek.eventOrganizer.city.cityLookupClient.CitySearchResult;
import com.mazurek.eventOrganizer.city.cityLookupClient.ResolvedCity;
import com.mazurek.eventOrganizer.exception.city.CityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CityServiceImpl implements CityService {

    private final CityRepository cityRepository;
    private final CityLookupClient cityLookupClient;

    @Override
    @Transactional(readOnly = true)
    public CityDto getCityById(UUID id) {
        City city = getCityByIdOrThrow(id);
        CityDto dto = new CityDto(city);
        dto.setEventCount(cityRepository.countEventsByCityId(id));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public City getCityByIdOrThrow(UUID id) {
        return cityRepository.findById(id).orElseThrow(CityNotFoundException::new);
    }

    @Override
    @Transactional
    public City resolve(String externalId) {
        if (externalId == null || externalId.isBlank() || externalId.strip().length() > 255) {
            throw new IllegalArgumentException("externalId must contain 1 to 255 characters");
        }
        String normalized = externalId.strip();
        var existing = cityRepository.findByExternalId(normalized);
        if (existing.isPresent()) {
            return existing.get();
        }

        ResolvedCity resolved = cityLookupClient.getById(normalized);
        if (resolved == null || !normalized.equals(resolved.externalId())) {
            throw new CityLookupException("City lookup returned an unexpected identifier");
        }

        City city;
        try {
            city = new City(
                    resolved.externalId(), resolved.name(), resolved.countryCode(),
                    resolved.adminArea(), resolved.latitude(), resolved.longitude(), resolved.timeZoneId());
        } catch (IllegalArgumentException exception) {
            throw new CityLookupException("City lookup returned invalid city data", exception);
        }

        // The unique constraint and conflict-safe insert also handle concurrent requests.
        cityRepository.insertIfAbsent(
                UUID.randomUUID(), city.getExternalId(), city.getName(),
                city.getCountryCode(), city.getAdminArea(), city.getLatitude(), city.getLongitude(), city.getTimeZoneId());
        return cityRepository.findByExternalId(normalized)
                .orElseThrow(() -> new IllegalStateException("Resolved city was not persisted"));
    }

    @Override
    public List<CitySearchResult> search(String query, String countryBias) {
        return cityLookupClient.search(query, countryBias);
    }
}
