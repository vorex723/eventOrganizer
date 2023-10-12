package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.event.dto.EventCreationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<?> createEvent(@RequestBody EventCreationDto eventCreationDto, @RequestHeader("Authorization") String jwt){
       return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(eventCreationDto, jwt.substring(7)));

    }

}
