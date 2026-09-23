package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.event.EventService;
import com.mazurek.eventOrganizer.event.dto.EventOverviewPageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cities")
public class CityController {

    private final CityService cityService;
    private final EventService eventService;

    @GetMapping("/{cityName}/events")
    public ResponseEntity<EventOverviewPageDto> getCityEvents(
            @PathVariable("cityName") String cityName,
            @RequestParam(name = "page", defaultValue = "0") int page) {
        return ResponseEntity.ok(eventService.getCityEventsByCityName(cityName, page));
    }

    @GetMapping("/{cityName}")
    public ResponseEntity<CityDto> getCityByName(@PathVariable("cityName") String cityName){
        return ResponseEntity.ok(cityService.getCityByName(cityName));
    }
}
