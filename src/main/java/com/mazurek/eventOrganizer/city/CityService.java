package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.exception.city.CityNotFoundException;
import com.mazurek.eventOrganizer.validators.LookupNameValidator;
import com.mazurek.eventOrganizer.validators.ValidationConstraints;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;


@RequiredArgsConstructor
@Service
public class CityService {

    private final CityRepository cityRepository;

    @Transactional(readOnly = true)
    public CityDto getCityByName(String name){
        City city = getCityByNameOrThrow(name);
        CityDto dto = new CityDto(city);
        dto.setEventCount(cityRepository.countEventsByCityId(city.getId()));
        return dto;
    }

    @Transactional(readOnly = true)
    public City getCityByNameOrThrow(String name) {
        LookupNameValidator.requireValid(
                name,
                ValidationConstraints.CITY_MIN_LENGTH,
                ValidationConstraints.CITY_MAX_LENGTH,
                "City name"
        );
        return cityRepository.findByIgnoreCaseName(name.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(CityNotFoundException::new);
    }

    @Transactional
    public City getCityByNameOrCreate(String cityName) {

        LookupNameValidator.requireValid(
                cityName,
                ValidationConstraints.CITY_MIN_LENGTH,
                ValidationConstraints.CITY_MAX_LENGTH,
                "City name"
        );

        String normalized = cityName.trim().toLowerCase(Locale.ROOT);
        var existing = cityRepository.findByIgnoreCaseName(normalized);
        if (existing.isPresent()) {
            return existing.get();
        }
        cityRepository.insertIfAbsent(UUID.randomUUID(), normalized);
        return cityRepository.findByIgnoreCaseName(normalized).orElseThrow();
    }

}
