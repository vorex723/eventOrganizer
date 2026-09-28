package com.mazurek.eventOrganizer.tag;

import com.mazurek.eventOrganizer.event.EventService;
import com.mazurek.eventOrganizer.event.dto.EventOverviewPageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagService tagService;
    private final EventService eventService;

    @GetMapping("/{tagName}/events")
    public ResponseEntity<EventOverviewPageDto> getTagEvents(
            @PathVariable("tagName") String tagName,
            @RequestParam(name = "page", defaultValue = "0") int page) {
        return ResponseEntity.ok(eventService.getTagEventsByTagName(tagName, page));
    }

    @GetMapping("/{tagName}")
    public ResponseEntity<TagDto> getTag(@PathVariable("tagName") String tagName){
            return ResponseEntity.ok(tagService.getTagByName(tagName));
    }
}
