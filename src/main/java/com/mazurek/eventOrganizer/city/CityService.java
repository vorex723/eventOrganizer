package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.exception.city.CityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;


    @Transactional
    public CityDto getCityByName(String name){
        return new CityDto(cityRepository.findByIgnoreCaseName(name).orElseThrow(() ->
                new CityNotFoundException("There is no city with that name.")));
    }

}
