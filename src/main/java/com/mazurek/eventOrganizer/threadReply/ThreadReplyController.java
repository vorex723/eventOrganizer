package com.mazurek.eventOrganizer.threadReply;

import com.mazurek.eventOrganizer.threadReply.dto.ThreadReplyCreateDto;
import com.mazurek.eventOrganizer.threadReply.dto.ThreadReplyDto;
import com.mazurek.eventOrganizer.threadReply.dto.ThreadReplyPageDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events")
public class ThreadReplyController {

    private final ThreadReplyService threadReplyService;

    @PostMapping("/{eventId}/threads/{threadId}/replies")
    public ResponseEntity<ThreadReplyDto> createReplyInThread(@PathVariable("eventId") UUID eventId,
                                                              @PathVariable("threadId") UUID threadId,
                                                              @Valid @RequestBody ThreadReplyCreateDto threadReplyCreateDto)
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(threadReplyService.createReplyInThread(threadReplyCreateDto, eventId, threadId));
    }

    @PutMapping("/{eventId}/threads/{threadId}/replies/{replyId}")
    public ResponseEntity<ThreadReplyDto> updateReplyInThread(@PathVariable("eventId") UUID eventId,
                                                              @PathVariable("threadId") UUID threadId,
                                                              @PathVariable("replyId") UUID replyId,
                                                              @Valid @RequestBody ThreadReplyCreateDto threadReplyCreateDto)
    {
        return ResponseEntity.ok().body(threadReplyService.updateThreadReplyInEventThread(threadReplyCreateDto, eventId, threadId, replyId));
    }

    @GetMapping("/{eventId}/threads/{threadId}/replies")
    public ResponseEntity<ThreadReplyPageDto> getRepliesInThread(@PathVariable("eventId") UUID eventId,
                                                                 @PathVariable("threadId") UUID threadId,
                                                                 @RequestParam(name = "page", defaultValue = "0", required = false) int pageNumber)
    {
        return ResponseEntity.ok(threadReplyService.getRepliesInEventThread(eventId, threadId, pageNumber));
    }
}
