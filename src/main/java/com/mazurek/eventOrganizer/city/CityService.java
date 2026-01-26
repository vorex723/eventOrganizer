package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.exception.city.CityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;


@RequiredArgsConstructor
@Service
public class CityService {

    private final CityRepository cityRepository;

    public CityDto getCityByName(String name){
        return new CityDto(cityRepository.findByIgnoreCaseName(name).orElseThrow(CityNotFoundException::new));
    }

    public City getCityByNameOrCreate(String cityName){
        /* city list needed for additional city name verifying */
        if (cityName == null || cityName.isBlank())
            return null;

        Optional<City> cityOptional = cityRepository.findByIgnoreCaseName(cityName);
        return cityOptional.orElseGet(() -> cityRepository.save(new City(cityName.toLowerCase())));
    }


}
