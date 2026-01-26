package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.dto.EventDto;
import com.mazurek.eventOrganizer.event.dto.EventOverviewDto;
import com.mazurek.eventOrganizer.file.File;
import com.mazurek.eventOrganizer.file.FileOverviewDto;
import com.mazurek.eventOrganizer.file.FileOverviewPageDto;
import com.mazurek.eventOrganizer.file.FileUploadDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadReplyCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadReplyDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;
    private final int TOKEN_PREFIX_LENGTH = 7;


    //**********************************************************************************************************************
    //---------------------------------------------------GET----------------------------------------------------------------
    // *********************************************************************************************************************

    @GetMapping
    public ResponseEntity<List<EventOverviewDto>> getEvents(@RequestParam(name = "page", required = false, defaultValue = "0") int page){
        return ResponseEntity.ok(eventService.getEvents(page));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventDto> getEventById(@PathVariable("eventId") UUID eventId){
        return ResponseEntity.status(HttpStatus.OK).body(eventService.getEventById(eventId));
    }

    @GetMapping("/{eventId}/files")
    public ResponseEntity<FileOverviewPageDto> getFileOverviewsPageFromEventByEventId(@PathVariable("eventId") UUID eventId,
                                                   @RequestParam(name = "page", required = false, defaultValue = "0") int pageNumber,
                                                   @RequestHeader("Authorization") String jwt)
    {
        FileOverviewPageDto fileOverviewPageDto = eventService.getFileOverviewPageByEventId(eventId, pageNumber,jwt.substring(TOKEN_PREFIX_LENGTH));
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(fileOverviewPageDto);
    }
    @GetMapping("/{eventId}/files/{fileId}")
    public ResponseEntity<FileOverviewDto> getFileOverviewFromEventByFileId(@PathVariable("eventId") UUID eventId,
                                                   @PathVariable("fileId")UUID fileId,
                                                   @RequestHeader("Authorization") String jwt)
    {
        FileOverviewDto fileOverviewToServe = eventService.getFileOverviewById(fileId,eventId,jwt.substring(TOKEN_PREFIX_LENGTH));
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(fileOverviewToServe);
    }

    @GetMapping("/{eventId}/files/{fileId}/data")
    public ResponseEntity<byte[]> getFileDataFromEventByFileId(@PathVariable("eventId") UUID eventId,
                                                   @PathVariable("fileId")UUID fileId,
                                                   @RequestHeader("Authorization") String jwt)
    {
        File fileToServe = eventService.getFileDataById(fileId,eventId,jwt.substring(TOKEN_PREFIX_LENGTH));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(fileToServe.getContentType())).body(fileToServe.getContent());
    }

    @GetMapping("/search")
    public ResponseEntity<List<EventOverviewDto>> searchEvents(@RequestParam(name = "words", required = false) List<String> queryWordList,
                                                               @RequestParam(name = "tags", required = false) List<String> tags,
                                                               @RequestParam(name = "city", required = false) String cityName)
    {
        return ResponseEntity.ok(eventService.searchEvents(queryWordList, tags, cityName));
    }
    //**********************************************************************************************************************
    //---------------------------------------------------POST---------------------------------------------------------------
    // *********************************************************************************************************************
    @PostMapping
    @Transactional
    public ResponseEntity<EventDto> createEvent(@Valid @RequestBody EventCreateDto eventCreateDto)
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(eventCreateDto));
    }

    @PostMapping("/{eventId}/files")
    public ResponseEntity<FileOverviewDto> uploadFileToEvent(@Valid @ModelAttribute FileUploadDto fileUploadDto,
                                                             @PathVariable("eventId") UUID eventId,
                                                             @RequestHeader("Authorization") String jwt) throws IOException
    {
        return ResponseEntity.ok(eventService.uploadFileToEvent(fileUploadDto,eventId,jwt.substring(TOKEN_PREFIX_LENGTH)));
    }

    @PostMapping("/{eventId}/attend")
    public ResponseEntity<Boolean> attendEvent(@PathVariable("eventId") UUID eventId,
                                               @RequestHeader("Authorization") String jwt)
    {
        return  ResponseEntity.ok(eventService.addAttenderToEvent(eventId, jwt.substring(TOKEN_PREFIX_LENGTH)));
    }

    @PostMapping("/{eventId}/threads")
    public ResponseEntity<ThreadDto> createNewThreadInEvent(@PathVariable("eventId") UUID eventId,
                                                            @Valid @RequestBody ThreadCreateDto threadCreateDto,
                                                            @RequestHeader("Authorization") String jwt)
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createThreadInEvent(threadCreateDto, eventId, jwt.substring(TOKEN_PREFIX_LENGTH)));
    }

    @PostMapping("/{eventId}/threads/{threadId}/replies")
    public ResponseEntity<ThreadReplyDto> createReplyInThread(@PathVariable("eventId") UUID eventId,
                                                              @PathVariable("threadId") UUID threadId,
                                                              @Valid @RequestBody ThreadReplyCreateDto threadReplyCreateDto,
                                                              @RequestHeader("Authorization") String jwt)
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createReplyInThread(threadReplyCreateDto, eventId, threadId, jwt.substring(TOKEN_PREFIX_LENGTH)));
    }

    //**********************************************************************************************************************
    //---------------------------------------------------PUT----------------------------------------------------------------
    // *********************************************************************************************************************
    @PutMapping("/{eventId}")
    @Transactional
    public ResponseEntity<EventDto> updateEvent(@Valid @RequestBody EventCreateDto eventUpdateDto,
                                                @PathVariable("eventId") UUID eventId,
                                                @RequestHeader("Authorization") String jwt)
    {
        return  ResponseEntity.ok(eventService.updateEvent(eventUpdateDto, eventId, jwt.substring(TOKEN_PREFIX_LENGTH)))  ;
    }

    @PutMapping("/{eventId}/threads/{threadId}")
    public ResponseEntity<ThreadDto> updateThreadInEventN(@PathVariable("eventId") UUID eventId,
                                                         @PathVariable("threadId") UUID threadId,
                                                         @Valid @RequestBody ThreadCreateDto threadUpdateDto,
                                                         @RequestHeader("Authorization") String jwt)
    {
        return ResponseEntity.ok(eventService.updateThreadInEvent(threadUpdateDto, eventId,threadId, jwt.substring(TOKEN_PREFIX_LENGTH)));
    }

    @PutMapping("/{eventId}/threads/{threadId}/replies/{replyId}")
    public ResponseEntity<ThreadReplyDto> updateReplyInThread(@PathVariable("eventId") UUID eventId,
                                                         @PathVariable("threadId") UUID threadId,
                                                         @PathVariable("replyId") UUID replyId,
                                                         @Valid @RequestBody ThreadReplyCreateDto threadReplyCreateDto,
                                                         @RequestHeader("Authorization") String jwt)
    {
        return ResponseEntity.ok().body(eventService.updateThreadReplyInEventThread(threadReplyCreateDto, eventId, threadId, replyId,jwt.substring(TOKEN_PREFIX_LENGTH)));
    }

}
