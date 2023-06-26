package com.mazurek.eventOrganizer.city;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;


@RequiredArgsConstructor
@Component
public class CityUtils {
    private final CityRepository cityRepository;

    public City resolveCity(String cityName){
        /* city list needed for additional city name verifying */
        if (cityName == null || cityName.isBlank())
            return null;

        Optional<City> cityOptional = cityRepository.findByName(cityName);
        return cityOptional.orElseGet(() -> cityRepository.save(new City(cityName)));
    }
}
