package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.exception.city.CityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;


@RequiredArgsConstructor
@Service
public class CityService {

    private final CityRepository cityRepository;

    @Transactional(readOnly = true)
    public CityDto getCityByName(String name){
        return new CityDto(cityRepository.findByIgnoreCaseName(name).orElseThrow(CityNotFoundException::new));
    }

    @Transactional
    public City getCityByNameOrCreate(String cityName) {

        if (cityName == null || cityName.isBlank())
            throw new IllegalArgumentException("City name cannot be null or blank");

        String normalized = cityName.toLowerCase(Locale.ROOT);

        return cityRepository.findByIgnoreCaseName(normalized)
                .orElseGet(() -> {
                    try {
                        return cityRepository.save(new City(normalized));
                    } catch (DataIntegrityViolationException e) {
                        // someone else inserted it concurrently
                        return cityRepository.findByIgnoreCaseName(normalized)
                                .orElseThrow();
                    }
                });
    }

}
