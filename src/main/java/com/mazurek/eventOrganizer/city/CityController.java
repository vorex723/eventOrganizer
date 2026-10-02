package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.city.cityLookupClient.CitySearchResult;
import com.mazurek.eventOrganizer.event.EventService;
import com.mazurek.eventOrganizer.event.dto.EventOverviewPageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cities")
public class CityController {
    private final CityService cityService;
    private final EventService eventService;

    @GetMapping("/search")
    public ResponseEntity<List<CitySearchResult>> search(
            @RequestParam("q") String query,
            @RequestParam(name = "countryBias", required = false) String countryBias) {
        return ResponseEntity.ok(cityService.search(query, countryBias));
    }

    @GetMapping("/{cityId}/events")
    public ResponseEntity<EventOverviewPageDto> getCityEvents(
            @PathVariable UUID cityId, @RequestParam(name = "page", defaultValue = "0") int page) {
        return ResponseEntity.ok(eventService.getCityEventsByCityId(cityId, page));
    }

    @GetMapping("/{cityId}")
    public ResponseEntity<CityDto> getCityById(@PathVariable UUID cityId) {
        return ResponseEntity.ok(cityService.getCityById(cityId));
    }
}
