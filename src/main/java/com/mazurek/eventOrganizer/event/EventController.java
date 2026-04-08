package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.dto.EventDto;
import com.mazurek.eventOrganizer.event.dto.EventOverviewPageDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;


    @PostMapping
    public ResponseEntity<EventDto> createEvent(@Valid @RequestBody EventCreateDto eventCreateDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(eventCreateDto));
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<EventDto> updateEvent(
            @Valid @RequestBody EventCreateDto eventUpdateDto,
            @PathVariable UUID eventId) {
        return ResponseEntity.ok(eventService.updateEvent(eventUpdateDto, eventId));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventDto> getEventById(@PathVariable UUID eventId) {
        return ResponseEntity.ok(eventService.getEventById(eventId));
    }

    @GetMapping
    public ResponseEntity<EventOverviewPageDto> getEvents(
            @RequestParam(name = "page", defaultValue = "0") int page) {
        return ResponseEntity.ok(eventService.getEvents(page));
    }


    @PostMapping("/{eventId}/attend")
    public ResponseEntity<Void> attendEvent(@PathVariable UUID eventId) {
        eventService.addAttenderToEvent(eventId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{eventId}/attend")
    public ResponseEntity<Void> leaveEvent(@PathVariable UUID eventId) {
        eventService.removeAttenderFromEvent(eventId);
        return ResponseEntity.ok().build();
    }
}
