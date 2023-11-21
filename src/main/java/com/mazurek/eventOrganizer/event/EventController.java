package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.dto.EventWithUsersDto;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.WrongEventOwnerException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@Controller
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    @GetMapping("/{id}")
    public ResponseEntity<?> getEventById(@PathVariable("id") Long id){
        EventWithUsersDto returnedEvent;
        try {

            return ResponseEntity.status(HttpStatus.OK).body(eventService.getEventById(id));
        }
        catch (EventNotFoundException e){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("Message" ,e.getMessage()));
        }
    }
    @PostMapping
    public ResponseEntity<?> createEvent(@Valid @RequestBody EventCreateDto eventCreateDto,
                                         @RequestHeader("Authorization") String jwt)
    {
        /*
        * Add error handling!!!!!!!!!!!!!!!!!!!!!!!!
        * */
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(eventCreateDto, jwt.substring(7)));
        }
        catch(RuntimeException e){
            return ResponseEntity.ok().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEvent(@Valid @RequestBody EventCreateDto eventUpdateDto,
                                         @PathVariable("id") Long id,
                                         @RequestHeader("Authorization") String jwt)
    {
        try{
            return  ResponseEntity.ok(eventService.updateEvent(eventUpdateDto, id, jwt.substring(7)));
        }
        catch (EventNotFoundException exception){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("Message", "There is no event with that id."));
        }
        catch (WrongEventOwnerException exception){
            return ResponseEntity.ok(Collections.singletonMap("Message", "You are not owner of this event!"));
        }

    }

    @PostMapping({"/{id}/attend"})
    public ResponseEntity<?> attendEvent(@PathVariable("id") Long id,
                                         @RequestHeader("Authorization") String jwt)
    {
        try {
            return  ResponseEntity.ok(eventService.addAttenderToEvent(id, jwt.substring(7)));
        }
        catch (EventNotFoundException exception){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("Message", "You can not attend not existing event"));
        }

    }



}
