package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.dto.EventWithUsersDto;
import com.mazurek.eventOrganizer.exception.event.*;
import com.mazurek.eventOrganizer.exception.file.FileNotFoundException;
import com.mazurek.eventOrganizer.exception.file.FileTypeNotAllowedException;
import com.mazurek.eventOrganizer.exception.search.NoSearchParametersPresentException;
import com.mazurek.eventOrganizer.exception.search.NoSearchResultException;
import com.mazurek.eventOrganizer.exception.thread.*;
import com.mazurek.eventOrganizer.file.File;
import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadReplayCreateDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;
    @GetMapping("/{eventId}")
    public ResponseEntity<?> getEventById(@PathVariable("eventId") Long eventId){
        EventWithUsersDto returnedEvent;
        try {
            return ResponseEntity.status(HttpStatus.OK).body(eventService.getEventById(eventId));
        }
        catch (EventNotFoundException exception){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("Message" ,exception.getMessage()));
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
        } catch (InvalidEventStartDateException exception){
            return ResponseEntity.badRequest().body(Collections.singletonMap("Message", exception.getMessage()));
        }
        catch (RuntimeException exception){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
        }

    }

    @PutMapping("/{eventId}")
    @Transactional
    public ResponseEntity<?> updateEvent(@Valid @RequestBody EventCreateDto eventUpdateDto,
                                         @PathVariable("eventId") Long eventId,
                                         @RequestHeader("Authorization") String jwt)
    {
        try{
            return  ResponseEntity.ok(eventService.updateEvent(eventUpdateDto, eventId, jwt.substring(7)));
        }
        catch (EventNotFoundException exception){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("Message", "There is no event with that id."));
        }
        catch (NotEventOwnerException | EventAlreadyHadPlaceException exception){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("Message", exception.getMessage()));
        }
        catch (RuntimeException exception){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
        }

    }

    @PostMapping("/{eventId}/attend")
    public ResponseEntity<?> attendEvent(@PathVariable("eventId") Long eventId,
                                         @RequestHeader("Authorization") String jwt)
    {
        try {
            return  ResponseEntity.ok(eventService.addAttenderToEvent(eventId, jwt.substring(7)));
        }
        catch (EventNotFoundException exception){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("Message", "You can not attend not existing event"));
        } catch (EventAlreadyHadPlaceException exception){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("Message", "You can not attend not existing event"));
        } catch (RuntimeException exception){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
        }
    }

    @PostMapping("/{eventId}/threads")
    public ResponseEntity<?> createNewThreadInEvent(@PathVariable("eventId") Long eventId,
                                                    @Valid @RequestBody ThreadCreateDto threadCreateDto,
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
                                                 @Valid @RequestBody ThreadCreateDto threadUpdateDto,
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
                                                    @Valid @RequestBody ThreadReplayCreateDto threadReplayCreateDto,
                                                    @RequestHeader("Authorization") String jwt){
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createReplyInThread(threadReplayCreateDto, eventId, threadId, jwt.substring(7)));
        }
        catch (EventNotFoundException | ThreadNotFoundException exception){
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
                                                 @Valid @RequestBody ThreadReplayCreateDto threadReplayCreateDto,
                                                 @RequestHeader("Authorization") String jwt){
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(eventService.updateThreadReplyInEvent(threadReplayCreateDto, eventId, threadId, replyId,jwt.substring(7)));
        }
        catch (EventNotFoundException | ThreadNotFoundException | ThreadReplyNotFoundException exception){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("Message", exception.getMessage()));
        }
        catch (NotAttenderException | WrongThreadException | NotThreadReplyOwnerException exception){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("Message", exception.getMessage()));
        }
        catch (RuntimeException exception){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchEvents(@RequestParam(name = "words", required = false) List<String> queryWordList,
                                          @RequestParam(name = "tags", required = false) List<String> tags,
                                          @RequestParam(name = "city", required = false) String cityName){
        try {
            return ResponseEntity.ok(eventService.searchEvents(queryWordList, tags, cityName));
        }
        catch (NoSearchParametersPresentException exception){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("Message",exception.getMessage()));
        }
        catch (NoSearchResultException exception){
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
    }

    @PostMapping("/{eventId}/files")
    public ResponseEntity<?> uploadFileToEvent(@RequestParam(name = "file") MultipartFile uploadedFile,
                                               @PathVariable("eventId") Long eventId,
                                               @RequestHeader("Authorization") String jwt){
        try{
            return ResponseEntity.ok(eventService.uploadFileToEvent(uploadedFile,eventId,jwt.substring(7)));
        } catch (NotAttenderException | IOException | FileTypeNotAllowedException exception){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("Message",exception.getMessage()));
        } catch (EventNotFoundException exception){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("Message",exception.getMessage()));
        } catch (RuntimeException exception){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("Message", "Something went wrong."));
        }
    }
    @GetMapping("/{eventId}/files/{fileId}")
    public ResponseEntity<?> getFileFromEvent(@PathVariable("eventId") Long eventId,
                                              @PathVariable("fileId")UUID fileId,
                                              @RequestHeader("Authorization") String jwt){
        try {
            File fileToServe = eventService.getFile(fileId,eventId,jwt.substring(7));
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(fileToServe.getContentType())).body(fileToServe.getContent());
        }   catch (FileNotFoundException exception){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("Message", exception.getMessage()));
        }   catch (NotAttenderException exception){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("Message", exception.getMessage()));
        }   catch (RuntimeException exception){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("Message", "Something went wrong."));
        }


    }
}
