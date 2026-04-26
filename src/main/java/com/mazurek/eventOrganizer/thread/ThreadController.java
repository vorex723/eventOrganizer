package com.mazurek.eventOrganizer.thread;

import com.mazurek.eventOrganizer.common.SortDirection;
import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadDto;
import com.mazurek.eventOrganizer.thread.dto.ThreadOverviewPageDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events")
public class ThreadController {

    private final ThreadService threadService;

    @GetMapping("/{eventId}/threads")
    public ResponseEntity<ThreadOverviewPageDto> getThreadsByEventId(
            @PathVariable UUID eventId,
            @RequestParam(name = "page", defaultValue = "0", required = false) int pageNumber,
            @RequestParam(name = "sortBy", defaultValue = "LAST_ACTIVITY", required = false) ThreadSortField sortBy,
            @RequestParam(name = "direction", defaultValue = "DESC", required = false) SortDirection direction)
    {
        return ResponseEntity.ok(threadService.getThreadsByEventId(eventId, pageNumber, sortBy, direction));
    }

    @PostMapping("/{eventId}/threads")
    public ResponseEntity<ThreadDto> createNewThreadInEvent(@PathVariable("eventId") UUID eventId,
                                                            @Valid @RequestBody ThreadCreateDto threadCreateDto)
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(threadService.createThreadInEvent(threadCreateDto, eventId));
    }

    @PutMapping("/{eventId}/threads/{threadId}")
    public ResponseEntity<ThreadDto> updateThreadInEvent(@PathVariable("eventId") UUID eventId,
                                                         @PathVariable("threadId") UUID threadId,
                                                         @Valid @RequestBody ThreadCreateDto threadUpdateDto)
    {
        return ResponseEntity.ok(threadService.updateThreadInEvent(threadUpdateDto, eventId, threadId));
    }
}
