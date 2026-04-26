package com.mazurek.eventOrganizer.city;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cities")
public class CityController {

    private final CityService cityService;

    @GetMapping("/{cityName}")
    public ResponseEntity<CityDto> getCityByName(@PathVariable("cityName") String cityName){
        return ResponseEntity.ok(cityService.getCityByName(cityName));
    }
}
