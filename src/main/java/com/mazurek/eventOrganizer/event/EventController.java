package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.dto.EventWithUsersDto;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.NotAttenderException;
import com.mazurek.eventOrganizer.exception.event.NotEventOwnerException;
import com.mazurek.eventOrganizer.exception.thread.NotThreadOwnerException;
import com.mazurek.eventOrganizer.exception.thread.ThreadNotFoundException;
import com.mazurek.eventOrganizer.exception.thread.ThreadReplyNotFoundException;
import com.mazurek.eventOrganizer.exception.thread.WrongThreadException;
import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadReplayCreateDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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
        catch (RuntimeException exception){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
        }
    }
    @PostMapping
    @Transactional
    public ResponseEntity<?> createEvent(@Valid @RequestBody EventCreateDto eventCreateDto,
                                         @RequestHeader("Authorization") String jwt)
    {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(eventCreateDto, jwt.substring(7)));
        }
        catch (RuntimeException exception){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
        }

    }

    @PutMapping("/{id}")
    @Transactional
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
        catch (NotEventOwnerException exception){
            return ResponseEntity.ok(Collections.singletonMap("Message", "You are not owner of this event!"));
        }
        catch (RuntimeException exception){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
        }

    }

    @PostMapping("/{id}/attend")
    public ResponseEntity<?> attendEvent(@PathVariable("id") Long id,
                                         @RequestHeader("Authorization") String jwt)
    {
        try {
            return  ResponseEntity.ok(eventService.addAttenderToEvent(id, jwt.substring(7)));
        }
        catch (EventNotFoundException exception){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("Message", "You can not attend not existing event"));
        }
        catch (RuntimeException exception){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
        }
    }

    @PostMapping("/{id}/threads")
    public ResponseEntity<?> createNewThreadInEvent(@PathVariable("id") Long eventId,
                                                    @RequestBody ThreadCreateDto threadCreateDto,
                                                    @RequestHeader("Authorization") String jwt){
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createThreadInEvent(threadCreateDto, eventId, jwt.substring(7)));
        }
        catch (EventNotFoundException exception){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("Message", exception.getMessage()));
        }
        catch (NotAttenderException exception){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("Message", exception.getMessage()));
        }
        catch (RuntimeException exception){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
        }
    }

    @PutMapping("/{eventId}/threads/{threadId}")
    public ResponseEntity<?> updateThreadInEvent(@PathVariable("eventId") Long eventId,
                                                 @PathVariable("threadId") Long threadId,
                                                 @RequestBody ThreadCreateDto threadUpdateDto,
                                                 @RequestHeader("Authorization") String jwt)
    {
        try{
            return ResponseEntity.ok(eventService.updateThreadInEvent(threadUpdateDto, eventId,threadId, jwt.substring(7)));
        }
        catch (EventNotFoundException | ThreadNotFoundException exception){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("Message", exception.getMessage()));
        }
        catch (NotAttenderException | NotThreadOwnerException exception){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("Message", exception.getMessage()));
        }
        catch (RuntimeException exception){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
        }

    }

    @PostMapping("/{eventId}/threads/{threadId}/replies")
    public ResponseEntity<?> createReplyInThread(@PathVariable("eventId") Long eventId,
                                                    @PathVariable("threadId") Long threadId,
                                                    @RequestBody ThreadReplayCreateDto threadReplayCreateDto,
                                                    @RequestHeader("Authorization") String jwt){
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createReplyInThread(threadReplayCreateDto, eventId, threadId, jwt.substring(7)));
        }
        catch (EventNotFoundException exception){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("Message", exception.getMessage()));
        }
        catch (NotAttenderException exception){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("Message", exception.getMessage()));
        }
        catch (RuntimeException exception){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
        }
    }

    @PutMapping("/{eventId}/threads/{threadId}/replies/{replyId}")
    public ResponseEntity<?> updateReplyInThread(@PathVariable("eventId") Long eventId,
                                                 @PathVariable("threadId") Long threadId,
                                                 @PathVariable("replyId") Long replyId,
                                                 @RequestBody ThreadReplayCreateDto threadReplayCreateDto,
                                                 @RequestHeader("Authorization") String jwt){
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(eventService.updateThreadReplyInEvent(threadReplayCreateDto, eventId, threadId, replyId,jwt.substring(7)));
        }
        catch (EventNotFoundException | ThreadNotFoundException | ThreadReplyNotFoundException exception){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("Message", exception.getMessage()));
        }
        catch (NotAttenderException | WrongThreadException exception){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("Message", exception.getMessage()));
        }
        catch (RuntimeException exception){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
        }
    }



}
